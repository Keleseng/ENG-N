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
import com.example.ui.components.BridgeTelemetryCard
import com.example.ui.components.RealisticMoonPhaseCard
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
  val analysis = uiState.analysis
  val bestWindow = analysis.safeWindows.maxByOrNull { it.maxUkcMeters }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
  ) {

    // 0. Canlı Köprüüstü Seyir & Çevre Telemetrisi Kartı
    item {
      BridgeTelemetryCard(analysis = analysis)
    }

    // 1. Gerçekçi Ay Evresi & Gelgit Çekim Katsayısı Kartı
    item {
      RealisticMoonPhaseCard(analysis = analysis)
    }

    // 2. Canlı Seyir ve Güvenlik Tavsiyesi Başlığı
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (analysis.isCurrentlySafe) SeaGreenLight else DangerRedLight
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
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (analysis.isCurrentlySafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = analysis.advisoryBadge,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
              softWrap = true,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = analysis.advisorySummary,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = TextPrimary,
            softWrap = true
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { viewModel.saveCurrentTideCalculation() },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .testTag("btn_save_tide_to_history")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "💾 Bu Gelgit ve UKC Analizini Geçmişe Kaydet (Room DB)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val hwList = analysis.extrema.filter { it.type == ExtremumType.HIGH_WATER }
        val lwList = analysis.extrema.filter { it.type == ExtremumType.LOW_WATER }

        // HW Kartı
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardWhite),
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .background(PrimaryBlueLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Yüksek Su (HW)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (hwList.isNotEmpty()) {
              hwList.forEach { hw ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = hw.timeFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                  Text(text = "+${hw.tideHeightMeters}m", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                }
              }
            } else {
              Text("Maks: ${analysis.maxTideHeight24h}m", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
          }
        }

        // LW Kartı
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardWhite),
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .background(WarningAmberLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Alçak Su (LW)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = WarningAmber
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (lwList.isNotEmpty()) {
              lwList.forEach { lw ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = lw.timeFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                  Text(text = "${if (lw.tideHeightMeters >= 0) "+" else ""}${lw.tideHeightMeters}m", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = WarningAmber)
                }
              }
            } else {
              Text("Min: ${analysis.minTideHeight24h}m", style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
          )
          Text(
            text = "${analysis.safeWindows.size} uygun geçiş penceresi tespit edildi",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
      }
    }

    // Pencereler Listesi
    if (analysis.safeWindows.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardWhite),
          border = androidx.compose.foundation.BorderStroke(1.dp, DangerRedBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .background(DangerRedLight, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Dangerous, contentDescription = null, tint = DangerRed, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "24 Saat İçerisinde Güvenli Pencere Yok",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = DangerRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Gemi draftı (${analysis.actualDraftMeters}m) ve minimum güvenlik payı (${analysis.minUkcMeters}m) için gereken derinlik (${analysis.totalRequiredDepthMeters}m), bölgedeki maksimum su seviyesini (${analysis.maxAvailableDepth24h}m) aşmaktadır.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    } else {
      items(analysis.safeWindows, key = { it.id }) { window ->
        SafeWindowCard(
          window = window,
          isRecommendedBest = window.id == bestWindow?.id
        )
      }
    }

    // 6. DİNAMİK 2D GEMİ KESİTİ & SQUAT SİMÜLASYONU
    item {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Dinamik Gemi Su Altı Kesiti & Squat Analizi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("squat_simulation_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(WarningAmberLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Kanal Hızı & Hidrodinamik Squat",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary
                )
                Text(
                  text = "Sığ sularda hız arttıkça gemi tabana çöker",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Hız Kaydırıcısı (Slider)
          val speedVal = uiState.speedStr.toFloatOrNull() ?: 6f
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Kanal Seyir Hızı:",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = TextPrimary
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PrimaryBlueLight,
              border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder)
            ) {
              Text(
                text = "${String.format(java.util.Locale.US, "%.1f", speedVal)} knot",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = PrimaryBlue,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Slider(
            value = speedVal,
            onValueChange = { viewModel.updateSpeed(String.format(java.util.Locale.US, "%.1f", it)) },
            valueRange = 2f..14f,
            steps = 11,
            colors = SliderDefaults.colors(
              thumbColor = PrimaryBlue,
              activeTrackColor = PrimaryBlue,
              inactiveTrackColor = CardSubtleBorder
            ),
            modifier = Modifier.testTag("speed_slider")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Hıza göre Squat Değişim Tablosu
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SquatSpeedStepCard(
              speed = "4 knot",
              squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (16.0 / 100.0))}m",
              isCurrent = speedVal in 3.5f..4.5f,
              modifier = Modifier.weight(1f)
            )
            SquatSpeedStepCard(
              speed = "6 knot",
              squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (36.0 / 100.0))}m",
              isCurrent = speedVal in 5.5f..6.5f,
              modifier = Modifier.weight(1f)
            )
            SquatSpeedStepCard(
              speed = "8 knot",
              squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (64.0 / 100.0))}m",
              isCurrent = speedVal in 7.5f..8.5f,
              modifier = Modifier.weight(1f)
            )
            SquatSpeedStepCard(
              speed = "10 knot",
              squat = "${String.format(java.util.Locale.US, "%.2f", 2 * analysis.vessel.blockCoefficient * (100.0 / 100.0))}m",
              isCurrent = speedVal in 9.5f..10.5f,
              modifier = Modifier.weight(1f)
            )
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
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(
        if (isCurrent) PrimaryBlueLight else CardSubtle,
        RoundedCornerShape(8.dp)
      )
      .border(
        1.dp,
        if (isCurrent) PrimaryBlueBorder else CardBorder,
        RoundedCornerShape(8.dp)
      )
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = speed,
      style = MaterialTheme.typography.labelSmall,
      color = if (isCurrent) PrimaryBlue else TextMuted
    )
    Text(
      text = squat,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      color = if (isCurrent) PrimaryBlue else TextPrimary
    )
  }
}
