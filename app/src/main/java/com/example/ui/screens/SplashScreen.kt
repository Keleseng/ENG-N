package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Program Girişi Açılış Ekranı (Splash Screen) - Sadece resim görüntülenir, tüm yazılar kaldırılmıştır.
 */
@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isVisible by remember { mutableStateOf(true) }

  // Resim Nabız / Işıma Efekti
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.02f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  fun finishSplashNow() {
    if (isVisible) {
      isVisible = false
      onSplashFinished()
    }
  }

  LaunchedEffect(Unit) {
    // 3 saniye sonra otomatik geçiş
    delay(3000L)
    finishSplashNow()
  }

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(animationSpec = tween(300))
  ) {
    BoxWithConstraints(
      modifier = modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF030712),
              Color(0xFF0A192F),
              Color(0xFF020617)
            )
          )
        )
        .clickable { finishSplashNow() }
        .systemBarsPadding()
        .testTag("screen_splash"),
      contentAlignment = Alignment.Center
    ) {
      val maxW = maxWidth
      val isTabletOrLandscape = maxW > 600.dp

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(pulseScale)
      ) {
        // Arka Plan Işıma Halesi
        Box(
          modifier = Modifier
            .size(if (isTabletOrLandscape) 420.dp else 320.dp)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color(0xFF38BDF8).copy(alpha = 0.35f),
                  Color(0xFF0284C7).copy(alpha = 0.15f),
                  Color.Transparent
                )
              ),
              shape = CircleShape
            )
        )

        // Giriş Resmi (TCG İZMİR F-516)
        Surface(
          modifier = Modifier
            .size(if (isTabletOrLandscape) 360.dp else 280.dp)
            .shadow(28.dp, RoundedCornerShape(32.dp))
            .testTag("card_splash_image"),
          shape = RoundedCornerShape(32.dp),
          color = Color.White,
          border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF38BDF8))
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(id = R.drawable.imaj_1),
              contentDescription = "Giriş Resmi",
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}


