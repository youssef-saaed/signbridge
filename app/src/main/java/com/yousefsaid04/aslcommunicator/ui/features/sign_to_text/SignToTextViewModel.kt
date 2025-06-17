package com.yousefsaid04.aslcommunicator.ui.features.sign_to_text

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.yousefsaid04.aslcommunicator.services.SignLanguageApiService
import com.yousefsaid04.aslcommunicator.services.TextToSpeechService
import com.yousefsaid04.aslcommunicator.utils.HandLandmarkerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignToTextViewModel(application: Application) : AndroidViewModel(application), HandLandmarkerHelper.LandmarkerListener {

    // --- State Flows for UI
    private val _predictedText = MutableStateFlow("")
    val predictedText: StateFlow<String> = _predictedText

    private val _currentLetter = MutableStateFlow("")
    val currentLetter: StateFlow<String> = _currentLetter

    // --- Services and Helpers
    private val apiService = SignLanguageApiService()
    private val handLandmarkerHelper = HandLandmarkerHelper(application.applicationContext, this)
    private val textToSpeechService = TextToSpeechService(application.applicationContext) {
        Log.i("TTS", "TTS Initialized: $it")
    }

    // --- Prediction Logic
    private var frameCounter = 0
    private val predictionBuffer = mutableListOf<String>()
    private var predictionJob: Job? = null
    private var lastHandSeenTime: Long = 0

    fun onFrame(bitmap: Bitmap) {
        frameCounter++
        if (frameCounter % 5 == 0) { // Process every 5th frame
            handLandmarkerHelper.detectLiveStream(bitmap)
        }

        if (lastHandSeenTime > 0 && System.currentTimeMillis() - lastHandSeenTime > 2000) { // Also updated pause check to 3s
            resetPredictionCycle()
            lastHandSeenTime = 0
        }
    }

    override fun onResults(result: HandLandmarkerResult) {
        lastHandSeenTime = System.currentTimeMillis()

        if (result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0]

            val flattenedLandmarks = landmarks.flatMap {
                listOf(it.x(), it.y(), it.z())
            }

            Log.d("LandmarkCheck", "Sending ${flattenedLandmarks.size} landmarks: $flattenedLandmarks")

            viewModelScope.launch {
                apiService.predict(flattenedLandmarks)?.let {
                    predictionBuffer.add(it.prediction)
                }
            }

            if (predictionJob == null || predictionJob?.isCompleted == true) {
                startPredictionJob()
            }
        }
    }

    override fun onError(error: String) {
        Log.e("SignToTextViewModel", "MediaPipe Error: $error")
        resetPredictionCycle()
    }

    private fun startPredictionJob() {
        predictionJob = viewModelScope.launch {
            // Reverted back to 2000ms (2 seconds) as requested
            delay(2000)
            processPredictionBuffer()
            resetPredictionCycle()
        }
    }

    private fun processPredictionBuffer() {
        if (predictionBuffer.isNotEmpty()) {
            val mostFrequent = predictionBuffer.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            mostFrequent?.let { handlePrediction(it) }
        }
    }

    private fun handlePrediction(label: String) {
        _currentLetter.value = label
        when (label) {
            "space" -> {
                textToSpeechService.speak(_predictedText.value.substringAfterLast(" "))
                _predictedText.value += " "
            }
            "del" -> {
//                if (_predictedText.value.isNotEmpty()) {
//                    _predictedText.value = _predictedText.value.dropLast(1)
//                }
                textToSpeechService.speak(_predictedText.value.substringAfterLast(" "))
                _predictedText.value += " "
            }
            "nothing" -> { /* Do nothing */ }
            else -> {
                _predictedText.value += label
            }
        }
    }

    private fun resetPredictionCycle() {
        predictionJob?.cancel()
        predictionJob = null
        predictionBuffer.clear()
        _currentLetter.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechService.shutdown()
        handLandmarkerHelper.clear()
    }

    fun onResetClicked() {
        _predictedText.value = ""
        _currentLetter.value = ""
        resetPredictionCycle() // Also reset the prediction buffer
    }
}