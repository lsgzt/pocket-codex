package com.pocketdev.app.data.models

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class ProjectFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val language: Language,
    val code: String
)

@Immutable
data class ProjectSummary(
    val id: Long,
    val name: String,
    val language: Language,
    val createdAt: Long,
    val modifiedAt: Long,
    val description: String = "",
    val fileCount: Int = 0,
    val totalChars: Long = 0L
)

enum class Language(val displayName: String, val extension: String, val icon: String) {
    PYTHON("Python", ".py", "🐍"),
    JAVASCRIPT("JavaScript", ".js", "🟨"),
    HTML("HTML", ".html", "🌐"),
    CSS("CSS", ".css", "🎨"),
    JAVA("Java", ".java", "☕"),
    CPP("C++", ".cpp", "⚡"),
    KOTLIN("Kotlin", ".kt", "🎯"),
    JSON("JSON", ".json", "📋");

    companion object {
        fun fromDisplayName(name: String): Language {
            return values().find { it.displayName == name } ?: PYTHON
        }

        fun fromExtension(ext: String): Language {
            return values().find { it.extension == ext } ?: PYTHON
        }

        fun executableLanguages() = listOf(PYTHON, JAVASCRIPT, HTML)
        fun highlightLanguages() = values().toList()
        fun autocompleteLanguages() = values().filter { it != JSON }
    }
}

@Immutable
data class Project(
    val id: Long = 0,
    val name: String,
    val language: Language,
    val code: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val files: List<ProjectFile> = emptyList()
)
