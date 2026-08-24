package com.lightningcam.detector

enum class TriggerType { GLOBAL, LOCALIZED }

data class DetectionResult(
    val trigger: TriggerType?,
    val globalScore: Double,
    val localizedScore: Double,
    val changedPixelRatio: Double,
    val warmedUp: Boolean,
)
