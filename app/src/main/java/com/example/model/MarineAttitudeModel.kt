package com.example.model

import java.util.Locale
import kotlin.math.abs

data class MarineAttitude(
  val compassDegrees: Float = 270f,
  val compassCardinal: String = "B (Batı)",
  val pitchDegrees: Float = -0.8f, // + = Baş, - = Kıç (doğal kıça trim)
  val rollDegrees: Float = 1.4f,  // + = Sancak, - = İskele (doğal sancak meyil)
  val isSensorActive: Boolean = false,
  val pitchLabel: String = "Kıç -0.8°",
  val rollLabel: String = "Sancak +1.4°",
  val stabilityStatus: String = "Dengeli",
  val isHoldActive: Boolean = false,
  val isDampedFilterActive: Boolean = true
) {
  companion object {
    fun calculateCardinal(degrees: Float): String {
      val normalized = ((degrees % 360f) + 360f) % 360f
      return when {
        normalized >= 337.5 || normalized < 22.5 -> "K (Kuzey)"
        normalized >= 22.5 && normalized < 67.5 -> "KD (Kuzeydoğu)"
        normalized >= 67.5 && normalized < 112.5 -> "D (Doğu)"
        normalized >= 112.5 && normalized < 157.5 -> "GD (Güneydoğu)"
        normalized >= 157.5 && normalized < 202.5 -> "G (Güney)"
        normalized >= 202.5 && normalized < 247.5 -> "GB (Güneybatı)"
        normalized >= 247.5 && normalized < 292.5 -> "B (Batı)"
        else -> "KB (Kuzeybatı)"
      }
    }

    fun formatPitch(pitch: Float): String {
      val absVal = abs(pitch)
      return when {
        absVal < 0.1f -> "0.0° (Düz)"
        pitch > 0f -> "Baş +${String.format(Locale.US, "%.1f", absVal)}°"
        else -> "Kıç -${String.format(Locale.US, "%.1f", absVal)}°"
      }
    }

    fun formatRoll(roll: Float): String {
      val absVal = abs(roll)
      return when {
        absVal < 0.1f -> "0.0° (Düz)"
        roll > 0f -> "Sancak +${String.format(Locale.US, "%.1f", absVal)}°"
        else -> "İskele -${String.format(Locale.US, "%.1f", absVal)}°"
      }
    }

    fun determineStability(roll: Float, pitch: Float): String {
      val maxTilt = maxOf(abs(roll), abs(pitch))
      return when {
        maxTilt < 5f -> "Dengeli"
        maxTilt < 12f -> "Mutedil Yalpa"
        maxTilt < 25f -> "Yüksek Yalpa"
        else -> "Kritik Eğim"
      }
    }
  }
}
