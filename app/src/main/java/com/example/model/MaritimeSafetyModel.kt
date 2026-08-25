package com.example.model

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * Denize Adam Düştü (MOB - Man Overboard) Veri Modeli
 */
data class MobEvent(
  val isActive: Boolean = false,
  val timestampEpochMs: Long = 0L,
  val timeFormatted: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val vesselHeadingAtDrop: Int = 0,
  val vesselSpeedAtDropKnots: Double = 0.0
) {
  fun calculateDistanceNm(currentLat: Double, currentLon: Double): Double {
    return calculateHaversineDistanceNm(currentLat, currentLon, latitude, longitude)
  }

  fun calculateDistanceGomina(currentLat: Double, currentLon: Double): Double {
    return calculateDistanceNm(currentLat, currentLon) * 10.0
  }

  fun calculateDistanceMeters(currentLat: Double, currentLon: Double): Double {
    return calculateDistanceNm(currentLat, currentLon) * 1852.0
  }

  fun calculateBearingDegrees(currentLat: Double, currentLon: Double): Int {
    return calculateRhumbBearing(currentLat, currentLon, latitude, longitude)
  }
}

/**
 * Demirleme Mevkisi (Anchor Drop Point & Swinging Circles / Salma Dairesi) Veri Modeli
 * 1 Deniz Mili = 10 Gomina = 1852 Metre
 * 1 Gomina = 185.2 Metre (0.1 NM)
 * Toplam 10 Gomina çapında daireler: 1'er gomina aralıklarla
 */
data class AnchorDropEvent(
  val isAnchored: Boolean = false,
  val dropTimestampEpochMs: Long = 0L,
  val dropTimeFormatted: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val chartedDepthAtDropMeters: Double = 10.0,
  val totalGominaRings: Int = 10, // 1'den 10 gomina çapına/yarıçapına kadar halkalar
  val safeSwingingRadiusGomina: Double = 2.5 // Emniyetli salma yarıçapı
) {
  fun calculateDistanceNm(currentLat: Double, currentLon: Double): Double {
    return calculateHaversineDistanceNm(currentLat, currentLon, latitude, longitude)
  }

  fun calculateDistanceGomina(currentLat: Double, currentLon: Double): Double {
    return calculateDistanceNm(currentLat, currentLon) * 10.0
  }

  fun calculateDistanceMeters(currentLat: Double, currentLon: Double): Double {
    return calculateDistanceNm(currentLat, currentLon) * 1852.0
  }

  fun calculateBearingDegrees(currentLat: Double, currentLon: Double): Int {
    return calculateRhumbBearing(currentLat, currentLon, latitude, longitude)
  }

  fun isDragging(currentLat: Double, currentLon: Double): Boolean {
    if (!isAnchored) return false
    val distGomina = calculateDistanceGomina(currentLat, currentLon)
    return distGomina > safeSwingingRadiusGomina
  }
}

fun calculateHaversineDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val r = 6371.0 // Earth radius in km
  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)
  val a = sin(dLat / 2) * sin(dLat / 2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
      sin(dLon / 2) * sin(dLon / 2)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  val distKm = r * c
  return distKm / 1.852 // km to Nautical Miles
}

fun calculateRhumbBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
  val phi1 = Math.toRadians(lat1)
  val phi2 = Math.toRadians(lat2)
  val deltaLambda = Math.toRadians(lon2 - lon1)
  val y = sin(deltaLambda) * cos(phi2)
  val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
  val bearingRad = atan2(y, x)
  val deg = Math.toDegrees(bearingRad)
  return ((deg + 360) % 360).toInt()
}
