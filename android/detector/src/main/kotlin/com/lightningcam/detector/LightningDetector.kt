package com.lightningcam.detector

class LightningDetector(private val config: DetectionConfig = DetectionConfig()) {
    private var baseline = DoubleArray(0)
    private var observedFrames = 0
    private var lastTriggerMs = Long.MIN_VALUE

    fun analyze(frame: LuminanceFrame): DetectionResult {
        ensureBaseline(frame)
        if (observedFrames < config.warmupFrames) {
            warmBaseline(frame)
            observedFrames++
            return DetectionResult(null, 0.0, 0.0, 0.0, warmedUp = false)
        }

        var positiveDeltaSum = 0.0
        var changedDeltaSum = 0.0
        var changedCount = 0
        frame.pixels.forEachIndexed { index, value ->
            val delta = value - baseline[index]
            if (delta > 0) positiveDeltaSum += delta
            if (delta >= config.pixelDelta) {
                changedCount++
                changedDeltaSum += delta
            }
        }

        val changedRatio = changedCount.toDouble() / frame.pixels.size
        val globalScore = positiveDeltaSum / frame.pixels.size
        val localizedScore = if (changedCount == 0) 0.0 else changedDeltaSum / changedCount
        val outsideRefractory =
            lastTriggerMs == Long.MIN_VALUE || frame.timestampMs - lastTriggerMs >= config.refractoryMs
        val trigger = when {
            !outsideRefractory -> null
            globalScore >= config.globalMeanDelta && changedRatio >= config.globalChangedRatio -> TriggerType.GLOBAL
            localizedScore >= config.localizedMeanDelta && changedRatio >= config.localizedChangedRatio -> TriggerType.LOCALIZED
            else -> null
        }

        if (trigger != null) lastTriggerMs = frame.timestampMs
        updateBaseline(frame)
        return DetectionResult(trigger, globalScore, localizedScore, changedRatio, warmedUp = true)
    }

    fun reset() {
        baseline = DoubleArray(0)
        observedFrames = 0
        lastTriggerMs = Long.MIN_VALUE
    }

    private fun ensureBaseline(frame: LuminanceFrame) {
        if (baseline.size != frame.pixels.size) {
            baseline = DoubleArray(frame.pixels.size)
            observedFrames = 0
            lastTriggerMs = Long.MIN_VALUE
        }
    }

    private fun warmBaseline(frame: LuminanceFrame) {
        val count = observedFrames + 1.0
        frame.pixels.forEachIndexed { index, value ->
            baseline[index] += (value - baseline[index]) / count
        }
    }

    private fun updateBaseline(frame: LuminanceFrame) {
        frame.pixels.forEachIndexed { index, value ->
            baseline[index] += (value - baseline[index]) * config.baselineAlpha
        }
    }
}
