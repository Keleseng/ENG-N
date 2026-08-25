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
  val analysis = uiState.analysis
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {

    // 0. Canlı Köprüüstü Seyir & Çevre Telemetrisi Kartı
    BridgeTelemetryCard(analysis = analysis)

    // 1. Gerçekçi Ay Evresi & Gelgit Çekim Katsayısı Kartı
    RealisticMoonPhaseCard(analysis = analysis)

    // 2. Rapor Başlık ve Kopyalama Butonu
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
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
                .background(PrimaryBlueLight, RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Seyir & Gelgit Emniyet Raporu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                softWrap = true
              )
              Text(
                text = "${analysis.vessel.name} • ${analysis.location.name}",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryBlue,
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
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .background(SeaGreenLight, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SeaGreen, modifier = Modifier.size(16.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Köprüüstü Emniyet Kontrol Listesi",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
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
                .background(if (isPassed) SeaGreenLight else DangerRedLight, CircleShape)
                .border(1.dp, if (isPassed) SeaGreenBorder else DangerRedBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isPassed) SeaGreen else DangerRed,
                modifier = Modifier.size(13.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = itemText,
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
              color = if (isPassed) TextPrimary else DangerRed,
              softWrap = true,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. 12'ler Kuralı Tablosu
    RuleOfTwelfthsCard(steps = analysis.ruleOfTwelfths)

    // 5. Detaylı Metin Raporu Çerçevesi
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Resmi Seyir, Meteoroloji ve Gelgit Bildirimi",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(CardSubtle, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(
            text = buildReportString(uiState),
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              lineHeight = 18.sp
            ),
            color = TextSecondary
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
  sb.appendLine("• İstenen Min UKC: ${analysis.minUkcMeters} m")
  sb.appendLine("• Toplam Gerekli Derinlik: ${analysis.totalRequiredDepthMeters} m")
  sb.appendLine("")
  sb.appendLine("[KONUM & KOORDİNATLAR]")
  sb.appendLine("• Konum/Liman: ${analysis.location.name}")
  sb.appendLine("• Standart Deniz GPS Koordinatı: ${LocationPresets.formatMarineCoordinates(analysis.customLat, analysis.customLon)}")
  sb.appendLine("• Enlem: ${LocationPresets.formatMarineLatitude(analysis.customLat)} | Boylam: ${LocationPresets.formatMarineLongitude(analysis.customLon)}")
  sb.appendLine("• Harita Derinliği (CD): ${analysis.chartedDepthMeters} m")
  if (uiState.mobEvent.isActive) {
    val mob = uiState.mobEvent
    sb.appendLine("🚨 [ACİL DURUM - DENİZE ADAM DÜŞTÜ (MOB) KAYDI]")
    sb.appendLine("• MOB Zamanı: ${mob.timeFormatted}")
    sb.appendLine("• MOB Koordinatı: ${LocationPresets.formatMarineCoordinates(mob.latitude, mob.longitude)}")
    sb.appendLine("• Gemiye Göre Mesafe: ${String.format(java.util.Locale.US, "%.1f", mob.calculateDistanceGomina(analysis.customLat, analysis.customLon))} Gomina (${String.format(java.util.Locale.US, "%.2f", mob.calculateDistanceNm(analysis.customLat, analysis.customLon))} NM / ${mob.calculateDistanceMeters(analysis.customLat, analysis.customLon).toInt()} m)")
    sb.appendLine("• Gemiye Göre Kerteriz: ${String.format(java.util.Locale.US, "%03d°", mob.calculateBearingDegrees(analysis.customLat, analysis.customLon))}")
  }
  val anchorCalc = uiState.anchorCalculationResult
  sb.appendLine("⚓ [DEMİRLEME VE SALMA DAİRESİ HESABI (ANCHORING & SWINGING CIRCLES)]")
  sb.appendLine("• a (Verilen Kaloma): ${anchorCalc.formattedA}")
  sb.appendLine("• b (Derinlik): ${anchorCalc.formattedB}")
  sb.appendLine("• c (Loçadan Demir Yerine Yatay Mesafe): ${anchorCalc.formattedC} [c = √(a² - b²)]")
  sb.appendLine("• d (1. Salma Dairesi - Köprüüstü/Radar): ${anchorCalc.formattedD} [d = Köprüüstü-Loça + c]")
  sb.appendLine("• e (Köprüüstü-Kıç Referans Çemberi): ${anchorCalc.formattedE} [e = Köprüüstü-Kıç + c]")
  sb.appendLine("• f (2. Salma Dairesi - Toplam Emniyet Çemberi): ${anchorCalc.formattedF} [f = Gemi Boyu LOA + c]")
  sb.appendLine("• Kaloma Oranı (a/b): ${String.format(java.util.Locale.US, "%.1f", anchorCalc.scopeRatio)}x (${anchorCalc.scopeStatus.labelTr})")
  sb.appendLine("• Zemin Türü: ${uiState.anchorBottomType.displayNameTr}")
  if (uiState.anchorEvent.isAnchored) {
    val anchor = uiState.anchorEvent
    val anchorDistGom = anchor.calculateDistanceGomina(analysis.customLat, analysis.customLon)
    val isDrag = anchor.isDragging(analysis.customLat, analysis.customLon)
    sb.appendLine("• Demir Durumu: ⚓ DEMİRDE (Nöbet Aktif)")
    sb.appendLine("  - Demir Zamanı: ${anchor.dropTimeFormatted}")
    sb.appendLine("  - Demir Mevkii: ${LocationPresets.formatMarineCoordinates(anchor.latitude, anchor.longitude)}")
    sb.appendLine("  - Emniyetli Salma Sınırı: ${anchor.safeSwingingRadiusGomina} Gomina (${anchorCalc.formattedF})")
    sb.appendLine("  - Anlık Demir Mesafesi: ${String.format(java.util.Locale.US, "%.2f", anchorDistGom)} Gomina (${anchor.calculateDistanceMeters(analysis.customLat, analysis.customLon).toInt()} m)")
    sb.appendLine("  - Nöbet Durumu: ${if (isDrag) "DİKKAT: DEMİR TARAMA / SALMA SINIRI AŞILDI" else "GÜVENLİ SALMA ALANINDA"}")
  }
  sb.appendLine("")
  sb.appendLine("")
  sb.appendLine("[CANLI DENİZ VE METEOROLOJİ VERİLERİ (WINDY / OPEN-METEO)]")
  val sunTimes = weather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(analysis.customLat, analysis.customLon)
  sb.appendLine("• Güneş Doğumu: ${sunTimes.sunriseFormatted} | Gün Batımı: ${sunTimes.sunsetFormatted} (Gün Işığı: ${sunTimes.daylightDurationFormatted})")
  sb.appendLine("• Sivil Şafak: ${sunTimes.dawnCivilFormatted} | Sivil Alacakaranlık: ${sunTimes.duskCivilFormatted}")
  sb.appendLine("• Hava Sıcaklığı: ${weather.temperatureC}°C | Bağıl Nem: %${weather.relativeHumidityPercent}")
  sb.appendLine("• Yüzey Hava Basıncı: ${weather.surfacePressureHpa} hPa")
  sb.appendLine("• Rüzgar Sürati & Yönü: ${weather.windSpeedKnots} kn @ ${weather.windDirectionDegrees}° (${weather.windDirectionCardinal})")
  sb.appendLine("• Rüzgar Hamlesi (Gusts): ${weather.windGustsKnots} kn | Beaufort: ${weather.beaufortDescription}")
  sb.appendLine("• Dalga Yüksekliği & Periyodu: ${weather.waveHeightMeters} m (${weather.wavePeriodSeconds}s) | ${weather.seaStateDescription}")
  sb.appendLine("• Yağış Durumu: ${weather.precipitationMm} mm/h (${weather.precipitationStateText})")
  sb.appendLine("• Genel Durum: ${weather.weatherConditionDescription}")
  sb.appendLine("")
  sb.appendLine("[GPS SÜRATI VE YERE GÖRE SÜRAT (SOG vs STW) VEKTÖR ANALİZİ]")
  sb.appendLine("• GPS Sürati (SOG): ${speedRes.gpsSpeedKnots} knot (Aktif: ${if (speedRes.isGpsActive) "Evet" else "Hayır"})")
  sb.appendLine("• Suya Göre Sürat (STW): ${speedRes.speedThroughWaterKnots} knot (Pruva: ${String.format(java.util.Locale.US, "%03d°", speedRes.vesselHeadingDegrees)})")
  sb.appendLine("• Hesaplanmış Yere Göre Sürat: ${speedRes.calculatedGroundSpeedKnots} knot (COG: ${String.format(java.util.Locale.US, "%03d°", speedRes.groundCourseDegrees)})")
  sb.appendLine("• Akıntı & Rüzgar Hız Kazancı/Kaybı: ${if (speedRes.deltaSpeedKnots > 0) "+" else ""}${speedRes.deltaSpeedKnots} knot")
  sb.appendLine("• Vektörel Değerlendirme: ${speedRes.speedEvaluationText}")
  sb.appendLine("• Sürüklenme / Sapma: ${speedRes.driftStatusText}")
  sb.appendLine("")
  sb.appendLine("[ANLIK KÖPRÜÜSTÜ TELEMETRİSİ & DERİNLİK HESABI]")
  sb.appendLine("• Mevki Harita Derinliği (CD): ${analysis.chartedDepthMeters} m")
  sb.appendLine("• Anlık Gelgit Yüksekliği: +${analysis.currentInstantTideHeightMeters} m")
  sb.appendLine("• HESAPLANAN ANLIK GERÇEK SU DERİNLİĞİ: ${analysis.currentInstantTotalDepthMeters} m")
  sb.appendLine("• Anlık Net UKC (Omurga Altı Emniyeti): +${analysis.currentInstantUkcMeters} m (${if (analysis.isCurrentlySafe) "GÜVENLİ" else "YETERSİZ/RİSKLİ"})")
  sb.appendLine("")
  sb.appendLine("[GÜVENLİ GİRİŞ - ÇIKIŞ PENCERELERİ]")
  if (analysis.safeWindows.isEmpty()) {
    sb.appendLine("UYARI: 24 saat içinde güvenli geçiş penceresi bulunamamıştır!")
  } else {
    analysis.safeWindows.forEach { w ->
      sb.appendLine("• Pencere #${w.id}: ${w.startTimeFormatted} - ${w.endTimeFormatted} (${w.durationMinutes} dk)")
      sb.appendLine("  - Pik Zamanı: ${w.peakTimeFormatted} (Su: ${w.maxWaterDepthMeters} m, UKC: +${w.maxUkcMeters} m)")
    }
  }
  sb.appendLine("")
  sb.appendLine("[SEYİR TAVSİYESİ]")
  sb.appendLine("${analysis.advisoryBadge}: ${analysis.advisorySummary}")
  sb.appendLine("==========================================")
  return sb.toString()
}
