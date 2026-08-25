package com.example.engine

import com.example.location.GpsFix
import com.example.model.SpeedCalculationResult
import kotlin.math.*

object SpeedCalculationEngine {

  /**
   * GPS Sürati (SOG) ve Yere Göre Sürati (Ground Speed) Suya Göre Sürat (STW),
   * Deniz Akıntısı (Tidal Current) ve Rüzgar Sürüklenmesi (Leeway) vektörlerini birleştirerek hesaplar.
   */
  fun calculateSpeeds(
    gpsFix: GpsFix?,
    speedThroughWaterKnots: Double,
    vesselHeadingDegrees: Int,
    currentSpeedKnots: Double,
    currentDirectionDegrees: Int,
    windSpeedKnots: Double,
    windDirectionDegrees: Int
  ): SpeedCalculationResult {
    // 1. GPS Sürati (Sensörden veya son GPS telemetrisinden)
    val isGpsActive = gpsFix != null
    val gpsSog = if (gpsFix?.speedKnots != null && gpsFix.speedKnots >= 0.0) {
      round(gpsFix.speedKnots * 10.0) / 10.0
    } else {
      speedThroughWaterKnots
    }

    // 2. Rüzgar Sürüklenmesi (Leeway angle) hesabı
    // Leeway açısı gemi pruvası ile rüzgar açısı arasındaki fark ve rüzgar hızı/gemi hızına göre formülize edilir
    val relativeWindAngle = ((windDirectionDegrees - vesselHeadingDegrees + 180 + 360) % 360) - 180
    val stwSafe = if (speedThroughWaterKnots > 0.5) speedThroughWaterKnots else 1.0
    val rawLeewayDeg = (windSpeedKnots / (stwSafe * 8.0)) * sin(Math.toRadians(relativeWindAngle.toDouble()))
    val leewayAngleDeg = max(-15.0, min(15.0, rawLeewayDeg))

    // 3. Suya Göre Hareket Vektörü (STW + Leeway)
    val vesselCourseThroughWaterDeg = (vesselHeadingDegrees + leewayAngleDeg + 360.0) % 360.0
    val vWaterRad = Math.toRadians(vesselCourseThroughWaterDeg)
    val vWaterX = speedThroughWaterKnots * sin(vWaterRad) // Doğu bileşeni (East)
    val vWaterY = speedThroughWaterKnots * cos(vWaterRad) // Kuzey bileşeni (North)

    // 4. Akıntı Vektörü (Current Vector)
    val vCurrentRad = Math.toRadians(currentDirectionDegrees.toDouble())
    val vCurrentX = currentSpeedKnots * sin(vCurrentRad)
    val vCurrentY = currentSpeedKnots * cos(vCurrentRad)

    // 5. Toplam Yere Göre Hız Vektörü (Ground Vector = Water + Current)
    val vGroundX = vWaterX + vCurrentX
    val vGroundY = vWaterY + vCurrentY

    val calculatedGroundSpeed = round(hypot(vGroundX, vGroundY) * 10.0) / 10.0

    // Yere Göre Gidilen Rota (COG - Course Over Ground)
    val cogRad = atan2(vGroundX, vGroundY)
    val cogDeg = ((Math.toDegrees(cogRad).toInt() % 360) + 360) % 360

    // Fark Değerlendirmesi
    val effectiveSog = if (isGpsActive && gpsFix?.speedKnots != null) gpsSog else calculatedGroundSpeed
    val deltaSpeed = round((effectiveSog - speedThroughWaterKnots) * 10.0) / 10.0

    val speedEvaluation = when {
      deltaSpeed > 0.4 -> "Akıntı ve rüzgar lehte: Yere göre sürat +${deltaSpeed} kn arttı."
      deltaSpeed < -0.4 -> "Karşı akıntı/rüzgar direnci: Yere göre sürat ${deltaSpeed} kn azaldı."
      else -> "Akıntı ve rüzgar etkisi dengeli (ΔV ≈ ${deltaSpeed} kn)."
    }

    val driftStatus = when {
      abs(leewayAngleDeg) > 4.0 -> "Rüzgar kaçması (Leeway): ${round(abs(leewayAngleDeg) * 10.0) / 10.0}° (${if (leewayAngleDeg > 0) "Sancak" else "İskele"})."
      abs(cogDeg - vesselHeadingDegrees) > 5 -> "Akıntı rotadan saptırıyor: COG ${cogDeg}° (Pruva: ${vesselHeadingDegrees}°)."
      else -> "Rota sapması nominal (Pruva ve COG uyumlu)."
    }

    return SpeedCalculationResult(
      gpsSpeedKnots = gpsSog,
      isGpsActive = isGpsActive,
      calculatedGroundSpeedKnots = calculatedGroundSpeed,
      speedThroughWaterKnots = speedThroughWaterKnots,
      vesselHeadingDegrees = vesselHeadingDegrees,
      groundCourseDegrees = cogDeg,
      currentSpeedKnots = currentSpeedKnots,
      currentDirectionDegrees = currentDirectionDegrees,
      windDriftAngleDegrees = round(leewayAngleDeg * 10.0) / 10.0,
      deltaSpeedKnots = deltaSpeed,
      speedEvaluationText = speedEvaluation,
      driftStatusText = driftStatus
    )
  }
}
