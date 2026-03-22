package com.pocketdev.app.ui.screens

import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pocketdev.app.data.models.*
import com.pocketdev.app.editor.AutocompleteEngine
import com.pocketdev.app.editor.AutocompleteItem
import com.pocketdev.app.viewmodels.EditorViewModel
import com.pocketdev.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pocketdev.app.ui.components.MarkdownText
import com.pocketdev.app.ui.components.DiffViewer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.filled.Send

import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToProjects: () -> Unit
) {
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val activeFileIndex by viewModel.activeFileIndex.collectAsStateWithLifecycle()
    val projectName by viewModel.currentProjectName.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()
    val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
    val lineNumbers by settingsViewModel.lineNumbers.collectAsStateWithLifecycle()
    val autocompleteEnabled by settingsViewModel.autocomplete.collectAsStateWithLifecycle()
    val wordWrap by settingsViewModel.wordWrap.collectAsStateWithLifecycle()
    val allLanguages = remember { Language.values().toList() }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showAiWriteDialog by remember { mutableStateOf(false) }
    var showAiEditDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showHtmlPreview by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var isTerminalFullScreen by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Detect keyboard visibility
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible by remember {
        derivedStateOf {
            imeInsets.getBottom(density) > 0
        }
    }

    // Save state notification
    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            scope.launch {
                snackbarHostState.showSnackbar("Project saved!", duration = SnackbarDuration.Short)
            }
            viewModel.resetSaveState()
        }
    }

    // Show HTML preview when execution succeeds for HTML
    LaunchedEffect(executionState) {
        if (executionState is UiState.Success && language == Language.HTML) {
            showHtmlPreview = true
        }
    }

    val context = LocalContext.current

    BackHandler {
        onNavigateToProjects()
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val content = reader.readText()
                        viewModel.updateCode(content)
                        // Try to guess language from extension
                        val path = it.path ?: ""
                        val lang = when {
                            path.endsWith(".py") -> Language.PYTHON
                            path.endsWith(".js") -> Language.JAVASCRIPT
                            path.endsWith(".html") -> Language.HTML
                            path.endsWith(".css") -> Language.CSS
                            path.endsWith(".java") -> Language.JAVA
                            path.endsWith(".cpp") || path.endsWith(".cc") -> Language.CPP
                            path.endsWith(".kt") -> Language.KOTLIN
                            path.endsWith(".json") -> Language.JSON
                            else -> null
                        }
                        if (lang != null) {
                            viewModel.setLanguage(lang)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val writer = OutputStreamWriter(outputStream)
                        writer.write(viewModel.currentCode.value)
                        writer.flush()
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${language.icon} ${language.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Language Selector
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.SwapHoriz, "Change Language")
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            allLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${lang.icon} ${lang.displayName}")
                                    },
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        showLanguageMenu = false
                                    },
                                    leadingIcon = if (lang == language) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Terminal Toggle
                    IconButton(onClick = { showTerminal = !showTerminal }) {
                        Icon(Icons.Default.Terminal, "Toggle Terminal", tint = if (showTerminal) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }

                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, "Save")
                    }

                    // AI Features
                    Box {
                        IconButton(onClick = { showAiMenu = true }) {
                            Icon(Icons.Default.AutoAwesome, "AI Features", tint = Color(0xFFFFB74D))
                        }
                        DropdownMenu(
                            expanded = showAiMenu,
                            onDismissRequest = { showAiMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("✨ Write Code with AI") },
                                onClick = {
                                    showAiWriteDialog = true
                                    showAiMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📝 Edit Code with AI") },
                                onClick = {
                                    showAiEditDialog = true
                                    showAiMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔧 Fix Bug") },
                                onClick = {
                                    viewModel.fixBug()
                                    showAiMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💡 Explain Code") },
                                onClick = {
                                    viewModel.explainCode()
                                    showAiMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚡ Improve Code") },
                                onClick = {
                                    viewModel.improveCode()
                                    showAiMenu = false
                                }
                            )
                        }
                    }

                    // More options
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New File") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = {
                                    showNewFileDialog = true
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save") },
                                leadingIcon = { Icon(Icons.Default.Save, null) },
                                onClick = {
                                    showSaveDialog = true
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save to Device") },
                                leadingIcon = { Icon(Icons.Default.Download, null) },
                                onClick = {
                                    val ext = when (language) {
                                        Language.PYTHON -> ".py"
                                        Language.JAVASCRIPT -> ".js"
                                        Language.HTML -> ".html"
                                        Language.CSS -> ".css"
                                        Language.JAVA -> ".java"
                                        Language.CPP -> ".cpp"
                                        Language.KOTLIN -> ".kt"
                                        Language.JSON -> ".json"
                                    }
                                    createDocumentLauncher.launch("${projectName.ifBlank { "untitled" }}$ext")
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open from Device") },
                                leadingIcon = { Icon(Icons.Default.Upload, null) },
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("My Projects") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                                onClick = {
                                    onNavigateToProjects()
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Find & Replace") },
                                leadingIcon = { Icon(Icons.Default.FindReplace, null) },
                                onClick = {
                                    showFindReplace = !showFindReplace
                                    showMoreMenu = false
                                }
                            )
                            if (language == Language.HTML && htmlContent != null) {
                                DropdownMenuItem(
                                    text = { Text("Preview HTML") },
                                    leadingIcon = { Icon(Icons.Default.Visibility, null) },
                                    onClick = {
                                        showHtmlPreview = true
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (language in Language.executableLanguages()) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { 
                            showTerminal = true
                            viewModel.runWithAiFix() 
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.AutoFixHigh, "Run with AI Fix")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { 
                            showTerminal = true
                            viewModel.runCode() 
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.PlayArrow, "Run Code")
                    }
                }
            }
        }
    ) { paddingValues ->
        val code by viewModel.currentCode.collectAsStateWithLifecycle()
        var selection by remember { mutableStateOf(androidx.compose.ui.text.TextRange(0)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            // File Tabs
            if (files.isNotEmpty()) {
                androidx.compose.material3.ScrollableTabRow(
                    selectedTabIndex = activeFileIndex,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    files.forEachIndexed { index, file ->
                        key(file.id) {
                            androidx.compose.material3.Tab(
                                selected = activeFileIndex == index,
                                onClick = { viewModel.switchFile(index) },
                                text = { Text(file.name) }
                            )
                        }
                    }
                    androidx.compose.material3.Tab(
                        selected = false,
                        onClick = { showAddFileDialog = true },
                        text = { Icon(Icons.Default.Add, "Add File") }
                    )
                }
            }

            // Find & Replace bar
            AnimatedVisibility(visible = showFindReplace) {
                FindReplaceBar(
                    findText = findText,
                    replaceText = replaceText,
                    onFindChange = { findText = it },
                    onReplaceChange = { replaceText = it },
                    onReplace = {
                        if (findText.isNotBlank()) {
                            val newCode = code.replace(findText, replaceText)
                            viewModel.updateCode(newCode)
                        }
                    },
                    onClose = { showFindReplace = false }
                )
            }

            // Code Editor
            if (!isTerminalFullScreen) {
                Box(modifier = Modifier.weight(1f)) {
                    if (aiState is UiState.Success && (aiState as UiState.Success<AiResult>).data.isEdit) {
                        val result = (aiState as UiState.Success<AiResult>).data
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("AI Proposed Changes", style = MaterialTheme.typography.titleMedium)
                                    val modifiedFiles = result.patches.map { it.fileName }.distinct()
                                    if (modifiedFiles.isNotEmpty()) {
                                        Text("Modified files: ${modifiedFiles.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row {
                                    Button(onClick = { viewModel.applyAiEdit(result) }) {
                                        Text("Accept")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(onClick = viewModel::dismissAiResult) {
                                        Text("Reject")
                                    }
                                }
                            }

                            DiffViewer(
                                originalCode = code,
                                newCode = if (files.isNotEmpty()) viewModel.getPreviewCode(files[activeFileIndex].name, result) else code,
                                fontSize = fontSize,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val ghostSuggestion by viewModel.ghostSuggestion.collectAsStateWithLifecycle()
                            val inlineDiffSuggestion by viewModel.inlineDiffSuggestion.collectAsStateWithLifecycle()
                            CodeEditor(
                                code = code,
                                language = language,
                                fontSize = fontSize,
                                lineNumbers = lineNumbers,
                                wordWrap = wordWrap,
                                autocompleteEnabled = autocompleteEnabled,
                                ghostSuggestion = ghostSuggestion,
                                inlineDiffSuggestion = inlineDiffSuggestion,
                                onCodeChange = {
                                    viewModel.updateCode(it)
                                },
                                selection = selection,
                                onSelectionChange = { 
                                    selection = it 
                                    if (ghostSuggestion != null || inlineDiffSuggestion != null) viewModel.rejectGhostSuggestion()
                                    viewModel.requestGhostSuggestion(it.start)
                                },
                                onRejectGhostSuggestion = {
                                    viewModel.rejectGhostSuggestion()
                                },
                                onAcceptGhostSuggestionLine = {
                                    val length = viewModel.acceptGhostSuggestionLine(selection.start)
                                    selection = androidx.compose.ui.text.TextRange(selection.start + length)
                                },
                                onAcceptGhostSuggestionFull = {
                                    viewModel.acceptGhostSuggestion(selection.start)
                                    selection = androidx.compose.ui.text.TextRange(selection.start + ghostSuggestion!!.length)
                                },
                                onAcceptInlineDiff = {
                                    val length = viewModel.acceptInlineDiffSuggestion()
                                    if (length > 0) {
                                        val newSelection = viewModel.currentCode.value.indexOf(inlineDiffSuggestion!!.addText!!)
                                        if (newSelection >= 0) {
                                            selection = androidx.compose.ui.text.TextRange(newSelection + length)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // No more popup - suggestions are shown inline
                        }
                    }
                }
            }

            // Terminal Panel
            AnimatedVisibility(
                visible = showTerminal,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = if (isTerminalFullScreen) Modifier.weight(1f) else Modifier
            ) {
                TerminalPanel(
                    terminalManager = viewModel.terminalManager,
                    isFullScreen = isTerminalFullScreen,
                    onToggleFullScreen = { isTerminalFullScreen = !isTerminalFullScreen },
                    onClear = { viewModel.terminalManager.clearTerminal() },
                    onClose = { 
                        showTerminal = false
                        isTerminalFullScreen = false
                    },
                    onShowHtml = if (language == Language.HTML && htmlContent != null) {
                        { showHtmlPreview = true }
                    } else null,
                    onSendInput = { input ->
                        val isWaiting = viewModel.terminalManager.isWaitingForInput.value
                        if (isWaiting) {
                            viewModel.sendTerminalInput(input)
                        } else {
                            viewModel.chatWithAi(input)
                        }
                    },
                    modifier = if (isTerminalFullScreen) Modifier.fillMaxSize() else Modifier
                )
            }
            
            // Special Characters Bar - only visible when keyboard is open
            if (isKeyboardVisible) {
                SpecialCharactersBar(
                    onCharacterClick = { char ->
                        val currentCode = viewModel.currentCode.value
                        val cursorPos = selection.start.coerceIn(0, currentCode.length)
                        
                        // Auto-close brackets and quotes
                        val (insertText, cursorOffset) = when (char) {
                            "(" -> "()" to 1
                            "{" -> "{}" to 1
                            "[" -> "[]" to 1
                            "\"" -> "\"\"" to 1
                            "'" -> "''" to 1
                            "<" -> "<>" to 1
                            else -> char to char.length
                        }
                        
                        val newCode = currentCode.substring(0, cursorPos) + insertText + currentCode.substring(cursorPos)
                        viewModel.updateCode(newCode)
                        selection = androidx.compose.ui.text.TextRange(cursorPos + cursorOffset)
                    }
                )
            }
        }
    }

    // AI Result Dialog
    if (aiState is UiState.Loading) {
        AiLoadingDialog()
    } else if (aiState is UiState.Success) {
        val result = (aiState as UiState.Success<AiResult>).data
        if (!result.isEdit) {
            AiResultDialog(
                result = result,
                language = language,
                onApply = { 
                    viewModel.applyAiCode(it)
                },
                onDismiss = viewModel::dismissAiResult,
                onAskFollowUp = viewModel::askFollowUpQuestion,
                currentCode = viewModel.currentCode.value
            )
        }
    } else if (aiState is UiState.Error) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAiResult,
            title = { Text("AI Error") },
            text = { Text((aiState as UiState.Error).message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAiResult) { Text("OK") }
            }
        )
    }

    // Save Dialog
    if (showAiWriteDialog) {
        var prompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiWriteDialog = false },
            title = { Text("Write Code with AI") },
            text = {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe what you want to build or change") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            viewModel.writeCodeWithAi(prompt)
                            showAiWriteDialog = false
                        }
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiWriteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAiEditDialog) {
        var prompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiEditDialog = false },
            title = { Text("Edit Code with AI") },
            text = {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe what you want to modify") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            viewModel.editCodeWithAi(prompt)
                            showAiEditDialog = false
                        }
                    }
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSaveDialog) {
        SaveProjectDialog(
            currentName = projectName,
            onSave = { name ->
                viewModel.saveProject(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        NewFileDialog(
            onCreate = { lang ->
                viewModel.newFile(lang)
                showNewFileDialog = false
            },
            onDismiss = { showNewFileDialog = false }
        )
    }

    // Add File Dialog
    if (showAddFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var selectedLang by remember { mutableStateOf(Language.PYTHON) }
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            title = { Text("Add File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select language:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    allLanguages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLang = lang }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLang == lang,
                                onClick = { selectedLang = lang }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${lang.icon} ${lang.displayName}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val nameWithExt = if (newFileName.contains(".")) newFileName else newFileName + selectedLang.extension
                            viewModel.addFile(nameWithExt, selectedLang)
                            showAddFileDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // HTML Preview
    if (showHtmlPreview && htmlContent != null) {
        HtmlPreviewDialog(
            htmlContent = htmlContent!!,
            onDismiss = { showHtmlPreview = false }
        )
    }
}

@Composable
fun CodeEditor(
    code: String,
    language: Language,
    fontSize: Int,
    lineNumbers: Boolean,
    wordWrap: Boolean = false,
    autocompleteEnabled: Boolean = true,
    ghostSuggestion: String? = null,
    inlineDiffSuggestion: AiResult? = null,
    onCodeChange: (String) -> Unit,
    selection: androidx.compose.ui.text.TextRange,
    onSelectionChange: (androidx.compose.ui.text.TextRange) -> Unit,
    onRejectGhostSuggestion: () -> Unit = {},
    onAcceptGhostSuggestionLine: () -> Unit = {},
    onAcceptGhostSuggestionFull: () -> Unit = {},
    onAcceptInlineDiff: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = code,
                selection = selection
            )
        )
    }

    // Sync external code changes (e.g., loading a new file or AI edits)
    LaunchedEffect(code) {
        if (code != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = code)
        }
    }

    // Sync external selection changes
    LaunchedEffect(selection) {
        if (selection != textFieldValue.selection) {
            textFieldValue = textFieldValue.copy(selection = selection)
        }
    }

    var baseHighlightedCode by remember { mutableStateOf(androidx.compose.ui.text.AnnotatedString(textFieldValue.text)) }

    LaunchedEffect(textFieldValue.text, language) {
        baseHighlightedCode = withContext(Dispatchers.Default) {
            SyntaxHighlighter.highlight(textFieldValue.text, language)
        }
    }

    val highlightedCode = remember(
        baseHighlightedCode,
        ghostSuggestion,
        inlineDiffSuggestion,
        textFieldValue.selection,
        textFieldValue.text
    ) {
        when {
            ghostSuggestion != null -> {
                val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                builder.append(baseHighlightedCode.subSequence(0, cursor))
                builder.withStyle(
                    SpanStyle(
                        color = Color(0xFF888888),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                ) {
                    append(ghostSuggestion)
                }
                builder.append(baseHighlightedCode.subSequence(cursor, baseHighlightedCode.length))
                builder.toAnnotatedString()
            }

            inlineDiffSuggestion != null &&
                inlineDiffSuggestion.deleteText != null &&
                inlineDiffSuggestion.addText != null -> {
                val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                val deleteStart = inlineDiffSuggestion.editStartPos.coerceIn(0, textFieldValue.text.length)
                val deleteEnd = inlineDiffSuggestion.editEndPos.coerceIn(0, textFieldValue.text.length)

                builder.append(baseHighlightedCode.subSequence(0, deleteStart))

                if (deleteEnd > deleteStart) {
                    builder.withStyle(
                        SpanStyle(
                            color = Color(0xFFE53935),
                            background = Color(0x33FFCDD2),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    ) {
                        append(textFieldValue.text.substring(deleteStart, deleteEnd))
                    }
                }

                builder.withStyle(
                    SpanStyle(
                        color = Color(0xFF2E7D32),
                        background = Color(0x33C8E6C9)
                    )
                ) {
                    append(inlineDiffSuggestion.addText)
                }

                builder.append(baseHighlightedCode.subSequence(deleteEnd, baseHighlightedCode.length))
                builder.toAnnotatedString()
            }

            else -> baseHighlightedCode
        }
    }

    val lineHeight = (fontSize * 1.5).sp
    val codeTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = lineHeight
    )

    // Shared scroll state so line numbers scroll with code
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Autocomplete state
    var showAutocomplete by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<AutocompleteItem>>(emptyList()) }
    var cursorRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // Track visual line mapping for word wrap mode
    var visualLineInfo by remember { mutableStateOf(1 to emptyMap<Int, Int>()) }

    val logicalLineCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }
    val lineNumberWidth = remember(lineNumbers, logicalLineCount, fontSize) {
        if (lineNumbers) {
            (maxOf(logicalLineCount, 1).toString().length * fontSize * 0.6 + 12).dp
        } else {
            0.dp
        }
    }

    // Update suggestions when code changes
    LaunchedEffect(textFieldValue.text, textFieldValue.selection, language, autocompleteEnabled) {
        val cursorPosition = textFieldValue.selection.start
        if (autocompleteEnabled && cursorPosition <= textFieldValue.text.length) {
            val newSuggestions = withContext(Dispatchers.Default) {
                AutocompleteEngine.getSuggestions(textFieldValue.text, cursorPosition, language)
            }
            suggestions = newSuggestions
            showAutocomplete = newSuggestions.isNotEmpty()
        } else {
            suggestions = emptyList()
            showAutocomplete = false
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers — same font size and line height as code, shared scroll
            if (lineNumbers) {
                val (visualCount, lineMapping) = if (wordWrap) {
                    visualLineInfo
                } else {
                    logicalLineCount to emptyMap<Int, Int>()
                }

                val lineNumbersText = remember(wordWrap, lineMapping, visualCount, logicalLineCount) {
                    if (wordWrap && lineMapping.isNotEmpty()) {
                        (0 until visualCount).map { visualLine ->
                            lineMapping[visualLine] ?: (visualLine + 1)
                        }.joinToString("\n")
                    } else {
                        (1..maxOf(logicalLineCount, 1)).joinToString("\n")
                    }
                }

                Column(
                    modifier = Modifier
                        .width(lineNumberWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(start = 2.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = lineNumbersText,
                        style = codeTextStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }

            // Code input area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let {
                        if (!wordWrap) it.horizontalScroll(horizontalScrollState) else it
                    }
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { tfv ->
                        var newText = tfv.text
                        var newSelection = tfv.selection
                        var newCursor = newSelection.start
                        
                        // Auto-closing brackets
                        if (newText.length == textFieldValue.text.length + 1 && newCursor > 0) {
                            val insertedChar = newText[newCursor - 1]
                            val closingChar = when (insertedChar) {
                                '(' -> ")"
                                '{' -> "}"
                                '[' -> "]"
                                '"' -> "\""
                                '\'' -> "'"
                                else -> null
                            }
                            if (closingChar != null) {
                                newText = newText.substring(0, newCursor) + closingChar + newText.substring(newCursor)
                                newSelection = androidx.compose.ui.text.TextRange(newCursor)
                            }
                        }
                        
                        val newTfv = androidx.compose.ui.text.input.TextFieldValue(newText, newSelection)
                        textFieldValue = newTfv
                        
                        onSelectionChange(newSelection)
                        if (newText != code) {
                            onCodeChange(newText)
                        }
                    },
                    onTextLayout = { layoutResult ->
                        val cursorPosition = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                        cursorRect = layoutResult.getCursorRect(cursorPosition)
                        
                        // Calculate visual line info for word wrap mode
                        if (wordWrap) {
                            val lineCount = layoutResult.lineCount
                            val mapping = mutableMapOf<Int, Int>()
                            val text = textFieldValue.text
                            var currentLogicalLine = 1
                            var charIndex = 0
                            
                            for (visualLine in 0 until lineCount) {
                                mapping[visualLine] = currentLogicalLine
                                val lineEnd = layoutResult.getLineEnd(visualLine)
                                while (charIndex < lineEnd && charIndex < text.length) {
                                    if (text[charIndex] == '\n') {
                                        currentLogicalLine++
                                    }
                                    charIndex++
                                }
                            }
                            visualLineInfo = Pair(lineCount, mapping)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .pointerInput(ghostSuggestion, inlineDiffSuggestion) {
                            // Handle both ghost suggestion and inline diff
                            if (ghostSuggestion != null || inlineDiffSuggestion != null) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > 100f) {
                                            // Swipe right to accept
                                            if (ghostSuggestion != null) {
                                                onAcceptGhostSuggestionFull()
                                            } else if (inlineDiffSuggestion != null) {
                                                onAcceptInlineDiff()
                                            }
                                        } else if (totalDrag > 30f) {
                                            // Small swipe right - accept line
                                            if (ghostSuggestion != null) {
                                                onAcceptGhostSuggestionLine()
                                            } else if (inlineDiffSuggestion != null) {
                                                onAcceptInlineDiff()
                                            }
                                        } else if (totalDrag < -30f) {
                                            // Swipe left to reject
                                            onRejectGhostSuggestion()
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                }
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                // Tab to accept suggestion
                                if (keyEvent.key == Key.Tab) {
                                    if (ghostSuggestion != null) {
                                        val newCode = textFieldValue.text.substring(0, textFieldValue.selection.start) + ghostSuggestion!! + textFieldValue.text.substring(textFieldValue.selection.start)
                                        onCodeChange(newCode)
                                        val newCursor = textFieldValue.selection.start + ghostSuggestion!!.length
                                        onSelectionChange(androidx.compose.ui.text.TextRange(newCursor))
                                        return@onKeyEvent true
                                    } else if (inlineDiffSuggestion != null) {
                                        onAcceptInlineDiff()
                                        return@onKeyEvent true
                                    }
                                }
                                // Escape to reject suggestion
                                if (keyEvent.key == Key.Escape && (ghostSuggestion != null || inlineDiffSuggestion != null)) {
                                    onRejectGhostSuggestion()
                                    return@onKeyEvent true
                                }
                            }
                            false
                        },
                    textStyle = codeTextStyle.copy(color = Color.Transparent),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            // Syntax highlighted overlay
                            Text(
                                text = highlightedCode,
                                style = codeTextStyle
                            )
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Autocomplete dropdown
        if (showAutocomplete && suggestions.isNotEmpty()) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val xOffset = with(density) { 
                val baseLeft = if (lineNumbers) lineNumberWidth.toPx() else 0f
                val paddingLeft = 8.dp.toPx()
                val scrollX = if (!wordWrap) horizontalScrollState.value.toFloat() else 0f
                (baseLeft + paddingLeft + cursorRect.left - scrollX).toInt() 
            }
            val yOffset = with(density) { 
                val paddingTop = 8.dp.toPx()
                (paddingTop + cursorRect.bottom - verticalScrollState.value).toInt() 
            }
            
            Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(xOffset, yOffset),
                properties = PopupProperties(focusable = false)
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 320.dp)
                        .heightIn(max = 180.dp),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        suggestions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Insert completion: replace the prefix with the full text
                                        val cursorPosition = textFieldValue.selection.start
                                        val prefix = getWordPrefixForCompletion(textFieldValue.text, cursorPosition)
                                        val before = textFieldValue.text.substring(0, cursorPosition - prefix.length)
                                        val after = textFieldValue.text.substring(cursorPosition)
                                        val newCode = before + item.insertText + after
                                        
                                        val newSelection = androidx.compose.ui.text.TextRange(cursorPosition - prefix.length + item.insertText.length + item.cursorOffset)
                                        textFieldValue = androidx.compose.ui.text.input.TextFieldValue(newCode, newSelection)
                                        
                                        onCodeChange(newCode)
                                        onSelectionChange(newSelection)
                                        showAutocomplete = false
                                    }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (item.type) {
                                    com.pocketdev.app.editor.CompletionType.KEYWORD -> "K"
                                    com.pocketdev.app.editor.CompletionType.FUNCTION -> "F"
                                    com.pocketdev.app.editor.CompletionType.METHOD -> "M"
                                    com.pocketdev.app.editor.CompletionType.CLASS -> "C"
                                    com.pocketdev.app.editor.CompletionType.VARIABLE -> "V"
                                    com.pocketdev.app.editor.CompletionType.SNIPPET -> "S"
                                    com.pocketdev.app.editor.CompletionType.TAG -> "T"
                                    com.pocketdev.app.editor.CompletionType.PROPERTY -> "P"
                                    com.pocketdev.app.editor.CompletionType.ATTRIBUTE -> "A"
                                    com.pocketdev.app.editor.CompletionType.VALUE -> "V"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getWordPrefixForCompletion(code: String, cursorPos: Int): String {
    if (cursorPos <= 0 || cursorPos > code.length) return ""
    var start = cursorPos - 1
    while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_' || code[start] == '.')) {
        start--
    }
    return code.substring(start + 1, cursorPos)
}

@Composable
fun TerminalPanel(
    terminalManager: com.pocketdev.app.execution.TerminalManager,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onShowHtml: (() -> Unit)?,
    onSendInput: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val maxHeight = if (isFullScreen) Dp.Unspecified else 250.dp
    val messages by terminalManager.messages.collectAsStateWithLifecycle()
    val isWaitingForInput by terminalManager.isWaitingForInput.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isFullScreen) Modifier.heightIn(min = 100.dp, max = maxHeight) else Modifier),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Terminal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                if (onShowHtml != null) {
                    TextButton(onClick = onShowHtml, modifier = Modifier.height(28.dp)) {
                        Text("Preview HTML", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onToggleFullScreen, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        "Toggle Fullscreen",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Clear terminal", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Close terminal", modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "(no output)",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { index, msg -> "$index:${msg.type}:${msg.text.hashCode()}" }
                    ) { _, msg ->
                        val color = when (msg.type) {
                            com.pocketdev.app.execution.TerminalMessageType.NORMAL -> MaterialTheme.colorScheme.onSurface
                            com.pocketdev.app.execution.TerminalMessageType.ERROR -> Color(0xFFEF9A9A)
                            com.pocketdev.app.execution.TerminalMessageType.AGENT -> Color(0xFF81C784)
                            com.pocketdev.app.execution.TerminalMessageType.STATUS -> Color(0xFF64B5F6)
                            com.pocketdev.app.execution.TerminalMessageType.INPUT_PROMPT -> Color(0xFFFFD54F)
                        }
                        Text(
                            text = msg.text,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = color
                            )
                        )
                    }
                }
            }

            if (onSendInput != null) {
                var inputText by remember { mutableStateOf("") }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ">",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        onSendInput(inputText)
                                        inputText = ""
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        if (isWaitingForInput) "Enter input for script..." else "Type input here...",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendInput(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Send, "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FindReplaceBar(
    findText: String,
    replaceText: String,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplace: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = onFindChange,
                    placeholder = { Text("Find", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Replace", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.width(4.dp))
                Button(onClick = onReplace, modifier = Modifier.height(48.dp)) {
                    Text("Replace")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        }
    }
}

@Composable
fun AiLoadingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("AI is thinking...") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Analyzing your code with Groq AI")
            }
        },
        confirmButton = {}
    )
}

@Composable
fun AiResultDialog(
    result: AiResult,
    language: Language,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
    onAskFollowUp: ((String) -> Unit)? = null,
    currentCode: String = ""
) {
    val scrollState = rememberScrollState()
    var followUpText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Analysis", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    MarkdownText(
                        text = result.content,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (result.correctedCode != null && !result.isEdit) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Proposed Code:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.correctedCode,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                if (onAskFollowUp != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("Ask a follow-up question...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (followUpText.isNotBlank()) {
                                        onAskFollowUp(followUpText)
                                        followUpText = ""
                                    }
                                },
                                enabled = followUpText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Send, "Send")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (result.correctedCode != null && !result.isEdit) {
                Button(onClick = { onApply(result.correctedCode) }) {
                    Text("Apply Code")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SaveProjectDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NewFileDialog(
    onCreate: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(Language.PYTHON) }
    val allLanguages = remember { Language.values().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New File") },
        text = {
            Column {
                Text("Select language:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                allLanguages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = lang }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${lang.icon} ${lang.displayName}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(selectedLanguage) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HtmlPreviewDialog(
    htmlContent: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("HTML Preview", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                HorizontalDivider()
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Syntax Highlighter - highlights code text
object SyntaxHighlighter {
    fun highlight(code: String, language: Language): androidx.compose.ui.text.AnnotatedString {
        return buildAnnotatedString {
            val tokens = tokenize(code, language)
            for (token in tokens) {
                withStyle(SpanStyle(color = token.color)) {
                    append(token.text)
                }
            }
        }
    }

    private data class Token(val text: String, val color: Color)

    private fun tokenize(code: String, language: Language): List<Token> {
        val tokens = mutableListOf<Token>()
        val patterns = when (language) {
            Language.PYTHON -> pythonPatterns
            Language.JAVASCRIPT -> jsPatterns
            Language.HTML -> htmlPatterns
            Language.CSS -> cssPatterns
            Language.JAVA -> javaPatterns
            Language.CPP -> cppPatterns
            Language.KOTLIN -> kotlinPatterns
            Language.JSON -> jsonPatterns
        }

        val matchers = patterns.map { it.first.toPattern().matcher(code) to it.second }
        var index = 0
        val length = code.length

        while (index < length) {
            var bestMatchStart = length
            var bestMatchEnd = length
            var bestColor = colorDefault

            for ((matcher, color) in matchers) {
                matcher.region(index, length)
                if (matcher.find()) {
                    if (matcher.start() < bestMatchStart) {
                        bestMatchStart = matcher.start()
                        bestMatchEnd = matcher.end()
                        bestColor = color
                    }
                }
            }

            if (bestMatchStart > index) {
                tokens.add(Token(code.substring(index, bestMatchStart), colorDefault))
            }

            if (bestMatchStart < length) {
                tokens.add(Token(code.substring(bestMatchStart, bestMatchEnd), bestColor))
                index = bestMatchEnd
            } else {
                break
            }
        }
        return tokens
    }

    // Colors
    private val colorKeyword = Color(0xFF569CD6)
    private val colorString = Color(0xFFCE9178)
    private val colorComment = Color(0xFF6A9955)
    private val colorNumber = Color(0xFFB5CEA8)
    private val colorFunction = Color(0xFFDCDCAA)
    private val colorType = Color(0xFF4EC9B0)
    private val colorOperator = Color(0xFFD4D4D4)
    private val colorTag = Color(0xFF569CD6)
    private val colorAttr = Color(0xFF9CDCFE)
    private val colorDefault = Color(0xFFCDD9E5)
    private val colorPreprocessor = Color(0xFFC586C0)

    private val pythonPatterns = listOf(
        Regex("(#.*)") to colorComment,
        Regex("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?''')") to colorString,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')") to colorString,
        Regex("\\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|yield|pass|break|continue|and|or|not|in|is|lambda|global|nonlocal|del|raise|assert|True|False|None|async|await)\\b") to colorKeyword,
        Regex("\\b(print|len|range|type|int|str|float|bool|list|dict|set|tuple|input|open|enumerate|zip|map|filter|sorted|reversed|any|all|max|min|sum|abs|round|isinstance|issubclass|super|property|staticmethod|classmethod)\\b") to colorType,
        Regex("\\b(0x[0-9A-Fa-f]+|\\d+\\.\\d*|\\.\\d+|\\d+)\\b") to colorNumber,
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()") to colorFunction,
        Regex("(@\\w+)") to colorPreprocessor,
        Regex("[+\\-*/=<>!&|^~%@]") to colorOperator,
    )

    private val jsPatterns = listOf(
        Regex("(//.*)") to colorComment,
        Regex("(/\\*[\\s\\S]*?\\*/)") to colorComment,
        Regex("(`(?:[^`\\\\]|\\\\.)*`)") to colorString,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')") to colorString,
        Regex("\\b(var|let|const|function|class|if|else|for|while|do|try|catch|finally|return|throw|new|delete|typeof|instanceof|in|of|break|continue|switch|case|default|import|export|from|async|await|yield|extends|super|this|null|undefined|true|false|void)\\b") to colorKeyword,
        Regex("\\b(console|Math|Array|Object|String|Number|Boolean|Date|JSON|Promise|fetch|document|window|localStorage|sessionStorage|setTimeout|setInterval|clearTimeout|clearInterval|parseInt|parseFloat|isNaN|isFinite)\\b") to colorType,
        Regex("\\b(0x[0-9A-Fa-f]+|\\d+\\.\\d*|\\.\\d+|\\d+)\\b") to colorNumber,
        Regex("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)(?=\\s*\\()") to colorFunction,
        Regex("(=>)") to colorKeyword,
        Regex("[+\\-*/=<>!&|^~%]") to colorOperator,
    )

    private val htmlPatterns = listOf(
        Regex("(<!--[\\s\\S]*?-->)") to colorComment,
        Regex("(</?[a-zA-Z][a-zA-Z0-9]*)") to colorTag,
        Regex("(/?>)") to colorTag,
        Regex("([a-zA-Z-]+)(?=\\s*=)") to colorAttr,
        Regex("(\"[^\"]*\"|'[^']*')") to colorString,
        Regex("(&[a-zA-Z]+;|&#\\d+;)") to colorPreprocessor,
    )

    private val cssPatterns = listOf(
        Regex("(/\\*[\\s\\S]*?\\*/)") to colorComment,
        Regex("(//.*$)", RegexOption.MULTILINE) to colorComment,
        Regex("([.#][a-zA-Z][a-zA-Z0-9_-]*)") to colorFunction,
        Regex("\\b([a-zA-Z-]+)(?=\\s*:)") to colorAttr,
        Regex("(\"[^\"]*\"|'[^']*')") to colorString,
        Regex("(#[0-9A-Fa-f]{3,8}\\b)") to colorString,
        Regex("\\b(\\d+\\.?\\d*)(px|em|rem|vh|vw|%|pt|cm|mm|ex|ch|vmin|vmax)?\\b") to colorNumber,
        Regex("\\b(important|px|em|rem|vh|vw|none|auto|block|flex|grid|inline|absolute|relative|fixed|sticky|inherit|initial|unset)\\b") to colorKeyword,
        Regex("(@[a-zA-Z-]+)") to colorPreprocessor,
    )

    private val javaPatterns = listOf(
        Regex("(//.*)") to colorComment,
        Regex("(/\\*[\\s\\S]*?\\*/)") to colorComment,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\")") to colorString,
        Regex("('(?:[^'\\\\]|\\\\.)*')") to colorString,
        Regex("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\\b") to colorKeyword,
        Regex("\\b(String|Integer|Long|Double|Float|Boolean|Character|Byte|Short|Object|System|Math|Arrays|ArrayList|HashMap|List|Map|Set|Iterator|Scanner|PrintStream|StringBuilder|StringBuffer)\\b") to colorType,
        Regex("\\b(0x[0-9A-Fa-f]+L?|\\d+\\.\\d*[fFdD]?|\\.\\d+[fFdD]?|\\d+[lLfFdD]?)\\b") to colorNumber,
        Regex("\\b([A-Z][a-zA-Z0-9]*)\\b") to colorType,
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()") to colorFunction,
        Regex("(@[A-Za-z]+)") to colorPreprocessor,
    )

    private val cppPatterns = listOf(
        Regex("(//.*)") to colorComment,
        Regex("(/\\*[\\s\\S]*?\\*/)") to colorComment,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')") to colorString,
        Regex("(#\\s*(?:include|define|ifdef|ifndef|endif|else|elif|pragma|undef)\\b[^\\n]*)") to colorPreprocessor,
        Regex("\\b(auto|bool|break|case|catch|char|class|const|continue|default|delete|do|double|else|enum|explicit|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|nullptr|operator|private|protected|public|register|return|short|signed|sizeof|static|struct|switch|template|this|throw|true|try|typedef|typename|union|unsigned|using|virtual|void|volatile|while)\\b") to colorKeyword,
        Regex("\\b(std|cout|cin|endl|string|vector|map|set|pair|array|queue|stack|algorithm|iostream|fstream|sstream)\\b") to colorType,
        Regex("\\b(0x[0-9A-Fa-f]+[uUlL]*|\\d+\\.\\d*[fFlL]?|\\.\\d+[fFlL]?|\\d+[uUlL]*)\\b") to colorNumber,
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()") to colorFunction,
        Regex("(::)") to colorOperator,
    )

    private val kotlinPatterns = listOf(
        Regex("(//.*)") to colorComment,
        Regex("(/\\*[\\s\\S]*?\\*/)") to colorComment,
        Regex("(\"\"\"[\\s\\S]*?\"\"\")") to colorString,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\")") to colorString,
        Regex("('(?:[^'\\\\]|\\\\.)*')") to colorString,
        Regex("\\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|delegate|do|dynamic|else|enum|expect|external|false|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|it|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|true|try|typealias|typeof|val|var|vararg|when|where|while)\\b") to colorKeyword,
        Regex("\\b(String|Int|Long|Double|Float|Boolean|Char|Byte|Short|Any|Unit|Nothing|Array|List|MutableList|Map|MutableMap|Set|MutableSet|Pair|Triple|Sequence|Flow|StateFlow|println|print|readLine|TODO)\\b") to colorType,
        Regex("\\b(0x[0-9A-Fa-f]+[lLuU]*|\\d+\\.\\d*[fF]?|\\.\\d+[fF]?|\\d+[lLuU]*)\\b") to colorNumber,
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()") to colorFunction,
        Regex("(@[A-Za-z]+)") to colorPreprocessor,
    )

    private val jsonPatterns = listOf(
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\")(?=\\s*:)") to colorAttr,
        Regex("(\"(?:[^\"\\\\]|\\\\.)*\")") to colorString,
        Regex("\\b(true|false|null)\\b") to colorKeyword,
        Regex("\\b(-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)\\b") to colorNumber,
        Regex("[{\\[\\]},:]") to colorOperator,
    )
}


/**
 * Special Characters Bar - A swipeable row of frequently used programming symbols
 * Shows above the keyboard for quick symbol access
 */
@Composable
fun SpecialCharactersBar(
    onCharacterClick: (String) -> Unit
) {
    val specialChars = remember {
        listOf(
            "(", ")", "{", "}", "[", "]", "<", ">", "=", "+", "-", "*", "/",
            ".", ":", ";", "\"", "'", "!", "?", "@", "#", "$", "%", "&", "_",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "->", "=>", "::"
        )
    }
    val scrollState = rememberScrollState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            specialChars.forEach { char ->
                key(char) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onCharacterClick(char)
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
