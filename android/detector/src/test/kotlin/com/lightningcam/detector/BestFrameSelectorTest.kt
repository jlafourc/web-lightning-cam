package com.lightningcam.detector

import kotlin.test.Test
import kotlin.test.assertEquals

class BestFrameSelectorTest {
    @Test
    fun `selects sharp high information frame`() {
        val candidates = listOf(
            FrameCandidate("before", sharpness = 20.0, information = 5.0, saturationRatio = 0.0),
            FrameCandidate("bolt", sharpness = 70.0, information = 90.0, saturationRatio = 0.03),
            FrameCandidate("after", sharpness = 35.0, information = 25.0, saturationRatio = 0.0),
        )

        assertEquals("bolt", BestFrameSelector.select(candidates)?.value)
    }

    @Test
    fun `rejects heavily saturated frame`() {
        val candidates = listOf(
            FrameCandidate("detail", sharpness = 60.0, information = 75.0, saturationRatio = 0.05),
            FrameCandidate("white", sharpness = 100.0, information = 100.0, saturationRatio = 0.80),
        )

        assertEquals("detail", BestFrameSelector.select(candidates)?.value)
    }

    @Test
    fun `returns null for no candidates`() {
        assertEquals(null, BestFrameSelector.select<String>(emptyList()))
    }
}
