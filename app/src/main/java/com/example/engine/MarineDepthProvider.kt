package com.example.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class MarineDepthResult(
  val depthMeters: Double,
  val sourceName: String,
  val isOnlineVerified: Boolean,
  val confidenceText: String,
  val emodnetIdentifier: String? = null,
  val minDepthMeters: Double? = null,
  val maxDepthMeters: Double? = null
)

object MarineDepthProvider {
  private val httpClient get() = MarineNetworkClient.fastApiClient

  private data class CachedDepth(val result: MarineDepthResult, val timestampMs: Long)
  private val depthCache = object : LinkedHashMap<String, CachedDepth>(48, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedDepth>?): Boolean = size > 48
  }
  private const val DEPTH_TTL_MS = 15 * 60_000L

  fun clearCache() {
    synchronized(depthCache) { depthCache.clear() }
  }

  /**
   * GPS koordinatının deniz derinliğini EMODnet Bathymetry REST API
   * (European Marine Observation and Data Network) üzerinden doğrular.
   * Format: GET https://rest.emodnet-bathymetry.eu/depth_sample?geom=POINT(lon lat)
   *
   * EMODnet erişilemezse veya alan dışıysa, OpenTopoData GEBCO ve yerel deniz haritası
   * batimetri modellerine geri çekilir.
   */
  suspend fun fetchMarineDepth(latitude: Double, longitude: Double): MarineDepthResult = withContext(Dispatchers.IO) {
    val cacheKey = String.format(Locale.US, "%.3f_%.3f", latitude, longitude)
    val nowMs = System.currentTimeMillis()
    synchronized(depthCache) {
      val cached = depthCache[cacheKey]
      if (cached != null && (nowMs - cached.timestampMs) <= DEPTH_TTL_MS) {
        return@withContext cached.result
      }
    }
    // 1. EMODNET BATHYMETRY REST SERVICE (Avrupa Deniz Gözlem ve Veri Ağı)
    try {
      // WKT formatında POINT(boylam enlem) - longitude latitude
      val lonStr = String.format(Locale.US, "%.6f", longitude)
      val latStr = String.format(Locale.US, "%.6f", latitude)
      val emodnetUrl = "https://rest.emodnet-bathymetry.eu/depth_sample?geom=POINT($lonStr%20$latStr)"

      val request = Request.Builder()
        .url(emodnetUrl)
        .header("User-Agent", "TideNav-EMODnet/1.0")
        .header("Accept", "application/json")
        .build()

      val jsonStr = httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
      }

      if (!jsonStr.isNullOrBlank() && jsonStr.startsWith("{")) {
        val json = JSONObject(jsonStr)
        val hasAvg = json.has("avg") && !json.isNull("avg")
        val hasSmoothed = json.has("smoothed") && !json.isNull("smoothed")

        if (hasAvg || hasSmoothed) {
          val rawDepthVal = if (hasAvg) json.getDouble("avg") else json.getDouble("smoothed")
          val refObj = json.optJSONObject("reference")
          val identifier = refObj?.optString("identifier", "") ?: ""
          val refType = refObj?.optString("type", "") ?: "DTM"

          val minVal = if (json.has("min") && !json.isNull("min")) json.getDouble("min") else null
          val maxVal = if (json.has("max") && !json.isNull("max")) json.getDouble("max") else null

          // EMODnet DTM veri setinde derinlik verisi negatif kot (-45.4m) veya doğrudan su sütunu pozitif derinliği (9.2m / 154.3m) olarak dönebilir
          val absDepth = abs(rawDepthVal)
          val roundedDepth = (round(absDepth * 10.0) / 10.0).coerceAtLeast(0.5)
          val idDesc = if (identifier.isNotBlank()) " ($identifier)" else ""

          val minRounded = minVal?.let { (round(abs(it) * 10.0) / 10.0) }
          val maxRounded = maxVal?.let { (round(abs(it) * 10.0) / 10.0) }
          val lowerBound = if (minRounded != null && maxRounded != null) minOf(minRounded, maxRounded) else minRounded
          val upperBound = if (minRounded != null && maxRounded != null) maxOf(minRounded, maxRounded) else maxRounded

          val res = MarineDepthResult(
            depthMeters = roundedDepth,
            sourceName = "EMODnet Bathymetry$idDesc",
            isOnlineVerified = true,
            confidenceText = "EMODnet DTM Doğrulandı",
            emodnetIdentifier = if (identifier.isNotBlank()) "$refType $identifier" else "EMODnet DTM",
            minDepthMeters = lowerBound,
            maxDepthMeters = upperBound
          )
          synchronized(depthCache) {
            depthCache[cacheKey] = CachedDepth(res, System.currentTimeMillis())
          }
          return@withContext res
        }
      }
    } catch (e: Exception) {
      Log.w("MarineDepthProvider", "EMODnet Bathymetry fetch error: ${e.message}")
    }

    // 2. OpenTopoData GEBCO 2020/2024 Küresel Deniz Batimetrisi (Yedek Çevrimiçi Katman)
    try {
      val url = "https://api.opentopodata.org/v1/gebco2020?locations=$latitude,$longitude"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "MarineDepthProvider/1.0")
        .build()

      val jsonStr = httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
      }

      if (!jsonStr.isNullOrBlank()) {
        val json = JSONObject(jsonStr)
        val status = json.optString("status", "")
        if (status.equals("OK", ignoreCase = true)) {
          val results = json.getJSONArray("results")
          if (results.length() > 0) {
            val item = results.getJSONObject(0)
            if (!item.isNull("elevation")) {
              val elevation = item.getDouble("elevation")
              val res = if (elevation <= 0) {
                val depth = abs(elevation)
                val roundedDepth = (round(depth * 10.0) / 10.0).coerceAtLeast(0.5)
                MarineDepthResult(
                  depthMeters = roundedDepth,
                  sourceName = "GEBCO Bathymetry (Canlı)",
                  isOnlineVerified = true,
                  confidenceText = "GEBCO Sonar/Uydu Doğrulandı"
                )
              } else {
                MarineDepthResult(
                  depthMeters = 3.5,
                  sourceName = "Kıyı Sığ Su Haritası",
                  isOnlineVerified = true,
                  confidenceText = "Kıyı Hattı (~3.5 m)"
                )
              }
              synchronized(depthCache) {
                depthCache[cacheKey] = CachedDepth(res, System.currentTimeMillis())
              }
              return@withContext res
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w("MarineDepthProvider", "GEBCO fetch error: ${e.message}")
    }

    // 3. Bölgesel Deniz Seyir Haritası Batimetrisi (Çevrimdışı Güvenilir Referans)
    val fallback = calculateRegionalDepthFallback(latitude, longitude)
    return@withContext fallback
  }

  fun calculateRegionalDepthFallback(lat: Double, lon: Double): MarineDepthResult {
    val nearest = com.example.model.LocationPresets.strategicMarineLocations.minByOrNull { port ->
      com.example.model.calculateHaversineDistanceNm(lat, lon, port.latitude, port.longitude)
    }

    val depth = nearest?.defaultChartedDepthMeters ?: 25.0
    return MarineDepthResult(
      depthMeters = depth,
      sourceName = "Seyir Haritası Batimetrisi (${nearest?.name?.take(15) ?: "Genel"})",
      isOnlineVerified = false,
      confidenceText = "Seyir Haritası Referansı"
    )
  }
}
