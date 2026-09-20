package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ShipCrossSectionCanvas(
  analysis: NavigationAnalysis,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  // Dalga animasyonu (Hafif ve pürüzsüz)
  val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
  val waveOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2f * Math.PI.toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave"
  )

  val currentTide = analysis.curvePoints.minByOrNull {
    kotlin.math.abs(it.hourOfDay - analysis.currentHour)
  }?.tideHeight ?: analysis.maxTideHeight24h

  val totalCurrentDepth = analysis.chartedDepthMeters + currentTide
  val dynamicDraft = analysis.actualDraftMeters + analysis.calculatedSquatMeters
  val currentUkc = totalCurrentDepth - dynamicDraft
  val isUkcSafe = currentUkc >= analysis.minUkcMeters

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("ship_cross_section_card"),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
      // Başlık & UKC Emniyet Rozeti (Tek satırda kibar ve kompakt)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isDarkMode) PrimaryBlueLight.copy(alpha = 0.5f) else Color(0xFFDBEAFE),
            modifier = Modifier.size(18.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.DirectionsBoat,
                contentDescription = null,
                tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                modifier = Modifier.size(11.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Gemi Hidrostatik & Derinlik Kesiti",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = textPrimary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "• ${analysis.vessel.name.ifBlank { "Gemi" }}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
            maxLines = 1
          )
        }

        Surface(
          shape = RoundedCornerShape(3.5.dp),
          color = if (isUkcSafe) (if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDarkMode) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isUkcSafe) (if (isDarkMode) Color(0xFF059669) else Color(0xFF10B981)) else (if (isDarkMode) Color(0xFFDC2626) else Color(0xFFEF4444))
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isUkcSafe) Icons.Default.Info else Icons.Default.Warning,
              contentDescription = null,
              tint = if (isUkcSafe) (if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDarkMode) Color(0xFFF87171) else Color(0xFFDC2626)),
              modifier = Modifier.size(9.dp)
            )
            Spacer(modifier = Modifier.width(2.5.dp))
            val ukcFormatted = String.format(Locale.US, "%.1fm", currentUkc)
            Text(
              text = if (isUkcSafe) "UKC GÜVENLİ (+$ukcFormatted)" else "UKC RİSKLİ ($ukcFormatted)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
              color = if (isUkcSafe) (if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDarkMode) Color(0xFFF87171) else Color(0xFFDC2626))
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 2D Kesit Çizimi Canvas (Kibar & Ekranda Az Yer Kaplayan 95dp Yükseklik)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(95.dp)
          .background(Color(0xFF090F24), RoundedCornerShape(6.dp))
          .border(0.5.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFF1E293B), RoundedCornerShape(6.dp))
          .padding(3.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w: Float = size.width
          val h: Float = size.height

          val skyHeight: Float = h * 0.19f
          val waterLineY: Float = skyHeight
          val seabedY: Float = h * 0.86f
          val availableWaterHeightPx: Float = seabedY - waterLineY

          // Gökyüzü
          drawRect(
            color = Color(0xFF060A18),
            topLeft = Offset(0f, 0f),
            size = Size(w, waterLineY)
          )

          // Deniz Tabanı
          val seabedPath = Path().apply {
            moveTo(0f, seabedY)
            lineTo(w, seabedY)
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(
            path = seabedPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723)),
              startY = seabedY,
              endY = h
            )
          )

          // Deniz tabanı tarama çizgileri
          for (xStep in 0..(w / 24f).toInt()) {
            val startX = xStep * 24f
            drawLine(
              color = Color(0x22000000),
              start = Offset(startX, seabedY),
              end = Offset(startX + 12f, h),
              strokeWidth = 1.dp.toPx()
            )
          }

          // Su Katmanı
          val waterPath = Path().apply {
            moveTo(0f, waterLineY)
            val waveSegments = 30
            for (i in 0..waveSegments) {
              val x = (w / waveSegments.toFloat()) * i.toFloat()
              val y = waterLineY + (kotlin.math.sin((i * 0.4f) + waveOffset) * 2f)
              lineTo(x, y)
            }
            lineTo(w, seabedY)
            lineTo(0f, seabedY)
            close()
          }

          drawPath(
            path = waterPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xA60284C7), Color(0xC00369A1), Color(0xEB082F49)),
              startY = waterLineY,
              endY = seabedY
            )
          )

          // Su çizgisi
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(0f, waterLineY),
            end = Offset(w, waterLineY),
            strokeWidth = 1.2.dp.toPx()
          )

          // Gemi Gövdesi Orantıları
          val shipCenter: Float = w * 0.44f
          val shipBeamPx: Float = (w * 0.35f).coerceIn(75f, 130f)

          val maxDepthRef: Float = (totalCurrentDepth.toFloat() + 1.5f).coerceAtLeast(8.0f)
          val pixelsPerMeter: Float = availableWaterHeightPx / maxDepthRef

          val draftPx: Float = (analysis.actualDraftMeters.toFloat() * pixelsPerMeter).coerceIn(20f, (availableWaterHeightPx - 6f).coerceAtLeast(20f))
          val squatPx: Float = (analysis.calculatedSquatMeters.toFloat() * pixelsPerMeter).coerceAtLeast(2f)
          val keelY: Float = waterLineY + draftPx + squatPx

          val hullTopY: Float = waterLineY - 14.dp.toPx()
          val hullHalfWidth: Float = shipBeamPx / 2f
          val keelHalfWidth: Float = hullHalfWidth * 0.70f
          val bottomY: Float = waterLineY + draftPx

          val hullPath = Path().apply {
            moveTo(shipCenter - hullHalfWidth, hullTopY)
            lineTo(shipCenter + hullHalfWidth, hullTopY)
            lineTo(shipCenter + hullHalfWidth, bottomY - 6.dp.toPx())
            quadraticBezierTo(
              shipCenter + hullHalfWidth, bottomY,
              shipCenter + keelHalfWidth, bottomY
            )
            lineTo(shipCenter - keelHalfWidth, bottomY)
            quadraticBezierTo(
              shipCenter - hullHalfWidth, bottomY,
              shipCenter - hullHalfWidth, bottomY - 6.dp.toPx()
            )
            close()
          }

          // Gövde boyaması
          val hullGradientSplit = ((waterLineY - hullTopY) / (bottomY - hullTopY)).coerceIn(0f, 1f)
          drawPath(
            path = hullPath,
            brush = Brush.verticalGradient(
              0.0f to Color(0xFF1E293B),
              hullGradientSplit to Color(0xFF1E293B),
              hullGradientSplit to Color(0xFFB91C1C),
              1.0f to Color(0xFF7F1D1D),
              startY = hullTopY,
              endY = bottomY
            )
          )

          drawPath(
            path = hullPath,
            color = Color.White.copy(alpha = 0.8f),
            style = Stroke(width = 0.8.dp.toPx())
          )

          // Üstyapı / Köprüüstü (Kibar, minik)
          val deck1W = shipBeamPx * 0.75f
          val deck1H = 5.dp.toPx()
          val deck1Y = hullTopY - deck1H
          drawRoundRect(color = Color(0xFFF1F5F9), topLeft = Offset(shipCenter - deck1W / 2, deck1Y), size = Size(deck1W, deck1H), cornerRadius = CornerRadius(1.5.dp.toPx()))

          val bridgeW = shipBeamPx * 0.44f
          val bridgeH = 6.dp.toPx()
          val bridgeY = deck1Y - bridgeH
          drawRoundRect(color = Color(0xFFFFFFFF), topLeft = Offset(shipCenter - bridgeW / 2, bridgeY), size = Size(bridgeW, bridgeH), cornerRadius = CornerRadius(1.5.dp.toPx()))

          val windowW = bridgeW * 0.65f
          val windowH = 2.5.dp.toPx()
          drawRect(color = Color(0xFF0284C7), topLeft = Offset(shipCenter - windowW / 2, bridgeY + 1.5.dp.toPx()), size = Size(windowW, windowH))

          // Radar direği
          drawLine(color = Color(0xFF94A3B8), start = Offset(shipCenter, bridgeY), end = Offset(shipCenter, bridgeY - 6.dp.toPx()), strokeWidth = 1.dp.toPx())

          // Squat alanı
          val squatPath = Path().apply {
            moveTo(shipCenter - (hullHalfWidth * 0.82f), waterLineY + draftPx)
            lineTo(shipCenter + (hullHalfWidth * 0.82f), waterLineY + draftPx)
            lineTo(shipCenter + (hullHalfWidth * 0.78f), keelY)
            lineTo(shipCenter - (hullHalfWidth * 0.78f), keelY)
            close()
          }
          drawPath(
            path = squatPath,
            color = WarningAmber.copy(alpha = 0.45f)
          )

          // UKC Bölgesi
          val ukcTopY: Float = keelY
          val ukcBottomY: Float = seabedY
          val ukcHeightPx: Float = (ukcBottomY - ukcTopY).coerceAtLeast(0f)

          val ukcColor = if (isUkcSafe) Color(0xFF10B981) else Color(0xFFEF4444)
          drawRect(
            color = ukcColor.copy(alpha = 0.30f),
            topLeft = Offset(shipCenter - (hullHalfWidth * 0.72f), ukcTopY),
            size = Size(hullHalfWidth * 1.44f, ukcHeightPx)
          )

          // Sağ Taraf: Metrik Derinlik Çizgisi
          val rulerX: Float = w * 0.86f
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(rulerX, waterLineY),
            end = Offset(rulerX, seabedY),
            strokeWidth = 1.dp.toPx()
          )

          val totalDepthText = textMeasurer.measure(
            text = "Su: ${String.format(Locale.US, "%.1fm", totalCurrentDepth)}",
            style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
          )
          drawText(
            textLayoutResult = totalDepthText,
            topLeft = Offset(rulerX - totalDepthText.size.width.toFloat() - 3.dp.toPx(), (waterLineY + seabedY) / 2f - 5.dp.toPx())
          )

          // Sol Taraf: Draft, Squat & UKC Etiketleri (Düzgün Hizalı, Çakışmayan Mini HUD Paneli)
          val hudX = 5.dp.toPx()
          val hudY = waterLineY + 3.dp.toPx()
          val hudPadX = 4.5.dp.toPx()
          val hudPadY = 3.dp.toPx()

          val draftStr = "Draft: ${String.format(Locale.US, "%.1fm", analysis.actualDraftMeters)}"
          val squatStr = "+Sq: ${String.format(Locale.US, "%.2fm", analysis.calculatedSquatMeters)}"
          val ukcStr = "UKC: ${String.format(Locale.US, "%.2fm", currentUkc)}"

          val draftLabel = textMeasurer.measure(
            text = draftStr,
            style = TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
          )
          val squatLabel = textMeasurer.measure(
            text = squatStr,
            style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = WarningAmber)
          )
          val ukcLabel = textMeasurer.measure(
            text = ukcStr,
            style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Black, color = ukcColor)
          )

          val maxLabelW = maxOf(
            draftLabel.size.width.toFloat(),
            squatLabel.size.width.toFloat(),
            ukcLabel.size.width.toFloat()
          )
          val hudW = maxLabelW + (hudPadX * 2f)
          val lineGap = 10.dp.toPx()
          val hudH = (lineGap * 2f) + ukcLabel.size.height.toFloat() + (hudPadY * 2f)

          // Arka plan panel kutusu
          drawRoundRect(
            color = Color(0xD9060A18),
            topLeft = Offset(hudX, hudY),
            size = Size(hudW, hudH),
            cornerRadius = CornerRadius(4.dp.toPx())
          )
          drawRoundRect(
            color = Color(0x3338BDF8),
            topLeft = Offset(hudX, hudY),
            size = Size(hudW, hudH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 0.6.dp.toPx())
          )

          // 3 Satır Düzgün Hizalı ve Sabit Aralıklı Metinler
          drawText(
            textLayoutResult = draftLabel,
            topLeft = Offset(hudX + hudPadX, hudY + hudPadY)
          )
          drawText(
            textLayoutResult = squatLabel,
            topLeft = Offset(hudX + hudPadX, hudY + hudPadY + lineGap)
          )
          drawText(
            textLayoutResult = ukcLabel,
            topLeft = Offset(hudX + hudPadX, hudY + hudPadY + (lineGap * 2f))
          )

          val seabedLabel = textMeasurer.measure(
            text = "Taban: ${String.format(Locale.US, "%.1fm", analysis.chartedDepthMeters)}",
            style = TextStyle(fontSize = 7.sp, color = Color(0xFFD7CCC8))
          )
          drawText(
            textLayoutResult = seabedLabel,
            topLeft = Offset(shipCenter - (seabedLabel.size.width.toFloat() / 2f), seabedY + 1.dp.toPx())
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Hidrodinamik Bilgi Çipleri (Kompakt ve Alçak Boyutlu)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        CompactInfoChip(
          title = "Anlık Toplam Su",
          value = "${String.format(Locale.US, "%.1f", totalCurrentDepth)} m",
          subtitle = "Harita + Gelgit",
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
        CompactInfoChip(
          title = "Squat (Çökelme)",
          value = "+${String.format(Locale.US, "%.2f", analysis.calculatedSquatMeters)} m",
          subtitle = "${String.format(Locale.US, "%.1f", analysis.vesselSpeedKnots)} kn GPS",
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
        CompactInfoChip(
          title = "Gerekli Emniyet",
          value = "${String.format(Locale.US, "%.1f", analysis.minUkcMeters)} m",
          subtitle = "Min UKC Limiti",
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // En Alttaki Seyir & Vektör Bilgileri: Rota, Hız ve Bir Alt Satırda Akıntı, Rüzgar
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = subtleBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp)
        ) {
          // 1. Satır: Rota ve Hız
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = if (isDarkMode) MarineCyan else PrimaryBlue,
                modifier = Modifier.size(9.5.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              val headingDir = extractCleanCardinal(analysis.vesselHeadingCardinal)
              Text(
                text = "Rota: ${String.format(Locale.US, "%03d°", analysis.vesselHeadingDegrees)} ($headingDir)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = if (isDarkMode) MarineCyan else PrimaryBlue,
                modifier = Modifier.size(9.5.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              val speedFormatted = String.format(Locale.US, "%.1f", analysis.vesselSpeedKnots)
              Text(
                text = "Hız: $speedFormatted kts",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))
          HorizontalDivider(
            color = cardBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 1.dp)
          )
          Spacer(modifier = Modifier.height(1.dp))

          // 2. Satır (Bir alt satırda): Akıntı ve Rüzgar (Sığacak şekilde sade, net ve tek satırda)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1.1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Waves,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue,
                modifier = Modifier.size(9.5.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              val currSpeed = String.format(Locale.US, "%.1f", analysis.currentInfo.speedKnots)
              val curDir = extractCleanCardinal(analysis.currentInfo.directionCardinal)
              val curPhase = extractCleanPhase(analysis.currentInfo.phaseName)
              Text(
                text = "Akıntı: $currSpeed kts ($curDir) • $curPhase",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.sp),
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Row(
              modifier = Modifier.weight(0.9f),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue,
                modifier = Modifier.size(9.5.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              val windSpeed = String.format(Locale.US, "%.1f", analysis.windInfo.speedKnots)
              val windDir = extractCleanCardinal(analysis.windInfo.directionCardinal)
              Text(
                text = "Rüzgar: $windSpeed kts ($windDir)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.sp),
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompactInfoChip(
  title: String,
  value: String,
  subtitle: String? = null,
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)
  val cardBorder = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
  val textPrimary = if (isDarkMode) Color.White else Color(0xFF0F172A)
  val textMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

  Surface(
    shape = RoundedCornerShape(4.dp),
    color = cardBg,
    border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.5.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp),
        color = textMuted,
        maxLines = 1
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp),
        color = textPrimary,
        maxLines = 1
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Normal),
          color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
          maxLines = 1
        )
      }
    }
  }
}

private fun extractCleanCardinal(cardinal: String): String {
  // Örnek: "KD / Kuzeydoğu (045°)" -> "KD"
  // Örnek: "D / Doğu (090°)" -> "D"
  val raw = cardinal.substringBefore("/").substringBefore("(").trim()
  return raw.ifBlank { cardinal }
}

private fun extractCleanPhase(phase: String): String {
  // Örnek: "Çekilme Akıntısı (Ebb Stream)" -> "Çekilme"
  // Örnek: "Taşkın Akıntısı (Flood Stream)" -> "Taşkın"
  // Örnek: "Ölü Su (Slack Water)" -> "Ölü Su"
  val raw = phase.substringBefore("Akıntısı").substringBefore("(").trim()
  return raw.ifBlank { phase }
}

