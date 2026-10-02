package com.pocketdev.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pocketdev.app.data.models.Language
import com.pocketdev.app.editor.SoraAutocompleteAdapter
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

private const val LARGE_FILE_CHARS = 120_000

/**
 * Debounced text synchronization controller.
 *
 * - Schedules a single delayed job per burst of keystrokes; the expensive
 *   `editor.text.toString()` materialization happens ONLY when the debounce
 *   actually fires.
 * - [lastSynced] lets the AndroidView update block decide whether an external
 *   `setText` is needed WITHOUT copying the whole buffer per recomposition.
 * - [cancelPending] prevents a stale debounce (e.g. from a previous tab) from
 *   overwriting freshly loaded content.
 */
@Stable
private class TextSyncer(private val scope: CoroutineScope) {
    private var job: kotlinx.coroutines.Job? = null
    var lastSynced: String = ""
    fun schedule(supply: () -> String, onEmit: (String) -> Unit) {
        job?.cancel()
        job = scope.launch {
            kotlinx.coroutines.delay(100) // 100ms debounce
            val text = supply()
            lastSynced = text
            onEmit(text)
        }
    }
    fun cancelPending() { job?.cancel() }
}

private fun lineColumnToOffset(text: CharSequence, line: Int, column: Int): Int {
    if (line <= 0) return column.coerceIn(0, text.length)
    var currentLine = 0
    var index = 0
    while (index < text.length && currentLine < line) {
        if (text[index] == '\n') currentLine++
        index++
    }
    return (index + column).coerceIn(0, text.length)
}

private fun getCursorIndex(pos: Any, text: CharSequence): Int {
    return runCatching {
        (pos.javaClass.getMethod("getIndex").invoke(pos) as? Int ?: 0)
            .coerceIn(0, text.length)
    }.recoverCatching {
        pos.javaClass.getDeclaredField("index").also { it.isAccessible = true }
            .get(pos) as? Int ?: 0
    }.recoverCatching {
        val line = pos.javaClass.getMethod("getLine").invoke(pos) as? Int ?: 0
        val col = pos.javaClass.getMethod("getColumn").invoke(pos) as? Int ?: 0
        lineColumnToOffset(text, line, col)
    }.getOrDefault(0)
}


private fun getCursorLine(pos: Any): Int {
    return runCatching {
        pos.javaClass.getMethod("getLine").invoke(pos) as? Int ?: 0
    }.recoverCatching {
        pos.javaClass.getDeclaredField("line").also { it.isAccessible = true }
            .get(pos) as? Int ?: 0
    }.getOrDefault(0).coerceAtLeast(0)
}

private fun getCursorColumn(pos: Any): Int {
    return runCatching {
        pos.javaClass.getMethod("getColumn").invoke(pos) as? Int ?: 0
    }.recoverCatching {
        pos.javaClass.getDeclaredField("column").also { it.isAccessible = true }
            .get(pos) as? Int ?: 0
    }.getOrDefault(0).coerceAtLeast(0)
}

/**
 * Memory-efficient cursor position tracking with debounced updates.
 * Reduces unnecessary recompositions during rapid cursor movement.
 */
@Stable
private class CursorTracker {
    private var lastCursorPosition: Int = 0
    private var lastScrollX: Float = 0f
    private var lastScrollY: Float = 0f
    
    fun updatePosition(position: Int, scrollX: Float = 0f, scrollY: Float = 0f): Boolean {
        if (position == lastCursorPosition && scrollX == lastScrollX && scrollY == lastScrollY) {
            return false
        }
        lastCursorPosition = position
        lastScrollX = scrollX
        lastScrollY = scrollY
        return true
    }
    
    fun getPosition(): Int = lastCursorPosition
}


@OptIn(FlowPreview::class)
@Composable
fun EditorCoreView(
    code: String,
    language: Language,
    fontSize: Int,
    isDark: Boolean,
    ghostSuggestion: String?,
    onCodeChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit,
    onCursorPositionChange: (Float, Float) -> Unit = { _, _ -> },
    selectionOffset: Int? = null,
    onGhostAccepted: (String) -> Unit = {},
    lineNumbers: Boolean = true,
    wordWrap: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Flag to differentiate between user edits and external code updates
    var internalUpdate by remember { mutableStateOf(false) }
    // Memoize language and autocomplete adapter per language to avoid recreation on every recomposition
    val memoizedLang = remember(language) { TextMateLanguage.create(language.toScopeName(), true) }
    val memoizedAdapter = remember(language) { SoraAutocompleteAdapter(language) }
    val memoizedDelegatedLang = remember(language) {
        val adapter = memoizedAdapter
        val lang = memoizedLang
        runCatching { adapter.createDelegatedLanguage(lang) }
            .getOrNull() ?: lang
    }

    // --- Large-file handling: expensive features are auto-disabled -------------
    // Word-wrap forces full relayout on every edit; skip it for very large files.
    val isLargeFile = code.length > LARGE_FILE_CHARS
    val effectiveWordWrap = wordWrap && !isLargeFile

    // --- Debounced text sync ------------------------------------------------------
    // Key optimizations over the naive approach:
    //  1. The full text is materialized (editor.text.toString()) only when the
    //     debounce actually fires — NOT on every keystroke (critical for 1MB files).
    //  2. lastSynced tracks the last text we know about so the AndroidView update
    //     block never needs to copy the whole buffer just to compare.
    //  3. Pending debounce jobs are cancelled when an external setText happens,
    //     which prevents a stale edit from a previous file overwriting the new one.
    val coroutineScope = rememberCoroutineScope()
    val latestOnCodeChange by rememberUpdatedState(onCodeChange)
    val latestOnCursorChange by rememberUpdatedState(onCursorChange)
    val latestOnCursorPositionChange by rememberUpdatedState(onCursorPositionChange)
    val latestSelectionOffset by rememberUpdatedState(selectionOffset)
    val latestOnGhostAccepted by rememberUpdatedState(onGhostAccepted)
    val latestGhostSuggestion by rememberUpdatedState(ghostSuggestion)
    val latestEffectiveWordWrap by rememberUpdatedState(effectiveWordWrap)

    val textSyncer = remember { TextSyncer(coroutineScope) }
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.sp.toPx() }

    val editorRef = remember { mutableStateOf<CodeEditor?>(null) }
    var currentCursorLine by remember { mutableStateOf(0) }
    var currentCursorColumn by remember { mutableStateOf(0) }
    
    // Memory-efficient cursor tracking
    val cursorTracker = remember { CursorTracker() }
    
    // Initialize the syncer with the starting text
    LaunchedEffect(Unit) { textSyncer.lastSynced = code }
    

    // Cache the color scheme — recreate only when theme changes (stable key)
    val cachedColorScheme = remember("darcula") {
        runCatching {
            val themeRegistry = ThemeRegistry.getInstance()
            themeRegistry.setTheme("darcula")
            TextMateColorScheme.create(themeRegistry)
        }.getOrNull()
    }

    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CodeEditor(context).also { editor ->
                editorRef.value = editor
                try {
                    // Basic editor config
                    runCatching { editor.isLineNumberEnabled = lineNumbers }
                    runCatching { editor.isWordwrap = effectiveWordWrap }
                    runCatching { editor.setTextSizePx(textSizePx) }

                    // Theme: apply cached color scheme
                    cachedColorScheme?.let { editor.colorScheme = it }

                    // Language with syntax highlighting + autocomplete
                    val textMateLang = memoizedLang
                    val delegatedLang = runCatching { memoizedAdapter.createDelegatedLanguage(textMateLang) }
                        .map { it as? io.github.rosemoe.sora.langs.textmate.TextMateLanguage ?: textMateLang }
                        .getOrDefault(textMateLang)
                    runCatching { editor.setEditorLanguage(delegatedLang as io.github.rosemoe.sora.langs.textmate.TextMateLanguage) }
                        .recoverCatching { editor.setEditorLanguage(textMateLang) }

                    if (code.isNotEmpty()) editor.setText(code)
                    // Mark initial content as synced so the first AndroidView
                    // update pass does not repeat setText (avoids an extra full
                    // buffer pass on first frame for large files).
                    textSyncer.lastSynced = code

                    // Text change listener with debounce for large files.
                    // NOTE: the full text is only materialized inside the debounce
                    // coroutine, not per keystroke.
                    runCatching {
                        editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                            if (internalUpdate) return@subscribeEvent
                            runCatching {
                                textSyncer.schedule(
                                    supply = { editor.text.toString() },
                                    onEmit = { newText -> latestOnCodeChange(newText) }
                                )
                            }
                        }
                    }

                    // Cursor position listener with memory-efficient tracking
                    runCatching {
                        editor.subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                            val cursorIdx = getCursorIndex(event.left, editor.text)
                            val line = getCursorLine(event.left)
                            val column = getCursorColumn(event.left)
                            currentCursorLine = line
                            currentCursorColumn = column

                            if (cursorTracker.updatePosition(cursorIdx)) {
                                latestOnCursorChange(cursorIdx)
                            }

                            runCatching {
                                val rowHeight = editor.javaClass.getMethod("getRowHeight")
                                    .invoke(editor) as? Float ?: 40f
                                val charWidth = textSizePx * 0.62f
                                val x = (column * charWidth) - editor.scroller.currX
                                val y = (line * rowHeight) - editor.scroller.currY
                                latestOnCursorPositionChange(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
                            }
                        }
                    }

                    // Tab key → accept ghost suggestion
                    runCatching {
                        editor.setOnKeyListener { _, keyCode, event ->
                            val ghost = latestGhostSuggestion
                            if (keyCode == android.view.KeyEvent.KEYCODE_TAB &&
                                event?.action == android.view.KeyEvent.ACTION_DOWN &&
                                !ghost.isNullOrEmpty()
                            ) {
                                latestOnGhostAccepted(ghost)
                                true
                            } else {
                                false
                            }
                        }
                    }
                    
                    // Smooth scrolling is enabled by default in Sora editor
                } catch (t: Throwable) {
                    android.util.Log.e("PocketDev", "Editor init failed: ${t.message}")
                    if (code.isNotEmpty()) runCatching { editor.setText(code) }
                }
            }
        },
        update = { editor ->
            runCatching { editor.isLineNumberEnabled = lineNumbers }
            runCatching { editor.isWordwrap = latestEffectiveWordWrap }
            runCatching { editor.setTextSizePx(textSizePx) }

            // Update language if scope changed
            val currentScope = runCatching {
                val lang = editor.editorLanguage
                // Try direct getScopeName
                runCatching { lang?.javaClass?.getMethod("getScopeName")?.invoke(lang) as? String }
                    .recoverCatching {
                        // Try accessing 'base' field for DelegatedLanguage
                        val baseField = lang?.javaClass?.getDeclaredField("base")
                        baseField?.isAccessible = true
                        val base = baseField?.get(lang)
                        base?.javaClass?.getMethod("getScopeName")?.invoke(base) as? String
                    }.getOrNull()
            }.getOrNull()

            if (currentScope != language.toScopeName()) {
                runCatching {
                    // Reuse memoized instances — no per-recomposition grammar loading
                    val delegatedLang = runCatching { memoizedAdapter.createDelegatedLanguage(memoizedLang) }
                        .map { it as? io.github.rosemoe.sora.langs.textmate.TextMateLanguage ?: memoizedLang }
                        .getOrDefault(memoizedLang)
                    runCatching { editor.setEditorLanguage(delegatedLang as io.github.rosemoe.sora.langs.textmate.TextMateLanguage) }
                        .recoverCatching { editor.setEditorLanguage(memoizedLang) }
                }
            }

            // Sync external code changes.
            // PERF: compare against the lightweight lastSynced marker instead of
            // copying the entire editor buffer (editor.text.toString()) on every
            // recomposition — critical to keep large files buttery smooth.
            if (code != textSyncer.lastSynced) {
                // Cancel any pending user-edit emission so a stale debounce from a
                // previous file/tab can never overwrite this fresh content.
                textSyncer.cancelPending()
                // Prevent triggering the ContentChangeEvent handler while updating programmatically
                internalUpdate = true
                val cursorPos = latestSelectionOffset?.coerceIn(0, code.length)
                    ?: getCursorIndex(editor.cursor.left, editor.text)
                runCatching {
                    editor.setText(code)
                    textSyncer.lastSynced = code
                    val pos = cursorPos.coerceAtMost(editor.text.length)
                    runCatching { editor.setSelection(pos, pos) }
                        .recoverCatching {
                            editor.javaClass.methods.firstOrNull {
                                it.name == "setSelection" && it.parameterTypes.size == 1
                            }?.invoke(editor, pos)
                        }
                }
                internalUpdate = false
            }
        }
    )

    // Smooth scroll tracking
    LaunchedEffect(editorRef.value, textSizePx) {
        val editor = editorRef.value ?: return@LaunchedEffect
        snapshotFlow {
            Pair(editor.scroller.currX, editor.scroller.currY)
        }
            .debounce(16)
            .distinctUntilChanged()
            .collect {
                runCatching {
                    val rowHeight = editor.javaClass.getMethod("getRowHeight")
                        .invoke(editor) as? Float ?: 40f
                    val charWidth = textSizePx * 0.62f
                    val x = (currentCursorColumn * charWidth) - editor.scroller.currX
                    val y = (currentCursorLine * rowHeight) - editor.scroller.currY
                    latestOnCursorPositionChange(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            editorRef.value?.release()
            editorRef.value = null
        }
    }
}

private fun Language.toScopeName() = when (this) {
    Language.PYTHON -> "source.python"
    Language.JAVASCRIPT -> "source.js"
    Language.KOTLIN -> "source.kotlin"
    Language.JAVA -> "source.java"
    Language.HTML -> "text.html.basic"
    Language.CSS -> "source.css"
    Language.CPP -> "source.cpp"
    Language.JSON -> "source.json"
}
