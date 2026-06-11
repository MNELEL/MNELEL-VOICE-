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
    val createdAt: Long = System.currentTimeMillis(),
    
    // Technical analysis dashboard metrics
    val frequencyHz: Int = 145, // גובה תדר ממוצע ב-Hz
    val clarityScore: Int = 85, // מדד חיתוך דיבור ומאפייני דיור (0-100)
    val pronunciationClarity: Int = 80, // סגנון הגייה וצורת הגייה (0-100)
    val intonationScore: Int = 78, // אינטונציה והתנגנות (0-100)
    val breathPauseScore: Int = 85, // סדירות נשימה והפסקות (0-100)
    val distortionLevel: Int = 12 // עיוותים ורעשי רקע (0-100)
)
