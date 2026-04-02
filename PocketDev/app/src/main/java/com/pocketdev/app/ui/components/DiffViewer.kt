package com.pocketdev.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class DiffLine(val text: String, val type: DiffLineType)

enum class DiffLineType { UNCHANGED, ADDED, DELETED }

@Composable
fun DiffViewer(
    originalCode: String,
    newCode: String,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    var diffLines by remember { mutableStateOf<List<DiffLine>>(emptyList()) }

    LaunchedEffect(originalCode, newCode) {
        diffLines = withContext(Dispatchers.Default) {
            computeDiff(originalCode, newCode)
        }
    }

    val codeTextStyle = remember(fontSize) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5).sp
        )
    }
    val horizontalScrollState = rememberScrollState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .horizontalScroll(horizontalScrollState)
    ) {
        itemsIndexed(
            items = diffLines,
            key = { index, line -> "$index:${line.type}:${line.text.length}:${line.text.hashCode()}" }
        ) { _, line ->
            val backgroundColor = when (line.type) {
                DiffLineType.ADDED -> Color(0x404CAF50)
                DiffLineType.DELETED -> Color(0x40F44336)
                DiffLineType.UNCHANGED -> Color.Transparent
            }
            val textColor = when (line.type) {
                DiffLineType.ADDED -> Color(0xFF81C784)
                DiffLineType.DELETED -> Color(0xFFE57373)
                DiffLineType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
            }
            val prefix = when (line.type) {
                DiffLineType.ADDED -> "+ "
                DiffLineType.DELETED -> "- "
                DiffLineType.UNCHANGED -> "  "
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
            ) {
                Text(
                    text = prefix + line.text,
                    style = codeTextStyle,
                    color = textColor,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun computeDiff(original: String, revised: String): List<DiffLine> {
    val originalLines = original.lines()
    val revisedLines = revised.lines()
    val patch = DiffUtils.diff(originalLines, revisedLines)

    val result = mutableListOf<DiffLine>()
    var currentOriginalIndex = 0

    for (delta in patch.getDeltas()) {
        while (currentOriginalIndex < delta.source.position) {
            result.add(DiffLine(originalLines[currentOriginalIndex], DiffLineType.UNCHANGED))
            currentOriginalIndex++
        }

        when (delta.type) {
            DeltaType.INSERT -> {
                delta.target.lines.forEach { line ->
                    result.add(DiffLine(line as String, DiffLineType.ADDED))
                }
            }
            DeltaType.DELETE -> {
                delta.source.lines.forEach { line ->
                    result.add(DiffLine(line as String, DiffLineType.DELETED))
                }
                currentOriginalIndex += delta.source.lines.size
            }
            DeltaType.CHANGE -> {
                delta.source.lines.forEach { line ->
                    result.add(DiffLine(line as String, DiffLineType.DELETED))
                }
                delta.target.lines.forEach { line ->
                    result.add(DiffLine(line as String, DiffLineType.ADDED))
                }
                currentOriginalIndex += delta.source.lines.size
            }
            else -> {}
        }
    }

    while (currentOriginalIndex < originalLines.size) {
        result.add(DiffLine(originalLines[currentOriginalIndex], DiffLineType.UNCHANGED))
        currentOriginalIndex++
    }

    return result
}
