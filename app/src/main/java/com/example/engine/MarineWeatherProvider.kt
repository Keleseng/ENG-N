package com.example.engine

import android.util.Log
import com.example.model.MarineWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

class MarineWeatherProvider {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .build()

  /**
   * GPS veya Liman Koordinatlarına Göre Canlı Deniz & Hava Durumu Verilerini Alır
   */
  suspend fun fetchMarineWeather(latitude: Double, longitude: Double): MarineWeather = withContext(Dispatchers.IO) {
    try {
      // 1. Open-Meteo Hava Durumu (Sıcaklık, Nem, Basınç, Rüzgar, Hamle, Yağış)
      val weatherUrl = "https://api.open-meteo.com/v1/forecast?" +
          "latitude=$latitude&longitude=$longitude" +
          "&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,precipitation,weather_code" +
          "&wind_speed_unit=kn&timezone=auto"

      val weatherRequest = Request.Builder().url(weatherUrl).build()
      val weatherResponse = httpClient.newCall(weatherRequest).execute()
      val weatherJsonStr = weatherResponse.body?.string()

      // 2. Open-Meteo & Copernicus Marine (CMEMS) Deniz Durumu & Akıntı
      var waveHeight = 0.8
      var wavePeriod = 4.5
      var waveDir = 0
      var oceanCurrentSpeed = 0.8
      var oceanCurrentDir = 225
      var marineSuccess = false

      try {
        val marineUrl = "https://marine-api.open-meteo.com/v1/marine?" +
            "latitude=$latitude&longitude=$longitude" +
            "&current=wave_height,wave_direction,wave_period,ocean_current_velocity,ocean_current_direction" +
            "&timezone=auto"

        val marineRequest = Request.Builder().url(marineUrl).build()
        val marineResponse = httpClient.newCall(marineRequest).execute()
        if (marineResponse.isSuccessful) {
          val marineJson = JSONObject(marineResponse.body?.string() ?: "{}")
          val currentMarine = marineJson.optJSONObject("current")
          if (currentMarine != null) {
            val wh = currentMarine.optDouble("wave_height", Double.NaN)
            val wp = currentMarine.optDouble("wave_period", Double.NaN)
            val wd = currentMarine.optInt("wave_direction", -1)
            val ocv = currentMarine.optDouble("ocean_current_velocity", Double.NaN)
            val ocd = currentMarine.optInt("ocean_current_direction", -1)

            if (!wh.isNaN()) waveHeight = wh
            if (!wp.isNaN()) wavePeriod = wp
            if (wd >= 0) waveDir = wd
            if (!ocv.isNaN()) {
              // Open-Meteo current velocity km/h -> knot dönüşümü (1 km/h = 0.539957 kn)
              oceanCurrentSpeed = round(ocv * 0.539957 * 10.0) / 10.0
            }
            if (ocd >= 0) oceanCurrentDir = ocd
            marineSuccess = true
          }
        }
      } catch (e: Exception) {
        Log.w("MarineWeather", "Copernicus Marine API call skipped or internal sea, using wave-current model: ${e.message}")
      }

      if (weatherResponse.isSuccessful && !weatherJsonStr.isNullOrBlank()) {
        val json = JSONObject(weatherJsonStr)
        val current = json.getJSONObject("current")

        val temp = current.optDouble("temperature_2m", 21.0)
        val humidity = current.optInt("relative_humidity_2m", 68)
        val pressure = current.optDouble("surface_pressure", 1013.2)
        val windSpeed = current.optDouble("wind_speed_10m", 12.0)
        val windDir = current.optInt("wind_direction_10m", 210)
        val windGusts = current.optDouble("wind_gusts_10m", windSpeed * 1.35)
        val precip = current.optDouble("precipitation", 0.0)
        val wCode = current.optInt("weather_code", 0)

        // Eğer marine API kara/iç su nedeniyle null döndüyse rüzgar hızından tahmini dalga yüksekliği
        if (!marineSuccess) {
          waveHeight = calculateWaveHeightFromWind(windSpeed)
          wavePeriod = calculateWavePeriodFromWind(windSpeed)
          waveDir = windDir
        }

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val updatedTime = timeFormat.format(Date())

        val (beaufort, beaufortText) = getBeaufortScale(windSpeed)
        val seaStateText = getSeaStateDouglas(waveHeight)
        val precipText = getPrecipitationDescription(precip, wCode)
        val weatherText = getWeatherCodeDescription(wCode)
        val cardinal = degreesToCardinal(windDir)

        val windyUrl = generateWindyEmbedUrl(latitude, longitude)
        val sunInfo = SunCalculator.calculateSunTimes(latitude, longitude)

        return@withContext MarineWeather(
          latitude = latitude,
          longitude = longitude,
          temperatureC = round(temp * 10.0) / 10.0,
          relativeHumidityPercent = humidity,
          surfacePressureHpa = round(pressure * 10.0) / 10.0,
          windSpeedKnots = round(windSpeed * 10.0) / 10.0,
          windDirectionDegrees = windDir,
          windDirectionCardinal = cardinal,
          windGustsKnots = round(windGusts * 10.0) / 10.0,
          waveHeightMeters = round(waveHeight * 100.0) / 100.0,
          wavePeriodSeconds = round(wavePeriod * 10.0) / 10.0,
          waveDirectionDegrees = waveDir,
          oceanCurrentSpeedKnots = oceanCurrentSpeed,
          oceanCurrentDirectionDegrees = oceanCurrentDir,
          precipitationMm = round(precip * 10.0) / 10.0,
          precipitationStateText = precipText,
          weatherCode = wCode,
          weatherConditionDescription = weatherText,
          beaufortScale = beaufort,
          beaufortDescription = beaufortText,
          seaStateDescription = seaStateText,
          lastUpdatedFormatted = updatedTime,
          isLiveFromNetwork = true,
          windyEmbedUrl = windyUrl,
          sunTimes = sunInfo
        )
      }
    } catch (e: Exception) {
      Log.e("MarineWeather", "Network weather fetch failed, using realistic fallback", e)
    }

    // Fallback Realistic Marine Weather Calculation
    return@withContext generateFallbackMarineWeather(latitude, longitude)
  }

  private fun calculateWaveHeightFromWind(windKnots: Double): Double {
    // Sverdrup-Munk-Bretschneider (SMB) basitleştirilmiş dalga yüksekliği
    val ms = windKnots * 0.514444
    val h = 0.0246 * ms.pow(1.6)
    return max(0.2, min(8.0, h))
  }

  private fun calculateWavePeriodFromWind(windKnots: Double): Double {
    val ms = windKnots * 0.514444
    return max(3.0, min(12.0, 0.8 * ms + 2.0))
  }

  fun generateFallbackMarineWeather(latitude: Double, longitude: Double): MarineWeather {
    // Koordinata bağlı deterministik gerçekçi değerler
    val latMod = abs(latitude % 5.0)
    val lonMod = abs(longitude % 5.0)
    val temp = 18.0 + latMod * 1.5
    val humidity = (60 + (lonMod * 4).toInt()).coerceIn(40, 95)
    val pressure = 1012.5 + (sin(latitude) * 4.0)
    val windSpeed = 10.0 + latMod * 2.5
    val windDir = ((longitude * 35.0).toInt() % 360 + 360) % 360
    val windGusts = windSpeed * 1.4
    val waveHeight = calculateWaveHeightFromWind(windSpeed)
    val wavePeriod = calculateWavePeriodFromWind(windSpeed)
    val precip = 0.0

    val (beaufort, beaufortText) = getBeaufortScale(windSpeed)
    val seaStateText = getSeaStateDouglas(waveHeight)
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    return MarineWeather(
      latitude = latitude,
      longitude = longitude,
      temperatureC = round(temp * 10.0) / 10.0,
      relativeHumidityPercent = humidity,
      surfacePressureHpa = round(pressure * 10.0) / 10.0,
      windSpeedKnots = round(windSpeed * 10.0) / 10.0,
      windDirectionDegrees = windDir,
      windDirectionCardinal = degreesToCardinal(windDir),
      windGustsKnots = round(windGusts * 10.0) / 10.0,
      waveHeightMeters = round(waveHeight * 100.0) / 100.0,
      wavePeriodSeconds = round(wavePeriod * 10.0) / 10.0,
      waveDirectionDegrees = windDir,
      oceanCurrentSpeedKnots = round((0.8 + (latMod * 0.3)) * 10.0) / 10.0,
      oceanCurrentDirectionDegrees = ((longitude * 45.0).toInt() % 360 + 360) % 360,
      precipitationMm = precip,
      precipitationStateText = "Yağış Yok",
      weatherCode = 1,
      weatherConditionDescription = "Az Bulutlu / Açık",
      beaufortScale = beaufort,
      beaufortDescription = beaufortText,
      seaStateDescription = seaStateText,
      lastUpdatedFormatted = timeFormat.format(Date()),
      isLiveFromNetwork = false,
      windyEmbedUrl = generateWindyEmbedUrl(latitude, longitude),
      sunTimes = SunCalculator.calculateSunTimes(latitude, longitude)
    )
  }

  fun generateWindyEmbedUrl(lat: Double, lon: Double): String {
    return "https://embed.windy.com/embed2.html?lat=$lat&lon=$lon&zoom=9&level=surface&overlay=wind&menu=&message=true&marker=true&calendar=now&pressure=true&type=map&location=coordinates&detail=&metricWind=kt&metricTemp=%C2%B0C&radarRange=-1"
  }

  private fun degreesToCardinal(degrees: Int): String {
    val directions = arrayOf("K (N)", "KKD (NNE)", "KD (NE)", "DKD (ENE)", "D (E)", "DGD (ESE)", "GD (SE)", "GGD (SSE)", "G (S)", "GGB (SSW)", "GB (SW)", "BGB (WSW)", "B (W)", "BKB (WNW)", "KB (NW)", "KKB (NNW)")
    val index = (((degrees % 360) + 11.25) / 22.5).toInt() % 16
    return directions[index]
  }

  private fun getBeaufortScale(knots: Double): Pair<Int, String> {
    return when {
      knots < 1.0 -> 0 to "0 Bft - Sakin (Calm)"
      knots < 4.0 -> 1 to "1 Bft - Esinti (Light Air)"
      knots < 7.0 -> 2 to "2 Bft - Hafif Rüzgar (Light Breeze)"
      knots < 11.0 -> 3 to "3 Bft - Tatlı Rüzgar (Gentle Breeze)"
      knots < 17.0 -> 4 to "4 Bft - Orta Rüzgar (Moderate Breeze)"
      knots < 22.0 -> 5 to "5 Bft - Sert Rüzgar (Fresh Breeze)"
      knots < 28.0 -> 6 to "6 Bft - Kuvvetli Rüzgar (Strong Breeze)"
      knots < 34.0 -> 7 to "7 Bft - Fırtınamsı Rüzgar (Near Gale)"
      knots < 41.0 -> 8 to "8 Bft - Fırtına (Gale)"
      knots < 48.0 -> 9 to "9 Bft - Kuvvetli Fırtına (Strong Gale)"
      knots < 56.0 -> 10 to "10 Bft - Tam Fırtına (Storm)"
      knots < 64.0 -> 11 to "11 Bft - Çok Şiddetli Fırtına (Violent Storm)"
      else -> 12 to "12 Bft - Kasırga (Hurricane)"
    }
  }

  private fun getSeaStateDouglas(waveMeters: Double): String {
    return when {
      waveMeters < 0.1 -> "0 - Sütliman (Glassy)"
      waveMeters < 0.5 -> "1-2 - Sakin / Kırışık (Rippled)"
      waveMeters < 1.25 -> "3 - Hafif Dalgalı (Slight)"
      waveMeters < 2.5 -> "4 - Orta Dalgalı (Moderate)"
      waveMeters < 4.0 -> "5 - Kaba Dalgalı (Rough)"
      waveMeters < 6.0 -> "6 - Çok Kaba Dalgalı (Very Rough)"
      waveMeters < 9.0 -> "7 - Yüksek Dalgalı (High)"
      else -> "8-9 - Çok Yüksek / Çılgın Deniz (Phenomenal)"
    }
  }

  private fun getPrecipitationDescription(precipMm: Double, wCode: Int): String {
    return when {
      precipMm > 10.0 -> "Şiddetli Yağmur ($precipMm mm/h)"
      precipMm > 2.5 -> "Orta Yağış ($precipMm mm/h)"
      precipMm > 0.1 -> "Hafif Yağmur ($precipMm mm/h)"
      wCode in 51..67 -> "Çiseleme / Yağmur"
      wCode in 71..77 -> "Kar Yağışlı"
      wCode in 95..99 -> "Gök Gürültülü Fırtına"
      else -> "Yağış Yok (Kuru)"
    }
  }

  private fun getWeatherCodeDescription(code: Int): String {
    return when (code) {
      0 -> "Açık ve Güneşli"
      1, 2, 3 -> "Parçalı / Az Bulutlu"
      45, 48 -> "Sisli / Puslu (Görüş Kısıtlı)"
      51, 53, 55 -> "Hafif Çisenti"
      61, 63, 65 -> "Yağmurlu"
      71, 73, 75 -> "Kar Yağışı"
      80, 81, 82 -> "Sağanak Yağış"
      95, 96, 99 -> "Gök Gürültülü Fırtına"
      else -> "Normal Deniz Havası"
    }
  }
}
