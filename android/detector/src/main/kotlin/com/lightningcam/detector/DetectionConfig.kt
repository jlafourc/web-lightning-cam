package com.lightningcam.detector

data class DetectionConfig(
    val warmupFrames: Int = 12,
    val globalMeanDelta: Double = 22.0,
    val globalChangedRatio: Double = 0.18,
    val localizedMeanDelta: Double = 52.0,
    val localizedChangedRatio: Double = 0.012,
    val pixelDelta: Int = 42,
    val refractoryMs: Long = 650,
    val baselineAlpha: Double = 0.05,
) {
    companion object {
        fun field() = DetectionConfig(
            globalMeanDelta = 12.0,
            globalChangedRatio = 0.08,
            localizedMeanDelta = 30.0,
            localizedChangedRatio = 0.008,
            pixelDelta = 28,
            baselineAlpha = 0.03,
        )
    }

    init {
        require(warmupFrames > 0)
        require(globalChangedRatio in 0.0..1.0)
        require(localizedChangedRatio in 0.0..1.0)
        require(pixelDelta in 1..255)
        require(refractoryMs >= 0)
        require(baselineAlpha in 0.0..1.0)
    }
}
