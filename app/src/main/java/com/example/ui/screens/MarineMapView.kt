package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*

/**
 * MarineTraffic AIS Canlı Harita Görünümü (Sadece MarineTraffic Sayfası)
 * URL: https://www.marinetraffic.com/en/ais/home/centerx:29.301/centery:40.836/zoom:13
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
  val marineTrafficUrl = "https://www.marinetraffic.com/en/ais/home/centerx:29.301/centery:40.836/zoom:13"

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var loadingProgress by remember { mutableStateOf(0) }
  var hasLoadError by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9))
      .testTag("screen_marine_traffic_map")
  ) {
    // ══════════════════════════════════════════════════════════════════════
    // MARINETRAFFIC WEBVIEW HARİTASI (TAM EKRAN)
    // ══════════════════════════════════════════════════════════════════════
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("webview_marinetraffic_map"),
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
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MarineTrafficNav/1.0"
          }
          webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
              hasLoadError = false
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
              if (request?.isForMainFrame == true) {
                isLoading = false
                hasLoadError = true
              }
            }
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              super.onProgressChanged(view, newProgress)
              loadingProgress = newProgress
              if (newProgress >= 100) {
                isLoading = false
              }
            }
          }

          webViewRef = this
          loadUrl(marineTrafficUrl)
        }
      },
      update = { webView ->
        if (webView.url != marineTrafficUrl && webView.url == null) {
          webView.loadUrl(marineTrafficUrl)
        }
      }
    )

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
            text = "MarineTraffic Haritası Yüklenemedi",
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(marineTrafficUrl))
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
