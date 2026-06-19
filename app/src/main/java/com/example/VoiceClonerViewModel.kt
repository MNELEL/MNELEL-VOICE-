package com.example

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
import com.example.data.VoiceGenerationResult
import com.example.utils.AudioHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _playerTrackTitle = MutableStateFlow("קובץ שמע אינו פעיל 📭")
    val playerTrackTitle: StateFlow<String> = _playerTrackTitle.asStateFlow()

    private val _isPlayerMuted = MutableStateFlow(false)
    val isPlayerMuted: StateFlow<Boolean> = _isPlayerMuted.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioHelper.setPlaybackSpeed(speed)
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
