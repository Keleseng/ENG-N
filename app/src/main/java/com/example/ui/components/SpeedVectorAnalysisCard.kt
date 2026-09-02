package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
fun SpeedVectorAnalysisCard(
  speedAnalysis: SpeedCalculationResult,
  onSyncGpsSpeed: () -> Unit,
  onRequestGps: () -> Unit,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  var isExpanded by rememberSaveable { mutableStateOf(false) }
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
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // 1. Başlık
      Row(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Yere Göre Sürat",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = textPrimary
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5)) else subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (speedAnalysis.isGpsActive) SeaGreenBorder else subtleBorder)
        ) {
          Text(
            text = if (speedAnalysis.isGpsActive) "🛰️ GPS AKTİF" else "STATİK MOD",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 9.sp
            ),
            color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textMuted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column {
          Spacer(modifier = Modifier.height(14.dp))

      // 2. Yan Yana 3 Ana Sürat Kutusu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // A) GPS Sürati (SOG)
        Surface(
          shape = RoundedCornerShape(12.dp),
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
              .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "GPS Sürati",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
            Text(
              text = "SOG",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
              color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${speedAnalysis.gpsSpeedKnots}",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
              ),
              color = if (speedAnalysis.isGpsActive) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else textPrimary
            )
            Text(
              text = "knot (kn)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
          }
        }

        // B) Suya Göre Sürat (STW)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Su Sürati",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
            Text(
              text = "STW (Kütük)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
              color = if (isDarkMode) MarineCyan else PrimaryBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${speedAnalysis.speedThroughWaterKnots}",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
              ),
              color = if (isDarkMode) MarineCyan else PrimaryBlue
            )
            Text(
              text = "knot (kn)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
          }
        }

        // C) Yere Göre Hesaplanmış Sürat (V_ground)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Yere Göre Sürat",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
            Text(
              text = "Vektörel SOG",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
              color = if (isDarkMode) WarningAmber else Color(0xFFD97706)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${speedAnalysis.calculatedGroundSpeedKnots}",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
              ),
              color = if (isDarkMode) WarningAmber else Color(0xFFD97706)
            )
            Text(
              text = "knot (kn)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Akıntı ve Sürüklenme Değerlendirme Şeridi
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (speedAnalysis.deltaSpeedKnots >= 0) (if (isDarkMode) SeaGreenLight.copy(alpha = 0.6f) else Color(0xFFD1FAE5)) else (if (isDarkMode) WarningAmberLight else Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (speedAnalysis.deltaSpeedKnots >= 0) SeaGreenBorder else WarningAmberBorder
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
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
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Hız Kazancı / Kaybı: ${if (speedAnalysis.deltaSpeedKnots > 0) "+" else ""}${speedAnalysis.deltaSpeedKnots} kn",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (speedAnalysis.deltaSpeedKnots >= 0) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else (if (isDarkMode) WarningAmberDark else Color(0xFFB45309))
              )
            }

            Text(
              text = "COG: ${String.format("%03d°", speedAnalysis.groundCourseDegrees)}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
              color = if (isDarkMode) MarineCyan else PrimaryBlueDark
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = speedAnalysis.speedEvaluationText,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkMode) textPrimary else Color(0xFF1E293B)
          )
          Text(
            text = speedAnalysis.driftStatusText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (isDarkMode) textMuted else Color(0xFF475569)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 4. Görsel Sürat & Akıntı Vektör Üçgeni (Canvas)
      Text(
        text = "Vektörel Seyir Üçgeni (Su Sürati + Akıntı = Yere Göre Sürat)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = textMuted
      )
      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF0B1322) else Color(0xFF1E293B),
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          val originX = w * 0.25f
          val originY = h * 0.75f

          // Izgara çizgileri
          drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, originY), Offset(w, originY), 1f)
          drawLine(Color.White.copy(alpha = 0.1f), Offset(originX, 0f), Offset(originX, h), 1f)

          val scale = min(w, h) / 25f

          // Suya Göre Vektör (Mavi)
          val vWaterAngleRad = Math.toRadians((speedAnalysis.vesselHeadingDegrees - 90).toDouble())
          val vWaterLen = (speedAnalysis.speedThroughWaterKnots * scale).toFloat().coerceIn(20f, w * 0.45f)
          val waterEndX = originX + vWaterLen * cos(vWaterAngleRad).toFloat()
          val waterEndY = originY + vWaterLen * sin(vWaterAngleRad).toFloat()

          // Akıntı Vektörü (Turuncu)
          val vCurrentAngleRad = Math.toRadians((speedAnalysis.currentDirectionDegrees - 90).toDouble())
          val vCurrentLen = (speedAnalysis.currentSpeedKnots * scale * 2.5f).toFloat().coerceIn(10f, w * 0.3f)
          val groundEndX = waterEndX + vCurrentLen * cos(vCurrentAngleRad).toFloat()
          val groundEndY = waterEndY + vCurrentLen * sin(vCurrentAngleRad).toFloat()

          // 1. Suya Göre Vektör (Mavi Çizgi)
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(originX, originY),
            end = Offset(waterEndX, waterEndY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
          )

          // 2. Akıntı Vektörü (Turuncu Çizgi)
          drawLine(
            color = Color(0xFFF59E0B),
            start = Offset(waterEndX, waterEndY),
            end = Offset(groundEndX, groundEndY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
          )

          // 3. Yere Göre Sürat Vektörü (Yeşil Çizgi)
          drawLine(
            color = Color(0xFF34D399),
            start = Offset(originX, originY),
            end = Offset(groundEndX, groundEndY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
          )
        }
      }

      // Legend / Gösterge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).background(Color(0xFF38BDF8), RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Su Sürati (STW)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B), RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Akıntı (${speedAnalysis.currentSpeedKnots} kn)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).background(Color(0xFF34D399), RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Yere Göre (SOG)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textMuted)
        }
      }
      }
      }
    }
  }
}
