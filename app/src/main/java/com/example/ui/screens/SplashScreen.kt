package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Açılış Ekranı (Splash Screen) - imaj 1 ile 5 saniye bekletme ve çözünürlük uyumu
 */
@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isVisible by remember { mutableStateOf(true) }
  var progress by remember { mutableFloatStateOf(0f) }
  var currentPhaseText by remember { mutableStateOf("🛰️ GPS Uyduları ve Deniz Telemetrisi Başlatılıyor...") }

  fun finishSplashNow() {
    if (isVisible) {
      isVisible = false
      onSplashFinished()
    }
  }

  LaunchedEffect(Unit) {
    val totalDurationMs = 1200L
    val stepIntervalMs = 40L
    val steps = (totalDurationMs / stepIntervalMs).toInt()

    for (i in 1..steps) {
      delay(stepIntervalMs)
      val currentProgress = i.toFloat() / steps.toFloat()
      progress = currentProgress

      currentPhaseText = when {
        currentProgress < 0.35f -> "🛰️ GPS Konumu & Uydu Seyir Parametreleri Alınıyor..."
        currentProgress < 0.70f -> "🌊 MarineTraffic AIS Telemetrisi Yükleniyor..."
        currentProgress < 0.95f -> "⚓ Hidrodinamik Squat, Dinamik UKC ve Gelgit Motoru Başlatılıyor..."
        else -> "✅ Sistem Hazır — Köprüüstüne Aktarılıyor..."
      }
    }

    finishSplashNow()
  }

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(200)),
    exit = fadeOut(animationSpec = tween(200))
  ) {
    BoxWithConstraints(
      modifier = modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF030712),
              Color(0xFF0B132B),
              Color(0xFF020617)
            )
          )
        )
        .clickable { finishSplashNow() }
        .systemBarsPadding()
        .testTag("screen_splash_imaj_1"),
      contentAlignment = Alignment.Center
    ) {
      val maxW = maxWidth
      val maxH = maxHeight
      val isTabletOrLandscape = maxW > 600.dp

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Üst Taktik Başlık
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "TÜRK DENİZ KUVVETLERİ • AIS SEYİR SİSTEMİ",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 1.5.sp,
                  fontSize = if (isTabletOrLandscape) 12.sp else 10.sp
                ),
                color = Color(0xFFE2E8F0)
              )
            }
          }
        }

        // Orta: İmaj 1 (TCG Logo Kartı)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            modifier = Modifier
              .widthIn(max = if (isTabletOrLandscape) 480.dp else 340.dp)
              .aspectRatio(1f)
              .shadow(24.dp, RoundedCornerShape(24.dp))
              .testTag("card_imaj_1"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.drawable.imaj_1),
                contentDescription = "imaj 1",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("img_imaj_1")
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Deniz Kuvvetleri • Canlı AIS Seyir & Gelgit Güvenlik Sistemi",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = if (isTabletOrLandscape) 18.sp else 15.sp,
              letterSpacing = 0.5.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )
        }

        // Alt: 5 Saniyelik İlerleme Çubuğu ve Telemetri Durumu
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth(0.85f)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF38BDF8),
            trackColor = Color(0xFF1E293B)
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = currentPhaseText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = Color(0xFF38BDF8),
              textAlign = TextAlign.Center
            )
          }

          Surface(
            onClick = { finishSplashNow() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0284C7).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
          ) {
            Text(
              text = "▶ Hemen Başla (Köprüüstü)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }

          Text(
            text = "MarineTraffic Canlı AIS • Canlı GPS & Seyir Sistemi",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Normal
            ),
            color = Color(0xFF64748B)
          )
        }
      }
    }
  }
}
