package com.lightningcam.camera

import com.lightningcam.detector.DetectionResult
import com.lightningcam.detector.LightningDetector

interface LuminanceInput {
    val width: Int
    val height: Int
    val rowStride: Int
    val pixelStride: Int
    val timestampMs: Long
    fun luminanceAt(byteIndex: Int): Int
    fun close()
}

data class AnalyzerDiagnostics(
    val frames: Long,
    val errors: Long,
    val lastLatencyMs: Double,
)

class LightningAnalyzer(
    private val detector: LightningDetector,
    private val onTrigger: (DetectionResult, com.lightningcam.detector.LuminanceFrame, Double) -> Unit = { _, _, _ -> },
) {
    private var frames = 0L
    private var errors = 0L
    private var lastLatencyMs = 0.0

    fun analyze(input: LuminanceInput) {
        val started = System.nanoTime()
        try {
            val targetWidth = minOf(64, input.width)
            val targetHeight = minOf(36, input.height)
            val frame = YPlaneSampler.sample(input, targetWidth, targetHeight)
            val result = detector.analyze(frame)
            if (result.trigger != null) {
                val photoFrame = YPlaneSampler.sample(input, minOf(1280, input.width), minOf(720, input.height))
                val latency = (System.nanoTime() - started) / 1_000_000.0
                onTrigger(result, photoFrame, latency)
            }
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
