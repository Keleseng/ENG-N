package com.example.model

data class MarineWeather(
  val latitude: Double,
  val longitude: Double,
  val temperatureC: Double, // Hava Sıcaklığı (°C)
  val relativeHumidityPercent: Int, // Bağıl Nem (%)
  val surfacePressureHpa: Double, // Yüzey Basıncı (hPa / mbar)
  val windSpeedKnots: Double, // Rüzgar Hızı (knot / kts)
  val windDirectionDegrees: Int, // Rüzgar Yönü (derece 0-360)
  val windDirectionCardinal: String, // N, NE, E, SE, S, SW, W, NW vb.
  val windGustsKnots: Double, // Rüzgar Hamlesi (knot)
  val waveHeightMeters: Double, // Deniz Dalga Yüksekliği (metre)
  val wavePeriodSeconds: Double, // Dalga Periyodu (saniye)
  val waveDirectionDegrees: Int, // Dalga Yönü (derece)
  val oceanCurrentSpeedKnots: Double = 0.0, // Akıntı Sürati (knot) - Copernicus Marine
  val oceanCurrentDirectionDegrees: Int = 0, // Akıntı Yönü (derece 0-360) - Copernicus Marine
  val precipitationMm: Double, // Yağış Durumu (mm / saat)
  val precipitationStateText: String, // "Yağış Yok", "Hafif Yağmur", "Sağanak", vb.
  val weatherCode: Int, // WMO Weather Code
  val weatherConditionDescription: String, // "Açık", "Parçalı Bulutlu", "Deniz Çalkantılı" vb.
  val beaufortScale: Int, // 0 - 12 Beaufort Rüzgar Skalası
  val beaufortDescription: String, // "Meltem", "Fırtınamsı Rüzgar", vb.
  val seaStateDescription: String, // Douglas Deniz Durumu: "Sakin", "Hafif Dalgalı", "Kaba Dalgalı" vb.
  val lastUpdatedFormatted: String,
  val isLiveFromNetwork: Boolean = false,
  val windyEmbedUrl: String,
  val sunTimes: com.example.engine.SunTimesInfo? = null
)

data class SpeedCalculationResult(
  val gpsSpeedKnots: Double, // GPS Sürati (SOG - Speed Over Ground)
  val isGpsActive: Boolean,
  val calculatedGroundSpeedKnots: Double, // Yere Göre Hesaplanmış Sürat (V_ground = V_water + V_current + V_leeway)
  val speedThroughWaterKnots: Double, // Suya Göre Sürat (STW / Kütük Sürati)
  val vesselHeadingDegrees: Int, // Gemi Pruva / Rota Açısı (Heading)
  val groundCourseDegrees: Int, // Gidilen Yere Göre Rota (COG - Course Over Ground)
  val currentSpeedKnots: Double, // Akıntı Hızı
  val currentDirectionDegrees: Int, // Akıntı Yönü (Set)
  val windDriftAngleDegrees: Double, // Rüzgar Sürüklenme Açısı (Leeway Angle)
  val deltaSpeedKnots: Double, // SOG - STW Hız Farkı (+ Akıntı Yardımı, - Akıntı Direnci)
  val speedEvaluationText: String, // "Akıntı lehte (+1.2 kn avantaj)", "Karşı akıntı (-0.8 kn direnç)"
  val driftStatusText: String
)
