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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*

/**
 * Anlık Su Derinliği Hesaplaması Kartı
 * Harita Derinliği (CD) + Anlık Gelgit = Anlık Toplam Su Derinliği ve Net UKC Güvenlik Durumu.
 */
@Composable
fun InstantWaterDepthCard(
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
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("instant_depth_calculation_card")
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      // Başlık & Güvenlik Rozeti
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
            modifier = Modifier.size(24.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Water,
                contentDescription = "Anlık Su Derinliği",
                tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                modifier = Modifier.size(14.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "ANLIK SU DERİNLİĞİ HESAPLAMASI",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 10.5.sp),
              color = if (isDarkMode) MarineCyan else PrimaryBlueDark
            )
            Text(
              text = "${analysis.vessel.name.ifBlank { "Gemi" }} • Harita (CD) + Gelgit = Toplam Derinlik",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = textMuted
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onOpenAisMap != null) {
            FilledTonalButton(
              onClick = onOpenAisMap,
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE),
                contentColor = if (isDarkMode) MarineCyan else PrimaryBlueDark
              ),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp).padding(end = 4.dp).testTag("btn_instant_depth_map")
            ) {
              Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(11.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Harita", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
            }
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

      Spacer(modifier = Modifier.height(6.dp))

      // Hesaplama Kutusu: Harita Derinliği (CD) + Gelgit = Anlık Toplam Derinlik
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = subtleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) MarineCyan.copy(alpha = 0.25f) else PrimaryBlue.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
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
          HorizontalDivider(color = cardBorder, thickness = 0.8.dp)
          Spacer(modifier = Modifier.height(6.dp))

          // Net UKC Durumu ve Draft Karşılaştırması
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

            Text(
              text = "Draft: ${analysis.vessel.draftMeters} m",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
          }
        }
      }
    }
  }
}

/**
 * Geriye uyumluluk için BridgeTelemetryCard artık doğrudan Anlık Su Derinliği Kartı olarak hizmet verir.
 */
@Composable
fun BridgeTelemetryCard(
  analysis: NavigationAnalysis,
  isDarkMode: Boolean = false,
  onOpenAisMap: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  InstantWaterDepthCard(
    analysis = analysis,
    isDarkMode = isDarkMode,
    onOpenAisMap = onOpenAisMap,
    modifier = modifier
  )
}
