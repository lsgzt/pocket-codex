package com.pocketdev.app.execution

import com.pocketdev.app.api.AiConfig
import com.pocketdev.app.data.models.Language
import com.pocketdev.app.repository.AiRepository

class AIExecutionAgent(
    private val executionManager: ExecutionManager,
    private val aiRepository: AiRepository,
    private val terminalManager: TerminalManager,
    private val updateCode: (String) -> Unit,
    private val getAiConfig: suspend () -> AiConfig?
) {
    private val MAX_ATTEMPTS = 5

    data class AttemptHistory(
        val code: String,
        val error: String,
        val aiFix: String? = null
    )

    suspend fun runWithAiFix(initialCode: String, language: Language, input: String): com.pocketdev.app.data.models.ExecutionResult? {
        var currentCode = initialCode
        val history = mutableListOf<AttemptHistory>()
        val config = getAiConfig() ?: run {
            terminalManager.appendError(
                "AI API key not set. Please add your key in Settings to use AI auto-fix."
            )
            return null
        }

        terminalManager.appendStatusMessage("Running code...")

        for (attempt in 1..MAX_ATTEMPTS) {
            val result = executionManager.execute(currentCode, language, input)

            if (result.isSuccess) {
                terminalManager.appendOutput(result.output)
                terminalManager.appendStatusMessage("Execution successful.")
                return result
            } else {
                terminalManager.appendError(result.error ?: "Unknown error")

                if (attempt == MAX_ATTEMPTS) {
                    terminalManager.appendAgentMessage("AI could not fix the code automatically.")
                    return result
                }

                terminalManager.appendAgentMessage("Error detected. Asking AI to fix...\nAI attempt $attempt/$MAX_ATTEMPTS")

                // Record history before AI fix
                val currentAttempt = AttemptHistory(currentCode, result.error ?: "Unknown error")
                history.add(currentAttempt)

                val historyText = history.takeLast(2).joinToString("\n---\n") {
                    "Error:\n${it.error}\nAI Fix:\n${it.aiFix ?: "None"}"
                }

                val aiResult = aiRepository.autoFixCode(currentCode, result.error ?: "Unknown error", historyText, language, config)

                if (aiResult.isSuccess && aiResult.patches.isNotEmpty()) {
                    // Apply patches
                    val lines = currentCode.lines().toMutableList()
                    val sortedPatches = aiResult.patches.sortedByDescending { it.editStart }

                    for (patch in sortedPatches) {
                        val startIndex = maxOf(0, patch.editStart - 1)
                        val endIndex = minOf(lines.size, patch.editEnd)

                        if (startIndex <= endIndex) {
                            for (i in startIndex until endIndex) {
                                if (startIndex < lines.size) {
                                    lines.removeAt(startIndex)
                                }
                            }
                            val newLines = patch.newCode.lines()
                            lines.addAll(startIndex, newLines)
                        }
                    }

                    val newCode = lines.joinToString("\n")

                    // Update history with AI fix
                    history[history.size - 1] = currentAttempt.copy(aiFix = newCode)

                    // Show the model's reasoning (thinking models) if available
                    val reasoning = aiResult.reasoning
                    if (!reasoning.isNullOrBlank()) {
                        terminalManager.appendAgentMessage("🧠 AI Thought Process:\n${reasoning.take(1500)}")
                    }

                    val thoughtProcess = aiResult.content.split("FILE:").firstOrNull()?.trim() ?: ""
                    if (thoughtProcess.isNotBlank()) {
                        terminalManager.appendAgentMessage("AI Reasoning:\n$thoughtProcess")
                    }

                    terminalManager.appendAgentMessage("AI Applied Fixes.")

                    currentCode = newCode
                    updateCode(currentCode)
                    terminalManager.appendStatusMessage("Running updated code...")
                } else {
                    terminalManager.appendAgentMessage("AI failed to generate a fix: ${aiResult.errorMessage}")
                    return result
                }
            }
        }
        return null
    }
}
