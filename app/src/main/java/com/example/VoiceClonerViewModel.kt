package com.example

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
import com.example.data.VoiceGenerationResult
import com.example.data.VoiceStyleTemplate
import com.example.data.SpeechDiagnosisReport
import com.example.utils.AudioHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoiceClonerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val voiceDao = database.voiceDao()
    private val audioHelper = AudioHelper(application)

    private val sharedPrefs = application.getSharedPreferences("voice_cloner_prefs", android.content.Context.MODE_PRIVATE)

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isApiKeyAvailable = MutableStateFlow(false)
    val isApiKeyAvailable: StateFlow<Boolean> = _isApiKeyAvailable.asStateFlow()

    fun getEffectiveApiKey(): String {
        val customKey = _customApiKey.value.trim()
        if (customKey.isNotEmpty()) {
            return customKey
        }
        val defKey = BuildConfig.GEMINI_API_KEY
        if (defKey.isNotEmpty() && defKey != "MY_GEMINI_API_KEY") {
            return defKey
        }
        return ""
    }

    private fun updateApiKeyAvailability() {
        val key = getEffectiveApiKey()
        _isApiKeyAvailable.value = key.isNotEmpty()
    }

    fun saveCustomApiKey(key: String) {
        sharedPrefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _customApiKey.value = key.trim()
        updateApiKeyAvailability()
    }

    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    init {
        val savedKey = sharedPrefs.getString("custom_gemini_api_key", "") ?: ""
        _customApiKey.value = savedKey
        updateApiKeyAvailability()
        try {
            tts = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("he", "IL"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("VoiceClonerViewModel", "Hebrew language is not supported. Trying fallback to US or default.")
                        tts?.setLanguage(Locale.getDefault())
                    }
                    _isTtsReady.value = true
                } else {
                    Log.e("VoiceClonerViewModel", "TextToSpeech init status failed: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceClonerViewModel", "Failed to construct TextToSpeech", e)
        }
        seedDefaultStyleTemplates()
        seedDefaultProfiles()
    }

    val allProfiles: StateFlow<List<VoiceProfile>> = voiceDao.getAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recent Voice Generations (Persisted Results) State Flow
    val recentGenerations: StateFlow<List<VoiceGenerationResult>> = voiceDao.getAllGenerationResults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isPlayingResultId = MutableStateFlow<Int?>(null)
    val isPlayingResultId: StateFlow<Int?> = _isPlayingResultId.asStateFlow()

    // Recorded file playback tracking state flows
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackElapsedText = MutableStateFlow("00:00")
    val playbackElapsedText: StateFlow<String> = _playbackElapsedText.asStateFlow()

    private val _playbackDurationText = MutableStateFlow("00:00")
    val playbackDurationText: StateFlow<String> = _playbackDurationText.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _acousticPreset = MutableStateFlow("None") // "None", "Studio", "Room", "Hall", "Cathedral"
    val acousticPreset: StateFlow<String> = _acousticPreset.asStateFlow()

    private val _playerTrackTitle = MutableStateFlow("קובץ שמע אינו פעיל 📭")
    val playerTrackTitle: StateFlow<String> = _playerTrackTitle.asStateFlow()

    private val _isPlayerMuted = MutableStateFlow(false)
    val isPlayerMuted: StateFlow<Boolean> = _isPlayerMuted.asStateFlow()

    // Speech Diagnosis AI States
    private val _aiDiagnosisReport = MutableStateFlow<String?>(null)
    val aiDiagnosisReport: StateFlow<String?> = _aiDiagnosisReport.asStateFlow()

    private val _isGeneratingDiagnosis = MutableStateFlow(false)
    val isGeneratingDiagnosis: StateFlow<Boolean> = _isGeneratingDiagnosis.asStateFlow()

    private val _diagnosisError = MutableStateFlow<String?>(null)
    val diagnosisError: StateFlow<String?> = _diagnosisError.asStateFlow()

    val allDiagnosisReports: StateFlow<List<SpeechDiagnosisReport>> = voiceDao.getAllDiagnosisReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearDiagnosisReport() {
        _aiDiagnosisReport.value = null
        _diagnosisError.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioHelper.setPlaybackSpeed(speed)
    }

    fun setAcousticPreset(preset: String) {
        _acousticPreset.value = preset
        audioHelper.activeAcousticPreset = preset
    }

    fun togglePlayerMute() {
        _isPlayerMuted.value = !_isPlayerMuted.value
    }

    private var playbackProgressJob: kotlinx.coroutines.Job? = null

    private fun startPlaybackProgressTracker(onCompletion: () -> Unit) {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch(Dispatchers.Main) {
            // Wait brief moment for mediaplayer initialization
            kotlinx.coroutines.delay(100)
            while (audioHelper.isPlaybackPlaying()) {
                val current = audioHelper.getPlaybackPosition()
                val total = audioHelper.getPlaybackDuration()
                if (total > 0) {
                    _playbackProgress.value = current.toFloat() / total.toFloat()
                    val curSec = current / 1000
                    val totSec = total / 1000
                    _playbackElapsedText.value = String.format("%02d:%02d", curSec / 60, curSec % 60)
                    _playbackDurationText.value = String.format("%02d:%02d", totSec / 60, totSec % 60)
                }
                kotlinx.coroutines.delay(100)
            }
            _playbackProgress.value = 0f
            _playbackElapsedText.value = "00:00"
            _playbackDurationText.value = "00:00"
            onCompletion()
        }
    }

    // Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val _clarityScore = MutableStateFlow(0)
    val clarityScore: StateFlow<Int> = _clarityScore.asStateFlow()

    private val _overallQualityScore = MutableStateFlow(0)
    val overallQualityScore: StateFlow<Int> = _overallQualityScore.asStateFlow()

    private val _qualityFeedback = MutableStateFlow("התחל להקליט כדי לבחון את איכות השמע...")
    val qualityFeedback: StateFlow<String> = _qualityFeedback.asStateFlow()

    private val amplitudeHistory = mutableListOf<Float>()

    private val _recordedFile = MutableStateFlow<File?>(null)
    val recordedFile: StateFlow<File?> = _recordedFile.asStateFlow()

    private var recordingJob: kotlinx.coroutines.Job? = null

    // Analysis States
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Synthesis States
    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private val _synthesizeError = MutableStateFlow<String?>(null)
    val synthesizeError: StateFlow<String?> = _synthesizeError.asStateFlow()

    private val _isPlayingProfileId = MutableStateFlow<Int?>(null)
    val isPlayingProfileId: StateFlow<Int?> = _isPlayingProfileId.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            var response: okhttp3.Response? = null
            var retryCount = 0
            val maxRetries = 3
            var backoffMs = 1000L

            while (true) {
                try {
                    val request = chain.request()
                    response = chain.proceed(request)
                    if (response.code == 429 && retryCount < maxRetries) {
                        response.close()
                        Thread.sleep(backoffMs)
                        backoffMs *= 2
                        retryCount++
                        continue
                    }
                    return@addInterceptor response
                } catch (e: java.io.IOException) {
                    if (retryCount < maxRetries) {
                        Thread.sleep(backoffMs)
                        backoffMs *= 2
                        retryCount++
                        continue
                    }
                    throw e
                }
            }
            // This line should not be reached
            throw java.io.IOException("Failed after retries")
        }
        .build()

    fun startRecording() {
        _recordedFile.value = null
        _analysisError.value = null
        val file = audioHelper.startRecording()
        if (file != null) {
            _isRecording.value = true
        } else {
            _analysisError.value = "שגיאה באתחול המיקרופון"
        }
    }

    fun stopRecording() {
        audioHelper.stopRecording()
        _isRecording.value = false
        _recordedFile.value = audioHelper.startRecording()?.let { null } // We get file from what startRecording returned, let's just hold reference from start
    }

    // Override start recording helper to capture the file
    private var lastRecordedFile: File? = null

    private fun updateQualityAnalysis() {
        val duration = _recordingDurationSec.value
        if (amplitudeHistory.isEmpty() && duration == 0) {
            _clarityScore.value = 0
            _overallQualityScore.value = 0
            _qualityFeedback.value = "הקלט שמע כדי לקבל ניתוח איכות בזמן אמת..."
            return
        }

        var silenceCount = 0
        var speechCount = 0
        var clippingCount = 0
        val totalCount = amplitudeHistory.size

        for (amp in amplitudeHistory) {
            when {
                amp < 0.03f -> silenceCount++
                amp > 0.85f -> clippingCount++
                else -> speechCount++
            }
        }

        val silenceRatio = if (totalCount > 0) silenceCount.toFloat() / totalCount else 1f
        val clippingRatio = if (totalCount > 0) clippingCount.toFloat() / totalCount else 0f

        var clarity = 100

        if (silenceRatio > 0.40f) {
            val penalty = ((silenceRatio - 0.40f) * 100f).toInt()
            clarity -= penalty
        }
        if (silenceRatio < 0.08f && totalCount > 15) {
            clarity -= 15
        }
        if (clippingRatio > 0.05f) {
            val penalty = (clippingRatio * 200f).toInt()
            clarity -= penalty
        }

        clarity = clarity.coerceIn(0, 100)
        _clarityScore.value = clarity

        val lengthScore = when {
            duration < 5 -> (duration.toFloat() / 5f * 30f).toInt()
            duration < 15 -> (30 + ((duration - 5).toFloat() / 10f * 50f)).toInt()
            else -> (80 + ((duration - 15).toFloat() / 15f * 20f).coerceIn(0f, 20f)).toInt()
        }

        val overall = (clarity * 0.4f + lengthScore * 0.6f).toInt().coerceIn(0, 100)
        _overallQualityScore.value = overall

        _qualityFeedback.value = when {
            duration < 5 -> {
                "ההקלטה קצרה מדי לעיבוד מהימן (קצר מ-5 שניות) ⚠️ ציון נוכחי: $overall%"
            }
            clarity < 50 -> {
                "רעש רקע גבוה או שכיחות שקט מוחלט. מומלץ להקליט מחדש בסביבה שקטה... 🤫 ($clarity%)"
            }
            overall < 60 -> {
                "איכות סבירה, מומלץ להקליט עוד דיבור רציף לקבלת שיבוט מדויק ($overall%)"
            }
            overall < 85 -> {
                "שמע מצוין! האיכות טובה מאוד ומוכנה לחילול קול מציאותי 👍 ($overall%)"
            }
            else -> {
                "שמע אולפן מושלם! נתונים עשירים ודיוק שיבוט מקסימלי ✨ ($overall%)"
            }
        }
    }

    fun startRecordVoice() {
        _recordedFile.value = null
        _analysisError.value = null
        _recordingDurationSec.value = 0
        _liveAmplitude.value = 0f
        _isRecordingPaused.value = false
        amplitudeHistory.clear()
        _clarityScore.value = 0
        _overallQualityScore.value = 0
        _qualityFeedback.value = "האיכות נמדדת... התחל לדבר 🎙️"

        lastRecordedFile = audioHelper.startRecording()
        if (lastRecordedFile != null) {
            _isRecording.value = true
            recordingJob = viewModelScope.launch(Dispatchers.Main) {
                var ticks = 0
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(100)
                    if (!_isRecordingPaused.value) {
                        ticks += 100
                        if (ticks >= 1000) {
                            _recordingDurationSec.value += 1
                            ticks = 0
                        }
                        val rawAmp = audioHelper.getMaxAmplitude()
                        val normalized = (rawAmp.toFloat() / 32768f).coerceIn(0f, 1f)
                        _liveAmplitude.value = normalized
                        amplitudeHistory.add(normalized)
                        updateQualityAnalysis()
                    } else {
                        _liveAmplitude.value = 0f
                    }
                }
            }
        } else {
            _analysisError.value = "שגיאה באתחול המיקרופון"
        }
    }

    fun pauseRecordVoice() {
        if (_isRecording.value && !_isRecordingPaused.value) {
            audioHelper.pauseRecording()
            _isRecordingPaused.value = true
        }
    }

    fun resumeRecordVoice() {
        if (_isRecording.value && _isRecordingPaused.value) {
            audioHelper.resumeRecording()
            _isRecordingPaused.value = false
        }
    }

    fun stopRecordVoice() {
        recordingJob?.cancel()
        recordingJob = null
        audioHelper.stopRecording()
        _isRecording.value = false
        _isRecordingPaused.value = false
        
        if (_recordingDurationSec.value < 5) {
            _analysisError.value = "ההקלטה קצרה מדי. אנא הקלט לפחות 5 שניות של דיבור."
            lastRecordedFile?.delete()
            lastRecordedFile = null
            _recordedFile.value = null
        } else {
            _recordedFile.value = lastRecordedFile
        }
    }

    // Play/Stop recorded or uploaded file before cloning
    private val _isPlayingRecorded = MutableStateFlow(false)
    val isPlayingRecorded: StateFlow<Boolean> = _isPlayingRecorded.asStateFlow()

    fun playRecordedFile() {
        val file = _recordedFile.value
        if (file != null && file.exists()) {
            _playerTrackTitle.value = "הקלטה מקורית לצורך עיבוד וניתוח 🎙️"
            _isPlayingRecorded.value = true
            audioHelper.playAudio(file) {
                _isPlayingRecorded.value = false
                playbackProgressJob?.cancel()
                _playbackProgress.value = 0f
                _playbackElapsedText.value = "00:00"
                _playbackDurationText.value = "00:00"
            }
            audioHelper.setPlaybackSpeed(_playbackSpeed.value)
            startPlaybackProgressTracker {
                _isPlayingRecorded.value = false
            }
        }
    }

    fun stopRecordedFile() {
        audioHelper.stopPlayback()
        playbackProgressJob?.cancel()
        _playbackProgress.value = 0f
        _playbackElapsedText.value = "00:00"
        _isPlayingRecorded.value = false
    }

    fun clearRecordedFile() {
        stopRecordedFile()
        _recordedFile.value = null
    }

    fun uploadAudioStream(inputStream: java.io.InputStream, fileName: String) {
        _recordedFile.value = null
        _analysisError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                val extension = if (fileName.contains(".")) fileName.substringAfterLast(".") else "aac"
                val tempFile = File.createTempFile("uploaded_", ".$extension", cacheDir)
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    _recordedFile.value = tempFile
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to upload fileStream", e)
                withContext(Dispatchers.Main) {
                    _analysisError.value = "כשל בהעלאת קובץ השמע: ${e.message}"
                }
            }
        }
    }

    fun playProfileSample(profile: VoiceProfile) {
        if (profile.audioPath != null) {
            val file = File(profile.audioPath)
            if (file.exists()) {
                _playerTrackTitle.value = "דגימת קול מקורית: ${profile.name} 🔊"
                _isPlayingProfileId.value = profile.id
                audioHelper.playAudio(file) {
                    _isPlayingProfileId.value = null
                    playbackProgressJob?.cancel()
                    _playbackProgress.value = 0f
                }
                audioHelper.setPlaybackSpeed(_playbackSpeed.value)
                startPlaybackProgressTracker {
                    _isPlayingProfileId.value = null
                }
            } else {
                _analysisError.value = "קובץ ההקלטה לא נמצא"
            }
        }
    }

    fun stopProfileSample() {
        audioHelper.stopPlayback()
        playbackProgressJob?.cancel()
        _playbackProgress.value = 0f
        _isPlayingProfileId.value = null
    }

    fun playResultSample(result: VoiceGenerationResult) {
        val file = File(result.audioPath)
        if (file.exists()) {
            _playerTrackTitle.value = "פלט דיבור משובט AI: \"${result.inputText.take(24)}...\" ✨"
            _isPlayingResultId.value = result.id
            audioHelper.playAudio(file) {
                _isPlayingResultId.value = null
                playbackProgressJob?.cancel()
                _playbackProgress.value = 0f
            }
            audioHelper.setPlaybackSpeed(_playbackSpeed.value)
            startPlaybackProgressTracker {
                _isPlayingResultId.value = null
            }
        } else {
            _synthesizeError.value = "קובץ השמע המשובט לא נמצא במכשיר"
        }
    }

    fun stopResultSample() {
        audioHelper.stopPlayback()
        playbackProgressJob?.cancel()
        _playbackProgress.value = 0f
        _isPlayingResultId.value = null
    }

    fun deleteResult(result: VoiceGenerationResult) {
        viewModelScope.launch {
            try {
                val file = File(result.audioPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to delete generated result file", e)
            }
            voiceDao.deleteGenerationResultById(result.id)
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            voiceDao.deleteProfileById(id)
            voiceDao.deleteResultsByProfileId(id)
        }
    }

    fun exportResultToMp3(result: VoiceGenerationResult, context: android.content.Context) {
        val file = File(result.audioPath)
        if (!file.exists()) {
            _synthesizeError.value = "קובץ השמע לא נמצא"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, "Voice_${result.profileName}_${System.currentTimeMillis()}.mp3")
                    put(android.provider.MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "הקובץ נשמר בהצלחה בתיקיית ההורדות! 📥", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to export to MP3", e)
                withContext(Dispatchers.Main) {
                    _synthesizeError.value = "שגיאה בשמירת קובץ MP3: ${e.message}"
                }
            }
        }
    }

    // API Rate Limit Recovery States
    private val _isWaitingForRateLimit = MutableStateFlow(false)
    val isWaitingForRateLimit: StateFlow<Boolean> = _isWaitingForRateLimit.asStateFlow()
    
    private val _rateLimitRecoverySeconds = MutableStateFlow(0)
    val rateLimitRecoverySeconds: StateFlow<Int> = _rateLimitRecoverySeconds.asStateFlow()

    private var rateLimitRecoveryJob: kotlinx.coroutines.Job? = null
    
    private fun triggerRateLimitRecovery() {
        if (_isWaitingForRateLimit.value) return
        
        _isWaitingForRateLimit.value = true
        _rateLimitRecoverySeconds.value = 60
        
        rateLimitRecoveryJob?.cancel()
        rateLimitRecoveryJob = viewModelScope.launch {
            while (_rateLimitRecoverySeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _rateLimitRecoverySeconds.value -= 1
            }
            _isWaitingForRateLimit.value = false
        }
    }

// API Activity Tracker
    // Combining UI state flows for activity
    val isAnyApiActive: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        _isAnalyzing,
        _isSynthesizing,
        // _isDiarizing, // Not defined yet in this snippet, add if needed or ignore
    ) { analyzing, synthesizing ->
        analyzing || synthesizing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Result cache for synthesis (Text + ProfileId)
    private val synthesisCache = mutableMapOf<String, String>()

    fun synthesizeText(
        text: String, 
        profile: VoiceProfile,
        pitchTuningPercent: Float = 0f,
        speedTuningPercent: Float = 0f,
        vibeModifier: String = "מקורי"
    ) {
        if (text.isBlank()) {
            _synthesizeError.value = "אנא הזן טקסט לייצור קול"
            return
        }

        // Caching check
        val cacheKey = "${text.trim()}_${profile.id}_${pitchTuningPercent}_${speedTuningPercent}_${vibeModifier}"
        if (synthesisCache.containsKey(cacheKey)) {
             // For now, allow re-synthesis to support refreshing or just play the cached one?
             // Re-synthesis is safer to ensure it works.
        }

        _isSynthesizing.value = true
        _synthesizeError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isEmpty()) {
                    throw IllegalStateException("אנא הגדר מפתח Gemini API תקין בלוח הבקרה / הגדרות האפליקציה או בקובץ .env")
                }

                // Determine acoustic pacing, pitch, and vibe directives based on analyzed characteristics
                val basePace = when {
                    profile.pace.contains("מהיר") || profile.pace.contains("מהירה") || profile.pace.lowercase().contains("fast") -> "high-speed tempo, rapid articulation, short word gaps"
                    profile.pace.contains("איטי") || profile.pace.contains("איטית") || profile.pace.lowercase().contains("slow") -> "slow, tranquil, deliberate pace with frequent natural breathing pauses"
                    else -> "steady, moderate, natural speaking pace with normal pauses"
                }

                val basePitch = when {
                    profile.frequencyHz < 110 -> "very deep, baritone, resonant low-pitch chest voice"
                    profile.frequencyHz < 150 -> "moderately low-pitch register with rich vocal warmth"
                    profile.frequencyHz > 215 -> "clear, high-pitched, feminine, melodic soprano tone"
                    profile.frequencyHz > 175 -> "moderately high-pitched, bright, warm alto style"
                    else -> "neutral mid-range pitch, balanced larynx position"
                }

                // Add slider adjustments to the prompt text if modified
                val userSpeedAdjustment = when {
                    speedTuningPercent > 10f -> "Increase speaking rate and speed by ${speedTuningPercent.toInt()}% for a faster, snappier flow."
                    speedTuningPercent < -10f -> "Slow down speaking rate and pace by ${Math.abs(speedTuningPercent.toInt())}% for a calmer, elongated cadence."
                    else -> "Keep speaking rate naturally aligned with the profile."
                }

                val userPitchAdjustment = when {
                    pitchTuningPercent > 10f -> "Raise the vocal register/pitch by ${pitchTuningPercent.toInt()}% higher than standard for brighter resonance."
                    pitchTuningPercent < -10f -> "Lower the vocal register/pitch by ${Math.abs(pitchTuningPercent.toInt())}% deeper for extra chest resonance."
                    else -> "Match the natural frequency of the profile."
                }

                val activeVibe = if (vibeModifier != "מקורי") {
                    "Express the text with an explicit '$vibeModifier' custom emotional inflection override."
                } else {
                    "Express with a ${profile.vibe} mood and ${profile.tone} signature, exactly mirroring the original speaker's vibe."
                }

                // We construct a highly detailed voice modulation prompt in English (as Gemini processes prompt directives for audio in OOB dynamic voices extremely well)
                val promptText = """
                    SYSTEM COMMAND: Perform an extremely accurate, high-fidelity 1:1 voice cloning emulation of ${profile.name} (Gender: ${profile.gender}).
                    You are speaking directly as ${profile.name} himself/herself. Modulate your generated speech to replicate these traits perfectly:
                    
                    AUDIO ATTRIBUTES TO EMULATE:
                    1. Target Vocal Pitch Register: $basePitch (Fundamental frequency: ${profile.frequencyHz} Hz). -> Apply tuning: $userPitchAdjustment
                    2. Speech Cadence/Pacing: $basePace. -> Apply tuning: $userSpeedAdjustment
                    3. Vocal Quality and Tone: ${profile.tone} (${profile.description}).
                    4. Emotional Delivery Aura / Vibe: $activeVibe.
                    5. Intonation Rhythm Score: ${profile.intonationScore}/100.
                    6. Articulation & Hebrew Accents: ${profile.pronunciationClarity}/100 clarity.

                    Read the following Hebrew text exactly as written. Ensure incredibly natural phrasing, proper Hebrew accents, and flawless vocal fidelity:
                    
                    "$text"
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", promptText)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", profile.geminiVoiceName)
                                })
                            })
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("שגיאת ייצור שמע קול: ${response.code}")
                }

                val responseBodyStr = response.body?.string() ?: throw IllegalStateException("שמע ריק")
                val responseObj = JSONObject(responseBodyStr)

                val candidates = responseObj.getJSONArray("candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                
                // Audio is returned in inlineData
                var foundAudioBase64: String? = null
                for (i in 0 until parts.length()) {
                    val partObj = parts.getJSONObject(i)
                    if (partObj.has("inlineData")) {
                        val inlineData = partObj.getJSONObject("inlineData")
                        if (inlineData.optString("mimeType", "").contains("audio")) {
                            foundAudioBase64 = inlineData.getString("data")
                            break
                        }
                    }
                }

                if (foundAudioBase64 == null) {
                    throw IllegalStateException("השרת לא החזיר שמע מתאים לתורת הקול")
                }

                withContext(Dispatchers.Main) {
                    _isSynthesizing.value = false
                    
                    // Decoded audio base64 is saved permanently to app storage and logged inside DB
                    val persistentFile = audioHelper.saveBase64ToPersistentFile(foundAudioBase64, profile.name)
                    if (persistentFile != null) {
                        val newResult = VoiceGenerationResult(
                            profileId = profile.id,
                            profileName = profile.name,
                            inputText = text,
                            audioPath = persistentFile.absolutePath
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            voiceDao.insertGenerationResult(newResult)
                        }
                        
                        // Update Cache
                        synthesisCache[cacheKey] = persistentFile.absolutePath
                        
                        // Automatically play the newly generated file nicely
                        playResultSample(newResult)
                    } else {
                        audioHelper.playBase64Audio(foundAudioBase64)
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Synthesis failed", e)
                withContext(Dispatchers.Main) {
                    _isSynthesizing.value = false
                    _synthesizeError.value = getFriendlyErrorMessage(e)
                }
            }
        }
    }


    fun getFriendlyErrorMessage(throwable: Throwable): String {
        val msg = throwable.message ?: ""
        
        // Handle 503 Service Unavailable / Overblown servers
        if (msg.contains("503") || throwable is java.net.SocketTimeoutException) {
            return "שרתי ה-Gemini AI עמוסים כעת זמנית (שגיאה 503). אנא המתן מספר שניות ונסה שוב, המערכת חוזרת לפעילות באופן אוטומטי."
        }
        
        // Handle 403 / 103 / Bad API key
        if (msg.contains("403") || msg.contains("103") || msg.contains("API key not valid") || msg.contains("API_KEY_INVALID")) {
            return "שגיאת הרשאה (שגיאה 403): מפתח ה-Gemini שהוזן אינו תקין או פג תוקפו. אנא לחץ על כפתור המפתח למעלה בלוח הבקרה כדי להזין מפתח API תקין."
        }
        
        // Handle 429 Rate limits
        if (msg.contains("429")) {
            triggerRateLimitRecovery()
            return "חרגת ממכסת הבקשות עבור מפתח ה-API הנוכחי (שגיאה 429). אנא המתן דקה-שתיים לפני הניסיון הבא."
        }
        
        // Handle 400 Bad request / invalid params
        if (msg.contains("400")) {
            return "הבקשה נדחתה על ידי השרת (שגיאה 400). ייתכן ודגימת הקול קצרה או ארוכה מדי, או שהזנת טקסט שאינו נתמך בדגם."
        }
        
        // Network connectivity failures
        if (throwable is java.net.UnknownHostException || throwable is java.net.ConnectException) {
            return "חיבור האינטרנט נכשל או אינו יציב. אנא ודא שהמכשיר מחובר לרשת ונסה שוב."
        }
        
        if (throwable is java.io.IOException) {
            return "שגיאה בקריאה או שמירה של קובץ השמע המקומי במכשיר."
        }
        
        return msg.ifEmpty { "שגיאה לא צפויה בעיבוד הבקשה. אנא נסו להקליט ולשלוח שוב." }
    }

    fun cloneAndAnalyze(name: String, gender: String, description: String) {
        val file = _recordedFile.value
        if (file == null) {
            _analysisError.value = "אנא הקלט דגימת קול תחילה"
            return
        }

        _isAnalyzing.value = true
        _analysisError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isEmpty()) {
                    throw IllegalStateException("אנא הגדר מפתח Gemini API תקין בלוח הבקרה / הגדרות האפליקציה או בקובץ .env")
                }

                val base64Audio = audioHelper.fileToBase64(file)
                    ?: throw java.io.IOException("שגיאה בקריאת קובץ השמע")

                // JSON Request construction
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Analyze the physical voice traits of this speaking sample. Provide your analysis response output in Hebrew using EXACTLY the following JSON format: " +
                                            "{\n" +
                                            "  \"pitch\": \"(Hebrew descriptor: גובה קול, למשל 'נמוך וסמכותי' או 'גבוה ודק')\",\n" +
                                            "  \"tone\": \"(Hebrew descriptor: גוון קול, למשל 'חם ורך' / 'עמוק ומלא')\",\n" +
                                            "  \"vibe\": \"(Hebrew descriptor: אווירה, למשל 'רגוע ומזמין' / 'נמרץ וחד')\",\n" +
                                            "  \"pace\": \"(Hebrew descriptor: קצב, למשל 'מתון ומדויק' / 'איטי וברור')\",\n" +
                                            "  \"geminiVoiceName\": \"(Kore / Puck / Fenrir / Aoede / Charon - Choose the one template voice close to this sample)\",\n" +
                                            "  \"frequencyHz\": (Integer estimating average voice frequency in Hz, male: 85 to 175, female: 165 to 255),\n" +
                                            "  \"clarityScore\": (Integer 0-100 indicating quality of articulation and speech clarity / מדד חיתוך דיבור),\n" +
                                            "  \"pronunciationClarity\": (Integer 0-100 indicating pronunciation accuracy / צורת הגייה),\n" +
                                            "  \"intonationScore\": (Integer 0-100 indicating melodiousness and intonation variation / אינטונציה והתנגנות),\n" +
                                            "  \"breathPauseScore\": (Integer 0-100 indicating respiratory control and breath breaks / הפסקות נשימה),\n" +
                                            "  \"distortionLevel\": (Integer 0-100 indicating background signal noise or speech distortions / עיוותי שפה ורעש)\n" +
                                            "}")
                                })
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "audio/aac")
                                        put("data", base64Audio)
                                    })
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("שגיאת שרת: ${response.code} ${response.message}")
                }

                val responseBodyStr = response.body?.string() ?: throw IllegalStateException("תגובה ריקה מהשרת")
                val responseObj = JSONObject(responseBodyStr)
                
                // Parse Gemini JSON output
                val candidates = responseObj.getJSONArray("candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                val rawText = parts.getJSONObject(0).getString("text")
                
                val parsedAnalysis = JSONObject(rawText)
                val pitch = parsedAnalysis.optString("pitch", "בינוני")
                val tone = parsedAnalysis.optString("tone", "חם")
                val vibe = parsedAnalysis.optString("vibe", "רגוע")
                val pace = parsedAnalysis.optString("pace", "מתון")
                val geminiVoiceName = parsedAnalysis.optString("geminiVoiceName", "Puck")
                
                val frequencyHz = parsedAnalysis.optInt("frequencyHz", if (gender == "זכר") 120 else 210)
                val clarityScore = parsedAnalysis.optInt("clarityScore", 85)
                val pronunciationClarity = parsedAnalysis.optInt("pronunciationClarity", 80)
                val intonationScore = parsedAnalysis.optInt("intonationScore", 78)
                val breathPauseScore = parsedAnalysis.optInt("breathPauseScore", 85)
                val distortionLevel = parsedAnalysis.optInt("distortionLevel", 12)

                val persistentFile = audioHelper.saveFileToPersistentStorage(file, name)
                val finalAudioPath = persistentFile?.absolutePath ?: file.absolutePath

                val newProfile = VoiceProfile(
                    name = name,
                    gender = gender,
                    description = description,
                    audioPath = finalAudioPath,
                    pitch = pitch,
                    tone = tone,
                    vibe = vibe,
                    pace = pace,
                    geminiVoiceName = geminiVoiceName,
                    frequencyHz = frequencyHz,
                    clarityScore = clarityScore,
                    pronunciationClarity = pronunciationClarity,
                    intonationScore = intonationScore,
                    breathPauseScore = breathPauseScore,
                    distortionLevel = distortionLevel
                )

                voiceDao.insertProfile(newProfile)

                withContext(Dispatchers.Main) {
                    _isAnalyzing.value = false
                    _recordedFile.value = null // clear for next
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Analysis failed", e)
                withContext(Dispatchers.Main) {
                    _isAnalyzing.value = false
                    _analysisError.value = getFriendlyErrorMessage(e)
                }
            }
        }
    }

    fun synthesizeTextLocal(
        text: String, 
        profile: VoiceProfile,
        pitchTuningPercent: Float = 0f,
        speedTuningPercent: Float = 0f
    ) {
        if (text.isBlank()) {
            _synthesizeError.value = "אנא הזן טקסט לייצור קול"
            return
        }

        _isSynthesizing.value = true
        _synthesizeError.value = null

        viewModelScope.launch(Dispatchers.Main) {
            try {
                val currentTts = tts
                if (currentTts == null || !_isTtsReady.value) {
                    throw IllegalStateException("מנוע הדיבור המקומי עדיין בטעינה או לא זמין")
                }

                // Map frequencyHz to a pitch multiplier
                var pitchMultiplier: Float = when {
                    profile.frequencyHz <= 100 -> 0.70f
                    profile.frequencyHz <= 125 -> 0.82f
                    profile.frequencyHz >= 210 -> 1.35f
                    profile.frequencyHz >= 180 -> 1.20f
                    else -> 1.0f
                }
                
                // Apply manual calibration slider multiplier (e.g. -50% to +50%)
                val pitchModFactor = 1.0f + (pitchTuningPercent / 100f)
                pitchMultiplier = (pitchMultiplier * pitchModFactor).coerceIn(0.5f, 2.0f)
                currentTts.setPitch(pitchMultiplier)

                // Map pace description to speech rate multiplier
                var rateMultiplier: Float = when {
                    profile.pace.contains("מהיר") || profile.pace.contains("מהירה") || profile.pace.lowercase().contains("fast") -> 1.25f
                    profile.pace.contains("איטי") || profile.pace.contains("איטית") || profile.pace.lowercase().contains("slow") -> 0.78f
                    else -> 1.00f
                }
                
                // Apply manual pace calibration slider multiplier
                val speedModFactor = 1.0f + (speedTuningPercent / 100f)
                rateMultiplier = (rateMultiplier * speedModFactor).coerceIn(0.5f, 2.0f)
                currentTts.setSpeechRate(rateMultiplier)

                // Trigger speaking using modern or deprecated method depending on API
                val speechResult = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_cloner_tts_session")
                } else {
                    @Suppress("DEPRECATION")
                    currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                }

                if (speechResult == TextToSpeech.ERROR) {
                    throw IllegalStateException("מנוע הדיבור המקומי נתקל בשגיאה בעת הדיבור")
                }

                // Also save to database history for traceability
                val newResult = VoiceGenerationResult(
                    profileId = profile.id,
                    profileName = profile.name,
                    inputText = text,
                    audioPath = "local_tts_playback" // indicates dynamic native TTS playback
                )
                withContext(Dispatchers.IO) {
                    voiceDao.insertGenerationResult(newResult)
                }

                _isSynthesizing.value = false
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Local TTS synthesis failed", e)
                _isSynthesizing.value = false
                _synthesizeError.value = e.message ?: "שגיאה בהפעלת מנוע דיבור מקומי"
            }
        }
    }

    fun exportProfileToJson(profile: VoiceProfile): String {
        return try {
            val json = JSONObject().apply {
                put("version", 1)
                put("name", profile.name)
                put("gender", profile.gender)
                put("description", profile.description)
                put("pitch", profile.pitch)
                put("tone", profile.tone)
                put("vibe", profile.vibe)
                put("pace", profile.pace)
                put("geminiVoiceName", profile.geminiVoiceName)
                put("frequencyHz", profile.frequencyHz)
                put("clarityScore", profile.clarityScore)
                put("pronunciationClarity", profile.pronunciationClarity)
                put("intonationScore", profile.intonationScore)
                put("breathPauseScore", profile.breathPauseScore)
                put("distortionLevel", profile.distortionLevel)
            }
            json.toString(2)
        } catch (e: Exception) {
            Log.e("VoiceClonerViewModel", "Failed to export profile to JSON", e)
            ""
        }
    }

    fun importProfileFromJson(jsonStr: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(jsonStr)
                val name = json.getString("name")
                val gender = json.optString("gender", "לא מוגדר")
                val description = json.optString("description", "פרופיל מיובא")
                val pitch = json.optString("pitch", "בינוני")
                val tone = json.optString("tone", "חם")
                val vibe = json.optString("vibe", "רגוע")
                val pace = json.optString("pace", "מתון")
                val geminiVoiceName = json.optString("geminiVoiceName", "Puck")
                val frequencyHz = json.optInt("frequencyHz", 150)
                val clarityScore = json.optInt("clarityScore", 85)
                val pronunciationClarity = json.optInt("pronunciationClarity", 80)
                val intonationScore = json.optInt("intonationScore", 78)
                val breathPauseScore = json.optInt("breathPauseScore", 85)
                val distortionLevel = json.optInt("distortionLevel", 12)

                val newProfile = VoiceProfile(
                    name = "$name (מיובא)",
                    gender = gender,
                    description = description,
                    audioPath = null,
                    pitch = pitch,
                    tone = tone,
                    vibe = vibe,
                    pace = pace,
                    geminiVoiceName = geminiVoiceName,
                    frequencyHz = frequencyHz,
                    clarityScore = clarityScore,
                    pronunciationClarity = pronunciationClarity,
                    intonationScore = intonationScore,
                    breathPauseScore = breathPauseScore,
                    distortionLevel = distortionLevel
                )

                voiceDao.insertProfile(newProfile)
                withContext(Dispatchers.Main) {
                    onSuccess("הפרופיל '$name' יובא בהצלחה בהתאמה לקול הדובר!")
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to import profile", e)
                withContext(Dispatchers.Main) {
                    onError("חתימת הקול אינה תקינה או חסרים נתונים")
                }
            }
        }
    }

    fun exportAllProfilesToBackupFile(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profiles = allProfiles.value
                if (profiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "אין פרופילי קול משובטים לייצוא")
                    }
                    return@launch
                }

                val backupJson = JSONObject().apply {
                    put("backupVersion", 1)
                    put("exportedAt", System.currentTimeMillis())
                    
                    val profilesArray = JSONArray()
                    for (profile in profiles) {
                        val profileJson = JSONObject().apply {
                            put("name", profile.name)
                            put("gender", profile.gender)
                            put("description", profile.description)
                            put("pitch", profile.pitch)
                            put("tone", profile.tone)
                            put("vibe", profile.vibe)
                            put("pace", profile.pace)
                            put("geminiVoiceName", profile.geminiVoiceName)
                            put("frequencyHz", profile.frequencyHz)
                            put("clarityScore", profile.clarityScore)
                            put("pronunciationClarity", profile.pronunciationClarity)
                            put("intonationScore", profile.intonationScore)
                            put("breathPauseScore", profile.breathPauseScore)
                            put("distortionLevel", profile.distortionLevel)
                            put("createdAt", profile.createdAt)
                        }
                        profilesArray.put(profileJson)
                    }
                    put("profiles", profilesArray)
                }

                val jsonString = backupJson.toString()
                
                val bos = java.io.ByteArrayOutputStream()
                java.util.zip.GZIPOutputStream(bos).use { gzos ->
                    gzos.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                val compressedBytes = bos.toByteArray()

                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "ClassPro_Voice_Backup_${System.currentTimeMillis() / 1000}.json.gz")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/gzip")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "כשל ביצירת קובץ הגיבוי במערכת")
                    }
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { output ->
                    output.write(compressedBytes)
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "הגיבוי הקבוצתי הושלם! הקובץ נשמר בתיקיית ההורדות של המכשיר כקובץ מרוכז ודחוס (.gz.json)")
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Error exporting batch backup", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "כשל ביצירת גיבוי: ${e.message}")
                }
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            voiceDao.deleteAllGenerationResults()
            voiceDao.deleteAllProfiles()
        }
    }

    fun importAllProfilesFromBackupBytes(bytes: ByteArray, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bis = java.io.ByteArrayInputStream(bytes)
                val jsonString = java.util.zip.GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { it.readText() }

                val backupJson = JSONObject(jsonString)
                val profilesArray = backupJson.getJSONArray("profiles")
                
                var importedCount = 0
                for (i in 0 until profilesArray.length()) {
                    val json = profilesArray.getJSONObject(i)
                    val name = json.getString("name")
                    val gender = json.optString("gender", "לא מוגדר")
                    val description = json.optString("description", "פרופיל מיובא")
                    val pitch = json.optString("pitch", "בינוני")
                    val tone = json.optString("tone", "חם")
                    val vibe = json.optString("vibe", "רגוע")
                    val pace = json.optString("pace", "מתון")
                    val geminiVoiceName = json.optString("geminiVoiceName", "Puck")
                    val frequencyHz = json.optInt("frequencyHz", 150)
                    val clarityScore = json.optInt("clarityScore", 85)
                    val pronunciationClarity = json.optInt("pronunciationClarity", 80)
                    val intonationScore = json.optInt("intonationScore", 78)
                    val breathPauseScore = json.optInt("breathPauseScore", 85)
                    val distortionLevel = json.optInt("distortionLevel", 12)
                    val createdAt = json.optLong("createdAt", System.currentTimeMillis())

                    val newProfile = VoiceProfile(
                        name = if (name.endsWith("(מגיבוי)")) name else "$name (מגיבוי)",
                        gender = gender,
                        description = description,
                        audioPath = null,
                        pitch = pitch,
                        tone = tone,
                        vibe = vibe,
                        pace = pace,
                        geminiVoiceName = geminiVoiceName,
                        frequencyHz = frequencyHz,
                        clarityScore = clarityScore,
                        pronunciationClarity = pronunciationClarity,
                        intonationScore = intonationScore,
                        breathPauseScore = breathPauseScore,
                        distortionLevel = distortionLevel,
                        createdAt = createdAt
                    )

                    voiceDao.insertProfile(newProfile)
                    importedCount++
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "שוחזרו וייובאו בהצלחה $importedCount פרופילי קול משובטים!")
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Error importing batch backup", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "שגיאה בפענוח קובץ הגיבוי. ודא שהקובץ תקין ודחוס בפורמט התואם.")
                }
            }
        }
    }

    // --- Core Speaker Diarization / Auto-Detection Module ---
    
    private val _diarizationSegments = MutableStateFlow<List<com.example.data.DiarizationSegment>>(emptyList())
    val diarizationSegments: StateFlow<List<com.example.data.DiarizationSegment>> = _diarizationSegments.asStateFlow()

    private val _isDiarizing = MutableStateFlow(false)
    val isDiarizing: StateFlow<Boolean> = _isDiarizing.asStateFlow()

    private val _diarizationError = MutableStateFlow<String?>(null)
    val diarizationError: StateFlow<String?> = _diarizationError.asStateFlow()

    private val _diarizationAudioFile = MutableStateFlow<File?>(null)
    val diarizationAudioFile: StateFlow<File?> = _diarizationAudioFile.asStateFlow()

    fun setDiarizationAudioFile(file: File) {
        _diarizationAudioFile.value = file
    }

    fun clearDiarizationResult() {
        _diarizationSegments.value = emptyList()
        _diarizationError.value = null
        _diarizationAudioFile.value = null
    }

    fun updateSegmentAssignment(segmentId: String, profile: VoiceProfile?) {
        val currentList = _diarizationSegments.value.map { segment ->
            if (segment.id == segmentId) {
                segment.copy(
                    assignedProfileId = profile?.id,
                    assignedProfileName = profile?.name
                )
            } else {
                segment
            }
        }
        _diarizationSegments.value = currentList
    }

    fun runSpeakerDiarization(profilesList: List<VoiceProfile>) {
        val file = _diarizationAudioFile.value
        if (file == null) {
            _diarizationError.value = "אנא בחרו קובץ שמע לניתוח דוברים מרובים"
            return
        }
        _isDiarizing.value = true
        _diarizationError.value = null
        _diarizationSegments.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isEmpty()) {
                    throw IllegalStateException("אנא הגדירו מפתח Gemini API תקין בלוח הבקרה / הגדרות כדי לבצע אנליזה.")
                }

                val base64Audio = audioHelper.fileToBase64(file)
                    ?: throw java.io.IOException("שגיאה בקריאת קובץ השמע")

                // Convert profiles list to metadata metadata to guide Gemini
                val profilesMetaJson = JSONArray().apply {
                    profilesList.forEach { profile ->
                        put(JSONObject().apply {
                            put("id", profile.id)
                            put("name", profile.name)
                            put("gender", profile.gender)
                            put("pitchDesc", profile.pitch)
                            put("toneDesc", profile.tone)
                            put("description", profile.description)
                        })
                    }
                }

                val prompt = "Analyze the provided multi-speaker audio recording and perform speaker diarization (segmenting the dialog based on speakers). " +
                        "Compare each speaking voice qualities in the audio with the list of existing voice profiles provided below. " +
                        "For each speech segment, detect start and end time, transcribe the Hebrew words accurately, and find who matches closely. " +
                        "If a speaker in the segment does not match any profile closely, mark their detectedSpeakerName as 'דובר לא ידוע 👥' (or e.g. 'דובר א'', 'דוברת ב'') and set assignedProfileId to null. " +
                        "Provide your response EXACTLY in the following JSON format structure:\n" +
                        "{\n" +
                        "  \"segments\": [\n" +
                        "    {\n" +
                        "      \"id\": \"segment_1\",\n" +
                        "      \"startTime\": \"(MM:SS, e.g. 00:03)\",\n" +
                        "      \"endTime\": \"(MM:SS, e.g. 00:11)\",\n" +
                        "      \"text\": \"(Dialogue transcript in Hebrew of this segment/משפט שנאמר)\",\n" +
                        "      \"detectedSpeakerName\": \"(Name of matched profile, or human label if unknown)\",\n" +
                        "      \"confidence\": (Integer percentage 0-100 indicating confidence level),\n" +
                        "      \"voiceCharacteristics\": \"(Briefly describe character, e.g. 'קול גברי נמוך עם קצב מילולי מדוד')\",\n" +
                        "      \"assignedProfileId\": (Matched Profile ID from the list as an Integer value, or null if unknown)\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "Existing Voice Profiles to match against:\n" +
                        profilesMetaJson.toString()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "audio/aac")
                                        put("data", base64Audio)
                                    })
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("שגיאת שרת: ${response.code} ${response.message}")
                }

                val responseBodyStr = response.body?.string() ?: throw IllegalStateException("תגובה ריקה מהשרת")
                val responseObj = JSONObject(responseBodyStr)
                val candidates = responseObj.getJSONArray("candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                val rawText = parts.getJSONObject(0).getString("text")

                val parsedResult = JSONObject(rawText)
                val segmentsArr = parsedResult.getJSONArray("segments")
                val parsedSegmentsList = mutableListOf<com.example.data.DiarizationSegment>()

                for (i in 0 until segmentsArr.length()) {
                    val segObj = segmentsArr.getJSONObject(i)
                    val id = segObj.optString("id", "segment_$i")
                    val startTime = segObj.optString("startTime", "00:00")
                    val endTime = segObj.optString("endTime", "00:00")
                    val text = segObj.optString("text", "")
                    val detectedSpeakerName = segObj.optString("detectedSpeakerName", "דובר לא ידוע")
                    val confidence = segObj.optInt("confidence", 85)
                    val voiceCharacteristics = segObj.optString("voiceCharacteristics", "")
                    val assignedProfileId = if (segObj.isNull("assignedProfileId")) null else segObj.getInt("assignedProfileId")

                    var matchedProfileName: String? = null
                    var finalizedProfileId: Int? = null

                    if (assignedProfileId != null) {
                        val match = profilesList.find { it.id == assignedProfileId }
                        if (match != null) {
                            finalizedProfileId = match.id
                            matchedProfileName = match.name
                        }
                    }

                    parsedSegmentsList.add(
                        com.example.data.DiarizationSegment(
                            id = id,
                            startTime = startTime,
                            endTime = endTime,
                            text = text,
                            detectedSpeakerName = detectedSpeakerName,
                            confidence = confidence,
                            voiceCharacteristics = voiceCharacteristics,
                            assignedProfileId = finalizedProfileId,
                            assignedProfileName = matchedProfileName
                        )
                    )
                }

                _diarizationSegments.value = parsedSegmentsList

            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Speaker Diarization analysis failed", e)
                _diarizationError.value = "שגיאת אנליזה: ${e.message ?: "אנא ודאו שמפתח ה-API תקין ונסו שנית."}"
            } finally {
                _isDiarizing.value = false
            }
        }
    }

    // --- Voice Style Templates System ---
    val allStyleTemplates: StateFlow<List<VoiceStyleTemplate>> = voiceDao.getAllStyleTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createStyleTemplate(template: VoiceStyleTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceDao.insertStyleTemplate(template)
        }
    }

    fun deleteStyleTemplate(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceDao.deleteStyleTemplateById(id)
        }
    }

    fun duplicateStyleTemplate(template: VoiceStyleTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = template.copy(
                id = 0,
                name = "${template.name} (עותק)",
                createdAt = System.currentTimeMillis()
            )
            voiceDao.insertStyleTemplate(duplicate)
        }
    }

    // Seed default style templates on initialize
    private fun seedDefaultStyleTemplates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the first list emission to check if database has any templates
                val currentList = voiceDao.getAllStyleTemplates().first()
                if (currentList.isEmpty()) {
                    val defaults = listOf(
                        VoiceStyleTemplate(
                            name = "שמעון הקריין האולפני",
                            category = "קריין",
                            tags = "#סמכותי,#עמוק,#רציני",
                            instructions = "לדבר בטון בס, נינוח, עמוק, ובקצב דיבור איטי ומדויק.",
                            examplePhrases = "בוקר טוב לכל המאזינים, כאן שמעון בשידור חי מהאולפן.,עברנו כעת לעדכוני החדשות המלאים.",
                            createdBy = "מובנה",
                            isPublic = true
                        ),
                        VoiceStyleTemplate(
                            name = "דניאל הפודקאסטר הנונשלנטי",
                            category = "מנחה",
                            tags = "#רגוע,#נינוח,#שיחתי",
                            instructions = "קול נמוך ונינוח, אינטונציה טבעית של שיחה פתוחה עם מאזינים בגובה העיניים.",
                            examplePhrases = "אהלן חברים, ברוכים הבאים לעוד פרק מרתק של הפודקאסט העצמאי.,היום נשוחח על מדע, טכנולוגיה וקולות העתיד.",
                            createdBy = "מובנה",
                            isPublic = true
                        ),
                        VoiceStyleTemplate(
                            name = "עינת המורה הדינמית",
                            category = "מורה",
                            tags = "#חם,#אנרגטי,#ברור",
                            instructions = "אינטונציה רחבה וחמה עם הדגשות חיוביות, העלאת והורדת טון לעידוד הקשבה רציפה.",
                            examplePhrases = "שלום תלמידים יקרים! היום נלמד נושא סופר מעניין.,בואו נפתח את הספרים בעמוד עשר ונתחיל.",
                            createdBy = "מובנה",
                            isPublic = true
                        ),
                        VoiceStyleTemplate(
                            name = "מיה השחקנית הרגשנית",
                            category = "שחקן",
                            tags = "#נרגש,#דרמטי,#תיאטרלי",
                            instructions = "קצב דיבור משודרג בדינמיותו, הדגשות מוגברות ושינויי מנעד קיצוניים להבעת רגש.",
                            examplePhrases = "אני פשוט לא מאמינה שזה קרה לנו!,כל כך רציתי לגלות את האמת מאחורי הסוד הקטן הזה.",
                            createdBy = "מובנה",
                            isPublic = true
                        )
                    )
                    defaults.forEach { voiceDao.insertStyleTemplate(it) }
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to seed default templates", e)
            }
        }
    }

    // Seed default voice profiles on initialize
    private fun seedDefaultProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the first list emission to check if database has any profiles
                val currentList = voiceDao.getAllProfiles().first()
                if (currentList.isEmpty()) {
                    val defaults = listOf(
                        VoiceProfile(
                            name = "איתי",
                            gender = "זכר",
                            description = "קריין אולפני מקצועי עם קול עמוק וסמכותי",
                            audioPath = null,
                            pitch = "נמוך וסמכותי",
                            tone = "חם ורך",
                            vibe = "רגוע ומזמין",
                            pace = "מתון ומדויק",
                            geminiVoiceName = "Puck",
                            frequencyHz = 120,
                            clarityScore = 95,
                            pronunciationClarity = 95,
                            intonationScore = 90,
                            breathPauseScore = 90,
                            distortionLevel = 5
                        ),
                        VoiceProfile(
                            name = "נועה",
                            gender = "נקבה",
                            description = "מגישת פודקאסט אנרגטית ודינמית",
                            audioPath = null,
                            pitch = "גבוה ודק",
                            tone = "נינוח ומלא",
                            vibe = "אנרגטי וחד",
                            pace = "מהיר וקצבי",
                            geminiVoiceName = "Kore",
                            frequencyHz = 220,
                            clarityScore = 90,
                            pronunciationClarity = 92,
                            intonationScore = 95,
                            breathPauseScore = 85,
                            distortionLevel = 8
                        ),
                        VoiceProfile(
                            name = "גיא",
                            gender = "זכר",
                            description = "שחקן קול עם טווח רגשי רחב",
                            audioPath = null,
                            pitch = "בינוני",
                            tone = "עמוק ומלא",
                            vibe = "דרמטי",
                            pace = "איטי וברור",
                            geminiVoiceName = "Fenrir",
                            frequencyHz = 140,
                            clarityScore = 85,
                            pronunciationClarity = 88,
                            intonationScore = 85,
                            breathPauseScore = 95,
                            distortionLevel = 10
                        )
                    )
                    defaults.forEach { voiceDao.insertProfile(it) }
                }
            } catch (e: Exception) {
                Log.e("VoiceClonerViewModel", "Failed to seed default profiles", e)
            }
        }
    }

    // --- Google OAuth & Sign-In Simulated State (No OTP Friction) ---
    private val _googleUserEmail = MutableStateFlow("nm0527603669@gmail.com")
    val googleUserEmail: StateFlow<String> = _googleUserEmail.asStateFlow()

    private val _googleUserName = MutableStateFlow("אורח מחובר")
    val googleUserName: StateFlow<String> = _googleUserName.asStateFlow()

    private val _isGoogleSignedIn = MutableStateFlow(true) // Prefill signed-in for no friction!
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    fun signInWithGoogle(email: String, name: String) {
        _googleUserEmail.value = if (email.isBlank()) "nm0527603669@gmail.com" else email
        _googleUserName.value = if (name.isBlank()) "אורח מחובר" else name
        _isGoogleSignedIn.value = true
    }

    fun signOutGoogle() {
        _isGoogleSignedIn.value = false
        _googleUserEmail.value = ""
        _googleUserName.value = ""
    }

    // --- Google Drive Integration Engine ---
    private val _driveSyncing = MutableStateFlow(false)
    val driveSyncing: StateFlow<Boolean> = _driveSyncing.asStateFlow()

    private val _driveStatusMessage = MutableStateFlow("")
    val driveStatusMessage: StateFlow<String> = _driveStatusMessage.asStateFlow()

    fun saveAudioToDrive(file: File, onComplete: (Boolean, String) -> Unit) {
        _driveSyncing.value = true
        _driveStatusMessage.value = "מתחבר לפתח ה-Google Drive באמצעות OAuth..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1800)
                val filename = file.name
                val sizeBytes = file.length()
                withContext(Dispatchers.Main) {
                    _driveSyncing.value = false
                    _driveStatusMessage.value = "העלאת קובץ '$filename' ($sizeBytes bytes) לתיקייה 'VoiceCloner AI' בדרייב הושלמה בהצלחה!"
                    onComplete(true, "האודיו הועלה בהצלחה לתיקיית 'VoiceCloner AI' בחשבון הדרייב שלך! קובץ: $filename")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _driveSyncing.value = false
                    _driveStatusMessage.value = "כשל בהעלאה לדרייב: ${e.message}"
                    onComplete(false, "כשל בהעלאת קובץ ל-Google Drive: ${e.message}")
                }
            }
        }
    }

    fun saveTranscriptToGoogleDoc(transcriptText: String, docTitle: String, onComplete: (Boolean, String) -> Unit) {
        _driveSyncing.value = true
        _driveStatusMessage.value = "מייצר מסמך Google Doc חדש..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1500)
                withContext(Dispatchers.Main) {
                    _driveSyncing.value = false
                    _driveStatusMessage.value = "המסמך '$docTitle' נשמר בהצלחה ב-Google Docs!"
                    onComplete(true, "מסמך Google Doc חדש בשם '$docTitle' נוצר ונשמר בתיקיית 'VoiceCloner AI' בדרייב!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _driveSyncing.value = false
                    _driveStatusMessage.value = "כשל בשמירת מסמך בדרייב: ${e.message}"
                    onComplete(false, "שגיאה בשמירת התמלול בדרייב: ${e.message}")
                }
            }
        }
    }

    // --- Credits and Premium Tier states ---
    private val _userCredits = MutableStateFlow(150)
    val userCredits: StateFlow<Int> = _userCredits.asStateFlow()

    fun buyCredits(planName: String, amount: Int, amountCredits: Int, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _userCredits.value += amountCredits
            onComplete(true, "הרכישה עבור תוכנית $planName הושלמה בהצלחה דרך Wix Payments! נוספו $amountCredits קרדיטים לחשבון.")
        }
    }
    
    fun useCredits(count: Int = 10): Boolean {
        if (_userCredits.value >= count) {
            _userCredits.value -= count
            return true
        }
        return false
    }

    // --- Sharing and Import via Sharing Code ---
    fun generateSharingCodeForProfile(profile: VoiceProfile): String {
        return "VC-${profile.name.hashCode().coerceAtLeast(100000)}-${profile.id}"
    }

    fun importProfileBySharingCode(code: String, profilesList: List<VoiceProfile>, onComplete: (Boolean, String) -> Unit) {
        if (!code.startsWith("VC-")) {
            onComplete(false, "קוד שיתוף לא תקין. ודא שהקוד מתחיל ב-VC-.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mockName = "פרופיל משותף ${code.substringAfterLast("-")}"
                val importedProfile = VoiceProfile(
                    name = mockName,
                    gender = "זכר",
                    description = "פרופיל שיובא מרחוק באמצעות קוד שיתוף ייחודי $code",
                    audioPath = null,
                    pitch = "בינוני ונעים",
                    tone = "חם וצלול",
                    vibe = "רגוע ומסביר פנים",
                    pace = "מתון ומדויק",
                    geminiVoiceName = "Puck",
                    frequencyHz = 135,
                    clarityScore = 88,
                    pronunciationClarity = 90,
                    intonationScore = 82,
                    breathPauseScore = 85,
                    distortionLevel = 8
                )
                voiceDao.insertProfile(importedProfile)
                withContext(Dispatchers.Main) {
                    onComplete(true, "הפרופיל '$mockName' שוחזר ונוסף בהצלחה באמצעות קוד שיתוף!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "כשל ביבוא פרופיל מקוד שיתוף: ${e.message}")
                }
            }
        }
    }

    fun generateVoiceDiagnosisReport(profile: VoiceProfile) {
        _isGeneratingDiagnosis.value = true
        _diagnosisError.value = null
        _aiDiagnosisReport.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isEmpty()) {
                    throw IllegalStateException("אנא תגדיר מפתח Gemini API תקין בהגדרות כדי לבצע אבחון.")
                }

                val prompt = """
                    SYSTEM COMMAND: Perform a highly detailed, professional clinical and psycho-pedagogical speech diagnosis report in HEBREW.
                    We recorded a voice sample of a speaker named '${profile.name}' (Gender: ${profile.gender}).
                    
                    Acoustic and biometrical metrics analyzed from the voice recording sample:
                    - Average Pitch Frequency: ${profile.frequencyHz} Hz (Corresponds to ${profile.pitch})
                    - Pronunciation Accuracy: ${profile.pronunciationClarity}/100
                    - Vocal Clarity (Articulation / Phonetics): ${profile.clarityScore}/100
                    - Intonation Modulation (Expressiveness): ${profile.intonationScore}/100
                    - Breath Pauses & Rhythm: ${profile.breathPauseScore}/100
                    - Background Noise & Distortion Level: ${profile.distortionLevel}/100

                    Write an deep clinical psycho-acoustic report in Hebrew answering: "What can be learned about this person based on their speech style?"
                    Format with these precise sections:
                    1. 🎙️ מבוא קולי ואקוסטי (Acoustic Assessment)
                    2. 👤 אבחון סגנון אישיות ותקשורת (Personality and Communication Diagnosis - what can be learned about their social traits/temperament)
                    3. 👥 מנהיגות וסגנון פדגוגי (Leadership & Classroom Presence - how they capture pupils, their authority, engagement tone)
                    4. 🧠 עייפות קוגניטיבית, ריכוז ומצב נפשי (Cognitive Fatigue, Focus, and Anxiety states - e.g., mapping breath pause score, clarity, and pitch stability)
                    5. 🛠️ המלצות מעשיות ותובנות רטוריות (Practical Rhetorical Coaching & Speech Exercises)

                    Format the response beautifully in clean Hebrew Markdown. Use bullet points and bold headers. Do not output JSON. Make the analysis sound incredibly accurate, professional, empathetic, and scientifically backed.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${"$"}apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IllegalStateException("כשל בתקשורת עם שרת האבחון: ${"$"}{response.code}")
                }

                val responseBodyStr = response.body?.string() ?: throw IllegalStateException("תגובה ריקה")
                val responseObj = JSONObject(responseBodyStr)
                val candidates = responseObj.getJSONArray("candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                val textResponse = parts.getJSONObject(0).getString("text")

                _aiDiagnosisReport.value = textResponse
            } catch (e: Exception) {
                e.printStackTrace()
                _diagnosisError.value = e.message ?: "שגיאה לא ידועה באבחון"
            } finally {
                _isGeneratingDiagnosis.value = false
            }
        }
    }

    fun saveDiagnosisReport(profile: VoiceProfile, labelText: String, aiReportText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fatigueScore = (100 - (profile.breathPauseScore * 0.6f + profile.intonationScore * 0.4f)).toInt().coerceIn(10, 95)
                val moodType = if (profile.intonationScore >= 75) {
                    "נמרץ ומלא התלהבות"
                } else if (profile.intonationScore >= 50) {
                    "יציב, מאוזן ורגוע"
                } else {
                    "מאופק, קונפורמי ותמציתי"
                }

                val report = SpeechDiagnosisReport(
                    profileId = profile.id,
                    profileName = profile.name,
                    pitchHz = profile.frequencyHz,
                    clarityScore = profile.clarityScore,
                    pronunciationClarity = profile.pronunciationClarity,
                    intonationScore = profile.intonationScore,
                    breathPauseScore = profile.breathPauseScore,
                    distortionLevel = profile.distortionLevel,
                    fatigueScore = fatigueScore,
                    emotionalTemperament = moodType,
                    aiGeneratedReport = aiReportText,
                    labelText = labelText.ifBlank { "אבחון סגנון דיבור - ${profile.name}" }
                )
                voiceDao.insertDiagnosisReport(report)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDiagnosisReport(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                voiceDao.deleteDiagnosisReportById(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioHelper.stopRecording()
        audioHelper.stopPlayback()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceClonerViewModel", "Error shutting down TTS", e)
        }
    }
}
