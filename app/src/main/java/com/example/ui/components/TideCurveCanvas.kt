package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtremumType
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*
import java.util.*
import kotlin.math.roundToInt

@Composable
fun TideCurveCanvas(
  analysis: NavigationAnalysis,
  inspectedHour: Double?,
  onHourSelected: (Double?) -> Unit,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  // Gerekli minimum gelgit yüksekliği (Eşik)
  val requiredTideHeight = (analysis.totalRequiredDepthMeters - analysis.chartedDepthMeters)

  val minTide = (analysis.minTideHeight24h - 0.5).coerceAtMost(-0.5)
  val maxTide = (analysis.maxTideHeight24h + 0.8).coerceAtLeast(requiredTideHeight + 0.6)
  val tideSpan = (maxTide - minTide).coerceAtLeast(1.0)

  // Aktif incelenen nokta (eğer seçilmişse veya anlık saat)
  val activeHour = inspectedHour ?: analysis.currentHour
  val activePoint = analysis.curvePoints.minByOrNull { kotlin.math.abs(it.hourOfDay - activeHour) }
    ?: analysis.curvePoints.firstOrNull()

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("tide_curve_card"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // Üst Başlık ve İnteraktif Bilgi Çubuğu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "24 Saatlik Gelgit Eğrisi & Su Derinliği",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
          )
          Text(
            text = "Grafiğe dokunarak saatlik derinliği inceleyin",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary
          )
        }

        if (inspectedHour != null) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PrimaryBlueLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder)
          ) {
            Text(
              text = "Saat: ${activePoint?.timeFormatted ?: ""}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = PrimaryBlue,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // İnteraktif İnceleme Gösterge Paneli
      activePoint?.let { point ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(CardSubtle, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Seçili Saat", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(text = point.timeFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Gelgit Seviyesi", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(text = "${if (point.tideHeight >= 0) "+" else ""}${point.tideHeight} m", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Toplam Su", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(text = "${point.totalWaterDepth} m", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "UKC Boşluğu", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(
              text = "${if (point.availableUkc >= 0) "+" else ""}${point.availableUkc} m",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = if (point.isSafe) SeaGreen else DangerRed
            )
          }
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (point.isSafe) SeaGreenLight else DangerRedLight,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (point.isSafe) SeaGreenBorder else DangerRedBorder
            )
          ) {
            Text(
              text = if (point.isSafe) "GÜVENLİ" else "RİSKLİ",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
              color = if (point.isSafe) SeaGreen else DangerRed,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Canvas Çizim Alanı
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(165.dp)
          .background(CardSubtle, RoundedCornerShape(10.dp))
          .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = { offset ->
                val padLeft = 40.dp.toPx()
                val padRight = 16.dp.toPx()
                val chartW = size.width - padLeft - padRight
                if (chartW > 0) {
                  val h = ((offset.x - padLeft) / chartW * 24.0).coerceIn(0.0, 24.0)
                  onHourSelected(h)
                }
              }
            )
          }
          .pointerInput(Unit) {
            detectDragGestures { change, _ ->
              val padLeft = 40.dp.toPx()
              val padRight = 16.dp.toPx()
              val chartW = size.width - padLeft - padRight
              if (chartW > 0) {
                val h = ((change.position.x - padLeft) / chartW * 24.0).coerceIn(0.0, 24.0)
                onHourSelected(h)
              }
            }
          }
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val paddingLeft = 44.dp.toPx()
          val paddingRight = 16.dp.toPx()
          val paddingTop = 20.dp.toPx()
          val paddingBottom = 28.dp.toPx()

          val chartWidth = size.width - paddingLeft - paddingRight
          val chartHeight = size.height - paddingTop - paddingBottom

          if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

          fun hourToX(hour: Double): Float =
            (paddingLeft + (hour / 24.0 * chartWidth)).toFloat()

          fun tideToY(tide: Double): Float {
            val normalized = (tide - minTide) / tideSpan
            return (paddingTop + chartHeight * (1.0 - normalized)).toFloat()
          }

          // 1. Arka Plan Güvenli / Güvensiz Bölge Gölgelendirmesi
          val reqY = tideToY(requiredTideHeight).coerceIn(paddingTop, paddingTop + chartHeight)
          // Güvenli bölge (üst kısım)
          drawRect(
            color = Color(0x1510B981),
            topLeft = Offset(paddingLeft, paddingTop),
            size = Size(chartWidth, (reqY - paddingTop).coerceAtLeast(0f))
          )
          // Güvensiz bölge (alt kısım)
          drawRect(
            color = Color(0x15EF4444),
            topLeft = Offset(paddingLeft, reqY),
            size = Size(chartWidth, ((paddingTop + chartHeight) - reqY).coerceAtLeast(0f))
          )

          // 2. Yatay Izgara Çizgileri ve Derinlik Etiketleri
          val gridSteps = 4
          for (i in 0..gridSteps) {
            val tideVal = minTide + (tideSpan * (i.toDouble() / gridSteps))
            val y = tideToY(tideVal)

            drawLine(
              color = CardBorder,
              start = Offset(paddingLeft, y),
              end = Offset(paddingLeft + chartWidth, y),
              strokeWidth = 1.dp.toPx()
            )

            val tideText = String.format(Locale.US, "%.1fm", tideVal)
            val measuredText = textMeasurer.measure(
              text = tideText,
              style = TextStyle(fontSize = 9.sp, color = TextMuted)
            )
            drawText(
              textLayoutResult = measuredText,
              topLeft = Offset(4.dp.toPx(), y - (measuredText.size.height.toFloat() / 2f))
            )
          }

          // 3. Dikey Saat Izgara Çizgileri
          val hoursList = listOf(0, 4, 8, 12, 16, 20, 24)
          for (h in hoursList) {
            val x = hourToX(h.toDouble())
            drawLine(
              color = CardBorder,
              start = Offset(x, paddingTop),
              end = Offset(x, paddingTop + chartHeight),
              strokeWidth = 1.dp.toPx()
            )

            val label = String.format(Locale.US, "%02d:00", h % 24)
            val measuredHour = textMeasurer.measure(
              text = label,
              style = TextStyle(fontSize = 9.sp, color = TextMuted)
            )
            drawText(
              textLayoutResult = measuredHour,
              topLeft = Offset(x - (measuredHour.size.width.toFloat() / 2f), paddingTop + chartHeight + 6.dp.toPx())
            )
          }

          // 4. Kritik Güvenlik Eşik Çizgisi (Gerekli Min Derinlik Eşiği)
          val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
          drawLine(
            color = WarningAmber,
            start = Offset(paddingLeft, reqY),
            end = Offset(paddingLeft + chartWidth, reqY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = pathEffect
          )

          val thresholdLabel = textMeasurer.measure(
            text = "Kritik Eşik (${String.format(Locale.US, "%.1fm", requiredTideHeight)})",
            style = TextStyle(fontSize = 10.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
          )
          drawText(
            textLayoutResult = thresholdLabel,
            topLeft = Offset(paddingLeft + 8.dp.toPx(), reqY - thresholdLabel.size.height.toFloat() - 2.dp.toPx())
          )

          // 5. Gelgit Eğrisinin Doldurulması (Gradient Fill)
          val points = analysis.curvePoints
          if (points.isNotEmpty()) {
            val fillPath = Path().apply {
              moveTo(hourToX(points.first().hourOfDay), paddingTop + chartHeight)
              for (p in points) {
                lineTo(hourToX(p.hourOfDay), tideToY(p.tideHeight))
              }
              lineTo(hourToX(points.last().hourOfDay), paddingTop + chartHeight)
              close()
            }

            drawPath(
              path = fillPath,
              brush = Brush.verticalGradient(
                colors = listOf(Color(0x352563EB), Color(0x052563EB)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
              )
            )

            // 6. Gelgit Eğrisi Ana Çizgisi
            val strokePath = Path().apply {
              moveTo(hourToX(points.first().hourOfDay), tideToY(points.first().tideHeight))
              for (i in 1 until points.size) {
                lineTo(hourToX(points[i].hourOfDay), tideToY(points[i].tideHeight))
              }
            }

            drawPath(
              path = strokePath,
              color = PrimaryBlue,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
          }

          // 7. Yüksek Su (HW) ve Alçak Su (LW) Noktaları
          for (ext in analysis.extrema) {
            val exX = hourToX(ext.hourOfDay)
            val exY = tideToY(ext.tideHeightMeters)
            val isHw = ext.type == ExtremumType.HIGH_WATER

            // Nokta çemberi
            drawCircle(
              color = if (isHw) PrimaryBlue else WarningAmber,
              radius = 5.dp.toPx(),
              center = Offset(exX, exY)
            )
            drawCircle(
              color = Color.White,
              radius = 2.dp.toPx(),
              center = Offset(exX, exY)
            )

            val tagText = if (isHw) "HW ${ext.timeFormatted}" else "LW ${ext.timeFormatted}"
            val tagLayout = textMeasurer.measure(
              text = tagText,
              style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isHw) PrimaryBlue else WarningAmber)
            )

            val tagYOffset = if (isHw) -16.dp.toPx() else 8.dp.toPx()
            drawText(
              textLayoutResult = tagLayout,
              topLeft = Offset(exX - (tagLayout.size.width.toFloat() / 2f), exY + tagYOffset)
            )
          }

          // 8. Şu Anki Zaman Çizgisi (Current Time Line)
          val nowX = hourToX(analysis.currentHour)
          if (nowX in paddingLeft..(paddingLeft + chartWidth)) {
            drawLine(
              color = TextMuted.copy(alpha = 0.6f),
              start = Offset(nowX, paddingTop),
              end = Offset(nowX, paddingTop + chartHeight),
              strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
          }

          // 9. İnteraktif İnceleme Gösterge Çizgisi (Cursor Line)
          val cursorX = hourToX(activeHour)
          if (cursorX in paddingLeft..(paddingLeft + chartWidth)) {
            drawLine(
              color = PrimaryBlue,
              start = Offset(cursorX, paddingTop),
              end = Offset(cursorX, paddingTop + chartHeight),
              strokeWidth = 2.dp.toPx()
            )

            activePoint?.let { p ->
              val pointY = tideToY(p.tideHeight)
              drawCircle(
                color = if (p.isSafe) SeaGreen else DangerRed,
                radius = 7.dp.toPx(),
                center = Offset(cursorX, pointY)
              )
              drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(cursorX, pointY)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Lejant / Açıklama
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).background(Color(0x3010B981), RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Güvenli Geçiş Alanı", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).background(WarningAmber, RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Min Gerekli Eşik", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).background(PrimaryBlue, RoundedCornerShape(2.dp)))
          Spacer(modifier = Modifier.width(4.dp))
          Text("HW (Pik)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
      }
    }
  }
}

