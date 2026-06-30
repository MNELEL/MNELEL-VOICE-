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
    val pitch: String,
    val tone: String,
    val vibe: String,
    val pace: String,
    val geminiVoiceName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val frequencyHz: Int,
    val clarityScore: Int,
    val pronunciationClarity: Int,
    val intonationScore: Int,
    val breathPauseScore: Int,
    val distortionLevel: Int,
    val embedding: ByteArray? = null,
    val isDraft: Boolean = false
)
