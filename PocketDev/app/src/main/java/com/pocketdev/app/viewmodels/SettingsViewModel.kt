package com.pocketdev.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.app.api.AiProvider
import com.pocketdev.app.api.ModelCatalog
import com.pocketdev.app.utils.PreferencesManager
import com.pocketdev.app.utils.SecureStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)
    private val secureStorage = SecureStorage(application)

    val theme: StateFlow<String> = prefsManager.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesManager.THEME_DARK)

    val fontSize: StateFlow<Int> = prefsManager.fontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesManager.DEFAULT_FONT_SIZE)

    val tabSize: StateFlow<Int> = prefsManager.tabSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesManager.DEFAULT_TAB_SIZE)

    val autoSave: StateFlow<Boolean> = prefsManager.autoSave
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autocomplete: StateFlow<Boolean> = prefsManager.autocomplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val ghostSuggestions: StateFlow<Boolean> = prefsManager.ghostSuggestions
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val lineNumbers: StateFlow<Boolean> = prefsManager.lineNumbers
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val wordWrap: StateFlow<Boolean> = prefsManager.wordWrap
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val aiModel: StateFlow<String> = prefsManager.aiModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesManager.DEFAULT_AI_MODEL)

    val aiProvider: StateFlow<AiProvider> = prefsManager.aiProvider
        .map { AiProvider.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiProvider.GROQ)

    val thinkingMode: StateFlow<Boolean> = prefsManager.thinkingMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferencesManager.DEFAULT_THINKING_MODE)

    private val _apiKeyStates = MutableStateFlow<Map<AiProvider, ApiKeyState>>(emptyMap())
    val apiKeyStates: StateFlow<Map<AiProvider, ApiKeyState>> = _apiKeyStates.asStateFlow()

    /** Key status of the currently selected provider. */
    val apiKeyState: StateFlow<ApiKeyState> = combine(aiProvider, apiKeyStates) { provider, states ->
        states[provider] ?: ApiKeyState.NotSet
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ApiKeyState.NotSet)

    init {
        refreshApiKeyStates()
    }

    private fun refreshApiKeyStates() {
        val states = AiProvider.entries.associateWith { provider ->
            if (secureStorage.hasApiKey(provider)) {
                ApiKeyState.Set(maskApiKey(secureStorage.getApiKey(provider)))
            } else {
                ApiKeyState.NotSet
            }
        }
        _apiKeyStates.value = states
    }

    fun setApiKey(provider: AiProvider, key: String) {
        if (key.isBlank()) {
            _apiKeyStates.value = _apiKeyStates.value.toMutableMap().apply {
                put(provider, ApiKeyState.Error("API key cannot be empty"))
            }
            return
        }
        if (!secureStorage.validateApiKey(provider, key)) {
            _apiKeyStates.value = _apiKeyStates.value.toMutableMap().apply {
                put(provider, ApiKeyState.Error(
                    "Invalid API key format. ${provider.displayName} keys look like '${provider.keyPrefixHint}'"
                ))
            }
            return
        }
        secureStorage.setApiKey(provider, key)
        _apiKeyStates.value = _apiKeyStates.value.toMutableMap().apply {
            put(provider, ApiKeyState.Set(maskApiKey(key)))
        }
    }

    fun clearApiKey(provider: AiProvider) {
        secureStorage.clearApiKey(provider)
        _apiKeyStates.value = _apiKeyStates.value.toMutableMap().apply {
            put(provider, ApiKeyState.NotSet)
        }
    }

    /** Switch provider; resets the model to the new provider's default. */
    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            prefsManager.setAiProvider(provider.id)
            prefsManager.setAiModel(ModelCatalog.defaultModel(provider))
        }
    }

    fun setThinkingMode(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setThinkingMode(enabled) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefsManager.setTheme(theme) }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch { prefsManager.setFontSize(size) }
    }

    fun setTabSize(size: Int) {
        viewModelScope.launch { prefsManager.setTabSize(size) }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setAutoSave(enabled) }
    }

    fun setAutocomplete(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setAutocomplete(enabled) }
    }

    fun setGhostSuggestions(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setGhostSuggestions(enabled) }
    }

    fun setLineNumbers(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setLineNumbers(enabled) }
    }

    fun setWordWrap(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setWordWrap(enabled) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { prefsManager.setAiModel(model) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { prefsManager.resetToDefaults() }
    }

    private fun maskApiKey(key: String): String {
        return if (key.length > 8) {
            "${key.take(7)}${"*".repeat((key.length - 11).coerceAtLeast(1))}${key.takeLast(4)}"
        } else "****"
    }

    sealed class ApiKeyState {
        object NotSet : ApiKeyState()
        data class Set(val maskedKey: String) : ApiKeyState()
        data class Error(val message: String) : ApiKeyState()
    }
}
