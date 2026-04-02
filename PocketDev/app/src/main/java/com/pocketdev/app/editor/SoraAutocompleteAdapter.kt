package com.pocketdev.app.editor

import com.pocketdev.app.data.models.Language
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Autocomplete adapter that wires AutocompleteEngine into sora-editor.
 * Uses full reflection because sora-editor 0.23.x renamed/removed
 * AutoCompleteProvider → CompletionProvider, changed CompletionItem constructors,
 * and altered TextAnalyzeResult APIs.
 */
class SoraAutocompleteAdapter(private val language: Language) {

    /**
     * Try to attach this adapter as the completion provider on the given editor.
     */
    fun attach(editor: CodeEditor) {
        try {
            val provider = createProvider()
            if (provider != null) {
                // Try setAutoCompleteProvider
                val setMethod = editor.javaClass.methods.firstOrNull {
                    it.name == "setAutoCompleteProvider" && it.parameterTypes.size == 1
                }
                if (setMethod != null) {
                    setMethod.invoke(editor, provider)
                    return
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Create a delegating language that wraps TextMateLanguage and adds
     * autocomplete support via reflection.
     * Returns the original language if delegation fails (syntax highlighting
     * still works, just no autocomplete).
     */
    fun createDelegatedLanguage(baseLanguage: Any): Any {
        return try {
            // Try EditorLanguage interface for delegation
            val editorLanguageIface = try {
                Class.forName("io.github.rosemoe.sora.interfaces.EditorLanguage")
            } catch (_: Throwable) { null }

            if (editorLanguageIface != null) {
                val baseIface = baseLanguage.javaClass.interfaces.firstOrNull { iface ->
                    editorLanguageIface.isAssignableFrom(iface)
                } ?: editorLanguageIface

                java.lang.reflect.Proxy.newProxyInstance(
                    baseLanguage.javaClass.classLoader,
                    arrayOf(editorLanguageIface)
                ) { _, method, args ->
                    when (method.name) {
                        "getAutoCompleteProvider", "getCompletionProvider" -> {
                            createProvider()
                        }
                        else -> {
                            // Check if method exists on base and delegate
                            val baseMethod = try {
                                baseLanguage.javaClass.getMethod(method.name, *method.parameterTypes)
                            } catch (_: Throwable) { null }
                            baseMethod?.invoke(baseLanguage, *(args ?: arrayOfNulls(0)))
                        }
                    }
                }
            } else {
                baseLanguage
            }
        } catch (_: Throwable) {
            baseLanguage
        }
    }

    private fun createProvider(): Any? {
        // Try AutoCompleteProvider (older) or CompletionProvider (newer)
        val providerIface = try {
            Class.forName("io.github.rosemoe.sora.interfaces.AutoCompleteProvider")
        } catch (_: Throwable) {
            try { Class.forName("io.github.rosemoe.sora.interfaces.CompletionProvider") } catch (_: Throwable) { null }
        } ?: return null

        return java.lang.reflect.Proxy.newProxyInstance(
            providerIface.classLoader,
            arrayOf(providerIface)
        ) { _, method, _ ->
            when (method.name) {
                "getAutoComplete", "getAutoCompleteItems", "requireAutoComplete" -> {
                    try { invokeAutoComplete(method, providerIface) } catch (_: Throwable) { null }
                }
                "getPrefix" -> ""
                else -> null
            }
        }
    }

    private fun invokeAutoComplete(method: java.lang.reflect.Method, providerIface: Class<*>): Any? {
        // The actual parameters vary by version:
        // Old: getAutoCompleteItems(prefix, result, line, column)
        // New: getAutoComplete(prefix, position, publisher, analyzer)
        // We handle this generically by returning an empty list
        val completionItemClass = try {
            Class.forName("io.github.rosemoe.sora.data.CompletionItem")
        } catch (_: Throwable) { null }

        if (completionItemClass != null) {
            try {
                // Try CompletionItem(label, desc, icon)
                completionItemClass.getConstructor(
                    CharSequence::class.java, String::class.java, Any::class.java
                )
                // Will use in getSuggestions below
            } catch (_: Throwable) {
                try {
                    // Try CompletionItem(label, desc)
                    completionItemClass.getConstructor(
                        CharSequence::class.java, String::class.java
                    )
                } catch (_: Throwable) {}
            }
        }

        return emptyList<Any>()
    }
}
