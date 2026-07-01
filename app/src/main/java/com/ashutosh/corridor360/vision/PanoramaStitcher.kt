package com.ashutosh.corridor360.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.stitching.Stitcher
import java.io.File

class PanoramaStitcher {

    sealed class Result {
        data class Success(val outputPath: String) : Result()
        data class Failure(val reason: String, val statusCode: Int? = null) : Result()
    }

    /**
     * Stitches a list of Bitmaps into a panorama.
     */
    fun stitchBitmaps(bitmaps: List<Bitmap>, outputDir: File, nodeId: String): Result {
        if (bitmaps.size < 2) {
            return Result.Failure("Need at least 2 bitmaps to stitch")
        }

        val mats = mutableListOf<Mat>()
        try {
            for (bitmap in bitmaps) {
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                // Stitcher works best with BGR images (OpenCV default)
                // bitmapToMat usually produces RGBA
                mats.add(mat)
            }
            return performStitch(mats, outputDir, nodeId)
        } catch (e: Exception) {
            return Result.Failure("Bitmap conversion failed: ${e.message}")
        } finally {
            mats.forEach { it.release() }
        }
    }

    // Run OFF the main thread — stitching is CPU-heavy and can take several seconds for 6-10 frames
    fun stitch(framePaths: List<String>, outputDir: File, nodeId: String): Result {
        if (framePaths.size < 2) {
            return Result.Failure("Need at least 2 frames to stitch, got ${framePaths.size}")
        }

        val mats = mutableListOf<Mat>()
        try {
            for (path in framePaths) {
                val mat = Imgcodecs.imread(path)
                if (mat.empty()) {
                    Log.w("PanoramaStitcher", "Skipping unreadable frame: $path")
                    continue
                }
                mats.add(mat)
            }

            if (mats.size < 2) {
                return Result.Failure("Fewer than 2 frames decoded successfully")
            }

            return performStitch(mats, outputDir, nodeId)
        } catch (e: Exception) {
            Log.e("PanoramaStitcher", "Stitching threw exception", e)
            return Result.Failure("Exception: ${e.message}")
        } finally {
            mats.forEach { it.release() } // critical — native Mat memory, not GC'd automatically
        }
    }

    private fun performStitch(mats: List<Mat>, outputDir: File, nodeId: String): Result {
        val stitcher = Stitcher.create(Stitcher.PANORAMA)
        val panoramaResult = Mat()
        val status = stitcher.stitch(mats, panoramaResult)

        if (status != Stitcher.OK) {
            panoramaResult.release()
            return Result.Failure("Stitch failed, OpenCV status code: $status", status)
        }

        outputDir.mkdirs()
        val outFile = File(outputDir, "${nodeId}_panorama.jpg")
        val saved = Imgcodecs.imwrite(outFile.absolutePath, panoramaResult)
        panoramaResult.release()

        return if (saved) Result.Success(outFile.absolutePath)
        else Result.Failure("imwrite returned false — check storage permissions/path")
    }
}
