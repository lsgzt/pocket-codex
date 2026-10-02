package com.pocketdev.app.api.models

import com.google.gson.annotations.SerializedName

/**
 * OpenAI-compatible chat request shared by Groq / OpenRouter / Gemini.
 *
 * Nullable fields are omitted by Gson during serialization (we intentionally
 * do NOT serialize nulls), so provider-specific reasoning fields are simply
 * left null for providers / models that don't support them.
 */
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean? = null,
    // ---- Thinking / reasoning controls -------------------------------------
    /** OpenAI-style effort dial: low / medium / high. Groq gpt-oss, Gemini compat. */
    @SerializedName("reasoning_effort")
    val reasoningEffort: String? = null,
    /** Groq: "parsed" separates reasoning from content, "hidden" suppresses it. */
    @SerializedName("reasoning_format")
    val reasoningFormat: String? = null,
    /** OpenRouter reasoning object: { enabled, effort }. */
    val reasoning: ReasoningOptions? = null
)

/** OpenRouter `reasoning` request object. */
data class ReasoningOptions(
    val enabled: Boolean? = null,
    val effort: String? = null
)

data class Message(
    val role: String,
    val content: String? = null,
    /** Reasoning content returned by thinking models (OpenRouter, Groq gpt-oss). */
    val reasoning: String? = null,
    /** Alternate reasoning field used by some OpenAI-compatible providers. */
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

data class Choice(
    val index: Int = 0,
    val message: Message? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

data class ErrorResponse(
    val error: ErrorDetail?
)

data class ErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

// ---------------------------------------------------------------------------
// Streaming (SSE) chunk models
// ---------------------------------------------------------------------------

data class StreamResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<StreamChoice>? = null
)

data class StreamChoice(
    val index: Int = 0,
    val delta: DeltaMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)
