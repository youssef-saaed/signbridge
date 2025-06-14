package com.yousefsaid04.aslcommunicator.ui.features.sign_to_text

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraView(
    onFrame: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)

                    ProcessCameraProvider.getInstance(ctx).addListener({
                        val provider = ProcessCameraProvider.getInstance(ctx).get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build() // We don't need to set the format, the default is fine

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            // THE FIX IS APPLIED HERE
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            val bitmap = imageProxy.toBitmap()

                            if (bitmap != null) {
                                // Rotate and flip the bitmap to match the Python environment
                                val correctedBitmap = bitmap.rotateAndFlip(rotationDegrees.toFloat())
                                onFrame(correctedBitmap)
                            }
                            imageProxy.close()
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraView", "Use case binding failed", e)
                        }

                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Helper function to rotate and flip a bitmap to match the environment
 * of a typical Python/OpenCV setup for selfie cameras.
 */
private fun Bitmap.rotateAndFlip(degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        // Rotate the image to be upright
        postRotate(degrees)
        // Flip the image horizontally for a mirror effect
        postScale(-1f, 1f, width / 2f, height / 2f)
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}