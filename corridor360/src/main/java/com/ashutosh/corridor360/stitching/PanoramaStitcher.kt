package com.ashutosh.corridor360.stitching

import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.UUID

/**
 * Custom multi-image panorama stitcher.
 *
 * org.opencv.stitching.Stitcher has no Java bindings in the Android build of
 * OpenCV we depend on (org.opencv:opencv:4.10.0) -- the native stitching module
 * isn't compiled into that artifact. This replaces it with a from-scratch
 * pipeline: ORB features -> brute-force Hamming matching -> per-pair RANSAC
 * homography -> a homography graph chained to one reference frame -> a shared
 * canvas -> warp + basic overwrite blending.
 *
 * Known limitations (fine for Phase 1, revisit if quality isn't good enough):
 * - No bundle adjustment / global refinement -- only pairwise homographies
 *   chained through the reference, so error can accumulate across many frames.
 * - No loop closure -- for a full 360 set (e.g. 8 frames @ 45 degrees) the last
 *   frame isn't explicitly re-aligned back to the first, so a visible seam at
 *   the wrap-around point is expected.
 * - Blending is "last write wins" in overlap regions, not feathered/multi-band,
 *   so seams may be visible.
 * - Real risk for THIS capture pattern specifically: adjacent frames are 45
 *   degrees apart. A typical phone's horizontal FOV is ~65-70 degrees, which
 *   leaves only ~20-25 degrees of true overlap between neighbors -- borderline
 *   for reliable ORB matching. If pairwise homography estimation fails often
 *   in practice, the fix is capturing more frames at a smaller angular
 *   interval (e.g. 16 @ 22.5 degrees), not a smarter matcher.
 */
class PanoramaStitcher(
    private val outputDir: File? = null
) {

    private data class FrameFeatures(
        val colorMat: Mat,
        val keypoints: MatOfKeyPoint,
        val descriptors: Mat
    )

    fun stitch(
        imagePaths: List<String>,
        outDir: File? = null,
        fileName: String? = null
    ): StitchResult {
        val finalOutputDir = outDir ?: outputDir ?: return StitchResult.Failure("Output directory not configured")
        val finalFileName = if (fileName != null) {
            if (fileName.endsWith(".jpg")) fileName else "panorama_$fileName.jpg"
        } else {
            "panorama_${UUID.randomUUID()}.jpg"
        }

        if (imagePaths.size < 2) {
            return StitchResult.Failure("Need at least 2 frames to stitch")
        }

        val loaded = mutableListOf<FrameFeatures>()
        try {
            // --- Step 1: load + detect ORB features per frame ---
            val orb = ORB.create(3000)
            for (path in imagePaths) {
                val color = Imgcodecs.imread(path)
                if (color.empty()) {
                    return StitchResult.Failure("Failed to load: $path")
                }
                val gray = Mat()
                Imgproc.cvtColor(color, gray, Imgproc.COLOR_BGR2GRAY)

                val keypoints = MatOfKeyPoint()
                val descriptors = Mat()
                orb.detectAndCompute(gray, Mat(), keypoints, descriptors)
                gray.release()

                if (descriptors.empty()) {
                    return StitchResult.Failure("No features found in frame: $path")
                }
                loaded.add(FrameFeatures(color, keypoints, descriptors))
            }

            val n = loaded.size
            val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)

            // --- Step 2: pairwise homography for each adjacent pair (i -> i+1) ---
            // pairwiseH[i] maps points from frame i+1 into frame i's coordinate space.
            val pairwiseH = arrayOfNulls<Mat>(n - 1)
            for (i in 0 until n - 1) {
                val h = estimatePairwiseHomography(
                    matcher = matcher,
                    query = loaded[i + 1],
                    train = loaded[i]
                ) ?: return StitchResult.Failure(
                    "Not enough matching features between frame $i and frame ${i + 1} -- " +
                        "try recapturing with more overlap between adjacent shots"
                )
                pairwiseH[i] = h
            }

            // --- Step 3: build the homography graph -- chain every frame's pairwise H
            // through to one reference frame (the middle one, least accumulated error) ---
            val refIndex = n / 2
            val toReference = Array(n) { k -> cumulativeHomography(pairwiseH, k, refIndex) }

            // --- Step 4: compute the shared canvas size from all frames' warped corners ---
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE

            for (k in 0 until n) {
                val w = loaded[k].colorMat.size().width
                val h = loaded[k].colorMat.size().height
                val corners = MatOfPoint2f(
                    Point(0.0, 0.0), Point(w, 0.0), Point(w, h), Point(0.0, h)
                )
                val warpedCorners = MatOfPoint2f()
                Core.perspectiveTransform(corners, warpedCorners, toReference[k])
                for (p in warpedCorners.toArray()) {
                    if (p.x < minX) minX = p.x
                    if (p.y < minY) minY = p.y
                    if (p.x > maxX) maxX = p.x
                    if (p.y > maxY) maxY = p.y
                }
                corners.release()
                warpedCorners.release()
            }

            val canvasWidth = (maxX - minX).toInt().coerceAtLeast(1)
            val canvasHeight = (maxY - minY).toInt().coerceAtLeast(1)

            // Hard safety cap -- a bad homography chain can blow this up to an
            // unusable/OOM-inducing size. Bail out with a clear reason instead of
            // trying to allocate a multi-gigabyte Mat.
            val maxCanvasPixels = 40_000_000L // ~40MP ceiling
            if (canvasWidth.toLong() * canvasHeight.toLong() > maxCanvasPixels) {
                return StitchResult.Failure(
                    "Computed panorama canvas ($canvasWidth x $canvasHeight) is unreasonably large -- " +
                        "homography estimation likely diverged on this frame set"
                )
            }

            val translation = Mat.eye(3, 3, CvType.CV_64F)
            translation.put(0, 2, -minX)
            translation.put(1, 2, -minY)

            // --- Step 5: warp every frame into the shared canvas and composite ---
            val canvas = Mat.zeros(Size(canvasWidth.toDouble(), canvasHeight.toDouble()), CvType.CV_8UC3)

            // Reference frame first, then outward -- later frames only fill in gaps
            // the reference and closer frames didn't cover, which keeps the most
            // "central" (best-aligned) content least likely to be overwritten.
            val order = (0 until n).sortedBy { kotlin.math.abs(it - refIndex) }
            for (k in order) {
                val finalH = translation.matMul(toReference[k])
                val warped = Mat()
                Imgproc.warpPerspective(
                    loaded[k].colorMat, warped, finalH, Size(canvasWidth.toDouble(), canvasHeight.toDouble())
                )

                val warpedGray = Mat()
                Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
                val mask = Mat()
                Imgproc.threshold(warpedGray, mask, 0.0, 255.0, Imgproc.THRESH_BINARY)

                warped.copyTo(canvas, mask)

                warped.release()
                warpedGray.release()
                mask.release()
                finalH.release()
            }

            if (!finalOutputDir.exists()) finalOutputDir.mkdirs()
            val outputFile = File(finalOutputDir, finalFileName)
            Imgcodecs.imwrite(outputFile.absolutePath, canvas)
            canvas.release()
            translation.release()
            pairwiseH.forEach { it?.release() }
            toReference.forEach { it.release() }

            return StitchResult.Success(outputFile.absolutePath)
        } catch (e: Exception) {
            return StitchResult.Failure("Stitching error: ${e.message}")
        } finally {
            loaded.forEach {
                it.colorMat.release()
                it.keypoints.release()
                it.descriptors.release()
            }
        }
    }

    /** Returns the 3x3 homography mapping [query]'s points into [train]'s frame, or null if too few good matches/inliers. */
    private fun estimatePairwiseHomography(
        matcher: DescriptorMatcher,
        query: FrameFeatures,
        train: FrameFeatures
    ): Mat? {
        val knnMatches = mutableListOf<MatOfDMatch>()
        matcher.knnMatch(query.descriptors, train.descriptors, knnMatches, 2)

        // Lowe's ratio test
        val good = knnMatches.filter { it.toArray().size == 2 }
            .map { it.toArray() }
            .filter { it[0].distance < 0.75f * it[1].distance }
            .map { it[0] }

        val minMatches = 12
        if (good.size < minMatches) return null

        val queryKp = query.keypoints.toArray()
        val trainKp = train.keypoints.toArray()

        val srcPoints = MatOfPoint2f(*good.map { queryKp[it.queryIdx].pt }.toTypedArray())
        val dstPoints = MatOfPoint2f(*good.map { trainKp[it.trainIdx].pt }.toTypedArray())

        val homography = Calib3d.findHomography(srcPoints, dstPoints, Calib3d.RANSAC, 3.0)
        srcPoints.release()
        dstPoints.release()

        return if (homography.empty()) null else homography
    }

    /**
     * Chains pairwise homographies (each mapping frame i+1 -> frame i) into a
     * single homography mapping frame [k]'s points into frame [reference]'s frame.
     */
    private fun cumulativeHomography(pairwiseH: Array<Mat?>, k: Int, reference: Int): Mat {
        var acc = Mat.eye(3, 3, CvType.CV_64F)
        when {
            k == reference -> return acc
            k > reference -> {
                for (i in k - 1 downTo reference) {
                    val h = pairwiseH[i] ?: continue
                    val next = h.matMul(acc)
                    acc.release()
                    acc = next
                }
            }
            else -> {
                for (i in k until reference) {
                    val h = pairwiseH[i] ?: continue
                    val inv = h.inv()
                    val next = inv.matMul(acc)
                    inv.release()
                    acc.release()
                    acc = next
                }
            }
        }
        return acc
    }
}
