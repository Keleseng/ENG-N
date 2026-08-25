package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.LocationPresets
import com.example.model.PortLocation
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AisVesselDetailDialog
import com.example.engine.AisTrackingEngine
import com.example.ui.theme.*
import java.util.Locale

/**
 * VesselFinder tabanlı canlı deniz ve gemi trafiği haritası (vesselfinder.com).
 * Sadece canlı harita görünümü (https://www.vesselfinder.com/tr/?mmsi=222111447).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarineWeatherMapView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  var showAnchorConfirmDialog by remember { mutableStateOf(false) }
  var showMobConfirmDialog by remember { mutableStateOf(false) }
  var showMobProcedureSheet by remember { mutableStateOf(false) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var lastLoadedUrl by remember { mutableStateOf("") }
  var webViewCrashKey by remember { mutableIntStateOf(0) }

  DisposableEffect(Unit) {
    onDispose {
      try {
        webViewRef?.stopLoading()
        webViewRef?.loadUrl("about:blank")
        webViewRef?.clearHistory()
        webViewRef?.removeAllViews()
        webViewRef?.destroy()
        webViewRef = null
      } catch (e: Exception) {
        // Safe disposal
      }
    }
  }

  val context = LocalContext.current
  val lat = uiState.latStr.toDoubleOrNull() ?: 38.4410
  val lon = uiState.lonStr.toDoubleOrNull() ?: 27.1438
  val mob = uiState.mobEvent
  val anchor = uiState.anchorEvent
  val weather = uiState.marineWeather

  val mmsi = uiState.mmsiStr.trim().ifBlank { "222111447" }

  // VesselFinder Canlı Harita URL'si
  val vesselFinderMapUrl = remember(mmsi) {
    "https://www.vesselfinder.com/tr/?mmsi=$mmsi"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A))
  ) {

    // ══════════════════════════════════════════════════════════════════════
    // 1. ACİL DURUM VE DEMİRLEME AKTİF BİLGİ ŞERİTLERİ (HUD ALERT BANNERS)
    // ══════════════════════════════════════════════════════════════════════
    if (mob.isActive) {
      val mobDistNm = mob.calculateDistanceNm(lat, lon)
      val mobDistGomina = mob.calculateDistanceGomina(lat, lon)
      val mobDistMeters = mob.calculateDistanceMeters(lat, lon)
      val mobBearing = mob.calculateBearingDegrees(lat, lon)

      Surface(
        color = DangerRed,
        shadowElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showMobProcedureSheet = true }
          .testTag("banner_mob_active")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color.White, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Emergency, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "🚨 MOB (DENİZE ADAM DÜŞTÜ) - ${mob.timeFormatted}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp),
                color = Color.White
              )
              Text(
                text = "Mesafe: ${String.format(Locale.US, "%.1f", mobDistGomina)} Gomina (${String.format(Locale.US, "%.2f", mobDistNm)} NM / ${mobDistMeters.toInt()}m) • Kerteriz: ${String.format(Locale.US, "%03d°", mobBearing)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = Color(0xFFFFECEC)
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
              onClick = { showMobProcedureSheet = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DangerRed),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text("VHF / Prosedür", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
              onClick = { viewModel.cancelMob() },
              modifier = Modifier.size(26.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    if (anchor.isAnchored) {
      val anchorDistGomina = anchor.calculateDistanceGomina(lat, lon)
      val anchorDistMeters = anchor.calculateDistanceMeters(lat, lon)
      val isDragging = anchor.isDragging(lat, lon)

      Surface(
        color = if (isDragging) WarningAmber else Color(0xFF065F46),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDragging) DangerRed else Color(0xFF34D399)),
        shadowElevation = 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_anchor_active")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (isDragging) Icons.Default.Warning else Icons.Default.Anchor,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (isDragging) "⚠️ DEMİR TARAMA UYARISI!" else "⚓ DEMİR NÖBETİ AKTİF",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp),
                color = Color.White
              )
              Text(
                text = "Mesafe: ${String.format(Locale.US, "%.2f", anchorDistGomina)} Gomina (${anchorDistMeters.toInt()}m) / İzin Verilen: ${anchor.safeSwingingRadiusGomina} Gomina",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }

          Button(
            onClick = { viewModel.liftAnchor() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("Demir Al", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. CANLI HARİTA WEBVIEW ALANI (SADECE HARİTA)
    // ══════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      key(webViewCrashKey) {
        AndroidView(
          modifier = Modifier
            .fillMaxSize()
            .testTag("vesselfinder_webview"),
          factory = { ctx ->
            WebView(ctx).apply {
              setLayerType(View.LAYER_TYPE_SOFTWARE, null)
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.databaseEnabled = true
              settings.allowFileAccess = false
              settings.allowContentAccess = false
              settings.mediaPlaybackRequiresUserGesture = true
              settings.setGeolocationEnabled(true)
              settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              settings.loadWithOverviewMode = true
              settings.useWideViewPort = true
              settings.setSupportZoom(true)
              settings.builtInZoomControls = true
              settings.displayZoomControls = false
              settings.cacheMode = WebSettings.LOAD_DEFAULT
              settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
              webChromeClient = WebChromeClient()
              webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                  try {
                    view?.stopLoading()
                    view?.loadUrl("about:blank")
                    view?.destroy()
                  } catch (e: Exception) {
                    // Safe cleanup
                  }
                  webViewRef = null
                  lastLoadedUrl = ""
                  webViewCrashKey++
                  return true // CRITICAL: Prevent host app crash
                }
              }
              loadUrl(vesselFinderMapUrl)
              lastLoadedUrl = vesselFinderMapUrl
              webViewRef = this
            }
          },
          update = { webView ->
            if (lastLoadedUrl != vesselFinderMapUrl) {
              lastLoadedUrl = vesselFinderMapUrl
              webView.loadUrl(vesselFinderMapUrl)
            }
            webViewRef = webView
          }
        )
      }

      // Sağ Alt: Hızlı Eylemler (VesselFinder Dış Link, Yenile)
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
      ) {
        // Tarayıcıda Aç
        FloatingActionButton(
          onClick = {
            val intent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse(vesselFinderMapUrl)
            )
            context.startActivity(intent)
          },
          containerColor = Color(0xFF1E293B),
          contentColor = Color(0xFF38BDF8),
          modifier = Modifier.size(42.dp).testTag("fab_open_vesselfinder_web")
        ) {
          Icon(Icons.Default.OpenInBrowser, contentDescription = "VesselFinder Web", modifier = Modifier.size(20.dp))
        }

        // Harita Yenile FAB
        FloatingActionButton(
          onClick = {
            webViewRef?.reload()
          },
          containerColor = Color(0xFF0284C7),
          contentColor = Color.White,
          modifier = Modifier.size(48.dp).testTag("fab_refresh_map")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Yenile")
        }
      }
    }
  }

  // ══════════════════════════════════════════════════════════════════════
  // DIALOGLAR: MOB & DEMİRLEME ONAYLARI
  // ══════════════════════════════════════════════════════════════════════
  if (showMobConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showMobConfirmDialog = false },
      icon = {
        Icon(Icons.Default.Emergency, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("DENİZE ADAM DÜŞTÜ (MOB)?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = DangerRed)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "Anlık GPS mevkii kaydedilecek ve gemiye göre dönüş rotası/mesafesi hesaplanacaktır.",
            style = MaterialTheme.typography.bodyMedium
          )
          Surface(
            color = CardSubtle,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
          ) {
            Text(
              text = "Mevcut Konum: ${LocationPresets.formatMarineCoordinates(lat, lon)}\nSürat: ${uiState.speedStr} kn | Pruva: ${uiState.headingDegreesStr}°",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.triggerMob()
            showMobConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
        ) {
          Text("EVET, MOB KAYDET", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showMobConfirmDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  if (showAnchorConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showAnchorConfirmDialog = false },
      icon = {
        Icon(Icons.Default.Anchor, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("Demir Atılsın mı?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "Mevcut GPS konumunuza demirleme işareti konulacak ve demir nöbeti (salma dairesi kontrolü) başlatılacaktır.",
            style = MaterialTheme.typography.bodyMedium
          )
          Surface(
            color = CardSubtle,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
          ) {
            Text(
              text = "Demirleme Mevkii: ${LocationPresets.formatMarineCoordinates(lat, lon)}\nHarita Derinliği: ${uiState.chartedDepthStr}m",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.dropAnchor()
            showAnchorConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)
        ) {
          Text("Demir At (Nöbeti Başlat)", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAnchorConfirmDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  if (showMobProcedureSheet) {
    AlertDialog(
      onDismissRequest = { showMobProcedureSheet = false },
      icon = {
        Icon(Icons.Default.Emergency, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("MOB Kurtarma & VHF MAYDAY", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = DangerRed)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(color = DangerRedLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("MOB Mevkii: ${LocationPresets.formatMarineCoordinates(mob.latitude, mob.longitude)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = DangerRedDark)
              Text("Zaman: ${mob.timeFormatted} • Mesafe: ${String.format(Locale.US, "%.1f", mob.calculateDistanceGomina(lat, lon))} Gomina (${String.format(Locale.US, "%.2f", mob.calculateDistanceNm(lat, lon))} NM)", style = MaterialTheme.typography.bodySmall, color = DangerRedDark)
              Text("Kerteriz: ${String.format(Locale.US, "%03d°", mob.calculateBearingDegrees(lat, lon))}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = DangerRedDark)
            }
          }

          Text("1. Williamson Dönüşü:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          Text("• Dümeni adamın düştüğü tarafa tam alabanda basınız.\n• Başlangıç rotasından 60° sapınca ters tarafa tam alabanda basınız.\n• Karşı rotaya (başlangıç rotası + 180°) dönüldüğünde dümeni ortalayınız.", style = MaterialTheme.typography.bodySmall, color = TextPrimary)

          Text("2. VHF Telsiz Bildirimi (Kanal 16):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          Text("• 'MAYDAY MAYDAY MAYDAY - This is [${uiState.vesselName.ifBlank { "GEMİ ADI" }}] - MAN OVERBOARD at position ${LocationPresets.formatMarineCoordinates(mob.latitude, mob.longitude)} - Request immediate assistance!'", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.cancelMob()
            showMobProcedureSheet = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SeaGreen, contentColor = Color.White)
        ) {
          Text("Kurtarıldı / Alarmı Kapat", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showMobProcedureSheet = false }) {
          Text("Kapat")
        }
      }
    )
  }

  // AIS Gemi Detay Dialogu
  if (uiState.showAisDetailDialog) {
    val aisToShow = uiState.activeAisVesselData ?: AisTrackingEngine.getMarineTraffic10481795ShipData()
    AisVesselDetailDialog(
      aisData = aisToShow,
      onDismiss = { viewModel.setShowAisDetailDialog(false) },
      onApplyToApp = { selectedShip ->
        viewModel.applyAisVesselData(selectedShip)
      }
    )
  }
}
