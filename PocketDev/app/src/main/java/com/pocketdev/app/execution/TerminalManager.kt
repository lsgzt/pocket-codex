package com.pocketdev.app.execution

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.LinkedBlockingQueue

enum class TerminalMessageType {
    NORMAL, ERROR, AGENT, STATUS, INPUT_PROMPT
}

data class TerminalMessage(
    val id: Long,
    val text: String,
    val type: TerminalMessageType
)

class TerminalManager {
    private companion object {
        const val MAX_MESSAGES = 400
    }

    private val _messages = MutableStateFlow<List<TerminalMessage>>(emptyList())
    val messages: StateFlow<List<TerminalMessage>> = _messages.asStateFlow()
    private var nextMessageId = 0L

    // Interactive input support
    private val _isWaitingForInput = MutableStateFlow(false)
    val isWaitingForInput: StateFlow<Boolean> = _isWaitingForInput.asStateFlow()

    val inputQueue = LinkedBlockingQueue<String>()

    private fun appendMessage(text: String, type: TerminalMessageType) {
        _messages.update { current ->
            val next = current + TerminalMessage(
                id = nextMessageId++,
                text = text,
                type = type
            )
            if (next.size > MAX_MESSAGES) next.takeLast(MAX_MESSAGES) else next
        }
    }

    fun appendOutput(text: String) {
        val trimmed = text.trimEnd()
        if (trimmed.isNotEmpty()) {
            appendMessage(trimmed, TerminalMessageType.NORMAL)
        }
    }

    fun appendError(text: String) {
        val trimmed = text.trimEnd()
        if (trimmed.isNotEmpty()) {
            appendMessage(trimmed, TerminalMessageType.ERROR)
        }
    }

    fun appendAgentMessage(text: String) {
        if (text.isNotEmpty()) {
            appendMessage(text, TerminalMessageType.AGENT)
        }
    }

    fun appendStatusMessage(text: String) {
        if (text.isNotEmpty()) {
            appendMessage(text, TerminalMessageType.STATUS)
        }
    }

    fun appendInputPrompt(text: String) {
        appendMessage(text, TerminalMessageType.INPUT_PROMPT)
    }

    fun requestInput(prompt: String): String {
        if (prompt.isNotEmpty()) {
            appendInputPrompt(prompt)
        }
        _isWaitingForInput.value = true
        // Block until input is provided
        val input = inputQueue.take()
        _isWaitingForInput.value = false
        appendOutput("> $input")
        return input
    }

    fun sendInput(input: String) {
        inputQueue.put(input)
    }

    fun clearTerminal() {
        _messages.value = emptyList()
        inputQueue.clear()
        _isWaitingForInput.value = false
    }
}
