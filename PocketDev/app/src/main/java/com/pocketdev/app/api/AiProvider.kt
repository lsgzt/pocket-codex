package com.pocketdev.app.api

/**
 * AI provider registry for PocketDev.
 *
 * All three providers expose an OpenAI-compatible /chat/completions endpoint,
 * which lets us share one request/response model layer while still applying
 * provider-specific reasoning ("thinking") parameters:
 *
 *  - Groq       : reasoning_format ("parsed") + reasoning_effort for gpt-oss models
 *  - OpenRouter : reasoning { effort } object
 *  - Gemini     : OpenAI-compat endpoint, reasoning_effort for 2.5 models
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val keyPrefixHint: String,
    val consoleUrl: String,
    val tagline: String
) {
    GROQ(
        id = "groq",
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1/",
        keyPrefixHint = "gsk_...",
        consoleUrl = "https://console.groq.com/keys",
        tagline = "Ultra-fast inference • free tier"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1/",
        keyPrefixHint = "sk-or-v1-...",
        consoleUrl = "https://openrouter.ai/settings/keys",
        tagline = "300+ models • one key"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
        keyPrefixHint = "AIza...",
        consoleUrl = "https://aistudio.google.com/apikey",
        tagline = "Gemini 2.5 with built-in thinking"
    );

    companion object {
        fun fromId(id: String?): AiProvider =
            entries.firstOrNull { it.id == id } ?: GROQ
    }
}

/**
 * A curated model entry for the model picker in Settings.
 *
 * @param id       exact model id sent to the provider API
 * @param label    human-friendly name
 * @param thinking true if the model can produce reasoning / chain-of-thought
 * @param hybrid   true if thinking can be turned ON and OFF for this model
 * @param free     true if this listing is a free variant (OpenRouter ":free")
 */
data class ModelInfo(
    val id: String,
    val label: String,
    val thinking: Boolean = false,
    val hybrid: Boolean = false,
    val description: String = "",
    val free: Boolean = false
)

/** Curated, opinionated model lists per provider. */
object ModelCatalog {

    val models: Map<AiProvider, List<ModelInfo>> = mapOf(
        AiProvider.GROQ to listOf(
            ModelInfo(
                "llama-3.3-70b-versatile",
                "Llama 3.3 70B Versatile",
                description = "Great all-rounder for coding"
            ),
            ModelInfo(
                "llama-3.1-8b-instant",
                "Llama 3.1 8B Instant",
                description = "Fastest — great for quick edits"
            ),
            ModelInfo(
                "moonshotai/kimi-k2-instruct",
                "Kimi K2 Instruct",
                description = "Strong coding & agentic model"
            ),
            ModelInfo(
                "meta-llama/llama-4-scout-17b-16e-instruct",
                "Llama 4 Scout 17B",
                description = "Multimodal-native Llama 4"
            ),
            ModelInfo(
                "openai/gpt-oss-120b",
                "GPT-OSS 120B 🧠",
                thinking = true,
                description = "OpenAI open-weight reasoning model"
            ),
            ModelInfo(
                "openai/gpt-oss-20b",
                "GPT-OSS 20B 🧠",
                thinking = true,
                description = "Compact reasoning model"
            ),
            ModelInfo(
                "qwen/qwen3-32b",
                "Qwen3 32B 🧠",
                thinking = true,
                hybrid = true,
                description = "Thinking on/off switchable"
            )
        ),
        AiProvider.OPENROUTER to listOf(
            ModelInfo(
                "deepseek/deepseek-chat-v3.1",
                "DeepSeek V3.1 🧠",
                thinking = true,
                hybrid = true,
                description = "Hybrid reasoning, excellent for code"
            ),
            ModelInfo(
                "deepseek/deepseek-r1",
                "DeepSeek R1 🧠",
                thinking = true,
                description = "Deep reasoning, budget-friendly"
            ),
            ModelInfo(
                "qwen/qwen3-coder",
                "Qwen3 Coder 🧠",
                thinking = true,
                hybrid = true,
                description = "Built for code editing"
            ),
            ModelInfo(
                "google/gemini-2.5-flash",
                "Gemini 2.5 Flash 🧠",
                thinking = true,
                hybrid = true,
                description = "Fast thinking, generous free tier"
            ),
            ModelInfo(
                "google/gemini-2.5-pro",
                "Gemini 2.5 Pro 🧠",
                thinking = true,
                hybrid = true,
                description = "Deep thinking, most capable"
            ),
            ModelInfo(
                "anthropic/claude-sonnet-4",
                "Claude Sonnet 4 🧠",
                thinking = true,
                hybrid = true,
                description = "Balanced power & speed"
            ),
            ModelInfo(
                "openai/gpt-4o-mini",
                "GPT-4o mini",
                description = "Cheap, fast OpenAI workhorse"
            ),
            ModelInfo(
                "meta-llama/llama-3.3-70b-instruct",
                "Llama 3.3 70B",
                description = "Open-weights all-rounder"
            ),
            ModelInfo(
                "deepseek/deepseek-r1:free",
                "DeepSeek R1 (Free) 🧠",
                thinking = true,
                free = true,
                description = "Free tier — rate limited"
            ),
            ModelInfo(
                "meta-llama/llama-3.3-70b-instruct:free",
                "Llama 3.3 70B (Free)",
                free = true,
                description = "Free tier — rate limited"
            )
        ),
        AiProvider.GEMINI to listOf(
            ModelInfo(
                "gemini-2.5-flash",
                "Gemini 2.5 Flash 🧠",
                thinking = true,
                hybrid = true,
                description = "Best speed/thought balance (default)"
            ),
            ModelInfo(
                "gemini-2.5-pro",
                "Gemini 2.5 Pro 🧠",
                thinking = true,
                hybrid = true,
                description = "Deep thinking, most capable"
            ),
            ModelInfo(
                "gemini-2.5-flash-lite",
                "Gemini 2.5 Flash Lite 🧠",
                thinking = true,
                hybrid = true,
                description = "Cheapest, lowest latency"
            ),
            ModelInfo(
                "gemini-2.0-flash",
                "Gemini 2.0 Flash",
                description = "Previous gen, no thinking"
            ),
            ModelInfo(
                "gemini-2.0-flash-lite",
                "Gemini 2.0 Flash Lite",
                description = "Lightweight previous gen"
            )
        )
    )

    /** Default model when switching provider. */
    fun defaultModel(provider: AiProvider): String = when (provider) {
        AiProvider.GROQ -> "llama-3.3-70b-versatile"
        AiProvider.OPENROUTER -> "deepseek/deepseek-chat-v3.1"
        AiProvider.GEMINI -> "gemini-2.5-flash"
    }

    private val THINKING_HINTS = listOf(
        "gpt-oss", "deepseek-r1", "/o1", "/o3", "/o4", "qwq", "thinking",
        "gemini-2.5", "deepseek-chat-v3.1", "sonnet-4", "grok-4", "gpt-5",
        "qwen3", "r1-0528", "glm-4.5", "minimax-m2"
    )

    /**
     * Heuristic + catalog lookup used to decide whether thinking/reasoning
     * parameters should be attached to a request (also works for custom
     * model ids typed by the user).
     */
    fun isThinkingModel(provider: AiProvider, modelId: String): Boolean {
        val lower = modelId.lowercase()
        return models[provider]?.any { it.id == modelId && it.thinking } == true ||
                THINKING_HINTS.any { lower.contains(it) }
    }

    fun find(provider: AiProvider, modelId: String): ModelInfo? =
        models[provider]?.firstOrNull { it.id == modelId }

    fun isHybridModel(provider: AiProvider, modelId: String): Boolean {
        val lower = modelId.lowercase()
        return find(provider, modelId)?.hybrid == true ||
                lower.contains("qwen3") || lower.contains("gemini-2.5") ||
                lower.contains("deepseek-chat-v3.1") || lower.contains("sonnet-4") ||
                lower.contains("gpt-5")
    }
}

/**
 * Immutable snapshot of everything needed to talk to a provider.
 * Built per-request from preferences + secure storage.
 */
data class AiConfig(
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String,
    val model: String,
    val thinkingEnabled: Boolean = true
) {
    /** Whether reasoning params will actually be attached for this model. */
    val wantsThinking: Boolean
        get() = thinkingEnabled && ModelCatalog.isThinkingModel(provider, model)
}
