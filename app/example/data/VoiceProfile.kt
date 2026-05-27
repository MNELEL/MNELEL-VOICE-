package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gender: String,
    val description: String,
    val audioPath: String?,
    val pitch: String = "לא נותח",
    val tone: String = "לא נותח",
    val vibe: String = "לא נותח",
    val pace: String = "לא נותח",
    val geminiVoiceName: String = "Puck",
    val createdAt: Long = System.currentTimeMillis()
)
