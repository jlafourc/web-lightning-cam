package com.lightningcam.camera

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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

class AndroidCameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onDetection: (com.lightningcam.detector.DetectionResult, Double) -> Unit,
    private val onStatus: (String) -> Unit,
) : CapturePort {
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.FHD))
        .build()
    private val videoCapture = VideoCapture.withOutput(recorder)
    private var recording: Recording? = null
    private var completion: ((CaptureOutcome) -> Unit)? = null
    private var pendingPhotoUri: String? = null
    private var pendingVideoUri: String? = null
    private var videoFinalized = false
    private val analyzer = LightningAnalyzer(LightningDetector(DetectionConfig())) { result ->
        if (result.trigger != null) {
            mainExecutor.execute { onDetection(result, 0.0) }
        }
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
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analysisExecutor) { image ->
                            analyzer.analyze(ImageProxyInput(image))
                        }
                    }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                    imageCapture,
                    videoCapture,
                )
                startRecording()
                onStatus("Armé · analyse native active")
            } catch (error: Exception) {
                onStatus("Caméra indisponible : ${error.message ?: error.javaClass.simpleName}")
            }
        }, mainExecutor)
    }

    override fun capture(completion: (CaptureOutcome) -> Unit) {
        if (this.completion != null) return
        this.completion = completion
        pendingPhotoUri = null
        pendingVideoUri = null
        videoFinalized = false
        val name = fileName("IMG", "jpg")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LightningCam")
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
        imageCapture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                pendingPhotoUri = result.savedUri?.toString()
                finishIfReady()
            }

            override fun onError(exception: ImageCaptureException) {
                onStatus("Échec photo : ${exception.message}")
                this@AndroidCameraSession.completion = null
            }
        })
        handler.postDelayed({ recording?.stop() }, 1_200)
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
                recording = null
                pendingVideoUri = if (!event.hasError()) event.outputResults.outputUri.toString() else null
                videoFinalized = true
                finishIfReady()
                startRecording()
            }
        }
    }

    private fun finishIfReady() {
        val photo = pendingPhotoUri ?: return
        if (videoFinalized) {
            completion?.invoke(CaptureOutcome(photo, pendingVideoUri))
            completion = null
            onStatus("Capture enregistrée")
        }
    }

    fun close() {
        recording?.stop()
        analysisExecutor.shutdown()
    }

    private fun fileName(prefix: String, extension: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "${prefix}_${stamp}.$extension"
    }
}
