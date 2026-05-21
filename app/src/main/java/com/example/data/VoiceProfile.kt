package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val analysisData: String,
    val audioPath: String,
    val dateCreated: Long = System.currentTimeMillis()
)
