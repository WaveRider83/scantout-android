package com.ric.scantoutv5

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(onOpenCaptures: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permission caméra requise")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Autoriser") }
            }
        }
        return
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(android.util.Size(1440, 1080))
            .build()
    }
    var lastCaptured by remember { mutableStateOf<File?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(Modifier.fillMaxSize().background(Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Caméra indisponible: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Vignette dernière capture
        lastCaptured?.let { file ->
            val bmp = remember(file) { CapturesStore.decodeBitmap(file)?.asImageBitmap() }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp,
                    contentDescription = "Dernière capture",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 120.dp)
                        .size(72.dp)
                )
            }
        }

        // Barre de menu bas (type v3) : Historique | Capture | (emplacement futur)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xCC05070F))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onOpenCaptures) { Text("Historique") }
            Button(
                onClick = {
                if (capturing) return@Button
                capturing = true
                val out = CapturesStore.newCaptureFile(context)
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(out).build(),
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            capturing = false
                            lastCaptured = out
                        }
                        override fun onError(exc: ImageCaptureException) {
                            capturing = false
                            ContextCompat.getMainExecutor(context).execute {
                                Toast.makeText(context, "Échec capture: ${exc.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            },
            enabled = !capturing,
            modifier = Modifier.size(72.dp)
        ) { Text(if (capturing) "…" else "●", style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.width(88.dp))
        }
    }
}

private val Black = androidx.compose.ui.graphics.Color.Black
