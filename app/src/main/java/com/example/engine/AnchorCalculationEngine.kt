package com.example.engine

import com.example.model.*
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
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

    val recommendedScope = calculateRecommendedChainScope(
      depthMeters = b,
      currentChainMeters = a,
      metersPerShackle = metersPerShackle,
      bottomType = params.bottomType,
      windSpeedKnots = params.windSpeedKnots,
      waveHeightMeters = params.waveHeightMeters,
      beaufortScale = params.beaufortScale,
      seaStateDescription = params.seaStateDescription
    )

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
      isChainShorterThanDepth = isChainShorterThanDepth,
      recommendedChainScope = recommendedScope
    )
  }

  /**
   * Girilen derinlik, zincir uzunluğu, hava ve deniz durumu parametrelerine göre
   * ideal kaloma ve tavsiye edilen kilit sayısını hesaplar.
   */
  fun calculateRecommendedChainScope(
    depthMeters: Double,
    currentChainMeters: Double,
    metersPerShackle: Double,
    bottomType: AnchorBottomType = AnchorBottomType.MUD_SAND,
    windSpeedKnots: Double = 12.0,
    waveHeightMeters: Double = 0.5,
    beaufortScale: Int = 3,
    seaStateDescription: String = "Sakin"
  ): RecommendedChainScope {
    val d = max(1.0, depthMeters)
    val a = max(1.0, currentChainMeters)
    val currentShackles = a / metersPerShackle

    // 1. Deniz ve Hava Durumu Şiddet Seviyesi
    val weatherSeverity = when {
      windSpeedKnots >= 34.0 || waveHeightMeters >= 2.5 || beaufortScale >= 8 -> WeatherSeverityLevel.STORM
      windSpeedKnots >= 22.0 || waveHeightMeters >= 1.4 || beaufortScale >= 6 -> WeatherSeverityLevel.ROUGH
      windSpeedKnots >= 14.0 || waveHeightMeters >= 0.7 || beaufortScale >= 4 -> WeatherSeverityLevel.MODERATE
      else -> WeatherSeverityLevel.CALM
    }

    // 2. Derinlik Oranı (Sığ sularda kedi bükümü/catenary için yüksek katsayı gerekir)
    val baseDepthRatio = when {
      d <= 12.0 -> 5.5
      d <= 25.0 -> 4.8
      d <= 40.0 -> 4.2
      d <= 60.0 -> 3.6
      else -> 3.0
    }

    // 3. Hava ve Dalga Çarpanı
    val weatherMultiplier = when (weatherSeverity) {
      WeatherSeverityLevel.CALM -> 1.0
      WeatherSeverityLevel.MODERATE -> 1.22
      WeatherSeverityLevel.ROUGH -> 1.50
      WeatherSeverityLevel.STORM -> 1.85
    }

    // 4. Zemin Tutma Katsayısı
    val bottomMultiplier = when (bottomType) {
      AnchorBottomType.MUD_SAND -> 1.0
      AnchorBottomType.HARD_SAND -> 1.05
      AnchorBottomType.SOFT_MUD -> 1.15
      AnchorBottomType.GRAVEL_SHELL -> 1.25
      AnchorBottomType.ROCK_CORAL -> 1.40
    }

    // 5. Asgari Güvenli Kilit Kuralı (Donanma/Ticari Standart: Kedi eğrisi için en az 3 kilit şarttır)
    val minSafeShacklesCount = when (weatherSeverity) {
      WeatherSeverityLevel.CALM -> 3.0
      WeatherSeverityLevel.MODERATE -> 3.5
      WeatherSeverityLevel.ROUGH -> 4.5
      WeatherSeverityLevel.STORM -> 5.5
    }
    val minSafeMeters = minSafeShacklesCount * metersPerShackle

    val calcMeters = d * baseDepthRatio * weatherMultiplier * bottomMultiplier
    val targetMeters = max(calcMeters, minSafeMeters)

    // Kilit sayısını en yakın buçuklu veya tam kilite yuvarla (Örn: 4.0, 4.5, 5.0, 5.5 vb.)
    val rawShackles = targetMeters / metersPerShackle
    val roundedShackles = round(rawShackles * 2.0) / 2.0
    val recommendedShackles = max(minSafeShacklesCount, roundedShackles)
    val recommendedMeters = recommendedShackles * metersPerShackle

    val recMinShackles = max(3.0, recommendedShackles - 0.5)
    val recMaxShackles = recommendedShackles + 1.0
    val heavyWeatherShackles = max(recommendedShackles + 1.5, round((d * 7.0 / metersPerShackle) * 2.0) / 2.0)

    val currentScopeRatio = a / d
    val recommendedScopeRatio = recommendedMeters / d

    // 6. Mevcut Zincir ile Fark
    val diffShackles = currentShackles - recommendedShackles
    val diffMeters = a - recommendedMeters

    val status = when {
      diffShackles < -0.4 -> ChainRecommendationStatus.DEFICIENT
      diffShackles > 1.6 -> ChainRecommendationStatus.EXCESSIVE
      else -> ChainRecommendationStatus.OPTIMAL
    }

    val weatherSummaryTr = "Rüzgar: ${String.format(Locale.US, "%.1f", windSpeedKnots)} kn • Dalga: ${String.format(Locale.US, "%.1f", waveHeightMeters)} m (Bft $beaufortScale - ${weatherSeverity.labelTr})"

    val headingText = when (status) {
      ChainRecommendationStatus.DEFICIENT -> {
        val neededSh = -diffShackles
        val neededM = -diffMeters
        "⚠️ ${String.format(Locale.US, "%.1f", neededSh)} Kilit (${String.format(Locale.US, "%.0f", neededM)}m) İlave Kaloma Veriniz"
      }
      ChainRecommendationStatus.OPTIMAL -> {
        "✅ ${String.format(Locale.US, "%.1f", recommendedShackles)} Kilit Kaloma İdeal ve Emniyetli"
      }
      ChainRecommendationStatus.EXCESSIVE -> {
        "ℹ️ ${String.format(Locale.US, "%.1f", currentShackles)} Kilit Kaloma Geniş (Güçlü Tutuş)"
      }
    }

    val detailText = when (status) {
      ChainRecommendationStatus.DEFICIENT -> {
        val neededSh = -diffShackles
        val neededM = -diffMeters
        "Mevcut ${String.format(Locale.US, "%.1f", currentShackles)} kilit (${String.format(Locale.US, "%.1f", a)}m) kaloma; ${String.format(Locale.US, "%.1f", d)}m derinlik, ${String.format(Locale.US, "%.1f", windSpeedKnots)} kn rüzgar ve ${String.format(Locale.US, "%.1f", waveHeightMeters)}m dalga için yetersizdir. Zincir ağırlığı kedi eğrisini koruyamaz ve demir tarayabilir. Emniyet için ${String.format(Locale.US, "%.1f", neededSh)} kilit (${String.format(Locale.US, "%.0f", neededM)}m) daha kaloma verilerek en az ${String.format(Locale.US, "%.1f", recommendedShackles)} kilite (${String.format(Locale.US, "%.1f", recommendedMeters)}m) ulaşılmalıdır."
      }
      ChainRecommendationStatus.OPTIMAL -> {
        "Girilen ${String.format(Locale.US, "%.1f", currentShackles)} kilit (${String.format(Locale.US, "%.1f", a)}m) zincir; ${String.format(Locale.US, "%.1f", d)}m derinlik ve deniz şartlarında (${String.format(Locale.US, "%.1f", windSpeedKnots)} kn rüzgar, ${String.format(Locale.US, "%.1f", waveHeightMeters)}m dalga) dipte ideal kedi eğrisi (catenary) sağlar. Demir bedeni yatay çekilir."
      }
      ChainRecommendationStatus.EXCESSIVE -> {
        "Döşenen ${String.format(Locale.US, "%.1f", currentShackles)} kilit zincir, tavsiye edilen ${String.format(Locale.US, "%.1f", recommendedShackles)} kilitten fazladır (+${String.format(Locale.US, "%.1f", diffShackles)} kilit). Tutuş son derece güvenlidir; ancak salma dairesi genişler. Çevre gemilere ve sığlıklara dikkat ediniz."
      }
    }

    val ruleText = "Kural: Donanma & Ticari Kaloma Standardı (${String.format(Locale.US, "%.1f", recommendedScopeRatio)}x Derinlik + Dalga/Rüzgar Payı)"

    return RecommendedChainScope(
      recommendedShackles = recommendedShackles,
      recommendedShacklesMin = recMinShackles,
      recommendedShacklesMax = recMaxShackles,
      recommendedMeters = recommendedMeters,
      minSafeShackles = minSafeShacklesCount,
      heavyWeatherShackles = heavyWeatherShackles,
      scopeRatio = currentScopeRatio,
      recommendedScopeRatio = recommendedScopeRatio,
      weatherSeverity = weatherSeverity,
      weatherSummaryTr = weatherSummaryTr,
      status = status,
      differenceShackles = diffShackles,
      differenceMeters = diffMeters,
      recommendationHeading = headingText,
      recommendationDetailTr = detailText,
      seamanshipRuleText = ruleText
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
