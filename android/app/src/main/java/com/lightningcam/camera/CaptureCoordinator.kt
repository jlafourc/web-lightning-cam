package com.lightningcam.camera

import com.lightningcam.detector.DetectionResult
import com.lightningcam.storage.EventRepository
import com.lightningcam.storage.LightningEvent

data class CaptureOutcome(val photoUri: String, val videoUri: String?)

fun interface CapturePort {
    fun capture(completion: (CaptureOutcome) -> Unit)
}

class CaptureCoordinator(
    private val capturePort: CapturePort,
    private val repository: EventRepository,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private var capturing = false

    fun onDetection(result: DetectionResult, latencyMs: Double) {
        val trigger = result.trigger ?: return
        if (capturing) return
        capturing = true
        val timestamp = clockMs()
        capturePort.capture { outcome ->
            repository.save(
                LightningEvent(
                    id = timestamp,
                    timestampMs = timestamp,
                    triggerType = trigger,
                    globalScore = result.globalScore,
                    localizedScore = result.localizedScore,
                    detectionLatencyMs = latencyMs,
                    photoUri = outcome.photoUri,
                    videoUri = outcome.videoUri,
                ),
            )
            capturing = false
        }
    }
}
