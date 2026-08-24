package com.lightningcam.camera

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingLifecycleTest {
    @Test
    fun `restarts after event finalization while session is open`() {
        val lifecycle = RecordingLifecycle()
        assertTrue(lifecycle.shouldRestartAfterFinalize())
    }

    @Test
    fun `does not restart after session closes`() {
        val lifecycle = RecordingLifecycle()
        lifecycle.close()
        assertFalse(lifecycle.shouldRestartAfterFinalize())
    }
}
