package com.pocketdev.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pocketdev.app.data.models.Language

@Entity(
    tableName = "projects",
    indices = [Index("modifiedAt"), Index("name")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val primaryLanguage: Language,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val activeFileExternalId: String? = null
)

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "externalId"], unique = true),
        Index(value = ["projectId", "sortOrder"])
    ]
)
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val externalId: String,
    val name: String,
    val language: Language,
    val content: String,
    val sortOrder: Int,
    val charCount: Int,
    val lineCount: Int,
    val modifiedAt: Long = System.currentTimeMillis()
)

data class ProjectSummaryRow(
    val id: Long,
    val name: String,
    val language: Language,
    val createdAt: Long,
    val modifiedAt: Long,
    val description: String,
    val fileCount: Int,
    val totalChars: Long
)
