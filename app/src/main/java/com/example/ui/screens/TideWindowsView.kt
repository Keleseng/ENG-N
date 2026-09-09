package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

      // 2. Canlı Seyir ve Güvenlik Tavsiyesi Başlığı
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5)) else (if (isDarkMode) DangerRedDark else Color(0xFFFEE2E2))
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (analysis.isCurrentlySafe) SeaGreenBorder else DangerRedBorder
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("advisory_card")
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .background(
                    if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed,
                    CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (analysis.isCurrentlySafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = analysis.advisoryBadge,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF065F46)) else DangerRed,
                softWrap = true,
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = analysis.advisorySummary,
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp, fontSize = 11.5.sp),
              color = textPrimary,
              softWrap = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = { viewModel.saveCurrentTideCalculation() },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (analysis.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("btn_save_tide_to_history")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "💾 Analizi Kaydet (Room DB)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
              )
            }
          }
        }
      }

      // 3. İnteraktif Gelgit Eğrisi Grafiği
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

      // 5. Güvenli Giriş-Çıkış Zaman Pencereleri Başlığı
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Önerilen Güvenli Giriş - Çıkış Saatleri",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = textPrimary
            )
            Text(
              text = "${analysis.safeWindows.size} uygun geçiş penceresi tespit edildi",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = textSecondary
            )
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

      // 6. DİNAMİK 2D GEMİ KESİTİ & SQUAT SİMÜLASYONU
      item {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Dinamik Gemi Su Altı Kesiti & Squat",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = textPrimary
        )
      }

      item {
        ShipCrossSectionCanvas(
          analysis = analysis,
          modifier = Modifier.fillMaxWidth()
        )
      }

      // 7. Hidrodinamik Squat (Çökelme) ve Hız Simülasyon Paneli
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = cardBg),
          border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("squat_simulation_card")
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(if (isDarkMode) WarningAmberLight else Color(0xFFFEF3C7), RoundedCornerShape(6.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Speed, contentDescription = null, tint = if (isDarkMode) WarningAmber else Color(0xFFD97706), modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Kanal Hızı & Hidrodinamik Squat",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary
                  )
                  Text(
                    text = "Sığ sularda hız arttıkça gemi tabana çöker",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = textSecondary
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hız Kaydırıcısı (Slider)
            val speedVal = uiState.speedStr.toFloatOrNull() ?: 6f
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Kanal Seyir Hızı:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = textPrimary
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder)
              ) {
                Text(
                  text = "${String.format(java.util.Locale.US, "%.1f", speedVal)} knot",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Slider(
              value = speedVal,
              onValueChange = { viewModel.updateSpeed(String.format(java.util.Locale.US, "%.1f", it)) },
              valueRange = 2f..40f,
              steps = 37,
              colors = SliderDefaults.colors(
                thumbColor = if (isDarkMode) MarineCyan else PrimaryBlue,
                activeTrackColor = if (isDarkMode) MarineCyan else PrimaryBlue,
                inactiveTrackColor = subtleBorder
              ),
              modifier = Modifier.height(30.dp).testTag("speed_slider")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Hıza göre Squat Değişim Tablosu
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              SquatSpeedStepCard(
                speed = "10 knot",
                squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (100.0 / 100.0))}m",
                isCurrent = speedVal < 15f,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
              )
              SquatSpeedStepCard(
                speed = "20 knot",
                squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (400.0 / 100.0))}m",
                isCurrent = speedVal in 15f..24.9f,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
              )
              SquatSpeedStepCard(
                speed = "30 knot",
                squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (900.0 / 100.0))}m",
                isCurrent = speedVal in 25f..34.9f,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
              )
              SquatSpeedStepCard(
                speed = "40 knot",
                squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (1600.0 / 100.0))}m",
                isCurrent = speedVal >= 35f,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SquatSpeedStepCard(
  speed: String,
  squat: String,
  isCurrent: Boolean,
  isDarkMode: Boolean,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Column(
    modifier = modifier
      .background(
        if (isCurrent) (if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE)) else subtleBg,
        RoundedCornerShape(8.dp)
      )
      .border(
        1.dp,
        if (isCurrent) PrimaryBlueBorder else cardBorder,
        RoundedCornerShape(8.dp)
      )
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = speed,
      style = MaterialTheme.typography.labelSmall,
      color = if (isCurrent) (if (isDarkMode) MarineCyan else PrimaryBlueDark) else textMuted
    )
    Text(
      text = squat,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      color = if (isCurrent) (if (isDarkMode) MarineCyan else PrimaryBlueDark) else textPrimary
    )
  }
}
