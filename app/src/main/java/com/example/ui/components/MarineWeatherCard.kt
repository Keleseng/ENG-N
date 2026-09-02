package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
  val subtleBg = if (isDarkMode) PrimaryBlueLight.copy(alpha = 0.25f) else CardSubtle
  val subtleBorder = if (isDarkMode) MarineCyan.copy(alpha = 0.35f) else CardSubtleBorder

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
                Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
                RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (weather.weatherConditionDescription.contains("Yağmur", ignoreCase = true)) {
                Icons.Default.WaterDrop
              } else if (weather.weatherConditionDescription.contains("Bulut", ignoreCase = true)) {
                Icons.Default.Cloud
              } else {
                Icons.Default.WbSunny
              },
              contentDescription = null,
              tint = Color(0xFFFEF08A),
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
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (weather.isLiveFromNetwork) SeaGreenLight else PrimaryBlueLight
              ) {
                Text(
                  text = if (weather.isLiveFromNetwork) "🛰️ CANLI" else "TAHMİN",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 8.5.sp
                  ),
                  color = if (weather.isLiveFromNetwork) SeaGreenDark else (if (isDarkMode) MarineCyan else PrimaryBlueDark),
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

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.windy.com/?${weather.latitude},${weather.longitude},9")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
              }
              try { context.startActivity(intent) } catch (_: Exception) {}
            },
            modifier = Modifier
              .size(32.dp)
              .testTag("btn_verify_with_windy_quick")
          ) {
            Icon(
              imageVector = Icons.Default.Air,
              contentDescription = "Windy ile Doğrula",
              tint = Color(0xFFE11D48),
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onRefreshWeather,
            modifier = Modifier
              .size(32.dp)
              .testTag("btn_refresh_marine_weather")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Yenile",
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Ana Özet Bannerı (Sıcaklık + Dalga + Rüzgar Görseli)
      val bannerBg = if (isDarkMode) {
        Brush.horizontalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
      } else {
        Brush.horizontalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFE0F2FE)))
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) MarineCyan.copy(alpha = 0.5f) else Color(0xFF7DD3FC)),
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
                  color = if (isDarkMode) Color.White else TextPrimary
                )
                Text(
                  text = "C",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = if (isDarkMode) MarineCyan else TextMuted,
                  modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                )
              }
              Text(
                text = weather.weatherConditionDescription,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
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
                  tint = if (isDarkMode) MarineCyan else Color(0xFF0284C7),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "${weather.waveHeightMeters} m",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                  color = if (isDarkMode) Color.White else Color(0xFF0369A1)
                )
              }
              Text(
                text = "Periyot: ${weather.wavePeriodSeconds}s",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                color = if (isDarkMode) Color(0xFFCBD5E1) else TextMuted
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isDarkMode) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
                modifier = Modifier.padding(top = 1.dp)
              ) {
                Text(
                  text = weather.seaStateDescription,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  ),
                  color = if (isDarkMode) MarineCyan else Color(0xFF075985),
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
                      if (isDarkMode) PrimaryBlue else Color.White,
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (isDarkMode) Color.White else PrimaryBlueDark,
                    modifier = Modifier
                      .size(12.dp)
                      .rotate(weather.windDirectionDegrees.toFloat())
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${weather.windSpeedKnots} kn",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                  color = if (isDarkMode) Color.White else PrimaryBlueDark
                )
              }
              Text(
                text = "${weather.windDirectionCardinal} (${weather.windDirectionDegrees}°)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = if (isDarkMode) MarineCyan else TextPrimary
              )
              Text(
                text = "Hamle: ${weather.windGustsKnots} kn",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp
                ),
                color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
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

        // Hava Basıncı
        WeatherMetricChip(
          title = "Yüzey Basıncı",
          value = "${weather.surfacePressureHpa} hPa",
          subtitle = if (weather.surfacePressureHpa < 1010) "Alçak Basınç" else "Kararlı Basınç",
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

        // Yağış Durumu
        WeatherMetricChip(
          title = "Yağış Durumu",
          value = "${weather.precipitationMm} mm/h",
          subtitle = weather.precipitationStateText,
          icon = Icons.Default.Grain,
          iconTint = if (weather.precipitationMm > 0.0) Color(0xFF2563EB) else textSecondary,
          isDarkMode = isDarkMode,
          modifier = Modifier.weight(1f)
        )
      }

      // 4. GPS GÜN DOĞUMU & GÜN BATIMI (GÖKYÜZÜ GÖRSELİ)
      val sun = weather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(weather.latitude, weather.longitude)
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF1E293B) else WarningAmberLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.4f) else WarningAmberBorder),
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
                tint = if (isDarkMode) Color(0xFFFBBF24) else WarningAmber,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Güneş Doğuş & Batış (Mevki)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = if (isDarkMode) Color(0xFFFBBF24) else WarningAmber
              )
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (sun.isDaylightNow) WarningAmber.copy(alpha = 0.25f) else subtleBg
            ) {
              Text(
                text = if (sun.isDaylightNow) "☀️ GÜNDÜZ" else "🌙 GECE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                color = if (sun.isDaylightNow) (if (isDarkMode) Color(0xFFFBBF24) else WarningAmber) else textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
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
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(WarningAmberLight, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.WbSunny, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Doğumu", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = textSecondary))
                  Text(sun.sunriseFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = if (isDarkMode) Color(0xFFFBBF24) else WarningAmber, fontSize = 12.sp))
                  Text("Şafak: ${sun.dawnCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = textSecondary))
                }
              }
            }

            // Gün Batımı
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(PrimaryBlueLight, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Nightlight, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Gün Batımı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = textSecondary))
                  Text(sun.sunsetFormatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color(0xFFFB923C), fontSize = 12.sp))
                  Text("Alaca: ${sun.duskCivilFormatted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = textSecondary))
                }
              }
            }
          }

          // Gün Işığı Özeti Alt Satır
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (isDarkMode) Color(0xFF0F172A) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "☀️ Toplam: ${sun.daylightDurationFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = if (isDarkMode) Color(0xFFFDE047) else Color(0xFF78350F))
            )
            Text(
              text = "Zenit: ${sun.solarNoonFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = if (isDarkMode) Color(0xFFFDE047) else Color(0xFF92400E))
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
  val subtleBg = if (isDarkMode) PrimaryBlueLight.copy(alpha = 0.25f) else CardSubtle
  val subtleBorder = if (isDarkMode) MarineCyan.copy(alpha = 0.35f) else CardBorder.copy(alpha = 0.6f)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)

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
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
          color = textSecondary,
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
        color = textSecondary,
        softWrap = true,
        maxLines = 2
      )
    }
  }
}

