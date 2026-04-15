package com.pocketdev.app.repository

import android.content.Context
import androidx.room.withTransaction
import com.pocketdev.app.data.db.AppDatabase
import com.pocketdev.app.data.db.ProjectEntity
import com.pocketdev.app.data.db.ProjectFileEntity
import com.pocketdev.app.data.db.ProjectSummaryRow
import com.pocketdev.app.data.models.Language
import com.pocketdev.app.data.models.Project
import com.pocketdev.app.data.models.ProjectFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProjectRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val projectDao = database.projectDao()

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjectSummaries()
        .map { rows -> rows.map { it.toProjectListItem() } }

    fun searchProjects(query: String): Flow<List<Project>> =
        projectDao.searchProjectSummaries(query).map { rows -> rows.map { it.toProjectListItem() } }

    fun getProjectsByLanguage(language: Language): Flow<List<Project>> =
        projectDao.getProjectSummariesByLanguage(language).map { rows -> rows.map { it.toProjectListItem() } }

    suspend fun getProjectById(id: Long): Project? {
        val project = projectDao.getProjectEntityById(id) ?: return null
        val files = projectDao.getProjectFiles(id)
        return project.toDomain(files)
    }

    suspend fun createProject(
        name: String,
        language: Language,
        code: String = getDefaultCode(language)
    ): Long {
        val defaultFileName = "main${language.extension}"
        val initialFile = ProjectFile(
            name = defaultFileName,
            language = language,
            code = code
        )
        return saveProject(
            Project(
                name = name,
                language = language,
                code = code,
                files = listOf(initialFile),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveProject(project: Project): Long {
        val now = System.currentTimeMillis()
        val normalizedFiles = ensureProjectFiles(project)
        return database.withTransaction {
            if (project.id == 0L) {
                val insertedId = projectDao.insertProject(
                    ProjectEntity(
                        name = project.name,
                        primaryLanguage = normalizedFiles.firstOrNull()?.language ?: project.language,
                        createdAt = now,
                        modifiedAt = now,
                        description = project.description,
                        activeFileExternalId = normalizedFiles.firstOrNull()?.id
                    )
                )
                projectDao.insertFiles(
                    normalizedFiles.mapIndexed { index, file ->
                        file.toEntity(insertedId, index, now)
                    }
                )
                insertedId
            } else {
                projectDao.updateProject(
                    ProjectEntity(
                        id = project.id,
                        name = project.name,
                        primaryLanguage = normalizedFiles.firstOrNull()?.language ?: project.language,
                        createdAt = project.createdAt,
                        modifiedAt = now,
                        description = project.description,
                        activeFileExternalId = normalizedFiles.firstOrNull()?.id
                    )
                )
                projectDao.deleteFilesForProject(project.id)
                projectDao.insertFiles(
                    normalizedFiles.mapIndexed { index, file ->
                        file.toEntity(project.id, index, now)
                    }
                )
                project.id
            }
        }
    }

    suspend fun updateFileContent(
        projectId: Long,
        fileExternalId: String,
        code: String,
        language: Language
    ) {
        val now = System.currentTimeMillis()
        val lineCount = code.lineCount()

        database.withTransaction {
            val normalizedExternalId = if (fileExternalId.isBlank()) UUID.randomUUID().toString() else fileExternalId
            val updated = projectDao.updateFileContent(
                projectId = projectId,
                externalId = normalizedExternalId,
                content = code,
                language = language,
                charCount = code.length,
                lineCount = lineCount,
                modifiedAt = now
            )

            if (updated == 0) {
                val existingCount = projectDao.getProjectFiles(projectId).size
                projectDao.insertFiles(
                    listOf(
                        ProjectFileEntity(
                            projectId = projectId,
                            externalId = normalizedExternalId,
                            name = "main${language.extension}",
                            language = language,
                            content = code,
                            sortOrder = existingCount,
                            charCount = code.length,
                            lineCount = lineCount,
                            modifiedAt = now
                        )
                    )
                )
            }

            projectDao.updateProjectMetadata(
                projectId = projectId,
                language = language,
                activeFileExternalId = normalizedExternalId,
                modifiedAt = now
            )
        }
    }

    suspend fun updateCode(projectId: Long, code: String) {
        val project = projectDao.getProjectEntityById(projectId) ?: return
        val files = projectDao.getProjectFiles(projectId)
        val targetExternalId = files.firstOrNull()?.externalId ?: UUID.randomUUID().toString()
        updateFileContent(projectId, targetExternalId, code, files.firstOrNull()?.language ?: project.primaryLanguage)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProjectById(project.id)
    }

    suspend fun duplicateProject(project: Project): Long {
        val source = getProjectById(project.id) ?: return 0L
        val now = System.currentTimeMillis()
        val copy = source.copy(
            id = 0,
            name = "${source.name} (copy)",
            createdAt = now,
            modifiedAt = now,
            files = source.files.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        return saveProject(copy)
    }

    private fun ensureProjectFiles(project: Project): List<ProjectFile> {
        val source = project.files
        if (source.isNotEmpty()) {
            return source.map { file ->
                if (file.id.isBlank()) file.copy(id = UUID.randomUUID().toString()) else file
            }
        }

        val fallbackCode = if (project.code.isNotBlank()) project.code else getDefaultCode(project.language)
        return listOf(
            ProjectFile(
                name = "main${project.language.extension}",
                language = project.language,
                code = fallbackCode
            )
        )
    }

    private fun ProjectSummaryRow.toProjectListItem(): Project {
        return Project(
            id = id,
            name = name,
            language = language,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            description = description,
            files = emptyList(),
            code = ""
        )
    }

    private fun ProjectEntity.toDomain(fileEntities: List<ProjectFileEntity>): Project {
        val sortedFiles = fileEntities.sortedBy { it.sortOrder }.map { it.toDomain() }
        val primary = sortedFiles.firstOrNull()?.language ?: primaryLanguage
        val code = sortedFiles.firstOrNull()?.code.orEmpty()
        return Project(
            id = id,
            name = name,
            language = primary,
            code = code,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            description = description,
            files = sortedFiles
        )
    }

    private fun ProjectFileEntity.toDomain(): ProjectFile {
        return ProjectFile(
            id = externalId,
            name = name,
            language = language,
            code = content
        )
    }

    private fun ProjectFile.toEntity(projectId: Long, sortOrder: Int, now: Long): ProjectFileEntity {
        val external = if (id.isBlank()) UUID.randomUUID().toString() else id
        return ProjectFileEntity(
            projectId = projectId,
            externalId = external,
            name = name,
            language = language,
            content = code,
            sortOrder = sortOrder,
            charCount = code.length,
            lineCount = code.lineCount(),
            modifiedAt = now
        )
    }

    private fun String.lineCount(): Int {
        if (isEmpty()) return 0
        var count = 1
        for (c in this) {
            if (c == '\n') count++
        }
        return count
    }

    private fun getDefaultCode(language: Language): String {
        return when (language) {
            Language.PYTHON -> "# Python - Hello World\nprint(\"Hello, World!\")\n\n# Your code here\n"
            Language.JAVASCRIPT -> "// JavaScript - Hello World\nconsole.log(\"Hello, World!\");\n\n// Your code here\n"
            Language.HTML -> """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Page</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        h1 { color: #333; }
    </style>
</head>
<body>
    <h1>Hello, World!</h1>
    <p>Welcome to my HTML page.</p>

    <script>
        console.log("Page loaded!");
    </script>
</body>
</html>"""
            Language.CSS -> "/* CSS Styles */\nbody {\n    font-family: Arial, sans-serif;\n    background-color: #f5f5f5;\n    margin: 0;\n    padding: 20px;\n}\n\nh1 {\n    color: #333;\n    text-align: center;\n}\n"
            Language.JAVA -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n"
            Language.CPP -> "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello, World!\" << endl;\n    return 0;\n}\n"
            Language.KOTLIN -> "fun main() {\n    println(\"Hello, World!\")\n}\n"
            Language.JSON -> "{\n    \"name\": \"My Project\",\n    \"version\": \"1.0.0\",\n    \"description\": \"A sample JSON file\"\n}\n"
        }
    }
}
