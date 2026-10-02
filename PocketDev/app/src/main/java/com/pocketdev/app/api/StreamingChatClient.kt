package com.pocketdev.app.api

import com.google.gson.Gson
import com.pocketdev.app.api.models.ChatRequest
import com.pocketdev.app.api.models.StreamResponse
import com.pocketdev.app.api.service.ApiClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Thrown when the streaming endpoint answers with a non-2xx code. */
class StreamHttpException(val code: Int, val body: String) :
    IOException("HTTP $code: ${body.take(400)}")

/**
 * Minimal SSE (Server-Sent Events) streaming chat client on top of OkHttp.
 *
 * Parses `data: {...}` / `data: [DONE]` lines from the OpenAI-compatible
 * streaming endpoint of all three providers, normalizes the many reasoning
 * formats into a pair of (reasoning, content) accumulators and surfaces
 * incremental deltas through [onDelta].
 */
class StreamingChatClient(
    private val client: OkHttpClient = ApiClientFactory.streamingClient,
    private val gson: Gson = ApiClientFactory.gson
) {

    /**
     * Executes a streaming chat completion.
     *
     * @param onDelta called for each incremental piece of reasoning and/or
     *                content, on the IO dispatcher.
     * @return accumulated (reasoning, content) pair.
     */
    suspend fun streamChat(
        config: AiConfig,
        request: ChatRequest,
        onDelta: (reasoning: String?, content: String?) -> Unit
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val payload = gson.toJson(request)
        val builder = Request.Builder()
            .url(config.provider.baseUrl + "chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "text/event-stream")

        ApiClientFactory.extraHeaders(config.provider).forEach { (k, v) ->
            builder.header(k, v)
        }

        val call = client.newCall(
            builder
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
        )

        call.execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = try { response.body?.string().orEmpty() } catch (_: Exception) { "" }
                throw StreamHttpException(response.code, errBody)
            }
            val source = response.body?.source()
                ?: throw IOException("Empty response body")

            val splitter = ReasoningSplitter()
            val reasoning = StringBuilder()
            val content = StringBuilder()

            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty() || line.startsWith(":")) continue // SSE comments / keep-alives
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (data == "[DONE]") break

                val chunk: StreamResponse? = try {
                    gson.fromJson(data, StreamResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                val delta = chunk?.choices?.firstOrNull()?.delta ?: continue

                var reasoningDelta: String? = delta.reasoning ?: delta.reasoningContent
                var contentDelta: String? = delta.content

                if (contentDelta != null) {
                    val (r, c) = splitter.feed(contentDelta)
                    if (!r.isNullOrEmpty()) reasoningDelta = (reasoningDelta ?: "") + r
                    if (c != null) contentDelta = c
                }

                var emitted = false
                if (!reasoningDelta.isNullOrEmpty()) {
                    reasoning.append(reasoningDelta)
                    emitted = true
                }
                if (!contentDelta.isNullOrEmpty()) {
                    content.append(contentDelta)
                    emitted = true
                }
                if (emitted) {
                    onDelta(
                        reasoningDelta?.takeIf { it.isNotEmpty() },
                        contentDelta?.takeIf { it.isNotEmpty() }
                    )
                }
            }

            val (finalR, finalC) = splitter.finish()
            if (finalR.isNotEmpty()) { reasoning.append(finalR); onDelta(finalR, null) }
            if (finalC.isNotEmpty()) { content.append(finalC); onDelta(null, finalC) }

            reasoning.toString() to content.toString()
        }
    }
}

/**
 * Stateful splitter that separates inline reasoning blocks
 * (`<think>...</think>`, `<thinking>...</thinking>`) from real content.
 *
 * Handles tags that are split across chunk boundaries by holding back a
 * small partial-tag suffix until more data arrives.
 */
class ReasoningSplitter {

    private val openTags = listOf("<think>", "<thinking>")
    private val closeTags = listOf("</think>", "</thinking>")
    private var inThink = false
    private val pending = StringBuilder()

    fun feed(chunk: String): Pair<String?, String?> {
        pending.append(chunk)
        val reasoning = StringBuilder()
        val content = StringBuilder()
        while (pending.isNotEmpty()) {
            if (inThink) {
                val found = findTag(closeTags)
                if (found != null) {
                    val (tag, idx) = found
                    reasoning.append(pending, 0, idx)
                    pending.delete(0, idx + tag.length)
                    inThink = false
                } else {
                    val hold = partialSuffixLength(closeTags)
                    val emitLen = pending.length - hold
                    if (emitLen > 0) {
                        reasoning.append(pending, 0, emitLen)
                        pending.delete(0, emitLen)
                    }
                    break
                }
            } else {
                val found = findTag(openTags)
                if (found != null) {
                    val (tag, idx) = found
                    content.append(pending, 0, idx)
                    pending.delete(0, idx + tag.length)
                    inThink = true
                } else {
                    val hold = partialSuffixLength(openTags)
                    val emitLen = pending.length - hold
                    if (emitLen > 0) {
                        content.append(pending, 0, emitLen)
                        pending.delete(0, emitLen)
                    }
                    break
                }
            }
        }
        val r = reasoning.toString().takeIf { it.isNotEmpty() }
        val c = content.toString().takeIf { it.isNotEmpty() }
        return r to c
    }

    fun finish(): Pair<String, String> {
        val leftover = pending.toString()
        pending.clear()
        return if (inThink) leftover to "" else "" to leftover
    }

    private fun findTag(tags: List<String>): Pair<String, Int>? {
        var best: Pair<String, Int>? = null
        for (tag in tags) {
            val idx = pending.indexOf(tag)
            if (idx >= 0 && (best == null || idx < best.second)) best = tag to idx
        }
        return best
    }

    /** Longest suffix of [pending] that is a strict prefix of one of [tags]. */
    private fun partialSuffixLength(tags: List<String>): Int {
        val len = pending.length
        val maxCheck = minOf(len, 12)
        for (size in maxCheck downTo 1) {
            val suffix = pending.substring(len - size)
            for (tag in tags) {
                if (tag.length > size && tag.startsWith(suffix)) return size
            }
        }
        return 0
    }
}
