package com.lightningcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals

class YPlaneSamplerTest {
    @Test
    fun `samples luminance while respecting row and pixel stride`() {
        val bytes = byteArrayOf(
            10, 0, 20, 0, 30, 0, 99, 99,
            40, 0, 50, 0, 60, 0, 99, 99,
        )

        val sampled = YPlaneSampler.sample(
            bytes = bytes,
            sourceWidth = 3,
            sourceHeight = 2,
            rowStride = 8,
            pixelStride = 2,
            targetWidth = 3,
            targetHeight = 2,
            timestampMs = 7,
        )

        assertEquals(listOf(10, 20, 30, 40, 50, 60), sampled.pixels.toList())
    }

    @Test
    fun `downsamples to requested grid`() {
        val bytes = ByteArray(16) { it.toByte() }

        val sampled = YPlaneSampler.sample(bytes, 4, 4, 4, 1, 2, 2, 1)

        assertEquals(listOf(0, 2, 8, 10), sampled.pixels.toList())
    }
}
