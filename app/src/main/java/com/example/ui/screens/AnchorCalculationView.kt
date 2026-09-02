package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AnchorCalculationCard
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AnchorCalculationView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToTide: () -> Unit = {},
  onNavigateToMap: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkMode
  val result = uiState.anchorCalculationResult

  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textMuted = getMarineTextMuted(isDark)

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("screen_anchor_calculation"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
  ) {
    // 1. Üst Demirleme & Güvenlik Özeti Şeridi
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else PrimaryBlue,
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Anchor,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = if (uiState.anchorEvent.isAnchored) "⚓ DEMİR ATILDI (AKTİF)" else "DEMİRLEME & KALOMA",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                color = textPrimary
              )
              Text(
                text = "Salma: ${String.format(Locale.US, "%.1f", result.f_secondSwingingCircleMeters)}m (${String.format(Locale.US, "%.2f", result.f_secondSwingingCircleGomina)} Gomina) • Derinlik: ${String.format(Locale.US, "%.1f", result.b_depthMeters)}m",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isDark) MarineCyan else PrimaryBlue
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (result.scopeStatus.isSafe) (if (isDark) SeaGreenDark else Color(0xFFD1FAE5)) else (if (isDark) DangerRedDark else Color(0xFFFEE2E2)),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (result.scopeStatus.isSafe) SeaGreenBorder else DangerRedBorder
            )
          ) {
            Text(
              text = result.scopeStatus.labelTr,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
              color = if (result.scopeStatus.isSafe) (if (isDark) SeaGreen else Color(0xFF059669)) else DangerRed,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    // 2. Ana Demirleme ve Salma Dairesi Hesaplama Kartı
    item {
      AnchorCalculationCard(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToMap = onNavigateToMap
      )
    }
  }
}
