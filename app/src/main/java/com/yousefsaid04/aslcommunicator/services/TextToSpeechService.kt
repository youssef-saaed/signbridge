package com.yousefsaid04.aslcommunicator.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechService(
    context: Context,
    private val onInit: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
                isInitialized = false
                onInit(false)
            } else {
                // THE CHANGE IS HERE: Set the speech rate to 70% of normal speed.
                tts.setSpeechRate(0.7f)

                isInitialized = true
                onInit(true)
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
            isInitialized = false
            onInit(false)
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}