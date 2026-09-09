package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarineAttitude
import com.example.ui.theme.MarineCyan
import com.example.ui.theme.PrimaryBlueDark
import kotlin.math.abs

@Composable
fun MarineInclinometerCard(
  attitude: MarineAttitude,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
  val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
  val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
  val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = cardBg,
    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_pitch_roll_inclinometer")
  ) {
    Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)) {
      // Başlık Satırı
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = "Yalpa ve Eğim",
            tint = if (isDark) MarineCyan else PrimaryBlueDark,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "YALPA & MEYİL (INCLINOMETER)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.3.sp
            ),
            color = textSecondary
          )
        }

        Surface(
          shape = RoundedCornerShape(3.dp),
          color = when (attitude.stabilityStatus) {
            "Dengeli" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
            "Mutedil Yalpa" -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
            else -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
          }
        ) {
          Text(
            text = if (attitude.isSensorActive) "● SENSÖR: ${attitude.stabilityStatus}" else "● SALINIM: ${attitude.stabilityStatus}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = when (attitude.stabilityStatus) {
              "Dengeli" -> if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
              "Mutedil Yalpa" -> if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
              else -> if (isDark) Color(0xFFF87171) else Color(0xFF991B1B)
            },
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 2 Sütun: Baş/Kıç (Pitch) & Sancak/İskele (Roll)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // 1. BAŞ \ KIÇ (PITCH)
        Box(
          modifier = Modifier
            .weight(1f)
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "BAŞ \\ KIÇ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = textSecondary
              )
              Text(
                text = attitude.pitchLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (abs(attitude.pitchDegrees) < 0.2f) (if (isDark) Color(0xFF38BDF8) else PrimaryBlueDark)
                       else if (attitude.pitchDegrees > 0f) Color(0xFF10B981) else Color(0xFFF59E0B)
              )
            }

            Spacer(modifier = Modifier.height(3.dp))

            PitchGaugeBar(
              pitchDegrees = attitude.pitchDegrees,
              isDark = isDark,
              modifier = Modifier.fillMaxWidth().height(9.dp)
            )
          }
        }

        // 2. SANCAK \ İSKELE (ROLL)
        Box(
          modifier = Modifier
            .weight(1f)
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "SANCAK \\ İSKELE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = textSecondary
              )
              Text(
                text = attitude.rollLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (abs(attitude.rollDegrees) < 0.2f) (if (isDark) Color(0xFF38BDF8) else PrimaryBlueDark)
                       else if (attitude.rollDegrees > 0f) Color(0xFF10B981) else Color(0xFFEF4444)
              )
            }

            Spacer(modifier = Modifier.height(3.dp))

            RollGaugeBar(
              rollDegrees = attitude.rollDegrees,
              isDark = isDark,
              modifier = Modifier.fillMaxWidth().height(9.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun PitchGaugeBar(
  pitchDegrees: Float,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
  val centerMarkColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
  val indicatorColor = if (abs(pitchDegrees) < 0.2f) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                      else if (pitchDegrees > 0f) Color(0xFF10B981) else Color(0xFFF59E0B)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val centerY = h / 2f

    // Ray / Çubuk arka planı
    drawRoundRect(
      color = trackColor,
      size = Size(w, 3.dp.toPx()),
      topLeft = Offset(0f, centerY - 1.5.dp.toPx()),
      cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
    )

    // Merkez çizgi (0° Düz)
    drawLine(
      color = centerMarkColor,
      start = Offset(w / 2f, 0f),
      end = Offset(w / 2f, h),
      strokeWidth = 1.dp.toPx()
    )

    // Gösterge balonu (Pitch tilt) - Range -10° to +10°
    val normalized = (pitchDegrees / 10f).coerceIn(-1f, 1f)
    val indicatorX = (w / 2f) + (normalized * (w / 2f - 4.dp.toPx()))

    drawCircle(
      color = indicatorColor,
      radius = 3.5.dp.toPx(),
      center = Offset(indicatorX, centerY)
    )
  }
}

@Composable
fun RollGaugeBar(
  rollDegrees: Float,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
  val centerMarkColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
  val indicatorColor = if (abs(rollDegrees) < 0.2f) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                     else if (rollDegrees > 0f) Color(0xFF10B981) else Color(0xFFEF4444)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val centerY = h / 2f

    // Ray / Çubuk arka planı
    drawRoundRect(
      color = trackColor,
      size = Size(w, 3.dp.toPx()),
      topLeft = Offset(0f, centerY - 1.5.dp.toPx()),
      cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
    )

    // Sol taraf İskele (kırmızımsı hafif arka plan ipucu)
    drawRect(
      color = Color(0xFFEF4444).copy(alpha = 0.2f),
      size = Size(w / 2f, 2.dp.toPx()),
      topLeft = Offset(0f, centerY - 1.dp.toPx())
    )

    // Sağ taraf Sancak (yeşilimsi hafif arka plan ipucu)
    drawRect(
      color = Color(0xFF10B981).copy(alpha = 0.2f),
      size = Size(w / 2f, 2.dp.toPx()),
      topLeft = Offset(w / 2f, centerY - 1.dp.toPx())
    )

    // Merkez çizgi (0° Düz)
    drawLine(
      color = centerMarkColor,
      start = Offset(w / 2f, 0f),
      end = Offset(w / 2f, h),
      strokeWidth = 1.dp.toPx()
    )

    // Gösterge balonu (Roll tilt) - Range -20° to +20°
    val normalized = (rollDegrees / 20f).coerceIn(-1f, 1f)
    val indicatorX = (w / 2f) + (normalized * (w / 2f - 4.dp.toPx()))

    drawCircle(
      color = indicatorColor,
      radius = 3.5.dp.toPx(),
      center = Offset(indicatorX, centerY)
    )
  }
}
