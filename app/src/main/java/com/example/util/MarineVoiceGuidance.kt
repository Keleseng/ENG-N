package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Türkçe Kadın Sesli Denizci Rota Rehberi ve Anons Motoru (Text-To-Speech).
 *
 * Kadın ses tonuna (Female Voice) ayarlanmış, akıcı ve anlaşılır orijinal anons formatında
 * sesli yönlendirme ve ikazlar verir.
 */
class MarineVoiceGuidance(context: Context) : TextToSpeech.OnInitListener {

  private var tts: TextToSpeech? = null
  private var isInitialized = false
  private var pendingUtterance: String? = null

  init {
    try {
      tts = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      Log.e("MarineVoiceGuidance", "TTS başlatılamadı: ${e.localizedMessage}")
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val trLocale = Locale("tr", "TR")
      val result = tts?.setLanguage(trLocale)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        tts?.setLanguage(Locale.getDefault())
      }

      configureFemaleVoice()

      isInitialized = true
      pendingUtterance?.let { queued ->
        pendingUtterance = null
        speak(queued)
      }
    } else {
      Log.w("MarineVoiceGuidance", "TTS Init Başarısız: $status")
    }
  }

  var pitch: Float = 1.32f
  var speechRate: Float = 1.0f

  /**
   * Cihazdaki Türkçe Kadın Ses Paketini (Female Voice) kesin olarak seçer,
   * erkek ses paketlerini (cfg, mfg, sfg, male) eler ve tizliği ayarlayarak net kadın ses tonunu garanti eder.
   */
  fun configureFemaleVoice() {
    val t = tts ?: return
    try {
      t.setSpeechRate(speechRate)
      t.setPitch(pitch)

      val voices = t.voices
      if (!voices.isNullOrEmpty()) {
        val turkishVoices = voices.filter { it.locale.language == "tr" }

        val maleKeywords = listOf("male", "m00", "erkek", "cfg", "mfg", "sfg", "man", "boy")
        val femaleKeywords = listOf("female", "f00", "kadin", "bayan", "lady", "dfg", "efg", "smc", "tfe", "zeynep", "yelda", "seda")

        val nonMaleVoices = turkishVoices.filter { voice ->
          val nameLower = voice.name.lowercase()
          maleKeywords.none { nameLower.contains(it) }
        }

        val localNonMaleVoices = nonMaleVoices.filter { !it.isNetworkConnectionRequired }
        val localTurkishVoices = turkishVoices.filter { !it.isNetworkConnectionRequired }

        val bestFemaleVoice = localNonMaleVoices.find { voice ->
          val nameLower = voice.name.lowercase()
          femaleKeywords.any { nameLower.contains(it) }
        } ?: localNonMaleVoices.firstOrNull()
          ?: nonMaleVoices.find { voice ->
            val nameLower = voice.name.lowercase()
            femaleKeywords.any { nameLower.contains(it) }
          }
          ?: nonMaleVoices.firstOrNull()
          ?: localTurkishVoices.firstOrNull()
          ?: turkishVoices.firstOrNull()

        bestFemaleVoice?.let {
          t.voice = it
          Log.d("MarineVoiceGuidance", "Atanan Türkçe Kadın Ses Paketi: ${it.name} (offline=${!it.isNetworkConnectionRequired})")
        }
      }
    } catch (e: Exception) {
      Log.w("MarineVoiceGuidance", "Kadın ses konfigürasyonu hatası: ${e.localizedMessage}")
    }
  }

  /**
   * Kullanıcının seçtiği ses tonunu test etmesi için örnek Türkçe kadın sesli denizci anonsu yapar.
   */
  fun testVoice() {
    speak("Metehan Kaptan, Türkçe kadın sesli denizci anonsu test ediliyor. Rota rehberi aktiftir.")
  }

  /**
   * Rota oluşturulduğunda orijinal sesli rota anonsunu yapar.
   * Örnek: "Rota oluşturuldu. Dönüş noktası 1'e gidiş için 245 rotası tavsiye olunur."
   */
  fun speakRouteCreation(
    destinationName: String,
    firstWaypointName: String?,
    recommendedHeadingDegrees: Int
  ) {
    val text = if (!firstWaypointName.isNullOrBlank()) {
      "Rota oluşturuldu. ${cleanName(firstWaypointName)} mevkiine gidiş için $recommendedHeadingDegrees rotası tavsiye olunur."
    } else {
      "Rota oluşturuldu. ${cleanName(destinationName)} varış mevkiine denizden gidiş için $recommendedHeadingDegrees rotası tavsiye olunur."
    }
    speak(text)
  }

  /**
   * Harita üzerinde veya seyir esnasında dönüş noktasına (DN) yaklaşıldığında sesli ikaz verir.
   */
  fun speakApproachingWaypoint(waypointName: String, nextHeadingDegrees: Int) {
    val text = "${cleanName(waypointName)} mevkiine gelindi. Sıradaki deniz bacağı için $nextHeadingDegrees rotası tavsiye olunur."
    speak(text)
  }

  /**
   * Belirli bir dönüş noktasının detaylı sesli anonsunu (ETA dahil) yapar.
   */
  fun speakWaypointDetail(
    waypointName: String,
    legDistanceNm: Double,
    recommendedHeadingDegrees: Int,
    etaTimeStr: String = "",
    nextHeadingDegrees: Int? = null
  ) {
    val distFormatted = String.format(Locale.US, "%.1f", legDistanceNm).replace(".", ",")
    val etaPart = if (etaTimeStr.isNotBlank()) " Tahmini varış saati $etaTimeStr." else ""
    val nextPart = if (nextHeadingDegrees != null) " Dönüş sonrası yeni rota $nextHeadingDegrees derece." else ""
    val text = "${cleanName(waypointName)} deniz mevkii. Mesafe $distFormatted deniz mili, gidiş için $recommendedHeadingDegrees rotası tavsiye olunur.$etaPart$nextPart"
    speak(text)
  }

  /**
   * Haritada karaya tıklandığında sesli ikaz verir.
   */
  fun speakLandClickWarning() {
    speak("Seçilen mevki kara üzerindedir. Gemiler karadan gidemez, lütfen deniz üzerinden bir mevki seçiniz.")
  }

  /**
   * Rotadan 1 mil ve daha fazla sapıldığında 3 dakikada bir sesli ikaz verir.
   */
  fun speakOffRouteWarning() {
    speak("Belirtilen rotanın dışındasınız.")
  }

  fun speak(text: String) {
    if (!isInitialized || tts == null) {
      pendingUtterance = text
      return
    }
    try {
      configureFemaleVoice()
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MarineVoiceGuidance_${System.currentTimeMillis()}")
    } catch (e: Exception) {
      Log.e("MarineVoiceGuidance", "Sesli anons hatası: ${e.localizedMessage}")
    }
  }

  fun stop() {
    try {
      tts?.stop()
    } catch (_: Exception) {}
  }

  fun shutdown() {
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {}
  }

  private fun cleanName(raw: String): String {
    var result = raw.replace(Regex("""\(DN\d+\)"""), "")
    result = result.replace(Regex("""DN(\d+)""")) { match ->
      "dönüş noktası ${match.groupValues[1]}"
    }
    return result.trim()
  }
}
