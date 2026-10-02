package com.pocketdev.app.repository

import com.pocketdev.app.api.AiConfig
import com.pocketdev.app.api.AiProvider
import com.pocketdev.app.api.ReasoningSplitter
import com.pocketdev.app.api.StreamingChatClient
import com.pocketdev.app.api.StreamHttpException
import com.pocketdev.app.api.models.ChatRequest
import com.pocketdev.app.api.models.Message
import com.pocketdev.app.api.models.ReasoningOptions
import com.pocketdev.app.api.service.ApiClientFactory
import com.pocketdev.app.data.models.AiResult
import com.pocketdev.app.data.models.Language
import com.pocketdev.app.data.models.ProjectFile
import kotlinx.coroutines.delay
import java.io.IOException

/** Incremental events emitted while streaming a thinking-model response. */
sealed class ChatStreamEvent {
    object Started : ChatStreamEvent()
    data class Thinking(val delta: String) : ChatStreamEvent()
    data class Answer(val delta: String) : ChatStreamEvent()
}

/**
 * Multi-provider AI repository (Groq + OpenRouter + Gemini).
 *
 * Supports:
 *  - Thinking / reasoning models, with reasoning separated from content
 *    (message.reasoning, message.reasoning_content, or inline  modelling blocks).
 *  - SSE streaming for interactive features with live thinking display.
 *  - Automatic retry with backoff and a graceful fallback that retries once
 *    without thinking parameters when a provider rejects them.
 */
class AiRepository {

    private val streamingClient = StreamingChatClient()

    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_PROMPT_CHARS = 20000
        private const val THINKING_MAX_TOKENS = 8192
        private const val STANDARD_MAX_TOKENS = 4096
        private const val GHOST_MAX_TOKENS = 1024

        private val DEFAULT_SYSTEM_PROMPT = "You are a helpful coding assistant for students " +
                "learning to program. Provide clear, educational explanations and high-quality " +
                "code examples. Be encouraging and beginner-friendly in your responses."

        private val EDITOR_SYSTEM_PROMPT = "You are a precise code editor. Follow the " +
                "formatting rules strictly."
    }

    // ------------------------------------------------------------------
    // Public feature API
    // ------------------------------------------------------------------

    suspend fun fixBug(
        files: List<ProjectFile>,
        activeFileName: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val prompt = buildString {
            append("Analyze this project and identify any bugs, errors, or issues.\n\n")
            append(buildProjectContext(files, activeFileName))
            append("Please provide:\n")
            append("1. A clear explanation of what bugs/issues were found\n")
            append("2. The corrected code as patches\n")
            append("3. An explanation of each fix\n\n")
            append("Format your response as:\n")
            append("ISSUES FOUND:\n[explanation]\n\n")
            append("Then, provide the fixes as patches using the following format:\n")
            append("FILE: main.kt\n")
            append("EDIT_START: start_line_number\n")
            append("EDIT_END: end_line_number\n")
            append("NEW_CODE:\n")
            append("new lines to replace them with\n")
            append("EOF\n\n")
            append("You can include multiple patches.\n")
        }
        return callAi(prompt, config, EDITOR_SYSTEM_PROMPT, temperature = 0.2,
            extractCode = false, useStreaming = true, onEvent = onEvent,
            parsePatches = true)
    }

    suspend fun getGhostSuggestion(
        code: String,
        cursorPosition: Int,
        language: Language,
        config: AiConfig
    ): AiResult {
        val lines = code.lines()
        var currentLineIndex = 0
        var charCount = 0
        for (i in lines.indices) {
            charCount += lines[i].length + 1
            if (charCount > cursorPosition) {
                currentLineIndex = i
                break
            }
        }

        val currentLine = if (currentLineIndex < lines.size) lines[currentLineIndex] else ""
        val cursorPosInLine = cursorPosition - (charCount - lines[currentLineIndex].length - 1)
        val textBeforeCursorInLine = currentLine.substring(0, minOf(cursorPosInLine, currentLine.length))
        val textAfterCursorInLine = if (cursorPosInLine < currentLine.length) currentLine.substring(cursorPosInLine) else ""

        val prompt = buildString {
            append("You are a precise code completion assistant for ${language.displayName}.\n")
            append("Your task is to suggest the most likely completion based on the context.\n\n")

            append("CONTEXT:\n")
            append("- Current line: \"$currentLine\"\n")
            append("- Cursor position: after \"$textBeforeCursorInLine\"\n")
            append("- Text after cursor on same line: \"$textAfterCursorInLine\"\n\n")

            append("SURROUNDING CODE:\n")
            append("```")
            append(language.displayName.lowercase())
            append('\n')
            val startLine = maxOf(0, currentLineIndex - 10)
            val endLine = minOf(lines.size, currentLineIndex + 5)
            for (i in startLine until endLine) {
                if (i == currentLineIndex) {
                    append("${lines[i].substring(0, minOf(cursorPosInLine, lines[i].length))}<|CURSOR|>")
                    append(if (cursorPosInLine < lines[i].length) lines[i].substring(cursorPosInLine) else "")
                    append('\n')
                } else {
                    append(lines[i])
                    append('\n')
                }
            }
            append("```\n\n")

            append("RULES:\n")
            append("1. Only suggest completions that are contextually relevant\n")
            append("2. For method calls, suggest the most common/likely method\n")
            append("3. For variable names, use existing variables in scope\n")
            append("4. Keep suggestions SHORT - typically 1-3 words or a single method call\n")
            append("5. Do NOT suggest entire blocks of code unless truly necessary\n")
            append("6. Consider the language syntax and common patterns\n\n")

            append("RESPONSE FORMAT (strict):\n")
            append("TYPE: APPEND\n")
            append("SUGGESTION: [your short suggestion here]\n\n")
            append("OR for fixing/modifying the current line:\n")
            append("TYPE: REPLACE\n")
            append("DELETE: [text to delete - the part that's wrong]\n")
            append("ADD: [text to add - the correction]\n\n")
            append("Examples:\n")
            append("- If user typed 'fruits = [\"apple\", \"banana\"]\\nfruits.' suggest '.append(' or '.sort()'\n")
            append("- If user typed 'print(' suggest the variable name or closing ')'\n")
            append("- If there's a typo like 'printl' suggest DELETE: printl, ADD: println\n")
        }

        // Ghost suggestions always take the fast path: no reasoning params.
        val fastConfig = config.copy(thinkingEnabled = false)
        val result = callAi(
            prompt, fastConfig, EDITOR_SYSTEM_PROMPT,
            temperature = 0.3, extractCode = false, useStreaming = false,
            onEvent = {}, parsePatches = false, maxTokens = GHOST_MAX_TOKENS
        )
        if (!result.isSuccess) return result

        val content = result.content
        val typeMatch = Regex("TYPE:\\s*(APPEND|REPLACE)").find(content)
        val type = typeMatch?.groupValues?.get(1) ?: "APPEND"

        if (type == "REPLACE") {
            val deleteMatch = Regex("DELETE:\\s*(.+?)(?=\\nADD:|\\nTYPE:|$)", RegexOption.DOT_MATCHES_ALL).find(content)
            val addMatch = Regex("ADD:\\s*(.+?)(?=\\nTYPE:|$)", RegexOption.DOT_MATCHES_ALL).find(content)

            val deleteText = deleteMatch?.groupValues?.get(1)?.trim() ?: ""
            var addText = addMatch?.groupValues?.get(1)?.trim() ?: ""
            addText = addText.replace("\\n", "\n")

            val deletePos = code.indexOf(deleteText, maxOf(0, cursorPosition - deleteText.length - 50))
            val actualDeletePos = if (deletePos >= 0) deletePos else cursorPosition

            return result.copy(
                content = addText,
                isEdit = true,
                deleteText = deleteText,
                addText = addText,
                editStartPos = actualDeletePos,
                editEndPos = actualDeletePos + deleteText.length
            )
        }

        val suggestionMatch = Regex("SUGGESTION:\\s*(.+?)(?=\\nTYPE:|$)", RegexOption.DOT_MATCHES_ALL).find(content)
        var suggestion = suggestionMatch?.groupValues?.get(1)?.trim()
            ?: content.substringAfter("SUGGESTION:").trim()
        suggestion = suggestion.replace("\\n", "\n")
        return result.copy(content = suggestion, isEdit = false)
    }

    suspend fun explainCode(
        files: List<ProjectFile>,
        activeFileName: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val prompt = buildString {
            append("Explain the code in simple, beginner-friendly terms.\n\n")
            append(buildProjectContext(files, activeFileName))
            append("Break down what each part does step-by-step. ")
            append("Use simple language suitable for students who are learning to code. ")
            append("Include:\n")
            append("1. What the code does overall\n")
            append("2. A step-by-step breakdown of each part\n")
            append("3. Any important concepts used\n")
            append("4. Tips for beginners")
        }
        return callAi(prompt, config, DEFAULT_SYSTEM_PROMPT, temperature = 0.7,
            extractCode = false, useStreaming = true, onEvent = onEvent)
    }

    suspend fun improveCode(
        files: List<ProjectFile>,
        activeFileName: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val prompt = buildString {
            append("Suggest improvements for this project.\n\n")
            append(buildProjectContext(files, activeFileName))
            append("Focus on:\n")
            append("1. Best practices\n")
            append("2. Performance optimization\n")
            append("3. Code readability and maintainability\n")
            append("4. Error handling\n")
            append("5. Modern language features\n\n")
            append("Format your response as:\n")
            append("IMPROVEMENTS SUGGESTED:\n[list of improvements]\n\n")
            append("Then, provide the improvements as patches using the following format:\n")
            append("FILE: main.kt\n")
            append("EDIT_START: start_line_number\n")
            append("EDIT_END: end_line_number\n")
            append("NEW_CODE:\n")
            append("new lines to replace them with\n")
            append("EOF\n\n")
            append("You can include multiple patches.\n")
        }
        return callAi(prompt, config, EDITOR_SYSTEM_PROMPT, temperature = 0.2,
            extractCode = false, useStreaming = true, onEvent = onEvent,
            parsePatches = true)
    }

    suspend fun autoFixCode(
        currentCode: String,
        error: String,
        historyText: String,
        language: Language,
        config: AiConfig
    ): AiResult {
        val prompt = buildString {
            append("You are fixing code that failed to execute.\n\n")
            append("Current Code:\n$currentCode\n\n")
            append("Runtime Error:\n$error\n\n")
            if (historyText.isNotBlank()) {
                append("Previous Attempts:\n$historyText\n\n")
            }
            append("Task:\nFix the code so it runs correctly.\n\n")
            append("Rules:\n")
            append("Return ONLY edits using the following patch format:\n")
            append("FILE: main${language.extension}\n")
            append("EDIT_START: start_line_number\n")
            append("EDIT_END: end_line_number\n")
            append("NEW_CODE:\n")
            append("new lines to replace them with\n")
            append("EOF\n\n")
            append("You can include multiple patches.\n")
            append("First, provide a brief thought process explaining what went wrong and how you will fix it.\n")
            append("Then, provide the patches.\n")
            append("Avoid repeating previous failed fixes.\n")
        }
        return callAi(prompt, config, EDITOR_SYSTEM_PROMPT, temperature = 0.2,
            extractCode = false, useStreaming = false, onEvent = {},
            parsePatches = true)
    }

    suspend fun modifyCode(
        prompt: String,
        files: List<ProjectFile>,
        activeFileName: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val fullPrompt = buildString {
            append("You are an expert developer. Please modify the code according to this request: $prompt\n\n")
            append(buildProjectContext(files, activeFileName))
            append("Return ONLY edits using the following patch format:\n")
            append("FILE: main.kt\n")
            append("EDIT_START: start_line_number\n")
            append("EDIT_END: end_line_number\n")
            append("NEW_CODE:\n")
            append("new lines to replace them with\n")
            append("EOF\n\n")
            append("You can include multiple patches.\n")
        }
        return callAi(fullPrompt, config, EDITOR_SYSTEM_PROMPT, temperature = 0.2,
            extractCode = false, useStreaming = true, onEvent = onEvent,
            parsePatches = true)
    }

    suspend fun askFollowUp(
        previousPrompt: String,
        previousResponse: String,
        question: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val prompt = buildString {
            append("Previous Context:\n$previousPrompt\n\n")
            append("Your Previous Response:\n$previousResponse\n\n")
            append("User Follow-up Question:\n$question\n\n")
            append("Please answer the follow-up question based on the context above.")
        }
        return callAi(prompt, config, DEFAULT_SYSTEM_PROMPT, temperature = 0.7,
            extractCode = false, useStreaming = true, onEvent = onEvent)
    }

    suspend fun editCode(
        prompt: String,
        files: List<ProjectFile>,
        activeFileName: String,
        config: AiConfig,
        onEvent: (ChatStreamEvent) -> Unit = {}
    ): AiResult {
        val fullPrompt = buildString {
            append("You are modifying a project with multiple files.\n\n")
            append("Instruction:\n$prompt\n\n")
            append(buildProjectContext(files, activeFileName))
            append("Rules:\n")
            append("Return ONLY edits using the following patch format:\n")
            append("FILE: filename.ext\n")
            append("EDIT_START: start_line_number\n")
            append("EDIT_END: end_line_number\n")
            append("NEW_CODE:\n")
            append("new lines to replace them with\n")
            append("EOF\n\n")
            append("You can include multiple patches.\n")
            append("Do not include explanations.\n")
        }
        var lastError = ""
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = callAi(fullPrompt, config, EDITOR_SYSTEM_PROMPT,
                    temperature = 0.2, extractCode = false, useStreaming = true,
                    onEvent = onEvent, parsePatches = true)
                if (result.isSuccess) return result
                lastError = result.errorMessage ?: "Unknown error"
                if (lastError.contains("rate_limit", ignoreCase = true)) {
                    delay(BASE_DELAY_MS * (attempt + 1) * 2)
                } else {
                    delay(BASE_DELAY_MS * (attempt + 1))
                }
            } catch (e: IOException) {
                lastError = "Network error: ${e.message}"
                delay(BASE_DELAY_MS * (attempt + 1))
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                delay(BASE_DELAY_MS * (attempt + 1))
            }
        }
        return AiResult(content = "", isSuccess = false, errorMessage = lastError)
    }

    // ------------------------------------------------------------------
    // Core execution
    // ------------------------------------------------------------------

    private suspend fun callAi(
        prompt: String,
        config: AiConfig,
        systemPrompt: String,
        temperature: Double,
        extractCode: Boolean,
        useStreaming: Boolean,
        onEvent: (ChatStreamEvent) -> Unit,
        parsePatches: Boolean = false,
        maxTokens: Int? = null
    ): AiResult {
        val truncatedPrompt = if (prompt.length > MAX_PROMPT_CHARS) {
            prompt.take(MAX_PROMPT_CHARS) + "\n...[truncated]"
        } else prompt

        val messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = truncatedPrompt)
        )

        var lastError = ""
        var plainRetryAvailable = true

        repeat(MAX_RETRIES) { attempt ->
            try {
                var forcePlain = false
                var result: AiResult
                while (true) {
                    result = executeOnce(
                        messages = messages,
                        config = if (forcePlain) config.copy(thinkingEnabled = false) else config,
                        temperature = temperature,
                        extractCode = extractCode,
                        useStreaming = useStreaming,
                        onEvent = onEvent,
                        parsePatches = parsePatches,
                        maxTokens = maxTokens
                    )
                    if (result.isSuccess) return result

                    lastError = result.errorMessage ?: "Unknown error"

                    // A provider may reject our thinking params — retry once
                    // without them before giving up.
                    if (plainRetryAvailable && isUnsupportedThinkingError(lastError)) {
                        plainRetryAvailable = false
                        forcePlain = true
                        continue
                    }
                    break
                }

                if (lastError.contains("rate_limit", ignoreCase = true)) {
                    delay(BASE_DELAY_MS * (attempt + 1) * 2)
                } else if (isRetryableError(lastError)) {
                    delay(BASE_DELAY_MS * (attempt + 1))
                } else {
                    return result // non-retryable, already reported
                }
            } catch (e: IOException) {
                lastError = "Network error: ${e.message}"
                delay(BASE_DELAY_MS * (attempt + 1))
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                delay(BASE_DELAY_MS * (attempt + 1))
            }
        }
        return AiResult(content = "", isSuccess = false, errorMessage = lastError)
    }

    private suspend fun executeOnce(
        messages: List<Message>,
        config: AiConfig,
        temperature: Double,
        extractCode: Boolean,
        useStreaming: Boolean,
        onEvent: (ChatStreamEvent) -> Unit,
        parsePatches: Boolean,
        maxTokens: Int?
    ): AiResult {
        if (useStreaming) {
            try {
                onEvent(ChatStreamEvent.Started)
                val request = buildRequest(
                    config = config,
                    messages = messages,
                    temperature = temperature,
                    stream = true,
                    maxTokens = maxTokens
                )
                val (reasoning, content) = streamingClient.streamChat(config, request) { r, c ->
                    if (r != null) onEvent(ChatStreamEvent.Thinking(r))
                    if (c != null) onEvent(ChatStreamEvent.Answer(c))
                }
                return buildSuccessResult(content, reasoning, extractCode, parsePatches)
            } catch (e: StreamHttpException) {
                if (e.code == 400 || e.code == 402 || e.code == 404 || e.code == 422) {
                    // Keep the raw body so retry logic can detect unsupported
                    // thinking parameters and users can see provider details.
                    return AiResult(content = "", isSuccess = false,
                        errorMessage = "HTTP ${e.code}: ${e.body.take(400)}")
                }
                return AiResult(content = "", isSuccess = false,
                    errorMessage = friendlyError(e.code, e.body, config.provider))
            } catch (e: IOException) {
                // Network hiccup — fall back to a single non-streaming attempt.
            }
        }

        return executeNonStreaming(messages, config, temperature, extractCode, parsePatches, maxTokens)
    }

    private suspend fun executeNonStreaming(
        messages: List<Message>,
        config: AiConfig,
        temperature: Double,
        extractCode: Boolean,
        parsePatches: Boolean,
        maxTokens: Int?
    ): AiResult {
        val service = ApiClientFactory.chatService(config.provider)
        val request = buildRequest(
            config = config,
            messages = messages,
            temperature = temperature,
            stream = false,
            maxTokens = maxTokens
        )
        val response = service.chatCompletion(
            authorization = "Bearer ${config.apiKey}",
            extraHeaders = ApiClientFactory.extraHeaders(config.provider),
            request = request
        )

        return if (response.isSuccessful) {
            val body = response.body()
            val message = body?.choices?.firstOrNull()?.message
            var reasoning = message?.reasoning ?: message?.reasoningContent ?: ""
            var content = message?.content.orEmpty()

            // Some reasoning models inline their thoughts in content.
            val splitter = ReasoningSplitter()
            val (r1, c1) = splitter.feed(content)
            val (r2, c2) = splitter.finish()
            if (!r1.isNullOrEmpty() || r2.isNotEmpty()) {
                reasoning = (reasoning + r1 + r2).trim()
                content = (c1.orEmpty() + c2).trim()
            }

            buildSuccessResult(content, reasoning, extractCode, parsePatches)
        } else {
            val errorBody = try { response.errorBody()?.string().orEmpty() } catch (_: Exception) { "" }
            val errorMessage = if (response.code() == 400 || response.code() == 402 ||
                response.code() == 404 || response.code() == 422
            ) {
                "HTTP ${response.code()}: ${errorBody.take(400)}"
            } else {
                friendlyError(response.code(), errorBody, config.provider)
            }
            AiResult(
                content = "",
                isSuccess = false,
                errorMessage = errorMessage
            )
        }
    }

    private fun buildSuccessResult(
        content: String,
        reasoning: String,
        extractCode: Boolean,
        parsePatches: Boolean
    ): AiResult {
        if (content.isBlank()) {
            return AiResult(
                content = "",
                reasoning = reasoning.trim().takeIf { it.isNotBlank() },
                isSuccess = false,
                errorMessage = "The model returned only reasoning and no answer. " +
                        "Try a higher token limit or a non-thinking model."
            )
        }

        val patches = if (parsePatches) parsePatches(content) else emptyList()
        val isEdit = patches.isNotEmpty()

        return AiResult(
            content = content,
            correctedCode = if (extractCode) extractCodeBlock(content) else null,
            reasoning = reasoning.trim().takeIf { it.isNotBlank() },
            isSuccess = true,
            isEdit = isEdit,
            patches = patches,
            errorMessage = if (parsePatches && !isEdit) {
                "AI response did not contain valid patches, but here is the explanation."
            } else null
        )
    }

    private fun parsePatches(content: String): List<com.pocketdev.app.data.models.FilePatch> {
        val patches = mutableListOf<com.pocketdev.app.data.models.FilePatch>()
        val patchRegex = Regex("FILE:\\s*(.+)\\nEDIT_START:\\s*(\\d+)\\nEDIT_END:\\s*(\\d+)\\nNEW_CODE:\\n([\\s\\S]*?)\\nEOF")
        val matches = patchRegex.findAll(content).toList()
        for (match in matches) {
            val fileName = match.groupValues[1].trim()
            val editStart = match.groupValues[2].toIntOrNull() ?: continue
            val editEnd = match.groupValues[3].toIntOrNull() ?: continue
            val newCode = match.groupValues[4]
            patches.add(
                com.pocketdev.app.data.models.FilePatch(
                    fileName = fileName,
                    editStart = editStart,
                    editEnd = editEnd,
                    newCode = newCode
                )
            )
        }
        return patches
    }

    /** Builds a provider-specific request, attaching thinking params only where supported. */
    private fun buildRequest(
        config: AiConfig,
        messages: List<Message>,
        temperature: Double?,
        stream: Boolean,
        maxTokens: Int?
    ): ChatRequest {
        val thinkingOn = config.wantsThinking
        val effectiveMaxTokens = maxTokens ?: if (thinkingOn) THINKING_MAX_TOKENS else STANDARD_MAX_TOKENS

        var temperatureOut = temperature
        var reasoningEffort: String? = null
        var reasoningFormat: String? = null
        var reasoningOptions: ReasoningOptions? = null

        when (config.provider) {
            AiProvider.GROQ -> {
                if (thinkingOn) {
                    reasoningFormat = "parsed"
                    if (config.model.startsWith("openai/gpt-oss")) {
                        reasoningEffort = "medium"
                    }
                } else if (isHybrid(config)) {
                    reasoningFormat = "hidden" // suppress thinking for hybrid models
                }
            }
            AiProvider.OPENROUTER -> {
                reasoningOptions = when {
                    thinkingOn -> ReasoningOptions(effort = "medium")
                    isHybrid(config) -> ReasoningOptions(enabled = false)
                    else -> null
                }
                // Some strict OpenAI models reject custom temperatures.
                if (thinkingOn && needsDefaultTemperature(config.model)) {
                    temperatureOut = null
                }
            }
            AiProvider.GEMINI -> {
                if (thinkingOn) reasoningEffort = "medium"
            }
        }

        return ChatRequest(
            model = config.model,
            messages = messages,
            temperature = temperatureOut,
            maxTokens = effectiveMaxTokens,
            stream = stream,
            reasoningEffort = reasoningEffort,
            reasoningFormat = reasoningFormat,
            reasoning = reasoningOptions
        )
    }

    private fun isHybrid(config: AiConfig): Boolean =
        com.pocketdev.app.api.ModelCatalog.isHybridModel(config.provider, config.model)

    private fun needsDefaultTemperature(modelId: String): Boolean {
        val lower = modelId.lowercase()
        return lower.startsWith("openai/o") || lower.startsWith("openai/gpt-5")
    }

    private fun isUnsupportedThinkingError(error: String): Boolean {
        val lower = error.lowercase()
        val mentionsParam = lower.contains("reasoning") || lower.contains("thinking") ||
                lower.contains("effort") || lower.contains("max_tokens")
        val isRejection = lower.contains("400") || lower.contains("unsupported") ||
                lower.contains("not support") || lower.contains("unrecognized") ||
                lower.contains("unknown parameter") || lower.contains("invalid")
        return mentionsParam && isRejection
    }

    private fun isRetryableError(error: String): Boolean {
        val lower = error.lowercase()
        return lower.contains("rate_limit") || lower.contains("429") ||
                lower.contains("500") || lower.contains("502") || lower.contains("503") ||
                lower.contains("timeout") || lower.contains("network")
    }

    private fun friendlyError(code: Int, body: String, provider: AiProvider): String = when (code) {
        401 -> "Invalid API key. Please check your ${provider.displayName} API key in Settings."
        403 -> "Access denied by ${provider.displayName}. Check your key and account."
        429 -> "Rate limit exceeded. Please wait a moment and try again."
        500, 502, 503 -> "${provider.displayName} server error. Please try again later."
        else -> "API error $code: ${body.take(300)}"
    }

    private fun extractCodeBlock(content: String): String? {
        val trimmedContent = content.trim()

        val tripleWithLang = Regex("```[\\w]*\\n?([\\s\\S]*?)```")
        val tripleMatch = tripleWithLang.find(trimmedContent)
        if (tripleMatch != null) {
            return tripleMatch.groupValues[1].trim()
        }

        val singleBacktick = Regex("`([^`]+)`")
        val singleMatch = singleBacktick.find(trimmedContent)
        if (singleMatch != null) {
            return singleMatch.groupValues[1].trim()
        }

        return trimmedContent.removeSurrounding("```").removeSurrounding("`").trim()
    }

    private fun buildProjectContext(files: List<ProjectFile>, activeFileName: String): String {
        return buildString {
            append("Files:\n")
            files.forEach { file ->
                append("--- ${file.name} ---\n")
                if (file.name == activeFileName) {
                    val lines = file.code.lines()
                    lines.forEachIndexed { index, line ->
                        append("${index + 1}: $line\n")
                    }
                } else {
                    append("// Summary of ${file.name}\n")
                    val lines = file.code.lines()
                    var inComment = false
                    lines.forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("/*")) inComment = true
                        if (!inComment && trimmed.isNotEmpty() && !trimmed.startsWith("//")) {
                            if (trimmed.startsWith("fun ") || trimmed.startsWith("class ") ||
                                trimmed.startsWith("def ") || trimmed.startsWith("function ") ||
                                trimmed.startsWith("public ") || trimmed.startsWith("private ")
                            ) {
                                append("${index + 1}: $line\n")
                            }
                        }
                        if (trimmed.endsWith("*/")) inComment = false
                    }
                }
                append("\n")
            }
        }
    }
}
