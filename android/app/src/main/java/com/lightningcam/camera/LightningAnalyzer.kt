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
    val globalScore: Double,
    val localizedScore: Double,
    val changedPixelRatio: Double,
)

class LightningAnalyzer(
    private var detector: LightningDetector,
    private val diagnosticsEveryFrames: Long = 15,
    private val onDiagnostics: (AnalyzerDiagnostics) -> Unit = {},
    private val onTrigger: (DetectionResult, com.lightningcam.detector.LuminanceFrame, Double) -> Unit = { _, _, _ -> },
) {
    private var frames = 0L
    private var errors = 0L
    private var lastLatencyMs = 0.0
    private var globalScore = 0.0
    private var localizedScore = 0.0
    private var changedPixelRatio = 0.0

    init {
        require(diagnosticsEveryFrames > 0)
    }

    fun replaceDetector(detector: LightningDetector) {
        this.detector = detector
        globalScore = 0.0
        localizedScore = 0.0
        changedPixelRatio = 0.0
    }

    fun analyze(input: LuminanceInput) {
        val started = System.nanoTime()
        try {
            val targetWidth = minOf(64, input.width)
            val targetHeight = minOf(36, input.height)
            val frame = YPlaneSampler.sample(input, targetWidth, targetHeight)
            val result = detector.analyze(frame)
            globalScore = result.globalScore
            localizedScore = result.localizedScore
            changedPixelRatio = result.changedPixelRatio
            if (result.trigger != null) {
                val photoFrame = YPlaneSampler.sample(input, minOf(1280, input.width), minOf(720, input.height))
                val latency = (System.nanoTime() - started) / 1_000_000.0
                onTrigger(result, photoFrame, latency)
            }
            frames++
            lastLatencyMs = (System.nanoTime() - started) / 1_000_000.0
            if (frames % diagnosticsEveryFrames == 0L) onDiagnostics(diagnostics())
        } catch (_: RuntimeException) {
            errors++
        } finally {
            lastLatencyMs = (System.nanoTime() - started) / 1_000_000.0
            input.close()
        }
    }

    fun diagnostics() = AnalyzerDiagnostics(
        frames = frames,
        errors = errors,
        lastLatencyMs = lastLatencyMs,
        globalScore = globalScore,
        localizedScore = localizedScore,
        changedPixelRatio = changedPixelRatio,
    )
}
