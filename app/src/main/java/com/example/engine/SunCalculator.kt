package com.example.engine

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Jean Meeus / NOAA Solar Calculations Algoritması ile
 * Verilen Koordinat ve Tarih için Güneş Doğuşu, Gün Batımı, Alacakaranlık ve Gün Işığı Süresi Hesaplayıcı
 */
data class SunTimesInfo(
  val sunriseFormatted: String,        // Örn: "06:12"
  val sunsetFormatted: String,         // Örn: "19:48"
  val dawnCivilFormatted: String,      // Sivil Alacakaranlık / Şafak: "05:42"
  val duskCivilFormatted: String,      // Sivil Alacakaranlık Sonu: "20:18"
  val solarNoonFormatted: String,      // Güneş Tepe Noktası: "13:00"
  val daylightDurationFormatted: String, // Gün Işığı Süresi: "13 sa 36 dk"
  val isDaylightNow: Boolean,          // Şu an gündüz mü?
  val sunAltitudeDegrees: Double,      // Anlık Güneş Yüksekliği
  val sunAzimuthDegrees: Double        // Anlık Güneş Azimut Açısı
)

object SunCalculator {

  /**
   * Belirtilen enlem, boylam ve takvim için gün doğumu ve gün batımını hesaplar.
   */
  fun calculateSunTimes(
    latitude: Double,
    longitude: Double,
    calendar: Calendar = Calendar.getInstance(),
    timeZone: TimeZone = calendar.timeZone ?: TimeZone.getDefault()
  ): SunTimesInfo {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1 // 1-12
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val tzOffsetHours = timeZone.getOffset(calendar.timeInMillis) / 3600000.0

    // Yılın günü (Day of Year)
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

    // Kesirli yıl (radyan)
    val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1 + (hour - 12.0) / 24.0)

    // Zaman Denklemi (Equation of Time - dakika)
    val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
        0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma))

    // Güneş Sapması (Solar Declination - radyan)
    val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
        0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
        0.002697 * cos(3.0 * gamma) + 0.00148 * sin(3.0 * gamma)

    val latRad = Math.toRadians(latitude)

    // 1. Resmi Güneş Doğuşu & Batışı (Zenith = 90.833° - Atmosferik Kırılma ve Güneş Yarıçapı Dahil)
    val zenithOfficial = Math.toRadians(90.8333)
    val cosHourAngleOfficial = (cos(zenithOfficial) / (cos(latRad) * cos(decl))) - (tan(latRad) * tan(decl))

    // 2. Sivil Alacakaranlık (Zenith = 96.0°)
    val zenithCivil = Math.toRadians(96.0)
    val cosHourAngleCivil = (cos(zenithCivil) / (cos(latRad) * cos(decl))) - (tan(latRad) * tan(decl))

    val (sunriseMin, sunsetMin) = calculateTimesFromHa(cosHourAngleOfficial, eqTime, longitude, tzOffsetHours)
    val (dawnMin, duskMin) = calculateTimesFromHa(cosHourAngleCivil, eqTime, longitude, tzOffsetHours)

    val solarNoonMin = 720.0 - (4.0 * longitude) - eqTime + (tzOffsetHours * 60.0)

    // Gün Işığı Süresi
    val daylightMinutes = if (sunriseMin != null && sunsetMin != null) {
      max(0.0, sunsetMin - sunriseMin)
    } else if (latitude > 65.0) {
      1440.0 // Kutup Gündüzü
    } else {
      0.0 // Kutup Gecesi
    }

    val daylightHours = (daylightMinutes / 60.0).toInt()
    val daylightMins = (daylightMinutes % 60.0).toInt()
    val daylightStr = "${daylightHours} sa ${daylightMins} dk"

    // Anlık Güneş Pozisyonu (Azimut ve Yükseklik)
    val currentLocalMinutes = hour * 60.0 + minute
    val trueSolarTimeMin = (currentLocalMinutes + eqTime + (4.0 * longitude) - (60.0 * tzOffsetHours) + 1440.0) % 1440.0
    val hourAngleCurrent = Math.toRadians(if (trueSolarTimeMin / 4.0 < 0) trueSolarTimeMin / 4.0 + 180 else trueSolarTimeMin / 4.0 - 180)

    val sinAlt = sin(latRad) * sin(decl) + cos(latRad) * cos(decl) * cos(hourAngleCurrent)
    val altitudeRad = asin(sinAlt.coerceIn(-1.0, 1.0))
    val altitudeDeg = Math.toDegrees(altitudeRad)

    val cosAzimuth = (sin(decl) - sin(latRad) * sin(altitudeRad)) / (cos(latRad) * cos(altitudeRad))
    val azimuthRad = acos(cosAzimuth.coerceIn(-1.0, 1.0))
    val azimuthDeg = if (hourAngleCurrent > 0) 360.0 - Math.toDegrees(azimuthRad) else Math.toDegrees(azimuthRad)

    val isDaylight = altitudeDeg > -0.833 // Güneş ufkun üstündeyse

    return SunTimesInfo(
      sunriseFormatted = formatMinutesToTime(sunriseMin ?: 360.0),
      sunsetFormatted = formatMinutesToTime(sunsetMin ?: 1140.0),
      dawnCivilFormatted = formatMinutesToTime(dawnMin ?: 330.0),
      duskCivilFormatted = formatMinutesToTime(duskMin ?: 1170.0),
      solarNoonFormatted = formatMinutesToTime(solarNoonMin),
      daylightDurationFormatted = daylightStr,
      isDaylightNow = isDaylight,
      sunAltitudeDegrees = round(altitudeDeg * 10.0) / 10.0,
      sunAzimuthDegrees = round(azimuthDeg * 10.0) / 10.0
    )
  }

  private fun calculateTimesFromHa(
    cosHa: Double,
    eqTime: Double,
    longitude: Double,
    tzOffsetHours: Double
  ): Pair<Double?, Double?> {
    if (cosHa > 1.0) {
      // Güneş hiç doğmuyor (Kutup Gecesi)
      return null to null
    }
    if (cosHa < -1.0) {
      // Güneş hiç batmıyor (Kutup Gündüzü)
      return 0.0 to 1440.0
    }

    val haRad = acos(cosHa)
    val haDeg = Math.toDegrees(haRad)

    val sunriseMin = 720.0 - (4.0 * (longitude + haDeg)) - eqTime + (tzOffsetHours * 60.0)
    val sunsetMin = 720.0 - (4.0 * (longitude - haDeg)) - eqTime + (tzOffsetHours * 60.0)

    val normalizedSunrise = (sunriseMin + 1440.0) % 1440.0
    val normalizedSunset = (sunsetMin + 1440.0) % 1440.0

    return normalizedSunrise to normalizedSunset
  }

  private fun formatMinutesToTime(totalMinutes: Double): String {
    val mins = ((totalMinutes.toInt() % 1440) + 1440) % 1440
    val h = mins / 60
    val m = mins % 60
    return String.format(java.util.Locale.US, "%02d:%02d", h, m)
  }
}
