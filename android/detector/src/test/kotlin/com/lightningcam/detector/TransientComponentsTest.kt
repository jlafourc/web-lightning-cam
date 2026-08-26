package com.lightningcam.detector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransientComponentsTest {
    @Test
    fun `measures a connected vertical bolt`() {
        val mask = BooleanArray(8 * 8)
        repeat(6) { y -> mask[y * 8 + 3] = true }

        val component = TransientComponents.extract(8, 8, mask).single()

        assertEquals(6, component.area)
        assertEquals(6, component.span)
        assertTrue(component.elongation >= 6.0)
    }

    @Test
    fun `measures a compact illuminated cloud`() {
        val mask = BooleanArray(8 * 8)
        for (y in 2..4) for (x in 2..4) mask[y * 8 + x] = true

        val component = TransientComponents.extract(8, 8, mask).single()

        assertEquals(9, component.area)
        assertEquals(3, component.span)
        assertEquals(1.0, component.elongation)
    }

    @Test
    fun `keeps scattered reflections as isolated components`() {
        val mask = BooleanArray(8 * 8).also {
            it[0] = true
            it[7] = true
            it[7 * 8] = true
            it[7 * 8 + 7] = true
        }

        val components = TransientComponents.extract(8, 8, mask)

        assertEquals(4, components.size)
        assertTrue(components.all { it.area == 1 })
    }
}
