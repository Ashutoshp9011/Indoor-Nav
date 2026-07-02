package com.ashutosh.corridor360.stitching

import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.stitching.Stitcher
import java.io.File

sealed class StitchResult {
    data class Success(val outputPath: String) : StitchResult()
    data class Failure(val reason: String, val stitcherStatus: Int? = null) : StitchResult()
}

/**
 * Wraps OpenCV's Stitcher, configured in SCANS mode rather than the default
 * PANORAMA mode. SCANS assumes translation-dominant motion (walking down a
 * corridor) rather than a rotation-dominant panning shot, which matches how
 * Corridor360 captures frames — this should be materially more robust than
 * the default here.
 *
 * Runs on a background thread — call from a coroutine with Dispatchers.Default
 * or Dispatchers.IO, never the main thread (stitching is CPU-heavy and can
 * take several seconds for 6+ frames).
 */
class PanoramaStitcher {

    fun stitch(framePaths: List<String>, outputDir: File, outputFileName: String): StitchResult {
        if (framePaths.size < 2) {
            return StitchResult.Failure("Need at least 2 frames to stitch, got ${framePaths.size}")
        }

        val mats = framePaths.map { path ->
            Imgcodecs.imread(path)
        }

        val emptyIndex = mats.indexOfFirst { it.empty() }
        if (emptyIndex != -1) {
            return StitchResult.Failure("Failed to load frame at index $emptyIndex: ${framePaths[emptyIndex]}")
        }

        val stitcher = Stitcher.create(Stitcher.SCANS)
        val result = Mat()

        val status = stitcher.stitch(mats, result)

        mats.forEach { it.release() }

        if (status != Stitcher.OK) {
            result.release()
            return StitchResult.Failure(
                reason = describeStatus(status),
                stitcherStatus = status
            )
        }

        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, outputFileName)
        val written = Imgcodecs.imwrite(outputFile.absolutePath, result)
        result.release()

        return if (written) {
            StitchResult.Success(outputFile.absolutePath)
        } else {
            StitchResult.Failure("Stitch succeeded but failed to write output file")
        }
    }

    private fun describeStatus(status: Int): String = when (status) {
        Stitcher.ERR_NEED_MORE_IMGS ->
            "Not enough overlap between frames — try capturing with tighter spacing"
        Stitcher.ERR_HOMOGRAPHY_EST_FAIL ->
            "Couldn't align frames — likely low-texture surfaces (blank walls). " +
                    "Consider pose-assisted warping using ARCore data instead of pure feature matching"
        Stitcher.ERR_CAMERA_PARAMS_ADJUST_FAIL ->
            "Camera parameter estimation failed — check for inconsistent exposure/focus across frames"
        else -> "Unknown stitch failure (status code $status)"
    }
}