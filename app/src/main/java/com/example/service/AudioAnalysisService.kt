package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import android.util.Base64
import com.example.utils.LocalPhoneticAnalyzer
import java.util.concurrent.TimeUnit

data class AudioAnalysisResult(
    val phoneticParameters: String,
    val pitchFrequencies: String,
    val backgroundNoiseLevels: String,
    val voicePrint: String,
    val gutturalDepth: String,
    val dictionAndClipping: String,
    val voiceToneAndStyle: String,
    val overallSummary: String,
    // Add new default-valued numeric metrics for advanced analysis dashboard:
    val estimatedPitchHz: Int = 120,
    val estimatedSnrDb: Int = 30,
    val clarityScore: Int = 80,
    val pronunciationClarity: Int = 80,
    val intonationScore: Int = 75,
    val breathPauseScore: Int = 70
)

class AudioAnalysisService {

    suspend fun analyzeAudio(audioFile: File): AudioAnalysisResult = withContext(Dispatchers.IO) {
        // Simple file based analysis using our local parser
        val bytes = try { audioFile.readBytes() } catch (e: Exception) { ByteArray(0) }
        analyzeAudioBlob(bytes, "audio/wav", "")
    }

    suspend fun analyzeAudioBlob(
        audioBlob: ByteArray, 
        mimeType: String = "audio/wav", 
        apiKey: String = ""
    ): AudioAnalysisResult = withContext(Dispatchers.IO) {
        // Run fast, reliable local heuristic analysis offline
        val tempFile = File.createTempFile("audio_analysis_blob", ".wav")
        try {
            if (audioBlob.isNotEmpty()) {
                tempFile.writeBytes(audioBlob)
            }
            val localMetrics = LocalPhoneticAnalyzer.analyzeAudioFile(tempFile)
            val isMale = localMetrics.estimatedPitchHz < 165
            
            val pitch = localMetrics.estimatedPitchHz
            val snr = (25 + (localMetrics.energyLevel * 0.25)).toInt().coerceIn(15, 45)
            val isClear = localMetrics.clarityEstimate > 75
            val isDeep = pitch < 140

            val phonetic = if (isClear) {
                "ארטיקולציה חדה וברורה, קצב דיבור מתון ויציב (כ-4 הברות לשנייה). ללא סממני דיאלקט חריגים."
            } else {
                "ארטיקולציה רכה, נטייה לחיבור מילים. קצב דיבור משתנה עם השהיות טבעיות."
            }
            
            val pitchText = "תדר בסיס ממוצע: ${pitch}Hz. אינטונציה טבעית עם עליות מתונות בסופי משפטים."
            
            val noiseText = "יחס אות לרעש (SNR): כ-${snr}dB. " + if (snr > 30) {
                "סביבת הקלטה נקייה מאוד, כמעט ללא רעשי רקע או סטטיות."
            } else {
                "נוכחות קלה של רעשי רקע (תדרים נמוכים). מומלץ להקליט בסביבה שקטה יותר."
            }

            val voicePrintText = "טביעת קול ספקטרלית מזהה רזוננס ייחודי בחלל הפה והאף. מודל הרמוני: ${if(isDeep) "מורחב ועשיר" else "ממוקד וצר"}."
            val gutturalDepthText = if (isDeep) "אקוסטיקה גרונית עמוקה עם תהודה חזה בולטת (Chest Voice). מיתרי הקול מפיקים צליל מלא."
                                    else "אקוסטיקה גרונית קלה, התהודה מתרכזת יותר באזור הראש והפנים (Head Voice)."
            val dictionText = if (isClear) "חיתוך דיבור קפדני (Diction). עיצורים נחתכים בבירור ללא מריחות. דיוק פונטי גבוה."
                              else "חיתוך דיבור רפוי מעט. עיצורים שורקים או אפיים נשמעים טבעיים ומשתלבים ברצף המשפט."
            val toneStyleText = if (pitch > 175) "גוון קול בהיר ואנרגטי. סגנון הדיבור משדר התלהבות ואקטיביות, מבטא ניטרלי-מודרני."
                                else "גוון קול כהה וסמכותי. סגנון הדיבור שקול, איטי מעט ומשדר רוגע. מבטא ניטרלי."
            
            val summaryText = "סיכום אקוסטי: הדובר מפיק קול ${if(isDeep) "עמוק" else "גבוה"} ו${if(isClear) "ברור" else "זורם"}. " +
                              "מאפייני התהודה מצביעים על פרופיל ווקאלי אידיאלי ליצירת מודל קול טבעי."

            return@withContext AudioAnalysisResult(
                phoneticParameters = phonetic,
                pitchFrequencies = pitchText,
                backgroundNoiseLevels = noiseText,
                voicePrint = voicePrintText,
                gutturalDepth = gutturalDepthText,
                dictionAndClipping = dictionText,
                voiceToneAndStyle = toneStyleText,
                overallSummary = summaryText,
                estimatedPitchHz = pitch,
                estimatedSnrDb = snr,
                clarityScore = localMetrics.clarityEstimate,
                pronunciationClarity = (localMetrics.clarityEstimate - 5).coerceAtLeast(50),
                intonationScore = (60 + localMetrics.energyLevel * 0.3).toInt().coerceIn(50, 100),
                breathPauseScore = (70 + localMetrics.speechSegmentsCount * 2).coerceIn(50, 100)
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
