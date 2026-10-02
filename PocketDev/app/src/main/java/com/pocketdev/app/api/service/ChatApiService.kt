package com.pocketdev.app.api.service

import com.pocketdev.app.api.models.ChatRequest
import com.pocketdev.app.api.models.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/**
 * Provider-agnostic OpenAI-compatible chat service. The concrete base URL is
 * chosen per [com.pocketdev.app.api.AiProvider] by [ApiClientFactory].
 */
interface ChatApiService {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @HeaderMap extraHeaders: Map<String, String> = emptyMap(),
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
