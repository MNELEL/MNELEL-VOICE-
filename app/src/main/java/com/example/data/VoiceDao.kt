package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerationResult(result: VoiceGenerationResult): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStyleTemplate(template: VoiceStyleTemplate): Long

    @Query("SELECT * FROM voice_style_templates ORDER BY createdAt DESC")
    fun getAllStyleTemplates(): Flow<List<VoiceStyleTemplate>>

    @Query("DELETE FROM voice_style_templates WHERE id = :id")
    suspend fun deleteStyleTemplateById(id: Int)

    @Query("SELECT * FROM voice_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<VoiceProfile>>

    @Query("SELECT * FROM voice_generation_results ORDER BY createdAt DESC")
    fun getAllGenerationResults(): Flow<List<VoiceGenerationResult>>

    @Query("SELECT * FROM voice_generation_results WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getResultsForProfile(profileId: Int): Flow<List<VoiceGenerationResult>>

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Query("UPDATE voice_profiles SET name = :newName WHERE id = :id")
    suspend fun renameProfile(id: Int, newName: String)

    @Query("DELETE FROM voice_generation_results WHERE id = :id")
    suspend fun deleteGenerationResultById(id: Int)

    @Query("DELETE FROM voice_generation_results WHERE profileId = :profileId")
    suspend fun deleteResultsByProfileId(profileId: Int)
    @Query("DELETE FROM voice_profiles")
    suspend fun deleteAllProfiles()

    @Query("DELETE FROM voice_generation_results")
    suspend fun deleteAllGenerationResults()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosisReport(report: SpeechDiagnosisReport): Long

    @Query("SELECT * FROM speech_diagnosis_reports ORDER BY date DESC")
    fun getAllDiagnosisReports(): Flow<List<SpeechDiagnosisReport>>

    @Query("SELECT * FROM speech_diagnosis_reports WHERE profileId = :profileId ORDER BY date DESC")
    fun getReportsForProfile(profileId: Int): Flow<List<SpeechDiagnosisReport>>

    @Query("DELETE FROM speech_diagnosis_reports WHERE id = :id")
    suspend fun deleteDiagnosisReportById(id: Int)

    @Query("DELETE FROM speech_diagnosis_reports")
    suspend fun deleteAllDiagnosisReports()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueTask(task: DbQueueTask)

    @Query("SELECT * FROM tts_queue_tasks ORDER BY createdAt ASC")
    suspend fun getQueueTasks(): List<DbQueueTask>

    @Query("DELETE FROM tts_queue_tasks WHERE id = :id")
    suspend fun deleteQueueTaskById(id: String)

    @Query("DELETE FROM tts_queue_tasks")
    suspend fun clearAllQueueTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: AudioDraft)

    @Query("SELECT * FROM audio_drafts WHERE id = 'latest_draft'")
    suspend fun getLatestDraft(): AudioDraft?

    @Query("DELETE FROM audio_drafts WHERE id = 'latest_draft'")
    suspend fun clearDraft()
}
