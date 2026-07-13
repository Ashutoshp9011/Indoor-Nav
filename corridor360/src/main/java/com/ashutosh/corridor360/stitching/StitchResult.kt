package com.ashutosh.corridor360.stitching

sealed class StitchResult {
    data class Success(val outputPath: String) : StitchResult()
    data class Failure(val reason: String) : StitchResult()
}
