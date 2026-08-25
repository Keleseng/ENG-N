package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationAnalysis
import com.example.ui.theme.*

@Composable
fun BridgeTelemetryCard(
  analysis: NavigationAnalysis,
  onOpenAisMap: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("bridge_telemetry_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Surface(
            shape = CircleShape,
            color = PrimaryBlueLight,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Explore,
                contentDescription = "Anlık Durum",
                tint = PrimaryBlueDark,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Anlık durum",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.3.sp),
              color = HeaderNavy,
              softWrap = true
            )
            Text(
              text = "${analysis.vessel.name} • Derinlik, Rota, Hız, Akıntı ve Vektör Analizi",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = TextMuted,
              softWrap = true
            )
          }
        }

        if (onOpenAisMap != null) {
          FilledTonalButton(
            onClick = onOpenAisMap,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = PrimaryBlueLight,
              contentColor = PrimaryBlueDark
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_bridge_open_map")
          ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Harita", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 1. ANA DERİNLİK HESAP BÖLÜMÜ (Mevki Harita Derinliği + Gelgit = Anlık Toplam Derinlik)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "ANLIK SU DERİNLİĞİ HESAPLAMASI",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = PrimaryBlueDark
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Harita Derinliği
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text(
                text = "Harita (CD)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
              Text(
                text = "${analysis.chartedDepthMeters} m",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
              )
            }

            Text(
              text = "+",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = PrimaryBlue,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Anlık Gelgit
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text(
                text = "Gelgit",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
              Text(
                text = "${if (analysis.currentInstantTideHeightMeters >= 0) "+" else ""}${analysis.currentInstantTideHeightMeters} m",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (analysis.currentInstantTideHeightMeters >= 0) SeaGreen else DangerRed
              )
            }

            Text(
              text = "=",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = PrimaryBlue,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Anlık Toplam Derinlik
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
              Text(
                text = "TOPLAM SU",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = HeaderNavy
              )
              Text(
                text = "${analysis.currentInstantTotalDepthMeters} m",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = PrimaryBlueDark
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Divider(color = CardBorder, thickness = 1.dp)
          Spacer(modifier = Modifier.height(8.dp))

          // Anlık UKC Durumu (Dikeyde kelimelerin alt satıra topluca geçişini sağlayan düzen)
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  if (analysis.isCurrentlySafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                  contentDescription = null,
                  tint = if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Net UKC (Omurga Payı):",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "+${analysis.currentInstantUkcMeters} m",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                  color = if (analysis.isCurrentlySafe) SeaGreen else DangerRed
                )
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (analysis.isCurrentlySafe) SeaGreenLight else DangerRedLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (analysis.isCurrentlySafe) SeaGreenBorder else DangerRedBorder)
              ) {
                Text(
                  text = if (analysis.isCurrentlySafe) "GÜVENLİ" else "YETERSİZ",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                  color = if (analysis.isCurrentlySafe) SeaGreen else DangerRed,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. SEYİR HAREKETİ: GİDİLEN YÖN (ROTASI / COG) & HIZ (SOG)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Gidilen Yön / Rota
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardSubtle,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Navigation,
                contentDescription = "Rota",
                tint = PrimaryBlue,
                modifier = Modifier
                  .size(16.dp)
                  .rotate(analysis.vesselHeadingDegrees.toFloat())
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "ROTA (COG)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = String.format("%03d°", analysis.vesselHeadingDegrees),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = TextPrimary
            )
            Text(
              text = analysis.vesselHeadingCardinal,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp),
              color = PrimaryBlueDark,
              softWrap = true
            )
          }
        }

        // Anlık Hız (SOG)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardSubtle,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Speed, contentDescription = "Hız", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "HIZ (SOG)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${analysis.vesselSpeedKnots} kts",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = TextPrimary
            )
            Text(
              text = "Squat: +${analysis.calculatedSquatMeters} m",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp),
              color = DangerRed,
              softWrap = true
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. ÇEVRESEL VERİLER: AKINTI VE RÜZGAR BİLGİLERİ
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Bölgenin Anlık Akıntısı
        val curr = analysis.currentInfo
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardSubtle,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Waves,
                contentDescription = "Akıntı",
                tint = PrimaryBlue,
                modifier = Modifier
                  .size(16.dp)
                  .rotate(curr.directionDegrees.toFloat())
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "AKINTI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${curr.speedKnots} kts • ${String.format("%03d°", curr.directionDegrees)}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
              color = HeaderNavy
            )
            Text(
              text = curr.phaseName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp),
              color = PrimaryBlueDark,
              softWrap = true
            )
            Text(
              text = curr.directionCardinal,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
              color = TextSecondary,
              softWrap = true
            )
          }
        }

        // Bölgenin Anlık Rüzgarı
        val wind = analysis.windInfo
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardSubtle,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Air,
                contentDescription = "Rüzgar",
                tint = PrimaryBlue,
                modifier = Modifier
                  .size(16.dp)
                  .rotate(wind.directionDegrees.toFloat())
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RÜZGAR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${wind.speedKnots} kts • ${String.format("%03d°", wind.directionDegrees)}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
              color = HeaderNavy
            )
            Text(
              text = "Beaufort ${wind.beaufortScale} • ${wind.directionCardinal}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp),
              color = PrimaryBlueDark,
              softWrap = true
            )
            Text(
              text = wind.seaStateDescription,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
              color = TextSecondary,
              softWrap = true
            )
          }
        }
      }
    }
  }
}

