package com.example.model

import java.util.Locale
import kotlin.math.*

/**
 * Radar ekranında gösterilecek AIS Hedef Gemisi modeli.
 * Gerçek denizcilik CPA (Closest Point of Approach), TCPA (Time to CPA),
 * mesafe, kerteriz ve hız vektörlerini barındırır.
 */
data class RadarAisTarget(
  val id: String,
  val name: String,
  val mmsi: String,
  val imo: String = "",
  val callSign: String = "",
  val flag: String = "TR",
  val shipType: String = "Genel Kargo",
  val status: String = "Yolda Motorla Seyrediyor",
  var latitude: Double,
  var longitude: Double,
  val sogKnots: Double, // Hız (kn)
  val cogDegrees: Double, // Rota (derece)
  val headingDegrees: Int = cogDegrees.toInt(),
  val loaMeters: Double = 120.0,
  val beamMeters: Double = 18.0,
  val draftMeters: Double = 6.2,
  val destination: String = "İSTANBUL",
  val eta: String = "Bugün 18:00 UTC",
  val isHazardous: Boolean = false, // Çatışma riski var mı?
  val isAnchored: Boolean = sogKnots < 0.5
) {
  /**
   * Kendi gemimize göre mesafe (Deniz Mili / NM)
   */
  fun distanceNmFrom(ownLat: Double, ownLon: Double): Double {
    return calculateDistanceNm(ownLat, ownLon, latitude, longitude)
  }

  /**
   * Kendi gemimize göre gerçek kerteriz (True Bearing in Degrees 000-359°)
   */
  fun bearingFrom(ownLat: Double, ownLon: Double): Double {
    return calculateBearingDegrees(ownLat, ownLon, latitude, longitude)
  }

  /**
   * Çatışma Değerlendirmesi: CPA (En Yakın Geçiş Mesafesi - NM)
   * ve TCPA (En Yakın Geçişe Kalan Dakika).
   */
  fun calculateCpaTcpa(
    ownLat: Double,
    ownLon: Double,
    ownSogKnots: Double,
    ownCogDegrees: Double
  ): Pair<Double, Double> {
    // Kendi gemimiz ile hedefin relatif hareket vektörü analizi
    val distNm = distanceNmFrom(ownLat, ownLon)
    val brgDeg = bearingFrom(ownLat, ownLon)
    val brgRad = Math.toRadians(brgDeg)

    // Göreceli konum (NM)
    val dx = distNm * sin(brgRad) // Doğu bileşeni
    val dy = distNm * cos(brgRad) // Kuzey bileşeni

    // Hız bileşenleri (knots)
    val ownVx = ownSogKnots * sin(Math.toRadians(ownCogDegrees))
    val ownVy = ownSogKnots * cos(Math.toRadians(ownCogDegrees))

    val tgtVx = sogKnots * sin(Math.toRadians(cogDegrees))
    val tgtVy = sogKnots * cos(Math.toRadians(cogDegrees))

    // Hedefin kendi gemimize göre nispi hızı (Relative velocity: Vr = Vt - Vo)
    val relVx = tgtVx - ownVx
    val relVy = tgtVy - ownVy
    val relSpeedSq = relVx * relVx + relVy * relVy

    if (relSpeedSq < 0.0001) {
      // Nispi hız sıfıra yakın (paralel seyir veya sabit)
      return Pair(distNm, 999.0)
    }

    // TCPA = - (dx * relVx + dy * relVy) / (relSpeedSq) [saat cinsinden]
    val tcpaHours = - (dx * relVx + dy * relVy) / relSpeedSq
    val tcpaMinutes = tcpaHours * 60.0

    // CPA konumu
    val cpaX = dx + relVx * max(0.0, tcpaHours)
    val cpaY = dy + relVy * max(0.0, tcpaHours)
    val cpaNm = sqrt(cpaX * cpaX + cpaY * cpaY)

    return Pair(cpaNm, if (tcpaMinutes < 0) -1.0 else tcpaMinutes)
  }

  companion object {
    fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val rKm = 6371.0
      val dLat = Math.toRadians(lat2 - lat1)
      val dLon = Math.toRadians(lon2 - lon1)
      val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).pow(2)
      val c = 2 * atan2(sqrt(a), sqrt(1 - a))
      val distKm = rKm * c
      return distKm / 1.852 // km -> Nautical Miles
    }

    fun calculateBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val φ1 = Math.toRadians(lat1)
      val φ2 = Math.toRadians(lat2)
      val Δλ = Math.toRadians(lon2 - lon1)

      val y = sin(Δλ) * cos(φ2)
      val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)
      val θ = atan2(y, x)
      val brg = Math.toDegrees(θ)
      return (brg + 360.0) % 360.0
    }
  }
}
