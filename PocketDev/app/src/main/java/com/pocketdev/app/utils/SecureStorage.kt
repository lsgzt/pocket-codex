package com.pocketdev.app.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pocketdev.app.api.AiProvider

/**
 * Encrypted storage for per-provider API keys.
 *
 * The original "groq_api_key" storage key is preserved so existing users
 * keep their Groq key after upgrading.
 */
class SecureStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "pocket_dev_secure"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(provider: AiProvider): String = when (provider) {
        AiProvider.GROQ -> sharedPreferences.getString(KEY_GROQ_API_KEY, "") ?: ""
        AiProvider.OPENROUTER -> sharedPreferences.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
        AiProvider.GEMINI -> sharedPreferences.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setApiKey(provider: AiProvider, value: String) {
        val key = when (provider) {
            AiProvider.GROQ -> KEY_GROQ_API_KEY
            AiProvider.OPENROUTER -> KEY_OPENROUTER_API_KEY
            AiProvider.GEMINI -> KEY_GEMINI_API_KEY
        }
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun hasApiKey(provider: AiProvider): Boolean = getApiKey(provider).isNotBlank()

    fun clearApiKey(provider: AiProvider) {
        val key = when (provider) {
            AiProvider.GROQ -> KEY_GROQ_API_KEY
            AiProvider.OPENROUTER -> KEY_OPENROUTER_API_KEY
            AiProvider.GEMINI -> KEY_GEMINI_API_KEY
        }
        sharedPreferences.edit().remove(key).apply()
    }

    fun validateApiKey(provider: AiProvider, key: String): Boolean = when (provider) {
        AiProvider.GROQ -> key.startsWith("gsk_") && key.length > 20
        AiProvider.OPENROUTER -> key.startsWith("sk-or-") && key.length > 20
        AiProvider.GEMINI -> key.startsWith("AIza") && key.length > 20
    }

    fun keyPrefixHint(provider: AiProvider): String = provider.keyPrefixHint
}
