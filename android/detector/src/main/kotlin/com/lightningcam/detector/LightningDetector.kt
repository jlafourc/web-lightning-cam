package com.lightningcam.detector

import kotlin.math.abs

class LightningDetector(private val config: DetectionConfig = DetectionConfig()) {
    private var baseline = DoubleArray(0)
    private var noise = DoubleArray(0)
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
        var negativeChangedCount = 0
        val positiveMask = BooleanArray(frame.pixels.size)
        val negativeMask = BooleanArray(frame.pixels.size)
        frame.pixels.forEachIndexed { index, value ->
            val delta = value - baseline[index]
            if (delta > 0) positiveDeltaSum += delta
            val normalizedDelta = delta / maxOf(noise[index], config.noiseFloor)
            if (delta >= config.pixelDelta && normalizedDelta >= config.sigmaThreshold) {
                positiveMask[index] = true
                changedCount++
                changedDeltaSum += delta
            } else if (delta <= -config.pixelDelta && -normalizedDelta >= config.sigmaThreshold) {
                negativeMask[index] = true
                negativeChangedCount++
            }
        }

        val changedRatio = changedCount.toDouble() / frame.pixels.size
        val negativeChangedRatio = negativeChangedCount.toDouble() / frame.pixels.size
        val globalScore = positiveDeltaSum / frame.pixels.size
        val localizedScore = if (changedCount == 0) 0.0 else changedDeltaSum / changedCount
        val motionRatio = changedRatio + negativeChangedRatio
        val negativeToPositiveRatio = negativeChangedCount.toDouble() / maxOf(changedCount, 1)
        val motionRejected =
            motionRatio >= config.maxMotionRatio &&
                negativeToPositiveRatio >= config.maxNegativeToPositiveRatio
        val components = TransientComponents.extract(frame.width, frame.height, positiveMask)
        val largestComponentPixels = components.maxOfOrNull { it.area } ?: 0
        val spatiallyValid = components.any { component ->
            component.area >= config.minComponentPixels &&
                component.span >= config.minComponentSpan &&
                component.elongation >= config.minElongation
        }
        val outsideRefractory =
            lastTriggerMs == Long.MIN_VALUE || frame.timestampMs - lastTriggerMs >= config.refractoryMs
        val trigger = when {
            !outsideRefractory -> null
            motionRejected -> null
            globalScore >= config.globalMeanDelta &&
                changedRatio >= config.globalChangedRatio &&
                largestComponentPixels >= config.cloudComponentPixels -> TriggerType.GLOBAL
            localizedScore >= config.localizedMeanDelta &&
                changedRatio >= config.localizedChangedRatio &&
                spatiallyValid -> TriggerType.LOCALIZED
            else -> null
        }

        if (trigger != null) lastTriggerMs = frame.timestampMs
        updateBaseline(frame, positiveMask, negativeMask)
        return DetectionResult(
            trigger = trigger,
            globalScore = globalScore,
            localizedScore = localizedScore,
            changedPixelRatio = changedRatio,
            warmedUp = true,
            motionRejected = motionRejected,
            negativeChangedRatio = negativeChangedRatio,
            largestComponentPixels = largestComponentPixels,
        )
    }

    fun reset() {
        baseline = DoubleArray(0)
        noise = DoubleArray(0)
        observedFrames = 0
        lastTriggerMs = Long.MIN_VALUE
    }

    private fun ensureBaseline(frame: LuminanceFrame) {
        if (baseline.size != frame.pixels.size) {
            baseline = DoubleArray(frame.pixels.size)
            noise = DoubleArray(frame.pixels.size) { config.noiseFloor }
            observedFrames = 0
            lastTriggerMs = Long.MIN_VALUE
        }
    }

    private fun warmBaseline(frame: LuminanceFrame) {
        val count = observedFrames + 1.0
        frame.pixels.forEachIndexed { index, value ->
            baseline[index] += (value - baseline[index]) / count
            val deviation = abs(value - baseline[index])
            noise[index] += (deviation - noise[index]) * config.noiseAlpha
        }
    }

    private fun updateBaseline(
        frame: LuminanceFrame,
        positiveMask: BooleanArray,
        negativeMask: BooleanArray,
    ) {
        frame.pixels.forEachIndexed { index, value ->
            val delta = value - baseline[index]
            if (!positiveMask[index] && !negativeMask[index]) {
                noise[index] += (abs(delta) - noise[index]) * config.noiseAlpha
                baseline[index] += delta * config.baselineAlpha
            }
        }
    }
}
