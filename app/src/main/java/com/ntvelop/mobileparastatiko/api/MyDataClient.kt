package com.ntvelop.mobileparastatiko.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

object MyDataClient {

    private const val PROD_URL = "https://mydatapi.aade.gr/"
    private const val DEV_URL = "https://mydataapidev.aade.gr/"

    private fun getBaseUrl(): String {
        return if (sessionManager?.isSandboxMode() != false) DEV_URL else PROD_URL
    }

    var sessionManager: SessionManager? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        val userId = sessionManager?.getUsername() ?: ""
        val subKey = sessionManager?.getSubscriptionKey() ?: ""

        val requestBuilder = original.newBuilder()
            .header("aade-user-id", userId)
            .header("Ocp-Apim-Subscription-Key", subKey)
            .header("Accept", "application/xml")
            .header("Content-Type", "application/xml")
            .method(original.method, original.body)

        val request = requestBuilder.build()
            chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var retrofitInstance: Retrofit? = null

    fun getRetrofit(): Retrofit {
        val currentInstance = retrofitInstance
        if (currentInstance != null) return currentInstance

        val newInstance = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
        
        retrofitInstance = newInstance
        return newInstance
    }

    fun resetClient() {
        retrofitInstance = null
    }

    val api: MyDataApi 
        get() = getRetrofit().create(MyDataApi::class.java)
}
