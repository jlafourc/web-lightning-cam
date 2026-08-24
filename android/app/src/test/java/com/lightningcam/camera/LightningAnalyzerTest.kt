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

    private class TestFrameInput(
        override val bytes: ByteArray,
        private val closeAction: () -> Unit,
    ) : LuminanceInput {
        override val width = 2
        override val height = 2
        override val rowStride = 2
        override val pixelStride = 1
        override val timestampMs = 1L
        override fun close() = closeAction()
    }
}
