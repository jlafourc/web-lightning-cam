package com.lightningcam

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildSmokeTest {
    @Test
    fun `initial app state has product name`() {
        assertEquals("Lightning Cam", AppState.initial().productName)
    }
}
