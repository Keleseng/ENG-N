package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ShipCrossSectionCanvas(
  analysis: NavigationAnalysis,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  // Dalga animasyonu
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
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Gemi Hidrostatik & Derinlik Kesiti",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
          )
          Text(
            text = "${analysis.vessel.name} (Boy: ${analysis.vessel.loaMeters}m, En: ${analysis.vessel.beamMeters}m)",
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryBlue
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isUkcSafe) SeaGreenLight else DangerRedLight,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUkcSafe) SeaGreenBorder else DangerRedBorder
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isUkcSafe) Icons.Default.Info else Icons.Default.Warning,
              contentDescription = null,
              tint = if (isUkcSafe) SeaGreen else DangerRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isUkcSafe) "UKC GÜVENLİ" else "UKC RİSKLİ",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isUkcSafe) SeaGreen else DangerRed
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2D Kesit Çizimi Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(230.dp)
          .background(HeaderNavy, RoundedCornerShape(12.dp))
          .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
          .padding(8.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w: Float = size.width
          val h: Float = size.height

          val skyHeight: Float = h * 0.24f
          val waterLineY: Float = skyHeight
          val seabedY: Float = h * 0.86f
          val availableWaterHeightPx: Float = seabedY - waterLineY

          // Gökyüzü (Sky background)
          drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(0f, 0f),
            size = Size(w, waterLineY)
          )

          // Deniz Tabanı (Seabed)
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
              colors = listOf(Color(0xFF8D6E63), Color(0xFF5D4037)),
              startY = seabedY,
              endY = h
            )
          )

          // Deniz tabanı dokusu çizgileri
          for (xStep in 0..(w / 30f).toInt()) {
            val startX = xStep * 30f
            drawLine(
              color = Color(0x33000000),
              start = Offset(startX, seabedY),
              end = Offset(startX + 15f, h),
              strokeWidth = 2.dp.toPx()
            )
          }

          // Su Katmanı (Water column)
          val waterPath = Path().apply {
            moveTo(0f, waterLineY)
            // Sinüs dalgaları
            val waveSegments = 40
            for (i in 0..waveSegments) {
              val x = (w / waveSegments.toFloat()) * i.toFloat()
              val y = waterLineY + (kotlin.math.sin((i * 0.4f) + waveOffset) * 4f)
              lineTo(x, y)
            }
            lineTo(w, seabedY)
            lineTo(0f, seabedY)
            close()
          }

          drawPath(
            path = waterPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xCC0284C7), Color(0xDD0369A1), Color(0xFF0C4A6E)),
              startY = waterLineY,
              endY = seabedY
            )
          )

          // Su çizgisi (Waterline glow)
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(0f, waterLineY),
            end = Offset(w, waterLineY),
            strokeWidth = 2.dp.toPx()
          )

          // Gemi Gövdesi Çizimi (Ship Hull)
          val shipCenter: Float = w * 0.44f
          val shipBeamPx: Float = (w * 0.40f).coerceAtLeast(100f)

          // Orantılı derinlik piksel hesabı
          val maxDepthRef: Float = (totalCurrentDepth.toFloat() + 2.0f).coerceAtLeast(10.0f)
          val pixelsPerMeter: Float = availableWaterHeightPx / maxDepthRef

          val draftPx: Float = (analysis.actualDraftMeters.toFloat() * pixelsPerMeter).coerceIn(40f, (availableWaterHeightPx - 10f).coerceAtLeast(40f))
          val squatPx: Float = (analysis.calculatedSquatMeters.toFloat() * pixelsPerMeter).coerceAtLeast(4f)
          val keelY: Float = waterLineY + draftPx + squatPx

          // Gemi Gövdesi (Hull Polygon)
          val hullTopY: Float = waterLineY - 32.dp.toPx()
          val hullHalfWidth: Float = shipBeamPx / 2f

          val hullPath = Path().apply {
            moveTo(shipCenter - hullHalfWidth, hullTopY)
            lineTo(shipCenter + hullHalfWidth, hullTopY)
            // Bordadan omurgaya doğru kavis
            lineTo(shipCenter + (hullHalfWidth * 0.85f), waterLineY + draftPx)
            lineTo(shipCenter - (hullHalfWidth * 0.85f), waterLineY + draftPx)
            close()
          }

          // Gövde boyaması (Kırmızı karina altı / Koyu bordo)
          drawPath(
            path = hullPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFFDC2626)),
              startY = hullTopY,
              endY = waterLineY + draftPx
            )
          )

          drawPath(
            path = hullPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx())
          )

          // Köprüüstü (Superstructure)
          val bridgeW: Float = shipBeamPx * 0.45f
          val bridgeH: Float = 26.dp.toPx()
          drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(shipCenter - (bridgeW / 2f), hullTopY - bridgeH),
            size = Size(bridgeW, bridgeH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          // Köprüüstü pencereleri
          drawRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(shipCenter - (bridgeW / 2f) + 6.dp.toPx(), hullTopY - bridgeH + 6.dp.toPx()),
            size = Size(bridgeW - 12.dp.toPx(), 8.dp.toPx())
          )
          // Radar direği
          drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(shipCenter, hullTopY - bridgeH),
            end = Offset(shipCenter, hullTopY - bridgeH - 12.dp.toPx()),
            strokeWidth = 2.dp.toPx()
          )

          // Squat (Çökelme Alanı Gösterimi)
          val squatPath = Path().apply {
            moveTo(shipCenter - (hullHalfWidth * 0.85f), waterLineY + draftPx)
            lineTo(shipCenter + (hullHalfWidth * 0.85f), waterLineY + draftPx)
            lineTo(shipCenter + (hullHalfWidth * 0.80f), keelY)
            lineTo(shipCenter - (hullHalfWidth * 0.80f), keelY)
            close()
          }
          drawPath(
            path = squatPath,
            color = WarningAmber.copy(alpha = 0.45f)
          )

          // UKC (Omurga Altı Açıklığı) Gösterim Bölgesi
          val ukcTopY: Float = keelY
          val ukcBottomY: Float = seabedY
          val ukcHeightPx: Float = (ukcBottomY - ukcTopY).coerceAtLeast(0f)

          val ukcColor = if (isUkcSafe) SeaGreen else DangerRed
          drawRect(
            color = ukcColor.copy(alpha = 0.35f),
            topLeft = Offset(shipCenter - (hullHalfWidth * 0.75f), ukcTopY),
            size = Size(hullHalfWidth * 1.5f, ukcHeightPx)
          )

          // Sağ Taraf: Metrik Derinlik Cetveli & Oklar
          val rulerX: Float = w * 0.82f

          // Toplam Su Derinliği Oku
          drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(rulerX, waterLineY),
            end = Offset(rulerX, seabedY),
            strokeWidth = 2.dp.toPx()
          )

          val totalDepthText = textMeasurer.measure(
            text = "Toplam Su: ${String.format(Locale.US, "%.1fm", totalCurrentDepth)}",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
          )
          drawText(
            textLayoutResult = totalDepthText,
            topLeft = Offset(rulerX - totalDepthText.size.width.toFloat() - 6.dp.toPx(), (waterLineY + seabedY) / 2f)
          )

          // Sol Taraf: Draft, Squat & UKC Etiketleri
          val leftLabelX: Float = 10.dp.toPx()

          val draftLabel = textMeasurer.measure(
            text = "Draft: ${analysis.actualDraftMeters}m",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
          )
          drawText(
            textLayoutResult = draftLabel,
            topLeft = Offset(leftLabelX, waterLineY + (draftPx / 2f))
          )

          val squatLabel = textMeasurer.measure(
            text = "+Squat: ${analysis.calculatedSquatMeters}m",
            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = WarningAmber)
          )
          drawText(
            textLayoutResult = squatLabel,
            topLeft = Offset(leftLabelX, waterLineY + draftPx)
          )

          val ukcLabel = textMeasurer.measure(
            text = "Net UKC: ${String.format(Locale.US, "%.2fm", currentUkc)}",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ukcColor)
          )
          drawText(
            textLayoutResult = ukcLabel,
            topLeft = Offset(leftLabelX, ukcTopY + 2.dp.toPx())
          )

          val seabedLabel = textMeasurer.measure(
            text = "Deniz Tabanı (Harita: ${analysis.chartedDepthMeters}m)",
            style = TextStyle(fontSize = 9.sp, color = Color(0xFFD7CCC8))
          )
          drawText(
            textLayoutResult = seabedLabel,
            topLeft = Offset(shipCenter - (seabedLabel.size.width.toFloat() / 2f), seabedY + 4.dp.toPx())
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Hidrodinamik Açıklama Tablosu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        InfoChip(
          title = "Anlık Toplam Su",
          value = "${analysis.currentInstantTotalDepthMeters} m",
          modifier = Modifier.weight(1f)
        )
        InfoChip(
          title = "Squat (Çökelme)",
          value = "+${analysis.calculatedSquatMeters} m (${analysis.vesselSpeedKnots} kts)",
          modifier = Modifier.weight(1f)
        )
        InfoChip(
          title = "Gerekli Emniyet",
          value = "Min ${analysis.minUkcMeters} m UKC",
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Çevre & Seyir Vektörleri Şeridi
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = CardSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Rota: ${String.format("%03d°", analysis.vesselHeadingDegrees)} • Hız: ${analysis.vesselSpeedKnots} kts",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryBlueDark
          )
          Text(
            text = "Akıntı: ${analysis.currentInfo.speedKnots} kts • Rüzgar: ${analysis.windInfo.speedKnots} kts",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
          )
        }
      }
    }
  }
}

@Composable
private fun InfoChip(
  title: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(CardSubtle, RoundedCornerShape(8.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted, maxLines = 1)
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
  }
}

