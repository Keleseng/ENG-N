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
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(0) } // 0: AIS Özellikleri & Telemetri, 1: MarineTraffic Canlı Sayfa

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF0F172A),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
          color = Color(0xFF1E293B),
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
                  color = Color.White
                )
                Text(
                  text = "MMSI: ${aisData.mmsi} • IMO: ${aisData.imo} • ${aisData.flag}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = Color(0xFF94A3B8)
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("btn_close_ais_dialog")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
            }
          }
        }

        // 2. Tab Seçici (AIS Telemetrisi / MarineTraffic Canlı Web)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1120))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { selectedTab = 0 },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedTab == 0) PrimaryBlue else Color(0xFF1E293B),
              contentColor = Color.White
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
              containerColor = if (selectedTab == 1) Color(0xFF0284C7) else Color(0xFF1E293B),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("tab_ais_vesselfinder_web")
          ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("VesselFinder Harita", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
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
                      color = Color(0xFF38BDF8)
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (aisData.isApiKeyActive) Color(0xFF0369A1) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (aisData.isApiKeyActive) Color(0xFF38BDF8) else Color(0xFF475569))
                      ) {
                        Text(
                          text = if (aisData.isApiKeyActive) "🔑 MARINETRAFFIC API" else "🌐 AIS TELEMETRİ",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                          color = if (aisData.isApiKeyActive) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF065F46)
                      ) {
                        Text(
                          text = "CANLI SİNYAL",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                          color = Color(0xFF6EE7B7),
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }


                  Text(
                    text = aisData.status,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Hedef: ${aisData.destination}",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFFCBD5E1)
                    )
                    Text(
                      text = "Tahmini Varış (ETA): ${aisData.eta}",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFFFBBF24)
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
                  label = "ENLEM & BOYLAM (DENİZ GPS)",
                  value = "${com.example.model.LocationPresets.formatMarineLatitude(aisData.latitude)}\n${com.example.model.LocationPresets.formatMarineLongitude(aisData.longitude)}",
                  icon = Icons.Default.Place,
                  accentColor = Color(0xFF38BDF8),
                  modifier = Modifier.weight(1f)
                )
                AisStatBox(
                  label = "SOG (HIZ) & COG (ROTA)",
                  value = "${aisData.sogKnots} kn\n${String.format(Locale.US, "%03d°", aisData.headingDegrees)}",
                  icon = Icons.Default.Speed,
                  accentColor = Color(0xFF34D399),
                  modifier = Modifier.weight(1f)
                )
              }

              // Gemi Boyutları ve Teknik Özellikleri
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "🚢 GEMİ TEKNİK BOYUT VE BİLGİLERİ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFFFBBF24)
                  )

                  AisRowInfo(label = "Gemi Tipi", value = aisData.shipType)
                  AisRowInfo(label = "Bayrak", value = aisData.flag)
                  AisRowInfo(label = "MMSI Numarası", value = aisData.mmsi)
                  AisRowInfo(label = "IMO Numarası", value = aisData.imo)
                  AisRowInfo(label = "Çağrı İşareti (Call Sign)", value = aisData.callSign)
                  AisRowInfo(label = "Tam Boy (LOA)", value = "${aisData.loaMeters} m")
                  AisRowInfo(label = "Genişlik (Beam)", value = "${aisData.beamMeters} m")
                  AisRowInfo(label = "Su Çekimi (Draft)", value = "${aisData.draftMeters} m")
                  AisRowInfo(label = "Gros Tonaj (GT)", value = "${aisData.grossTonnage} GT")
                  AisRowInfo(label = "Detveyt Tonaj (DWT)", value = "${aisData.deadweightTon} DWT")
                  AisRowInfo(label = "Yapım Yılı", value = "${aisData.yearBuilt}")
                  AisRowInfo(label = "Son Sinyal", value = aisData.lastReportedTime)
                }
              }

              // VesselFinder Doğrudan Tarayıcı Butonu
              OutlinedButton(
                onClick = {
                  val mmsiClean = aisData.mmsi.trim().ifBlank { "222111447" }
                  val targetUrl = "https://www.vesselfinder.com/tr/?mmsi=$mmsiClean"
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                  context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("VesselFinder (vesselfinder.com) Sitesinde Aç", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          } else {
            // Canlı Gemi / Harita WebView (VesselFinder)
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
                  setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                      try {
                        view?.destroy()
                      } catch (e: Exception) {
                        // safe destroy
                      }
                      return true // Prevents app crash
                    }
                  }
                  val mmsiClean = aisData.mmsi.trim().ifBlank { "222111447" }
                  val dialogUrl = "https://www.vesselfinder.com/tr/?mmsi=$mmsiClean"
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
          color = Color(0xFF1E293B),
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
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1E293B),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF94A3B8))
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 12.sp), color = accentColor)
    }
  }
}

@Composable
private fun AisRowInfo(
  label: String,
  value: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
      color = Color(0xFF94A3B8),
      softWrap = true,
      modifier = Modifier.weight(1.2f)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
      color = Color.White,
      softWrap = true,
      textAlign = androidx.compose.ui.text.style.TextAlign.End,
      modifier = Modifier.weight(1f)
    )
  }
}
