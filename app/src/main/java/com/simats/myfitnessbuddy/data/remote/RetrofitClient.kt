package com.simats.myfitnessbuddy.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.simats.myfitnessbuddy.data.local.SettingsManager

object RetrofitClient {
    // private const val BASE_URL = "http://10.0.2.2:8000/"
    const val BASE_URL = "https://memorially-thallous-jett.ngrok-free.dev/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addHeaderInterceptor()
        .build()

    private fun OkHttpClient.Builder.addHeaderInterceptor() = addInterceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true")
        
        SettingsManager.authToken?.let { token ->
            requestBuilder.addHeader("Authorization", token)
        }
        
        chain.proceed(requestBuilder.build())
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
