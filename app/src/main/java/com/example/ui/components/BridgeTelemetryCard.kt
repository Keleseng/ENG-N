package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*

@Composable
fun BridgeTelemetryCard(
  analysis: NavigationAnalysis,
  isDarkMode: Boolean = false,
  onOpenAisMap: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("bridge_telemetry_card")
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE),
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Explore,
                contentDescription = "Anlık Durum",
                tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Anlık durum",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = textPrimary,
              softWrap = true
            )
            Text(
              text = "${analysis.vessel.name} • Dinamik Derinlik & UKC",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted,
              softWrap = true
            )
          }
        }

        if (onOpenAisMap != null) {
          FilledTonalButton(
            onClick = onOpenAisMap,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE),
              contentColor = if (isDarkMode) MarineCyan else PrimaryBlueDark
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp).testTag("btn_bridge_open_map")
          ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Harita", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 1. ANA DERİNLİK HESAP BÖLÜMÜ (Mevki Harita Derinliği + Gelgit = Anlık Toplam Derinlik)
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = subtleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) MarineCyan.copy(alpha = 0.3f) else PrimaryBlue.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text(
            text = "ANLIK SU DERİNLİĞİ HESAPLAMASI",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
            color = if (isDarkMode) MarineCyan else PrimaryBlueDark
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Harita Derinliği
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text(
                text = "Harita (CD)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = textMuted
              )
              Text(
                text = "${analysis.chartedDepthMeters} m",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimary
              )
            }

            Text(
              text = "+",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PrimaryBlue,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Anlık Gelgit
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text(
                text = "Gelgit",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = textMuted
              )
              Text(
                text = "${if (analysis.currentInstantTideHeightMeters >= 0) "+" else ""}${analysis.currentInstantTideHeightMeters} m",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (analysis.currentInstantTideHeightMeters >= 0) SeaGreen else DangerRed
              )
            }

            Text(
              text = "=",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PrimaryBlue,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Anlık Toplam Derinlik
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
              Text(
                text = "TOPLAM SU",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark
              )
              Text(
                text = "${analysis.currentInstantTotalDepthMeters} m",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Divider(color = cardBorder, thickness = 1.dp)
          Spacer(modifier = Modifier.height(6.dp))

          // Anlık Net UKC Durumu
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                if (analysis.isCurrentlySafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Net UKC:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textSecondary
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "+${analysis.currentInstantUkcMeters} m",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = if (analysis.isCurrentlySafe) SeaGreen else DangerRed
              )
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreenLight else Color(0xFFD1FAE5)) else (if (isDarkMode) DangerRedLight else Color(0xFFFEE2E2)),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (analysis.isCurrentlySafe) SeaGreenBorder else DangerRedBorder)
            ) {
              Text(
                text = if (analysis.isCurrentlySafe) "GÜVENLİ" else "YETERSİZ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                color = if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}

