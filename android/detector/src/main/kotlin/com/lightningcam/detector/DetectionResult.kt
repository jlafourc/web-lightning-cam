package com.lightningcam.detector

enum class TriggerType { GLOBAL, LOCALIZED }

data class DetectionResult(
    val trigger: TriggerType?,
    val globalScore: Double,
    val localizedScore: Double,
    val changedPixelRatio: Double,
    val warmedUp: Boolean,
    val motionRejected: Boolean = false,
    val negativeChangedRatio: Double = 0.0,
    val largestComponentPixels: Int = 0,
)
