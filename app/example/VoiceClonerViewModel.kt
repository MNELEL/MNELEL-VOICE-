package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceProfile
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
import java.util.concurrent.TimeUnit

class VoiceClonerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val voiceDao = database.voiceDao()
    private val audioHelper = AudioHelper(application)

    val allProfiles: StateFlow<List<VoiceProfile>> = voiceDao.getAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedFile = MutableStateFlow<File?>(null)
    val recordedFile: StateFlow<File?> = _recordedFile.asStateFlow()

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
        lastRecordedFile = audioHelper.startRecording()
        if (lastRecordedFile != null) {
            _isRecording.value = true
        } else {
            _analysisError.value = "שגיאה באתחול המיקרופון"
        }
    }

    fun stopRecordVoice() {
        audioHelper.stopRecording()
        _isRecording.value = false
        _recordedFile.value = lastRecordedFile
    }

    fun playProfileSample(profile: VoiceProfile) {
        if (profile.audioPath != null) {
            val file = File(profile.audioPath)
            if (file.exists()) {
                _isPlayingProfileId.value = profile.id
                audioHelper.playAudio(file) {
                    _isPlayingProfileId.value = null
                }
            } else {
                _analysisError.value = "קובץ ההקלטה לא נמצא"
            }
        }
    }

    fun stopProfileSample() {
        audioHelper.stopPlayback()
        _isPlayingProfileId.value = null
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            voiceDao.deleteProfileById(id)
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
                                            "  \"pitch\": \"(גובה קול: נמוך / בינוני / גבוה)\",\n" +
                                            "  \"tone\": \"(גוון קול: חם / מתכתי / צרוד / נקי etc. up to 3 words)\",\n" +
                                            "  \"vibe\": \"(אווירה: רגוע / סמכותי / ידידותי / אינטנסיבי)\",\n" +
                                            "  \"pace\": \"(קצב: איטי / מתון / מהיר)\",\n" +
                                            "  \"geminiVoiceName\": \"(Kore / Puck / Fenrir / Aoede / Charon - Choose the one template voice close to this sample)\"\n" +
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

                val newProfile = VoiceProfile(
                    name = name,
                    gender = gender,
                    description = description,
                    audioPath = file.absolutePath,
                    pitch = pitch,
                    tone = tone,
                    vibe = vibe,
                    pace = pace,
                    geminiVoiceName = geminiVoiceName
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
                    audioHelper.playBase64Audio(foundAudioBase64)
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

    override fun onCleared() {
        super.onCleared()
        audioHelper.stopRecording()
        audioHelper.stopPlayback()
    }
}
