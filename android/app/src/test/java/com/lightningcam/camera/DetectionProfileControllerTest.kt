package com.lightningcam.camera

import com.lightningcam.detector.DetectionProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class DetectionProfileControllerTest {
    @Test
    fun `cycles balanced sensitive strict and back to balanced`() {
        val controller = DetectionProfileController()

        assertEquals(DetectionProfile.BALANCED, controller.current)
        assertEquals(DetectionProfile.SENSITIVE, controller.next())
        assertEquals(DetectionProfile.STRICT, controller.next())
        assertEquals(DetectionProfile.BALANCED, controller.next())
    }
}
