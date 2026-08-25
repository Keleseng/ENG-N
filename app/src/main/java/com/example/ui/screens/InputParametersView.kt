package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale
import com.example.location.GpsLocationProvider
import com.example.model.VesselPresets
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AisVesselDetailDialog
import com.example.ui.components.AnchorCalculationCard
import com.example.ui.components.SpeedVectorAnalysisCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputParametersView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToTide: () -> Unit,
  onNavigateToMap: () -> Unit,
  onNavigateToWeather: () -> Unit = {},
  onNavigateToAnchor: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider(context) }
  var isVesselParamsExpanded by rememberSaveable { mutableStateOf(false) }
  var isPortDropdownExpanded by remember { mutableStateOf(false) }
  var showPortDetails by rememberSaveable { mutableStateOf(false) }

  fun fetchGps() {
    viewModel.setGpsLoading(true)
    coroutineScope.launch {
      val result = gpsProvider.getCurrentGpsFix()
      result.fold(
        onSuccess = { fix ->
          viewModel.applyGpsFix(fix)
          viewModel.startContinuousLocationUpdates(gpsProvider)
        },
        onFailure = { error ->
          viewModel.setGpsError(error.localizedMessage ?: "GPS konumu alınamadı.")
        }
      )
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      fetchGps()
    } else {
      viewModel.setGpsError("Canlı konum ve sürat takibi için konum izni gereklidir.")
    }
  }

  fun requestLocationAndFetch() {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (fine || coarse) {
      fetchGps()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {

    // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_location_inputs")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Başlık ve Canlı Takip Butonu
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = PrimaryBlueDark,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Seyir Koordinatları & Harita Derinliği",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
              color = TextPrimary,
              softWrap = true
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Sürekli Canlı Takip / Yenileme Durumu Butonu
          Button(
            onClick = {
              if (uiState.isContinuousTrackingActive) {
                viewModel.stopContinuousLocationUpdates()
              } else {
                requestLocationAndFetch()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (uiState.isContinuousTrackingActive) SeaGreenDark else PrimaryBlue
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_continuous_gps_toggle")
          ) {
            if (uiState.isGpsLoading) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(13.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Aranıyor...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
            } else if (uiState.isContinuousTrackingActive) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Canlı Takip", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            } else {
              Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Konum Yenile", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
          }
        }

        // GPS Bildirim Mesajları
        if (uiState.gpsSuccessMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SeaGreenLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, SeaGreenBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SeaGreenDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = uiState.gpsSuccessMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SeaGreenDark,
                modifier = Modifier.weight(1f),
                softWrap = true
              )
              IconButton(
                onClick = { viewModel.dismissGpsMessages() },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = SeaGreenDark, modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        // TÜM GPS BİLGİLERİNİ DENİZ PARAMETRELERİ OLARAK GETİR BUTONU
        Button(
          onClick = {
            if (uiState.lastGpsFix != null) {
              viewModel.syncAllGpsToMarineParameters()
            } else {
              requestLocationAndFetch()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0284C7),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_sync_all_gps_marine_parameters")
        ) {
          Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "🛰️ Tüm GPS Bilgilerini Deniz Parametreleri Olarak Getir",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
            softWrap = true,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }

        // GPS CANLI DENİZ TELEMETRİ KARTI (GPS Fix Varsa) - 2x2 Dikey Uyumlu Izgara
        val activeGps = uiState.lastGpsFix
        if (activeGps != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().testTag("card_gps_marine_telemetry")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(modifier = Modifier.size(7.dp).background(Color(0xFF10B981), CircleShape))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "CANLI GPS DENİZ TELEMETRİSİ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.5.sp, letterSpacing = 0.5.sp),
                    color = Color(0xFF38BDF8)
                  )
                }

                Text(
                  text = "±${String.format(Locale.US, "%.1f", activeGps.accuracyMeters)}m",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = Color(0xFF94A3B8)
                )
              }

              // 2x2 Izgara Düzeni (Dikey pozisyonda kelimelerin sıkışmasını önler)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF1E293B),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("SOG (Hız)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF94A3B8))
                    Text("${String.format(Locale.US, "%.1f", activeGps.speedKnots ?: 0.0)} kn", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF38BDF8))
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF1E293B),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("COG (Rota)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF94A3B8))
                    Text("${String.format(Locale.US, "%03d", activeGps.bearingDegrees?.toInt() ?: 0)}°", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF38BDF8))
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF1E293B),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("İrtifa", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF94A3B8))
                    Text("${String.format(Locale.US, "%.1f", activeGps.altitudeMeters ?: 0.0)} m", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFFE2E8F0))
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF1E293B),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("Dinamik UKC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF94A3B8))
                    Text("${String.format(Locale.US, "%.2f", uiState.analysis.currentInstantUkcMeters)} m", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = if (uiState.analysis.isCurrentlySafe) Color(0xFF34D399) else Color(0xFFF87171))
                  }
                }
              }

              if (uiState.gpsNearestPortInfo != null) {
                Text(
                  text = "📍 En Yakın Liman/Boğaz: ${uiState.gpsNearestPortInfo}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = Color(0xFFCBD5E1),
                  softWrap = true
                )
              }
            }
          }
        }

        if (uiState.gpsErrorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DangerRedLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRedBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRedDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = uiState.gpsErrorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = DangerRedDark,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { viewModel.dismissGpsMessages() },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = DangerRedDark, modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        // 2. Aşağı Açılır Liman / Boğaz / Seyir Mevkii Seçimi (Dropdown)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Liman / Boğaz / Seyir Mevkii Seçimi:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
          )

          ExposedDropdownMenuBox(
            expanded = isPortDropdownExpanded,
            onExpandedChange = { isPortDropdownExpanded = !isPortDropdownExpanded },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dropdown_box_port_select")
          ) {
            OutlinedTextField(
              value = "${uiState.selectedPort.name} (${uiState.selectedPort.category})",
              onValueChange = {},
              readOnly = true,
              label = { Text("Seçili Askeri Liman") },
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPortDropdownExpanded)
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = PrimaryBlueDark,
                  modifier = Modifier.size(20.dp)
                )
              },
              colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("input_port_dropdown_anchor")
            )

            ExposedDropdownMenu(
              expanded = isPortDropdownExpanded,
              onDismissRequest = { isPortDropdownExpanded = false },
              modifier = Modifier
                .background(CardWhite)
                .testTag("dropdown_menu_port_list")
            ) {
              com.example.model.LocationPresets.strategicMarineLocations.forEach { port ->
                val isSelected = uiState.selectedPort.id == port.id
                DropdownMenuItem(
                  text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = port.name,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) PrimaryBlueDark else TextPrimary
                          )
                        )
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (isSelected) PrimaryBlueLight else CardSubtle
                        ) {
                          Text(
                            text = port.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = if (isSelected) PrimaryBlueBorder else TextMuted
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = "${com.example.model.LocationPresets.formatMarineCoordinates(port.latitude, port.longitude)} • Derinlik: ${port.defaultChartedDepthMeters}m",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = TextMuted)
                      )
                    }
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (port.category.contains("Boğaz")) Icons.Default.DirectionsBoat else Icons.Default.Anchor,
                      contentDescription = null,
                      tint = if (isSelected) PrimaryBlueBorder else TextMuted,
                      modifier = Modifier.size(18.dp)
                    )
                  },
                  trailingIcon = {
                    if (isSelected) {
                      Icon(Icons.Default.Check, contentDescription = "Seçili", tint = PrimaryBlueBorder, modifier = Modifier.size(18.dp))
                    }
                  },
                  onClick = {
                    viewModel.selectPortPreset(port)
                    isPortDropdownExpanded = false
                  },
                  modifier = Modifier.background(if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color.Transparent)
                )
              }
            }
          }

          // Seçili Liman Bilgi Kartı (Aşağı Açılır / Genişletilebilir)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CardSubtle,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardSubtleBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showPortDetails = !showPortDetails }
              .testTag("card_selected_port_details")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlueDark, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Liman & Seyir Detayı: ${uiState.selectedPort.name}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = PrimaryBlueDark
                  )
                }
                Icon(
                  imageVector = if (showPortDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(16.dp)
                )
              }

              Text(
                text = uiState.selectedPort.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary)
              )

              AnimatedVisibility(visible = showPortDetails) {
                Column(
                  modifier = Modifier.padding(top = 4.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  HorizontalDivider(color = Color(0xFFCBD5E1))
                  Text(
                    text = "• Seyir Koordinatı: ${com.example.model.LocationPresets.formatMarineCoordinates(uiState.selectedPort.latitude, uiState.selectedPort.longitude)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextPrimary)
                  )
                  Text(
                    text = "• Standart Harita Derinliği: ${uiState.selectedPort.defaultChartedDepthMeters} metre",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextPrimary)
                  )
                  Text(
                    text = "• Ortalama Gelgit Genliği: ±${uiState.selectedPort.typicalTideRangeMeters} metre",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextPrimary)
                  )
                  Text(
                    text = "• Saat Dilimi: UTC+${uiState.selectedPort.timeZoneOffsetHours.toInt()} (TSİ)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextPrimary)
                  )
                }
              }
            }
          }
        }

        // Koordinat Girişleri
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = uiState.latStr,
            onValueChange = { viewModel.updateLat(it) },
            label = { Text("Enlem (Lat °N)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_latitude")
          )
          OutlinedTextField(
            value = uiState.lonStr,
            onValueChange = { viewModel.updateLon(it) },
            label = { Text("Boylam (Lon °E)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_longitude")
          )
        }

        // Harita Derinliği
        OutlinedTextField(
          value = uiState.chartedDepthStr,
          onValueChange = { viewModel.updateChartedDepth(it) },
          label = { Text("Harita Derinliği (Charted Depth - metre)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_charted_depth")
        )

        // Köprüüstü Hızlı Eylem Butonları (MOB & Demir At & Harita)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // MOB Butonu
          Button(
            onClick = {
              viewModel.triggerMob()
              onNavigateToMap()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (uiState.mobEvent.isActive) Color(0xFF991B1B) else DangerRed,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("btn_quick_mob")
          ) {
            Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.mobEvent.isActive) "MOB AKTİF" else "MOB (Adam Düştü)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp)
            )
          }

          // Demir At Butonu
          Button(
            onClick = {
              if (uiState.anchorEvent.isAnchored) {
                viewModel.liftAnchor()
              } else {
                viewModel.dropAnchor()
                onNavigateToMap()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else PrimaryBlueDark,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("btn_quick_anchor")
          ) {
            Icon(Icons.Default.Anchor, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At (10 Gom)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            )
          }
        }
      }
    }

    // AIS Bildirim / Uyarı Mesajları (Varsa)
    if (uiState.aisSuccessMessage != null) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF065F46),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = uiState.aisSuccessMessage,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFECFDF5))
            )
          }
          IconButton(onClick = { viewModel.dismissAisMessages() }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }
    }

    if (uiState.aisErrorMessage != null) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DangerRedDark,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = uiState.aisErrorMessage,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
          }
          IconButton(onClick = { viewModel.dismissAisMessages() }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }
    }

    // 2. GEMİ ÖZELLİKLERİ & SEYİR DEĞERLERİ KARTI (AÇILABİLİR / DARALTILABİLİR MENÜ)
    val chevronRotation by animateFloatAsState(
      targetValue = if (isVesselParamsExpanded) 180f else 0f,
      label = "chevronRotation"
    )

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isVesselParamsExpanded) PrimaryBlue.copy(alpha = 0.5f) else CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = if (isVesselParamsExpanded) 3.dp else 1.5.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("card_vessel_parameters")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        // Tıklanabilir Açılır / Kapanır Başlık Çubuğu (Yer Tasarruflu)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isVesselParamsExpanded = !isVesselParamsExpanded }
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(
                  if (isVesselParamsExpanded) PrimaryBlue else PrimaryBlue.copy(alpha = 0.12f),
                  RoundedCornerShape(10.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsBoat,
                contentDescription = null,
                tint = if (isVesselParamsExpanded) Color.White else PrimaryBlueDark,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Gemi Özellikleri & Seyir Değerleri",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = TextPrimary,
                softWrap = true
              )
              // Özet Gemi Değerleri Satırı
              Text(
                text = "${if (uiState.vesselName.isNotBlank()) "${uiState.vesselName} • " else ""}Draft: ${uiState.draftStr}m • Hız: ${uiState.speedStr}kn • Boy: ${uiState.loaStr}m",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = if (isVesselParamsExpanded) PrimaryBlueDark else TextMuted
                ),
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (uiState.isMmsiTrackingActive) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF065F46)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "MMSI AKTİF",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 8.5.sp,
                      color = Color(0xFFECFDF5)
                    )
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isVesselParamsExpanded) PrimaryBlue.copy(alpha = 0.12f) else CardSubtle,
              modifier = Modifier.size(32.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = if (isVesselParamsExpanded) "Menüyü Kapat" else "Menüyü Aç",
                  tint = if (isVesselParamsExpanded) PrimaryBlueDark else TextMuted,
                  modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
                )
              }
            }
          }
        }

        // Açılır / Kapanır Detay Giriş Alanları
        AnimatedVisibility(
          visible = isVesselParamsExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            HorizontalDivider(
              color = CardBorder.copy(alpha = 0.6f),
              thickness = 1.dp,
              modifier = Modifier.padding(bottom = 2.dp)
            )

            Text(
              text = "Gemi tipini, boyutlarını, MMSI numarasını ve anlık seyir değerlerini manuel olarak güncelleyebilir veya AIS üzerinden takip başlatabilirsiniz.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = TextMuted)
            )

            // Gemi Adı ve Gemi Tipi
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.vesselName,
                onValueChange = { viewModel.updateVesselName(it) },
                label = { Text("Gemi Adı") },
                placeholder = { Text("Gemi adı...") },
                singleLine = true,
                modifier = Modifier.weight(1.2f).testTag("input_vessel_name")
              )
              OutlinedTextField(
                value = uiState.vesselTypeStr,
                onValueChange = { viewModel.updateVesselType(it) },
                label = { Text("Gemi Tipi") },
                placeholder = { Text("Örn: Askeri / Kargo") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_type")
              )
            }

            // MMSI Numarası (Konum Takibi Başlatma Butonlu)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = uiState.mmsiStr,
                onValueChange = { viewModel.updateMmsi(it) },
                label = { Text("MMSI No (9 Hane)") },
                placeholder = { Text("271048179") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.1f).testTag("input_vessel_mmsi")
              )

              Button(
                onClick = {
                  if (uiState.isMmsiTrackingActive) {
                    viewModel.stopMmsiTracking()
                  } else {
                    viewModel.startMmsiTracking()
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (uiState.isMmsiTrackingActive) DangerRed else PrimaryBlue
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).testTag("btn_mmsi_track")
              ) {
                if (uiState.isAisLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Aranıyor...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                } else if (uiState.isMmsiTrackingActive) {
                  Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Takibi Durdur", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                } else {
                  Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("MMSI Takip Başlat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                }
              }
            }

            // IMO Numarası & Çağrı İşareti (Call Sign)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.imoStr,
                onValueChange = { viewModel.updateImo(it) },
                label = { Text("IMO No (7 Hane)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_imo")
              )
              OutlinedTextField(
                value = uiState.callSignStr,
                onValueChange = { viewModel.updateCallSign(it) },
                label = { Text("Çağrı İşareti (Call Sign)") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_callsign")
              )
            }

            // Boy (LOA) & En (Beam)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.loaStr,
                onValueChange = { viewModel.updateLoa(it) },
                label = { Text("Boy (LOA - m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_loa")
              )
              OutlinedTextField(
                value = uiState.beamStr,
                onValueChange = { viewModel.updateBeam(it) },
                label = { Text("En (Beam - m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_beam")
              )
            }

            // Draft & Blok Katsayısı (Cb) & Min UKC
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.draftStr,
                onValueChange = { viewModel.updateDraft(it) },
                label = { Text("Draft (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_draft")
              )
              OutlinedTextField(
                value = uiState.blockCoefficientStr,
                onValueChange = { viewModel.updateBlockCoefficient(it) },
                label = { Text("Blok Kats. (Cb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_cb")
              )
              OutlinedTextField(
                value = uiState.ukcStr,
                onValueChange = { viewModel.updateUkc(it) },
                label = { Text("Min UKC (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_ukc")
              )
            }

            // Suya Göre Sürat & Pruva Açısı
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.speedStr,
                onValueChange = { viewModel.updateSpeed(it) },
                label = { Text("Su Sürati (STW - kn)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_speed")
              )
              OutlinedTextField(
                value = uiState.headingDegreesStr,
                onValueChange = { viewModel.updateHeading(it) },
                label = { Text("Pruva Açısı (°)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_heading")
              )
            }

            // Menüyü Kapat Butonu
            OutlinedButton(
              onClick = { isVesselParamsExpanded = false },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark)
            ) {
              Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Menüyü Daralt / Kapat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }

    // 3. MARINETRAFFIC AIS ENTEGRASYONU (NB252 / MMSI: 222111447) KARTI
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_marinetraffic_ais")
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(Color(0xFF0284C7), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "MarineTraffic AIS Bilgileri",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
              )
              Text(
                text = "MMSI: 222111447 Entegrasyonu",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontSize = 11.sp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1E293B)
          ) {
            Text(
              text = "MARINETRAFFIC",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp, color = Color(0xFF94A3B8)),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Text(
          text = "MarineTraffic üzerindeki (MMSI: 222111447, IMO: 7654320, Call Sign: TST7) gemisinin gerçek zamanlı AIS telemetrisini, hızını, rotasını ve teknik özelliklerini doğrudan içe aktarın.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color(0xFFCBD5E1))
        )

        // Hızlı AIS Bilgi Önizlemesi (Yüklüyse veya Hazırsa)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Gemi", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF94A3B8)))
              Text(uiState.vesselName.ifEmpty { "-" }, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("MMSI / IMO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF94A3B8)))
              Text("222111447 / 7654320", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Boy / Draft", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF94A3B8)))
              Text("98m / 3.8m", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF34D399)))
            }
          }
        }

        // MarineTraffic AIS Butonları
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                viewModel.loadAtlanticMarineTrafficZone()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
              modifier = Modifier.weight(1f).testTag("btn_load_marinetraffic_atlantic_zone")
            ) {
              Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "MarineTraffic (-12/25 Z:4)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 13.sp),
                softWrap = true,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }

            Button(
              onClick = {
                viewModel.loadMarineTrafficShip10481795()
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
              modifier = Modifier.weight(1f).testTag("btn_load_vesselfinder_ship")
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Harita / AIS Getir",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                softWrap = true,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }

          OutlinedButton(
            onClick = {
              viewModel.setShowAisDetailDialog(true)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth().testTag("btn_open_ais_sheet")
          ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "AIS Canlı Detay & Gemi Haritası (VesselFinder)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              softWrap = true,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    // 3. DEMİRLEME KISMINA GEÇİŞ HIZLI KARTI
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = CardSubtle,
      border = androidx.compose.foundation.BorderStroke(1.dp, SeaGreenBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigateToAnchor() }
        .testTag("banner_go_to_anchor")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = SeaGreenDark,
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Anchor, contentDescription = null, tint = SeaGreen, modifier = Modifier.size(22.dp))
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Demirleme & Kaloma",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary
            )
            Text(
              text = "Salma Dairesi: ${String.format(java.util.Locale.US, "%.1f", uiState.anchorCalculationResult.f_secondSwingingCircleMeters)}m • Alt Menüden Aç",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = SeaGreen
            )
          }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SeaGreen)
      }
    }

    // 4. GPS SÜRATİ & YERE GÖRE SÜRAT ANALİZ KARTI
    SpeedVectorAnalysisCard(
      speedAnalysis = uiState.speedCalculationResult,
      onSyncGpsSpeed = { viewModel.syncGpsTelemetryToInputs() },
      onRequestGps = { requestLocationAndFetch() }
    )

    // 5. GELGİT HESAPLAMA VE GRAFİĞE GEÇİŞ BUTONU
    Button(
      onClick = onNavigateToTide,
      colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryBlue,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("btn_proceed_to_tide_chart")
    ) {
      Text(
        text = "Gelgit Analizine Git",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }

    Spacer(modifier = Modifier.height(16.dp))
  }

  // AIS Gemi Detayları ve MarineTraffic Canlı Bilgi İletişim Kutusu
  if (uiState.showAisDetailDialog) {
    val aisToShow = uiState.activeAisVesselData ?: com.example.engine.AisTrackingEngine.getMarineTraffic10481795ShipData()
    AisVesselDetailDialog(
      aisData = aisToShow,
      onDismiss = { viewModel.setShowAisDetailDialog(false) },
      onApplyToApp = { selectedShip ->
        viewModel.applyAisVesselData(selectedShip)
      }
    )
  }
}
