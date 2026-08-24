package com.lightningcam.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lightningcam.detector.DetectionConfig
import com.lightningcam.detector.LightningDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.lightningcam.detector.LuminanceFrame
import android.util.Size

class AndroidCameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onDetection: (com.lightningcam.detector.DetectionResult, Double) -> Unit,
    private val onStatus: (String) -> Unit,
) : CapturePort {
    private enum class StopReason { ROTATION, EVENT, CLOSE }
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val recordingLifecycle = RecordingLifecycle()
    private val handler = Handler(Looper.getMainLooper())
    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.FHD))
        .build()
    private val videoCapture = VideoCapture.withOutput(recorder)
    private var recording: Recording? = null
    private var videoAvailable = true
    private var completion: ((CaptureOutcome?) -> Unit)? = null
    private var pendingPhotoUri: String? = null
    private var pendingVideoUri: String? = null
    private var videoFinalized = false
    private var lastRotatedVideoUri: String? = null
    private var boundaryTriggerVideoUri: String? = null
    private var stopReason: StopReason? = null
    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (completion == null && stopReason == null) {
                stopReason = StopReason.ROTATION
                recording?.stop()
            } else {
                handler.postDelayed(this, 1_000)
            }
        }
    }
    @Volatile private var triggerFrame: LuminanceFrame? = null
    private val analyzer = LightningAnalyzer(LightningDetector(DetectionConfig())) { result, frame, latency ->
        triggerFrame = frame
        mainExecutor.execute { onDetection(result, latency) }
    }

    fun bind() {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build(),
                    )
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analysisExecutor) { image ->
                            analyzer.analyze(ImageProxyInput(image))
                        }
                    }
                provider.unbindAll()
                val camera = try {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                        videoCapture,
                    )
                } catch (_: IllegalArgumentException) {
                    videoAvailable = false
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
                val range = camera.cameraInfo.exposureState.exposureCompensationRange
                if (range.contains(-1)) camera.cameraControl.setExposureCompensationIndex(-1)
                if (videoAvailable && recordingLifecycle.shouldRestartAfterFinalize()) startRecording()
                onStatus(if (videoAvailable) "Armé · analyse native active" else "Armé · vidéo indisponible")
            } catch (error: Exception) {
                onStatus("Caméra indisponible : ${error.message ?: error.javaClass.simpleName}")
            }
        }, mainExecutor)
    }

    override fun capture(completion: (CaptureOutcome?) -> Unit) {
        if (this.completion != null) return
        this.completion = completion
        handler.removeCallbacks(rotationRunnable)
        pendingPhotoUri = null
        pendingVideoUri = null
        boundaryTriggerVideoUri = null
        videoFinalized = !videoAvailable
        val frame = triggerFrame
        triggerFrame = null
        if (frame == null) {
            completion(null)
            this.completion = null
            return
        }
        analysisExecutor.execute { saveTriggerFrame(frame) }
        if (videoAvailable && stopReason != StopReason.ROTATION) scheduleEventStop()
        onStatus("Éclair détecté · finalisation du clip…")
    }

    private fun startRecording() {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName("VID", "mp4"))
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LightningCam")
        }
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(values).build()
        recording = recorder.prepareRecording(context, options).start(mainExecutor) { event ->
            if (event is VideoRecordEvent.Finalize) {
                val finalizedReason = stopReason
                stopReason = null
                recording = null
                if (event.hasError()) {
                    videoAvailable = false
                    if (completion != null) {
                        pendingVideoUri = null
                        videoFinalized = true
                        finishIfReady()
                    }
                    onStatus("Vidéo désactivée après erreur · photos actives")
                    return@start
                }
                val finalizedUri = event.outputResults.outputUri.toString()
                if (finalizedReason == StopReason.CLOSE || !recordingLifecycle.shouldRestartAfterFinalize()) {
                    context.contentResolver.delete(android.net.Uri.parse(finalizedUri), null, null)
                    return@start
                }
                if (finalizedReason == StopReason.EVENT) {
                    val selection = EventVideoSelector.select(boundaryTriggerVideoUri, finalizedUri)
                    selection.discardUri?.let { context.contentResolver.delete(android.net.Uri.parse(it), null, null) }
                    pendingVideoUri = selection.retainedUri
                    boundaryTriggerVideoUri = null
                    videoFinalized = true
                    lastRotatedVideoUri?.let { context.contentResolver.delete(android.net.Uri.parse(it), null, null) }
                    lastRotatedVideoUri = null
                    finishIfReady()
                } else if (finalizedReason == StopReason.ROTATION && completion != null) {
                    boundaryTriggerVideoUri = finalizedUri
                } else {
                    lastRotatedVideoUri?.let { context.contentResolver.delete(android.net.Uri.parse(it), null, null) }
                    lastRotatedVideoUri = finalizedUri
                }
                if (recordingLifecycle.shouldRestartAfterFinalize()) startRecording()
                if (completion != null && finalizedReason == StopReason.ROTATION) scheduleEventStop()
            }
        }
        handler.removeCallbacks(rotationRunnable)
        handler.postDelayed(rotationRunnable, 4_000)
    }

    private fun scheduleEventStop() {
        handler.postDelayed({
            if (stopReason == null) {
                stopReason = StopReason.EVENT
                recording?.stop()
            }
        }, 1_200)
    }

    private fun finishIfReady() {
        val photo = pendingPhotoUri ?: return
        if (videoFinalized) {
            completion?.invoke(CaptureOutcome(photo, pendingVideoUri))
            completion = null
            onStatus("Capture enregistrée")
        }
    }

    private fun saveTriggerFrame(frame: LuminanceFrame) {
        var insertedUri: android.net.Uri? = null
        var bitmap: Bitmap? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName("IMG", "jpg"))
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LightningCam")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore image insertion failed")
            insertedUri = uri
            val argb = IntArray(frame.pixels.size) { index ->
                val y = frame.pixels[index]
                0xff000000.toInt() or (y shl 16) or (y shl 8) or y
            }
            val createdBitmap = Bitmap.createBitmap(argb, frame.width, frame.height, Bitmap.Config.ARGB_8888)
            bitmap = createdBitmap
            context.contentResolver.openOutputStream(uri)?.use { output ->
                check(createdBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
            } ?: error("MediaStore image stream unavailable")
            mainExecutor.execute {
                pendingPhotoUri = uri.toString()
                finishIfReady()
            }
        } catch (error: Exception) {
            insertedUri?.let { context.contentResolver.delete(it, null, null) }
            mainExecutor.execute {
                onStatus("Échec photo : ${error.message}")
                completion?.invoke(null)
                completion = null
            }
        } finally {
            bitmap?.recycle()
        }
    }

    fun close() {
        recordingLifecycle.close()
        handler.removeCallbacksAndMessages(null)
        stopReason = StopReason.CLOSE
        recording?.stop()
        lastRotatedVideoUri?.let { context.contentResolver.delete(android.net.Uri.parse(it), null, null) }
        boundaryTriggerVideoUri?.let { context.contentResolver.delete(android.net.Uri.parse(it), null, null) }
        analysisExecutor.shutdown()
    }

    private fun fileName(prefix: String, extension: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "${prefix}_${stamp}.$extension"
    }
}
