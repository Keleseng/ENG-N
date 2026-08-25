package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarineWeather
import com.example.ui.theme.*

@Composable
fun MarineWeatherCard(
  weather: MarineWeather,
  isGpsActive: Boolean,
  onRefreshWeather: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_marine_weather"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // 1. Üst Başlık ve Canlı Durum
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(
                Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
                RoundedCornerShape(10.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.WbSunny,
              contentDescription = null,
              tint = Color(0xFFFEF08A),
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Deniz & Hava Durumu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (weather.isLiveFromNetwork) SeaGreenLight else PrimaryBlueLight
              ) {
                Text(
                  text = if (weather.isLiveFromNetwork) "🛰️ CANLI GPS" else "YEREL TAHMİN",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                  ),
                  color = if (weather.isLiveFromNetwork) SeaGreenDark else PrimaryBlueDark,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Koordinat: ${String.format("%.4f", weather.latitude)}°N, ${String.format("%.4f", weather.longitude)}°E • Güncel: ${weather.lastUpdatedFormatted}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = TextMuted
            )
          }
        }

        IconButton(
          onClick = onRefreshWeather,
          modifier = Modifier.testTag("btn_refresh_marine_weather")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Yenile",
            tint = PrimaryBlueDark
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Ana Özet Bannerı (Sıcaklık + Dalga + Rüzgar)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F9FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Sıcaklık & Hava
          Column {
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "${weather.temperatureC}°",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 28.sp
                ),
                color = TextPrimary
              )
              Text(
                text = "C",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
              )
            }
            Text(
              text = weather.weatherConditionDescription,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = PrimaryBlueDark
            )
          }

          // Deniz Dalga Durumu
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Waves,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${weather.waveHeightMeters} m",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0369A1)
              )
            }
            Text(
              text = "Periyot: ${weather.wavePeriodSeconds}s",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = TextMuted
            )
            Text(
              text = weather.seaStateDescription,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
              ),
              color = Color(0xFF075985)
            )
          }

          // Rüzgar ve Hamle
          Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = PrimaryBlueDark,
                modifier = Modifier
                  .size(16.dp)
                  .rotate(weather.windDirectionDegrees.toFloat())
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${weather.windSpeedKnots} kn",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = PrimaryBlueDark
              )
            }
            Text(
              text = "${weather.windDirectionCardinal} (${weather.windDirectionDegrees}°)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
              color = TextPrimary
            )
            Text(
              text = "Hamle: ${weather.windGustsKnots} kn",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
              ),
              color = Color(0xFFD97706)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3. 2x2 veya 4'lü Detay Parametre Izgarası
      // (Bağıl Nem, Basınç, Rüzgar Hamlesi, Yağış Durumu)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Bağıl Nem
        WeatherMetricChip(
          title = "Bağıl Nem",
          value = "%${weather.relativeHumidityPercent}",
          subtitle = if (weather.relativeHumidityPercent > 80) "Yüksek Nem" else "Normal",
          icon = Icons.Default.WaterDrop,
          iconTint = Color(0xFF0284C7),
          modifier = Modifier.weight(1f)
        )

        // Hava Basıncı
        WeatherMetricChip(
          title = "Yüzey Basıncı",
          value = "${weather.surfacePressureHpa} hPa",
          subtitle = if (weather.surfacePressureHpa < 1010) "Alçak Basınç" else "Kararlı Basınç",
          icon = Icons.Default.Speed,
          iconTint = Color(0xFF7C3AED),
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Rüzgar Hamlesi (Gusts)
        WeatherMetricChip(
          title = "Rüzgar Hamlesi",
          value = "${weather.windGustsKnots} kn",
          subtitle = weather.beaufortDescription,
          icon = Icons.Default.Air,
          iconTint = Color(0xFFD97706),
          modifier = Modifier.weight(1f)
        )

        // Yağış Durumu
        WeatherMetricChip(
          title = "Yağış Durumu",
          value = "${weather.precipitationMm} mm/h",
          subtitle = weather.precipitationStateText,
          icon = Icons.Default.Grain,
          iconTint = if (weather.precipitationMm > 0.0) Color(0xFF2563EB) else TextMuted,
          modifier = Modifier.weight(1f)
        )
      }

      // 4. GPS GÜN DOĞUMU & GÜN BATIMI (ASTRONOMİK SEYİR BİLGİSİ)
      val sun = weather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(weather.latitude, weather.longitude)
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = WarningAmberLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmberBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.WbTwilight,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Güneş Doğuş & Batış Saatleri (GPS Mevkii)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = WarningAmber
              )
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (sun.isDaylightNow) WarningAmber.copy(alpha = 0.2f) else CardSubtle
            ) {
              Text(
                text = if (sun.isDaylightNow) "☀️ GÜNDÜZ" else "🌙 GECE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                color = if (sun.isDaylightNow) WarningAmber else TextPrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }

          // Güneş Doğuş & Batış Saatleri Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Gün Doğumu
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardSubtle,
              border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(WarningAmberLight, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.WbSunny, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Doğumu", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = TextMuted), softWrap = true)
                  Text(sun.sunriseFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = WarningAmber, fontSize = 13.sp))
                  Text("Şafak: ${sun.dawnCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = TextMuted), softWrap = true)
                }
              }
            }

            // Gün Batımı
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardSubtle,
              border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(PrimaryBlueLight, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Nightlight, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Batımı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = TextMuted), softWrap = true)
                  Text(sun.sunsetFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color(0xFFC2410C), fontSize = 13.sp))
                  Text("Alaca: ${sun.duskCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = TextMuted), softWrap = true)
                }
              }
            }
          }

          // Gün Işığı Özeti Alt Satır
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "☀️ Toplam Gün Işığı: ${sun.daylightDurationFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF78350F))
            )
            Text(
              text = "Öğle (Zenit): ${sun.solarNoonFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF92400E))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WeatherMetricChip(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = CardSubtle,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder.copy(alpha = 0.6f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
          color = TextMuted,
          softWrap = true,
          modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.5.sp),
        color = TextPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium, lineHeight = 11.sp),
        color = TextMuted,
        softWrap = true,
        maxLines = 2
      )
    }
  }
}
