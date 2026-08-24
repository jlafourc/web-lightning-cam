package com.lightningcam.camera

import com.lightningcam.detector.DetectionConfig
import com.lightningcam.detector.LightningDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LightningAnalyzerTest {
    @Test
    fun `always closes frame when sampling fails`() {
        var closed = false
        val analyzer = LightningAnalyzer(LightningDetector(DetectionConfig(warmupFrames = 1)))
        val input = TestFrameInput(bytes = byteArrayOf(), closeAction = { closed = true })

        analyzer.analyze(input)

        assertTrue(closed)
        assertEquals(1, analyzer.diagnostics().errors)
    }

    @Test
    fun `returns exact trigger frame and measured latency`() {
        var triggerWidth = 0
        var latency = -1.0
        val detector = LightningDetector(
            DetectionConfig(
                warmupFrames = 1,
                globalMeanDelta = 20.0,
                globalChangedRatio = 0.2,
            ),
        )
        val analyzer = LightningAnalyzer(detector) { _, frame, measuredLatency ->
            triggerWidth = frame.width
            latency = measuredLatency
        }
        analyzer.analyze(TestFrameInput(ByteArray(16) { 10 }, {}))
        analyzer.analyze(TestFrameInput(ByteArray(16) { 100 }, {}))

        assertEquals(4, triggerWidth)
        assertTrue(latency >= 0.0)
    }

    @Test
    fun `publishes periodic diagnostics with detector scores`() {
        var reported: AnalyzerDiagnostics? = null
        val detector = LightningDetector(
            DetectionConfig(
                warmupFrames = 1,
                globalMeanDelta = 20.0,
                globalChangedRatio = 0.2,
            ),
        )
        val analyzer = LightningAnalyzer(
            detector = detector,
            diagnosticsEveryFrames = 2,
            onDiagnostics = { reported = it },
        )

        analyzer.analyze(TestFrameInput(ByteArray(16) { 10 }, {}))
        analyzer.analyze(TestFrameInput(ByteArray(16) { 100.toByte() }, {}))

        val diagnostics = requireNotNull(reported)
        assertEquals(2, diagnostics.frames)
        assertTrue(diagnostics.globalScore > 0.0)
        assertTrue(diagnostics.localizedScore > 0.0)
        assertTrue(diagnostics.changedPixelRatio > 0.0)
    }

    private class TestFrameInput(
        private val bytes: ByteArray,
        private val closeAction: () -> Unit,
    ) : LuminanceInput {
        override val width = if (bytes.isEmpty()) 2 else 4
        override val height = if (bytes.isEmpty()) 2 else 4
        override val rowStride = if (bytes.isEmpty()) 2 else 4
        override val pixelStride = 1
        override val timestampMs = 1L
        override fun luminanceAt(byteIndex: Int): Int = bytes[byteIndex].toInt() and 0xff
        override fun close() = closeAction()
    }
}
