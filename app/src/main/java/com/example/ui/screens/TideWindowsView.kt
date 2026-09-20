package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtremumType
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.SafeWindowCard
import com.example.ui.components.ShipCrossSectionCanvas
import com.example.ui.components.TideCurveCanvas
import com.example.ui.theme.*

@Composable
fun TideWindowsView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val isDarkMode = uiState.isDarkMode
  val analysis = uiState.analysis
  val bestWindow = analysis.safeWindows.maxByOrNull { it.maxUkcMeters }

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxHeight()
        .widthIn(max = 440.dp)
        .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {

      // 1. İnteraktif Gelgit Eğrisi Grafiği
      item {
        TideCurveCanvas(
          analysis = analysis,
          inspectedHour = uiState.inspectedHour,
          onHourSelected = { viewModel.setInspectedHour(it) }
        )
      }

      // 4. Yüksek Su (HW) ve Alçak Su (LW) Zamanları
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val hwList = analysis.extrema.filter { it.type == ExtremumType.HIGH_WATER }
          val lwList = analysis.extrema.filter { it.type == ExtremumType.LOW_WATER }

          // HW Kartı
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (isDarkMode) MarineCyan else PrimaryBlueDark, modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Yüksek Su (HW)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = if (isDarkMode) MarineCyan else PrimaryBlue
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              if (hwList.isNotEmpty()) {
                hwList.forEach { hw ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = hw.timeFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                    Text(text = "+${hw.tideHeightMeters}m", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isDarkMode) MarineCyan else PrimaryBlue)
                  }
                }
              } else {
                Text("Maks: ${analysis.maxTideHeight24h}m", style = MaterialTheme.typography.labelSmall, color = textMuted)
              }
            }
          }

          // LW Kartı
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .background(if (isDarkMode) WarningAmberLight else Color(0xFFFEF3C7), RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if (isDarkMode) WarningAmber else Color(0xFFD97706), modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Alçak Su (LW)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = if (isDarkMode) WarningAmber else Color(0xFFD97706)
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              if (lwList.isNotEmpty()) {
                lwList.forEach { lw ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = lw.timeFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                    Text(text = "${if (lw.tideHeightMeters >= 0) "+" else ""}${lw.tideHeightMeters}m", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isDarkMode) WarningAmber else Color(0xFFD97706))
                  }
                }
              } else {
                Text("Min: ${analysis.minTideHeight24h}m", style = MaterialTheme.typography.labelSmall, color = textMuted)
              }
            }
          }
        }
      }

      // Pencereler Listesi
      if (analysis.safeWindows.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRedBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .background(if (isDarkMode) DangerRedLight else Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Dangerous, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Geçiş Uygun Değil",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = DangerRed
                )
                Text(
                  text = "Derinlik yetersiz.",
                  style = MaterialTheme.typography.labelSmall,
                  color = textSecondary
                )
              }
            }
          }
        }
      } else {
        items(analysis.safeWindows, key = { it.id }) { window ->
          SafeWindowCard(
            window = window,
            isRecommendedBest = window.id == bestWindow?.id,
            isDarkMode = isDarkMode
          )
        }
      }

      item {
        ShipCrossSectionCanvas(
          analysis = analysis,
          isDarkMode = isDarkMode,
          modifier = Modifier.fillMaxWidth()
        )
      }

    }
  }
}