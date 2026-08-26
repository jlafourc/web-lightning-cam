package com.lightningcam.detector

enum class DetectionProfile { STRICT, BALANCED, SENSITIVE }

data class DetectionConfig(
    val warmupFrames: Int = 12,
    val globalMeanDelta: Double = 22.0,
    val globalChangedRatio: Double = 0.18,
    val localizedMeanDelta: Double = 52.0,
    val localizedChangedRatio: Double = 0.012,
    val pixelDelta: Int = 42,
    val refractoryMs: Long = 650,
    val baselineAlpha: Double = 0.05,
    val noiseAlpha: Double = 0.08,
    val noiseFloor: Double = 4.0,
    val sigmaThreshold: Double = 4.5,
    val maxMotionRatio: Double = 0.16,
    val maxNegativeToPositiveRatio: Double = 0.65,
    val minComponentPixels: Int = 3,
    val minComponentSpan: Int = 4,
    val minElongation: Double = 1.8,
    val cloudComponentPixels: Int = 10,
) {
    companion object {
        fun runtimeDefault() = forProfile(DetectionProfile.BALANCED)

        fun field() = forProfile(DetectionProfile.SENSITIVE)

        fun forProfile(profile: DetectionProfile) = when (profile) {
            DetectionProfile.STRICT -> DetectionConfig(
                globalMeanDelta = 18.0,
                globalChangedRatio = 0.15,
                localizedMeanDelta = 45.0,
                localizedChangedRatio = 0.010,
                pixelDelta = 38,
                baselineAlpha = 0.025,
                sigmaThreshold = 6.0,
                maxMotionRatio = 0.12,
                maxNegativeToPositiveRatio = 0.45,
                minComponentPixels = 5,
                minComponentSpan = 6,
                minElongation = 2.2,
                cloudComponentPixels = 16,
            )
            DetectionProfile.BALANCED -> DetectionConfig(
                globalMeanDelta = 14.0,
                globalChangedRatio = 0.10,
                localizedMeanDelta = 36.0,
                localizedChangedRatio = 0.007,
                pixelDelta = 32,
                baselineAlpha = 0.03,
                sigmaThreshold = 4.5,
                minComponentPixels = 3,
                minComponentSpan = 4,
                minElongation = 1.8,
                cloudComponentPixels = 10,
            )
            DetectionProfile.SENSITIVE -> DetectionConfig(
                globalMeanDelta = 10.0,
                globalChangedRatio = 0.06,
                localizedMeanDelta = 28.0,
                localizedChangedRatio = 0.004,
                pixelDelta = 26,
                baselineAlpha = 0.03,
                sigmaThreshold = 3.5,
                maxMotionRatio = 0.22,
                maxNegativeToPositiveRatio = 0.85,
                minComponentPixels = 2,
                minComponentSpan = 3,
                minElongation = 1.5,
                cloudComponentPixels = 7,
            )
        }
    }

    init {
        require(warmupFrames > 0)
        require(globalChangedRatio in 0.0..1.0)
        require(localizedChangedRatio in 0.0..1.0)
        require(pixelDelta in 1..255)
        require(refractoryMs >= 0)
        require(baselineAlpha in 0.0..1.0)
        require(noiseAlpha in 0.0..1.0)
        require(noiseFloor > 0.0)
        require(sigmaThreshold > 0.0)
        require(maxMotionRatio in 0.0..1.0)
        require(maxNegativeToPositiveRatio >= 0.0)
        require(minComponentPixels > 0)
        require(minComponentSpan > 0)
        require(minElongation >= 1.0)
        require(cloudComponentPixels >= minComponentPixels)
    }
}
