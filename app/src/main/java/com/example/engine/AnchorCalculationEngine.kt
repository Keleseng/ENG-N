package com.example.engine

import com.example.model.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Denizcilik Demirleme ve Salma Dairesi Hesaplama Motoru
 *
 * Ders Kitabı Formül Eşleştirmesi:
 * a² = b² + c²
 *
 * a = Gemi demir zincirine verilen kaloma (metre / kilit)
 * b = Derinlik (metre)
 * c = Loçadan demir yerine olan yatay mesafe [c² = a² - b² => c = √(a² - b²)]
 * d = Köprüüstünden loçaya olan mesafe + Loçadan demir yerine olan yatay mesafe (I. Salma Dairesi için)
 * e = Köprüüstünden kıça kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi için)
 * f = Gemi Boyu kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi için)
 */
object AnchorCalculationEngine {

  const val METERS_PER_SHACKLE = 27.5 // 1 Kilit = 27.5 Metre (Kitap Standardı)
  const val METERS_PER_FATHOM = 1.8288 // 1 Kulaç = 6 Feet = 1.8288 metre
  const val METERS_PER_FEET = 0.3048 // 1 Foot = 0.3048 metre
  const val METERS_PER_GOMINA = 185.2 // 1 Gomina = 0.1 NM = 185.2 metre
  const val METERS_PER_NM = 1852.0 // 1 Deniz Mili = 1852 metre

  fun calculate(params: AnchorCalculationParams): AnchorCalculationResult {
    val a = max(1.0, params.chainScopeMeters)
    val b = max(0.5, params.depthMeters)
    val isChainShorterThanDepth = a <= b

    val aSq = a.pow(2)
    val bSq = b.pow(2)
    val cSq = if (aSq >= bSq) aSq - bSq else 0.0

    // c: Loçadan demir yerine olan yatay mesafe
    val c = if (params.isAutoCalculateHorizontal || params.customHorizontalDistanceMeters == null) {
      if (a > b) {
        sqrt(cSq)
      } else {
        0.0
      }
    } else {
      max(0.0, params.customHorizontalDistanceMeters)
    }

    val distBridgeToHawse = max(0.0, params.distBridgeToHawseMeters)
    val distBridgeToStern = max(0.0, params.distBridgeToSternMeters)
    val calculatedShipLength = distBridgeToHawse + distBridgeToStern
    val loa = if (params.loaMeters > 0) params.loaMeters else calculatedShipLength
    val safetyMargin = max(0.0, params.safetyMarginMeters)

    // d = Köprüüstünden loçaya olan mesafe + c (I. Salma Dairesi)
    val d = distBridgeToHawse + c

    // e = Köprüüstünden kıça kadar olan mesafe + c (II. Salma Dairesi alternatif köprüüstü gözlemi)
    val e = distBridgeToStern + c

    // f = Gemi Boyu kadar olan mesafe (LOA) + c (II. Salma Dairesi için - Toplam Emniyet Salma Çemberi)
    val f = loa + c
    val f_withSafety = f + safetyMargin

    // Kaloma oranı: a / b
    val scopeRatio = if (b > 0.0) a / b else 0.0

    val scopeStatus = when {
      isChainShorterThanDepth || scopeRatio < 2.5 -> ScopeSafetyStatus.CRITICAL_SHORT
      scopeRatio < 4.0 -> ScopeSafetyStatus.LOW_SCOPE
      scopeRatio <= 6.5 -> ScopeSafetyStatus.NORMAL_SCOPE
      else -> ScopeSafetyStatus.HEAVY_WEATHER_SCOPE
    }

    val metersPerShackle = params.shackleStandard.metersPerShackle

    return AnchorCalculationResult(
      a_chainScopeMeters = a,
      a_chainScopeShackles = a / metersPerShackle,
      a_chainScopeFathoms = a / METERS_PER_FATHOM,
      shackleStandard = params.shackleStandard,

      b_depthMeters = b,
      b_depthFeet = b / METERS_PER_FEET,
      b_depthFathoms = b / METERS_PER_FATHOM,

      c_horizontalDistanceMeters = c,
      c_horizontalDistanceGomina = c / METERS_PER_GOMINA,

      d_firstSwingingCircleMeters = d,
      d_firstSwingingCircleGomina = d / METERS_PER_GOMINA,
      d_firstSwingingCircleNm = d / METERS_PER_NM,

      e_bridgeSternSwingingCircleMeters = e,
      e_bridgeSternSwingingCircleGomina = e / METERS_PER_GOMINA,

      f_secondSwingingCircleMeters = f,
      f_secondSwingingCircleGomina = f / METERS_PER_GOMINA,
      f_secondSwingingCircleNm = f / METERS_PER_NM,
      f_withSafetyMarginMeters = f_withSafety,

      aSquared = aSq,
      bSquared = bSq,
      cSquared = cSq,
      calculatedShipLengthMeters = calculatedShipLength,

      scopeRatio = scopeRatio,
      scopeStatus = scopeStatus,
      distBridgeToHawseMeters = distBridgeToHawse,
      distBridgeToSternMeters = distBridgeToStern,
      loaMeters = loa,
      safetyMarginMeters = safetyMargin,
      isChainShorterThanDepth = isChainShorterThanDepth
    )
  }

  /**
   * Kilit (Shackle) sayısını metreye çevirir
   */
  fun shacklesToMeters(shackles: Double): Double {
    return shackles * METERS_PER_SHACKLE
  }

  /**
   * Metreyi Kilit (Shackle) sayısına çevirir
   */
  fun metersToShackles(meters: Double): Double {
    return meters / METERS_PER_SHACKLE
  }
}
