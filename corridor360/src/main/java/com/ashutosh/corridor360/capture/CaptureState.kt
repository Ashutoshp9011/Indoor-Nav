package com.ashutosh.corridor360.capture

enum class CaptureState {
    CAMERA_ACTIVE,
    CAPTURING,
    STOPPING_CAMERA,
    ARCORE_READING,
    RESTARTING_CAMERA,
    STITCHING,
    STITCH_COMPLETE,
    ERROR
}