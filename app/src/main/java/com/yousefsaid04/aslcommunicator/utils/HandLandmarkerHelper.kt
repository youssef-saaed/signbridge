package com.yousefsaid04.aslcommunicator.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.lang.RuntimeException

class HandLandmarkerHelper(
    val context: Context,
    val listener: LandmarkerListener
) {

    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("hand_landmarker.task")
        val baseOptions = baseOptionsBuilder.build()
        try {
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener(this::onResults)
                .setErrorListener(this::onError)

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            listener.onError(e.message ?: "Hand Landmarker failed to initialize")
        }
    }

    fun detectLiveStream(bitmap: Bitmap) {
        if (handLandmarker == null) return
        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
    }

    private fun onResults(result: HandLandmarkerResult, input: MPImage) {
        listener.onResults(result)
    }

    private fun onError(error: RuntimeException) {
        listener.onError(error.message ?: "Unknown MediaPipe error")
    }

    fun clear() {
        handLandmarker?.close()
        handLandmarker = null
    }

    interface LandmarkerListener {
        fun onError(error: String)
        // This line has been corrected
        fun onResults(result: HandLandmarkerResult)
    }
}