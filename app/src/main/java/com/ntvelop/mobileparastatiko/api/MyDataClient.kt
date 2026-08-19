package com.ntvelop.mobileparastatiko.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object MyDataClient {

    private const val PROD_URL = "https://mydatapi.aade.gr/myDATA/"
    private const val DEV_URL = "https://mydataapidev.aade.gr/"

    fun getBaseUrl(): String {
        return if (sessionManager?.isSandboxMode() != false) DEV_URL else PROD_URL
    }

    var sessionManager: SessionManager? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        val userId = sessionManager?.getUsername() ?: ""
        val subKey = sessionManager?.getSubscriptionKey() ?: ""

        val requestBuilder = original.newBuilder()
            .header("aade-user-id", userId)
            .header("ocp-apim-subscription-key", subKey)
            .header("Content-Type", "text/xml; charset=utf-8")
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

    /**
     * Converter factory to pass raw XML strings seamlessly
     */
    private val rawStringConverterFactory = object : Converter.Factory() {
        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<out Annotation>,
            methodAnnotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<*, RequestBody>? {
            if (type == String::class.java) {
                return Converter<String, RequestBody> { value ->
                    RequestBody.create("text/xml; charset=utf-8".toMediaType(), value)
                }
            }
            return null
        }

        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            if (type == String::class.java) {
                return Converter<ResponseBody, String> { value -> value.string() }
            }
            return null
        }
    }

    fun getRetrofit(): Retrofit {
        val currentInstance = retrofitInstance
        if (currentInstance != null) return currentInstance

        val newInstance = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(rawStringConverterFactory)
            .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
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
