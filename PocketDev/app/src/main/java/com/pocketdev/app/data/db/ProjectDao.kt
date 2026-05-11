package com.pocketdev.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.app.data.models.Language
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query(
        """
        SELECT p.id,
               p.name,
               p.primaryLanguage AS language,
               p.createdAt,
               p.modifiedAt,
               p.description,
               p.fileCount,
               p.totalChars
        FROM projects p
        ORDER BY p.modifiedAt DESC
        """
    )
    fun getAllProjectSummaries(): Flow<List<ProjectSummaryRow>>

    @Query(
        """
        SELECT p.id,
               p.name,
               p.primaryLanguage AS language,
               p.createdAt,
               p.modifiedAt,
               p.description,
               p.fileCount,
               p.totalChars
        FROM projects p
        WHERE p.name LIKE '%' || :query || '%'
        ORDER BY p.modifiedAt DESC
        """
    )
    fun searchProjectSummaries(query: String): Flow<List<ProjectSummaryRow>>

    @Query(
        """
        SELECT p.id,
               p.name,
               p.primaryLanguage AS language,
               p.createdAt,
               p.modifiedAt,
               p.description,
               p.fileCount,
               p.totalChars
        FROM projects p
        WHERE p.primaryLanguage = :language
        ORDER BY p.modifiedAt DESC
        """
    )
    fun getProjectSummariesByLanguage(language: Language): Flow<List<ProjectSummaryRow>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectEntityById(id: Long): ProjectEntity?

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY sortOrder ASC, id ASC")
    suspend fun getProjectFiles(projectId: Long): List<ProjectFileEntity>

    @Query(
        """
        SELECT externalId, language
        FROM project_files
        WHERE projectId = :projectId AND externalId = :externalId
        LIMIT 1
        """
    )
    suspend fun getProjectFileRef(projectId: Long, externalId: String): ProjectFileRefRow?

    @Query(
        """
        SELECT externalId, language
        FROM project_files
        WHERE projectId = :projectId
        ORDER BY sortOrder ASC, id ASC
        LIMIT 1
        """
    )
    suspend fun getFirstProjectFileRef(projectId: Long): ProjectFileRefRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesForProject(projectId: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT COUNT(*) FROM project_files WHERE projectId = :projectId")
    suspend fun getProjectFileCount(projectId: Long): Int

    @Query(
        """
        SELECT COUNT(*) AS fileCount,
               COALESCE(SUM(charCount), 0) AS totalChars
        FROM project_files
        WHERE projectId = :projectId
        """
    )
    suspend fun getProjectFileStats(projectId: Long): ProjectFileStatsRow

    @Query(
        """
        UPDATE project_files
        SET content = :content,
            language = :language,
            charCount = :charCount,
            lineCount = :lineCount,
            modifiedAt = :modifiedAt
        WHERE projectId = :projectId AND externalId = :externalId
        """
    )
    suspend fun updateFileContent(
        projectId: Long,
        externalId: String,
        content: String,
        language: Language,
        charCount: Int,
        lineCount: Int,
        modifiedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE projects
        SET modifiedAt = :modifiedAt,
            primaryLanguage = :language,
            activeFileExternalId = :activeFileExternalId,
            fileCount = :fileCount,
            totalChars = :totalChars
        WHERE id = :projectId
        """
    )
    suspend fun updateProjectMetadata(
        projectId: Long,
        language: Language,
        activeFileExternalId: String?,
        fileCount: Int,
        totalChars: Long,
        modifiedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE projects SET activeFileExternalId = :externalId WHERE id = :projectId")
    suspend fun setActiveFile(projectId: Long, externalId: String?)
}
