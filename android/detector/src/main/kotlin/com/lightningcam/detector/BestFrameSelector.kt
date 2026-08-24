package com.lightningcam.detector

data class FrameCandidate<T>(
    val value: T,
    val sharpness: Double,
    val information: Double,
    val saturationRatio: Double,
) {
    init {
        require(sharpness >= 0.0)
        require(information >= 0.0)
        require(saturationRatio in 0.0..1.0)
    }
}

object BestFrameSelector {
    fun <T> select(candidates: List<FrameCandidate<T>>): FrameCandidate<T>? =
        candidates
            .asSequence()
            .filter { it.saturationRatio < 0.5 }
            .maxByOrNull { candidate ->
                candidate.sharpness * 0.4 +
                    candidate.information * 0.6 -
                    candidate.saturationRatio * 100.0
            }
}
