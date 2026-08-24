package com.lightningcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraControlMapperTest {
    private val capabilities = CameraCapabilities(
        exposureCompensationRange = -4..4,
        supportsManualSensor = true,
        minimumFocusDistance = 10f,
        isoRange = 50..3200,
        exposureTimeNsRange = 100_000L..100_000_000L,
    )

    @Test
    fun `automatic defaults favor short lightning exposure`() {
        val settings = CameraSettings.automatic()
        assertFalse(settings.manualExposure)
        assertEquals(FocusMode.CONTINUOUS, settings.focusMode)
    }

    @Test
    fun `clamps compensation and exposes infinity focus`() {
        val mapped = CameraControlMapper.map(
            CameraSettings.automatic().copy(exposureCompensation = 20, focusMode = FocusMode.INFINITY),
            capabilities,
        )
        assertEquals(4, mapped.exposureCompensation)
        assertEquals(0f, mapped.focusDistance)
    }

    @Test
    fun `falls back from manual sensor when unsupported`() {
        val mapped = CameraControlMapper.map(
            CameraSettings(manualExposure = true, iso = 800, exposureTimeNs = 5_000_000),
            capabilities.copy(supportsManualSensor = false),
        )
        assertFalse(mapped.manualExposure)
        assertTrue(mapped.warning != null)
    }
}
