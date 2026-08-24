package com.lightningcam.camera

enum class FocusMode { CONTINUOUS, INFINITY }

data class CameraSettings(
    val exposureCompensation: Int = 0,
    val focusMode: FocusMode = FocusMode.CONTINUOUS,
    val manualExposure: Boolean = false,
    val iso: Int = 400,
    val exposureTimeNs: Long = 4_000_000,
) {
    companion object {
        fun automatic() = CameraSettings()
    }
}
