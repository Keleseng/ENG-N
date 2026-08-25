package com.example.model

enum class ExtremumType {
  HIGH_WATER, // Yüksek Su (HW)
  LOW_WATER   // Alçak Su (LW)
}

enum class WindowSafetyRating {
  OPTIMAL,  // Geniş pay ve yüksek UKC
  SUFFICIENT, // Güvenli ancak dikkatli seyir
  MARGINAL,   // Minimum UKC sınırında
  UNSAFE      // Yetersiz derinlik
}

data class TideHeightPoint(
  val hourOfDay: Double, // 0.0 to 24.0
  val timeFormatted: String,
  val tideHeight: Double, // metre (CD'ye göre yükseklik)
  val totalWaterDepth: Double, // Harita Derinliği + Gelgit Yüksekliği
  val availableUkc: Double, // totalWaterDepth - (draft + squat)
  val isSafe: Boolean
)

data class TideExtremum(
  val type: ExtremumType,
  val hourOfDay: Double,
  val timeFormatted: String,
  val tideHeightMeters: Double,
  val totalWaterDepthMeters: Double
)

data class SafeNavigationWindow(
  val id: Int,
  val startHour: Double,
  val endHour: Double,
  val startTimeFormatted: String,
  val endTimeFormatted: String,
  val durationMinutes: Int,
  val peakHour: Double,
  val peakTimeFormatted: String,
  val maxTideHeightMeters: Double,
  val maxWaterDepthMeters: Double,
  val maxUkcMeters: Double,
  val minUkcMeters: Double,
  val rating: WindowSafetyRating,
  val isCurrentTimeInside: Boolean
)

data class RuleOfTwelfthStep(
  val stepIndex: Int, // 1 to 6
  val timeRangeFormatted: String,
  val fractionLabel: String, // "1/12", "2/12", "3/12", vb.
  val fractionValue: Double,
  val intervalRiseMeters: Double,
  val cumulativeHeightMeters: Double
)

data class MarineCurrentInfo(
  val directionDegrees: Int, // e.g. 245°
  val directionCardinal: String, // "WSW / Batı-Güneybatı"
  val speedKnots: Double, // e.g. 1.8 knot
  val phaseName: String, // "Taşkın Akıntısı (Flood)", "Çekilme Akıntısı (Ebb)", "Ölü Su (Slack)"
  val summary: String
)

data class MarineWindInfo(
  val directionDegrees: Int, // e.g. 035°
  val directionCardinal: String, // "NNE / Poyraz"
  val speedKnots: Double, // e.g. 14 knot
  val beaufortScale: Int, // e.g. 4
  val beaufortDescription: String, // "Orta Rüzgar (Moderate Breeze)"
  val seaStateDescription: String // "Küçük Dalgalar, Beyaz Köpükler"
)

data class NavigationAnalysis(
  val vessel: VesselProfile,
  val location: PortLocation,
  val customLat: Double,
  val customLon: Double,
  val chartedDepthMeters: Double,
  val actualDraftMeters: Double,
  val minUkcMeters: Double,
  val vesselSpeedKnots: Double,
  val vesselHeadingDegrees: Int,
  val vesselHeadingCardinal: String,
  val calculatedSquatMeters: Double,
  val totalRequiredDepthMeters: Double,
  val currentInstantTideHeightMeters: Double,
  val currentInstantTotalDepthMeters: Double,
  val currentInstantUkcMeters: Double,
  val currentInfo: MarineCurrentInfo,
  val windInfo: MarineWindInfo,
  val selectedDateFormatted: String,
  val moonPhaseName: String,
  val moonIlluminationPercent: Int,
  val isSpringTide: Boolean,
  val realMoonInfo: com.example.engine.RealMoonInfo? = null,
  val maxTideHeight24h: Double,
  val minTideHeight24h: Double,
  val maxAvailableDepth24h: Double,
  val minAvailableDepth24h: Double,
  val safeWindows: List<SafeNavigationWindow>,
  val extrema: List<TideExtremum>,
  val curvePoints: List<TideHeightPoint>,
  val ruleOfTwelfths: List<RuleOfTwelfthStep>,
  val currentHour: Double,
  val isCurrentlySafe: Boolean,
  val advisoryBadge: String,
  val advisorySummary: String,
  val safetyChecklist: List<Pair<String, Boolean>>
)
