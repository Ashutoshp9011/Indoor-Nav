package com.ashutosh.corridor360.stitching

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.stitching.Stitcher
import java.io.File
import java.util.UUID

class PanoramaStitcher(
    private val outputDir: File? = null
) {

    /**
     * Versatile stitch method that supports all existing call sites in the project.
     * 
     * @param imagePaths List of paths to images to be stitched.
     * @param outDir Optional output directory. If null, uses the one from constructor.
     * @param fileName Optional output filename. If null, generates a random UUID.
     */
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

        val mats = imagePaths.map { path ->
            val mat = Imgcodecs.imread(path)
            if (mat.empty()) return StitchResult.Failure("Failed to load: $path")
            mat
        }

        val stitcher = Stitcher.create(Stitcher.PANORAMA)
        val result = Mat()
        val status = stitcher.stitch(mats, result)

        mats.forEach { it.release() }

        if (status != Stitcher.OK) {
            result.release()
            return StitchResult.Failure("Stitch failed, status=$status")
        }

        if (!finalOutputDir.exists()) {
            finalOutputDir.mkdirs()
        }
        val outputFile = File(finalOutputDir, finalFileName)
        Imgcodecs.imwrite(outputFile.absolutePath, result)
        result.release()

        return StitchResult.Success(outputFile.absolutePath)
    }
}
