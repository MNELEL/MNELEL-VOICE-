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

    private var tts: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    init {
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

    fun startRecordVoice() {
        _recordedFile.value = null
        _analysisError.value = null
        _recordingDurationSec.value = 0
        _liveAmplitude.value = 0f
        _isRecordingPaused.value = false
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
        _recordedFile.value = lastRecordedFile
    }

    // Play/Stop recorded or uploaded file before cloning
    private val _isPlayingRecorded = MutableStateFlow(false)
    val isPlayingRecorded: StateFlow<Boolean> = _isPlayingRecorded.asStateFlow()

    fun playRecordedFile() {
        val file = _recordedFile.value
        if (file != null && file.exists()) {
            _isPlayingRecorded.value = true
            audioHelper.playAudio(file) {
                _isPlayingRecorded.value = false
                playbackProgressJob?.cancel()
                _playbackProgress.value = 0f
                _playbackElapsedText.value = "00:00"
                _playbackDurationText.value = "00:00"
            }
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
                _isPlayingProfileId.value = profile.id
                audioHelper.playAudio(file) {
                    _isPlayingProfileId.value = null
                    playbackProgressJob?.cancel()
                    _playbackProgress.value = 0f
                }
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
            _isPlayingResultId.value = result.id
            audioHelper.playAudio(file) {
                _isPlayingResultId.value = null
                playbackProgressJob?.cancel()
                _playbackProgress.value = 0f
            }
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
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("אנא הגדר מפתח Gemini API תקין בהגדרות או בקובץ .env")
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
                    _analysisError.value = e.message ?: "חיבור רשת נכשל"
                }
            }
        }
    }

    fun synthesizeText(text: String, profile: VoiceProfile) {
        if (text.isBlank()) {
            _synthesizeError.value = "אנא הזן טקסט לייצור קול"
            return
        }

        _isSynthesizing.value = true
        _synthesizeError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("אנא הגדר מפתח Gemini API תקין בהגדרות או בקובץ .env")
                }

                // Construct request for TTS modality
                val promptText = "Say the following text in Hebrew: $text"
                
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
                    _synthesizeError.value = e.message ?: "שגיאה בחיבור אל שרת ג׳מיני קול"
                }
            }
        }
    }

    fun synthesizeTextLocal(text: String, profile: VoiceProfile) {
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
                val pitchMultiplier: Float = when {
                    profile.frequencyHz <= 100 -> 0.70f
                    profile.frequencyHz <= 125 -> 0.82f
                    profile.frequencyHz >= 210 -> 1.35f
                    profile.frequencyHz >= 180 -> 1.20f
                    else -> 1.0f
                }
                currentTts.setPitch(pitchMultiplier)

                // Map pace description to speech rate multiplier
                val rateMultiplier: Float = when {
                    profile.pace.contains("מהיר") || profile.pace.contains("מהירה") || profile.pace.lowercase().contains("fast") -> 1.25f
                    profile.pace.contains("איטי") || profile.pace.contains("איטית") || profile.pace.lowercase().contains("slow") -> 0.78f
                    else -> 1.00f
                }
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
