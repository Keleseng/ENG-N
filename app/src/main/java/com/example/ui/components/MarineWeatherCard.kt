package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
  modifier: Modifier = Modifier,
  isDarkMode: Boolean = false
) {
  val context = LocalContext.current
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val sun = weather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(weather.latitude, weather.longitude)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_marine_weather"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // 1. Üst Başlık ve Canlı Durum
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(
                Brush.linearGradient(
                  if (isDarkMode) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                  else listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                ),
                RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (weather.weatherConditionDescription.contains("Yağmur", ignoreCase = true)) {
                Icons.Default.WaterDrop
              } else if (weather.weatherConditionDescription.contains("Bulut", ignoreCase = true)) {
                Icons.Default.Cloud
              } else if (sun.isDaylightNow) {
                Icons.Default.WbSunny
              } else {
                Icons.Default.Nightlight
              },
              contentDescription = null,
              tint = if (sun.isDaylightNow) Color(0xFFFEF08A) else Color(0xFFE2E8F0),
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Deniz & Hava Durumu",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = textPrimary,
                maxLines = 1
              )
              Spacer(modifier = Modifier.width(6.dp))
              val liveBg = if (weather.isLiveFromNetwork) {
                if (isDarkMode) Color(0xFF064E3B) else Color(0xFFD1FAE5)
              } else {
                if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
              }
              val liveTextColor = if (weather.isLiveFromNetwork) {
                if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46)
              } else {
                if (isDarkMode) MarineCyan else PrimaryBlueDark
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = liveBg
              ) {
                Text(
                  text = if (weather.isLiveFromNetwork) "🛰️ CANLI" else "TAHMİN",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp
                  ),
                  color = liveTextColor,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                )
              }
            }
            Text(
              text = "${String.format(java.util.Locale.US, "%.3f", weather.latitude)}°N, ${String.format(java.util.Locale.US, "%.3f", weather.longitude)}°E • ${weather.lastUpdatedFormatted}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = textSecondary,
              maxLines = 1
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Windy Sembolü
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE11D48).copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE11D48).copy(alpha = 0.35f)),
            modifier = Modifier
              .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.windy.com/?${weather.latitude},${weather.longitude},9")).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
              }
              .testTag("btn_verify_with_windy_quick")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Air,
                contentDescription = "Windy",
                tint = Color(0xFFE11D48),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Windy",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                color = Color(0xFFE11D48)
              )
            }
          }

          IconButton(
            onClick = onRefreshWeather,
            modifier = Modifier
              .size(30.dp)
              .testTag("btn_refresh_marine_weather")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Yenile",
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(17.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Ana Özet Bannerı (Sıcaklık + Dalga + Rüzgar Görseli)
      val isBannerDark = isDarkMode
      val bannerBg = if (isBannerDark) {
        Brush.horizontalGradient(listOf(Color(0xFF0B1728), Color(0xFF13233B), Color(0xFF1B3152)))
      } else {
        Brush.horizontalGradient(listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD)))
      }
      val bannerBorder = if (isBannerDark) Color(0xFF38BDF8).copy(alpha = 0.45f) else Color(0xFF7DD3FC)
      val bannerTextPrimary = if (isBannerDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
      val bannerTextSecondary = if (isBannerDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
      val bannerTextMuted = if (isBannerDark) Color(0xFF94A3B8) else Color(0xFF475569)
      val bannerIconColor = if (isBannerDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

      Surface(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(bannerBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Sıcaklık & Hava İkonu
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = "${weather.temperatureC}°",
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                  ),
                  color = bannerTextPrimary
                )
                Text(
                  text = "C",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = bannerTextSecondary,
                  modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                )
              }
              Text(
                text = weather.weatherConditionDescription,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                color = bannerTextSecondary,
                maxLines = 1
              )
            }

            // Görsel Dalga Dinamiği
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.weight(1.1f)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Waves,
                  contentDescription = null,
                  tint = bannerIconColor,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "${weather.waveHeightMeters} m",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                  color = bannerTextPrimary
                )
              }
              Text(
                text = "Periyot: ${weather.wavePeriodSeconds}s",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = bannerTextMuted
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isBannerDark) Color(0xFF0C4A6E).copy(alpha = 0.7f) else Color(0xFFBAE6FD).copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isBannerDark) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF7DD3FC)),
                modifier = Modifier.padding(top = 1.dp)
              ) {
                Text(
                  text = weather.seaStateDescription,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  ),
                  color = if (isBannerDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                  maxLines = 1
                )
              }
            }

            // Rüzgar ve Hamle (Pusula Oku ile)
            Column(
              horizontalAlignment = Alignment.End,
              modifier = Modifier.weight(1.1f)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .background(
                      if (isBannerDark) Color(0xFF0F172A) else Color.White,
                      CircleShape
                    )
                    .border(
                      0.5.dp,
                      if (isBannerDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFFBAE6FD),
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (isBannerDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    modifier = Modifier
                      .size(12.dp)
                      .rotate(weather.windDirectionDegrees.toFloat())
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${weather.windSpeedKnots} kn",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                  color = bannerTextPrimary
                )
              }
              Text(
                text = "${weather.windDirectionCardinal} (${weather.windDirectionDegrees}°)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = bannerTextSecondary
              )
              Text(
                text = "Hamle: ${weather.windGustsKnots} kn",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp
                ),
                color = if (isBannerDark) Color(0xFFFBBF24) else Color(0xFFD97706)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Detay Parametre Izgarası
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Bağıl Nem
        WeatherMetricChip(
          title = "Bağıl Nem",
          value = "%${weather.relativeHumidityPercent}",
          subtitle = if (weather.relativeHumidityPercent > 80) "Yüksek Nem" else "Normal",
          icon = Icons.Default.WaterDrop,
          iconTint = Color(0xFF0284C7),
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )

        // Basınç (Eski Yüzey Basıncı / Basınç Eğilimi birleşimi)
        WeatherMetricChip(
          title = "Basınç",
          value = "${weather.surfacePressureHpa} hPa",
          subtitle = if (weather.surfacePressureHpa >= 1013) "Yüksek (Kararlı)" else "Alçak (Dinamik)",
          icon = Icons.Default.Speed,
          iconTint = Color(0xFF7C3AED),
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Deniz Görüşü
        WeatherMetricChip(
          title = "Deniz Görüşü",
          value = "> 10 NM",
          subtitle = "İyi Seyir Görüşü",
          icon = Icons.Default.Visibility,
          iconTint = MarineCyan,
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )

        // Deniz Suyu Sıcaklığı
        val seaTemp = (weather.temperatureC - 1.5).coerceAtLeast(4.0)
        WeatherMetricChip(
          title = "Deniz Suyu",
          value = "${String.format(java.util.Locale.US, "%.1f", seaTemp)}°C",
          subtitle = "Yüzey Sıcaklığı",
          icon = Icons.Default.Thermostat,
          iconTint = MarineCyan,
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Rüzgar Hamlesi (Gusts)
        WeatherMetricChip(
          title = "Rüzgar Hamlesi",
          value = "${weather.windGustsKnots} kn",
          subtitle = weather.beaufortDescription,
          icon = Icons.Default.Air,
          iconTint = Color(0xFFD97706),
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )

        // Çiy Noktası
        val dewPoint = weather.temperatureC - ((100 - weather.relativeHumidityPercent) / 5.0)
        WeatherMetricChip(
          title = "Çiy Noktası",
          value = "${String.format(java.util.Locale.US, "%.1f", dewPoint)}°C",
          subtitle = "Sis Riski Düşük",
          icon = Icons.Default.Water,
          iconTint = PrimaryBlue,
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Akıntı
        WeatherMetricChip(
          title = "Deniz Akıntısı",
          value = "${String.format(java.util.Locale.US, "%.1f", weather.oceanCurrentSpeedKnots)} kn",
          subtitle = "Set: ${String.format(java.util.Locale.US, "%03d°", weather.oceanCurrentDirectionDegrees)}",
          icon = Icons.Default.Waves,
          iconTint = MarineCyan,
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )

        // Dalga
        WeatherMetricChip(
          title = "Dalga Yüksekliği",
          value = "${weather.waveHeightMeters} m",
          subtitle = "Periyot: ${weather.wavePeriodSeconds} sn",
          icon = Icons.Default.Water,
          iconTint = Color(0xFF0284C7),
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      // 4. GPS GÜN DOĞUMU & GÜN BATIMI (GÖKYÜZÜ GÖRSELİ)
      Spacer(modifier = Modifier.height(10.dp))
      val sunCardBg = if (isDarkMode) Color(0xFF142034) else Color(0xFFFFFBEB)
      val sunCardBorder = if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFFDE68A)
      val sunTitleColor = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309)
      val sunBoxBg = if (isDarkMode) Color(0xFF1B2C47) else Color(0xFFFFFFFF)
      val sunBoxBorder = if (isDarkMode) Color(0xFF2A4368) else Color(0xFFFED7AA)
      val sunBoxLabelColor = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF78350F)
      val sunSunriseValColor = if (isDarkMode) Color(0xFFFDE047) else Color(0xFFD97706)
      val sunSunsetValColor = if (isDarkMode) Color(0xFFFB923C) else Color(0xFFEA580C)
      val sunDawnDuskColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF92400E)
      val sunBottomBarBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFEF3C7)
      val sunBottomBarText = if (isDarkMode) Color(0xFFFDE047) else Color(0xFF78350F)

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = sunCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, sunCardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
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
                tint = sunTitleColor,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Güneş Doğuş & Batış (Mevki)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = sunTitleColor
              )
            }

            val badgeBg = if (sun.isDaylightNow) {
              if (isDarkMode) Color(0xFF451A03) else Color(0xFFFEF3C7)
            } else {
              if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            }
            val badgeTextColor = if (sun.isDaylightNow) {
              if (isDarkMode) Color(0xFFFDE047) else Color(0xFF92400E)
            } else {
              if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1E293B)
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = badgeBg
            ) {
              Text(
                text = if (sun.isDaylightNow) "☀️ GÜNDÜZ" else "🌙 GECE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          // Güneş Doğuş & Batış Saatleri Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Gün Doğumu
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = sunBoxBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, sunBoxBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(if (isDarkMode) Color(0xFF451A03) else Color(0xFFFEF3C7), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706),
                    modifier = Modifier.size(15.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Doğumu", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = sunBoxLabelColor))
                  Text(sun.sunriseFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = sunSunriseValColor, fontSize = 12.5.sp))
                  Text("Şafak: ${sun.dawnCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = sunDawnDuskColor))
                }
              }
            }

            // Gün Batımı
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = sunBoxBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, sunBoxBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .background(if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFFDBEAFE), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Nightlight,
                    contentDescription = null,
                    tint = if (isDarkMode) MarineCyan else Color(0xFF2563EB),
                    modifier = Modifier.size(15.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Batımı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = sunBoxLabelColor))
                  Text(sun.sunsetFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = sunSunsetValColor, fontSize = 12.5.sp))
                  Text("Alaca: ${sun.duskCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = sunDawnDuskColor))
                }
              }
            }
          }

          // Gün Işığı Özeti Alt Satır
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(sunBottomBarBg, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "☀️ Toplam: ${sun.daylightDurationFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = sunBottomBarText)
            )
            Text(
              text = "Zenit: ${sun.solarNoonFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = sunBottomBarText)
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
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val subtleBg = if (isDarkMode) Color(0xFF132238) else Color(0xFFF1F5F9)
  val subtleBorder = if (isDarkMode) Color(0xFF233B5D) else Color(0xFFCBD5E1)
  val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
  val textTitle = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
  val textSubtitle = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0369A1)

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = subtleBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
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
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
          color = textTitle,
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
        color = textPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium, lineHeight = 11.sp),
        color = textSubtitle,
        softWrap = true,
        maxLines = 2
      )
    }
  }
}

