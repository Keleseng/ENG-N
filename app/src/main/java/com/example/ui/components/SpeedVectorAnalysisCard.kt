package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedCalculationResult
import com.example.ui.theme.*
import kotlin.math.*

private fun formatKnots(value: Double): String {
  return String.format(java.util.Locale.US, "%.1f", value)
}

@Composable
fun SpeedVectorAnalysisCard(
  speedAnalysis: SpeedCalculationResult,
  onSyncGpsSpeed: () -> Unit,
  onRequestGps: () -> Unit,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_speed_vector_analysis"),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
    ) {
      // 1. Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(14.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "Yere Göre Sürat",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
              color = textPrimary
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5)) else subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (speedAnalysis.isGpsActive) SeaGreenBorder else subtleBorder)
        ) {
          Text(
            text = if (speedAnalysis.isGpsActive) "🛰️ GPS AKTİF" else "STATİK MOD",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 8.sp
            ),
            color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Yan Yana 3 Ana Sürat Kutusu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // A) GPS Sürati (SOG)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5)) else subtleBg,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (speedAnalysis.isGpsActive) SeaGreenBorder else subtleBorder
          ),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "GPS Sürati (SOG)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textMuted,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = formatKnots(speedAnalysis.gpsSpeedKnots),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp
                ),
                color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textPrimary
              )
              Text(
                text = " kn",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = textMuted,
                modifier = Modifier.padding(bottom = 2.dp)
              )
            }
          }
        }

        // B) Suya Göre Sürat (STW)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Su Sürati (STW)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = if (isDarkMode) MarineCyan else PrimaryBlue,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = formatKnots(speedAnalysis.speedThroughWaterKnots),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp
                ),
                color = if (isDarkMode) MarineCyan else PrimaryBlue
              )
              Text(
                text = " kn",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = textMuted,
                modifier = Modifier.padding(bottom = 2.dp)
              )
            }
          }
        }

        // C) Yere Göre Hesaplanmış Sürat (V_ground)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Vektörel SOG",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = if (isDarkMode) WarningAmber else Color(0xFFD97706),
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = formatKnots(speedAnalysis.calculatedGroundSpeedKnots),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp
                ),
                color = if (isDarkMode) WarningAmber else Color(0xFFD97706)
              )
              Text(
                text = " kn",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = textMuted,
                modifier = Modifier.padding(bottom = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Akıntı ve Sürüklenme Değerlendirme Şeridi
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (speedAnalysis.deltaSpeedKnots >= 0) (if (isDarkMode) SeaGreenLight.copy(alpha = 0.6f) else Color(0xFFD1FAE5)) else (if (isDarkMode) WarningAmberLight else Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          if (speedAnalysis.deltaSpeedKnots >= 0) SeaGreenBorder else WarningAmberBorder
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (speedAnalysis.deltaSpeedKnots >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (speedAnalysis.deltaSpeedKnots >= 0) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else (if (isDarkMode) WarningAmberDark else Color(0xFFB45309)),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${if (speedAnalysis.deltaSpeedKnots > 0) "+" else ""}${formatKnots(speedAnalysis.deltaSpeedKnots)} kn",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = if (speedAnalysis.deltaSpeedKnots >= 0) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else (if (isDarkMode) WarningAmberDark else Color(0xFFB45309))
              )
            }

            Text(
              text = "COG: ${String.format(java.util.Locale.US, "%03d°", speedAnalysis.groundCourseDegrees)}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
              color = if (isDarkMode) MarineCyan else PrimaryBlueDark
            )
          }

          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = speedAnalysis.speedEvaluationText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = if (isDarkMode) textPrimary else Color(0xFF1E293B)
          )
        }
      }
    }
  }
}
