package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AisTrackingEngine
import com.example.model.AisVesselData
import com.example.model.LocationPresets
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AisRadarFullScreenDialog
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AisCoordinatesView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToMap: () -> Unit = {},
  onNavigateToAnchor: () -> Unit = {},
  onNavigateToAnamenu: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  val isDark = uiState.isDarkMode
  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)
  val inputBg = getMarineInputBg(isDark)
  val inputBorder = getMarineInputBorder(isDark)
  val subtleBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

  val inputTextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = if (isDark) MarineYellow else Color(0xFF78350F),
    unfocusedTextColor = getMarineInputTextColor(isDark),
    focusedLabelColor = if (isDark) MarineYellowBold else Color(0xFF92400E),
    unfocusedLabelColor = getMarineInputLabel(isDark),
    focusedContainerColor = inputBg,
    unfocusedContainerColor = inputBg,
    focusedBorderColor = if (isDark) MarineYellow else Color(0xFFD97706),
    unfocusedBorderColor = inputBorder,
    cursorColor = if (isDark) MarineYellowBold else Color(0xFFD97706)
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {

    // AIS Bildirim / Uyarı Mesajları
    if (uiState.aisSuccessMessage != null) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF065F46),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.aisSuccessMessage,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color(0xFFECFDF5))
            )
          }
          IconButton(onClick = { viewModel.dismissAisMessages() }, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(12.dp))
          }
        }
      }
    }

    if (uiState.aisErrorMessage != null) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = DangerRedDark,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.aisErrorMessage,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color(0xFFFEF2F2))
            )
          }
          IconButton(onClick = { viewModel.dismissAisMessages() }, modifier = Modifier.size(18.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(12.dp))
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 0. CANLI AIS RADARI (TAM EKRAN PPI RADAR BUTONU)
    // ══════════════════════════════════════════════════════════════════════
    Button(
      onClick = { viewModel.openAisRadar() },
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) PrimaryBlueDark else PrimaryBlue,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_open_ais_radar")
    ) {
      Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        "AIS RADARI",
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
      )
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. MMSI / IMO SORGULAMA PENCERESİ (EN ÜSTTE)
    // ══════════════════════════════════════════════════════════════════════
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_ais_search")
    ) {
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = if (isDark) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Search MMSI \\ IMO",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
              color = textPrimary
            )
          }

          Surface(
            shape = RoundedCornerShape(5.dp),
            color = if (uiState.isMmsiTrackingActive) SeaGreen.copy(alpha = 0.2f) else MarineCyan.copy(alpha = 0.15f)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(if (uiState.isMmsiTrackingActive) SeaGreen else MarineCyan, CircleShape)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (uiState.isMmsiTrackingActive) "CANLI AIS" else "RADAR HAZIR",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  color = if (uiState.isMmsiTrackingActive) SeaGreen else (if (isDark) MarineCyan else PrimaryBlueDark)
                )
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = uiState.mmsiStr,
            onValueChange = { viewModel.updateMmsi(it) },
            label = { Text("Search MMSI \\ IMO", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            placeholder = { Text("Search MMSI \\ IMO", fontSize = 11.sp, color = textSecondary) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp),
            colors = inputTextFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_mmsi_ais")
          )

          Button(
            onClick = { viewModel.startMmsiTracking() },
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) MarineCyan else PrimaryBlue),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(44.dp).testTag("btn_track_mmsi")
          ) {
            if (uiState.isAisLoading) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
            } else {
              Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Sorgula", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color.White)
            }
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. GPS & AIS KOORDİNATLARI YÖNETİM KARTI
    // ══════════════════════════════════════════════════════════════════════
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_ais_coordinates_management")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        // Başlık Satırı
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(if (isDark) MarineCyan.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = if (isDark) MarineCyan else PrimaryBlueDark,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "GPS & AIS Koordinatları",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text(
                text = if (uiState.isCustomPort) "Özel / Canlı Seyir Mevkii" else uiState.selectedPort.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textSecondary),
                maxLines = 1
              )
            }
          }

          // Durum Rozeti: Sadece "GPS" / "AIS"
          val isAisSource = uiState.activeLocationSource == com.example.ui.LocationSource.AIS
          val badgeText = if (isAisSource) "AIS" else "GPS"
          val badgeColor = if (isAisSource) Color(0xFF38BDF8) else SeaGreen
          val badgeBgColor = badgeColor.copy(alpha = 0.16f)
          val badgeIcon = if (isAisSource) Icons.Default.DirectionsBoat else Icons.Default.GpsFixed

          Surface(
            shape = RoundedCornerShape(5.dp),
            color = badgeBgColor,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.5f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(3.5.dp))
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                color = badgeColor
              )
            }
          }
        }

        HorizontalDivider(color = cardBorder, thickness = 0.6.dp)

        // Koordinat ve Derinlik Giriş Kutuları (Enlem, Boylam, Derinlik Yan Yana)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Enlem (Latitude)
          OutlinedTextField(
            value = uiState.latStr,
            onValueChange = { viewModel.updateLat(it) },
            label = { Text("Enlem", fontWeight = FontWeight.Bold, fontSize = 9.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 10.5.sp),
            colors = inputTextFieldColors,
            singleLine = true,
            modifier = Modifier.weight(1.15f).testTag("input_ais_latitude")
          )

          // Boylam (Longitude)
          OutlinedTextField(
            value = uiState.lonStr,
            onValueChange = { viewModel.updateLon(it) },
            label = { Text("Boylam", fontWeight = FontWeight.Bold, fontSize = 9.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 10.5.sp),
            colors = inputTextFieldColors,
            singleLine = true,
            modifier = Modifier.weight(1.15f).testTag("input_ais_longitude")
          )

          // Harita Derinliği (Charted Depth)
          OutlinedTextField(
            value = uiState.chartedDepthStr,
            onValueChange = { viewModel.updateChartedDepth(it) },
            label = { Text("Derinlik (m)", fontWeight = FontWeight.Bold, fontSize = 9.sp) },
            trailingIcon = {
              if (uiState.isDepthLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(13.dp),
                  color = MarineYellow,
                  strokeWidth = 1.8.dp
                )
              } else {
                IconButton(
                  onClick = { viewModel.fetchVerifiedDepth() },
                  modifier = Modifier.size(20.dp)
                ) {
                  Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "Derinlik Sorgula",
                    tint = if (isDark) MarineCyan else PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 10.5.sp),
            colors = inputTextFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(0.95f).testTag("input_ais_charted_depth")
          )
        }

        // Hızlı Eşitleme & Eylem Butonları (GPS, AIS, Derinlik)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // GPS VERİSİ Butonu
          val isGpsActiveSource = uiState.activeLocationSource == com.example.ui.LocationSource.GPS
          Button(
            onClick = { viewModel.syncGpsTelemetryToInputs(asDms = true) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isGpsActiveSource) (if (isDark) MarineCyan else PrimaryBlue) else Color.Transparent,
              contentColor = if (isGpsActiveSource) (if (isDark) cardBg else Color.White) else textPrimary
            ),
            border = if (isGpsActiveSource) null else androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("btn_sync_gps_to_ais_coords")
          ) {
            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // AIS VERİSİ Butonu
          val isAisActiveSource = uiState.activeLocationSource == com.example.ui.LocationSource.AIS
          Button(
            onClick = { viewModel.syncAisShipPosition() },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isAisActiveSource) (if (isDark) MarineCyan else PrimaryBlue) else Color.Transparent,
              contentColor = if (isAisActiveSource) (if (isDark) cardBg else Color.White) else textPrimary
            ),
            border = if (isAisActiveSource) null else androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("btn_use_ais_ship_pos")
          ) {
            Icon(Icons.Default.DirectionsBoat, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AIS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // DERİNLİK Butonu
          Button(
            onClick = { viewModel.fetchVerifiedDepth() },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.Transparent,
              contentColor = textPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).height(34.dp).testTag("btn_fetch_verified_depth")
          ) {
            if (uiState.isDepthLoading) {
              CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MarineYellow, strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isDark) MarineCyan else PrimaryBlue)
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text("DERİNLİK BUL", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. SEÇİLİ GEMİ CANLI AIS TELEMETRİ KARTI
    // ══════════════════════════════════════════════════════════════════════
    val aisVessel = uiState.activeAisVesselData ?: AisTrackingEngine.getNb252ShipData()
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_ais_vessel_tracking")
    ) {
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        // Gemi Üst Bilgisi
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "🚢 ${aisVessel.name.ifBlank { uiState.vesselName.ifBlank { "Gemi Bilgisi" } }}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 13.5.sp),
              color = textPrimary
            )
            Text(
              text = "${aisVessel.shipType} • Bayrak: ${aisVessel.flag}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, color = textSecondary)
            )
          }

          Surface(
            shape = RoundedCornerShape(5.dp),
            color = SeaGreen.copy(alpha = 0.2f)
          ) {
            Text(
              text = "MMSI: ${aisVessel.mmsi}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
              color = SeaGreen,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        HorizontalDivider(color = cardBorder, thickness = 0.6.dp)

        // 1. Satır: Tanımlayıcılar (IMO, Çağrı İşareti, İnşa Yılı)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("IMO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text(aisVessel.imo.ifBlank { "7654320" }, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = textPrimary)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Çağrı (CallSign)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text(aisVessel.callSign.ifBlank { "TST7" }, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = textPrimary)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("İnşa / Tonaj", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${aisVessel.yearBuilt} • ${aisVessel.grossTonnage} GT", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = textPrimary)
            }
          }
        }

        // 2. Satır: Boyutlar & Draft
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Boy (LOA)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${aisVessel.loaMeters} m", fontWeight = FontWeight.Black, fontSize = 11.sp, color = textPrimary)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Genişlik (Beam)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${aisVessel.beamMeters} m", fontWeight = FontWeight.Black, fontSize = 11.sp, color = textPrimary)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Draft (Su Çekimi)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${aisVessel.draftMeters} m", fontWeight = FontWeight.Black, fontSize = 11.sp, color = DangerRed)
            }
          }
        }

        // 3. Satır: Dinamik AIS Telemetrisi (SOG, COG, Heading, Seyir Durumu)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Sürat (SOG)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${String.format(Locale.US, "%.1f", aisVessel.sogKnots)} kn", fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = if (isDark) MarineCyan else PrimaryBlueDark)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Rota (COG)/Pruva", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text("${String.format(Locale.US, "%03d°", aisVessel.cogDegrees.toInt())} / ${String.format(Locale.US, "%03d°", aisVessel.headingDegrees)}", fontWeight = FontWeight.Black, fontSize = 10.5.sp, color = textPrimary)
            }
          }
          Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(5.dp)) {
              Text("Seyir Durumu", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text(aisVessel.status.take(16), fontWeight = FontWeight.Bold, fontSize = 9.sp, color = SeaGreen, maxLines = 1)
            }
          }
        }

        // 4. Satır: Mevki (Enlem & Boylam DMS)
        Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("AIS Anlık Mevki (Deniz GPS DMS)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text(
                "${LocationPresets.formatMarineLatDMS(aisVessel.latitude)} • ${LocationPresets.formatMarineLonDMS(aisVessel.longitude)}",
                fontWeight = FontWeight.Black,
                fontSize = 10.5.sp,
                color = if (isDark) MarineCyan else PrimaryBlueDark
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Hedef / ETA", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = textSecondary)
              Text(
                "${aisVessel.destination.take(14)}",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = textPrimary,
                maxLines = 1
              )
            }
          }
        }

        // 5. Satır: AIS Mevkii Deniz Derinliği (GPS Koordinatları Batimetrisi)
        Surface(shape = RoundedCornerShape(5.dp), color = subtleBg, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Waves,
                contentDescription = null,
                tint = if (isDark) MarineCyan else PrimaryBlue,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  "AIS Mevkii Deniz Derinliği (GPS Batimetri)",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = textSecondary
                )
                val depthDisplay = uiState.verifiedMarineDepth?.let {
                  "${String.format(Locale.US, "%.1f", it.depthMeters)} m • ${it.confidenceText}"
                } ?: "${uiState.chartedDepthStr} m"
                Text(
                  depthDisplay,
                  fontWeight = FontWeight.Black,
                  fontSize = 10.5.sp,
                  color = if (isDark) MarineCyan else PrimaryBlueDark
                )
              }
            }
            if (uiState.isDepthLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                color = MarineYellow,
                strokeWidth = 1.8.dp
              )
            } else {
              IconButton(
                onClick = { viewModel.fetchVerifiedDepth(aisVessel.latitude, aisVessel.longitude) },
                modifier = Modifier.size(22.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Derinliği Yenile",
                  tint = if (isDark) MarineCyan else PrimaryBlue,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. AIS & DENİZCİLİK HABERLEŞME REHBERİ (KALDIRILDI)
    // ══════════════════════════════════════════════════════════════════════
    Spacer(modifier = Modifier.height(10.dp))
  }

  // ══════════════════════════════════════════════════════════════════════
  // TAM EKRAN CANLI AIS RADARI DİYALOĞU
  // ══════════════════════════════════════════════════════════════════════
  if (uiState.isAisRadarOpen) {
    AisRadarFullScreenDialog(
      uiState = uiState,
      viewModel = viewModel,
      onDismiss = { viewModel.closeAisRadar() },
      onNavigateToMap = onNavigateToMap
    )
  }
}
