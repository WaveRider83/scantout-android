package com.ric.scantoutv5

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var menuOpen by remember { mutableStateOf(false) }
    var barcodesOn by remember { mutableStateOf(Settings.barcodesEnabled(context)) }
    var confirmClear by remember { mutableStateOf(false) }
    var lastCaptured by remember { mutableStateOf<File?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val cam = camera ?: return@detectTransformGestures
                    val maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 5f
                    val newZoom = (zoomRatio * zoomChange).coerceIn(1f, maxZoom)
                    zoomRatio = newZoom
                    cam.cameraControl.setZoomRatio(newZoom)
                }
            }
    ) {
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
                        camera = provider.bindToLifecycle(
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

        // Flash (haut droite, sous la status bar)
        IconButton(
            onClick = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture.flashMode = flashMode
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                    else -> Icons.Default.FlashOff
                },
                contentDescription = "Flash",
                tint = Color(0xFFFFEB3B)
            )
        }

        // Vignette dernière capture
        lastCaptured?.let { file ->
            val bmp = remember(file) { CapturesStore.decodeBitmap(file)?.asImageBitmap() }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp,
                    contentDescription = "Dernière capture",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp)
                        .size(64.dp)
                )
            }
        }

        // Barre de menu bas : Historique | Capture | options (engrenage)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC05070F))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenCaptures) {
                Icon(Icons.Default.History, contentDescription = "Historique", tint = Color(0xFF00E5FF))
            }
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
                modifier = Modifier.size(64.dp)
            ) { Text(if (capturing) "…" else "●", style = MaterialTheme.typography.headlineSmall) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Options", tint = Color(0xFF00E5FF))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        text = { Text(if (barcodesOn) "QR / codes-barres : ON" else "QR / codes-barres : OFF") },
                        onClick = {
                            barcodesOn = !barcodesOn
                            Settings.setBarcodesEnabled(context, barcodesOn)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Brightness6, contentDescription = null) },
                        text = { Text(if (Settings.darkTheme(context)) "Thème : sombre" else "Thème : clair") },
                        onClick = {
                            Settings.setDarkTheme(context, !Settings.darkTheme(context))
                            menuOpen = false
                            (context as? android.app.Activity)?.recreate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Effacer l'historique…") },
                        onClick = { menuOpen = false; confirmClear = true }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Effacer l'historique ?") },
            text = { Text("Toutes les captures seront supprimées définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    CapturesStore.deleteAll(context)
                    lastCaptured = null
                    confirmClear = false
                    Toast.makeText(context, "Historique effacé", Toast.LENGTH_SHORT).show()
                }) { Text("Effacer") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annuler") } }
        )
    }
}
