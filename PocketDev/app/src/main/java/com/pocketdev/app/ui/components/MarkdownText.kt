package com.pocketdev.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val INLINE_MARKDOWN_REGEX = Regex("\\*\\*(.*?)\\*\\*|`(.*?)`")

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant
    val inlineCodeColor = MaterialTheme.colorScheme.onSurfaceVariant

    val annotatedString = remember(text, inlineCodeBackground, inlineCodeColor) {
        buildAnnotatedString {
            var currentIndex = 0
            val matches = INLINE_MARKDOWN_REGEX.findAll(text)

            for (match in matches) {
                if (match.range.first < currentIndex) continue

                append(text.substring(currentIndex, match.range.first))

                if (match.groups[1] != null) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[1]!!.value)
                    }
                } else if (match.groups[2] != null) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = inlineCodeBackground,
                            color = inlineCodeColor
                        )
                    ) {
                        append(match.groups[2]!!.value)
                    }
                }

                currentIndex = match.range.last + 1
            }

            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium
    )
}
