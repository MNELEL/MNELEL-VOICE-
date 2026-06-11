package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<VoiceProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfile): Long

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    // Voice Generation Results operations (recent cloning results)
    @Query("SELECT * FROM voice_generation_results ORDER BY createdAt DESC")
    fun getAllGenerationResults(): Flow<List<VoiceGenerationResult>>

    @Query("SELECT * FROM voice_generation_results WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getResultsForProfile(profileId: Int): Flow<List<VoiceGenerationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerationResult(result: VoiceGenerationResult): Long

    @Query("DELETE FROM voice_generation_results WHERE id = :id")
    suspend fun deleteGenerationResultById(id: Int)

    @Query("DELETE FROM voice_generation_results WHERE profileId = :profileId")
    suspend fun deleteResultsByProfileId(profileId: Int)
}
