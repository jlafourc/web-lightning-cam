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

        assertEquals(listOf(4, 6, 12, 14), sampled.pixels.toList())
    }

    @Test
    fun `keeps a thin two-pixel bolt between old point samples`() {
        val bytes = ByteArray(64) { 10 }
        bytes[2 * 8 + 3] = 240.toByte()
        bytes[3 * 8 + 3] = 240.toByte()

        val sampled = YPlaneSampler.sample(bytes, 8, 8, 8, 1, 2, 2, 1)

        assertEquals(240, sampled.pixels.max())
    }

    @Test
    fun `rejects a lone hot pixel inside a tile`() {
        val bytes = ByteArray(16) { 10 }
        bytes[5] = 255.toByte()

        val sampled = YPlaneSampler.sample(bytes, 4, 4, 4, 1, 1, 1, 1)

        assertEquals(10, sampled.pixels.single())
    }
}
