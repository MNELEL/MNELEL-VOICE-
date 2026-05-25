package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
import com.example.utils.AudioHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ClonerStep { RECORDING, ANALYSIS, SAVING }

class VoiceClonerViewModel(private val db: AppDatabase, private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceClonerUiState())
    val uiState: StateFlow<VoiceClonerUiState> = _uiState
    val savedProfiles = db.voiceDao().getAllProfiles()

    private val audioHelper = AudioHelper(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("voice_cloner_prefs", Context.MODE_PRIVATE)

    private var timerJob: Job? = null
    private var speakingJob: Job? = null

    init {
        var savedKey = prefs.getString("gemini_api_key", "") ?: ""
        if (savedKey.isBlank()) {
            val buildConfigKey = try {
                BuildConfig.GEMINI_API_KEY.orEmpty()
            } catch (e: Throwable) {
                ""
            }
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                savedKey = buildConfigKey
            }
        }
        _uiState.update { it.copy(apiKey = savedKey) }
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _uiState.update { it.copy(apiKey = key) }
    }

    private fun showToast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

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
            _uiState.update { it.copy(isRecording = true, recordingDuration = 0, errorMessage = null) }
            timerJob?.cancel()

            audioHelper.startRecording(
                onSuccess = { path ->
                    if (_uiState.value.isRecording) {
                        _uiState.update { it.copy(recordingPath = path) }
                        timerJob = viewModelScope.launch {
                            while (true) {
                                delay(1000)
                                _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
                            }
                        }
                    } else {
                        audioHelper.stopRecording()
                    }
                },
                onError = { e ->
                    if (_uiState.value.isRecording) {
                        timerJob?.cancel()
                        _uiState.update { it.copy(isRecording = false, errorMessage = "שגיאה בהקלטה: ${e.message}") }
                    }
                }
            )
        } else {
            val path = audioHelper.stopRecording()
            timerJob?.cancel()
            _uiState.update { it.copy(isRecording = false, recordingPath = path) }
        }
    }

    private fun resetRecordingState() {
        timerJob?.cancel()
        audioHelper.stopRecording()
        _uiState.update { it.copy(isRecording = false, recordingDuration = 0, errorMessage = null) }
    }

    fun saveProfile(name: String) {
        val path = _uiState.value.recordingPath ?: ""
        val accent = _uiState.value.selectedAccent
        val pitch = _uiState.value.pitchLevel
        val speed = _uiState.value.speedLevel
        val depth = _uiState.value.emotionalDepth
        val phonetic = _uiState.value.phoneticAccuracy
        val apiKey = _uiState.value.apiKey

        _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val analysisReport = if (apiKey.isNotBlank() && path.isNotBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        withContext(Dispatchers.IO) {
                            analyzeAudioWithGemini(apiKey, file, accent, pitch, speed, depth, phonetic)
                        }
                    } else {
                        generateLocalAnalysis(accent, pitch, speed, depth, phonetic)
                    }
                } else {
                    generateLocalAnalysis(accent, pitch, speed, depth, phonetic)
                }

                _uiState.update { it.copy(analysisResult = analysisReport, isAnalyzing = false) }

                withContext(Dispatchers.IO) {
                    val displayName = name.ifBlank { "פרופיל קולי שנוצר" }
                    db.voiceDao().insert(
                        VoiceProfile(
                            name = displayName,
                            analysisData = analysisReport,
                            audioPath = path
                        )
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                val fallbackReport = generateLocalAnalysis(accent, pitch, speed, depth, phonetic) +
                        "\n\n*(שים לב: ניתוח ה-AI נכשל עקב שגיאת תקשורת: ${e.message}. מבוצע ניתוח מקומי חלופי)*"

                _uiState.update {
                    it.copy(
                        analysisResult = fallbackReport,
                        isAnalyzing = false,
                        errorMessage = "בעיית חיבור: ${e.message}"
                    )
                }

                withContext(Dispatchers.IO) {
                    try {
                        val displayName = name.ifBlank { "פרופיל קולי שנוצר" }
                        db.voiceDao().insert(
                            VoiceProfile(
                                name = displayName,
                                analysisData = fallbackReport,
                                audioPath = path
                            )
                        )
                    } catch (dbEx: Throwable) {
                        dbEx.printStackTrace()
                    }
                }
            }
        }
    }

    fun deleteProfile(profile: VoiceProfile) {
        if (_uiState.value.playingProfileId == profile.id) {
            stopProfileRecordingPlayback()
        }
        if (_uiState.value.speakingProfileName == profile.name) {
            stopSpeakingSimulation()
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.voiceDao().delete(profile)
                if (profile.audioPath.isNotBlank()) {
                    val file = File(profile.audioPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                showToast("נכשל במחיקת הפרופיל: ${e.message}")
            }
        }
    }

    fun clearAllProfiles() {
        stopProfileRecordingPlayback()
        stopSpeakingSimulation()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.voiceDao().deleteAll()
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("voice_record_") || file.name.startsWith("temp_tts_")) {
                        file.delete()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                showToast("נכשל בניקוי הפרופילים: ${e.message}")
            }
        }
    }

    fun startSpeakingSimulation(text: String, profileName: String) {
        val apiKey = _uiState.value.apiKey
        speakingJob?.cancel()
        audioHelper.stopPlayback()
        _uiState.update { it.copy(speakingText = text, speakingProfileName = profileName) }

        if (apiKey.isNotBlank() && text.isNotBlank()) {
            speakingJob = viewModelScope.launch {
                try {
                    val tempAudioFile = withContext(Dispatchers.IO) {
                        synthesizeSpeechWithGemini(apiKey, text)
                    }
                    audioHelper.playAudio(
                        path = tempAudioFile.absolutePath,
                        onComplete = {
                            _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
                        },
                        onError = { e ->
                            _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
                            showToast("שגיאה בהקראת ה-TTS: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("שגיאה בחיבור ל-Gemini TTS, מפעיל הדמיה מקומית...")
                    delay(5000)
                    _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
                }
            }
        } else {
            speakingJob = viewModelScope.launch {
                delay(5000)
                _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
            }
        }
    }

    fun stopSpeakingSimulation() {
        speakingJob?.cancel()
        audioHelper.stopPlayback()
        _uiState.update { it.copy(speakingText = null, speakingProfileName = null) }
    }

    fun playProfileRecording(profile: VoiceProfile) {
        if (profile.audioPath.isBlank()) {
            showToast("אין קובץ שמע מוקלט לפרופיל זה שמוצג.")
            return
        }

        _uiState.update { it.copy(isPlaying = true, playingProfileId = profile.id) }
        audioHelper.playAudio(
            path = profile.audioPath,
            onComplete = {
                _uiState.update { it.copy(isPlaying = false, playingProfileId = null) }
            },
            onError = { e ->
                _uiState.update { it.copy(isPlaying = false, playingProfileId = null) }
                showToast("שגיאה בהשמעת הקובץ: ${e.message}")
            }
        )
    }

    fun stopProfileRecordingPlayback() {
        audioHelper.stopPlayback()
        _uiState.update { it.copy(isPlaying = false, playingProfileId = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        speakingJob?.cancel()
        audioHelper.stopRecording()
        audioHelper.stopPlayback()
    }

    private fun generateLocalAnalysis(accent: String, pitch: Float, speed: Float, depth: Float, phonetic: Boolean): String {
        val pitchDesc = when {
            pitch < 0.35f -> "טון נמוך ועמוק (בס)"
            pitch > 0.65f -> "טון גבוה וצלול (סופרן/טנור)"
            else -> "טון בינוני מאוזן"
        }
        val speedDesc = when {
            speed < 0.35f -> "קצב דיבור מתון ורגוע"
            speed > 0.65f -> "קצב דיבור מהיר ונמרץ"
            else -> "קצב דיבור ממוצע וטבעי"
        }
        val depthDesc = when {
            depth < 0.35f -> "נוכחות רגשית קלה וממוקדת"
            depth > 0.65f -> "עומק רגשי דרמטי ומלא הבעה"
            else -> "תוצר רגשי חם ויומיומי"
        }

        return """
            דוח ניתוח קולי (מקומי):
            • סגנון ומבטא נבחר: $accent
            • מאפיינים פיזיקליים: $pitchDesc, $speedDesc.
            • דינמיקה קולית: $depthDesc.
            • התאמה פונטית לעברית: ${if (phonetic) "מופעלת (רמת דיוק גבוהה)" else "כבויה"}.
            
            * שים לב: לא הוגדר מפתח API של Gemini. לניתוח בינה מלאכותית מקצועי המבוסס על קובץ הקול האמיתי שלך, אנא הזן מפתח API בהגדרות שבראש המסך.
        """.trimIndent()
    }

    private suspend fun analyzeAudioWithGemini(
        apiKey: String,
        audioFile: File,
        accent: String,
        pitch: Float,
        speed: Float,
        depth: Float,
        phonetic: Boolean
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val fileBytes = audioFile.readBytes()
        val audioBase64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val prompt = """
            You are an expert voice coach and voice cloner AI analyzer.
            The user has recorded a short audio segment to clone their voice.
            Please write a highly detailed, professional, encouraging, and informative Voice Analysis Report (דוח ניתוח קולי) in Hebrew.
            Analyze:
            1. Accent properties: The user selected '$accent'. Verify or analyze how it blends.
            2. Acoustic features: Pitch setting is ${(pitch * 100).toInt()}%, Speed is ${(speed * 100).toInt()}%, Emotional expression is ${(depth * 100).toInt()}%. Describe how these sliders impact the sound.
            3. Hebrew phonetics: Phonetic compliance is $phonetic. Mention spelling/pronunciation details.
            4. Practical tips for improving voice cloning results.
            
            Format the response in a very elegant, clean, bulleted Hebrew structure. Write natural Hebrew, right-to-left friendly.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/3gpp")
                                put("data", audioBase64)
                            })
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API Error ${response.code}: ${response.message}")
        }

        val responseStr = response.body?.string() ?: throw Exception("Empty response from Gemini")
        val resJson = JSONObject(responseStr)
        val candidates = resJson.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        parts.getJSONObject(0).getString("text")
    }

    private suspend fun synthesizeSpeechWithGemini(apiKey: String, text: String): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
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
                            put("voiceName", "Kore")
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API Error ${response.code}: ${response.message}")
        }

        val responseStr = response.body?.string() ?: throw Exception("Empty response from Gemini TTS")
        val resJson = JSONObject(responseStr)
        val candidates = resJson.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")

        var base64Data: String? = null
        var mimeType = "audio/mp3"

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inlineData")) {
                val inlineData = part.getJSONObject("inlineData")
                base64Data = inlineData.getString("data")
                mimeType = inlineData.optString("mimeType", mimeType)
                break
            }
        }

        if (base64Data == null) {
            throw Exception("No inline audio data found in Gemini response")
        }

        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val fileExtension = if (mimeType.contains("wav")) "wav" else "mp3"
        val tempFile = File(context.cacheDir, "temp_tts_${System.currentTimeMillis()}.$fileExtension")
        tempFile.writeBytes(decodedBytes)
        tempFile
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
    val speakingProfileName: String? = null,

    val recordingPath: String? = null,
    val isPlaying: Boolean = false,
    val playingProfileId: Long? = null,
    val apiKey: String = "",
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val errorMessage: String? = null
)
