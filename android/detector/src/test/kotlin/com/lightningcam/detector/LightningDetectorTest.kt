package com.lightningcam.detector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightningDetectorTest {
    private val config = DetectionConfig(
        warmupFrames = 3,
        globalMeanDelta = 24.0,
        globalChangedRatio = 0.20,
        localizedMeanDelta = 55.0,
        localizedChangedRatio = 0.015,
        pixelDelta = 45,
        refractoryMs = 500,
    )

    @Test
    fun `does not trigger while baseline warms up`() {
        val detector = LightningDetector(config)

        repeat(3) { index ->
            assertNull(detector.analyze(frame(fill = 20, timestampMs = index * 10L)).trigger)
        }
    }

    @Test
    fun `detects a broad flash`() {
        val detector = warmedDetector()

        val result = detector.analyze(frame(fill = 100, timestampMs = 100))

        assertEquals(TriggerType.GLOBAL, result.trigger)
        assertTrue(result.changedPixelRatio > 0.9)
    }

    @Test
    fun `detects a narrow bolt covering two percent of image`() {
        val detector = warmedDetector()
        val pixels = IntArray(1_000) { 20 }
        repeat(20) { pixels[it * 50] = 220 }

        val result = detector.analyze(LuminanceFrame(50, 20, pixels, 100))

        assertEquals(TriggerType.LOCALIZED, result.trigger)
        assertTrue(result.localizedScore >= config.localizedMeanDelta)
    }

    @Test
    fun `rejects one hot pixel`() {
        val detector = warmedDetector()
        val pixels = IntArray(1_000) { 20 }.also { it[500] = 255 }

        assertNull(detector.analyze(LuminanceFrame(50, 20, pixels, 100)).trigger)
    }

    @Test
    fun `rejects a stable bright highlight after baseline adapts`() {
        val detector = LightningDetector(config.copy(warmupFrames = 5))
        val pixels = IntArray(1_000) { 20 }.also { values -> repeat(30) { values[it] = 200 } }
        repeat(5) { detector.analyze(LuminanceFrame(50, 20, pixels, it * 10L)) }

        assertNull(detector.analyze(LuminanceFrame(50, 20, pixels, 100)).trigger)
    }

    @Test
    fun `suppresses triggers during refractory period`() {
        val detector = warmedDetector()

        assertEquals(TriggerType.GLOBAL, detector.analyze(frame(100, 100)).trigger)
        assertNull(detector.analyze(frame(20, 120)).trigger)
        assertNull(detector.analyze(frame(100, 200)).trigger)
        assertEquals(TriggerType.GLOBAL, detector.analyze(frame(100, 700)).trigger)
    }

    private fun warmedDetector(): LightningDetector = LightningDetector(config).also { detector ->
        repeat(3) { detector.analyze(frame(20, it * 10L)) }
    }

    private fun frame(fill: Int, timestampMs: Long) =
        LuminanceFrame(50, 20, IntArray(1_000) { fill }, timestampMs)
}
