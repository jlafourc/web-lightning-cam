package com.lightningcam.detector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameRingBufferTest {
    @Test
    fun `evicts oldest frame when capacity is reached`() {
        val buffer = FrameRingBuffer<Int>(capacity = 3)

        (1..4).forEach { buffer.add(it.toLong(), it) }

        assertEquals(listOf(2, 3, 4), buffer.snapshot().map { it.value })
    }

    @Test
    fun `returns entries in timestamp order`() {
        val buffer = FrameRingBuffer<String>(capacity = 3)
        buffer.add(30, "late")
        buffer.add(10, "early")
        buffer.add(20, "middle")

        assertEquals(listOf("early", "middle", "late"), buffer.snapshot().map { it.value })
    }

    @Test
    fun `clear removes every entry`() {
        val buffer = FrameRingBuffer<Int>(capacity = 2)
        buffer.add(1, 1)
        buffer.clear()

        assertTrue(buffer.snapshot().isEmpty())
    }
}
