package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClonerStep { RECORDING, ANALYSIS, SAVING }

class VoiceClonerViewModel(private val db: AppDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceClonerUiState())
    val uiState: StateFlow<VoiceClonerUiState> = _uiState
    val savedProfiles = db.voiceDao().getAllProfiles()

    private var timerJob: Job? = null
    private var speakingJob: Job? = null

    fun setStep(step: ClonerStep) {
        _uiState.update { it.copy(currentStep = step) }
        if (step == ClonerStep.RECORDING) {
            resetRecordingState()
        }
    }

    fun togglePhoneticAccuracy(enabled: Boolean) {
        _uiState.update { it.copy(phoneticAccuracy = enabled) }
    }

    fun setSelectedAccent(accent: String) {
        _uiState.update { it.copy(selectedAccent = accent) }
    }

    fun updatePitchLevel(level: Float) {
        _uiState.update { it.copy(pitchLevel = level) }
    }

    fun updateSpeedLevel(level: Float) {
        _uiState.update { it.copy(speedLevel = level) }
    }

    fun updateEmotionalDepth(level: Float) {
        _uiState.update { it.copy(emotionalDepth = level) }
    }

    fun toggleRecording() {
        val current = _uiState.value.isRecording
        if (!current) {
            _uiState.update { it.copy(isRecording = true, recordingDuration = 0) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
                }
            }
        } else {
            stopRecordingInternal()
        }
    }

    private fun stopRecordingInternal() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
    }

    private fun resetRecordingState() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRecording = false, recordingDuration = 0) }
    }

    fun saveProfile(name: String, analysis: String, path: String) {
        viewModelScope.launch {
            val displayName = name.ifBlank { "פרופיל קולי שנוצר" }
            val stats = "מבטא: ${_uiState.value.selectedAccent} | פונטי: ${if (_uiState.value.phoneticAccuracy) "פעיל" else "כבוי"}"
            db.voiceDao().insert(VoiceProfile(name = displayName, analysisData = stats, audioPath = path))
        }
    }

    fun deleteProfile(profile: VoiceProfile) {
        viewModelScope.launch {
            db.voiceDao().delete(profile)
        }
    }

    fun clearAllProfiles() {
        viewModelScope.launch {
            db.voiceDao().deleteAll()
        }
    }

    fun startSpeakingSimulation(text: String, profileName: String) {
        speakingJob?.cancel()
        _uiState.update { it.copy(speakingText = text, speakingProfileName = profileName) }
        speakingJob = viewModelScope.launch {
            delay(5000) // Simulate finishing speaking after 5 seconds
            _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
        }
    }

    fun stopSpeakingSimulation() {
        speakingJob?.cancel()
        _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        speakingJob?.cancel()
    }
}

data class VoiceClonerUiState(
    val currentStep: ClonerStep = ClonerStep.RECORDING,
    val analysisDirection: String = "סטנדרטי",
    val phoneticAccuracy: Boolean = false,
    val selectedAccent: String = "מבטא ישראלי צברי",
    val pitchLevel: Float = 0.5f,
    val speedLevel: Float = 0.5f,
    val emotionalDepth: Float = 0.6f,
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val speakingText: String? = null,
    val speakingProfileName: String? = null
)
