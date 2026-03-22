package com.pocketdev.app.ui.screens

import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay
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

private enum class EditorDialogType {
    AI_WRITE,
    AI_EDIT,
    SAVE_PROJECT,
    NEW_FILE,
    ADD_FILE
}

@Stable
private class EditorUiState {
    var activeDialog by mutableStateOf<EditorDialogType?>(null)
    var showHtmlPreview by mutableStateOf(false)
    var showFindReplace by mutableStateOf(false)
    var showTerminal by mutableStateOf(false)
    var isTerminalFullScreen by mutableStateOf(false)
    var findText by mutableStateOf("")
    var replaceText by mutableStateOf("")
}
@Stable
private class EditorSelectionState {
    var value by mutableStateOf(androidx.compose.ui.text.TextRange(0))
}

@Composable
private fun rememberEditorSelectionState(): EditorSelectionState {
    return remember { EditorSelectionState() }
}


@Composable
private fun rememberEditorUiState(): EditorUiState {
    return remember { EditorUiState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToProjects: () -> Unit
) {
    val uiState = rememberEditorUiState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    BackHandler(onBack = onNavigateToProjects)

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    viewModel.updateCode(content)

                    val path = uri.path ?: ""
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
            } catch (_: Exception) {
            }
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(viewModel.currentCode.value)
                    writer.flush()
                }
            } catch (_: Exception) {
            }
        }
    )

    val openDocument: () -> Unit = remember(openDocumentLauncher) {
        { openDocumentLauncher.launch(arrayOf("*/*")) }
    }
    val createDocument: (String) -> Unit = remember(createDocumentLauncher) {
        { fileName -> createDocumentLauncher.launch(fileName) }
    }

    EditorScreenEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onShowHtmlPreview = { uiState.showHtmlPreview = true }
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorTopBar(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateToProjects = onNavigateToProjects,
                onOpenDocument = openDocument,
                onCreateDocument = createDocument
            )
        },
        floatingActionButton = {
            EditorRunFab(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    ) { paddingValues ->
        EditorMainContent(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            uiState = uiState,
            paddingValues = paddingValues
        )
    }

    EditorDialogsHost(
        viewModel = viewModel,
        uiState = uiState
    )
}

@Composable
private fun EditorScreenEffects(
    viewModel: EditorViewModel,
    snackbarHostState: SnackbarHostState,
    onShowHtmlPreview: () -> Unit
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            snackbarHostState.showSnackbar(
                message = "Project saved!",
                duration = SnackbarDuration.Short
            )
            viewModel.resetSaveState()
        }
    }

    LaunchedEffect(executionState, language) {
        if (executionState is UiState.Success && language == Language.HTML) {
            onShowHtmlPreview()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onNavigateToProjects: () -> Unit,
    onOpenDocument: () -> Unit,
    onCreateDocument: (String) -> Unit
) {
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val projectName by viewModel.currentProjectName.collectAsStateWithLifecycle()
    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()

    val onLanguageSelected = remember { { lang: Language -> viewModel.setLanguage(lang) } }
    val onFixBug = remember { { viewModel.fixBug() } }
    val onExplainCode = remember { { viewModel.explainCode() } }
    val onImproveCode = remember { { viewModel.improveCode() } }
    val onWriteCode = remember { { uiState.activeDialog = EditorDialogType.AI_WRITE } }
    val onEditCode = remember { { uiState.activeDialog = EditorDialogType.AI_EDIT } }
    val onOpenSaveDialog = remember { { uiState.activeDialog = EditorDialogType.SAVE_PROJECT } }
    val onOpenNewFileDialog = remember { { uiState.activeDialog = EditorDialogType.NEW_FILE } }
    val onToggleFindReplace = remember { { uiState.showFindReplace = !uiState.showFindReplace } }
    val onPreviewHtml = remember { { uiState.showHtmlPreview = true } }

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
            LanguageSelectorMenu(
                currentLanguage = language,
                onLanguageSelected = onLanguageSelected
            )
            TerminalToggleButton(uiState = uiState)
            IconButton(
                onClick = onOpenSaveDialog,
                modifier = Modifier.focusable(false)
            ) {
                Icon(Icons.Default.Save, "Save")
            }
            AiFeaturesMenu(
                onWriteCode = onWriteCode,
                onEditCode = onEditCode,
                onFixBug = onFixBug,
                onExplainCode = onExplainCode,
                onImproveCode = onImproveCode
            )
            EditorMoreMenu(
                language = language,
                projectName = projectName,
                canPreviewHtml = language == Language.HTML && htmlContent != null,
                onNewFile = onOpenNewFileDialog,
                onSave = onOpenSaveDialog,
                onSaveToDevice = onCreateDocument,
                onOpenFromDevice = onOpenDocument,
                onNavigateToProjects = onNavigateToProjects,
                onToggleFindReplace = onToggleFindReplace,
                onPreviewHtml = onPreviewHtml
            )
        }
    )
}

@Composable
private fun TerminalToggleButton(uiState: EditorUiState) {
    IconButton(
        onClick = { uiState.showTerminal = !uiState.showTerminal },
        modifier = Modifier.focusable(false)
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Toggle Terminal",
            tint = if (uiState.showTerminal) MaterialTheme.colorScheme.primary
            else LocalContentColor.current
        )
    }
}

@Composable
private fun EditorMoreMenu(
    language: Language,
    projectName: String,
    canPreviewHtml: Boolean,
    onNewFile: () -> Unit,
    onSave: () -> Unit,
    onSaveToDevice: (String) -> Unit,
    onOpenFromDevice: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onToggleFindReplace: () -> Unit,
    onPreviewHtml: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.focusable(false)
        ) {
            Icon(Icons.Default.MoreVert, "More")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text("New File") },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                onClick = {
                    onNewFile()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Save") },
                leadingIcon = { Icon(Icons.Default.Save, null) },
                onClick = {
                    onSave()
                    expanded = false
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
                    onSaveToDevice("${projectName.ifBlank { "untitled" }}$ext")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Open from Device") },
                leadingIcon = { Icon(Icons.Default.Upload, null) },
                onClick = {
                    onOpenFromDevice()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("My Projects") },
                leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                onClick = {
                    onNavigateToProjects()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Find & Replace") },
                leadingIcon = { Icon(Icons.Default.FindReplace, null) },
                onClick = {
                    onToggleFindReplace()
                    expanded = false
                }
            )
            if (canPreviewHtml) {
                DropdownMenuItem(
                    text = { Text("Preview HTML") },
                    leadingIcon = { Icon(Icons.Default.Visibility, null) },
                    onClick = {
                        onPreviewHtml()
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun EditorRunFab(
    viewModel: EditorViewModel,
    uiState: EditorUiState
) {
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()

    if (language !in Language.executableLanguages()) {
        return
    }

    val onRunWithAiFix = remember(viewModel, uiState) {
        {
            uiState.showTerminal = true
            viewModel.runWithAiFix()
        }
    }
    val onRunCode = remember(viewModel, uiState) {
        {
            uiState.showTerminal = true
            viewModel.runCode()
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        SmallFloatingActionButton(
            onClick = onRunWithAiFix,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.AutoFixHigh, "Run with AI Fix")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FloatingActionButton(
            onClick = onRunCode,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.PlayArrow, "Run Code")
        }
    }
}

@Composable
private fun EditorMainContent(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    uiState: EditorUiState,
    paddingValues: PaddingValues
) {
    val selectionState = rememberEditorSelectionState()
    val onAddFile = remember(uiState) { { uiState.activeDialog = EditorDialogType.ADD_FILE } }

    val onCharacterClick = remember(viewModel) {
        { char: String ->
            val currentCode = viewModel.currentCode.value
            val cursorPos = selectionState.value.start.coerceIn(0, currentCode.length)
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
            selectionState.value = androidx.compose.ui.text.TextRange(cursorPos + cursorOffset)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)
    ) {
        EditorFileTabs(
            viewModel = viewModel,
            onAddFile = onAddFile
        )

        EditorFindReplaceSection(
            viewModel = viewModel,
            uiState = uiState
        )

        EditorEditorSection(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            uiState = uiState,
            selectionState = selectionState,
            modifier = Modifier.weight(1f)
        )

        val terminalModifier by remember { derivedStateOf {
            if (uiState.isTerminalFullScreen) Modifier.weight(1f) else Modifier
        }}
        EditorTerminalSection(
            viewModel = viewModel,
            uiState = uiState,
            modifier = terminalModifier
        )

        KeyboardAwareSpecialCharactersBar(
            onCharacterClick = onCharacterClick
        )
    }
}

@Composable
private fun EditorFindReplaceSection(
    viewModel: EditorViewModel,
    uiState: EditorUiState
) {
    AnimatedVisibility(
        visible = uiState.showFindReplace,
        enter = fadeIn(animationSpec = tween(durationMillis = 120)),
        exit = fadeOut(animationSpec = tween(durationMillis = 80))
    ) {
        EditorFindReplaceBar(
            viewModel = viewModel,
            uiState = uiState
        )
    }
}

@Composable
private fun EditorEditorSection(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    uiState: EditorUiState,
    selectionState: EditorSelectionState,
    modifier: Modifier = Modifier
) {
    val isFullScreen by remember { derivedStateOf { uiState.isTerminalFullScreen } }
    if (!isFullScreen) {
        Box(modifier = modifier) {
            EditorWorkArea(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                selectionState = selectionState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EditorFileTabs(
    viewModel: EditorViewModel,
    onAddFile: () -> Unit
) {
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val activeFileIndex by viewModel.activeFileIndex.collectAsStateWithLifecycle()
    val onSwitchFile = remember(viewModel) { { index: Int -> viewModel.switchFile(index) } }

    if (files.isEmpty()) {
        return
    }

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
                    onClick = { onSwitchFile(index) },
                    text = { Text(file.name) }
                )
            }
        }
        androidx.compose.material3.Tab(
            selected = false,
            onClick = onAddFile,
            text = { Icon(Icons.Default.Add, "Add File") }
        )
    }
}

@Composable
private fun EditorFindReplaceBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState
) {
    val code by viewModel.currentCode.collectAsStateWithLifecycle()
    val onUpdateCode = remember(viewModel) { { newCode: String -> viewModel.updateCode(newCode) } }
    val onFindChange = remember(uiState) { { text: String -> uiState.findText = text } }
    val onReplaceChange = remember(uiState) { { text: String -> uiState.replaceText = text } }
    val onReplace = remember(uiState, code, onUpdateCode) {
        {
            if (uiState.findText.isNotBlank()) {
                onUpdateCode(code.replace(uiState.findText, uiState.replaceText))
            }
        }
    }
    val onClose = remember(uiState) { { uiState.showFindReplace = false } }

    FindReplaceBar(
        findText = uiState.findText,
        replaceText = uiState.replaceText,
        onFindChange = onFindChange,
        onReplaceChange = onReplaceChange,
        onReplace = onReplace,
        onClose = onClose
    )
}

@Composable
private fun EditorWorkArea(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    selectionState: EditorSelectionState,
    modifier: Modifier = Modifier
) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        if (aiState is UiState.Success && (aiState as UiState.Success<AiResult>).data.isEdit) {
            AiEditPreviewPane(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                result = (aiState as UiState.Success<AiResult>).data
            )
        } else {
            EditorCodePane(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                selectionState = selectionState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AiEditPreviewPane(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    result: AiResult
) {
    val code by viewModel.currentCode.collectAsStateWithLifecycle()
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val activeFileIndex by viewModel.activeFileIndex.collectAsStateWithLifecycle()
    val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
    val modifiedFiles = remember(result) { result.patches.map { it.fileName }.distinct() }

    val onApplyAiEdit = remember(viewModel) { { aiResult: AiResult -> viewModel.applyAiEdit(aiResult) } }
    val onDismissAiResult = remember(viewModel) { { viewModel.dismissAiResult() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI Proposed Changes", style = MaterialTheme.typography.titleMedium)
                if (modifiedFiles.isNotEmpty()) {
                    Text(
                        text = "Modified files: ${modifiedFiles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                Button(onClick = { onApplyAiEdit(result) }) {
                    Text("Accept")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onDismissAiResult) {
                    Text("Reject")
                }
            }
        }

        DiffViewer(
            originalCode = code,
            newCode = if (files.isNotEmpty()) {
                viewModel.getPreviewCode(files[activeFileIndex].name, result)
            } else {
                code
            },
            fontSize = fontSize,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditorCodePane(
    viewModel: EditorViewModel,
    settingsViewModel: SettingsViewModel,
    selectionState: EditorSelectionState,
    modifier: Modifier = Modifier
) {
    val code by viewModel.currentCode.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
    val lineNumbers by settingsViewModel.lineNumbers.collectAsStateWithLifecycle()
    val autocompleteEnabled by settingsViewModel.autocomplete.collectAsStateWithLifecycle()
    val wordWrap by settingsViewModel.wordWrap.collectAsStateWithLifecycle()
    val ghostSuggestion by viewModel.ghostSuggestion.collectAsStateWithLifecycle()
    val inlineDiffSuggestion by viewModel.inlineDiffSuggestion.collectAsStateWithLifecycle()

    val selection = selectionState.value
    val latestSelection = rememberUpdatedState(selection)
    val onCodeChange = remember(viewModel) { { newCode: String -> viewModel.updateCode(newCode) } }
    val onRejectGhostSuggestion = remember(viewModel) { { viewModel.rejectGhostSuggestion() } }
    val onSelectionChangedInternal = remember(viewModel, selectionState) {
        { newSelection: androidx.compose.ui.text.TextRange ->
            selectionState.value = newSelection
            if (viewModel.ghostSuggestion.value != null || viewModel.inlineDiffSuggestion.value != null) {
                viewModel.rejectGhostSuggestion()
            }
            viewModel.requestGhostSuggestion(newSelection.start)
        }
    }
    val onAcceptGhostSuggestionLine = remember(viewModel, selectionState) {
        {
            val cursor = latestSelection.value.start
            val length = viewModel.acceptGhostSuggestionLine(cursor)
            selectionState.value = androidx.compose.ui.text.TextRange(cursor + length)
        }
    }
    val onAcceptGhostSuggestionFull = remember(viewModel, selectionState) {
        {
            val cursor = latestSelection.value.start
            val suggestionLength = viewModel.ghostSuggestion.value?.length ?: 0
            if (suggestionLength > 0) {
                viewModel.acceptGhostSuggestion(cursor)
                selectionState.value = androidx.compose.ui.text.TextRange(cursor + suggestionLength)
            }
        }
    }
    val onAcceptInlineDiff = remember(viewModel, selectionState) {
        {
            val suggestion = viewModel.inlineDiffSuggestion.value
            val length = viewModel.acceptInlineDiffSuggestion()
            if (length > 0) {
                val addText = suggestion?.addText
                if (addText != null) {
                    val newSelection = viewModel.currentCode.value.indexOf(addText)
                    if (newSelection >= 0) {
                        selectionState.value = androidx.compose.ui.text.TextRange(newSelection + length)
                    }
                }
            }
        }
    }

    CodeEditor(
        code = code,
        language = language,
        fontSize = fontSize,
        lineNumbers = lineNumbers,
        wordWrap = wordWrap,
        autocompleteEnabled = autocompleteEnabled,
        ghostSuggestion = ghostSuggestion,
        inlineDiffSuggestion = inlineDiffSuggestion,
        onCodeChange = onCodeChange,
        selection = selection,
        onSelectionChange = onSelectionChangedInternal,
        onRejectGhostSuggestion = onRejectGhostSuggestion,
        onAcceptGhostSuggestionLine = onAcceptGhostSuggestionLine,
        onAcceptGhostSuggestionFull = onAcceptGhostSuggestionFull,
        onAcceptInlineDiff = onAcceptInlineDiff,
        modifier = modifier
    )
}
@Composable
private fun EditorTerminalSection(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    val onToggleFullScreen = remember(uiState) { { uiState.isTerminalFullScreen = !uiState.isTerminalFullScreen } }
    val onClearTerminal = remember(viewModel) { { viewModel.terminalManager.clearTerminal() } }
    val onCloseTerminal = remember(uiState) {
        {
            uiState.showTerminal = false
            uiState.isTerminalFullScreen = false
        }
    }
    val onSendInput = remember(viewModel) {
        { input: String ->
            if (viewModel.terminalManager.isWaitingForInput.value) {
                viewModel.sendTerminalInput(input)
            } else {
                viewModel.chatWithAi(input)
            }
        }
    }
    val onShowHtmlPreview = remember(uiState) { { uiState.showHtmlPreview = true } }

    AnimatedVisibility(
        visible = uiState.showTerminal,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
        val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()

        TerminalPanel(
            terminalManager = viewModel.terminalManager,
            isFullScreen = uiState.isTerminalFullScreen,
            onToggleFullScreen = onToggleFullScreen,
            onClear = onClearTerminal,
            onClose = onCloseTerminal,
            onShowHtml = if (language == Language.HTML && htmlContent != null) onShowHtmlPreview else null,
            onSendInput = onSendInput,
            modifier = if (uiState.isTerminalFullScreen) Modifier.fillMaxSize() else Modifier
        )
    }
}

@Composable
private fun KeyboardAwareSpecialCharactersBar(
    onCharacterClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible by remember(density, imeInsets) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    AnimatedVisibility(
        visible = isKeyboardVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        SpecialCharactersBar(onCharacterClick = onCharacterClick)
    }
}

@Composable
private fun EditorDialogsHost(
    viewModel: EditorViewModel,
    uiState: EditorUiState
) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    val onDismissAiResult = remember(viewModel) { { viewModel.dismissAiResult() } }
    val onApplyAiCode = remember(viewModel) { { code: String -> viewModel.applyAiCode(code) } }
    val onFollowUp = remember(viewModel) { { question: String -> viewModel.askFollowUpQuestion(question) } }
    val onCloseDialog = remember(uiState) { { uiState.activeDialog = null } }
    val onConfirmAiWrite = remember(viewModel, uiState) {
        { prompt: String ->
            viewModel.writeCodeWithAi(prompt)
            uiState.activeDialog = null
        }
    }
    val onConfirmAiEdit = remember(viewModel, uiState) {
        { prompt: String ->
            viewModel.editCodeWithAi(prompt)
            uiState.activeDialog = null
        }
    }
    val onSaveProject = remember(viewModel, uiState) {
        { name: String ->
            viewModel.saveProject(name)
            uiState.activeDialog = null
        }
    }
    val onCreateNewFile = remember(viewModel, uiState) {
        { lang: Language ->
            viewModel.newFile(lang)
            uiState.activeDialog = null
        }
    }
    val onAddFile = remember(viewModel, uiState) {
        { name: String, selectedLang: Language ->
            val nameWithExt = if (name.contains(".")) name else name + selectedLang.extension
            viewModel.addFile(nameWithExt, selectedLang)
            uiState.activeDialog = null
        }
    }

    if (aiState is UiState.Loading) {
        AiLoadingDialog()
    } else if (aiState is UiState.Success) {
        val result = (aiState as UiState.Success<AiResult>).data
        if (!result.isEdit) {
            AiResultDialog(
                result = result,
                language = viewModel.currentLanguage.value,
                onApply = onApplyAiCode,
                onDismiss = onDismissAiResult,
                onAskFollowUp = onFollowUp,
                currentCode = viewModel.currentCode.value
            )
        }
    } else if (aiState is UiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissAiResult,
            title = { Text("AI Error") },
            text = { Text((aiState as UiState.Error).message) },
            confirmButton = {
                TextButton(onClick = onDismissAiResult) { Text("OK") }
            }
        )
    }

    when (uiState.activeDialog) {
        EditorDialogType.AI_WRITE -> {
            AiPromptDialog(
                title = "Write Code with AI",
                label = "Describe what you want to build or change",
                confirmText = "Generate",
                onConfirm = onConfirmAiWrite,
                onDismiss = onCloseDialog
            )
        }
        EditorDialogType.AI_EDIT -> {
            AiPromptDialog(
                title = "Edit Code with AI",
                label = "Describe what you want to modify",
                confirmText = "Edit",
                onConfirm = onConfirmAiEdit,
                onDismiss = onCloseDialog
            )
        }
        EditorDialogType.SAVE_PROJECT -> {
            SaveProjectDialog(
                currentName = viewModel.currentProjectName.value,
                onSave = onSaveProject,
                onDismiss = onCloseDialog
            )
        }
        EditorDialogType.NEW_FILE -> {
            NewFileDialog(
                onCreate = onCreateNewFile,
                onDismiss = onCloseDialog
            )
        }
        EditorDialogType.ADD_FILE -> {
            AddFileDialog(
                onAdd = onAddFile,
                onDismiss = onCloseDialog
            )
        }
        null -> Unit
    }

    val htmlContent = viewModel.htmlContent.value
    if (uiState.showHtmlPreview && htmlContent != null) {
        HtmlPreviewDialog(
            htmlContent = htmlContent!!,
            onDismiss = { uiState.showHtmlPreview = false }
        )
    }
}

@Composable
private fun AiPromptDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        onConfirm(prompt)
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddFileDialog(
    onAdd: (String, Language) -> Unit,
    onDismiss: () -> Unit
) {
    var newFileName by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(Language.PYTHON) }
    val allLanguages = remember { Language.values().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onAdd(newFileName, selectedLang)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LanguageSelectorMenu(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.focusable(false)
        ) {
            Icon(Icons.Default.SwapHoriz, "Change Language")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            Language.values().forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.icon} ${lang.displayName}") },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    },
                    leadingIcon = if (lang == currentLanguage) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun AiFeaturesMenu(
    onWriteCode: () -> Unit,
    onEditCode: () -> Unit,
    onFixBug: () -> Unit,
    onExplainCode: () -> Unit,
    onImproveCode: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.focusable(false)
        ) {
            Icon(Icons.Default.AutoAwesome, "AI Features", tint = Color(0xFFFFB74D))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(text = { Text("✨ Write Code with AI") }, onClick = { onWriteCode(); expanded = false })
            DropdownMenuItem(text = { Text("📝 Edit Code with AI") }, onClick = { onEditCode(); expanded = false })
            DropdownMenuItem(text = { Text("🔧 Fix Bug") }, onClick = { onFixBug(); expanded = false })
            DropdownMenuItem(text = { Text("💡 Explain Code") }, onClick = { onExplainCode(); expanded = false })
            DropdownMenuItem(text = { Text("⚡ Improve Code") }, onClick = { onImproveCode(); expanded = false })
        }
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
            textFieldValue = textFieldValue.copy(
                text = code,
                selection = if (selection.start <= code.length && selection.end <= code.length) selection 
                            else androidx.compose.ui.text.TextRange(code.length)
            )
        }
    }

    // Sync external selection changes
    LaunchedEffect(selection) {
        if (selection != textFieldValue.selection && selection.end <= textFieldValue.text.length) {
            textFieldValue = textFieldValue.copy(selection = selection)
        }
    }

    // Use a stable highlighter state that persists across recompositions
    // This maintains the token cache for incremental updates
    val highlighterState = remember { SyntaxHighlighterState() }
    
    // PERSISTENT highlighted code - initialized with current text, NOT empty
    // The key is using remember WITHOUT textFieldValue.text as a key
    // This prevents the "flash" by keeping the previous highlighting visible
    // while the LaunchedEffect updates it incrementally
    var highlightedCode by remember { 
        mutableStateOf(
            try {
                highlighterState.highlightIncremental(textFieldValue.text, language)
            } catch (e: Exception) {
                androidx.compose.ui.text.AnnotatedString(textFieldValue.text)
            }
        )
    }
    
    // Track last highlighted text hash to avoid redundant work
    var lastTextHash by remember { mutableStateOf(0) }
    
    // Update highlighting incrementally via LaunchedEffect
    // This runs AFTER the UI renders, so the previous highlighting stays visible
    // until the new highlighting is ready - NO FLASH!
    LaunchedEffect(textFieldValue.text, language) {
        val hash = textFieldValue.text.hashCode()
        if (hash != lastTextHash || highlighterState.currentLanguage != language) {
            lastTextHash = hash
            // The incremental highlighter uses cached tokens, so this is FAST
            highlightedCode = highlighterState.highlightIncremental(textFieldValue.text, language)
        }
    }
    
    // Apply ghost suggestions or inline diffs on top of highlighted code
    val displayCode = when {
        ghostSuggestion != null -> {
            val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
            val builder = androidx.compose.ui.text.AnnotatedString.Builder()
            builder.append(highlightedCode.subSequence(0, cursor))
            builder.withStyle(
                SpanStyle(
                    color = Color(0xFF888888),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            ) {
                append(ghostSuggestion)
            }
            builder.append(highlightedCode.subSequence(cursor, highlightedCode.length))
            builder.toAnnotatedString()
        }
        
        inlineDiffSuggestion != null &&
            inlineDiffSuggestion.deleteText != null &&
            inlineDiffSuggestion.addText != null -> {
            val builder = androidx.compose.ui.text.AnnotatedString.Builder()
            val deleteStart = inlineDiffSuggestion.editStartPos.coerceIn(0, textFieldValue.text.length)
            val deleteEnd = inlineDiffSuggestion.editEndPos.coerceIn(0, textFieldValue.text.length)

            builder.append(textFieldValue.text.substring(0, deleteStart))

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

            builder.append(textFieldValue.text.substring(deleteEnd))
            builder.toAnnotatedString()
        }
        
        else -> highlightedCode
    }


    val codeTextStyle = remember(fontSize) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5).sp
        )
    }
    val transparentCodeTextStyle = remember(codeTextStyle) { codeTextStyle.copy(color = Color.Transparent) }
    val lineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val lineNumberTextStyle = remember(codeTextStyle, lineNumberColor) {
        codeTextStyle.copy(color = lineNumberColor)
    }

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
    val autocompletePopupShape = remember { RoundedCornerShape(8.dp) }
    val autocompleteBadgeShape = remember { RoundedCornerShape(4.dp) }
    val autocompleteItemTextStyle = remember {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Update suggestions when code changes
    LaunchedEffect(textFieldValue.text, language, autocompleteEnabled) {
        val cursorPosition = textFieldValue.selection.start
        if (autocompleteEnabled && cursorPosition <= textFieldValue.text.length) {
            // Debounce autocomplete to avoid stuttering
            delay(200)
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
                        style = lineNumberTextStyle,
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
                        val textChanged = tfv.text != textFieldValue.text
                        val selectionChanged = tfv.selection != textFieldValue.selection

                        if (!textChanged && !selectionChanged) return@BasicTextField

                        if (!textChanged) {
                            textFieldValue = tfv
                            onSelectionChange(tfv.selection)
                            return@BasicTextField
                        }

                        var newText = tfv.text
                        var newSelection = tfv.selection
                        var newCursor = newSelection.start
                        
                        // Auto-closing brackets
                        if (tfv.composition == null && newText.length == textFieldValue.text.length + 1 && newCursor > 0) {
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
                        onCodeChange(newText)
                    },

                    onTextLayout = { layoutResult ->
                        val cursorPosition = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                        val newCursorRect = layoutResult.getCursorRect(cursorPosition)
                        if (newCursorRect != cursorRect) {
                            cursorRect = newCursorRect
                        }

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

                            val newVisualInfo = lineCount to mapping
                            if (newVisualInfo != visualLineInfo) {
                                visualLineInfo = newVisualInfo
                            }
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
                    textStyle = transparentCodeTextStyle,
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
                    shape = autocompletePopupShape,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        suggestions.forEach { item ->
                            key("${item.type}:${item.text}:${item.insertText}:${item.cursorOffset}") {
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
                                                autocompleteBadgeShape
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.text,
                                            style = autocompleteItemTextStyle,
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
        if (messages.isEmpty()) return@LaunchedEffect

        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val wasNearBottom = lastVisibleIndex == -1 || lastVisibleIndex >= messages.lastIndex - 1
        if (wasNearBottom) {
            listState.scrollToItem(messages.lastIndex)
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
            TerminalPanelHeader(
                isFullScreen = isFullScreen,
                onShowHtml = onShowHtml,
                onToggleFullScreen = onToggleFullScreen,
                onClear = onClear,
                onClose = onClose
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            TerminalMessagesList(
                messages = messages,
                listState = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            if (onSendInput != null) {
                TerminalInputBar(
                    isWaitingForInput = isWaitingForInput,
                    onSendInput = onSendInput
                )
            }
        }
    }
}

@Composable
private fun TerminalPanelHeader(
    isFullScreen: Boolean,
    onShowHtml: (() -> Unit)?,
    onToggleFullScreen: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
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
}

@Composable
private fun TerminalMessagesList(
    messages: List<com.pocketdev.app.execution.TerminalMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val terminalBaseTextStyle = remember {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val normalTextStyle = remember(terminalBaseTextStyle, onSurfaceColor) {
        terminalBaseTextStyle.copy(color = onSurfaceColor)
    }
    val emptyTextStyle = remember(terminalBaseTextStyle, onSurfaceVariantColor) {
        terminalBaseTextStyle.copy(color = onSurfaceVariantColor.copy(alpha = 0.6f))
    }
    val errorTextStyle = remember(terminalBaseTextStyle) { terminalBaseTextStyle.copy(color = Color(0xFFEF9A9A)) }
    val agentTextStyle = remember(terminalBaseTextStyle) { terminalBaseTextStyle.copy(color = Color(0xFF81C784)) }
    val statusTextStyle = remember(terminalBaseTextStyle) { terminalBaseTextStyle.copy(color = Color(0xFF64B5F6)) }
    val inputPromptTextStyle = remember(terminalBaseTextStyle) { terminalBaseTextStyle.copy(color = Color(0xFFFFD54F)) }

    if (messages.isEmpty()) {
        Box(
            modifier = modifier.padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = "(no output)", style = emptyTextStyle)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { index, msg -> "$index:${msg.type}:${msg.text.hashCode()}" }
            ) { _, msg ->
                val style = when (msg.type) {
                    com.pocketdev.app.execution.TerminalMessageType.NORMAL -> normalTextStyle
                    com.pocketdev.app.execution.TerminalMessageType.ERROR -> errorTextStyle
                    com.pocketdev.app.execution.TerminalMessageType.AGENT -> agentTextStyle
                    com.pocketdev.app.execution.TerminalMessageType.STATUS -> statusTextStyle
                    com.pocketdev.app.execution.TerminalMessageType.INPUT_PROMPT -> inputPromptTextStyle
                }
                Text(text = msg.text, style = style)
            }
        }
    }
}

@Composable
private fun TerminalInputBar(
    isWaitingForInput: Boolean,
    onSendInput: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val terminalBaseTextStyle = remember {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor2 = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor2 = MaterialTheme.colorScheme.onSurfaceVariant
    val promptTextStyle = remember(terminalBaseTextStyle, primaryColor) {
        terminalBaseTextStyle.copy(color = primaryColor)
    }
    val inputTextStyle = remember(terminalBaseTextStyle, onSurfaceColor2) {
        terminalBaseTextStyle.copy(color = onSurfaceColor2)
    }
    val placeholderTextStyle = remember(terminalBaseTextStyle, onSurfaceVariantColor2) {
        terminalBaseTextStyle.copy(color = onSurfaceVariantColor2.copy(alpha = 0.5f))
    }

    val submitInput = remember(onSendInput) {
        {
            if (inputText.isNotBlank()) {
                onSendInput(inputText)
                inputText = ""
            }
        }
    }

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
                style = promptTextStyle,
                modifier = Modifier.padding(end = 4.dp)
            )
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = inputTextStyle,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = { submitInput() }
                ),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            if (isWaitingForInput) "Enter input for script..." else "Type input here...",
                            style = placeholderTextStyle
                        )
                    }
                    innerTextField()
                }
            )
            IconButton(
                onClick = submitInput,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Send, "Send", modifier = Modifier.size(18.dp))
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
    val fieldTextStyle = remember {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }

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
                    textStyle = fieldTextStyle
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Replace", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = fieldTextStyle
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
    val proposedCodeShape = remember { RoundedCornerShape(8.dp) }
    val onSurfaceVariantColor3 = MaterialTheme.colorScheme.onSurfaceVariant
    val proposedCodeTextStyle = remember(onSurfaceVariantColor3) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = onSurfaceVariantColor3
        )
    }

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
                            shape = proposedCodeShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.correctedCode,
                                style = proposedCodeTextStyle,
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
    var name by remember(currentName) { mutableStateOf(currentName) }

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

/**
 * Represents a single syntax token with its position, text, and style type.
 * Used for incremental highlighting - only changed tokens are re-processed.
 */
internal data class SyntaxToken(
    val start: Int,
    val end: Int,
    val text: String,
    val type: String // KEYWORD, STRING, COMMENT, NUMBER, FUNCTION, TYPE, etc.
)

/**
 * Stateful highlighter that maintains a TOKEN cache for truly incremental updates.
 * 
 * KEY OPTIMIZATION: Instead of re-highlighting everything, we:
 * 1. Keep a cache of all highlighted tokens
 * 2. On change, find the minimal region that needs re-highlighting
 * 3. Preserve ALL other highlighting exactly as-is
 * 
 * This eliminates the "white flash" because old highlighting is always visible
 * while new highlighting is being computed.
 */
class SyntaxHighlighterState {
    // Token cache - stores all highlighted tokens with their positions
    private var tokenCache: List<SyntaxToken> = emptyList()
    
    var currentLanguage: Language? = null
        private set
    
    // Cached code and result for quick return
    private var cachedCode: String = ""
    private var cachedResult: androidx.compose.ui.text.AnnotatedString? = null
    
    /**
     * Incremental highlighting that ONLY re-processes changed tokens.
     * 
     * Algorithm:
     * 1. Find exact change region (start, end in new code)
     * 2. Find tokens that overlap with change region
     * 3. Keep tokens before change region unchanged
     * 4. Keep tokens after change region (adjust positions)
     * 5. Re-tokenize ONLY the affected region
     */
    fun highlightIncremental(code: String, language: Language): androidx.compose.ui.text.AnnotatedString {
        // Quick return for exact same code
        if (code == cachedCode && cachedResult != null && currentLanguage == language) {
            return cachedResult!!
        }
        
        // Language change requires full rebuild
        if (currentLanguage != language) {
            currentLanguage = language
            tokenCache = SyntaxHighlighter.tokenizeAll(code, language)
            cachedCode = code
            cachedResult = buildAnnotatedStringFromTokens(code, tokenCache)
            return cachedResult!!
        }
        
        // First time - do full highlight
        if (cachedCode.isEmpty()) {
            tokenCache = SyntaxHighlighter.tokenizeAll(code, language)
            cachedCode = code
            cachedResult = buildAnnotatedStringFromTokens(code, tokenCache)
            return cachedResult!!
        }
        
        // INCREMENTAL UPDATE
        val lengthDelta = code.length - cachedCode.length
        
        // Find change boundaries
        var changeStart = 0
        val minLen = minOf(cachedCode.length, code.length)
        while (changeStart < minLen && cachedCode[changeStart] == code[changeStart]) {
            changeStart++
        }
        
        // No actual change (shouldn't reach here but safety check)
        if (changeStart == minLen && cachedCode.length == code.length) {
            return cachedResult!!
        }
        
        // Find change end from the back
        var oldEnd = cachedCode.length - 1
        var newEnd = code.length - 1
        while (oldEnd >= changeStart && newEnd >= changeStart && cachedCode[oldEnd] == code[newEnd]) {
            oldEnd--
            newEnd--
        }
        val changeEnd = newEnd + 1
        
        // Partition tokens:
        // - beforeTokens: tokens that END before changeStart (unchanged)
        // - afterTokens: tokens that START after oldEnd+1 (just need position adjustment)
        // - affected: tokens in between (need re-tokenizing)
        
        val beforeTokens = mutableListOf<SyntaxToken>()
        val afterTokens = mutableListOf<SyntaxToken>()
        
        for (token in tokenCache) {
            when {
                token.end <= changeStart -> {
                    // Token is completely before the change - keep as-is
                    beforeTokens.add(token)
                }
                token.start > oldEnd + 1 -> {
                    // Token is completely after the change - adjust position
                    afterTokens.add(token.copy(
                        start = token.start + lengthDelta,
                        end = token.end + lengthDelta
                    ))
                }
                // Token overlaps with change region - will be re-tokenized
                // (implicitly skipped, will be part of newTokens)
            }
        }
        
        // Calculate region to re-tokenize
        // Start: end of last beforeToken, or 0 if none
        // End: start of first afterToken (in new coordinates), or code.length if none
        val retokenizeStart = beforeTokens.lastOrNull()?.end ?: 0
        val retokenizeEnd = afterTokens.firstOrNull()?.start ?: code.length
        
        // Re-tokenize the affected region
        val regionText = code.substring(retokenizeStart, retokenizeEnd.coerceAtMost(code.length))
        val newTokens = SyntaxHighlighter.tokenizeRegion(regionText, language, retokenizeStart)
        
        // Update cache
        tokenCache = beforeTokens + newTokens + afterTokens
        cachedCode = code
        cachedResult = buildAnnotatedStringFromTokens(code, tokenCache)
        
        return cachedResult!!
    }
    
    private fun buildAnnotatedStringFromTokens(code: String, tokens: List<SyntaxToken>): androidx.compose.ui.text.AnnotatedString {
        return buildAnnotatedString {
            var lastEnd = 0
            
            for (token in tokens) {
                // Add unstyled text before this token
                if (token.start > lastEnd) {
                    withStyle(SpanStyle(color = SyntaxHighlighter.colorDefault)) {
                        append(code.substring(lastEnd, token.start))
                    }
                }
                
                // Add the styled token
                val color = SyntaxHighlighter.colorMap[token.type] ?: SyntaxHighlighter.colorDefault
                withStyle(SpanStyle(color = color)) {
                    append(token.text)
                }
                
                lastEnd = token.end
            }
            
            // Add any remaining unstyled text
            if (lastEnd < code.length) {
                withStyle(SpanStyle(color = SyntaxHighlighter.colorDefault)) {
                    append(code.substring(lastEnd))
                }
            }
        }
    }
    
    fun clear() {
        tokenCache = emptyList()
        cachedCode = ""
        cachedResult = null
    }
}

// Multi-line state tracking (internal for use by SyntaxHighlighter)
internal enum class MultiLineState {
    NORMAL,
    IN_BLOCK_COMMENT,
    IN_STRING
}

// Syntax Highlighter - highlights code text
object SyntaxHighlighter {
    private val colorKeyword = Color(0xFF569CD6)
    private val colorString = Color(0xFFCE9178)
    private val colorComment = Color(0xFF6A9955)
    private val colorNumber = Color(0xFFB5CEA8)
    private val colorFunction = Color(0xFFDCDCAA)
    private val colorType = Color(0xFF4EC9B0)
    private val colorOperator = Color(0xFFD4D4D4)
    private val colorTag = Color(0xFF569CD6)
    private val colorAttr = Color(0xFF9CDCFE)
    internal val colorDefault = Color(0xFFCDD9E5)  // Made internal for SyntaxHighlighterState
    private val colorPreprocessor = Color(0xFFC586C0)

    // Pre-compiled patterns for each language - created once and reused
    private val patternsMap = mapOf(
        Language.PYTHON to listOf(
            "COMMENT" to "#.*",
            "STRING" to "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'",
            "KEYWORD" to "\\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|yield|pass|break|continue|and|or|not|in|is|lambda|global|nonlocal|del|raise|assert|True|False|None|async|await)\\b",
            "TYPE" to "\\b(print|len|range|type|int|str|float|bool|list|dict|set|tuple|input|open|enumerate|zip|map|filter|sorted|reversed|any|all|max|min|sum|abs|round|isinstance|issubclass|super|property|staticmethod|classmethod)\\b",
            "NUMBER" to "\\b(0x[0-9A-Fa-f]+|\\d+\\.\\d*|\\.\\d+|\\d+)\\b",
            "FUNCTION" to "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()",
            "PREPROCESSOR" to "@\\w+",
            "OPERATOR" to "[+\\-*/=<>!&|^~%@]"
        ),
        Language.JAVASCRIPT to listOf(
            "COMMENT" to "//.*|/\\*[\\s\\S]*?\\*/",
            "STRING" to "`(?:[^`\\\\]|\\\\.)*`|\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'",
            "KEYWORD" to "\\b(var|let|const|function|class|if|else|for|while|do|try|catch|finally|return|throw|new|delete|typeof|instanceof|in|of|break|continue|switch|case|default|import|export|from|async|await|yield|extends|super|this|null|undefined|true|false|void)\\b",
            "TYPE" to "\\b(console|Math|Array|Object|String|Number|Boolean|Date|JSON|Promise|fetch|document|window|localStorage|sessionStorage|setTimeout|setInterval|clearTimeout|clearInterval|parseInt|parseFloat|isNaN|isFinite)\\b",
            "NUMBER" to "\\b(0x[0-9A-Fa-f]+|\\d+\\.\\d*|\\.\\d+|\\d+)\\b",
            "FUNCTION" to "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)(?=\\s*\\()",
            "OPERATOR" to "=>|[+\\-*/=<>!&|^~%]"
        ),
        Language.HTML to listOf(
            "COMMENT" to "<!--[\\s\\S]*?-->",
            "TAG" to "</?[a-zA-Z][a-zA-Z0-9]*|/?>",
            "ATTR" to "[a-zA-Z-]+(?=\\s*=)",
            "STRING" to "\"[^\"]*\"|'[^']*'",
            "PREPROCESSOR" to "&[a-zA-Z]+;|&#\\d+;"
        ),
        Language.CSS to listOf(
            "COMMENT" to "/\\*[\\s\\S]*?\\*/|//.*",
            "FUNCTION" to "[.#][a-zA-Z][a-zA-Z0-9_-]*",
            "ATTR" to "\\b([a-zA-Z-]+)(?=\\s*:)",
            "STRING" to "\"[^\"]*\"|'[^']*'|#[0-9A-Fa-f]{3,8}\\b",
            "NUMBER" to "\\b(\\d+\\.?\\d*)(px|em|rem|vh|vw|%|pt|cm|mm|ex|ch|vmin|vmax)?\\b",
            "KEYWORD" to "\\b(important|px|em|rem|vh|vw|none|auto|block|flex|grid|inline|absolute|relative|fixed|sticky|inherit|initial|unset)\\b",
            "PREPROCESSOR" to "@[a-zA-Z-]+"
        ),
        Language.JAVA to listOf(
            "COMMENT" to "//.*|/\\*[\\s\\S]*?\\*/",
            "STRING" to "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'",
            "KEYWORD" to "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\\b",
            "TYPE" to "\\b(String|Integer|Long|Double|Float|Boolean|Character|Byte|Short|Object|System|Math|Arrays|ArrayList|HashMap|List|Map|Set|Iterator|Scanner|PrintStream|StringBuilder|StringBuffer)\\b|[A-Z][a-zA-Z0-9]*\\b",
            "NUMBER" to "\\b(0x[0-9A-Fa-f]+L?|\\d+\\.\\d*[fFdD]?|\\.\\d+[fFdD]?|\\d+[lLfFdD]?)\\b",
            "FUNCTION" to "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()",
            "PREPROCESSOR" to "@[A-Za-z]+"
        ),
        Language.CPP to listOf(
            "COMMENT" to "//.*|/\\*[\\s\\S]*?\\*/",
            "STRING" to "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'",
            "PREPROCESSOR" to "#\\s*(?:include|define|ifdef|ifndef|endif|else|elif|pragma|undef)\\b[^\\n]*",
            "KEYWORD" to "\\b(auto|bool|break|case|catch|char|class|const|continue|default|delete|do|double|else|enum|explicit|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|nullptr|operator|private|protected|public|register|return|short|signed|sizeof|static|struct|switch|template|this|throw|true|try|typedef|typename|union|unsigned|using|virtual|void|volatile|while)\\b",
            "TYPE" to "\\b(std|cout|cin|endl|string|vector|map|set|pair|array|queue|stack|algorithm|iostream|fstream|sstream)\\b",
            "NUMBER" to "\\b(0x[0-9A-Fa-f]+[uUlL]*|\\d+\\.\\d*[fFlL]?|\\.\\d+[fFlL]?|\\d+[uUlL]*)\\b",
            "FUNCTION" to "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()",
            "OPERATOR" to "::"
        ),
        Language.KOTLIN to listOf(
            "COMMENT" to "//.*|/\\*[\\s\\S]*?\\*/",
            "STRING" to "\"\"\"[\\s\\S]*?\"\"\"|\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'",
            "KEYWORD" to "\\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|delegate|do|dynamic|else|enum|expect|external|false|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|it|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|true|try|typealias|typeof|val|var|vararg|when|where|while)\\b",
            "TYPE" to "\\b(String|Int|Long|Double|Float|Boolean|Char|Byte|Short|Any|Unit|Nothing|Array|List|MutableList|Map|MutableMap|Set|MutableSet|Pair|Triple|Sequence|Flow|StateFlow|println|print|readLine|TODO)\\b",
            "NUMBER" to "\\b(0x[0-9A-Fa-f]+[lLuU]*|\\d+\\.\\d*[fF]?|\\.\\d+[fF]?|\\d+[lLuU]*)\\b",
            "FUNCTION" to "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()",
            "PREPROCESSOR" to "@[A-Za-z]+"
        ),
        Language.JSON to listOf(
            "ATTR" to "\"(?:[^\"\\\\]|\\\\.)*\"(?=\\s*:)",
            "STRING" to "\"(?:[^\"\\\\]|\\\\.)*\"",
            "KEYWORD" to "\\b(true|false|null)\\b",
            "NUMBER" to "\\b(-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)\\b",
            "OPERATOR" to "[{\\[\\]},:]"
        )
    )

    // Made internal for SyntaxHighlighterState to access
    internal val colorMap = mapOf(
        "KEYWORD" to colorKeyword,
        "STRING" to colorString,
        "COMMENT" to colorComment,
        "NUMBER" to colorNumber,
        "FUNCTION" to colorFunction,
        "TYPE" to colorType,
        "OPERATOR" to colorOperator,
        "TAG" to colorTag,
        "ATTR" to colorAttr,
        "PREPROCESSOR" to colorPreprocessor
    )

    // Lazy-initialized compiled patterns
    private val compiledPatterns = mutableMapOf<Language, java.util.regex.Pattern>()
    
    private fun getPattern(language: Language): java.util.regex.Pattern {
        return compiledPatterns.getOrPut(language) {
            val patterns = patternsMap[language] ?: emptyList()
            val combined = patterns.joinToString("|") { (name, pattern) -> "(?<$name>$pattern)" }
            java.util.regex.Pattern.compile(combined)
        }
    }

    /**
     * Tokenizes all code - used for initial highlighting or full refresh.
     * Returns a list of SyntaxToken objects representing all highlighted tokens.
     */
    internal fun tokenizeAll(code: String, language: Language): List<SyntaxToken> {
        val pattern = getPattern(language)
        val matcher = pattern.matcher(code)
        val tokens = mutableListOf<SyntaxToken>()
        
        while (matcher.find()) {
            // Find which group matched
            var matchedType: String? = null
            for ((name, _) in colorMap) {
                try {
                    val groupText = matcher.group(name)
                    if (groupText != null) {
                        matchedType = name
                        break
                    }
                } catch (_: Exception) {}
            }
            
            matchedType?.let { type ->
                tokens.add(SyntaxToken(
                    start = matcher.start(),
                    end = matcher.end(),
                    text = matcher.group(),
                    type = type
                ))
            }
        }
        
        return tokens
    }
    
    /**
     * Tokenizes a specific region of code - used for incremental highlighting.
     * Only processes tokens within the given region, adjusting positions with offset.
     */
    internal fun tokenizeRegion(code: String, language: Language, offset: Int = 0): List<SyntaxToken> {
        val pattern = getPattern(language)
        val matcher = pattern.matcher(code)
        val tokens = mutableListOf<SyntaxToken>()
        
        while (matcher.find()) {
            // Find which group matched
            var matchedType: String? = null
            for ((name, _) in colorMap) {
                try {
                    val groupText = matcher.group(name)
                    if (groupText != null) {
                        matchedType = name
                        break
                    }
                } catch (_: Exception) {}
            }
            
            matchedType?.let { type ->
                tokens.add(SyntaxToken(
                    start = matcher.start() + offset,
                    end = matcher.end() + offset,
                    text = matcher.group(),
                    type = type
                ))
            }
        }
        
        return tokens
    }
    
    /**
     * Highlights a single line. Used by SyntaxHighlighterState for incremental updates.
     * Internal visibility to avoid exposing MultiLineState.
     */
    internal fun highlightLine(line: String, language: Language, startState: MultiLineState): Pair<androidx.compose.ui.text.AnnotatedString, MultiLineState> {
        val pattern = getPattern(language)
        val matcher = pattern.matcher(line)
        
        val annotated = buildAnnotatedString {
            var lastEnd = 0
            
            while (matcher.find()) {
                // Add unhighlighted text before match
                if (matcher.start() > lastEnd) {
                    withStyle(SpanStyle(color = colorDefault)) {
                        append(line.substring(lastEnd, matcher.start()))
                    }
                }
                
                // Find which group matched and apply appropriate color
                var matched = false
                for ((name, color) in colorMap) {
                    try {
                        val groupText = matcher.group(name)
                        if (groupText != null) {
                            withStyle(SpanStyle(color = color)) {
                                append(groupText)
                            }
                            matched = true
                            break
                        }
                    } catch (_: Exception) {}
                }
                
                if (!matched) {
                    append(matcher.group())
                }
                lastEnd = matcher.end()
            }
            
            // Add remaining text
            if (lastEnd < line.length) {
                withStyle(SpanStyle(color = colorDefault)) {
                    append(line.substring(lastEnd))
                }
            }
        }
        
        // For now, we use a simplified state tracking
        // A full implementation would track multi-line strings and block comments
        return annotated to MultiLineState.NORMAL
    }
    
    /**
     * Full highlight function for backward compatibility
     */
    fun highlight(code: String, language: Language): androidx.compose.ui.text.AnnotatedString {
        val lines = code.split('\n')
        return buildAnnotatedString {
            lines.forEachIndexed { index, line ->
                val (annotated, _) = highlightLine(line, language, MultiLineState.NORMAL)
                append(annotated)
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
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
            "(", ")", "{", "}", "[", "]", "\"", "'", "<", ">", "=", ";", ":",
            ".", ",", "+", "-", "*", "/", "!", "?", "@", "#", "$", "%", "&", "_", "|", "\\", "`",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "->", "=>", "::"
        )
    }
    val scrollState = rememberScrollState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val chipShape = remember { RoundedCornerShape(8.dp) }
    val chipTextStyle = remember {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                specialChars.forEach { char ->
                    key(char) {
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onCharacterClick(char)
                                },
                            shape = chipShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            tonalElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = chipTextStyle,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
