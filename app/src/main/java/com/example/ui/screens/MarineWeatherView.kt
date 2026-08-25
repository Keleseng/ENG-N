package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationPresets
import com.example.model.MarineWeather
import com.example.model.PortLocation
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.MarineWeatherCard
import com.example.ui.theme.*
import java.util.Locale

/**
 * Deniz & Hava Durumu Ana Görünümü (Marine Weather Tab)
 * Canlı Open-Meteo deniz tahminleri, rüzgar hamleleri, dalga periyotları,
 * barometrik basınç, gün doğumu/batımı ve Beaufort referans skalasını içerir.
 */
@Composable
fun MarineWeatherView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier,
  onNavigateToMap: (() -> Unit)? = null
) {
  val weather = uiState.marineWeather
  val scrollState = rememberScrollState()
  var showBeaufortGuide by rememberSaveable { mutableStateOf(false) }
  var isPortDropdownExpanded by remember { mutableStateOf(false) }

  val latNum = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
  val lonNum = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
  val sunInfo = weather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(latNum, lonNum)
  val marineCoordStr = LocationPresets.formatMarineCoordinates(latNum, lonNum)

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("screen_marine_weather"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // ══════════════════════════════════════════════════════════════════════
    // 1. ÜST BAŞLIK VE HIZLI YENİLEME / KONUM KARTI
    // ══════════════════════════════════════════════════════════════════════
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_weather_header")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
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
                .size(42.dp)
                .background(
                  Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
                  RoundedCornerShape(12.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color(0xFFFEF08A),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Deniz & Hava Durumu",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 16.sp
                ),
                color = TextPrimary
              )
              Text(
                text = "Mevki: ${uiState.selectedPort.name}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                  color = PrimaryBlueDark
                ),
                maxLines = 1
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (weather.isLiveFromNetwork) SeaGreenLight else PrimaryBlueLight
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(
                      if (weather.isLiveFromNetwork) SeaGreen else PrimaryBlueDark,
                      CircleShape
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (weather.isLiveFromNetwork) "CANLI AĞ" else "TAHMİN",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    color = if (weather.isLiveFromNetwork) SeaGreenDark else PrimaryBlueDark
                  )
                )
              }
            }

            IconButton(
              onClick = { viewModel.refreshWeather() },
              modifier = Modifier
                .size(36.dp)
                .background(CardSubtle, RoundedCornerShape(8.dp))
                .testTag("btn_weather_refresh")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Yenile",
                tint = PrimaryBlueDark,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Koordinat ve Son Güncelleme Şeridi
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = BackgroundCanvas,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "📍 $marineCoordStr",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = TextPrimary
            )
            Text(
              text = "Son Güncelleme: ${weather.lastUpdatedFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = TextMuted
            )
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. ANA DENİZ & HAVA DURUMU KARTI (TAM METRİKLER)
    // ══════════════════════════════════════════════════════════════════════
    MarineWeatherCard(
      weather = weather,
      isGpsActive = uiState.isGpsActive,
      onRefreshWeather = { viewModel.refreshWeather() },
      modifier = Modifier.fillMaxWidth()
    )

    // ══════════════════════════════════════════════════════════════════════
    // 3. DENİZ GÖRÜŞÜ & EKSTRA METEOROLOJİ PARAMETRELERİ
    // ══════════════════════════════════════════════════════════════════════
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_weather_extra_metrics")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = PrimaryBlueDark,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Denizcilik Meteoroloji Detayları",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Deniz Görüş Mesafesi
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSubtle,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deniz Görüşü", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "> 10 NM (Açık)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = TextPrimary
              )
              Text("İyi Seyir Görüşü", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = SeaGreen))
            }
          }

          // Deniz Suyu Sıcaklığı
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSubtle,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Thermostat, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deniz Suyu Sıcaklığı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
              }
              Spacer(modifier = Modifier.height(4.dp))
              val seaTemp = (weather.temperatureC - 1.5).coerceAtLeast(4.0)
              Text(
                text = "${String.format(Locale.US, "%.1f", seaTemp)}°C",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = TextPrimary
              )
              Text("Yüzey Sıcaklığı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MarineCyan))
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Çiy Noktası
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSubtle,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Water, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Çiy Noktası", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
              }
              Spacer(modifier = Modifier.height(4.dp))
              val dewPoint = weather.temperatureC - ((100 - weather.relativeHumidityPercent) / 5.0)
              Text(
                text = "${String.format(Locale.US, "%.1f", dewPoint)}°C",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = TextPrimary
              )
              Text("Sis Riski Düşük", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TextMuted))
            }
          }

          // Hava Basınç Kararlılığı
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSubtle,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Basınç Eğilimi", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${weather.surfacePressureHpa} hPa",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = TextPrimary
              )
              Text(
                text = if (weather.surfacePressureHpa >= 1013) "Yüksek Basınç (Kararlı)" else "Alçak Basınç (Dinamik)",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  color = if (weather.surfacePressureHpa >= 1013) SeaGreen else WarningAmber
                )
              )
            }
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. BEAUFORT RÜZGAR VE DENİZ SKALASI REHBERİ (AÇILIR AKORDEON)
    // ══════════════════════════════════════════════════════════════════════
    val chevronRotation by animateFloatAsState(
      targetValue = if (showBeaufortGuide) 180f else 0f,
      label = "beaufortRotation"
    )

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_beaufort_guide")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showBeaufortGuide = !showBeaufortGuide }
            .padding(14.dp),
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
                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Beaufort Rüzgar & Deniz Skalası Rehberi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
              )
              Text(
                text = "Mevcut Durum: ${weather.beaufortDescription}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = PrimaryBlueDark)
              )
            }
          }

          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = PrimaryBlueDark,
            modifier = Modifier
              .size(22.dp)
              .rotate(chevronRotation)
          )
        }

        AnimatedVisibility(
          visible = showBeaufortGuide,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))

            listOf(
              Triple("Bft 0 (0-1 kn)", "Sakin (Calm)", "Deniz ayna gibi düzgündür. Dalga 0m."),
              Triple("Bft 1-2 (1-6 kn)", "Esinti (Light Air / Breeze)", "Küçük dalgacıklar, köpüksüz. Dalga 0.1-0.3m."),
              Triple("Bft 3 (7-10 kn)", "Tatlı Rüzgar (Gentle Breeze)", "Dalgacıkların tepeleri kırılmaya başlar. Dalga 0.6m."),
              Triple("Bft 4 (11-16 kn)", "Mutedil Rüzgar (Moderate)", "Küçük dalgalar uzar, beyaz köpükler sıklaşır. Dalga 1.0m."),
              Triple("Bft 5 (17-21 kn)", "Sertçe Rüzgar (Fresh Breeze)", "Orta büyüklükte dalgalar, serpinti riski. Dalga 2.0m."),
              Triple("Bft 6 (22-27 kn)", "Kuvvetli Rüzgar (Strong Breeze)", "Büyük dalgalar oluşur, beyaz köpükler genişler. Dalga 3.0m."),
              Triple("Bft 7 (28-33 kn)", "Fırtınamsı Rüzgar (Near Gale)", "Deniz kabarır, rüzgar yönünde köpük çizgileri. Dalga 4.0m."),
              Triple("Bft 8 (34-40 kn)", "Fırtına (Gale)", "Oldukça yüksek dalgalar, savrulan serpintiler. Dalga 5.5m."),
              Triple("Bft 9+ (41+ kn)", "Kuvvetli Fırtına & Kasırga", "Çok yüksek kaba dalgalar, görüş azalır. Dalga 7m+.")
            ).forEach { (bft, name, desc) ->
              val isCurrent = weather.beaufortDescription.contains(bft.split(" ")[1], ignoreCase = true)
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) WarningAmberLight else CardSubtle,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isCurrent) WarningAmberBorder else CardBorder.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = bft,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) WarningAmber else PrimaryBlue
                      )
                    )
                    Text(
                      text = name,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TextPrimary
                      )
                    )
                  }
                  Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = TextMuted)
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
