package com.pocketdev.app.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.app.data.models.*
import com.pocketdev.app.execution.ExecutionManager
import com.pocketdev.app.api.AiConfig
import com.pocketdev.app.api.AiProvider
import com.pocketdev.app.repository.AiRepository
import com.pocketdev.app.repository.ChatStreamEvent
import com.pocketdev.app.repository.ProjectRepository
import com.pocketdev.app.utils.NetworkUtils
import com.pocketdev.app.utils.PreferencesManager
import com.pocketdev.app.utils.SecureStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first

import com.pocketdev.app.execution.TerminalManager
import com.pocketdev.app.execution.AIExecutionAgent
import com.pocketdev.app.execution.TerminalMessage
import com.pocketdev.app.execution.TerminalMessageType

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val projectRepository = ProjectRepository(context)
    private val aiRepository = AiRepository()
    private val executionManager = ExecutionManager(context)
    private val secureStorage = SecureStorage(context)
    private val prefsManager = PreferencesManager(context)

    val terminalManager = TerminalManager()
    private val aiExecutionAgent = AIExecutionAgent(
        executionManager = executionManager,
        aiRepository = aiRepository,
        terminalManager = terminalManager,
        updateCode = { newCode -> updateCode(newCode) },
        getAiConfig = { resolveAiConfig(reportError = false) }
    )

    // Current editor state
    private val _currentFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val currentFiles: StateFlow<List<ProjectFile>> = _currentFiles.asStateFlow()

    private val _activeFileIndex = MutableStateFlow(0)
    val activeFileIndex: StateFlow<Int> = _activeFileIndex.asStateFlow()

    private val _currentCode = MutableStateFlow("")
    val currentCode: StateFlow<String> = _currentCode.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Language.PYTHON)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId: StateFlow<Long?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow("Untitled")
    val currentProjectName: StateFlow<String> = _currentProjectName.asStateFlow()

    // Execution state
    private val _executionState = MutableStateFlow<UiState<ExecutionResult>>(UiState.Idle)
    val executionState: StateFlow<UiState<ExecutionResult>> = _executionState.asStateFlow()

    private val _htmlContent = MutableStateFlow<String?>(null)
    val htmlContent: StateFlow<String?> = _htmlContent.asStateFlow()

    private val _stdInput = MutableStateFlow("")
    val stdInput: StateFlow<String> = _stdInput.asStateFlow()

    fun updateStdInput(input: String) {
        _stdInput.value = input
    }

    // AI state
    private val _aiState = MutableStateFlow<UiState<AiResult>>(UiState.Idle)
    val aiState: StateFlow<UiState<AiResult>> = _aiState

    // Live streaming state (thinking + answer preview) for the loading dialog
    private val _aiStreamState = MutableStateFlow<AiStreamState?>(null)
    val aiStreamState: StateFlow<AiStreamState?> = _aiStreamState

    private val _ghostSuggestion = MutableStateFlow<String?>(null)
    val ghostSuggestion: StateFlow<String?> = _ghostSuggestion

    // Inline diff suggestion (for REPLACE type edits)
    private val _inlineDiffSuggestion = MutableStateFlow<AiResult?>(null)
    val inlineDiffSuggestion: StateFlow<AiResult?> = _inlineDiffSuggestion

    // Keep for backward compatibility but mark as deprecated
    @Deprecated("Use inlineDiffSuggestion instead")
    private val _diffSuggestion = MutableStateFlow<AiResult?>(null)
    val diffSuggestion: StateFlow<AiResult?> = _diffSuggestion

    private var ghostSuggestionJob: Job? = null

    companion object {
        /** UI stream snapshots are throttled so fast reasoning models can't jank the UI. */
        private const val STREAM_UI_THROTTLE_MS = 80L
        /** Caps for text shown live in the AI progress dialog (tail is kept). */
        private const val REASONING_UI_CAP = 6000
        private const val ANSWER_UI_CAP = 12000
        /** AI ghost suggestions are skipped for files larger than this. */
        private const val GHOST_MAX_FILE_CHARS = 80_000
    }

    // Save state
    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    private var lastAiPrompt = ""
    private var lastAiResult = ""

    private var hasUnsavedChanges = false

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Projects list now uses SQL-backed search/summaries.
    val allProjects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredProjects: StateFlow<List<Project>> = _searchQuery
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) projectRepository.getAllProjects()
            else projectRepository.searchProjects(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            val lastProjectId = prefsManager.lastProjectId.first()
            val unsavedCode = prefsManager.unsavedCode.first()
            val unsavedLanguage = prefsManager.unsavedLanguage.first()

            if (lastProjectId != -1L) {
                val project = projectRepository.getProjectById(lastProjectId)
                if (project != null) {
                    applyLoadedProject(project)
                    if (unsavedCode != null) {
                        _currentCode.value = unsavedCode
                        hasUnsavedChanges = true
                    }
                } else {
                    newFile()
                    if (unsavedCode != null) {
                        _currentCode.value = unsavedCode
                        hasUnsavedChanges = true
                    }
                }
            } else {
                newFile()
                if (unsavedCode != null) {
                    _currentCode.value = unsavedCode
                    hasUnsavedChanges = true
                }
                if (unsavedLanguage != null) {
                    try {
                        _currentLanguage.value = Language.valueOf(unsavedLanguage)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    private var syncFilesJob: Job? = null
    private var saveJob: kotlinx.coroutines.Job? = null

    fun updateCode(code: String) {
        if (_currentCode.value == code) return
        _currentCode.value = code
        hasUnsavedChanges = true
        
        // Clear ghost suggestion but do not cancel the job, so it can run after typing
        _ghostSuggestion.value = null

        val activeIndexSnapshot = _activeFileIndex.value
        val activeFileSnapshot = _currentFiles.value.getOrNull(activeIndexSnapshot)
        val activeFileIdSnapshot = activeFileSnapshot?.id
        val languageSnapshot = _currentLanguage.value

        // Sync to files list only after a short delay to avoid excessive recompositions
        // in UI components observing files (like tabs)
        syncFilesJob?.cancel()
        syncFilesJob = viewModelScope.launch {
            delay(500)
            val files = _currentFiles.value
            val activeIndex = activeIndexSnapshot
            if (
                activeIndex in files.indices &&
                files[activeIndex].id == activeFileIdSnapshot &&
                files[activeIndex].code != code
            ) {
                val updated = files.toMutableList()
                updated[activeIndex] = updated[activeIndex].copy(code = code)
                _currentFiles.value = updated
            }
        }

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(2000)
            val projectId = _currentProjectId.value
            if (projectId == null) {
                prefsManager.setUnsavedCode(code)
            } else {
                prefsManager.setUnsavedCode(null)
                if (!activeFileIdSnapshot.isNullOrBlank()) {
                    runCatching {
                        projectRepository.updateFileContent(
                            projectId = projectId,
                            fileExternalId = activeFileIdSnapshot,
                            code = code,
                            language = languageSnapshot
                        )
                    }
                }
            }
        }
    }

    fun switchFile(index: Int) {
        if (index in _currentFiles.value.indices) {
            syncCurrentCodeToActiveFile()
            _activeFileIndex.value = index
            val file = _currentFiles.value[index]
            _currentCode.value = file.code
            _currentLanguage.value = file.language
            rejectGhostSuggestion()
        }
    }

    fun addFile(name: String, language: Language) {
        val newFile = ProjectFile(name = name, language = language, code = getDefaultCode(language))
        val updatedFiles = _currentFiles.value + newFile
        _currentFiles.value = updatedFiles
        switchFile(updatedFiles.size - 1)
        hasUnsavedChanges = true
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        viewModelScope.launch {
            prefsManager.setUnsavedLanguage(language.name)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun applyLoadedProject(project: Project) {
        _currentFiles.value = if (project.files.isEmpty()) {
            val fallbackCode = if (project.code.isNotBlank()) project.code else getDefaultCode(project.language)
            listOf(ProjectFile(name = "main${project.language.extension}", language = project.language, code = fallbackCode))
        } else {
            project.files
        }

        _activeFileIndex.value = 0
        _currentCode.value = _currentFiles.value.first().code
        _currentLanguage.value = _currentFiles.value.first().language
        _currentProjectId.value = project.id
        _currentProjectName.value = project.name
        _executionState.value = UiState.Idle
        _htmlContent.value = null
        hasUnsavedChanges = false

        viewModelScope.launch {
            prefsManager.setLastProjectId(project.id)
            prefsManager.setUnsavedCode(null)
            prefsManager.setUnsavedLanguage(null)
        }
    }

    fun loadProject(project: Project) {
        viewModelScope.launch {
            val hydrated = if (project.files.isEmpty() && project.id != 0L) {
                projectRepository.getProjectById(project.id) ?: project
            } else {
                project
            }
            applyLoadedProject(hydrated)
        }
    }

    fun newFile(language: Language = Language.PYTHON) {
        val defaultCode = getDefaultCode(language)
        val defaultFileName = "main${language.extension}"
        val initialFile = ProjectFile(name = defaultFileName, language = language, code = defaultCode)
        _currentFiles.value = listOf(initialFile)
        _activeFileIndex.value = 0
        _currentCode.value = defaultCode
        _currentLanguage.value = language
        _currentProjectId.value = null
        _currentProjectName.value = "Untitled"
        _executionState.value = UiState.Idle
        _htmlContent.value = null
        hasUnsavedChanges = false
        viewModelScope.launch {
            prefsManager.setLastProjectId(-1L)
            prefsManager.setUnsavedCode(null)
            prefsManager.setUnsavedLanguage(null)
        }
    }

    fun runCode() {
        val code = _currentCode.value
        val language = _currentLanguage.value

        if (code.isBlank()) {
            terminalManager.appendError("No code to execute. Write some code first!")
            return
        }

        if (!executionManager.isExecutable(language)) {
            terminalManager.appendError(
                "${language.displayName} execution is not supported.\n" +
                        "Executable languages: Python, JavaScript, HTML"
            )
            return
        }

        viewModelScope.launch {
            terminalManager.clearTerminal()
            terminalManager.appendStatusMessage("Running script...")
            
            // Use interactive execution for Python and JavaScript
            val result = if (language == Language.PYTHON || language == Language.JAVASCRIPT) {
                executionManager.executeInteractive(code, language, terminalManager)
            } else {
                val execResult = executionManager.execute(code, language)
                if (execResult.isSuccess) {
                    terminalManager.appendOutput(execResult.output)
                }
                execResult
            }

            if (result.isSuccess) {
                terminalManager.appendStatusMessage("Execution successful.")
                if (language == Language.HTML) {
                    _htmlContent.value = result.output
                }
            } else {
                terminalManager.appendError(result.error ?: "Unknown error")
            }
            
            _executionState.value = UiState.Success(result)
        }
    }

    fun sendTerminalInput(input: String) {
        terminalManager.sendInput(input)
    }

    fun runWithAiFix() {
        val code = _currentCode.value
        val language = _currentLanguage.value
        val input = _stdInput.value

        if (code.isBlank()) {
            terminalManager.appendError("No code to execute. Write some code first!")
            return
        }

        if (!executionManager.isExecutable(language)) {
            terminalManager.appendError(
                "${language.displayName} execution is not supported.\n" +
                        "Executable languages: Python, JavaScript, HTML"
            )
            return
        }

        viewModelScope.launch {
            terminalManager.clearTerminal()
            _executionState.value = UiState.Loading
            val result = aiExecutionAgent.runWithAiFix(code, language, input)
            if (result != null) {
                if (language == Language.HTML && result.isSuccess) {
                    _htmlContent.value = result.output
                }
                _executionState.value = UiState.Success(result)
            } else {
                _executionState.value = UiState.Idle
            }
        }
    }

    fun clearOutput() {
        _executionState.value = UiState.Idle
        _htmlContent.value = null
    }

    fun saveProject(name: String? = null) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            try {
                val projectName = name ?: _currentProjectName.value
                val existingId = _currentProjectId.value

                saveJob?.cancel()
                syncFilesJob?.cancel()
                syncCurrentCodeToActiveFile()
                val project = Project(
                    id = existingId ?: 0,
                    name = projectName,
                    language = _currentLanguage.value,
                    code = _currentCode.value,
                    files = _currentFiles.value,
                    modifiedAt = System.currentTimeMillis()
                )

                val savedId = projectRepository.saveProject(project)
                _currentProjectId.value = savedId
                _currentProjectName.value = projectName
                hasUnsavedChanges = false
                _saveState.value = UiState.Success(Unit)
                prefsManager.setLastProjectId(savedId)
                prefsManager.setUnsavedCode(null)
                prefsManager.setUnsavedLanguage(null)
            } catch (e: Exception) {
                _saveState.value = UiState.Error("Failed to save: ${e.message}")
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(project)
                if (_currentProjectId.value == project.id) {
                    newFile()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            try {
                projectRepository.duplicateProject(project)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    // AI Features
    fun fixBug() {
        callAiFeature { files, activeFileName, config, onEvent ->
            aiRepository.fixBug(files, activeFileName, config, onEvent)
        }
    }

    fun explainCode() {
        val files = _currentFiles.value
        val activeFileName = if (files.isNotEmpty()) files[_activeFileIndex.value].name else ""
        val code = _currentCode.value
        val language = _currentLanguage.value
        lastAiPrompt = buildString {
            append("Explain this ${language.displayName} code in simple, beginner-friendly terms.\n\n")
            append("```${language.displayName.lowercase()}\n$code\n```\n\n")
            append("Break down what each part does step-by-step. ")
            append("Use simple language suitable for students who are learning to code. ")
            append("Include:\n")
            append("1. What the code does overall\n")
            append("2. A step-by-step breakdown of each part\n")
            append("3. Any important concepts used\n")
            append("4. Tips for beginners")
        }
        callAiFeature { f, a, config, onEvent ->
            val result = aiRepository.explainCode(f, a, config, onEvent)
            lastAiResult = result.content
            result
        }
    }

    fun askFollowUpQuestion(question: String) {
        viewModelScope.launch {
            _aiState.value = UiState.Loading
            val config = resolveAiConfig() ?: return@launch
            val handler = makeStreamHandler(config)
            try {
                val result = aiRepository.askFollowUp(lastAiPrompt, lastAiResult, question, config, handler)
                if (result.isSuccess) {
                    lastAiResult = lastAiResult + "\n\n**Q: $question**\n\n" + result.content
                    _aiState.value = UiState.Success(result.copy(content = lastAiResult))
                } else {
                    _aiState.value = UiState.Error(result.errorMessage ?: "Failed to get follow-up answer")
                }
            } finally {
                _aiStreamState.value = null
            }
        }
    }

    fun improveCode() {
        callAiFeature { files, activeFileName, config, onEvent ->
            aiRepository.improveCode(files, activeFileName, config, onEvent)
        }
    }

    fun writeCodeWithAi(prompt: String) {
        val files = _currentFiles.value

        if (files.isEmpty()) {
            _aiState.value = UiState.Error("No code to analyze. Write some code first!")
            return
        }

        viewModelScope.launch {
            _aiState.value = UiState.Loading
            val config = resolveAiConfig() ?: return@launch
            val handler = makeStreamHandler(config)
            try {
                val activeFileName = if (files.isNotEmpty()) files[_activeFileIndex.value].name else "main.py"
                val result = aiRepository.modifyCode(prompt, files, activeFileName, config, handler)
                if (result.isSuccess) {
                    if (result.patches.isNotEmpty()) {
                        _aiState.value = UiState.Success(result.copy(isEdit = true))
                    } else {
                        _aiState.value = UiState.Success(result.copy(isEdit = false))
                    }
                } else {
                    _aiState.value = UiState.Error(result.errorMessage ?: "AI request failed to generate patches")
                }
            } finally {
                _aiStreamState.value = null
            }
        }
    }

    fun editCodeWithAi(prompt: String) {
        val files = _currentFiles.value

        if (files.isEmpty()) {
            _aiState.value = UiState.Error("No code to analyze. Write some code first!")
            return
        }

        viewModelScope.launch {
            _aiState.value = UiState.Loading
            val config = resolveAiConfig() ?: return@launch
            val handler = makeStreamHandler(config)
            try {
                val activeFileName = if (files.isNotEmpty()) files[_activeFileIndex.value].name else ""
                val result = aiRepository.editCode(prompt, files, activeFileName, config, handler)
                if (result.isSuccess) {
                    if (result.patches.isNotEmpty() && result.isEdit) {
                        _aiState.value = UiState.Success(result)
                    } else {
                        // Show the explanation in the AiResultDialog
                        _aiState.value = UiState.Success(result.copy(isEdit = false))
                    }
                } else {
                    _aiState.value = UiState.Error(result.errorMessage ?: "AI response did not contain valid patches")
                }
            } finally {
                _aiStreamState.value = null
            }
        }
    }

    fun getPreviewCode(fileName: String, result: AiResult): String {
        val file = _currentFiles.value.find { it.name == fileName } ?: return ""
        val patches = result.patches.filter { it.fileName == fileName }
        if (patches.isEmpty()) return file.code
        
        val lines = file.code.lines().toMutableList()
        // Apply patches in reverse order to avoid index shifting
        val sortedPatches = patches.sortedByDescending { it.editStart }
        
        for (patch in sortedPatches) {
            val startIndex = maxOf(0, patch.editStart - 1)
            val endIndex = minOf(lines.size, patch.editEnd)
            
            if (startIndex <= endIndex) {
                // Remove old lines
                for (i in startIndex until endIndex) {
                    if (startIndex < lines.size) {
                        lines.removeAt(startIndex)
                    }
                }
                
                // Insert new lines
                val newLines = patch.newCode.lines()
                lines.addAll(startIndex, newLines)
            }
        }
        
        return lines.joinToString("\n")
    }

    fun requestGhostSuggestion(cursorPosition: Int) {
        ghostSuggestionJob?.cancel()
        ghostSuggestionJob = viewModelScope.launch {
            if (!prefsManager.ghostSuggestions.first()) return@launch
            delay(500) // Wait for 500ms of inactivity for better UX
            val code = _currentCode.value
            val language = _currentLanguage.value
            // Skip AI completions for very large files — latency & token cost
            // outweigh the value of a one-word suggestion, and building the
            // prompt would copy huge strings on every idle period.
            if (code.length > GHOST_MAX_FILE_CHARS) return@launch
            if (!NetworkUtils.isOnline(context)) return@launch
            val config = resolveAiConfig(reportError = false) ?: return@launch

            val result = aiRepository.getGhostSuggestion(code, cursorPosition, language, config)
            if (result.isSuccess && result.content.isNotBlank()) {
                if (result.isEdit && result.deleteText != null && result.addText != null) {
                    // REPLACE type with inline diff - show directly in editor
                    _ghostSuggestion.value = null
                    _inlineDiffSuggestion.value = result
                    _diffSuggestion.value = null // No popup
                } else {
                    // APPEND type - simple ghost text in gray
                    _ghostSuggestion.value = result.content
                    _inlineDiffSuggestion.value = null
                    _diffSuggestion.value = null
                }
            }
        }
    }

    fun acceptGhostSuggestion(cursorPosition: Int) {
        val suggestion = _ghostSuggestion.value ?: return
        val code = _currentCode.value
        val newCode = code.substring(0, cursorPosition) + suggestion + code.substring(cursorPosition)
        updateCode(newCode)
        _ghostSuggestion.value = null
    }

    fun acceptGhostSuggestionLine(cursorPosition: Int): Int {
        val suggestion = _ghostSuggestion.value ?: return 0
        val code = _currentCode.value
        
        // Find the first newline in the suggestion
        val newlineIndex = suggestion.indexOf('\n')
        val lineToAccept = if (newlineIndex != -1) {
            suggestion.substring(0, newlineIndex + 1)
        } else {
            suggestion
        }
        
        val newCode = code.substring(0, cursorPosition) + lineToAccept + code.substring(cursorPosition)
        updateCode(newCode)
        
        // Update the remaining suggestion
        if (newlineIndex != -1 && newlineIndex + 1 < suggestion.length) {
            _ghostSuggestion.value = suggestion.substring(newlineIndex + 1)
        } else {
            _ghostSuggestion.value = null
        }
        
        return lineToAccept.length
    }

    fun acceptDiffSuggestion(cursorPosition: Int) {
        val suggestion = _diffSuggestion.value ?: return
        val code = _currentCode.value
        
        if (suggestion.isEdit) {
            // For REPLACE type, we need to replace the current line or block
            // A simple implementation: replace the current line
            val lines = code.lines()
            var currentLineIndex = 0
            var charCount = 0
            for (i in lines.indices) {
                charCount += lines[i].length + 1 // +1 for newline
                if (charCount > cursorPosition) {
                    currentLineIndex = i
                    break
                }
            }
            
            val newLines = lines.toMutableList()
            newLines[currentLineIndex] = suggestion.content
            updateCode(newLines.joinToString("\n"))
        } else {
            // For APPEND type but > 2 lines
            val newCode = code.substring(0, cursorPosition) + suggestion.content + code.substring(cursorPosition)
            updateCode(newCode)
        }
        
        _diffSuggestion.value = null
    }

    fun rejectDiffSuggestion() {
        _diffSuggestion.value = null
        _inlineDiffSuggestion.value = null
    }

    fun expandGhostToDiff() {
        val suggestion = _ghostSuggestion.value ?: return
        _ghostSuggestion.value = null
        _diffSuggestion.value = AiResult(
            content = suggestion,
            isEdit = false // It's an APPEND type since it was a ghost suggestion
        )
    }

    fun acceptInlineDiffSuggestion(): Int {
        val suggestion = _inlineDiffSuggestion.value ?: return 0
        val code = _currentCode.value
        
        if (suggestion.deleteText != null && suggestion.addText != null) {
            // Replace the deleteText with addText
            val deletePos = suggestion.editStartPos
            val newCode = code.substring(0, deletePos) + suggestion.addText + code.substring(suggestion.editEndPos)
            updateCode(newCode)
            _inlineDiffSuggestion.value = null
            return suggestion.addText.length
        }
        _inlineDiffSuggestion.value = null
        return 0
    }

    fun rejectGhostSuggestion() {
        ghostSuggestionJob?.cancel()
        _ghostSuggestion.value = null
        _diffSuggestion.value = null
        _inlineDiffSuggestion.value = null
    }

    fun clearGhostSuggestion() {
        _ghostSuggestion.value = null
        _diffSuggestion.value = null
        _inlineDiffSuggestion.value = null
    }

    fun applyAiEdit(result: AiResult) {
        val patches = result.patches
        if (patches.isEmpty()) return
        
        val currentFiles = _currentFiles.value.toMutableList()
        
        // Group patches by file
        val patchesByFile = patches.groupBy { it.fileName }
        
        for ((fileName, filePatches) in patchesByFile) {
            val fileIndex = currentFiles.indexOfFirst { it.name == fileName }
            if (fileIndex != -1) {
                val file = currentFiles[fileIndex]
                val lines = file.code.lines().toMutableList()
                
                // Apply patches in reverse order to avoid index shifting
                val sortedPatches = filePatches.sortedByDescending { it.editStart }
                
                for (patch in sortedPatches) {
                    // Convert 1-based line numbers to 0-based indices
                    val startIndex = maxOf(0, patch.editStart - 1)
                    val endIndex = minOf(lines.size, patch.editEnd)
                    
                    if (startIndex <= endIndex) {
                        // Remove old lines
                        for (i in startIndex until endIndex) {
                            if (startIndex < lines.size) {
                                lines.removeAt(startIndex)
                            }
                        }
                        
                        // Insert new lines
                        val newLines = patch.newCode.lines()
                        lines.addAll(startIndex, newLines)
                    }
                }
                
                val newCode = lines.joinToString("\n")
                currentFiles[fileIndex] = file.copy(code = newCode)
            }
        }
        
        _currentFiles.value = currentFiles
        
        // Update current code if active file was modified
        val activeIndex = _activeFileIndex.value
        if (activeIndex in currentFiles.indices) {
            _currentCode.value = currentFiles[activeIndex].code
        }
        
        clearGhostSuggestion()
        
        hasUnsavedChanges = true
        saveProject()
        dismissAiResult()
    }

    private fun callAiFeature(
        action: suspend (List<com.pocketdev.app.data.models.ProjectFile>, String, AiConfig, (ChatStreamEvent) -> Unit) -> AiResult
    ) {
        val files = _currentFiles.value
        val activeFileName = if (files.isNotEmpty()) files[_activeFileIndex.value].name else ""

        if (files.isEmpty()) {
            _aiState.value = UiState.Error("No code to analyze. Write some code first!")
            return
        }

        viewModelScope.launch {
            _aiState.value = UiState.Loading
            val config = resolveAiConfig() ?: return@launch
            val handler = makeStreamHandler(config)
            try {
                val result = action(files, activeFileName, config, handler)
                _aiState.value = if (result.isSuccess) {
                    UiState.Success(result)
                } else {
                    UiState.Error(result.errorMessage ?: "AI request failed")
                }
            } catch (e: Exception) {
                _aiState.value = UiState.Error(e.message ?: "AI request failed")
            } finally {
                _aiStreamState.value = null
            }
        }
    }

    fun chatWithAi(message: String) {
        val files = _currentFiles.value
        val activeFileName = if (files.isNotEmpty()) files[_activeFileIndex.value].name else "main.py"

        terminalManager.appendOutput("> $message")
        terminalManager.appendStatusMessage("AI is thinking...")

        viewModelScope.launch {
            val config = resolveAiConfig(reportError = false)
            if (config == null) {
                if (!NetworkUtils.isOnline(context)) {
                    terminalManager.appendError("No internet connection.")
                } else {
                    val provider = AiProvider.fromId(prefsManager.aiProvider.first())
                    terminalManager.appendError(
                        "${provider.displayName} API key not set. Please add it in Settings."
                    )
                }
                return@launch
            }

            // Stream the answer live into a dedicated terminal message.
            val messageId = terminalManager.startStreamingAgent()
            val reasoning = StringBuilder()
            val answer = StringBuilder()
            var phase = AiStreamPhase.CONNECTING
            var lastPush = 0L

            fun snapshot(): String = buildString {
                if (phase == AiStreamPhase.THINKING || reasoning.isNotEmpty()) {
                    if (answer.isEmpty()) {
                        append("🧠 Thinking…\n")
                        append(reasoning.takeLast(1800))
                    } else {
                        append("🧠 Thought process (tail):\n")
                        append(reasoning.takeLast(900))
                        append("\n———\n")
                    }
                }
                if (answer.isNotEmpty()) {
                    if (reasoning.isNotEmpty()) append("\n")
                    append(answer.takeLast(3500))
                }
            }

            try {
                val result = aiRepository.editCode(message, files, activeFileName, config) { event ->
                    when (event) {
                        ChatStreamEvent.Started -> {}
                        is ChatStreamEvent.Thinking -> {
                            phase = AiStreamPhase.THINKING
                            reasoning.append(event.delta)
                        }
                        is ChatStreamEvent.Answer -> {
                            phase = AiStreamPhase.ANSWERING
                            answer.append(event.delta)
                        }
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastPush >= STREAM_UI_THROTTLE_MS) {
                        lastPush = now
                        terminalManager.updateStreamingAgent(messageId, snapshot())
                    }
                }
                // Final message: reasoning summary + full answer
                terminalManager.updateStreamingAgent(messageId, buildString {
                    if (result.reasoning != null && result.reasoning.isNotBlank()) {
                        append("🧠 Thought process:\n")
                        append(result.reasoning.takeLast(2000))
                        append("\n\n———\n\n")
                    }
                    append(if (result.isSuccess) result.content else (result.errorMessage ?: "AI request failed"))
                })
            } catch (e: Exception) {
                terminalManager.updateStreamingAgent(
                    messageId,
                    "AI request failed: ${e.message ?: "unknown error"}"
                )
            }
        }
    }

    fun applyAiCode(code: String) {
        updateCode(code)
        _aiState.value = UiState.Idle
    }

    fun dismissAiResult() {
        _aiState.value = UiState.Idle
    }

    /**
     * Resolves the current AI settings (provider / key / model / thinking mode)
     * into an [AiConfig] snapshot, or reports a friendly error to [_aiState].
     */
    private suspend fun resolveAiConfig(reportError: Boolean = true): AiConfig? {
        if (!NetworkUtils.isOnline(context)) {
            if (reportError) {
                _aiState.value = UiState.Error(
                    "No internet connection.\nPlease connect to the internet to use AI features."
                )
            }
            return null
        }
        val provider = AiProvider.fromId(prefsManager.aiProvider.first())
        val apiKey = secureStorage.getApiKey(provider)
        if (apiKey.isBlank()) {
            if (reportError) {
                _aiState.value = UiState.Error(
                    "${provider.displayName} API key not set.\n" +
                            "Please add your API key in Settings to use AI features."
                )
            }
            return null
        }
        return AiConfig(
            provider = provider,
            apiKey = apiKey,
            model = prefsManager.aiModel.first(),
            thinkingEnabled = prefsManager.thinkingMode.first()
        )
    }

    /**
     * Builds a live-stream handler that feeds [_aiStreamState] (rendered by the
     * AI progress dialog in the editor) with throttled, tail-capped snapshots,
     * so even the fastest reasoning models can never jank the UI thread.
     */
    private fun makeStreamHandler(config: AiConfig): (ChatStreamEvent) -> Unit {
        val reasoning = StringBuilder()
        val answer = StringBuilder()
        var phase = AiStreamPhase.CONNECTING
        var lastPush = 0L

        _aiStreamState.value = AiStreamState(
            provider = config.provider,
            model = config.model,
            thinkingEnabled = config.wantsThinking,
            phase = phase
        )

        return { event ->
            when (event) {
                ChatStreamEvent.Started -> {}
                is ChatStreamEvent.Thinking -> {
                    phase = AiStreamPhase.THINKING
                    reasoning.append(event.delta)
                }
                is ChatStreamEvent.Answer -> {
                    phase = AiStreamPhase.ANSWERING
                    answer.append(event.delta)
                }
            }
            val now = System.currentTimeMillis()
            if (now - lastPush >= STREAM_UI_THROTTLE_MS) {
                lastPush = now
                _aiStreamState.value = AiStreamState(
                    provider = config.provider,
                    model = config.model,
                    thinkingEnabled = config.wantsThinking,
                    phase = phase,
                    reasoning = reasoning.toString().takeLast(REASONING_UI_CAP),
                    answer = answer.toString().takeLast(ANSWER_UI_CAP)
                )
            }
        }
    }

    private fun syncCurrentCodeToActiveFile() {
        val files = _currentFiles.value
        val activeIndex = _activeFileIndex.value
        if (activeIndex !in files.indices) return

        val current = files[activeIndex]
        val code = _currentCode.value
        if (current.code == code && current.language == _currentLanguage.value) return

        val updated = files.toMutableList()
        updated[activeIndex] = current.copy(code = code, language = _currentLanguage.value)
        _currentFiles.value = updated
    }

    private fun getDefaultCode(language: Language): String {
        return when (language) {
            Language.PYTHON -> "# Python\nprint(\"Hello, World!\")\n"
            Language.JAVASCRIPT -> "// JavaScript\nconsole.log(\"Hello, World!\");\n"
            Language.HTML -> "<!DOCTYPE html>\n<html>\n<head><title>My Page</title></head>\n<body>\n    <h1>Hello, World!</h1>\n</body>\n</html>"
            Language.CSS -> "/* CSS */\nbody {\n    font-family: Arial, sans-serif;\n    background: #f5f5f5;\n}\n"
            Language.JAVA -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n"
            Language.CPP -> "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello, World!\" << endl;\n    return 0;\n}\n"
            Language.KOTLIN -> "fun main() {\n    println(\"Hello, World!\")\n}\n"
            Language.JSON -> "{\n    \"message\": \"Hello, World!\"\n}\n"
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
        syncFilesJob?.cancel()
        ghostSuggestionJob?.cancel()
    }
}
