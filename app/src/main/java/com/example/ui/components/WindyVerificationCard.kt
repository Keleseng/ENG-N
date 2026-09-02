package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LocationPresets
import com.example.model.MarineWeather
import com.example.ui.theme.*
import java.util.Locale

/**
 * Windy Deniz & Hava Durumu Doğrulama Kartı
 * Canlı ECMWF/GFS modelleri, rüzgar akımları, dalga, soluğan ve basınç katmanlarını
 * doğrudan uygulama içinde veya harici tarayıcıda interaktif olarak doğrular.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WindyVerificationCard(
  weather: MarineWeather,
  onRefreshWeather: () -> Unit,
  isDarkMode: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val subtleBg = if (isDarkMode) PrimaryBlueLight.copy(alpha = 0.25f) else CardSubtle
  val subtleBorder = if (isDarkMode) MarineCyan.copy(alpha = 0.35f) else CardSubtleBorder

  var selectedOverlay by rememberSaveable { mutableStateOf("wind") }
  var isExpanded by rememberSaveable { mutableStateOf(true) }
  var showFullScreenDialog by rememberSaveable { mutableStateOf(false) }

  val overlays = listOf(
    WindyOverlay("wind", "💨 Rüzgar", Icons.Default.Air),
    WindyOverlay("waves", "🌊 Dalga & Soluğan", Icons.Default.Waves),
    WindyOverlay("gust", "🌪️ Hamle (Gust)", Icons.Default.Storm),
    WindyOverlay("radar", "🌧️ Yağış / Radar", Icons.Default.Grain),
    WindyOverlay("pressure", "🧭 Basınç İzobar", Icons.Default.Speed)
  )

  fun buildWindyEmbedUrl(overlay: String): String {
    return "https://embed.windy.com/embed2.html?lat=${weather.latitude}&lon=${weather.longitude}&detailLat=${weather.latitude}&detailLon=${weather.longitude}&width=650&height=450&zoom=9&level=surface&overlay=$overlay&product=ecmwf&menu=&message=true&marker=true&calendar=now&pressure=true&type=map&location=coordinates&detail=true&metricWind=kt&metricTemp=%C2%B0C&radarRange=-1"
  }

  fun openExternalWindy(context: Context, overlay: String) {
    val url = "https://www.windy.com/?$overlay,${weather.latitude},${weather.longitude},9"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDarkMode) Color(0xFF0284C7) else Color(0xFF38BDF8)),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_windy_verification")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // ══════════════════════════════════════════════════════════════════════
      // 1. BAŞLIK VE DOĞRULAMA DURUM ROZETİ
      // ══════════════════════════════════════════════════════════════════════
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(
                Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF43F5E))),
                RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Air,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Windy",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = textPrimary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = SeaGreenLight
              ) {
                Text(
                  text = "ECMWF",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                  color = SeaGreenDark,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                )
              }
            }
            Text(
              text = "Mevki: ${LocationPresets.formatMarineCoordinates(weather.latitude, weather.longitude)}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = textSecondary
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { openExternalWindy(context, selectedOverlay) },
            modifier = Modifier
              .size(32.dp)
              .testTag("btn_open_external_windy")
          ) {
            Icon(
              imageVector = Icons.Default.OpenInNew,
              contentDescription = "Windy.com'da Aç",
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = "Genişlet / Daralt",
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // ══════════════════════════════════════════════════════════════════════
      // 2. DOĞRULAMA METRİK KARŞILAŞTIRMA ŞERİDİ
      // ══════════════════════════════════════════════════════════════════════
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = subtleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Doğrulanan Model Parametreleri",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = textSecondary
            )
            Text(
              text = "${weather.windSpeedKnots} kn (${weather.windDirectionCardinal}) • Dalga: ${weather.waveHeightMeters}m • ${weather.surfacePressureHpa} hPa",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = if (isDarkMode) MarineCyan else PrimaryBlueDark
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SeaGreen)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SeaGreen, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Doğrulandı",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                color = SeaGreen
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // ══════════════════════════════════════════════════════════════════
          // 3. WINDY KATMAN SEÇİCİ TABLARI
          // ══════════════════════════════════════════════════════════════════
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            overlays.forEach { overlay ->
              val isSelected = selectedOverlay == overlay.id
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) (if (isDarkMode) Color(0xFF0284C7) else PrimaryBlue) else subtleBg,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) (if (isDarkMode) MarineCyan else PrimaryBlueDark) else subtleBorder
                ),
                modifier = Modifier
                  .clickable { selectedOverlay = overlay.id }
                  .testTag("tab_windy_overlay_${overlay.id}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = overlay.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else textSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = overlay.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 10.5.sp
                    ),
                    color = if (isSelected) Color.White else textPrimary
                  )
                }
              }
            }
          }

          // ══════════════════════════════════════════════════════════════════
          // 4. İNTERAKTİF WINDY WEB HARİTASI (WEBVIEW)
          // ══════════════════════════════════════════════════════════════════
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(290.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
              .testTag("webview_windy_embed_container")
          ) {
            key(weather.latitude, weather.longitude, selectedOverlay) {
              WindyWebView(
                url = buildWindyEmbedUrl(selectedOverlay),
                modifier = Modifier.fillMaxSize()
              )
            }

            // Harita Sağ Üst Eylem Rozetleri
            Row(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.clickable { showFullScreenDialog = true }
              ) {
                Icon(
                  imageVector = Icons.Default.Fullscreen,
                  contentDescription = "Tam Ekran",
                  tint = Color.White,
                  modifier = Modifier
                    .padding(5.dp)
                    .size(16.dp)
                )
              }

              Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.clickable { onRefreshWeather() }
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Yenile",
                  tint = Color.White,
                  modifier = Modifier
                    .padding(5.dp)
                    .size(16.dp)
                )
              }
            }
          }

          // ══════════════════════════════════════════════════════════════════
          // 5. HIZLI DOĞRULAMA VE DIŞARI AÇMA BUTONLARI
          // ══════════════════════════════════════════════════════════════════
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = { openExternalWindy(context, selectedOverlay) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_windy_full_verify")
            ) {
              Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Windy'de Aç & Doğrula", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }

            OutlinedButton(
              onClick = { showFullScreenDialog = true },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_windy_modal_view")
            ) {
              Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Tam Ekran", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
          }
        }
      }
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // TAM EKRAN WINDY İNCELEME DİYALOGU
  // ══════════════════════════════════════════════════════════════════════════
  if (showFullScreenDialog) {
    Dialog(
      onDismissRequest = { showFullScreenDialog = false },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Dialog Başlığı
          Surface(
            color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Windy Canlı Doğrulama Haritası", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                  Text("${weather.latitude}°N, ${weather.longitude}°E • ECMWF Modeli", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                }
              }

              IconButton(onClick = { showFullScreenDialog = false }) {
                Icon(Icons.Default.Close, contentDescription = "Kapat")
              }
            }
          }

          // Katmanlar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9))
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            overlays.forEach { overlay ->
              val isSelected = selectedOverlay == overlay.id
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFFE11D48) else (if (isDarkMode) Color(0xFF334155) else Color.White),
                modifier = Modifier.clickable { selectedOverlay = overlay.id }
              ) {
                Text(
                  text = overlay.title,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = if (isSelected) Color.White else textPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          // Harita
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(weather.latitude, weather.longitude, selectedOverlay) {
              WindyWebView(
                url = buildWindyEmbedUrl(selectedOverlay),
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WindyWebView(
  url: String,
  modifier: Modifier = Modifier
) {
  var isLoading by remember { mutableStateOf(true) }

  Box(modifier = modifier) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
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
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MarineWindyNav/1.0"
          }
          webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isLoading = false
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?
            ) {
              super.onReceivedError(view, request, error)
              isLoading = false
            }
          }
          webChromeClient = WebChromeClient()
          loadUrl(url)
        }
      },
      update = { webView ->
        if (webView.url != url) {
          webView.loadUrl(url)
        }
      }
    )

    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
          color = Color(0xFFE11D48),
          modifier = Modifier.size(32.dp)
        )
      }
    }
  }
}

private data class WindyOverlay(
  val id: String,
  val title: String,
  val icon: ImageVector
)
