package com.yousefsaid04.aslcommunicator.ui.features.speech_to_text

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SpeechToTextScreen(viewModel: SpeechToTextViewModel = viewModel()) {
    val recognizedText by viewModel.recognizedText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    var hasAudioPermission by remember { mutableStateOf(false) }

    // Create a permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            if (isGranted) {
                viewModel.startListening()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Pulsing animation for the microphone icon when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.onSurface,
        targetValue = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasAudioPermission) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening..." else "Not listening",
                modifier = Modifier.size(64.dp).scale(scale),
                tint = color
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isListening) "Listening..." else "Microphone is idle",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = recognizedText.ifEmpty { "Say something to begin." },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Audio permission is required to use this feature. Please grant the permission.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}