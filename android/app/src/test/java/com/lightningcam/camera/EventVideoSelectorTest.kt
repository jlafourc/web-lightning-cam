package com.lightningcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals

class EventVideoSelectorTest {
    @Test
    fun `keeps rotation segment when trigger arrived during rotation`() {
        val selected = EventVideoSelector.select(boundarySegment = "contains-bolt", postSegment = "aftermath")
        assertEquals(EventVideoSelection("contains-bolt", "aftermath"), selected)
    }

    @Test
    fun `uses post-trigger segment for normal event stop`() {
        val selected = EventVideoSelector.select(boundarySegment = null, postSegment = "event")
        assertEquals(EventVideoSelection("event", null), selected)
    }
}
