package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_diagnosis_reports")
data class SpeechDiagnosisReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val profileName: String,
    val date: Long = System.currentTimeMillis(),
    
    // Acoustic biometric parameters at snapshot time
    val pitchHz: Int,
    val clarityScore: Int,
    val pronunciationClarity: Int,
    val intonationScore: Int,
    val breathPauseScore: Int,
    val distortionLevel: Int,
    
    // High-level diagnostic snapshots
    val fatigueScore: Int,
    val emotionalTemperament: String,
    val aiGeneratedReport: String, // The Gemini markdown report
    
    // User notes or custom label
    val labelText: String = "אבחון סגנון דיבור קולי"
)
