package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MarineWaypoint
import com.example.model.Coordinate
import com.example.model.LocationPresets
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.BigDipperConstellationIcon
import com.example.ui.theme.*
import java.util.Locale

/**
 * ROTA VE DENİZ DÖNÜŞ NOKTALARI (WAYPOINTS - DN) EKRANI.
 *
 * Alt navigasyon çubuğundaki "Büyük Ayı Takım Yıldızı" simgesine basıldığında açılır.
 * %100 deniz üzerinden oluşturulan rotayı, tüm dönüş noktalarının (DN1, DN2...) koordinatlarını,
 * bacak mesafelerini, tavsiye edilen pusula rotalarını ve her dönüş noktasına ait ayrı ETA bilgisini gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToMap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkMode
  val route = uiState.activeSeaRoute

  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)

  val totalDistNm = route?.totalDistanceNm ?: 0.0
  val speedKnots = route?.speedKnots
    ?: uiState.lastGpsFix?.speedKnots?.takeIf { it > 0 }
    ?: (uiState.speedStr.toDoubleOrNull()?.takeIf { it > 0 } ?: 12.0)
  val etaStr = route?.etaTimeStr ?: "--:--"
  val durationStr = route?.durationStr ?: "--"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. HEADER CARD: BÜYÜK AYI KILAVUZU & %100 DENİZ ROTASI ÖZETİ
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("card_route_header")
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(
                    Brush.radialGradient(
                      colors = listOf(Color(0xFF0284C7).copy(alpha = 0.4f), Color(0xFF0F172A))
                    ),
                    CircleShape
                  )
                  .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                BigDipperConstellationIcon(
                  isSelected = true,
                  iconSize = 26.dp
                )
              }

              Column {
                Text(
                  text = "ROTA REHBERİ",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.4.sp
                  ),
                  color = if (isDark) MarineYellow else Color(0xFFB45309)
                )
                Text(
                  text = "🌊 %100 Deniz Üzerinden Emniyetli Dönüş Noktaları (DN)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = { viewModel.setShowVoiceSettingsDialog(true) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_open_voice_settings_route")
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Ses ve Kadın Tonu Ayarları",
                  tint = if (isDark) MarineCyan else PrimaryBlue,
                  modifier = Modifier.size(20.dp)
                )
              }

              IconButton(
                onClick = { viewModel.toggleVoiceGuidance() },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_toggle_voice_guidance")
              ) {
                Icon(
                  imageVector = if (uiState.isVoiceGuidanceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                  contentDescription = "Sesli Rota Rehberini Aç/Kapat",
                  tint = if (uiState.isVoiceGuidanceEnabled) (if (isDark) MarineCyan else PrimaryBlue) else textSecondary,
                  modifier = Modifier.size(22.dp)
                )
              }

              IconButton(
                onClick = { viewModel.speakActiveRouteRecommendation() },
                modifier = Modifier
                  .size(48.dp)
                  .background(if (isDark) PrimaryBlueDark else PrimaryBlueLight, CircleShape)
                  .testTag("btn_speak_route")
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = "Sesli Rota Tavsiyesini Dinle",
                  tint = if (isDark) MarineCyan else Color.White,
                  modifier = Modifier.size(21.dp)
                )
              }
            }
          }

          HorizontalDivider(color = cardBorder, thickness = 0.8.dp)

          // Kalkış & Varış Özeti
          val originCoordText = route?.let {
            LocationPresets.formatMarineDmsCoordinates(it.originLat, it.originLon)
          } ?: "${uiState.latStr}, ${uiState.lonStr}"
          val destName = route?.destinationName ?: uiState.selectedPort.name

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "KALKIŞ DENİZ MEVKİİ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                color = textSecondary
              )
              Text(
                text = if (uiState.lastGpsFix != null) "📍 Canlı GPS Mevkii" else "🛳️ Mevcut Deniz Konumu",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = textPrimary,
                maxLines = 1
              )
              Text(
                text = originCoordText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = textSecondary,
                maxLines = 1
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = if (isDark) MarineCyan else PrimaryBlue,
              modifier = Modifier.padding(horizontal = 6.dp).size(20.dp)
            )

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text(
                text = "VARIŞ LİMANI / MEVKİİ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                color = textSecondary
              )
              Text(
                text = "🏁 $destName",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = if (isDark) MarineYellow else Color(0xFFB45309),
                maxLines = 1
              )
              if (route != null) {
                Text(
                  text = LocationPresets.formatMarineDmsCoordinates(route.destLat, route.destLon),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = textSecondary,
                  maxLines = 1
                )
              }
            }
          }

          // 4'lü İstatistik Izgarası
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatMiniCard(
              title = "DENİZ MESAFESİ",
              value = "${String.format(Locale.US, "%.1f", totalDistNm)} NM",
              subtitle = if (route?.isSeaRouteWithWaypoints == true) "${route.waypoints.size} Dönüş Noktası" else "Açık Deniz Hattı",
              icon = Icons.Default.Straighten,
              isDark = isDark,
              modifier = Modifier.weight(1f)
            )

            StatMiniCard(
              title = "İLK ROTA",
              value = "${String.format(Locale.US, "%03d", route?.initialBearingDegrees ?: 0)}°",
              subtitle = "${String.format(Locale.US, "%.1f", speedKnots)} kn Sürat",
              icon = Icons.Default.Explore,
              isDark = isDark,
              modifier = Modifier.weight(1f)
            )

            StatMiniCard(
              title = "VARIŞ (ETA)",
              value = etaStr,
              subtitle = route?.fullEtaDateStr ?: "TSİ",
              icon = Icons.Default.Schedule,
              isDark = isDark,
              modifier = Modifier.weight(1f)
            )

            StatMiniCard(
              title = "TOPLAM SÜRE",
              value = durationStr,
              subtitle = "Deniz Seyri",
              icon = Icons.Default.Timer,
              isDark = isDark,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 2. SESLİ ROTA TAVSİYESİ BANDI
    item {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth().testTag("card_voice_guidance_control")
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = null,
              tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "SESLİ ROTA TAVSİYESİ (DÖNÜŞ NOKTASI KILAVUZU)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
              )
              val firstWaypoint = route?.waypoints?.firstOrNull()
              val initialBrg = String.format(Locale.US, "%03d", route?.initialBearingDegrees ?: 0)
              val spokenPreview = if (firstWaypoint != null) {
                "\"Rota oluşturuldu. Dönüş Noktası 1'e gidiş için $initialBrg° rotası tavsiye olunur.\""
              } else if (route != null) {
                "\"Rota oluşturuldu. ${route.destinationName} varış mevkiine denizden gidiş için $initialBrg° rotası tavsiye olunur.\""
              } else {
                "Haritadan veya aşağıdaki limanlardan deniz varış noktası seçiniz."
              }
              Text(
                text = "💬 $spokenPreview",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = textPrimary
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          Button(
            onClick = { viewModel.speakActiveRouteRecommendation() },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("btn_speak_now")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("ANONS ET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3. HIZLI VARIŞ LİMANI SEÇİMİ (GEMİ KONUMUNU BOZMADAN HEDEF ROTA OLUŞTURUR)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "HIZLI VARIŞ LİMANI SEÇİMİ (DENİZ ROTASI HESAPLA)",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp, letterSpacing = 0.5.sp),
          color = textSecondary,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
          val quickPorts = LocationPresets.defaultPorts
          itemsIndexed(quickPorts) { _, port ->
            val isSelected = route?.destinationName == port.name
            FilterChip(
              selected = isSelected,
              onClick = {
                viewModel.calculateAndSetSeaRoute(
                  destLat = port.latitude,
                  destLon = port.longitude,
                  destName = port.name,
                  playVoice = true,
                  rejectLandDestination = false
                )
              },
              label = {
                Text(
                  text = port.name,
                  fontSize = 9.5.sp,
                  fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                )
              },
              leadingIcon = if (isSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (isDark) Color(0xFF0284C7) else PrimaryBlue,
                selectedLabelColor = Color.White,
                containerColor = cardBg,
                labelColor = textPrimary
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = cardBorder,
                selectedBorderColor = Color(0xFF38BDF8),
                enabled = true,
                selected = isSelected
              )
            )
          }
        }
      }
    }

    // 4. DÖNÜŞ NOKTALARI LİSTESİ BAŞLIĞI
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = Icons.Default.AltRoute,
            contentDescription = null,
            tint = if (isDark) MarineCyan else PrimaryBlue,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "DENİZ DÖNÜŞ NOKTALARI VE ETA TABLOSU (${route?.waypoints?.size ?: 0} DN)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp),
            color = textPrimary
          )
        }

        TextButton(
          onClick = { onNavigateToMap() },
          modifier = Modifier.heightIn(min = 48.dp).testTag("btn_open_route_on_map")
        ) {
          Text(
            text = "Haritada Göster 🗺️",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = if (isDark) MarineCyan else PrimaryBlue
          )
        }
      }
    }

    // 5. DÖNÜŞ NOKTALARI (DN1, DN2...) VE VARIŞ HEDEFİ KARTLARI
    if (route == null) {
      item {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = cardBg,
          border = BorderStroke(1.dp, cardBorder),
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(
              text = "Henüz aktif bir deniz rotası oluşturulmadı. Harita üzerinden denizde bir mevki seçebilir veya yukarıdaki limanlardan birine dokunabilirsiniz.",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = textSecondary
            )
          }
        }
      }
    } else {
      itemsIndexed(route.waypoints) { index, waypoint ->
        WaypointItemCard(
          index = index,
          waypoint = waypoint,
          isDark = isDark,
          onFocusMap = {
            viewModel.setMapFocusCoordinate(Coordinate(waypoint.latitude, waypoint.longitude))
            onNavigateToMap()
          },
          onSpeakWaypoint = {
            viewModel.speakWaypointDetail(waypoint)
          }
        )
      }

      // Nihai Varış Limanı / Mevkii Kartı
      item {
        val finalLeg = route.legs.lastOrNull()
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = cardBg,
          border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth().testTag("waypoint_item_final_destination")
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF10B981).copy(alpha = 0.18f),
                  border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                  Text(
                    text = "VARIŞ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp),
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
                Column {
                  Text(
                    text = "🏁 ${route.destinationName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                    color = textPrimary
                  )
                  Text(
                    text = if (route.waypoints.isEmpty()) "Açık Deniz Doğrudan Emniyetli Seyir" else "${route.waypoints.last().id} ➔ Nihai Deniz Varış Bacağı",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = textSecondary
                  )
                }
              }

              IconButton(
                onClick = {
                  viewModel.setMapFocusCoordinate(Coordinate(route.destLat, route.destLon))
                  onNavigateToMap()
                },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PinDrop,
                  contentDescription = "Varış Noktasına Haritada Odaklan",
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            HorizontalDivider(color = cardBorder, thickness = 0.5.dp)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "DENİZ KOORDİNATI",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                  color = textSecondary
                )
                Text(
                  text = LocationPresets.formatMarineDmsCoordinates(route.destLat, route.destLon),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = textPrimary
                )
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "SON BACAK / TOPLAM",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                  color = textSecondary
                )
                Text(
                  text = "${String.format(Locale.US, "%.1f", finalLeg?.distanceNm ?: route.totalDistanceNm)} / ${String.format(Locale.US, "%.1f", route.totalDistanceNm)} NM",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 10.5.sp),
                  color = if (isDark) MarineCyan else PrimaryBlueDark
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "VARIŞ ETA & ROTA",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                  color = textSecondary
                )
                Text(
                  text = "⏱️ ${route.etaTimeStr} (${String.format(Locale.US, "%03d", finalLeg?.bearingDegrees ?: route.initialBearingDegrees)}°)",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                  color = Color(0xFF10B981)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WaypointItemCard(
  index: Int,
  waypoint: MarineWaypoint,
  isDark: Boolean,
  onFocusMap: () -> Unit,
  onSpeakWaypoint: () -> Unit
) {
  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)

  val statusColor = if (index == 0) Color(0xFF38BDF8) else Color(0xFFFBBF24)

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = cardBg,
    border = BorderStroke(1.dp, if (index == 0) statusColor.copy(alpha = 0.6f) else cardBorder),
    modifier = Modifier.fillMaxWidth().testTag("waypoint_item_${waypoint.id}")
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, statusColor)
          ) {
            Text(
              text = waypoint.id,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
              color = statusColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Column {
            Text(
              text = waypoint.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
              color = textPrimary
            )
            if (waypoint.description.isNotBlank()) {
              Text(
                text = "🌊 ${waypoint.description}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = textSecondary
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(
            onClick = onSpeakWaypoint,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "${waypoint.name} Sesli Anons Et",
              tint = if (isDark) MarineCyan else PrimaryBlue,
              modifier = Modifier.size(19.dp)
            )
          }

          IconButton(
            onClick = onFocusMap,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "${waypoint.name} Haritada Odaklan",
              tint = if (isDark) MarineYellow else Color(0xFFD97706),
              modifier = Modifier.size(19.dp)
            )
          }
        }
      }

      // Dönüş Noktası Özel ETA Bandı
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F9FF),
        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF1E3A5F) else Color(0xFFBAE6FD)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "⏱️ ${waypoint.id} VARIŞ (ETA): ${waypoint.etaTimeStr.ifBlank { "--:--" }}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.5.sp),
            color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
          )
          Text(
            text = "Kalan: ${waypoint.durationStr.ifBlank { "--" }} • Top: ${String.format(Locale.US, "%.1f", waypoint.cumulativeDistanceNm)} NM",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
            color = textSecondary
          )
        }
      }

      // Alt Satır: Koordinat, Bacak Mesafesi, Tavsiye Rotalar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "DENİZ MEVKİİ (DMS)",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
            color = textSecondary
          )
          Text(
            text = LocationPresets.formatMarineDmsCoordinates(waypoint.latitude, waypoint.longitude),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = textPrimary
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "BACAK MESAFESİ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
            color = textSecondary
          )
          Text(
            text = "${String.format(Locale.US, "%.1f", waypoint.legDistanceNm)} NM",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
            color = if (isDark) MarineCyan else PrimaryBlueDark
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "GELİŞ ➔ YENİ ROTA",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
            color = textSecondary
          )
          Text(
            text = "🧭 ${String.format(Locale.US, "%03d", waypoint.legBearingDegrees)}° ➔ ${String.format(Locale.US, "%03d", waypoint.nextLegBearingDegrees)}°",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
            color = if (isDark) MarineYellow else Color(0xFFB45309)
          )
        }
      }
    }
  }
}

@Composable
private fun StatMiniCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
    border = BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Black),
        color = getMarineTextMuted(isDark),
        maxLines = 1
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp),
        color = getMarineTextPrimary(isDark),
        maxLines = 1
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
        color = getMarineTextSecondary(isDark),
        maxLines = 1
      )
    }
  }
}
