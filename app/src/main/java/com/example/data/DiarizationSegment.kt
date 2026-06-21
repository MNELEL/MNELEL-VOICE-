package com.example.data

data class DiarizationSegment(
    val id: String,
    val startTime: String,
    val endTime: String,
    val text: String,
    val detectedSpeakerName: String,
    val confidence: Int,
    val voiceCharacteristics: String,
    var assignedProfileId: Int? = null, // Matched VoiceProfile.id (null if UNKNOWN)
    var assignedProfileName: String? = null // Human-friendly display name of the matched profile
)
