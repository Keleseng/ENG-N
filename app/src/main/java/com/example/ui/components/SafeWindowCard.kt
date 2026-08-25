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
  modifier: Modifier = Modifier
) {
  val (ratingColor, ratingBg, ratingBorder, ratingText) = when (window.rating) {
    WindowSafetyRating.OPTIMAL -> Tuple4(SeaGreen, SeaGreenLight, SeaGreenBorder, "OPTİMAL GEÇİŞ")
    WindowSafetyRating.SUFFICIENT -> Tuple4(PrimaryBlue, PrimaryBlueLight, PrimaryBlueBorder, "GÜVENLİ")
    WindowSafetyRating.MARGINAL -> Tuple4(WarningAmber, WarningAmberLight, WarningAmberBorder, "KRİTİK SINIR")
    WindowSafetyRating.UNSAFE -> Tuple4(DangerRed, DangerRedLight, DangerRedBorder, "RİSKLİ")
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("safe_window_card_${window.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = if (isRecommendedBest) {
      androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue)
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    },
    elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommendedBest) 3.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Üst Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(if (window.isCurrentTimeInside) SeaGreen else PrimaryBlueLight, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (window.isCurrentTimeInside) Icons.Default.CheckCircle else Icons.Default.Schedule,
              contentDescription = null,
              tint = if (window.isCurrentTimeInside) Color.White else PrimaryBlue,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Pencere #${window.id}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            softWrap = true
          )

          if (isRecommendedBest) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PrimaryBlueLight,
              border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = PrimaryBlue,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                  text = "EN İYİ",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                  color = PrimaryBlue
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = ratingBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, ratingBorder)
        ) {
          Text(
            text = ratingText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ratingColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Saat ve Süre Gösterimi
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CardSubtle, RoundedCornerShape(12.dp))
          .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "GÜVENLİ GİRİŞ - ÇIKIŞ SAATİ",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${window.startTimeFormatted} - ${window.endTimeFormatted}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "AÇIK SÜRE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(2.dp))
          val hours = window.durationMinutes / 60
          val mins = window.durationMinutes % 60
          val durStr = if (hours > 0) "${hours}s ${mins}dk" else "${mins} dk"
          Text(
            text = durStr,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = SeaGreen
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Detay Parametreleri
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .background(CardSubtle, RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "Pik HW", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(text = window.peakTimeFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .background(CardSubtle, RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "Maksimum Su", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(text = "${window.maxWaterDepthMeters} m", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .background(CardSubtle, RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(text = "Pik UKC Payı", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(
            text = "+${window.maxUkcMeters} m",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = SeaGreen
          )
        }
      }
    }
  }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

