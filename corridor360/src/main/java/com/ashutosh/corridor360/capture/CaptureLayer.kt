package com.ashutosh.corridor360.capture

/**
 * The three tilt rings captured per node, per the scoped-down orbit-capture plan
 * (fixed-count orbit at 2-3 tilt levels, guaranteed coverage by design).
 *
 * pitchRangeDeg is the SensorHeadingProvider.pitchDeg band that counts as "in this
 * layer." Signs/ranges are a starting point — SensorManager.getOrientation's pitch
 * convention depends on how the phone is held, so these will likely need a quick
 * on-device tuning pass rather than trusting the numbers as-is.
 */
enum class CaptureLayer(val label: String, val pitchRangeDeg: ClosedFloatingPointRange<Float>) {
    MIDDLE("eye level", -15f..15f),
    TOP("ceiling", 25f..55f),
    BOTTOM("floor", -55f..-25f);

    companion object {
        // Start level, then up, then down — easiest physical motion for the user.
        val scanOrder = listOf(MIDDLE, TOP, BOTTOM)
    }
}
