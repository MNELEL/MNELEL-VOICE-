package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_style_templates")
data class VoiceStyleTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val tags: String, // Comma-separated tags, e.g. "#אנרגטי,#רציני"
    val instructions: String,
    val examplePhrases: String, // Comma-separated sentences
    val createdBy: String = "משתמש",
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
