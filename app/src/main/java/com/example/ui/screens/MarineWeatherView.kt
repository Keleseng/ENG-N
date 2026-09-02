package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LocationPresets
import com.example.model.MarineWeather
import com.example.model.PortLocation
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.MarineWeatherCard
import com.example.ui.components.RealisticMoonPhaseCard
import com.example.ui.components.WindyVerificationCard
import com.example.ui.theme.*
import java.util.Locale

/**
 * Deniz & Hava Durumu Ana Görünümü (Marine Weather Tab)
 * Canlı Deniz Görselleri, Open-Meteo tahminleri, rüzgar hamleleri, dalga periyotları,
 * gerçekçi ay evresi grafiği, barometrik basınç ve Beaufort skalasını içerir.
 */
@Composable
fun MarineWeatherView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val weather = uiState.marineWeather
  val scrollState = rememberScrollState()
  var showBeaufortGuide by rememberSaveable { mutableStateOf(false) }
  val isDarkMode = uiState.isDarkMode

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val subtleBg = if (isDarkMode) PrimaryBlueLight.copy(alpha = 0.25f) else CardSubtle
  val subtleBorder = if (isDarkMode) MarineCyan.copy(alpha = 0.35f) else CardSubtleBorder

  val latNum = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
  val lonNum = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
  val marineCoordStr = LocationPresets.formatMarineCoordinates(latNum, lonNum)

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("screen_marine_weather"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // ══════════════════════════════════════════════════════════════════════
    // 1. GÖRSEL DENİZ HERO KARTI (FOTOĞRAF + CANLI HAVA DURUMU KAPLAMASI)
    // ══════════════════════════════════════════════════════════════════════
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("card_weather_hero_banner")
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(135.dp)
      ) {
        // Arka Plan Deniz Görseli
        Image(
          painter = painterResource(id = R.drawable.img_marine_hero),
          contentDescription = "Deniz Görseli",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Gradient Kaplama (Metinlerin ve İkonların Net Görünmesi İçin)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Black.copy(alpha = 0.35f),
                  Color(0xFF0F172A).copy(alpha = 0.85f)
                )
              )
            )
        )

        // Görsel Üzerindeki Bilgi ve Kontroller
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Üst Satır: Konum ve Canlı Rozet
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color.Black.copy(alpha = 0.60f),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MarineCyan,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = uiState.selectedPort.name,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                  color = Color.White,
                  maxLines = 1
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (weather.isLiveFromNetwork) SeaGreen else PrimaryBlue
              ) {
                Text(
                  text = if (weather.isLiveFromNetwork) "🛰️ CANLI AĞ" else "TAHMİN",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }

              IconButton(
                onClick = { viewModel.refreshWeather() },
                modifier = Modifier
                  .size(28.dp)
                  .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                  .testTag("btn_weather_refresh")
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Yenile",
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }

          // Alt Satır: Görsel Sıcaklık, Dalga ve Rüzgar Özeti
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Column {
              Text(
                text = "${weather.temperatureC}°C",
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 22.sp
                ),
                color = Color.White
              )
              Text(
                text = weather.weatherConditionDescription,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                color = MarineCyan,
                maxLines = 1
              )
            }

            // Dalga & Rüzgar İkonları
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.60f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Waves, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "${weather.waveHeightMeters}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White)
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.60f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "${weather.windSpeedKnots}kn",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White)
                  )
                }
              }
            }
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. ANA DENİZ & HAVA DURUMU KARTI (METRİKLER + GÖKYÜZÜ GÖRSELİ)
    // ══════════════════════════════════════════════════════════════════════
    MarineWeatherCard(
      weather = weather,
      isGpsActive = uiState.isGpsActive,
      onRefreshWeather = { viewModel.refreshWeather() },
      isDarkMode = isDarkMode,
      modifier = Modifier.fillMaxWidth()
    )

    // ══════════════════════════════════════════════════════════════════════
    // 3. WINDY CANLI DENİZ & HAVA DURUMU DOĞRULAMA KARTI
    // ══════════════════════════════════════════════════════════════════════
    WindyVerificationCard(
      weather = weather,
      onRefreshWeather = { viewModel.refreshWeather() },
      isDarkMode = isDarkMode,
      modifier = Modifier.fillMaxWidth()
    )

    // ══════════════════════════════════════════════════════════════════════
    // 4. GERÇEKÇİ 3D AY EVRESİ & ASTRONOMİK GELGİT ÇEKİM GÖRSELİ
    // ══════════════════════════════════════════════════════════════════════
    RealisticMoonPhaseCard(
      analysis = uiState.analysis,
      isDarkMode = isDarkMode,
      modifier = Modifier.fillMaxWidth()
    )

    // ══════════════════════════════════════════════════════════════════════
    // 5. BEAUFORT RÜZGAR VE DENİZ SKALASI REHBERİ (AÇILIR AKORDEON)
    // ══════════════════════════════════════════════════════════════════════
    val chevronRotation by animateFloatAsState(
      targetValue = if (showBeaufortGuide) 180f else 0f,
      label = "beaufortRotation"
    )

    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_beaufort_guide")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showBeaufortGuide = !showBeaufortGuide }
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Beaufort Rüzgar & Deniz Rehberi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text(
                text = "Mevcut: ${weather.beaufortDescription}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = if (isDarkMode) MarineCyan else PrimaryBlueDark)
              )
            }
          }

          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
            modifier = Modifier
              .size(20.dp)
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
              .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            HorizontalDivider(color = cardBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(3.dp))

            listOf(
              Triple("Bft 0 (0-1 kn)", "Sakin (Calm)", "Deniz ayna gibi düzgündür. Dalga 0m."),
              Triple("Bft 1-2 (1-6 kn)", "Esinti (Light)", "Küçük dalgacıklar, köpüksüz. Dalga 0.1-0.3m."),
              Triple("Bft 3 (7-10 kn)", "Tatlı Rüzgar (Gentle)", "Dalgacıkların tepeleri kırılmaya başlar. Dalga 0.6m."),
              Triple("Bft 4 (11-16 kn)", "Mutedil Rüzgar", "Küçük dalgalar uzar, beyaz köpükler. Dalga 1.0m."),
              Triple("Bft 5 (17-21 kn)", "Sertçe Rüzgar", "Orta büyüklükte dalgalar, serpinti riski. Dalga 2.0m."),
              Triple("Bft 6 (22-27 kn)", "Kuvvetli Rüzgar", "Büyük dalgalar oluşur, köpükler genişler. Dalga 3.0m."),
              Triple("Bft 7 (28-33 kn)", "Fırtınamsı Rüzgar", "Deniz kabarır, rüzgar yönünde köpük. Dalga 4.0m."),
              Triple("Bft 8 (34-40 kn)", "Fırtına (Gale)", "Yüksek dalgalar, savrulan serpintiler. Dalga 5.5m."),
              Triple("Bft 9+ (41+ kn)", "Kuvvetli Fırtına", "Çok yüksek kaba dalgalar, görüş azalır. Dalga 7m+.")
            ).forEach { (bft, name, desc) ->
              val isCurrent = weather.beaufortDescription.contains(bft.split(" ")[1], ignoreCase = true)
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) (if (isDarkMode) Color(0xFF78350F) else WarningAmberLight) else subtleBg,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isCurrent) (if (isDarkMode) Color(0xFFFBBF24) else WarningAmberBorder) else subtleBorder
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(7.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = bft,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isCurrent) (if (isDarkMode) Color(0xFFFBBF24) else WarningAmber) else (if (isDarkMode) MarineCyan else PrimaryBlue)
                      )
                    )
                    Text(
                      text = name,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        color = textPrimary
                      )
                    )
                  }
                  Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = textSecondary)
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

