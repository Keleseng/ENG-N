package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Coordinate
import com.example.model.LocationPresets
import com.example.model.RadarAisTarget
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AisRadarFullScreenDialog(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onDismiss: () -> Unit,
  onNavigateToMap: () -> Unit = {}
) {
  val textMeasurer = rememberTextMeasurer()
  val density = LocalDensity.current

  // Kendi gemimizin anlık konumu ve seyir telemetrisi
  val (ownLat, ownLon) = viewModel.getEffectiveShipCoordinates()
  val ownSpeed = uiState.lastGpsFix?.speedKnots?.takeIf { it > 0 } ?: uiState.activeAisVesselData?.sogKnots ?: 0.0
  val ownCog = uiState.lastGpsFix?.bearingDegrees?.toDouble()?.takeIf { it > 0 } ?: uiState.activeAisVesselData?.cogDegrees ?: 270.0

  val targets = uiState.surroundingAisTargets
  val selectedTarget = uiState.selectedRadarTarget
  val rangeNm = uiState.radarRangeNm
  val isHeadUp = uiState.isRadarHeadUp

  // Cihaz yönünü (pusula) al. Eğer sensör aktifse onu kullan, yoksa ownCog'a düş.
  val deviceHeading = if (uiState.marineAttitude.isSensorActive) {
    uiState.marineAttitude.compassDegrees.toDouble()
  } else {
    ownCog
  }

  // Sabit referanslar (recomposition olmadan pointerInput'ta erişmek için)
  val currentDeviceHeading by rememberUpdatedState(deviceHeading)
  val currentOwnLat by rememberUpdatedState(ownLat)
  val currentOwnLon by rememberUpdatedState(ownLon)

  // Tüm gemiler listesi alt paneli
  var showTargetListSheet by remember { mutableStateOf(false) }
  var listFilterType by remember { mutableStateOf("ALL") } // ALL, HAZARD, MOVING, ANCHORED

  // Radar tarama (Sweep) açısı animasyonu: 0°'den 360°'ye kesintisiz döner (yaklaşık 24 RPM)
  val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "SweepAngle"
  )

  // Çatışma uyarısı pulsing efekti
  val hazardPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "HazardPulse"
  )

  // Otomatik gemi pozisyon simülasyonu / periyodik güncelleme
  LaunchedEffect(Unit) {
    while (true) {
      delay(3000)
      if (uiState.isAisRadarOpen) {
        val updated = com.example.engine.SurroundingAisRadarEngine.stepVessels(
          targets = viewModel.uiState.value.surroundingAisTargets,
          centerLat = ownLat,
          centerLon = ownLon,
          ownSog = ownSpeed,
          ownCog = ownCog,
          deltaSeconds = 3.0
        )
        // Hedef listesini güncelle
        viewModel.updateSurroundingAisTargetsDirect(updated)
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF030712)) // Derin Takti̇k Deniz Mavisi / Gece Radarı
        .statusBarsPadding()
        .navigationBarsPadding()
        .testTag("dialog_ais_radar_fullscreen")
    ) {
      val isLandscape = maxWidth > maxHeight
      
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
              var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([$lat, $lon], $zoom);
              L.tileLayer('https://mt1.google.com/vt/lyrs=$lyr&x={x}&y={y}&z={z}', {
                maxZoom: 20,
                subdomains: ['mt0','mt1','mt2','mt3']
              }).addTo(map);
              L.tileLayer('https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png', {
                maxZoom: 18
              }).addTo(map);
              L.marker([$lat, $lon]).addTo(map);
            </script>
          </body>
          </html>
        """.trimIndent()
      }

      var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

      fun loadMapLocation(lat: Double, lon: Double, zoom: Int = 13) {
        val html = buildGoogleMapHtml(lat, lon, zoom)
        webViewRef?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
      }

      LaunchedEffect(Unit) {
         loadMapLocation(ownLat, ownLon, 13)
      }

      LaunchedEffect(selectedTarget) {
         if (selectedTarget != null) {
            loadMapLocation(selectedTarget.latitude, selectedTarget.longitude, 15)
         } else {
            loadMapLocation(ownLat, ownLon, 13)
         }
      }

      LaunchedEffect(uiState.surroundingAisTargets, ownLat, ownLon, selectedTarget) {
         webViewRef?.evaluateJavascript(
            com.example.engine.MapOverlayInjector.getInjectableJavascript(uiState),
            null
         )
      }


      if (isLandscape) {
        // YATAY MOD (Landscape)
        Box(modifier = Modifier.fillMaxSize()) {
          // ALT KATMAN: HARİTA
          androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
              android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = android.webkit.WebViewClient()
                webViewRef = this
              }
            },
            modifier = Modifier.fillMaxSize()
          )
          
          // ÜST KATMAN: RADAR VE ARAYÜZ
          Row(modifier = Modifier.fillMaxSize()) {
            // Sol: Radar (Canvas)
            Box(
              modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight(),
              contentAlignment = Alignment.Center
            ) {
              RadarCanvasContent(
                viewModel = viewModel,
                targets = targets,
                rangeNm = rangeNm,
                isHeadUp = isHeadUp,
                ownCog = ownCog,
                ownSpeed = ownSpeed,
                selectedTarget = selectedTarget,
                ownLat = ownLat,
                ownLon = ownLon,
                sweepAngle = sweepAngle,
                hazardPulseAlpha = hazardPulseAlpha,
                textMeasurer = textMeasurer,
                deviceHeading = deviceHeading,
                onShowTargetList = { showTargetListSheet = true }
              )
            }

            // Sağ: Kontroller ve Hedef Detayları
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0B132B).copy(alpha = 0.5f))
            ) {
              RadarHeaderBar(
                ownLat = ownLat,
                ownLon = ownLon,
                ownSpeed = ownSpeed,
                ownCog = ownCog,
                targetCount = targets.size,
                isHeadUp = isHeadUp,
                onToggleOrientation = { viewModel.toggleRadarOrientation() },
                onClose = onDismiss,
                onRefresh = { viewModel.refreshSurroundingAisTargets() }
              )
              
              Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                if (selectedTarget != null) {
                  RadarTargetDetailCard(
                    target = selectedTarget,
                    ownLat = ownLat,
                    ownLon = ownLon,
                    ownSpeed = ownSpeed,
                    ownCog = ownCog,
                    onClose = { viewModel.selectRadarTarget(null) },
                    onNavigateToMap = {
                      onDismiss()
                      viewModel.setTab(4)
                      onNavigateToMap()
                    },
                    onCalculateEta = {
                      viewModel.calculateAndSetEta(
                        Coordinate(selectedTarget.latitude, selectedTarget.longitude),
                        selectedTarget.name
                      )
                      onDismiss()
                      viewModel.setTab(0)
                    }
                  )
                }
              }

              RadarBottomQuickBar(
                currentRangeNm = rangeNm,
                onSelectRange = { viewModel.setRadarRange(it) },
                onOpenList = { showTargetListSheet = true }
              )
            }
          }
        }
      } else {
        // DİKEY MOD (Portrait)
        Box(modifier = Modifier.fillMaxSize()) {
          // ALT KATMAN: HARİTA
          androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
              android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = android.webkit.WebViewClient()
                webViewRef = this
              }
            },
            modifier = Modifier.fillMaxSize()
          )

          // ÜST KATMAN: RADAR VE ARAYÜZ
          Column(modifier = Modifier.fillMaxSize()) {
            RadarHeaderBar(
              ownLat = ownLat,
              ownLon = ownLon,
              ownSpeed = ownSpeed,
              ownCog = ownCog,
              targetCount = targets.size,
              isHeadUp = isHeadUp,
              onToggleOrientation = { viewModel.toggleRadarOrientation() },
              onClose = onDismiss,
              onRefresh = { viewModel.refreshSurroundingAisTargets() }
            )

            Box(
              modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              RadarCanvasContent(
                viewModel = viewModel,
                targets = targets,
                rangeNm = rangeNm,
                isHeadUp = isHeadUp,
                ownCog = ownCog,
                ownSpeed = ownSpeed,
                selectedTarget = selectedTarget,
                ownLat = ownLat,
                ownLon = ownLon,
                sweepAngle = sweepAngle,
                hazardPulseAlpha = hazardPulseAlpha,
                textMeasurer = textMeasurer,
                deviceHeading = deviceHeading,
                onShowTargetList = { showTargetListSheet = true }
              )
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
               if (selectedTarget != null) {
                  Box(modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
                      RadarTargetDetailCard(
                        target = selectedTarget,
                        ownLat = ownLat,
                        ownLon = ownLon,
                        ownSpeed = ownSpeed,
                        ownCog = ownCog,
                        onClose = { viewModel.selectRadarTarget(null) },
                        onNavigateToMap = {
                          onDismiss()
                          viewModel.setTab(4)
                          onNavigateToMap()
                        },
                        onCalculateEta = {
                          viewModel.calculateAndSetEta(
                            Coordinate(selectedTarget.latitude, selectedTarget.longitude),
                            selectedTarget.name
                          )
                          onDismiss()
                          viewModel.setTab(0)
                        }
                      )
                  }
                } else {
                   Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                      RadarBottomQuickBar(
                        currentRangeNm = rangeNm,
                        onSelectRange = { viewModel.setRadarRange(it) },
                        onOpenList = { showTargetListSheet = true }
                      )
                   }
                }
            }

          }
        }
      }

      // Tüm Çevre Gemileri Listesi (Yatay ve Dikey Ortak)
      if (showTargetListSheet) {
        SurroundingVesselsListDialog(
          targets = targets,
          ownLat = ownLat,
          ownLon = ownLon,
          ownSpeed = ownSpeed,
          ownCog = ownCog,
          selectedTargetId = selectedTarget?.id,
          onSelectTarget = { target ->
            viewModel.selectRadarTarget(target)
            showTargetListSheet = false
          },
          onDismiss = { showTargetListSheet = false }
        )
      }
    }
  }
}

/**
 * Radar Üst Bilgi Barı (Header HUD)
 */
@Composable
private fun RadarHeaderBar(
  ownLat: Double,
  ownLon: Double,
  ownSpeed: Double,
  ownCog: Double,
  targetCount: Int,
  isHeadUp: Boolean,
  onToggleOrientation: () -> Unit,
  onClose: () -> Unit,
  onRefresh: () -> Unit
) {
  Surface(
    color = Color(0xFF0B132B).copy(alpha = 0.95f),
    border = BorderStroke(1.dp, Color(0xFF1E293B)),
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Sol: Radar Başlığı & Kendi Gemi Telemetrisi
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF0284C7).copy(alpha = 0.25f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AIS RADAR PPI", fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SeaGreen.copy(alpha = 0.25f)
            ) {
              Text(
                "CANLI 360°",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = SeaGreen,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          // Coordinate text removed as per user request
          Text(
            text = "Kendi Hız: ${String.format(Locale.US, "%.1f", ownSpeed)} kn",
            fontSize = 9.sp,
            color = Color(0xFF94A3B8)
          )
        }
      }

      // Sağ: Yön Modu Seçici (North-Up / Head-Up), Yenile & Kapat
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // North-Up / Head-Up toggle
        OutlinedButton(
          onClick = onToggleOrientation,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MarineCyan),
          border = BorderStroke(1.dp, MarineCyan.copy(alpha = 0.6f))
        ) {
          Text(
            text = if (isHeadUp) "PRUVA-UP" else "KUZEY-UP",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black
          )
        }

        // Yenile Butonu
        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Taramayı Yenile", tint = MarineCyan, modifier = Modifier.size(16.dp))
        }

        // Kapat Butonu
        IconButton(
          onClick = onClose,
          modifier = Modifier
            .size(28.dp)
            .background(Color(0xFF334155), CircleShape)
            .testTag("btn_close_ais_radar")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Radarı Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

/**
 * Radar PPI Ekran Çizim Mantığı
 */
private fun DrawScope.drawRadarScreen(
  center: Offset,
  radius: Float,
  sweepAngle: Float,
  rangeNm: Double,
  isHeadUp: Boolean,
  ownCog: Double,
  ownSpeed: Double,
  targets: List<RadarAisTarget>,
  selectedTarget: RadarAisTarget?,
  ownLat: Double,
  ownLon: Double,
  hazardPulseAlpha: Float,
  textMeasurer: TextMeasurer,
  deviceHeading: Double
) {
  // 1. Radar Arka Plan Dairesi (Şeffaf Taktik CRT Fosfor Mavisi)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF061427).copy(alpha = 0.55f), Color(0xFF030914).copy(alpha = 0.65f), Color(0xFF01040A).copy(alpha = 0.75f)),
      center = center,
      radius = radius
    ),
    center = center,
    radius = radius
  )

  // 2. Döner Radar Tarama Işını (Phosphor Sweep Beam with Trail)
  val sweepRotation = if (isHeadUp) (sweepAngle - deviceHeading.toFloat()) else sweepAngle
  rotate(sweepRotation, center) {
    // Tarama Işını Hattı
    drawLine(
      color = Color(0xFF38BDF8),
      start = center,
      end = Offset(center.x, center.y - radius),
      strokeWidth = 2.dp.toPx()
    )

    // Tarama İzi (Phosphor Fade Trail - 35 derecelik hafif solan gradyan yay)
    val sweepBrush = Brush.sweepGradient(
      0.0f to Color(0xFF38BDF8).copy(alpha = 0.28f),
      0.1f to Color(0xFF38BDF8).copy(alpha = 0.08f),
      0.2f to Color.Transparent,
      1.0f to Color.Transparent,
      center = center
    )
    drawArc(
      brush = sweepBrush,
      startAngle = -90f - 40f,
      sweepAngle = 40f,
      useCenter = true,
      topLeft = Offset(center.x - radius, center.y - radius),
      size = Size(radius * 2, radius * 2)
    )
  }

  // 3. Eşmerkezli Mesafe Halkaları (Concentric Range Rings) - 4 eşit halka
  val ringSteps = 4
  val ringColor = Color(0xFF1E3A8A).copy(alpha = 0.45f)
  val majorLineColor = Color(0xFF1E3A8A).copy(alpha = 0.35f)

  for (i in 1..ringSteps) {
    val r = radius * (i.toFloat() / ringSteps)
    drawCircle(
      color = ringColor,
      center = center,
      radius = r,
      style = Stroke(width = if (i == ringSteps) 1.5.dp.toPx() else 0.8.dp.toPx())
    )

    // Halka Mesafe Etiketi
    val ringDistNm = (rangeNm / ringSteps) * i
    val distLabel = if (ringDistNm >= 10) "${ringDistNm.toInt()} NM" else String.format(Locale.US, "%.1f NM", ringDistNm)
    val textResult = textMeasurer.measure(
      text = distLabel,
      style = TextStyle(
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF38BDF8).copy(alpha = 0.65f),
        fontFamily = FontFamily.Monospace
      )
    )
    drawText(
      textLayoutResult = textResult,
      topLeft = Offset(center.x + 4.dp.toPx(), center.y - r - 12.dp.toPx())
    )
  }

  // 4. Çapraz ve Ana Kerteriz Eksenleri (000°, 090°, 180°, 270°)
  drawLine(
    color = majorLineColor,
    start = Offset(center.x, center.y - radius),
    end = Offset(center.x, center.y + radius),
    strokeWidth = 1.dp.toPx()
  )
  drawLine(
    color = majorLineColor,
    start = Offset(center.x - radius, center.y),
    end = Offset(center.x + radius, center.y),
    strokeWidth = 1.dp.toPx()
  )

  // 5. Pusula Gülü & Dış Halka Derece Çentikleri (0°..350°, her 30° ana, her 10° küçük)
  val compassRotation = if (isHeadUp) -deviceHeading.toFloat() else 0f
  rotate(compassRotation, center) {
    for (deg in 0 until 360 step 10) {
      val isMajor = deg % 30 == 0
      val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
      val rad = Math.toRadians(deg.toDouble())
      val startX = center.x + (radius - tickLength) * sin(rad).toFloat()
      val startY = center.y - (radius - tickLength) * cos(rad).toFloat()
      val endX = center.x + radius * sin(rad).toFloat()
      val endY = center.y - radius * cos(rad).toFloat()

      drawLine(
        color = if (isMajor) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color(0xFF38BDF8).copy(alpha = 0.35f),
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = if (isMajor) 1.4.dp.toPx() else 0.8.dp.toPx()
      )

      if (isMajor) {
        val label = when (deg) {
          0 -> "K / 000°"
          90 -> "D / 090°"
          180 -> "G / 180°"
          270 -> "B / 270°"
          else -> String.format(Locale.US, "%03d°", deg)
        }
        val textResult = textMeasurer.measure(
          text = label,
          style = TextStyle(
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (deg == 0) Color(0xFFEF4444) else Color(0xFF38BDF8),
            fontFamily = FontFamily.Monospace
          )
        )
        val textRadius = radius - tickLength - 10.dp.toPx()
        val textX = center.x + textRadius * sin(rad).toFloat() - (textResult.size.width / 2f)
        val textY = center.y - textRadius * cos(rad).toFloat() - (textResult.size.height / 2f)
        drawText(textLayoutResult = textResult, topLeft = Offset(textX, textY))
      }
    }
  }

  // 6. Kendi Gemimiz (Own Ship Marker): Merkezde Üçgen / Gemi Simgesi + Pruva Çizgisi
  val ownShipColor = Color(0xFF38BDF8)
  val ownShipHdgRotation = if (isHeadUp) (ownCog - deviceHeading).toFloat() else ownCog.toFloat()

  rotate(ownShipHdgRotation, center) {
    // Pruva Çizgisi (Heading Line)
    drawLine(
      color = Color(0xFFF59E0B).copy(alpha = 0.85f),
      start = center,
      end = Offset(center.x, center.y - radius * 0.95f),
      strokeWidth = 1.2.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )

    // Kendi Gemisi İkonu (Denizcilik Şekli)
    val shipPath = Path().apply {
      moveTo(center.x, center.y - 12.dp.toPx()) // Burun
      lineTo(center.x + 6.dp.toPx(), center.y + 7.dp.toPx()) // Sancak kıç
      lineTo(center.x, center.y + 4.dp.toPx()) // Kıç girinti
      lineTo(center.x - 6.dp.toPx(), center.y + 7.dp.toPx()) // İskele kıç
      close()
    }
    drawPath(path = shipPath, color = ownShipColor)
    drawPath(path = shipPath, color = Color.White, style = Stroke(width = 1.dp.toPx()))
    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
  }

  // 7. Çevre AIS Hedefleri Çizimi (AIS Targets on Radar PPI)
  targets.forEach { target ->
    val distNm = target.distanceNmFrom(ownLat, ownLon)
    if (distNm > rangeNm * 1.05) return@forEach // Menzil dışı

    val trueBearingDeg = target.bearingFrom(ownLat, ownLon)
    val relativeBearingDeg = if (isHeadUp) (trueBearingDeg - deviceHeading + 360.0) % 360.0 else trueBearingDeg

    // Polar -> Kartezyen Ekran Konumu
    val distRatio = (distNm / rangeNm).toFloat()
    val rad = Math.toRadians(relativeBearingDeg)
    val tgtX = center.x + radius * distRatio * sin(rad).toFloat()
    val tgtY = center.y - radius * distRatio * cos(rad).toFloat()
    val tgtOffset = Offset(tgtX, tgtY)

    val isSelected = selectedTarget?.id == target.id
    val isHazard = target.isHazardous

    // Hedef Rengi Belirleme
    val tgtColor = when {
      isHazard -> Color(0xFFEF4444) // Çatışma riski kırmızı
      target.isAnchored -> Color(0xFFF59E0B) // Demirde sarı/kehribar
      else -> Color(0xFF10B981) // Normal güvenli yeşil
    }

    // Seçili Hedef Edinme Kutusu (Target Acquisition Brackets / Ring)
    if (isSelected) {
      drawCircle(
        color = Color(0xFF38BDF8),
        center = tgtOffset,
        radius = 18.dp.toPx(),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
      )
    }

    // Çatışma Tehlikesi Pulsing Dairesi
    if (isHazard) {
      drawCircle(
        color = Color(0xFFEF4444).copy(alpha = hazardPulseAlpha * 0.4f),
        center = tgtOffset,
        radius = 22.dp.toPx()
      )
    }

    // Hedef Simgesi: AIS Üçgeni (Kendi COG rotasına göre döner)
    val tgtRotation = if (isHeadUp) (target.cogDegrees - deviceHeading).toFloat() else target.cogDegrees.toFloat()
    rotate(tgtRotation, tgtOffset) {
      val triPath = Path().apply {
        moveTo(tgtOffset.x, tgtOffset.y - 7.dp.toPx()) // Baş
        lineTo(tgtOffset.x + 4.5.dp.toPx(), tgtOffset.y + 5.dp.toPx())
        lineTo(tgtOffset.x - 4.5.dp.toPx(), tgtOffset.y + 5.dp.toPx())
        close()
      }
      drawPath(path = triPath, color = tgtColor)
      drawPath(path = triPath, color = Color.White, style = Stroke(width = 0.8.dp.toPx()))

      // Hız Vektörü (Speed Vector Line - SOG'a oranlı lider çizgi)
      if (target.sogKnots > 0.5) {
        val vectorLen = (target.sogKnots * 1.5).coerceIn(8.0, 30.0).dp.toPx()
        drawLine(
          color = tgtColor,
          start = tgtOffset,
          end = Offset(tgtOffset.x, tgtOffset.y - vectorLen),
          strokeWidth = 1.2.dp.toPx()
        )
      }
    }

    // Hedef Etiketi (Gemi Adı veya MMSI + Sürat)
    val labelText = "${target.name.take(10)} ${String.format(Locale.US, "%.0f", target.sogKnots)}k"
    val labelResult = textMeasurer.measure(
      text = labelText,
      style = TextStyle(
        fontSize = 7.5.sp,
        fontWeight = if (isSelected || isHazard) FontWeight.Black else FontWeight.Bold,
        color = if (isHazard) Color(0xFFF87171) else (if (isSelected) Color(0xFF38BDF8) else Color(0xFFE2E8F0)),
        fontFamily = FontFamily.Monospace
      )
    )
    drawText(
      textLayoutResult = labelResult,
      topLeft = Offset(tgtX + 8.dp.toPx(), tgtY - 8.dp.toPx())
    )
  }
}

/**
 * Alt Hızlı Menzil Seçim Şeridi
 */
@Composable
private fun RadarBottomQuickBar(
  currentRangeNm: Double,
  onSelectRange: (Double) -> Unit,
  onOpenList: () -> Unit
) {
  Surface(
    color = Color(0xFF0B132B).copy(alpha = 0.95f),
    border = BorderStroke(1.dp, Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(1.0, 3.0, 6.0, 12.0, 24.0).forEach { r ->
          val isSelected = currentRangeNm == r
          FilterChip(
            selected = isSelected,
            onClick = { onSelectRange(r) },
            label = { Text("${r.toInt()} NM", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MarineCyan,
              selectedLabelColor = Color(0xFF0B132B),
              containerColor = Color(0xFF1E293B),
              labelColor = Color(0xFF94A3B8)
            ),
            modifier = Modifier.height(26.dp)
          )
        }
      }

      Button(
        onClick = onOpenList,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
      ) {
        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("Liste", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Seçili Gemi Bilgi Kartı (Tactical Radar Target HUD Sheet)
 */
@Composable
private fun RadarTargetDetailCard(
  target: RadarAisTarget,
  ownLat: Double,
  ownLon: Double,
  ownSpeed: Double,
  ownCog: Double,
  onClose: () -> Unit,
  onNavigateToMap: () -> Unit,
  onCalculateEta: () -> Unit
) {
  val distNm = target.distanceNmFrom(ownLat, ownLon)
  val brgDeg = target.bearingFrom(ownLat, ownLon)
  val (cpaNm, tcpaMin) = target.calculateCpaTcpa(ownLat, ownLon, ownSpeed, ownCog)
  val isHazard = target.isHazardous

  Card(
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
    border = BorderStroke(1.dp, if (isHazard) DangerRed else MarineCyan),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_radar_selected_target")
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Üst Başlık & Çatışma Durumu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (target.isAnchored) Icons.Default.Anchor else Icons.Default.DirectionsBoat,
            contentDescription = null,
            tint = if (isHazard) DangerRed else MarineCyan,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = target.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
              color = Color.White
            )
            Text(
              text = "${target.shipType} • Bayrak: ${target.flag} • MMSI: ${target.mmsi}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = Color(0xFF94A3B8))
            )
          }
        }

        // Çatışma Durumu Rozeti
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (isHazard) DangerRedDark else SeaGreen.copy(alpha = 0.25f)
        ) {
          Text(
            text = if (isHazard) "⚠️ ÇATIŞMA RİSKİ" else "GÜVENLİ GEÇİŞ",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = if (isHazard) Color.White else SeaGreen,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)

      // Telemetri Izgarası: Mesafe, Kerteriz, CPA, TCPA, Sürat, Rota
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        MetricBox("Mesafe", String.format(Locale.US, "%.2f NM", distNm), Color.White, Modifier.weight(1f))
        MetricBox("Kerteriz", String.format(Locale.US, "%03d°", brgDeg.toInt()), MarineCyan, Modifier.weight(1f))
        MetricBox("Sürat (SOG)", "${String.format(Locale.US, "%.1f", target.sogKnots)} kn", Color.White, Modifier.weight(1f))
        MetricBox("Rota (COG)", String.format(Locale.US, "%03d°", target.cogDegrees.toInt()), Color.White, Modifier.weight(1f))
        MetricBox(
          "CPA",
          String.format(Locale.US, "%.2f NM", cpaNm),
          if (isHazard) DangerRed else SeaGreen,
          Modifier.weight(1f)
        )
        MetricBox(
          "TCPA",
          if (tcpaMin < 0) "--" else "${String.format(Locale.US, "%.0f", tcpaMin)} dk",
          if (isHazard) DangerRed else Color.White,
          Modifier.weight(1f)
        )
      }

      // Detay Bilgi Satırı: Hedef, ETA & Boyutlar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Hedef: ${target.destination.take(14)} (${target.eta})",
          fontSize = 9.sp,
          color = Color(0xFFCBD5E1)
        )
        Text(
          text = "Boy: ${target.loaMeters.toInt()}m • En: ${target.beamMeters.toInt()}m • Draft: ${target.draftMeters}m",
          fontSize = 8.5.sp,
          color = Color(0xFF94A3B8)
        )
      }

      // Eylem Butonları
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Rotaya / ETA'ya Ekle
        Button(
          onClick = onCalculateEta,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.weight(1f).height(32.dp).testTag("btn_target_add_eta")
        ) {
          Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ETA Hesapla", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }

        // Haritada Göster
        OutlinedButton(
          onClick = onNavigateToMap,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MarineCyan),
          border = BorderStroke(1.dp, MarineCyan.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.weight(1f).height(32.dp).testTag("btn_target_show_map")
        ) {
          Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Haritada Göster", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }

        // Kapat
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
private fun MetricBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(4.dp),
    color = Color(0xFF111C33),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(label, fontSize = 7.5.sp, color = Color(0xFF94A3B8), maxLines = 1)
      Text(value, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = valueColor, maxLines = 1)
    }
  }
}

/**
 * Tüm Çevre Gemileri Listesi Diyaloğu
 */
@Composable
private fun SurroundingVesselsListDialog(
  targets: List<RadarAisTarget>,
  ownLat: Double,
  ownLon: Double,
  ownSpeed: Double,
  ownCog: Double,
  selectedTargetId: String?,
  onSelectTarget: (RadarAisTarget) -> Unit,
  onDismiss: () -> Unit
) {
  var filter by remember { mutableStateOf("ALL") }

  val filteredTargets = remember(targets, filter) {
    when (filter) {
      "HAZARD" -> targets.filter { it.isHazardous }
      "MOVING" -> targets.filter { !it.isAnchored }
      "ANCHORED" -> targets.filter { it.isAnchored }
      else -> targets
    }.sortedBy { it.distanceNmFrom(ownLat, ownLon) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth(0.95f)
      .fillMaxHeight(0.80f)
      .testTag("dialog_surrounding_vessels_list"),
    containerColor = Color(0xFF0F172A),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Çevre AIS Gemileri (${targets.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Filtre Sekmeleri
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          FilterChip(
            selected = filter == "ALL",
            onClick = { filter = "ALL" },
            label = { Text("Tümü (${targets.size})", fontSize = 8.5.sp) },
            modifier = Modifier.height(24.dp)
          )
          FilterChip(
            selected = filter == "HAZARD",
            onClick = { filter = "HAZARD" },
            label = { Text("⚠️ Riskli (${targets.count { it.isHazardous }})", fontSize = 8.5.sp) },
            modifier = Modifier.height(24.dp)
          )
          FilterChip(
            selected = filter == "MOVING",
            onClick = { filter = "MOVING" },
            label = { Text("Seyirde (${targets.count { !it.isAnchored }})", fontSize = 8.5.sp) },
            modifier = Modifier.height(24.dp)
          )
          FilterChip(
            selected = filter == "ANCHORED",
            onClick = { filter = "ANCHORED" },
            label = { Text("Demirde (${targets.count { it.isAnchored }})", fontSize = 8.5.sp) },
            modifier = Modifier.height(24.dp)
          )
        }

        // Hedef Listesi
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(filteredTargets, key = { it.id }) { target ->
            val dist = target.distanceNmFrom(ownLat, ownLon)
            val brg = target.bearingFrom(ownLat, ownLon)
            val (cpa, tcpa) = target.calculateCpaTcpa(ownLat, ownLon, ownSpeed, ownCog)
            val isSelected = target.id == selectedTargetId

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
              border = BorderStroke(
                1.dp,
                if (target.isHazardous) DangerRed else (if (isSelected) MarineCyan else Color(0xFF334155))
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectTarget(target) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = target.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      color = Color.White
                    )
                    if (target.isHazardous) {
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("⚠️ CPA!", fontSize = 8.sp, fontWeight = FontWeight.Black, color = DangerRed)
                    }
                  }
                  Text(
                    text = "${target.shipType} • MMSI: ${target.mmsi}",
                    fontSize = 8.5.sp,
                    color = Color(0xFF94A3B8)
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "${String.format(Locale.US, "%.2f NM", dist)} • ${String.format(Locale.US, "%03d°", brg.toInt())}",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = MarineCyan
                  )
                  Text(
                    text = "${String.format(Locale.US, "%.1f", target.sogKnots)} kn • CPA: ${String.format(Locale.US, "%.1f", cpa)} NM",
                    fontSize = 8.5.sp,
                    color = if (target.isHazardous) DangerRed else Color(0xFFCBD5E1)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {}
  )
}

@Composable
fun RadarCanvasContent(
  viewModel: com.example.ui.TideNavViewModel,
  targets: List<com.example.model.RadarAisTarget>,
  rangeNm: Double,
  isHeadUp: Boolean,
  ownCog: Double,
  ownSpeed: Double,
  selectedTarget: com.example.model.RadarAisTarget?,
  ownLat: Double,
  ownLon: Double,
  sweepAngle: Float,
  hazardPulseAlpha: Float,
  textMeasurer: androidx.compose.ui.text.TextMeasurer,
  deviceHeading: Double,
  onShowTargetList: () -> Unit
) {
  var radarCenter by remember { mutableStateOf(Offset.Zero) }
  var radarRadiusPx by remember { mutableStateOf(100f) }
  val currentDeviceHeading by rememberUpdatedState(deviceHeading)
  val currentOwnLat by rememberUpdatedState(ownLat)
  val currentOwnLon by rememberUpdatedState(ownLon)

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(targets, rangeNm, isHeadUp, radarCenter, radarRadiusPx) {
        detectTapGestures { tapOffset ->
          val dx = tapOffset.x - radarCenter.x
          val dy = tapOffset.y - radarCenter.y
          val distPx = kotlin.math.sqrt(dx * dx + dy * dy)

          if (distPx <= 28f) {
            viewModel.selectRadarTarget(null)
            return@detectTapGestures
          }

          val tapDistNm = (distPx / radarRadiusPx) * rangeNm
          var angleDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), -dy.toDouble()))
          if (angleDeg < 0) angleDeg += 360.0
          val trueBearingDeg = if (isHeadUp) (angleDeg + currentDeviceHeading) % 360.0 else angleDeg

          val hitTarget = targets.minByOrNull { tgt ->
            val tgtDistNm = tgt.distanceNmFrom(currentOwnLat, currentOwnLon)
            val tgtBrgDeg = tgt.bearingFrom(currentOwnLat, currentOwnLon)
            val dDist = kotlin.math.abs(tgtDistNm - tapDistNm)
            var dAngle = kotlin.math.abs(tgtBrgDeg - trueBearingDeg)
            if (dAngle > 180) dAngle = 360 - dAngle
            dDist * 3.0 + (dAngle / 30.0)
          }

          if (hitTarget != null) {
            val tgtDistNm = hitTarget.distanceNmFrom(currentOwnLat, currentOwnLon)
            if (tgtDistNm <= rangeNm * 1.15) {
              viewModel.selectRadarTarget(hitTarget)
            }
          }
        }
      }
  ) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = kotlin.math.min(size.width, size.height) / 2f - 8.dp.toPx() // Maksimum büyüklük için margin'i azalttık (8dp)
    radarCenter = center
    radarRadiusPx = radius

    if (radius > 10f) {
      drawRadarScreen(
        center = center,
        radius = radius,
        sweepAngle = sweepAngle,
        rangeNm = rangeNm,
        isHeadUp = isHeadUp,
        ownCog = ownCog,
        ownSpeed = ownSpeed,
        targets = targets,
        selectedTarget = selectedTarget,
        ownLat = ownLat,
        ownLon = ownLon,
        hazardPulseAlpha = hazardPulseAlpha,
        textMeasurer = textMeasurer,
        deviceHeading = deviceHeading
      )
    }
  }

  // Hızlı Menzil Butonları & Taktik Kontroller (Canvas Üzerinde)
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 10.dp, top = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MarineCyan.copy(alpha = 0.5f))
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("MENZİL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MarineCyan)
          Text("${rangeNm.toInt()} NM", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }

      SmallFloatingActionButton(
        onClick = {
          val next = when (rangeNm) {
            24.0 -> 12.0
            12.0 -> 6.0
            6.0 -> 3.0
            3.0 -> 1.0
            else -> 1.0
          }
          viewModel.setRadarRange(next)
        },
        containerColor = Color(0xFF1E293B),
        contentColor = Color.White,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = "Yakınlaştır", modifier = Modifier.size(18.dp))
      }

      SmallFloatingActionButton(
        onClick = {
          val next = when (rangeNm) {
            1.0 -> 3.0
            3.0 -> 6.0
            6.0 -> 12.0
            12.0 -> 24.0
            else -> 24.0
          }
          viewModel.setRadarRange(next)
        },
        containerColor = Color(0xFF1E293B),
        contentColor = Color.White,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.Remove, contentDescription = "Uzaklaştır", modifier = Modifier.size(18.dp))
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = 10.dp, top = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.End
    ) {
      Button(
        onClick = onShowTargetList,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF0F172A).copy(alpha = 0.9f),
          contentColor = MarineCyan
        ),
        border = BorderStroke(1.dp, MarineCyan.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Gemiler (${targets.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      val hazardCount = targets.count { it.isHazardous }
      if (hazardCount > 0) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DangerRedDark.copy(alpha = 0.9f),
          border = BorderStroke(1.dp, DangerRed)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("$hazardCount ÇATIŞMA RİSKİ", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
        }
      }
    }
  }
}
