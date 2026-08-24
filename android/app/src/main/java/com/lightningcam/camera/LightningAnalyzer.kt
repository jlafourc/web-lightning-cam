package com.lightningcam.camera

import com.lightningcam.detector.DetectionResult
import com.lightningcam.detector.LightningDetector

interface LuminanceInput {
    val bytes: ByteArray
    val width: Int
    val height: Int
    val rowStride: Int
    val pixelStride: Int
    val timestampMs: Long
    fun close()
}

data class AnalyzerDiagnostics(
    val frames: Long,
    val errors: Long,
    val lastLatencyMs: Double,
)

class LightningAnalyzer(
    private val detector: LightningDetector,
    private val onResult: (DetectionResult) -> Unit = {},
) {
    private var frames = 0L
    private var errors = 0L
    private var lastLatencyMs = 0.0

    fun analyze(input: LuminanceInput) {
        val started = System.nanoTime()
        try {
            val targetWidth = minOf(64, input.width)
            val targetHeight = minOf(36, input.height)
            val frame = YPlaneSampler.sample(
                input.bytes,
                input.width,
                input.height,
                input.rowStride,
                input.pixelStride,
                targetWidth,
                targetHeight,
                input.timestampMs,
            )
            onResult(detector.analyze(frame))
            frames++
        } catch (_: RuntimeException) {
            errors++
        } finally {
            lastLatencyMs = (System.nanoTime() - started) / 1_000_000.0
            input.close()
        }
    }

    fun diagnostics() = AnalyzerDiagnostics(frames, errors, lastLatencyMs)
}
