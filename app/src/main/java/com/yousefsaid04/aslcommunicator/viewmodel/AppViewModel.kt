package com.yousefsaid04.aslcommunicator.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yousefsaid04.aslcommunicator.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private enum class AutoPilotState { PASSIVE, ACTIVE, DISABLED }

sealed class NavigationEvent {
    data object NavigateToSpeechToText : NavigationEvent()
    data object NavigateToSignToText : NavigationEvent()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private var autoPilotState = AutoPilotState.PASSIVE
    private var silenceTimerJob: Job? = null
    private var clearTextJob: Job? = null
    private val MAX_WORD_COUNT = 12

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
    private val speechRecognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    fun onManualNavigationTo(screen: Screen) {
        silenceTimerJob?.cancel()
        clearTextJob?.cancel()
        when (screen) {
            Screen.SignToText -> autoPilotState = AutoPilotState.PASSIVE
            Screen.SpeechToText -> autoPilotState = AutoPilotState.DISABLED
        }
        startListening()
    }

    private fun startSilenceTimer() {
        if (autoPilotState == AutoPilotState.ACTIVE) {
            silenceTimerJob?.cancel()
            silenceTimerJob = viewModelScope.launch {
                delay(3000)
                autoPilotState = AutoPilotState.PASSIVE
                _navigationEvent.emit(NavigationEvent.NavigateToSignToText)
            }
        }
    }

    private fun startClearTextTimer() {
        clearTextJob?.cancel()
        clearTextJob = viewModelScope.launch {
            delay(2500)
            _recognizedText.value = ""
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
        override fun onBeginningOfSpeech() {
            _isListening.value = true
            silenceTimerJob?.cancel()
            clearTextJob?.cancel()
        }

        override fun onEndOfSpeech() {
            _isListening.value = false
            startClearTextTimer()
            startSilenceTimer()
            // THE FIX: DO NOT CALL startListening() HERE. THIS BREAKS THE FREEZE LOOP.
        }

        override fun onError(error: Int) {
            _isListening.value = false
            startClearTextTimer()
            startSilenceTimer()
            // THE FIX: DO NOT CALL startListening() HERE. THIS BREAKS THE FREEZE LOOP.
        }

        override fun onPartialResults(partialResults: Bundle?) {
            clearTextJob?.cancel()
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.get(0) ?: ""
            if (recognizedText.isNotBlank()) {
                _recognizedText.value = recognizedText
                silenceTimerJob?.cancel()
                val wordCount = recognizedText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                if (wordCount >= 2 && autoPilotState == AutoPilotState.PASSIVE) {
                    viewModelScope.launch {
                        autoPilotState = AutoPilotState.ACTIVE
                        _navigationEvent.emit(NavigationEvent.NavigateToSpeechToText)
                    }
                }
                if (wordCount > MAX_WORD_COUNT) {
                    _recognizedText.value = ""
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { _recognizedText.value = matches[0] }
            startClearTextTimer()
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        speechRecognizer.setRecognitionListener(recognitionListener)
        startListening()
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            speechRecognizer.startListening(speechRecognizerIntent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
    }
}