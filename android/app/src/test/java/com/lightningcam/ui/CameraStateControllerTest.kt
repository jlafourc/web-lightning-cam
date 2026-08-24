package com.lightningcam.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraStateControllerTest {
    @Test
    fun `moves from permission request to armed state`() {
        val controller = CameraStateController()
        assertTrue(controller.state.needsPermission)

        controller.onPermission(true)
        controller.onStatus("Armé · analyse native active")

        assertFalse(controller.state.needsPermission)
        assertTrue(controller.state.armed)
    }

    @Test
    fun `can disarm and reports recoverable error`() {
        val controller = CameraStateController()
        controller.onPermission(true)
        controller.toggleArmed()
        controller.onStatus("Caméra indisponible")

        assertFalse(controller.state.armed)
        assertEquals("Caméra indisponible", controller.state.status)
    }
}
