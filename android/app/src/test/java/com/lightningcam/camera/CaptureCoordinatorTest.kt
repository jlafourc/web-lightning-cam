package com.lightningcam.camera

import com.lightningcam.detector.DetectionResult
import com.lightningcam.detector.TriggerType
import com.lightningcam.storage.InMemoryEventRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureCoordinatorTest {
    @Test
    fun `captures once while an event is already in progress`() {
        val capture = FakeCapturePort()
        val repository = InMemoryEventRepository()
        val coordinator = CaptureCoordinator(capture, repository) { 10L }
        val trigger = DetectionResult(TriggerType.LOCALIZED, 4.0, 80.0, 0.02, true)

        coordinator.onDetection(trigger, 3.0)
        coordinator.onDetection(trigger, 3.0)

        assertEquals(1, capture.requests)
    }

    @Test
    fun `preserves photo event when video fails`() {
        val capture = FakeCapturePort()
        val repository = InMemoryEventRepository()
        val coordinator = CaptureCoordinator(capture, repository) { 10L }

        coordinator.onDetection(DetectionResult(TriggerType.GLOBAL, 50.0, 60.0, 0.8, true), 2.0)
        capture.complete(CaptureOutcome("content://photo", null))

        assertEquals("content://photo", repository.events().single().photoUri)
        assertEquals(null, repository.events().single().videoUri)
    }

    @Test
    fun `allows a later capture after capture failure`() {
        val capture = FakeCapturePort()
        val coordinator = CaptureCoordinator(capture, InMemoryEventRepository()) { 10L }
        val trigger = DetectionResult(TriggerType.GLOBAL, 50.0, 60.0, 0.8, true)
        coordinator.onDetection(trigger, 2.0)
        capture.complete(null)

        coordinator.onDetection(trigger, 2.0)

        assertEquals(2, capture.requests)
    }

    private class FakeCapturePort : CapturePort {
        var requests = 0
        private var completion: ((CaptureOutcome?) -> Unit)? = null
        override fun capture(completion: (CaptureOutcome?) -> Unit) {
            requests++
            this.completion = completion
        }
        fun complete(outcome: CaptureOutcome?) = completion?.invoke(outcome) ?: Unit
    }
}
