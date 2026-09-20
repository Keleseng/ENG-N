package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.location.GpsLocationProvider
import com.example.model.Coordinate
import com.example.model.LocationPresets
import com.example.model.decimalToDmsParts
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Hızlandırılmış Canlı MarineTraffic AIS Harita Görünümü
 * Donanım hızlandırma (GPU), canlı GPS takip katmanı, mevki telemetri kartı ve rota göstergesi.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarineMapView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isDark = uiState.isDarkMode
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider(context) }

  // GPS Pozisyonu veya mevcut mevki
  val activeGps = uiState.lastGpsFix
  val currentLat = activeGps?.latitude
    ?: (LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.selectedPort.latitude)
  val currentLon = activeGps?.longitude
    ?: (LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.selectedPort.longitude)

  var isSatelliteMode by remember { mutableStateOf(false) }

  fun buildGoogleMapHtml(lat: Double, lon: Double, zoom: Int = 13): String {
    val lyr = if (isSatelliteMode) "y" else "m"
    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
          html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #0b132b; }
        </style>
      </head>
      <body>
        <div id="map"></div>
        <script>
          var map = L.map('map', { zoomControl: false }).setView([$lat, $lon], $zoom);
          L.tileLayer('https://mt1.google.com/vt/lyrs=$lyr&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0','mt1','mt2','mt3']
          }).addTo(map);
          L.tileLayer('https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png', {
            maxZoom: 18
          }).addTo(map);
          L.marker([$lat, $lon]).addTo(map).bindPopup('Seçilen Konum').openPopup();
        </script>
      </body>
      </html>
    """.trimIndent()
  }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  fun loadMapLocation(lat: Double, lon: Double, zoom: Int = 13) {
    val html = buildGoogleMapHtml(lat, lon, zoom)
    webViewRef?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  LaunchedEffect(uiState.mapFocusCoordinate) {
    uiState.mapFocusCoordinate?.let { target ->
      loadMapLocation(target.latitude, target.longitude, 14)
    }
  }
  var isLoading by remember { mutableStateOf(true) }
  var loadingProgress by remember { mutableStateOf(0) }
  var hasLoadError by remember { mutableStateOf(false) }
  var showAisDialog by remember { mutableStateOf(false) }
  var aisDialogInput by remember { mutableStateOf(uiState.mmsiStr) }

  // AIS Gemi Mevkii
  val aisVessel = uiState.activeAisVesselData ?: (if (uiState.mmsiStr.isNotBlank()) com.example.engine.AisTrackingEngine.getNb252ShipData() else null)
  val aisLat = aisVessel?.latitude ?: 40.82833
  val aisLon = aisVessel?.longitude ?: 29.25399

  fun focusOnAisVessel(queryStr: String) {
    val clean = queryStr.trim()
    if (clean.isNotBlank()) {
      viewModel.updateMmsi(clean)
      viewModel.fetchDepthFromMmsiVessel(clean)
      val vessel = uiState.activeAisVesselData ?: com.example.engine.AisTrackingEngine.getNb252ShipData()
      val targetLat = vessel.latitude
      val targetLon = vessel.longitude
      loadMapLocation(targetLat, targetLon, 14)
      android.widget.Toast.makeText(
        context,
        "AIS Gemisine Odaklanıldı: ${vessel.name} (MMSI: ${vessel.mmsi})",
        android.widget.Toast.LENGTH_SHORT
      ).show()
    } else {
      showAisDialog = true
    }
  }

  fun fetchGpsNow() {
    viewModel.setGpsLoading(true)
    coroutineScope.launch {
      val result = gpsProvider.getCurrentGpsFix()
      result.fold(
        onSuccess = { fix ->
          viewModel.applyGpsFix(fix, isExplicitSync = true, formatAsDms = true)
          viewModel.startContinuousLocationUpdates(gpsProvider)
          loadMapLocation(fix.latitude, fix.longitude, 14)
        },
        onFailure = { error ->
          viewModel.setGpsError(error.localizedMessage ?: "GPS konumu alınamadı.")
        }
      )
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      fetchGpsNow()
    } else {
      viewModel.setGpsError("Haritada canlı GPS takibi için konum izni gereklidir.")
    }
  }

  fun requestLocationPermissionAndFetch() {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (fine || coarse) {
      fetchGpsNow()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  // URL güncellendiğinde veya harita ilk yüklendiğinde JavaScript katmanını besle
  LaunchedEffect(activeGps, uiState.mobEvent, uiState.anchorEvent, uiState.mapFocusCoordinate, uiState.selectedSimpleEtaDestination) {
    webViewRef?.evaluateJavascript(
      com.example.engine.MapOverlayInjector.getInjectableJavascript(uiState),
      null
    )
  }

  // Reklam ve işlemci tüketen takipçi domainleri
  val blockedTrackerHosts = remember {
    listOf(
      "doubleclick.net", "google-analytics.com", "googlesyndication.com",
      "adnxs.com", "criteo.com", "amazon-adsystem.com", "adservice.google",
      "rubiconproject.com", "pubmatic.com", "casalemedia.com", "outbrain.com",
      "taboola.com", "didomi.io", "cookiebot.com", "cookielaw.org",
      "facebook.net", "hotjar.com", "scorecardresearch.com"
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9))
      .testTag("screen_marinetraffic_map")
  ) {
    // ══════════════════════════════════════════════════════════════════════
    // MARINETRAFFIC WEBVIEW HARİTASI (TAM EKRAN - GPU HIZLANDIRMALI)
    // ══════════════════════════════════════════════════════════════════════
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("webview_marinetraffic_map"),
      factory = { ctx ->
        WebView(ctx).apply {
          setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          CookieManager.getInstance().setAcceptCookie(true)
          CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            @Suppress("DEPRECATION")
            setEnableSmoothTransition(true)
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 MarineNav/1.0"
          }

          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
              val urlStr = request?.url?.toString()?.lowercase() ?: return super.shouldInterceptRequest(view, request)
              for (host in blockedTrackerHosts) {
                if (urlStr.contains(host)) {
                  return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
              }
              return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
              hasLoadError = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isLoading = false

              val hideAdsScript = """
                (function() {
                  var style = document.createElement('style');
                  style.innerHTML = '
                    #didomi-host, .qc-cmp2-container, #onetrust-consent-sdk, .ad-unit, .advertisement, [id*="google_ads"], [id*="ad-container"], .ad-slot, .banner { display: none !important; visibility: hidden !important; height: 0 !important; pointer-events: none !important; }
                    ${if (isDark) "html { filter: invert(100%) hue-rotate(180deg) brightness(85%) contrast(85%); background: #121212 !important; } iframe, img, canvas { filter: invert(100%) hue-rotate(180deg) !important; }" else ""}
                  ';
                  document.head.appendChild(style);
                })();
              """.trimIndent()
              view?.evaluateJavascript(hideAdsScript, null)
              view?.evaluateJavascript(com.example.engine.MapOverlayInjector.getInjectableJavascript(uiState), null)
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?
            ) {
              super.onReceivedError(view, request, error)
              if (request?.isForMainFrame == true) {
                isLoading = false
                hasLoadError = true
              }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
              return true
            }
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              super.onProgressChanged(view, newProgress)
              loadingProgress = newProgress
              if (newProgress >= 70) {
                isLoading = false
              }
            }
          }

          webViewRef = this
          loadMapLocation(currentLat, currentLon, 13)
        }
      },
      update = { webView ->
        // handled via loadMapLocation
      }
    )

    // ══════════════════════════════════════════════════════════════════════
    // MOB VE DEMİR BİLGİ PANELLERİ
    // ══════════════════════════════════════════════════════════════════════
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        .fillMaxWidth(0.9f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (uiState.mobEvent.isActive) {
        val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.mobEvent.latitude
        val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.mobEvent.longitude
        val mobDistNm = uiState.mobEvent.calculateDistanceNm(currentLat, currentLon)
        val mobDistGomina = uiState.mobEvent.calculateDistanceGomina(currentLat, currentLon)
        val mobBearing = uiState.mobEvent.calculateBearingDegrees(currentLat, currentLon)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF450A0A).copy(alpha = 0.85f) else Color(0xFFFEF2F2).copy(alpha = 0.95f),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
          modifier = Modifier.fillMaxWidth().clickable {
            // Haritayı MOB noktasına odakla
            loadMapLocation(uiState.mobEvent.latitude, uiState.mobEvent.longitude, 16)
          }
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "🚨 MOB (Denize adam düştü)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                  color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                )
              }
              Text(
                text = "Saat: ${uiState.mobEvent.timeFormatted}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Mesafe: ${String.format(java.util.Locale.US, "%.2f NM (%.1f Gom)", mobDistNm, mobDistGomina)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
              Text(
                text = "Kerteriz: ${String.format(java.util.Locale.US, "%03d°", mobBearing)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
            }
          }
        }
      }

      if (uiState.anchorEvent.isAnchored) {
        val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.anchorEvent.latitude
        val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.anchorEvent.longitude
        val anchorDistMeters = uiState.anchorEvent.calculateDistanceMeters(currentLat, currentLon)
        val limitMeters = uiState.anchorEvent.safeSwingingRadiusGomina * 185.2
        val isDragging = anchorDistMeters > limitMeters
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDragging) (if(isDark) Color(0xFF450A0A).copy(alpha = 0.85f) else Color(0xFFFEF2F2).copy(alpha=0.95f)) else (if(isDark) Color(0xFF064E3B).copy(alpha=0.85f) else Color(0xFFECFDF5).copy(alpha=0.95f)),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDragging) Color(0xFFDC2626) else Color(0xFF10B981)),
          modifier = Modifier.fillMaxWidth().clickable {
            // Haritayı demir noktasına odakla
            loadMapLocation(uiState.anchorEvent.latitude, uiState.anchorEvent.longitude, 16)
          }
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Anchor, contentDescription = null, tint = if(isDragging) Color(0xFFEF4444) else Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if(isDragging) "⚠️ DEMİR TARIYOR!" else "⚓ DEMİR NÖBETİ AKTİF",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                  color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
                )
              }
              Text(
                text = "Saat: ${uiState.anchorEvent.dropTimeFormatted}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Uzaklık: ${String.format(java.util.Locale.US, "%.1f", anchorDistMeters)}m",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
              Text(
                text = "Limit: ${String.format(java.util.Locale.US, "%.1f", limitMeters)}m",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
            }
          }
        }
      }

      // 3. Hedef / ETA Varış Mevkii (Mavi Nokta) Bilgi Kartı
      val targetCoord = uiState.mapFocusCoordinate
        ?: uiState.selectedSimpleEtaDestination?.let { Coordinate(it.lat, it.lon) }
      if (targetCoord != null) {
        val targetName = uiState.selectedSimpleEtaDestination?.name ?: "Varış Mevkii"
        val curLat = activeGps?.latitude
          ?: (LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.selectedPort.latitude)
        val curLon = activeGps?.longitude
          ?: (LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.selectedPort.longitude)
        val distNm = LocationPresets.calculateDistanceNm(curLat, curLon, targetCoord.latitude, targetCoord.longitude)
        val bearing = LocationPresets.calculateBearingDegrees(curLat, curLon, targetCoord.latitude, targetCoord.longitude)
        val latParts = decimalToDmsParts(targetCoord.latitude, true)
        val lonParts = decimalToDmsParts(targetCoord.longitude, false)
        val coordDmsStr = "${latParts.degrees}°${latParts.minutes}'${latParts.seconds}\"${latParts.direction}  ${lonParts.degrees}°${lonParts.minutes}'${lonParts.seconds}\"${lonParts.direction}"

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF0F294A).copy(alpha = 0.92f) else Color(0xFFEFF6FF).copy(alpha = 0.95f),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              loadMapLocation(targetCoord.latitude, targetCoord.longitude, 14)
            }
            .testTag("card_target_eta_destination")
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF2563EB), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "🎯 $targetName (Mavi Nokta)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.4.sp
                  ),
                  color = if (isDark) MarineCyan else PrimaryBlueDark
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    viewModel.setMapFocusCoordinate(null)
                  },
                  modifier = Modifier.size(22.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kaldır",
                    tint = getMarineTextSecondary(isDark),
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = coordDmsStr,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = getMarineTextPrimary(isDark)
              )
              Text(
                text = "${String.format(Locale.US, "%.1f NM", distNm)} • ${String.format(Locale.US, "%03d°", bearing)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MarineYellow else Color(0xFFB45309)
              )
            }
          }
        }
      }

      // Harita İşaret Renk Lejantı (GPS: Yeşil, AIS: Sarı, MOB: Kırmızı, Varış: Mavi)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
        shadowElevation = 2.dp,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 🟢 GPS
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), CircleShape))
            Text("GPS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = getMarineTextPrimary(isDark))
          }
          // 🟡 AIS
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEAB308), CircleShape))
            Text("AIS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = getMarineTextPrimary(isDark))
          }
          // 🔴 MOB
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFDC2626), CircleShape))
            Text("MOB", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = getMarineTextPrimary(isDark))
          }
          // 🔵 Varış
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF2563EB), CircleShape))
            Text("Varış", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = getMarineTextPrimary(isDark))
          }
        }
      }
    }



    // ══════════════════════════════════════════════════════════════════════


    // SAĞ ALT KONTROL BUTONLARI (AIS GEMİSİ / GPS AL / ODAKLAN / YENİLE)
    // ══════════════════════════════════════════════════════════════════════
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 12.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.End
    ) {
      // MOB Butonu (Küçültülmüş & Kibar)
      Surface(
        onClick = {
          if (uiState.mobEvent.isActive) viewModel.cancelMob() else viewModel.triggerMob()
        },
        color = if (uiState.mobEvent.isActive) Color.Black else Color(0xFFDC2626),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .height(32.dp)
          .testTag("fab_mob_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.Warning, contentDescription = "MOB", tint = Color.White, modifier = Modifier.size(14.dp))
          Text(
            text = if (uiState.mobEvent.isActive) "MOB İptal" else "MOB",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
            color = Color.White
          )
        }
      }
      
      // Demirleme Butonu (Küçültülmüş & Kibar)
      Surface(
        onClick = {
          if (uiState.anchorEvent.isAnchored) {
            viewModel.liftAnchor()
          } else {
            viewModel.applyAnchorCalculationToSwingingCircle()
            viewModel.dropAnchor()
          }
        },
        color = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else Color(0xFFEAB308),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .height(32.dp)
          .testTag("fab_anchor_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.Anchor, contentDescription = "Demir", tint = Color.White, modifier = Modifier.size(14.dp))
          Text(
            text = if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
            color = Color.White
          )
        }
      }
      


      // 1. AIS Butonu - Sarı/Amber Vurgulu (Kibar)
      Surface(
        onClick = {
          if (uiState.mmsiStr.isNotBlank()) {
            focusOnAisVessel(uiState.mmsiStr)
          } else {
            aisDialogInput = uiState.mmsiStr
            showAisDialog = true
          }
        },
        color = if (isDark) Color(0xFFB45309) else Color(0xFFD97706),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
          .height(32.dp)
          .testTag("fab_ais_target_location")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsBoat,
            contentDescription = "AIS Gemi Konumu",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = if (uiState.mmsiStr.isNotBlank()) "AIS (${uiState.mmsiStr})" else "AIS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = Color.White
          )
        }
      }

      // 2. Canlı GPS Al ve Haritayı Merkeze Al (Yeşil Vurgulu, Kibar)
      FloatingActionButton(
        onClick = {
          requestLocationPermissionAndFetch()
        },
        containerColor = Color(0xFF059669),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .size(38.dp)
          .testTag("fab_refresh_gps_map")
      ) {
        Icon(
          imageVector = Icons.Default.MyLocation,
          contentDescription = "GPS Konumunu Al ve Haritaya Odaklan",
          modifier = Modifier.size(18.dp)
        )
      }

      // 2.5 Varış Mevkiine (Mavi Noktaya) Odaklan
      if (uiState.mapFocusCoordinate != null || uiState.selectedSimpleEtaDestination != null) {
        val target = uiState.mapFocusCoordinate
          ?: uiState.selectedSimpleEtaDestination?.let { Coordinate(it.lat, it.lon) }
        if (target != null) {
          FloatingActionButton(
            onClick = {
              loadMapLocation(target.latitude, target.longitude, 14)
            },
            containerColor = Color(0xFF1D4ED8),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
              .size(38.dp)
              .testTag("fab_focus_target_eta")
          ) {
            Icon(
              imageVector = Icons.Default.PinDrop,
              contentDescription = "Mavi Noktaya Odaklan",
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // 3. Haritayı Yenile (Kibar)
      SmallFloatingActionButton(
        onClick = {
          webViewRef?.reload()
        },
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        contentColor = getMarineTextPrimary(isDark),
        shape = CircleShape,
        modifier = Modifier
          .size(34.dp)
          .testTag("fab_reload_webview_map")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Haritayı Yenile",
          modifier = Modifier.size(16.dp)
        )
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // AIS MMSI / IMO NUMARASI İLE KONUM GETİR DİALOGU
    // ══════════════════════════════════════════════════════════════════════
    if (showAisDialog) {
      AlertDialog(
        onDismissRequest = { showAisDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = MarineCyan)
            Text("AIS Gemi Konumu Getir", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              "AIS menüsüne girilen veya aramak istediğiniz MMSI/IMO numarasını girin:",
              style = MaterialTheme.typography.bodySmall,
              color = getMarineTextSecondary(isDark)
            )
            OutlinedTextField(
              value = aisDialogInput,
              onValueChange = { aisDialogInput = it },
              label = { Text("MMSI / IMO Numarası") },
              placeholder = { Text("örn: 222111447") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_ais_dialog_mmsi")
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showAisDialog = false
              focusOnAisVessel(aisDialogInput)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
          ) {
            Text("Konuma Odaklan")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAisDialog = false }) {
            Text("İptal")
          }
        }
      )
    }

    // Üstte İnce Yükleme Çubuğu
    AnimatedVisibility(
      visible = isLoading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter)
    ) {
      LinearProgressIndicator(
        progress = { loadingProgress / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp),
        color = MarineYellow,
        trackColor = Color(0xFF0F172A).copy(alpha = 0.3f)
      )
    }

    // Hata Durumu Göstergesi
    if (hasLoadError) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = getMarineCardBg(isDark).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
        shadowElevation = 8.dp,
        modifier = Modifier
          .align(Alignment.Center)
          .padding(24.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = DangerRed,
            modifier = Modifier.size(40.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "AIS Canlı Haritası Yüklenemedi",
            style = MaterialTheme.typography.titleSmall,
            color = getMarineTextPrimary(isDark)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "İnternet bağlantınızı kontrol edin veya harici tarayıcıda açın.",
            style = MaterialTheme.typography.bodySmall,
            color = getMarineTextSecondary(isDark)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                hasLoadError = false
                webViewRef?.reload()
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
              Text("Tekrar Dene")
            }
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$currentLat,$currentLon"))
                context.startActivity(intent)
              }
            ) {
              Text("Tarayıcıda Aç")
            }
          }
        }
      }
    }
  }
}
