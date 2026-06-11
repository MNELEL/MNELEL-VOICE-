package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_generation_results")
data class VoiceGenerationResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val profileName: String,
    val inputText: String,
    val audioPath: String, // Absolute saved file path of the synthetic .mp3 audio
    val createdAt: Long = System.currentTimeMillis()
)
