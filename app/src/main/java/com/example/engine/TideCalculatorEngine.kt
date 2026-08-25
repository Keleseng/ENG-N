package com.example.engine

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object TideCalculatorEngine {

  /**
   * Squat (Çökelme) Hesabı:
   * Sığ veya dar kanalda seyreden gemilerde dinamik su çekimi artışı.
   * Formül (Barrass Confined Waters): Squat = 2 * Cb * (V^2 / 100)
   */
  fun calculateSquat(blockCoefficient: Double, speedKnots: Double, isConfinedChannel: Boolean = true): Double {
    val factor = if (isConfinedChannel) 2.0 else 1.0
    val rawSquat = factor * blockCoefficient * ((speedKnots * speedKnots) / 100.0)
    // Sınırla ve yuvarla
    return (round(rawSquat * 100.0) / 100.0).coerceAtLeast(0.0)
  }

  data class LunarPhaseResult(
    val phaseName: String,
    val illuminationPercent: Int,
    val moonAgeDays: Double,
    val springNeapFactor: Double,
    val isSpringTide: Boolean,
    val isNeapTide: Boolean
  )

  /**
   * Yüksek Hassasiyetli Astronomik Ay Evresi ve Çekim Katsayısı Hesabı (Meeus Algoritması)
   * Seçilen takvim tarihi için kesin Jülyen Günü (JD) ve Sinodik Ay Döngüsü (29.53059 gün)
   * üzerinden ay yaşı, diskin aydınlanma yüzdesi ve gelgit çekim katsayısını hesaplar.
   */
  fun calculateLunarPhase(calendar: Calendar): LunarPhaseResult {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1 // 1..12
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = (y / 100.0).toInt()
    val b = 2 - a + (a / 4.0).toInt()
    val dayFraction = day.toDouble() + (hour.toDouble() + minute.toDouble() / 60.0) / 24.0
    val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + dayFraction + b - 1524.5

    // Referans Yeniay Başlangıç Çağı (Epoch): 6 Ocak 2000 18:14 UTC -> JD 2451549.260417
    val synodicMonth = 29.530588853 // Gün
    val jd0 = 2451549.260417
    val deltaDays = jd - jd0
    val moonAge = ((deltaDays % synodicMonth) + synodicMonth) % synodicMonth
    val phaseFraction = moonAge / synodicMonth

    // Ay diski geometrik aydınlanma yüzdesi: (1 - cos(2*pi*phase)) / 2 * 100
    val illumination = (0.5 * (1.0 - cos(2.0 * PI * phaseFraction))) * 100.0
    val illuminationPercent = illumination.roundToInt().coerceIn(0, 100)

    val name: String
    val factor: Double
    val isSpring: Boolean
    val isNeap: Boolean

    when {
      moonAge < 1.48 || moonAge >= 28.05 -> {
        name = "Yeniay (New Moon)"
        factor = 1.32
        isSpring = true
        isNeap = false
      }
      moonAge in 1.48..6.38 -> {
        name = "Hilal (Büyüyen / Waxing Crescent)"
        factor = 1.10
        isSpring = false
        isNeap = false
      }
      moonAge in 6.38..8.38 -> {
        name = "İlk Dördün (First Quarter)"
        factor = 0.74
        isSpring = false
        isNeap = true
      }
      moonAge in 8.38..13.76 -> {
        name = "Şişkin Ay (Büyüyen / Waxing Gibbous)"
        factor = 1.05
        isSpring = false
        isNeap = false
      }
      moonAge in 13.76..15.77 -> {
        name = "Dolunay (Full Moon)"
        factor = 1.30
        isSpring = true
        isNeap = false
      }
      moonAge in 15.77..21.15 -> {
        name = "Şişkin Ay (Küçülen / Waning Gibbous)"
        factor = 1.05
        isSpring = false
        isNeap = false
      }
      moonAge in 21.15..23.15 -> {
        name = "Son Dördün (Last Quarter)"
        factor = 0.72
        isSpring = false
        isNeap = true
      }
      else -> {
        name = "Hilal (Küçülen / Waning Crescent)"
        factor = 0.95
        isSpring = false
        isNeap = false
      }
    }

    val ageRounded = round(moonAge * 10.0) / 10.0

    return LunarPhaseResult(
      phaseName = name,
      illuminationPercent = illuminationPercent,
      moonAgeDays = ageRounded,
      springNeapFactor = factor,
      isSpringTide = isSpring,
      isNeapTide = isNeap
    )
  }

  /**
   * Konum ve koordinata göre temel gelgit parametreleri
   */
  private fun getTideConstituents(
    lat: Double,
    lon: Double,
    baseRange: Double,
    springFactor: Double
  ): TideHarmonics {
    // Bölgesel katsayılar
    val latRad = Math.toRadians(lat)
    val lonRad = Math.toRadians(lon)

    val halfRange = (baseRange / 2.0) * springFactor

    // M2 (Ana ay yarı-günlük): %65
    val ampM2 = halfRange * 0.65
    // S2 (Ana güneş yarı-günlük): %22
    val ampS2 = halfRange * 0.22
    // K1 (Ay-Güneş günlük): %09
    val ampK1 = halfRange * 0.09
    // O1 (Ana ay günlük): %04
    val ampO1 = halfRange * 0.04

    // Coğrafi faz kaymaları (Boylam ve Enlem etkisi)
    val phaseShiftHours = ((lon % 180.0) / 15.0) % 12.42
    val phaseM2 = (phaseShiftHours * (2 * PI / 12.4206)) + (latRad * 0.5)
    val phaseS2 = (phaseShiftHours * (2 * PI / 12.0000))
    val phaseK1 = (phaseShiftHours * (2 * PI / 23.9345)) + (lonRad * 0.3)
    val phaseO1 = (phaseShiftHours * (2 * PI / 25.8193)) - (latRad * 0.2)

    val meanLevel = halfRange * 1.02 // CD'ye göre ortalama su seviyesi

    return TideHarmonics(
      meanLevel = meanLevel,
      ampM2 = ampM2,
      ampS2 = ampS2,
      ampK1 = ampK1,
      ampO1 = ampO1,
      phaseM2 = phaseM2,
      phaseS2 = phaseS2,
      phaseK1 = phaseK1,
      phaseO1 = phaseO1
    )
  }

  private data class TideHarmonics(
    val meanLevel: Double,
    val ampM2: Double,
    val ampS2: Double,
    val ampK1: Double,
    val ampO1: Double,
    val phaseM2: Double,
    val phaseS2: Double,
    val phaseK1: Double,
    val phaseO1: Double
  )

  /**
   * Belirli bir t saatindeki gelgit yüksekliğini hesaplar
   */
  private fun calculateTideHeightAtHour(t: Double, h: TideHarmonics): Double {
    val m2 = h.ampM2 * cos((2 * PI * t / 12.4206) - h.phaseM2)
    val s2 = h.ampS2 * cos((2 * PI * t / 12.0000) - h.phaseS2)
    val k1 = h.ampK1 * cos((2 * PI * t / 23.9345) - h.phaseK1)
    val o1 = h.ampO1 * cos((2 * PI * t / 25.8193) - h.phaseO1)

    val height = h.meanLevel + m2 + s2 + k1 + o1
    return round(height * 100.0) / 100.0
  }

  /**
   * Ana Analiz ve Güvenli Pencere Motoru
   */
  fun analyzeNavigation(
    vessel: VesselProfile,
    location: PortLocation,
    customLat: Double,
    customLon: Double,
    chartedDepth: Double,
    actualDraft: Double,
    minUkc: Double,
    speedKnots: Double,
    headingDegrees: Int = 45,
    selectedCalendar: Calendar
  ): NavigationAnalysis {
    val squat = calculateSquat(vessel.blockCoefficient, speedKnots, isConfinedChannel = true)
    val totalRequiredDepth = actualDraft + minUkc + squat

    // Ay evresi ve astronomik çekim katsayısı (mooncalendar.today & Kesin Meeus Algoritması)
    val realMoon = RealMoonDataProvider.calculateAstronomicalRealMoon(selectedCalendar)
    val springFactor = realMoon.springNeapFactor
    val isSpringTide = realMoon.isSpringTide

    // Base tide range
    val baseRange = if (location.id == "custom") {
      // Enleme göre varsayılan gelgit aralığı kestirimi
      val latAbs = abs(customLat)
      when {
        latAbs > 48.0 -> 4.5
        latAbs in 30.0..48.0 -> 1.8
        else -> 2.2
      }
    } else {
      location.typicalTideRangeMeters
    }

    val harmonics = getTideConstituents(customLat, customLon, baseRange, springFactor)

    // 24 saatlik eğri örnekleri (Her 5 dakikada bir = 289 nokta)
    val curvePoints = mutableListOf<TideHeightPoint>()
    val stepMinutes = 5
    val totalSteps = (24 * 60) / stepMinutes

    var maxTide = -999.0
    var minTide = 999.0

    for (step in 0..totalSteps) {
      val hourFloat = (step * stepMinutes) / 60.0
      val tideH = calculateTideHeightAtHour(hourFloat, harmonics)
      val totalWaterDepth = chartedDepth + tideH
      val availableUkc = totalWaterDepth - (actualDraft + squat)
      val isSafe = totalWaterDepth >= totalRequiredDepth

      if (tideH > maxTide) maxTide = tideH
      if (tideH < minTide) minTide = tideH

      val hours = hourFloat.toInt()
      val mins = ((hourFloat - hours) * 60.0).roundToInt().coerceIn(0, 59)
      val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hours % 24, mins)

      curvePoints.add(
        TideHeightPoint(
          hourOfDay = hourFloat,
          timeFormatted = timeFormatted,
          tideHeight = tideH,
          totalWaterDepth = round(totalWaterDepth * 100.0) / 100.0,
          availableUkc = round(availableUkc * 100.0) / 100.0,
          isSafe = isSafe
        )
      )
    }

    // Ekstremleri bul (HW / LW)
    val extrema = findExtrema(curvePoints, chartedDepth)

    // Güvenli Giriş-Çıkış Zaman Pencerelerini Çıkart
    val safeWindows = extractSafeWindows(curvePoints, totalRequiredDepth)

    // Rule of Twelfths (12'ler Kuralı Tablosu)
    val ruleOfTwelfths = calculateRuleOfTwelfths(extrema)

    // Anlık Durum
    val currentCal = Calendar.getInstance()
    val currentHourFloat = currentCal.get(Calendar.HOUR_OF_DAY) + (currentCal.get(Calendar.MINUTE) / 60.0)
    val currentTideH = calculateTideHeightAtHour(currentHourFloat, harmonics)
    val currentTotalDepth = round((chartedDepth + currentTideH) * 100.0) / 100.0
    val currentInstantUkc = round((currentTotalDepth - (actualDraft + squat)) * 100.0) / 100.0
    val isCurrentlySafe = currentTotalDepth >= totalRequiredDepth

    // Anlık Akıntı & Rüzgar Hesaplamaları
    val currentInfo = calculateMarineCurrent(currentHourFloat, harmonics, baseRange, springFactor, customLat, customLon)
    val windInfo = calculateMarineWind(customLat, customLon, selectedCalendar)
    val headingCardinal = calculateHeadingCardinal(headingDegrees)

    // Tavsiye ve Durum Başlığı
    val (advisoryBadge, advisorySummary) = generateAdvisory(
      isCurrentlySafe = isCurrentlySafe,
      safeWindows = safeWindows,
      maxTide = maxTide,
      chartedDepth = chartedDepth,
      totalRequiredDepth = totalRequiredDepth,
      actualDraft = actualDraft,
      minUkc = minUkc,
      squat = squat
    )

    // Emniyet Kontrol Listesi
    val checklist = listOf(
      "Gemi Statik Draftı (${actualDraft}m)" to true,
      "Dinamik Squat (Çökelme) Payı (${squat}m @ ${speedKnots} kts)" to true,
      "Minimum UKC Güvenlik Payı (${minUkc}m)" to true,
      "Mevki Harita Derinliği (CD): ${chartedDepth}m" to true,
      "Anlık Gelgit Yüksekliği: +${currentTideH}m (Anlık Toplam Su: ${currentTotalDepth}m)" to true,
      "Anlık Net UKC Payı: ${currentInstantUkc}m" to (currentInstantUkc >= minUkc),
      "Toplam Gerekli Su Derinliği: ${round(totalRequiredDepth * 100.0) / 100.0}m" to (maxTide + chartedDepth >= totalRequiredDepth),
      if (safeWindows.isNotEmpty()) "Güvenli Geçiş Penceresi Mevcut (${safeWindows.size} pencere)" to true
      else "24 Saat İçinde Yeterli Güvenlik Payı Bulunamadı!" to false
    )

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))

    return NavigationAnalysis(
      vessel = vessel,
      location = location,
      customLat = customLat,
      customLon = customLon,
      chartedDepthMeters = chartedDepth,
      actualDraftMeters = actualDraft,
      minUkcMeters = minUkc,
      vesselSpeedKnots = speedKnots,
      vesselHeadingDegrees = headingDegrees,
      vesselHeadingCardinal = headingCardinal,
      calculatedSquatMeters = squat,
      totalRequiredDepthMeters = round(totalRequiredDepth * 100.0) / 100.0,
      currentInstantTideHeightMeters = round(currentTideH * 100.0) / 100.0,
      currentInstantTotalDepthMeters = currentTotalDepth,
      currentInstantUkcMeters = currentInstantUkc,
      currentInfo = currentInfo,
      windInfo = windInfo,
      selectedDateFormatted = dateFormat.format(selectedCalendar.time),
      moonPhaseName = "${realMoon.phaseName} (Yaş: ${realMoon.moonAgeDays} gün)",
      moonIlluminationPercent = realMoon.illuminationPercent,
      isSpringTide = isSpringTide,
      realMoonInfo = realMoon,
      maxTideHeight24h = round(maxTide * 100.0) / 100.0,
      minTideHeight24h = round(minTide * 100.0) / 100.0,
      maxAvailableDepth24h = round((chartedDepth + maxTide) * 100.0) / 100.0,
      minAvailableDepth24h = round((chartedDepth + minTide) * 100.0) / 100.0,
      safeWindows = safeWindows,
      extrema = extrema,
      curvePoints = curvePoints,
      ruleOfTwelfths = ruleOfTwelfths,
      currentHour = currentHourFloat,
      isCurrentlySafe = isCurrentlySafe,
      advisoryBadge = advisoryBadge,
      advisorySummary = advisorySummary,
      safetyChecklist = checklist
    )
  }

  fun calculateHeadingCardinal(degrees: Int): String {
    val normalized = ((degrees % 360) + 360) % 360
    return when (normalized) {
      in 349..360, in 0..11 -> "K / Kuzey (000°)"
      in 12..33 -> "KKD / Kuzey-Kuzeydoğu (022°)"
      in 34..56 -> "KD / Kuzeydoğu (045°)"
      in 57..78 -> "DKD / Doğu-Kuzeydoğu (067°)"
      in 79..101 -> "D / Doğu (090°)"
      in 102..123 -> "DGD / Doğu-Güneydoğu (112°)"
      in 124..146 -> "GD / Güneydoğu (135°)"
      in 147..168 -> "GGD / Güney-Güneydoğu (157°)"
      in 169..191 -> "G / Güney (180°)"
      in 192..213 -> "GGB / Güney-Güneybatı (202°)"
      in 214..236 -> "GB / Güneybatı (225°)"
      in 237..258 -> "BGB / Batı-Güneybatı (247°)"
      in 259..281 -> "B / Batı (270°)"
      in 282..303 -> "BKB / Batı-Kuzeybatı (292°)"
      in 304..326 -> "KB / Kuzeybatı (315°)"
      else -> "KKB / Kuzey-Kuzeybatı (337°)"
    }
  }

  private fun calculateMarineCurrent(
    hourFloat: Double,
    harmonics: TideHarmonics,
    baseRange: Double,
    springFactor: Double,
    lat: Double,
    lon: Double
  ): MarineCurrentInfo {
    // 1. Türev (rate of water level change dh/dt) ile gelgit akıntısı hesabı
    val dt = 0.1
    val h1 = calculateTideHeightAtHour(hourFloat, harmonics)
    val h2 = calculateTideHeightAtHour(hourFloat + dt, harmonics)
    val rate = (h2 - h1) / dt // m/saat

    // 2. GPS Koordinat Bölgesel Akıntı Rejimi
    val isBosphorus = lat in 40.9..41.3 && lon in 28.9..29.3
    val isDardanelles = lat in 40.0..40.5 && lon in 26.1..26.6
    val isAegean = lat in 35.5..40.8 && lon in 23.0..28.5
    val isMarmara = lat in 40.4..41.1 && lon in 26.7..29.9
    val isBlackSea = lat in 41.2..46.8 && lon in 27.5..41.8
    val isGibraltar = lat in 35.8..36.2 && lon in -6.0..-5.2
    val isEnglishChannel = lat in 49.0..51.2 && lon in -5.0..2.0

    val maxCurrentKnots = when {
      isBosphorus -> (3.2 * springFactor).coerceIn(1.8, 5.0) // Boğaz üst akıntısı
      isDardanelles -> (2.6 * springFactor).coerceIn(1.2, 4.2) // Çanakkale akıntısı
      isEnglishChannel -> (3.5 * springFactor).coerceIn(1.5, 4.8)
      isGibraltar -> (2.8 * springFactor).coerceIn(1.2, 3.8)
      else -> (baseRange * 0.45 * springFactor).coerceIn(0.4, 4.8)
    }

    val dynamicCurrentSpeed = (abs(rate) * 1.5 * springFactor).coerceIn(0.1, maxCurrentKnots)
    val currentSpeed = if (isBosphorus || isDardanelles) {
      // Boğazlarda sürekli bir yüzey akıntısı bileşeni vardır (Karadeniz'den Marmara/Ege'ye)
      (dynamicCurrentSpeed + 1.2).coerceIn(0.8, maxCurrentKnots)
    } else {
      dynamicCurrentSpeed
    }
    val speedRounded = round(currentSpeed * 10.0) / 10.0

    // GPS konumuna göre akıntı yönü açısı (000° - 359°)
    val baseDir = when {
      isBosphorus -> 205 // Boğaziçi: Karadeniz'den Marmara'ya Güneybatı/SSW
      isDardanelles -> 220 // Çanakkale: Ege'ye doğru Güneybatı/SW
      isAegean -> 195 // Ege genelinde güneye doğru yüzey akıntısı
      isMarmara -> 235 // Marmara genel akıntısı
      isBlackSea -> 270 // Karadeniz siklonik akıntısı (Batıya doğru)
      isGibraltar -> 85 // Cebelitarık: Atlantik'ten Akdeniz'e Doğu akıntısı
      isEnglishChannel -> if (rate > 0) 65 else 245 // Kanal: Taşkın KD, Çekilme GB
      else -> ((lat * 13.7 + lon * 21.3).toInt() % 360 + 360) % 360
    }

    return when {
      abs(rate) < 0.06 && !isBosphorus && !isDardanelles -> {
        MarineCurrentInfo(
          directionDegrees = baseDir,
          directionCardinal = calculateHeadingCardinal(baseDir),
          speedKnots = 0.2,
          phaseName = "Ölü Su (Slack Water)",
          summary = "Durgun su fazı. GPS konumunda akıntı hızı 0.2 knot civarında, yanaşma/ayrılma manevrası için en uygun periyot."
        )
      }
      rate > 0 -> {
        // Taşkın (Flood)
        val floodDir = if (isBosphorus || isDardanelles) baseDir else (baseDir + 45) % 360
        MarineCurrentInfo(
          directionDegrees = floodDir,
          directionCardinal = calculateHeadingCardinal(floodDir),
          speedKnots = speedRounded,
          phaseName = "Taşkın Akıntısı (Flood Stream)",
          summary = "Sular yükseliyor. GPS koordinatında kanala/kıyıya doğru $speedRounded kts hızında taşkın akıntısı etkindir."
        )
      }
      else -> {
        // Çekilme (Ebb)
        val ebbDir = if (isBosphorus || isDardanelles) baseDir else (baseDir + 225) % 360
        MarineCurrentInfo(
          directionDegrees = ebbDir,
          directionCardinal = calculateHeadingCardinal(ebbDir),
          speedKnots = speedRounded,
          phaseName = "Çekilme Akıntısı (Ebb Stream)",
          summary = "Sular alçalıyor. GPS koordinatında açık denize doğru $speedRounded kts hızında çekilme akıntısı etkindir."
        )
      }
    }
  }

  private fun calculateMarineWind(lat: Double, lon: Double, cal: Calendar): MarineWindInfo {
    val day = cal.get(Calendar.DAY_OF_YEAR)
    val hour = cal.get(Calendar.HOUR_OF_DAY)

    // Coğrafi GPS bölgesine göre hakim deniz rüzgarı modelleri
    val (windDir, baseSpeedKnots) = when {
      // İstanbul Boğazı & Marmara: Hakim rüzgar Poyraz (035° - 050°) veya Lodos (210° - 230°)
      lat in 40.4..41.4 && lon in 26.7..29.9 -> {
        val isLodos = (day % 7) == 0
        if (isLodos) Pair(215 + (hour % 15), 16.0 + (day % 10))
        else Pair(40 + (day % 20), 14.0 + (day % 8))
      }
      // Çanakkale & Kuzey Ege: Poyraz / Kuzey-Kuzeydoğu
      lat in 39.0..40.5 && lon in 25.0..27.5 -> Pair(35 + (day % 25), 15.0 + (day % 9))
      // Ege Denizi: Yazın Meltemi (Kuzey/Kuzeybatı 340° - 010°), Kışın Poyraz/Lodos
      lat in 35.5..39.0 && lon in 23.0..28.5 -> {
        val meltemMonth = cal.get(Calendar.MONTH) + 1 in 6..9
        if (meltemMonth) Pair(350 + (day % 25) % 360, 18.0 + (day % 11))
        else Pair(45 + (day % 40), 13.0 + (day % 10))
      }
      // Karadeniz: Poyraz (045°) ve Yıldız (000°)
      lat in 41.2..46.8 && lon in 27.5..41.8 -> Pair(20 + (day % 35), 14.0 + (day % 12))
      // Akdeniz: Batı / Karayel (260° - 310°)
      lat in 34.0..37.0 && lon in 28.0..36.0 -> Pair(275 + (day % 30), 12.0 + (day % 8))
      // Kuzeybatı Avrupa & Manş Denizi (Westerlies 230° - 260°)
      lat in 48.0..60.0 && lon in -10.0..10.0 -> Pair(245 + (day % 30), 17.0 + (day % 14))
      // Cebelitarık & Atlantik
      lat in 34.0..45.0 && lon in -20.0..-5.0 -> Pair(80 + (day % 40), 15.0 + (day % 10))
      // Dünya Geneli GPS Fallback
      else -> {
        val latFactor = ((lat.toInt() * 11) % 360 + 360) % 360
        val speed = 10.0 + (abs(lat) * 0.25).coerceIn(2.0, 15.0) + (day % 7)
        Pair(latFactor, speed)
      }
    }

    val windSpeedKnots = baseSpeedKnots.coerceIn(4.0, 38.0)
    val (bft, bftDesc, seaState) = when {
      windSpeedKnots < 4 -> Triple(1, "Hafif Esinti (Light Air)", "Sakin, ayna gibi deniz")
      windSpeedKnots in 4.0..10.0 -> Triple(2, "Tatlı Rüzgar (Light Breeze)", "Küçük kırışıklıklar")
      windSpeedKnots in 11.0..16.0 -> Triple(4, "Orta Rüzgar (Moderate Breeze)", "Küçük dalgalar, çatlayan beyaz köpükler")
      windSpeedKnots in 17.0..21.0 -> Triple(5, "Ferah Rüzgar (Fresh Breeze)", "Orta boy dalgalar, sık beyaz köpükler")
      windSpeedKnots in 22.0..27.0 -> Triple(6, "Kuvvetli Rüzgar (Strong Breeze)", "Büyük dalgalar, serpinti")
      windSpeedKnots in 28.0..33.0 -> Triple(7, "Sert Rüzgar (Near Gale)", "Köpük çizgileri, belirgin serpinti")
      else -> Triple(8, "Fırtınamsı Rüzgar (Gale)", "Yüksek dalga sırtları, savrulan serpinti")
    }

    val roundedSpeed = round(windSpeedKnots * 10.0) / 10.0

    return MarineWindInfo(
      directionDegrees = windDir % 360,
      directionCardinal = calculateHeadingCardinal(windDir % 360),
      speedKnots = roundedSpeed,
      beaufortScale = bft,
      beaufortDescription = bftDesc,
      seaStateDescription = seaState
    )
  }

  private fun findExtrema(points: List<TideHeightPoint>, chartedDepth: Double): List<TideExtremum> {
    val extrema = mutableListOf<TideExtremum>()
    if (points.size < 3) return extrema

    for (i in 1 until points.size - 1) {
      val prev = points[i - 1].tideHeight
      val curr = points[i].tideHeight
      val next = points[i + 1].tideHeight

      if (curr > prev && curr >= next) {
        // High Water (HW)
        extrema.add(
          TideExtremum(
            type = ExtremumType.HIGH_WATER,
            hourOfDay = points[i].hourOfDay,
            timeFormatted = points[i].timeFormatted,
            tideHeightMeters = curr,
            totalWaterDepthMeters = round((chartedDepth + curr) * 100.0) / 100.0
          )
        )
      } else if (curr < prev && curr <= next) {
        // Low Water (LW)
        extrema.add(
          TideExtremum(
            type = ExtremumType.LOW_WATER,
            hourOfDay = points[i].hourOfDay,
            timeFormatted = points[i].timeFormatted,
            tideHeightMeters = curr,
            totalWaterDepthMeters = round((chartedDepth + curr) * 100.0) / 100.0
          )
        )
      }
    }
    return extrema
  }

  private fun extractSafeWindows(
    points: List<TideHeightPoint>,
    totalRequiredDepth: Double
  ): List<SafeNavigationWindow> {
    val windows = mutableListOf<SafeNavigationWindow>()
    var inWindow = false
    var windowStartIndex = 0
    var windowId = 1

    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY) + (currentCal.get(Calendar.MINUTE) / 60.0)

    for (i in points.indices) {
      val isSafe = points[i].isSafe

      if (isSafe && !inWindow) {
        inWindow = true
        windowStartIndex = i
      } else if (!isSafe && inWindow) {
        inWindow = false
        val windowPoints = points.subList(windowStartIndex, i)
        val window = buildWindow(windowId++, windowPoints, currentHour)
        windows.add(window)
      }
    }

    if (inWindow) {
      val windowPoints = points.subList(windowStartIndex, points.size)
      val window = buildWindow(windowId++, windowPoints, currentHour)
      windows.add(window)
    }

    return windows
  }

  private fun buildWindow(
    id: Int,
    points: List<TideHeightPoint>,
    currentHour: Double
  ): SafeNavigationWindow {
    val first = points.first()
    val last = points.last()

    var maxTide = -999.0
    var maxPoint = first
    var minUkc = 999.0
    var maxUkc = -999.0

    for (p in points) {
      if (p.tideHeight > maxTide) {
        maxTide = p.tideHeight
        maxPoint = p
      }
      if (p.availableUkc < minUkc) minUkc = p.availableUkc
      if (p.availableUkc > maxUkc) maxUkc = p.availableUkc
    }

    val durationMin = ((last.hourOfDay - first.hourOfDay) * 60.0).roundToInt()
    val isInside = currentHour >= first.hourOfDay && currentHour <= last.hourOfDay

    val rating = when {
      minUkc >= 1.5 && durationMin >= 180 -> WindowSafetyRating.OPTIMAL
      minUkc >= 0.8 && durationMin >= 90 -> WindowSafetyRating.SUFFICIENT
      else -> WindowSafetyRating.MARGINAL
    }

    return SafeNavigationWindow(
      id = id,
      startHour = first.hourOfDay,
      endHour = last.hourOfDay,
      startTimeFormatted = first.timeFormatted,
      endTimeFormatted = last.timeFormatted,
      durationMinutes = durationMin,
      peakHour = maxPoint.hourOfDay,
      peakTimeFormatted = maxPoint.timeFormatted,
      maxTideHeightMeters = maxPoint.tideHeight,
      maxWaterDepthMeters = maxPoint.totalWaterDepth,
      maxUkcMeters = round(maxUkc * 100.0) / 100.0,
      minUkcMeters = round(minUkc * 100.0) / 100.0,
      rating = rating,
      isCurrentTimeInside = isInside
    )
  }

  private fun calculateRuleOfTwelfths(extrema: List<TideExtremum>): List<RuleOfTwelfthStep> {
    val steps = mutableListOf<RuleOfTwelfthStep>()
    val lw = extrema.firstOrNull { it.type == ExtremumType.LOW_WATER }
    val hw = extrema.firstOrNull { it.type == ExtremumType.HIGH_WATER && it.hourOfDay > (lw?.hourOfDay ?: -1.0) }

    if (lw == null || hw == null) {
      // Sentetik 6 saatlik kural örneği
      val range = 4.0
      val fractions = listOf(1.0/12.0, 2.0/12.0, 3.0/12.0, 3.0/12.0, 2.0/12.0, 1.0/12.0)
      val labels = listOf("1/12", "2/12", "3/12", "3/12", "2/12", "1/12")
      var cum = 0.0

      for (i in 0 until 6) {
        val frac = fractions[i]
        val rise = range * frac
        cum += rise
        steps.add(
          RuleOfTwelfthStep(
            stepIndex = i + 1,
            timeRangeFormatted = "+${i}s - +${i+1}s",
            fractionLabel = labels[i],
            fractionValue = frac,
            intervalRiseMeters = round(rise * 100.0) / 100.0,
            cumulativeHeightMeters = round(cum * 100.0) / 100.0
          )
        )
      }
      return steps
    }

    val range = (hw.tideHeightMeters - lw.tideHeightMeters).coerceAtLeast(0.1)
    val durationHours = (hw.hourOfDay - lw.hourOfDay).coerceAtLeast(1.0)
    val hourStep = durationHours / 6.0

    val fractions = listOf(1.0/12.0, 2.0/12.0, 3.0/12.0, 3.0/12.0, 2.0/12.0, 1.0/12.0)
    val labels = listOf("1/12", "2/12", "3/12", "3/12", "2/12", "1/12")
    var cumulative = lw.tideHeightMeters

    for (i in 0 until 6) {
      val t1 = lw.hourOfDay + (i * hourStep)
      val t2 = lw.hourOfDay + ((i + 1) * hourStep)

      val h1 = t1.toInt()
      val m1 = ((t1 - h1) * 60.0).roundToInt().coerceIn(0, 59)
      val h2 = t2.toInt()
      val m2 = ((t2 - h2) * 60.0).roundToInt().coerceIn(0, 59)

      val timeStr = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", h1 % 24, m1, h2 % 24, m2)
      val rise = range * fractions[i]
      cumulative += rise

      steps.add(
        RuleOfTwelfthStep(
          stepIndex = i + 1,
          timeRangeFormatted = timeStr,
          fractionLabel = labels[i],
          fractionValue = fractions[i],
          intervalRiseMeters = round(rise * 100.0) / 100.0,
          cumulativeHeightMeters = round(cumulative * 100.0) / 100.0
        )
      )
    }

    return steps
  }

  private fun generateAdvisory(
    isCurrentlySafe: Boolean,
    safeWindows: List<SafeNavigationWindow>,
    maxTide: Double,
    chartedDepth: Double,
    totalRequiredDepth: Double,
    actualDraft: Double,
    minUkc: Double,
    squat: Double
  ): Pair<String, String> {
    val maxPossibleDepth = chartedDepth + maxTide

    if (maxPossibleDepth < totalRequiredDepth) {
      val deficit = round((totalRequiredDepth - maxPossibleDepth) * 100.0) / 100.0
      return "GEÇİŞ RİSKLİ / DERİNLİK YETERSİZ" to
        "Mevcut draft ($actualDraft m), squat ($squat m) ve UKC payı ($minUkc m) ile toplam $totalRequiredDepth m derinlik gereklidir. Günün en yüksek gelgitinde dahi su derinliği $maxPossibleDepth m olup $deficit m yetersiz kalmaktadır. Giriş/çıkış yapılamaz veya draft hafifletilmelidir."
    }

    if (safeWindows.isEmpty()) {
      return "GÜVENLİ PENCERE YOK" to
        "Gün içinde hesaplanan su seviyeleri belirlenen minimum güvenlik payı için yetersizdir."
    }

    val bestWindow = safeWindows.maxByOrNull { it.maxUkcMeters } ?: safeWindows.first()

    return if (isCurrentlySafe) {
      "ŞU AN GÜVENLİ / GEÇİŞE UYGUN" to
        "Şu an su seviyesi güvenli geçiş limitleri dahilindedir. En elverişli geçiş aralığı: ${bestWindow.startTimeFormatted} - ${bestWindow.endTimeFormatted} (Pik UKC: +${bestWindow.maxUkcMeters}m)."
    } else {
      "BEKLEMEDE / GELGİT YÜKSELMESİ BEKLENİYOR" to
        "Mevcut saatte su seviyesi riskli sınırdadır. Önerilen ilk güvenli giriş-çıkış penceresi: ${bestWindow.startTimeFormatted} - ${bestWindow.endTimeFormatted} (Süre: ${bestWindow.durationMinutes} dk, Maks Derinlik: ${bestWindow.maxWaterDepthMeters}m)."
    }
  }
}
