package com.lightningcam.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lightningcam.camera.AndroidCameraSession
import com.lightningcam.camera.AnalyzerDiagnostics
import com.lightningcam.camera.CaptureCoordinator
import com.lightningcam.camera.CapturePort
import com.lightningcam.storage.InMemoryEventRepository
import java.util.Locale

@Composable
fun LightningCamApp() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xff7dd3fc))) {
        val context = LocalContext.current
        var permissionGranted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }
        var armed by remember { mutableStateOf(true) }
        var status by remember { mutableStateOf("Initialisation caméra…") }
        var diagnostics by remember {
            mutableStateOf(AnalyzerDiagnostics(0, 0, 0.0, 0.0, 0.0, 0.0))
        }
        val armedNow by rememberUpdatedState(armed)
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted = it
            status = if (it) "Initialisation caméra…" else "Autorisation caméra refusée"
        }

        if (!permissionGranted) {
            PermissionScreen { permissionLauncher.launch(Manifest.permission.CAMERA) }
        } else {
            val lifecycleOwner = LocalLifecycleOwner.current
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
            val repository = remember { InMemoryEventRepository() }
            DisposableEffect(previewView, lifecycleOwner) {
                lateinit var session: AndroidCameraSession
                val coordinator = CaptureCoordinator(
                    capturePort = CapturePort { completion -> session.capture(completion) },
                    repository = repository,
                )
                session = AndroidCameraSession(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onDetection = { result, latency -> if (armedNow) coordinator.onDetection(result, latency) },
                    onDiagnostics = { diagnostics = it },
                    onStatus = { status = it },
                )
                session.bind()
                onDispose { session.close() }
            }

            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color(0xaa020617))
                        .padding(16.dp),
                ) {
                    Text("LIGHTNING CAM · NATIVE", style = MaterialTheme.typography.titleMedium)
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(
                            Locale.US,
                            "Terrain · %,d images · %.1f ms",
                            diagnostics.frames,
                            diagnostics.lastLatencyMs,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        String.format(
                            Locale.US,
                            "G %.1f/12 · L %.1f/30 · Δ %.2f%%",
                            diagnostics.globalScore,
                            diagnostics.localizedScore,
                            diagnostics.changedPixelRatio * 100.0,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xcc020617), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(if (armed) "ARMÉ" else "PAUSE", color = if (armed) Color(0xff86efac) else Color.LightGray)
                        Text("Photos : Pictures/LightningCam", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = { armed = !armed }) {
                        Text(if (armed) "Désarmer" else "Armer")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Lightning Cam utilise la caméra pour détecter les éclairs en temps réel.")
        Button(onClick = onRequest, modifier = Modifier.padding(top = 20.dp)) { Text("Autoriser la caméra") }
    }
}
