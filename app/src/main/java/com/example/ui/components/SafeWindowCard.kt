package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SafeNavigationWindow
import com.example.model.WindowSafetyRating
import com.example.ui.theme.*

@Composable
fun SafeWindowCard(
  window: SafeNavigationWindow,
  isRecommendedBest: Boolean,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  val (ratingColor, ratingBg, ratingBorder) = when (window.rating) {
    WindowSafetyRating.OPTIMAL -> Tuple3(
      if (isDarkMode) SeaGreen else Color(0xFF059669),
      if (isDarkMode) SeaGreenLight else Color(0xFFD1FAE5),
      SeaGreenBorder
    )
    WindowSafetyRating.SUFFICIENT -> Tuple3(
      if (isDarkMode) MarineCyan else PrimaryBlue,
      if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE),
      PrimaryBlueBorder
    )
    WindowSafetyRating.MARGINAL -> Tuple3(
      if (isDarkMode) WarningAmber else Color(0xFFD97706),
      if (isDarkMode) WarningAmberLight else Color(0xFFFEF3C7),
      WarningAmberBorder
    )
    WindowSafetyRating.UNSAFE -> Tuple3(
      DangerRed,
      if (isDarkMode) DangerRedLight else Color(0xFFFEE2E2),
      DangerRedBorder
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("safe_window_card_${window.id}"),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = if (isRecommendedBest) {
      androidx.compose.foundation.BorderStroke(1.5.dp, ratingColor)
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    },
    elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommendedBest) 1.5.dp else 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Sol: İkon ve Saatler
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .background(ratingBg, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (window.isCurrentTimeInside) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = ratingColor,
            modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Geçiş Uygun",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = ratingColor
            )
            if (isRecommendedBest) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ratingColor,
                modifier = Modifier.size(12.dp)
              )
            }
          }
          Text(
            text = "${window.startTimeFormatted} - ${window.endTimeFormatted}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
          )
        }
      }

      // Sağ: Süre ve Derinlik
      Column(horizontalAlignment = Alignment.End) {
        val hours = window.durationMinutes / 60
        val mins = window.durationMinutes % 60
        val durStr = if (hours > 0) "${hours}s ${mins}dk" else "${mins} dk"
        
        Text(
          text = durStr,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
          color = ratingColor
        )
        Text(
          text = "Max: ${window.maxWaterDepthMeters}m",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = textMuted
        )
      }
    }
  }
}

private data class Tuple3<A, B, C>(val a: A, val b: B, val c: C)

