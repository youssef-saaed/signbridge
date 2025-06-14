package com.yousefsaid04.aslcommunicator.services

import com.yousefsaid04.aslcommunicator.data.PredictionRequest
import com.yousefsaid04.aslcommunicator.data.PredictionResponse
import com.yousefsaid04.aslcommunicator.utils.AppConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("predict")
    suspend fun predict(@Body landmarks: PredictionRequest): PredictionResponse
}

class SignLanguageApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConstants.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(ApiService::class.java)

    suspend fun predict(landmarks: List<Float>): PredictionResponse? {
        return try {
            service.predict(PredictionRequest(landmarks))
        } catch (e: Exception) {
            // Handle exceptions like network errors
            e.printStackTrace()
            null
        }
    }
}