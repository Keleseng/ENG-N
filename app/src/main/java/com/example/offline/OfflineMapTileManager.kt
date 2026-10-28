package com.example.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.engine.MarineLandPolygons
import com.example.model.Coordinate
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request

data class OfflineRegionPreset(
  val id: String,
  val name: String,
  val subtitle: String,
  val minLat: Double,
  val maxLat: Double,
  val minLon: Double,
  val maxLon: Double,
  val minZoom: Int,
  val maxZoom: Int
)

data class OfflineDownloadState(
  val isDownloading: Boolean = false,
  val taskTitle: String = "",
  val completedTiles: Int = 0,
  val totalTiles: Int = 0,
  val downloadedBytes: Long = 0L,
  val failedTiles: Int = 0,
  val cachedTileCount: Int = 0,
  val cachedSizeBytes: Long = 0L,
  val statusMessage: String? = null
)

private data class TileSpec(
  val layer: String,
  val z: Int,
  val x: Int,
  val y: Int
)

/**
 * Çevrimdışı (Offline) Harita Karosu İndirme, Önbellekleme ve WebView Kesme (Intercept) Yöneticisi.
 *
 * İnternet bağlantısı olmasa bile:
 * 1. Yerel olarak paketlenmiş Leaflet JS/CSS motorunu sunar.
 * 2. İndirilmiş veya daha önce görüntülenmiş harita karolarını diskten <1ms içinde sunar.
 * 3. İndirilen yakınlaştırma seviyesinin ötesine (Zoom 15-20) yaklaşıldığında üst karoyu kırparak (Overzoom) net görüntü üretir.
 * 4. Hiç indirilmemiş alanlarda dahi yerleşik kıyı poligonlarından vektörel deniz haritası karosu üretir.
 */
class OfflineMapTileManager private constructor(context: Context) {

  private val appContext = context.applicationContext
  private val rootTileDir = File(appContext.filesDir, "offline_map_tiles").apply { mkdirs() }
  private val prefs = appContext.getSharedPreferences("offline_map_prefs", Context.MODE_PRIVATE)
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  private val _forceOfflineMode = MutableStateFlow(prefs.getBoolean("force_offline_mode", false))
  val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

  private val _isDeviceOnline = MutableStateFlow(checkCurrentConnectivity())
  val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

  private val _downloadState = MutableStateFlow(OfflineDownloadState())
  val downloadState: StateFlow<OfflineDownloadState> = _downloadState.asStateFlow()

  private var activeDownloadJob: Job? = null

  private val transparent1x1Png: ByteArray by lazy {
    val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    out.toByteArray()
  }

  init {
    registerConnectivityMonitor()
    refreshStorageStatsAsync()
  }

  fun setForceOfflineMode(enabled: Boolean) {
    prefs.edit().putBoolean("force_offline_mode", enabled).apply()
    _forceOfflineMode.value = enabled
  }

  fun isEffectiveOffline(): Boolean {
    return _forceOfflineMode.value || !_isDeviceOnline.value
  }

  private fun checkCurrentConnectivity(): Boolean {
    return try {
      val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
      val net = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(net) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      false
    }
  }

  private fun registerConnectivityMonitor() {
    try {
      val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val req = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      cm.registerNetworkCallback(
        req,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            _isDeviceOnline.value = true
          }

          override fun onLost(network: Network) {
            _isDeviceOnline.value = checkCurrentConnectivity()
          }
        }
      )
    } catch (_: Exception) {}
  }

  fun refreshStorageStatsAsync() {
    managerScope.launch {
      var count = 0
      var bytes = 0L
      if (rootTileDir.exists()) {
        rootTileDir.walkTopDown().forEach { f ->
          if (f.isFile && f.name.endsWith(".tile")) {
            count++
            bytes += f.length()
          }
        }
      }
      _downloadState.update {
        it.copy(cachedTileCount = count, cachedSizeBytes = bytes)
      }
    }
  }

  fun clearAllOfflineTiles() {
    cancelDownload()
    managerScope.launch {
      try {
        rootTileDir.deleteRecursively()
        rootTileDir.mkdirs()
      } catch (_: Exception) {}
      _downloadState.update {
        it.copy(
          cachedTileCount = 0,
          cachedSizeBytes = 0L,
          statusMessage = "Çevrimdışı harita önbelleği temizlendi."
        )
      }
    }
  }

  fun cancelDownload() {
    activeDownloadJob?.cancel()
    activeDownloadJob = null
    _downloadState.update {
      if (it.isDownloading) {
        it.copy(isDownloading = false, statusMessage = "İndirme durduruldu (${it.completedTiles}/${it.totalTiles} karo hazır).")
      } else it
    }
    refreshStorageStatsAsync()
  }

  fun downloadRouteCorridor(
    pathCoordinates: List<Coordinate>,
    routeTitle: String,
    mapTypeMode: String = "roadmap",
    includeSeamarks: Boolean = true
  ) {
    if (pathCoordinates.size < 2) return
    val baseLayer = normalizeLayerName(mapTypeMode)
    val specs = buildRouteCorridorTileSpecs(
      path = pathCoordinates,
      corridorNm = 3.5,
      minZoom = 7,
      maxZoom = 14,
      baseLayer = baseLayer,
      includeSeamarks = includeSeamarks
    )
    launchTileDownloadJob("Rota: $routeTitle", specs)
  }

  fun downloadAroundLocation(
    centerLat: Double,
    centerLon: Double,
    radiusNm: Double,
    mapTypeMode: String = "roadmap",
    includeSeamarks: Boolean = true
  ) {
    val baseLayer = normalizeLayerName(mapTypeMode)
    val maxZ = when {
      radiusNm <= 18.0 -> 14
      radiusNm <= 40.0 -> 13
      else -> 12
    }
    val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.15)
    val dLat = radiusNm / 60.0
    val dLon = radiusNm / (60.0 * cosLat)
    val specs = buildBoundingBoxTileSpecs(
      minLat = (centerLat - dLat).coerceIn(-84.0, 84.0),
      maxLat = (centerLat + dLat).coerceIn(-84.0, 84.0),
      minLon = (centerLon - dLon).coerceIn(-179.9, 179.9),
      maxLon = (centerLon + dLon).coerceIn(-179.9, 179.9),
      minZoom = 7,
      maxZoom = maxZ,
      baseLayer = baseLayer,
      includeSeamarks = includeSeamarks,
      maxTileCap = 2600
    )
    launchTileDownloadJob("Çevre Haritası (${radiusNm.toInt()} NM)", specs)
  }

  fun downloadRegionPreset(
    preset: OfflineRegionPreset,
    mapTypeMode: String = "roadmap",
    includeSeamarks: Boolean = true
  ) {
    val baseLayer = normalizeLayerName(mapTypeMode)
    val specs = buildBoundingBoxTileSpecs(
      minLat = preset.minLat,
      maxLat = preset.maxLat,
      minLon = preset.minLon,
      maxLon = preset.maxLon,
      minZoom = preset.minZoom,
      maxZoom = preset.maxZoom,
      baseLayer = baseLayer,
      includeSeamarks = includeSeamarks && preset.maxZoom >= 7,
      maxTileCap = 3000
    )
    launchTileDownloadJob(preset.name, specs)
  }

  private fun launchTileDownloadJob(title: String, specs: List<TileSpec>) {
    if (specs.isEmpty()) return
    if (!_isDeviceOnline.value) {
      _downloadState.update {
        it.copy(
          isDownloading = false,
          statusMessage = "Harita indirmek için geçici internet bağlantısı gereklidir. Kayıtlı karolar çevrimdışı kullanıma hazırdır."
        )
      }
      return
    }

    activeDownloadJob?.cancel()
    activeDownloadJob = managerScope.launch {
      val total = specs.size
      val completedCounter = AtomicInteger(0)
      val failedCounter = AtomicInteger(0)
      val bytesCounter = AtomicLong(0L)
      var lastEmitMs = 0L

      _downloadState.update {
        it.copy(
          isDownloading = true,
          taskTitle = title,
          completedTiles = 0,
          totalTiles = total,
          downloadedBytes = 0L,
          failedTiles = 0,
          statusMessage = "$title indiriliyor..."
        )
      }

      try {
        val semaphore = Semaphore(8)
        coroutineScope {
          specs.map { spec ->
            async(Dispatchers.IO) {
              if (!isActive) return@async
              semaphore.withPermit {
                if (!isActive) return@withPermit
                val existing = getTileFile(spec.layer, spec.z, spec.x, spec.y)
                if (existing.exists() && existing.length() > 64L) {
                  bytesCounter.addAndGet(existing.length())
                  completedCounter.incrementAndGet()
                } else {
                  val bytes = fetchRemoteTileBytes(spec.layer, spec.z, spec.x, spec.y)
                  if (bytes != null && bytes.isNotEmpty()) {
                    saveTileBytesAtomically(spec.layer, spec.z, spec.x, spec.y, bytes)
                    bytesCounter.addAndGet(bytes.size.toLong())
                    completedCounter.incrementAndGet()
                  } else {
                    failedCounter.incrementAndGet()
                  }
                }

                val now = System.currentTimeMillis()
                if (now - lastEmitMs > 120L) {
                  lastEmitMs = now
                  _downloadState.update { cur ->
                    cur.copy(
                      completedTiles = completedCounter.get(),
                      downloadedBytes = bytesCounter.get(),
                      failedTiles = failedCounter.get()
                    )
                  }
                }
              }
            }
          }.awaitAll()
        }

        val doneCount = completedCounter.get()
        val mbStr = String.format(Locale.US, "%.1f", bytesCounter.get() / (1024.0 * 1024.0))
        _downloadState.update {
          it.copy(
            isDownloading = false,
            completedTiles = doneCount,
            downloadedBytes = bytesCounter.get(),
            failedTiles = failedCounter.get(),
            statusMessage = "✅ $title indirildi ($doneCount karo • $mbStr MB). İnternetsiz navigasyona hazır!"
          )
        }
        refreshStorageStatsAsync()
      } catch (_: CancellationException) {
        // Handled in cancelDownload()
      } catch (e: Exception) {
        _downloadState.update {
          it.copy(
            isDownloading = false,
            statusMessage = "İndirme kesildi (${completedCounter.get()}/$total karo kaydedildi)."
          )
        }
        refreshStorageStatsAsync()
      }
    }
  }

  /**
   * WebView içinden gelen Leaflet JS/CSS ve Harita Karosu isteklerini yakalar.
   * İnternet yokken %100 yerel kaynaklardan yanıt döner.
   */
  fun interceptWebRequest(request: WebResourceRequest): WebResourceResponse? {
    val uri = request.url ?: return null
    val urlStr = uri.toString()

    // 1. Yerel Leaflet JS & CSS Motoru (APK assets/leaflet/ içinden sıfır internetle sunulur)
    if (urlStr.contains("/local-assets/leaflet/leaflet.css") || urlStr.endsWith("/leaflet.css")) {
      return serveAssetFile("leaflet/leaflet.css", "text/css")
    }
    if (urlStr.contains("/local-assets/leaflet/leaflet.js") || urlStr.endsWith("/leaflet.js")) {
      return serveAssetFile("leaflet/leaflet.js", "application/javascript")
    }

    // 2. Google Harita Karoları (mt0..mt3.google.com/vt/lyrs=...&x=...&y=...&z=...)
    if (uri.host?.contains("google.com") == true && uri.path?.contains("/vt") == true) {
      val lyrs = uri.getQueryParameter("lyrs") ?: "m"
      val x = uri.getQueryParameter("x")?.toIntOrNull()
      val y = uri.getQueryParameter("y")?.toIntOrNull()
      val z = uri.getQueryParameter("z")?.toIntOrNull()
      if (x != null && y != null && z != null) {
        val layer = when (lyrs) {
          "s" -> "satellite"
          "y" -> "hybrid"
          "r" -> "dark"
          else -> "roadmap"
        }
        return resolveMapTileResponse(layer, z, x, y)
      }
    }

    // 3. OpenSeaMap Deniz İşaretleri Karoları (tiles.openseamap.org/seamark/{z}/{x}/{y}.png)
    if (uri.host?.contains("openseamap.org") == true) {
      val segments = uri.pathSegments
      if (segments.size >= 4) {
        val z = segments[segments.size - 3].toIntOrNull()
        val x = segments[segments.size - 2].toIntOrNull()
        val yStr = segments[segments.size - 1].removeSuffix(".png")
        val y = yStr.toIntOrNull()
        if (x != null && y != null && z != null) {
          return resolveMapTileResponse("seamark", z, x, y)
        }
      }
    }

    return null
  }

  private fun serveAssetFile(assetPath: String, mimeType: String): WebResourceResponse? {
    return try {
      val stream = appContext.assets.open(assetPath)
      WebResourceResponse(mimeType, "UTF-8", 200, "OK", corsHeaders(mimeType), stream)
    } catch (_: Exception) {
      null
    }
  }

  private fun resolveMapTileResponse(layer: String, z: Int, x: Int, y: Int): WebResourceResponse {
    // Aşama 1: Tam eşleşen çevrimdışı karo diskte var mı?
    val exactFile = getTileFile(layer, z, x, y)
    if (exactFile.exists() && exactFile.length() > 64L) {
      val mime = detectMimeType(exactFile)
      return WebResourceResponse(mime, null, 200, "OK", corsHeaders(mime), FileInputStream(exactFile))
    }

    // Çevrimdışı moddaysak ve kullanıcı Uydu/Hibrit seçtiyse diskteki "roadmap" karosunu da kontrol et
    if (isEffectiveOffline() && layer != "seamark" && layer != "roadmap") {
      val roadmapFallback = getTileFile("roadmap", z, x, y)
      if (roadmapFallback.exists() && roadmapFallback.length() > 64L) {
        val mime = detectMimeType(roadmapFallback)
        return WebResourceResponse(mime, null, 200, "OK", corsHeaders(mime), FileInputStream(roadmapFallback))
      }
    }

    // Aşama 2: İnternet varsa ve Zorunlu Çevrimdışı Mod kapalıysa canlı indir ve diske otomatik kaydet
    if (!isEffectiveOffline()) {
      val liveBytes = fetchRemoteTileBytes(layer, z, x, y)
      if (liveBytes != null && liveBytes.isNotEmpty()) {
        saveTileBytesAtomically(layer, z, x, y, liveBytes)
        val mime = detectMimeBytes(liveBytes)
        return WebResourceResponse(mime, null, 200, "OK", corsHeaders(mime), ByteArrayInputStream(liveBytes))
      }
    }

    // Aşama 3: Çevrimdışı Durum! Seamark katmanıysa şeffaf PNG dön (hata ikonunu önler)
    if (layer == "seamark") {
      return WebResourceResponse("image/png", null, 200, "OK", corsHeaders("image/png"), ByteArrayInputStream(transparent1x1Png))
    }

    // Aşama 4: Üst yakınlaştırma seviyesinden kırpma (Offline Overzoom z-1 .. z-6)
    val overzoomPng = buildOverzoomTileFromAncestor(layer, z, x, y)
    if (overzoomPng != null) {
      return WebResourceResponse("image/png", null, 200, "OK", corsHeaders("image/png"), ByteArrayInputStream(overzoomPng))
    }

    // Aşama 5: Hiç indirilmemiş bölgeyse yerleşik kıyı poligonlarıyla Vektörel Deniz Haritası Karosu üret
    val vectorPng = renderVectorNauticalFallbackTile(z, x, y, darkTheme = layer == "dark")
    return WebResourceResponse("image/png", null, 200, "OK", corsHeaders("image/png"), ByteArrayInputStream(vectorPng))
  }

  private fun buildOverzoomTileFromAncestor(layer: String, z: Int, x: Int, y: Int): ByteArray? {
    val minAncestorZ = max(0, z - 6)
    val candidateLayers = if (layer == "roadmap") listOf("roadmap") else listOf(layer, "roadmap")
    for (ancZ in (z - 1) downTo minAncestorZ) {
      val shift = z - ancZ
      val scale = 1 shl shift
      val ancX = x shr shift
      val ancY = y shr shift
      for (candLayer in candidateLayers) {
        val f = getTileFile(candLayer, ancZ, ancX, ancY)
        if (f.exists() && f.length() > 64L) {
          try {
            val parentBmp = BitmapFactory.decodeFile(f.absolutePath) ?: continue
            val tileW = parentBmp.width
            val tileH = parentBmp.height
            val subW = max(1, tileW / scale)
            val subH = max(1, tileH / scale)
            val srcX = ((x and (scale - 1)) * tileW) / scale
            val srcY = ((y and (scale - 1)) * tileH) / scale
            val safeX = srcX.coerceIn(0, max(0, tileW - subW))
            val safeY = srcY.coerceIn(0, max(0, tileH - subH))
            val cropped = Bitmap.createBitmap(parentBmp, safeX, safeY, subW, subH)
            val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 92, out)
            if (cropped !== parentBmp) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            parentBmp.recycle()
            return out.toByteArray()
          } catch (_: Exception) {
            continue
          }
        }
      }
    }
    return null
  }

  private fun renderVectorNauticalFallbackTile(z: Int, x: Int, y: Int, darkTheme: Boolean): ByteArray {
    val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val seaColor = if (darkTheme) 0xFF0B192C.toInt() else 0xFFD4ECFA.toInt()
    val landFillColor = if (darkTheme) 0xFF1E293B.toInt() else 0xFFEFECE4.toInt()
    val coastStrokeColor = if (darkTheme) 0xFF475569.toInt() else 0xFF64748B.toInt()
    val gridColor = if (darkTheme) 0x3338BDF8 else 0x330284C7

    canvas.drawColor(seaColor)

    val northLat = tileYToLat(y, z)
    val southLat = tileYToLat(y + 1, z)
    val westLon = tileXToLon(x, z)
    val eastLon = tileXToLon(x + 1, z)

    val landFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = landFillColor
    }
    val coastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.6f
      color = coastStrokeColor
    }

    val n = (1 shl z).toDouble()
    fun lonToPx(lon: Double): Float {
      val globalX = ((lon + 180.0) / 360.0) * n
      return ((globalX - x) * 256.0).toFloat()
    }
    fun latToPy(lat: Double): Float {
      val clamped = lat.coerceIn(-85.0511, 85.0511)
      val rad = Math.toRadians(clamped)
      val globalY = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n
      return ((globalY - y) * 256.0).toFloat()
    }

    for (poly in MarineLandPolygons.allPolygons) {
      if (poly.maxLat < southLat || poly.minLat > northLat || poly.maxLon < westLon || poly.minLon > eastLon) {
        continue
      }
      val pts = poly.coords
      if (pts.size < 6) continue
      val path = Path()
      path.moveTo(lonToPx(pts[1]), latToPy(pts[0]))
      var idx = 2
      while (idx < pts.size) {
        path.lineTo(lonToPx(pts[idx + 1]), latToPy(pts[idx]))
        idx += 2
      }
      path.close()
      canvas.drawPath(path, landFillPaint)
      canvas.drawPath(path, coastPaint)
    }

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1f
      color = gridColor
      pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }
    canvas.drawLine(0f, 0f, 256f, 0f, gridPaint)
    canvas.drawLine(0f, 0f, 0f, 256f, gridPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (darkTheme) 0x6694A3B8 else 0x66475569
      textSize = 10f
    }
    val centerLat = (northLat + southLat) * 0.5
    val centerLon = (westLon + eastLon) * 0.5
    canvas.drawText(
      String.format(Locale.US, "%.2f°, %.2f°", centerLat, centerLon),
      8f,
      246f,
      textPaint
    )

    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
    bmp.recycle()
    return out.toByteArray()
  }

  private fun fetchRemoteTileBytes(layer: String, z: Int, x: Int, y: Int): ByteArray? {
    val remoteUrl = if (layer == "seamark") {
      "https://tiles.openseamap.org/seamark/$z/$x/$y.png"
    } else {
      val lyrCode = when (layer) {
        "satellite" -> "s"
        "hybrid" -> "y"
        "dark" -> "r"
        else -> "m"
      }
      val sub = (x + y) and 3
      "https://mt$sub.google.com/vt/lyrs=$lyrCode&x=$x&y=$y&z=$z"
    }

    val req = Request.Builder()
      .url(remoteUrl)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
      .build()

    return try {
      httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        val bytes = resp.body?.bytes()
        if (bytes != null && bytes.size > 64) bytes else null
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun getTileFile(layer: String, z: Int, x: Int, y: Int): File {
    return File(rootTileDir, "$layer/$z/$x/$y.tile")
  }

  private fun saveTileBytesAtomically(layer: String, z: Int, x: Int, y: Int, bytes: ByteArray) {
    try {
      val target = getTileFile(layer, z, x, y)
      target.parentFile?.mkdirs()
      val tmp = File(target.parentFile, "${y}_${Thread.currentThread().id}.tmp")
      tmp.writeBytes(bytes)
      if (!tmp.renameTo(target)) {
        tmp.copyTo(target, overwrite = true)
        tmp.delete()
      }
    } catch (_: Exception) {}
  }

  private fun detectMimeType(file: File): String {
    return try {
      val header = ByteArray(4)
      FileInputStream(file).use { it.read(header) }
      detectMimeBytes(header)
    } catch (_: Exception) {
      "image/png"
    }
  }

  private fun detectMimeBytes(bytes: ByteArray): String {
    if (bytes.size >= 3 &&
      bytes[0] == 0xFF.toByte() &&
      bytes[1] == 0xD8.toByte() &&
      bytes[2] == 0xFF.toByte()
    ) {
      return "image/jpeg"
    }
    return "image/png"
  }

  private fun corsHeaders(contentType: String): Map<String, String> = mapOf(
    "Access-Control-Allow-Origin" to "*",
    "Cache-Control" to "max-age=31536000, public",
    "Content-Type" to contentType
  )

  private fun normalizeLayerName(mode: String): String = when (mode) {
    "satellite" -> "satellite"
    "hybrid" -> "hybrid"
    "dark" -> "dark"
    else -> "roadmap"
  }

  private fun buildRouteCorridorTileSpecs(
    path: List<Coordinate>,
    corridorNm: Double,
    minZoom: Int,
    maxZoom: Int,
    baseLayer: String,
    includeSeamarks: Boolean
  ): List<TileSpec> {
    val sampled = ArrayList<Coordinate>()
    for (i in 0 until path.size - 1) {
      val a = path[i]
      val b = path[i + 1]
      val cosLat = cos(Math.toRadians((a.latitude + b.latitude) * 0.5)).coerceAtLeast(0.15)
      val segNm = hypot((b.latitude - a.latitude) * 60.0, (b.longitude - a.longitude) * 60.0 * cosLat)
      val steps = max(1, (segNm / 0.8).toInt())
      for (s in 0..steps) {
        val t = s.toDouble() / steps.toDouble()
        sampled.add(
          Coordinate(
            a.latitude + t * (b.latitude - a.latitude),
            a.longitude + t * (b.longitude - a.longitude)
          )
        )
      }
    }

    val result = LinkedHashSet<TileSpec>()
    for (z in minZoom..maxZoom) {
      for (pt in sampled) {
        val cosLat = cos(Math.toRadians(pt.latitude)).coerceAtLeast(0.15)
        val dLat = corridorNm / 60.0
        val dLon = corridorNm / (60.0 * cosLat)
        val xMin = lonToTileX(pt.longitude - dLon, z)
        val xMax = lonToTileX(pt.longitude + dLon, z)
        val yMin = latToTileY(pt.latitude + dLat, z)
        val yMax = latToTileY(pt.latitude - dLat, z)
        for (tx in xMin..xMax) {
          for (ty in yMin..yMax) {
            result.add(TileSpec(baseLayer, z, tx, ty))
            if (includeSeamarks && z <= 14) {
              result.add(TileSpec("seamark", z, tx, ty))
            }
            if (result.size >= 2800) return result.toList()
          }
        }
      }
    }
    return result.toList()
  }

  private fun buildBoundingBoxTileSpecs(
    minLat: Double,
    maxLat: Double,
    minLon: Double,
    maxLon: Double,
    minZoom: Int,
    maxZoom: Int,
    baseLayer: String,
    includeSeamarks: Boolean,
    maxTileCap: Int
  ): List<TileSpec> {
    val result = LinkedHashSet<TileSpec>()
    for (z in minZoom..maxZoom) {
      val xMin = lonToTileX(minLon, z)
      val xMax = lonToTileX(maxLon, z)
      val yMin = latToTileY(maxLat, z)
      val yMax = latToTileY(minLat, z)
      for (tx in xMin..xMax) {
        for (ty in yMin..yMax) {
          result.add(TileSpec(baseLayer, z, tx, ty))
          if (includeSeamarks && z in 7..13) {
            result.add(TileSpec("seamark", z, tx, ty))
          }
          if (result.size >= maxTileCap) return result.toList()
        }
      }
    }
    return result.toList()
  }

  companion object {
    @Volatile
    private var INSTANCE: OfflineMapTileManager? = null

    fun getInstance(context: Context): OfflineMapTileManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: OfflineMapTileManager(context).also { INSTANCE = it }
      }
    }

    val presetRegions: List<OfflineRegionPreset> = listOf(
      OfflineRegionPreset(
        id = "marmara_straits",
        name = "Marmara Denizi & Boğazlar",
        subtitle = "İstanbul Boğazı, Marmara, İzmit Körfezi ve Çanakkale Boğazı (Zoom 7–13)",
        minLat = 40.00,
        maxLat = 41.30,
        minLon = 26.10,
        maxLon = 29.98,
        minZoom = 7,
        maxZoom = 13
      ),
      OfflineRegionPreset(
        id = "aegean_sea",
        name = "Ege Denizi & Kıyı Adaları",
        subtitle = "Saros, İzmir, Çeşme, Kuşadası, Bodrum, Datça ve Ege Adaları (Zoom 7–11)",
        minLat = 36.50,
        maxLat = 40.10,
        minLon = 24.80,
        maxLon = 28.30,
        minZoom = 7,
        maxZoom = 11
      ),
      OfflineRegionPreset(
        id = "mediterranean_tr",
        name = "Akdeniz Kıyıları & Kıbrıs",
        subtitle = "Fethiye, Kaş, Antalya, Mersin, İskenderun ve Kıbrıs (Zoom 7–11)",
        minLat = 34.50,
        maxLat = 37.00,
        minLon = 28.30,
        maxLon = 36.25,
        minZoom = 7,
        maxZoom = 11
      ),
      OfflineRegionPreset(
        id = "black_sea",
        name = "Karadeniz Kıyıları",
        subtitle = "İğneada, Zonguldak, Sinop, Samsun, Trabzon, Hopa (Zoom 7–11)",
        minLat = 40.90,
        maxLat = 42.35,
        minLon = 27.90,
        maxLon = 41.60,
        minZoom = 7,
        maxZoom = 11
      ),
      OfflineRegionPreset(
        id = "world_ocean_base",
        name = "Dünya Okyanusları Temel Haritası",
        subtitle = "Tüm dünya denizleri, Süveyş, Cebelitarık, Panama ve Malaka ana görünümü (Zoom 2–4)",
        minLat = -65.0,
        maxLat = 72.0,
        minLon = -179.9,
        maxLon = 179.9,
        minZoom = 2,
        maxZoom = 4
      )
    )

    fun lonToTileX(lon: Double, zoom: Int): Int {
      val n = 1 shl zoom
      val clampedLon = lon.coerceIn(-179.9999, 179.9999)
      return floor((clampedLon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
      val n = 1 shl zoom
      val clampedLat = lat.coerceIn(-85.0511, 85.0511)
      val rad = Math.toRadians(clampedLat)
      val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n
      return floor(y).toInt().coerceIn(0, n - 1)
    }

    fun tileXToLon(x: Int, zoom: Int): Double {
      val n = (1 shl zoom).toDouble()
      return (x.toDouble() / n) * 360.0 - 180.0
    }

    fun tileYToLat(y: Int, zoom: Int): Double {
      val n = (1 shl zoom).toDouble()
      val a = Math.PI * (1.0 - 2.0 * y.toDouble() / n)
      return Math.toDegrees(atan(sinh(a)))
    }
  }
}
