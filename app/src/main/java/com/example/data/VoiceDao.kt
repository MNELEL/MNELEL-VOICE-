package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_profiles ORDER BY dateCreated DESC")
    fun getAllProfiles(): Flow<List<VoiceProfile>>

    @Insert
    suspend fun insert(voiceProfile: VoiceProfile)

    @Delete
    suspend fun delete(voiceProfile: VoiceProfile)

    @Query("DELETE FROM voice_profiles")
    suspend fun deleteAll()
}
