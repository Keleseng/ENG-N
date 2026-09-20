package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationPresets
import com.example.model.NavigationAnalysis
import com.example.ui.TideUiState
import com.example.ui.components.BridgeTelemetryCard
import com.example.ui.components.RealisticMoonPhaseCard
import com.example.ui.components.RuleOfTwelfthsCard
import com.example.ui.theme.*

@Composable
fun ReportView(
  uiState: TideUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isDarkMode = uiState.isDarkMode
  val analysis = uiState.analysis
  val scrollState = rememberScrollState()

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {

    // 0. Canlı Köprüüstü Seyir & Çevre Telemetrisi Kartı
    BridgeTelemetryCard(
      analysis = analysis,
      isDarkMode = isDarkMode
    )

    // 1. Gerçekçi Ay Evresi & Gelgit Çekim Katsayısı Kartı
    RealisticMoonPhaseCard(
      analysis = analysis,
      isDarkMode = isDarkMode
    )

    // 2. Rapor Başlık ve Kopyalama Butonu
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
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
                .size(36.dp)
                .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Description, contentDescription = null, tint = if (isDarkMode) MarineCyan else PrimaryBlueDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Seyir & Gelgit Emniyet Raporu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimary,
                softWrap = true
              )
              Text(
                text = "${analysis.vessel.name} • ${analysis.location.name}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkMode) MarineCyan else PrimaryBlue,
                softWrap = true
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              val reportText = buildReportString(uiState)
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Seyir Raporu", reportText)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Seyir Raporu Panoya Kopyalandı", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            modifier = Modifier.testTag("btn_copy_report")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kopyala", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // 3. Emniyet Kontrol Listesi
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .background(if (isDarkMode) SeaGreenLight else Color(0xFFD1FAE5), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (isDarkMode) SeaGreen else Color(0xFF059669), modifier = Modifier.size(16.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Köprüüstü Emniyet Kontrol Listesi",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textPrimary
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        analysis.safetyChecklist.forEach { (itemText, isPassed) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(20.dp)
                .background(if (isPassed) (if (isDarkMode) SeaGreenLight else Color(0xFFD1FAE5)) else (if (isDarkMode) DangerRedLight else Color(0xFFFEE2E2)), CircleShape)
                .border(1.dp, if (isPassed) SeaGreenBorder else DangerRedBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isPassed) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed,
                modifier = Modifier.size(13.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = itemText,
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
              color = if (isPassed) textPrimary else DangerRed,
              softWrap = true,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. 12'ler Kuralı Tablosu
    RuleOfTwelfthsCard(
      steps = analysis.ruleOfTwelfths,
      isDarkMode = isDarkMode
    )

    // 5. Detaylı Metin Raporu Çerçevesi
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Resmi Seyir, Meteoroloji ve Gelgit Bildirimi",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = textMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(subtleBg, RoundedCornerShape(8.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(
            text = buildReportString(uiState),
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              lineHeight = 18.sp
            ),
            color = textSecondary
          )
        }
      }
    }
  }
}

private fun buildReportString(uiState: TideUiState): String {
  val analysis = uiState.analysis
  val weather = uiState.marineWeather
  val speedRes = uiState.speedCalculationResult

  val sb = StringBuilder()
  sb.appendLine("==========================================")
  sb.appendLine("   DENİZ SEYİR, METEOROLOJİ & GELGİT RAPORU")
  sb.appendLine("==========================================")
  sb.appendLine("Tarih: ${analysis.selectedDateFormatted}")
  sb.appendLine("Ay Evresi: ${analysis.moonPhaseName}")
  sb.appendLine("")
  sb.appendLine("[GEMİ BİLGİLERİ]")
  sb.appendLine("• Gemi Adı: ${analysis.vessel.name}")
  sb.appendLine("• Boy (LOA): ${analysis.vessel.loaMeters} m")
  sb.appendLine("• En (Beam): ${analysis.vessel.beamMeters} m")
  sb.appendLine("• Statik Draft: ${analysis.actualDraftMeters} m")
  sb.appendLine("• Seyir Hızı (STW): ${analysis.vesselSpeedKnots} knot")
  sb.appendLine("• Squat (Çökelme): ${analysis.calculatedSquatMeters} m")
  val dynamicDraft = analysis.actualDraftMeters + analysis.calculatedSquatMeters
  sb.appendLine("• Dinamik Seyir Draftı: ${String.format(java.util.Locale.US, "%.2f", dynamicDraft)} m")
  sb.appendLine("• İstenen Emniyet Payı (UKC): ${analysis.minUkcMeters} m")
  sb.appendLine("• Gereken Toplam Emniyetli Derinlik: ${analysis.totalRequiredDepthMeters} m")
  sb.appendLine("")
  sb.appendLine("[LİMAN & MEVKİ BİLGİSİ]")
  sb.appendLine("• Mevki / Liman: ${analysis.location.name}")
  sb.appendLine("• Harita Derinliği (Chart Datum): ${analysis.chartedDepthMeters} m")
  sb.appendLine("• 24 Saat Maksimum Su: ${analysis.maxAvailableDepth24h} m")
  sb.appendLine("• 24 Saat Minimum Su: ${analysis.minAvailableDepth24h} m")
  sb.appendLine("")
  sb.appendLine("[CANLI KÖPRÜÜSTÜ TELEMETRİSİ & METEOROLOJİ]")
  sb.appendLine("• Rüzgar Hızı: ${weather.windSpeedKnots} kn (${weather.windDirectionCardinal})")
  sb.appendLine("• Dalga Yüksekliği: ${weather.waveHeightMeters} m")
  sb.appendLine("• Akıntı Hızı: ${analysis.currentInfo.speedKnots} kn (${analysis.currentInfo.directionCardinal})")
  sb.appendLine("• Barometrik Basınç: ${weather.surfacePressureHpa} hPa")
  sb.appendLine("• Hava Durumu: ${weather.weatherConditionDescription} (${weather.seaStateDescription})")
  sb.appendLine("• Hava Sıcaklığı: ${weather.temperatureC}°C")
  sb.appendLine("")
  sb.appendLine("[SÜRAT & AKINTI VEKTÖR ANALİZİ]")
  sb.appendLine("• STW (Suya Göre Hız): ${String.format(java.util.Locale.US, "%.1f", speedRes.speedThroughWaterKnots)} kn")
  sb.appendLine("• SOG (Yere Göre Hız): ${String.format(java.util.Locale.US, "%.1f", speedRes.calculatedGroundSpeedKnots)} kn (GPS: ${String.format(java.util.Locale.US, "%.1f", speedRes.gpsSpeedKnots)} kn)")
  sb.appendLine("• Rota Üzerinde Hız Kazancı/Kaybı: ${if (speedRes.deltaSpeedKnots > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", speedRes.deltaSpeedKnots)} kn")
  sb.appendLine("• Rüzgar Sürüklenme Açısı: ${String.format(java.util.Locale.US, "%.1f", speedRes.windDriftAngleDegrees)}°")
  sb.appendLine("• Akıntı Değerlendirmesi: ${speedRes.speedEvaluationText}")
  sb.appendLine("")
  sb.appendLine("[GÜVENLİ GEÇİŞ PENCERELERİ]")
  if (analysis.safeWindows.isEmpty()) {
    sb.appendLine("• UYARI: Önümüzdeki 24 saat içinde güvenli geçiş penceresi BULUNMAMAKTADIR.")
  } else {
    analysis.safeWindows.forEachIndexed { i, win ->
      sb.appendLine("  ${i + 1}. Pencere: ${win.startTimeFormatted} - ${win.endTimeFormatted} (Süre: ${win.durationMinutes} dk)")
      sb.appendLine("     Pik HW: ${win.peakTimeFormatted} | Maks Su: ${win.maxWaterDepthMeters}m | Maks UKC: +${win.maxUkcMeters}m | Durum: ${win.rating.name}")
    }
  }
  sb.appendLine("")
  sb.appendLine("[EMNİYET ÖZETİ]")
  sb.appendLine(analysis.advisorySummary)
  sb.appendLine("==========================================")

  return sb.toString()
}
