package com.example.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_drafts")
data class AudioDraft(
    @PrimaryKey val id: String = "latest_draft",
    val audioBase64: String,
    val timestamp: Long = System.currentTimeMillis()
)
