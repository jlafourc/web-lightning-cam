package com.lightningcam.detector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetectionConfigTest {
    @Test
    fun `profiles are ordered from strict to sensitive`() {
        val strict = DetectionConfig.forProfile(DetectionProfile.STRICT)
        val balanced = DetectionConfig.forProfile(DetectionProfile.BALANCED)
        val sensitive = DetectionConfig.forProfile(DetectionProfile.SENSITIVE)

        assertTrue(strict.pixelDelta > balanced.pixelDelta)
        assertTrue(balanced.pixelDelta > sensitive.pixelDelta)
        assertTrue(strict.sigmaThreshold > balanced.sigmaThreshold)
        assertTrue(balanced.sigmaThreshold > sensitive.sigmaThreshold)
        assertTrue(strict.minComponentPixels > balanced.minComponentPixels)
        assertTrue(balanced.minComponentPixels > sensitive.minComponentPixels)
    }

    @Test
    fun `balanced is the default runtime profile`() {
        assertEquals(
            DetectionConfig.forProfile(DetectionProfile.BALANCED),
            DetectionConfig.runtimeDefault(),
        )
    }
}
