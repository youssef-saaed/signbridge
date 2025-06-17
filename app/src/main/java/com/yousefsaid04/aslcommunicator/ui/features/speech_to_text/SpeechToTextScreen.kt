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
import com.yousefsaid04.aslcommunicator.viewmodel.AppViewModel

@Composable
fun SpeechToTextScreen(appViewModel: AppViewModel) {
    val recognizedText by appViewModel.recognizedText.collectAsState()
    val isListening by appViewModel.isListening.collectAsState()
    var hasAudioPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            if (isGranted) {
                appViewModel.startListening()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "mic_scale"
    )
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.onSurfaceVariant,
        targetValue = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "mic_color"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasAudioPermission) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening..." else "Microphone Idle",
                modifier = Modifier.size(80.dp).scale(scale),
                tint = color
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                // THE FIX: Simplified text to reflect the always-on state
                text = if (isListening) "Listening..." else "Microphone Idle",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = recognizedText.ifEmpty { "Waiting for speech..." },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Audio permission is required for this feature.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}