package com.pocketdev.app.api.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pocketdev.app.api.AiProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Builds and caches one Retrofit instance per AI provider.
 *
 * Notes:
 * - Gson is lenient but does NOT serialize nulls, so provider-specific
 *   reasoning fields simply disappear from the JSON when unused.
 * - Read timeout is generous because thinking models can reason for a long
 *   time before emitting any final content.
 */
object ApiClientFactory {

    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 180L
    private const val STREAM_READ_TIMEOUT_SECONDS = 300L

    /** Shared, strict-enough Gson for both requests and responses. */
    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    /** Client used for classic (non-streaming) Retrofit calls. */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Long-timeout client used for SSE streaming (thinking models are slow). */
    val streamingClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(STREAM_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(STREAM_READ_TIMEOUT_SECONDS + 60, TimeUnit.SECONDS)
        .build()

    private val retrofits = ConcurrentHashMap<String, Retrofit>()

    private fun retrofitFor(provider: AiProvider): Retrofit =
        retrofits.computeIfAbsent(provider.id) {
            Retrofit.Builder()
                .baseUrl(provider.baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

    fun chatService(provider: AiProvider): ChatApiService =
        retrofitFor(provider).create(ChatApiService::class.java)

    /** Extra headers per provider (OpenRouter app attribution). */
    fun extraHeaders(provider: AiProvider): Map<String, String> = when (provider) {
        AiProvider.OPENROUTER -> mapOf(
            "HTTP-Referer" to "https://github.com/lsgzt/pocket-codex",
            "X-Title" to "PocketDev"
        )
        else -> emptyMap()
    }
}
