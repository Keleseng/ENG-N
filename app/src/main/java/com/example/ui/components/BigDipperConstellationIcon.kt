package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Büyük Ayı (Ursa Major / Big Dipper) Takım Yıldızı ve Kutup Yıldızı (Polaris) Simgesi.
 * Denizcilikte yön bulma ve seyir rotasını simgeler.
 */
@Composable
fun BigDipperConstellationIcon(
  modifier: Modifier = Modifier,
  tint: Color = Color.Unspecified,
  isSelected: Boolean = false,
  iconSize: Dp = 24.dp,
  size: Dp = iconSize
) {
  val defaultColor = if (isSelected) Color(0xFFFBBF24) else Color(0xFF38BDF8)
  val starColor = if (tint != Color.Unspecified) tint else defaultColor
  val lineColor = starColor.copy(alpha = if (isSelected) 0.85f else 0.55f)
  val polarisColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFFF59E0B)

  val targetSize = if (size != 24.dp) size else iconSize

  Canvas(modifier = modifier.size(targetSize)) {
    val canvasWidth = this.size.width
    val canvasHeight = this.size.height

    // Büyük Ayı 7 Yıldızının Göreli Koordinatları (0..1 norm)
    val alkaid = Offset(canvasWidth * 0.08f, canvasHeight * 0.72f)
    val mizar   = Offset(canvasWidth * 0.22f, canvasHeight * 0.65f)
    val alioth  = Offset(canvasWidth * 0.36f, canvasHeight * 0.58f)
    val megrez  = Offset(canvasWidth * 0.52f, canvasHeight * 0.50f)
    val phecda  = Offset(canvasWidth * 0.54f, canvasHeight * 0.78f)
    val merak   = Offset(canvasWidth * 0.78f, canvasHeight * 0.76f)
    val dubhe   = Offset(canvasWidth * 0.76f, canvasHeight * 0.44f)

    // Kutup Yıldızı (Polaris)
    val polaris = Offset(canvasWidth * 0.88f, canvasHeight * 0.12f)

    // 1. Takım Yıldız Çizgileri
    val constellationPath = Path().apply {
      moveTo(alkaid.x, alkaid.y)
      lineTo(mizar.x, mizar.y)
      lineTo(alioth.x, alioth.y)
      lineTo(megrez.x, megrez.y)
      lineTo(phecda.x, phecda.y)
      lineTo(merak.x, merak.y)
      lineTo(dubhe.x, dubhe.y)
      lineTo(megrez.x, megrez.y)
    }

    drawPath(
      path = constellationPath,
      color = lineColor,
      style = Stroke(width = 1.6f, cap = StrokeCap.Round)
    )

    // Kılavuz çizgi
    drawLine(
      color = polarisColor.copy(alpha = 0.45f),
      start = merak,
      end = polaris,
      strokeWidth = 1.0f
    )

    // 2. Yıldızları Çiz
    val stars = listOf(alkaid, mizar, alioth, megrez, phecda, merak, dubhe)
    stars.forEach { star ->
      drawCircle(
        color = starColor.copy(alpha = 0.25f),
        radius = 3.5f,
        center = star
      )
      drawCircle(
        color = starColor,
        radius = 1.8f,
        center = star
      )
    }

    // 3. Kutup Yıldızı (Polaris)
    drawCircle(
      color = polarisColor.copy(alpha = 0.35f),
      radius = 5.0f,
      center = polaris
    )
    drawCircle(
      color = Color.White,
      radius = 2.4f,
      center = polaris
    )

    val pSize = 4.0f
    drawLine(polarisColor, Offset(polaris.x - pSize, polaris.y), Offset(polaris.x + pSize, polaris.y), strokeWidth = 1.2f)
    drawLine(polarisColor, Offset(polaris.x, polaris.y - pSize), Offset(polaris.x, polaris.y + pSize), strokeWidth = 1.2f)
  }
}
