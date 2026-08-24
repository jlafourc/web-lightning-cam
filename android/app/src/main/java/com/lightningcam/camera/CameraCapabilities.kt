package com.lightningcam.camera

data class CameraCapabilities(
    val exposureCompensationRange: IntRange,
    val supportsManualSensor: Boolean,
    val minimumFocusDistance: Float?,
    val isoRange: IntRange?,
    val exposureTimeNsRange: LongRange?,
) {
    val supportsInfinityFocus: Boolean get() = minimumFocusDistance != null && minimumFocusDistance > 0f
}
