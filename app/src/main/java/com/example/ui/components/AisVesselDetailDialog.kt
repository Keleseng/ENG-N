package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AisVesselData
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AisVesselDetailDialog(
  aisData: AisVesselData,
  onDismiss: () -> Unit,
  onApplyToApp: (AisVesselData) -> Unit,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(0) } // 0: AIS Özellikleri & Telemetri, 1: AIS Canlı Harita Sayfası

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = cardBg,
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      modifier = modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .testTag("dialog_ais_vessel_details")
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // 1. Üst Başlık & Kapatma Butonu
        Surface(
          color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(PrimaryBlue, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = aisData.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                  color = textPrimary
                )
                Text(
                  text = "MMSI: ${aisData.mmsi} • IMO: ${aisData.imo} • ${aisData.flag}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = textSecondary
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("btn_close_ais_dialog")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = textPrimary)
            }
          }
        }

        // 2. Tab Seçici (AIS Telemetrisi / AIS Canlı Harita)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) Color(0xFF0B1120) else Color(0xFFF1F5F9))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { selectedTab = 0 },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedTab == 0) PrimaryBlue else (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
              contentColor = if (selectedTab == 0) Color.White else textPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("tab_ais_specs")
          ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AIS Özellikleri", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }

          Button(
            onClick = { selectedTab = 1 },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedTab == 1) Color(0xFF0284C7) else (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
              contentColor = if (selectedTab == 1) Color.White else textPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("tab_ais_vesselfinder_web")
          ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("MarineTraffic Harita", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }

        // 3. İçerik Alanı
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          if (selectedTab == 0) {
            // AIS Özet Telemetri & Gemi Kartları
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Canlı Durum ve Seyir Rozeti
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = subtleBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "🛰️ CANLI AIS SEYİR DURUMU",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                      color = if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
                      ) {
                        Text(
                          text = "🌐 AIS TELEMETRİ",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                          color = textMuted,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkMode) Color(0xFF065F46) else Color(0xFFD1FAE5)
                      ) {
                        Text(
                          text = "CANLI SİNYAL",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                          color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46),
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Text(
                    text = aisData.status,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Hedef: ${aisData.destination}",
                      style = MaterialTheme.typography.labelSmall,
                      color = textSecondary
                    )
                    Text(
                      text = "Tahmini Varış (ETA): ${aisData.eta}",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                  }
                }
              }

              // Konum & Seyir Verileri (Lat/Lon, SOG, COG)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                AisStatBox(
                  label = "ENLEM & BOYLAM (DENİZ GPS DMS)",
                  value = "${com.example.model.LocationPresets.formatMarineLatDMS(aisData.latitude)}\n${com.example.model.LocationPresets.formatMarineLonDMS(aisData.longitude)}",
                  icon = Icons.Default.Place,
                  accentColor = if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue,
                  isDarkMode = isDarkMode,
                  modifier = Modifier.weight(1f)
                )
                AisStatBox(
                  label = "SOG (HIZ) & COG (ROTA)",
                  value = "${aisData.sogKnots} kn\n${String.format(Locale.US, "%03d°", aisData.headingDegrees)}",
                  icon = Icons.Default.Speed,
                  accentColor = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                  isDarkMode = isDarkMode,
                  modifier = Modifier.weight(1f)
                )
              }

              // Gemi Boyutları ve Teknik Özellikleri
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = subtleBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "🚢 GEMİ TEKNİK BOYUT VE BİLGİLERİ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
                  )

                  AisRowInfo(label = "Gemi Tipi", value = aisData.shipType, isDarkMode = isDarkMode)
                  AisRowInfo(label = "Bayrak", value = aisData.flag, isDarkMode = isDarkMode)
                  AisRowInfo(label = "MMSI Numarası", value = aisData.mmsi, isDarkMode = isDarkMode)
                  AisRowInfo(label = "IMO Numarası", value = aisData.imo, isDarkMode = isDarkMode)
                  AisRowInfo(label = "Çağrı İşareti (Call Sign)", value = aisData.callSign, isDarkMode = isDarkMode)
                  AisRowInfo(label = "Tam Boy (LOA)", value = "${aisData.loaMeters} m", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Genişlik (Beam)", value = "${aisData.beamMeters} m", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Su Çekimi (Draft)", value = "${aisData.draftMeters} m", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Gros Tonaj (GT)", value = "${aisData.grossTonnage} GT", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Detveyt Tonaj (DWT)", value = "${aisData.deadweightTon} DWT", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Yapım Yılı", value = "${aisData.yearBuilt}", isDarkMode = isDarkMode)
                  AisRowInfo(label = "Son Sinyal", value = aisData.lastReportedTime, isDarkMode = isDarkMode)
                }
              }

              // MyShipTracking Doğrudan Tarayıcı Butonu
              OutlinedButton(
                onClick = {
                  val targetUrl = if (aisData.myShipTrackingUrl.isNotBlank()) aisData.myShipTrackingUrl else "https://www.myshiptracking.com/?mmsi=${aisData.mmsi}"
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                  context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("MyShipTracking Haritasında Aç", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          } else {
            // Canlı Gemi / Harita WebView (MyShipTracking)
            var dialogWebViewRef by remember { mutableStateOf<WebView?>(null) }
            DisposableEffect(Unit) {
              onDispose {
                try {
                  dialogWebViewRef?.stopLoading()
                  dialogWebViewRef?.loadUrl("about:blank")
                  dialogWebViewRef?.clearHistory()
                  dialogWebViewRef?.removeAllViews()
                  dialogWebViewRef?.destroy()
                } catch (e: Exception) {
                  // Ignore cleanup errors
                }
              }
            }
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  settings.databaseEnabled = true
                  settings.allowFileAccess = false
                  settings.allowContentAccess = false
                  settings.mediaPlaybackRequiresUserGesture = true
                  settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                  settings.loadWithOverviewMode = true
                  settings.useWideViewPort = true
                  settings.setSupportZoom(true)
                  settings.builtInZoomControls = true
                  settings.displayZoomControls = false
                  settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                  webChromeClient = WebChromeClient()
                  webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                      super.onPageFinished(view, url)
                      if (isDarkMode) {
                        view?.evaluateJavascript(
                          """
                          (function() {
                            var style = document.createElement('style');
                            style.innerHTML = 'html { filter: invert(100%) hue-rotate(180deg) brightness(85%) contrast(85%); background: #121212 !important; } iframe, img, canvas { filter: invert(100%) hue-rotate(180deg) !important; }';
                            document.head.appendChild(style);
                          })();
                          """.trimIndent(),
                          null
                        )
                      }
                    }
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                      try {
                        view?.destroy()
                      } catch (e: Exception) {
                        // safe destroy
                      }
                      return true // Prevents app crash
                    }
                  }
                  val dialogUrl = if (aisData.myShipTrackingUrl.isNotBlank()) aisData.myShipTrackingUrl else "https://www.myshiptracking.com/?mmsi=${aisData.mmsi}"
                  loadUrl(dialogUrl)
                  dialogWebViewRef = this
                }
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        // 4. Alt Eylem Çubuğu: "Bu Gemiyi Uygulamaya Uygula & Takip Et"
        Surface(
          color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                onApplyToApp(aisData)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = SeaGreenDark,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("btn_apply_ais_to_app")
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Bu Gemiyi Seyirde Takip Et", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AisStatBox(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  isDarkMode: Boolean,
  modifier: Modifier = Modifier
) {
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = subtleBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = textMuted)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 12.sp), color = accentColor)
    }
  }
}

@Composable
private fun AisRowInfo(
  label: String,
  value: String,
  isDarkMode: Boolean
) {
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
      color = textSecondary,
      softWrap = true,
      modifier = Modifier.weight(1.2f)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
      color = textPrimary,
      softWrap = true,
      textAlign = androidx.compose.ui.text.style.TextAlign.End,
      modifier = Modifier.weight(1f)
    )
  }
}
