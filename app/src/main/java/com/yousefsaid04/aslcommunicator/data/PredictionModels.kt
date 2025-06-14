package com.yousefsaid04.aslcommunicator.data

import com.google.gson.annotations.SerializedName

data class PredictionRequest(
    @SerializedName("landmarks")
    val landmarks: List<Float>
)

data class PredictionResponse(
    @SerializedName("prediction")
    val prediction: String,

    @SerializedName("confidence")
    val confidence: Float
)