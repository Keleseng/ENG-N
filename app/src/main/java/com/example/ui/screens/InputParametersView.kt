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
import com.example.model.LocationPresets
import com.example.model.VesselPresets
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AisVesselDetailDialog
import com.example.ui.components.AnchorCalculationCard
import com.example.ui.components.SpeedVectorAnalysisCard
import com.example.ui.components.MarineWeatherCard
import com.example.ui.components.RealisticMoonPhaseCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputParametersView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToTide: () -> Unit = {},
  onNavigateToWeather: () -> Unit = {},
  onNavigateToAnchor: () -> Unit = {},
  onNavigateToMap: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider(context) }
  var showBeaufortGuide by rememberSaveable { mutableStateOf(false) }
  var isVesselParamsExpanded by rememberSaveable { mutableStateOf(false) }

  val isDark = uiState.isDarkMode
  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val subtleBg = getMarineSubtleBg(isDark)
  val subtleBorder = getMarineSubtleBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)
  val textMuted = getMarineTextMuted(isDark)
  val inputBg = getMarineInputBg(isDark)
  val inputBorder = getMarineInputBorder(isDark)
  val inputLabel = getMarineInputLabel(isDark)

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

    
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_gps_section")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isDark) MarineCyan else PrimaryBlueDark, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GPS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp), color = textPrimary)
        }
        // GPS CANLI DENİZ TELEMETRİ KARTI - TÜM VERİLER AYRI KUTUCUKLARDA
        val activeGps = uiState.lastGpsFix
        if (activeGps != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().testTag("card_gps_marine_telemetry")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              // Canlı Telemetri Başlığı
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "GPS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp),
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                  )
                }

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White
                ) {
                  Text(
                    text = "Hassasiyet: ±${String.format(Locale.US, "%.1f", activeGps.accuracyMeters)}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // 1. Satır Kutucuklar: Enlem & Boylam (Denizci DDM Formatında)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Enlem", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLatDDM(activeGps.latitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Boylam", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLonDDM(activeGps.longitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                  }
                }
              }

              // 2. Satır Kutucuklar: Rota (COG) & Hız (SOG)
              val rotDeg = activeGps.bearingDegrees?.toInt() ?: uiState.analysis.vesselHeadingDegrees
              val sogKnots = activeGps.speedKnots ?: uiState.analysis.vesselSpeedKnots
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // ROTA (COG)
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Rota",
                        tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
                        modifier = Modifier.size(13.dp).rotate(rotDeg.toFloat())
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        "ROTA (COG)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = textSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${String.format(Locale.US, "%03d", rotDeg)}°",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.5.sp),
                      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                    Text(
                      text = uiState.analysis.vesselHeadingCardinal,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                      color = if (isDark) MarineCyan else PrimaryBlueDark
                    )
                  }
                }

                // HIZ (SOG)
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.Speed,
                        contentDescription = "Hız",
                        tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        "HIZ (SOG)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = textSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${String.format(Locale.US, "%.1f", sogKnots)} kn",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.5.sp),
                      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                    Text(
                      text = "Squat: +${uiState.analysis.calculatedSquatMeters} m",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                      color = DangerRed
                    )
                  }
                }
              }

              // 3. Satır Kutucuklar: Akıntı & Rüzgar
              val curr = uiState.analysis.currentInfo
              val wind = uiState.analysis.windInfo
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // AKINTI
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.Waves,
                        contentDescription = "Akıntı",
                        tint = if (isDark) MarineCyan else PrimaryBlue,
                        modifier = Modifier.size(13.dp).rotate(curr.directionDegrees.toFloat())
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        "AKINTI",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = textSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${curr.speedKnots} kn • ${String.format(Locale.US, "%03d°", curr.directionDegrees)}",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                      color = textPrimary
                    )
                    Text(
                      text = "${curr.phaseName} • ${curr.directionCardinal}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                      color = if (isDark) MarineCyan else PrimaryBlueDark,
                      maxLines = 1
                    )
                  }
                }

                // RÜZGAR
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.Air,
                        contentDescription = "Rüzgar",
                        tint = if (isDark) MarineCyan else PrimaryBlue,
                        modifier = Modifier.size(13.dp).rotate(wind.directionDegrees.toFloat())
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        "RÜZGAR",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = textSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${wind.speedKnots} kn • ${String.format(Locale.US, "%03d°", wind.directionDegrees)}",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                      color = textPrimary
                    )
                    Text(
                      text = "Bft ${wind.beaufortScale} • ${wind.directionCardinal}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                      color = if (isDark) MarineCyan else PrimaryBlueDark,
                      maxLines = 1
                    )
                  }
                }
              }

              // 4. Satır Kutucuklar: İrtifa & Dinamik UKC
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Derinlik", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = "${String.format(Locale.US, "%.1f", activeGps.altitudeMeters ?: 0.0)} m",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                      color = textPrimary
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Dinamik UKC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = "${String.format(Locale.US, "%.2f", uiState.analysis.currentInstantUkcMeters)} m",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                      color = if (uiState.analysis.isCurrentlySafe) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                  }
                }
              }

              // 5. Satır Kutucuk: Mevki & En Yakın Liman Bilgisi
              if (uiState.gpsNearestPortInfo != null) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Anchor,
                      contentDescription = null,
                      tint = if (isDark) MarineCyan else PrimaryBlueDark,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Mevki: ${uiState.gpsNearestPortInfo}",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                      color = textPrimary,
                      maxLines = 1
                    )
                  }
                }
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
              Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = uiState.gpsErrorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (isDark) Color(0xFFFCA5A5) else DangerRedDark,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { viewModel.dismissGpsMessages() },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = DangerRed, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
        // 5. DENİZ GÖRÜŞÜ & EKSTRA METEOROLOJİ PARAMETRELERİ
    // ══════════════════════════════════════════════════════════════════════
    val weather = uiState.marineWeather
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_weather_extra_metrics")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = if (isDark) MarineCyan else PrimaryBlueDark,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Meteoroloji",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = textPrimary
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Deniz Görüş Mesafesi
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = subtleBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deniz Görüşü", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = textSecondary)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "> 10 NM (Açık)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text("İyi Seyir Görüşü", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = SeaGreen))
            }
          }

          // Deniz Suyu Sıcaklığı
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = subtleBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Thermostat, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deniz Suyu Sıcaklığı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = textSecondary)
              }
              Spacer(modifier = Modifier.height(2.dp))
              val seaTemp = (weather.temperatureC - 1.5).coerceAtLeast(4.0)
              Text(
                text = "${String.format(Locale.US, "%.1f", seaTemp)}°C",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text("Yüzey Sıcaklığı", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = MarineCyan))
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Çiy Noktası
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = subtleBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Water, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Çiy Noktası", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = textSecondary)
              }
              Spacer(modifier = Modifier.height(2.dp))
              val dewPoint = weather.temperatureC - ((100 - weather.relativeHumidityPercent) / 5.0)
              Text(
                text = "${String.format(Locale.US, "%.1f", dewPoint)}°C",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text("Sis Riski Düşük", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = textSecondary))
            }
          }

          // Hava Basınç Kararlılığı
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = subtleBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Basınç Eğilimi", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = textSecondary)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${weather.surfacePressureHpa} hPa",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                color = textPrimary
              )
              Text(
                text = if (weather.surfacePressureHpa >= 1013) "Yüksek (Kararlı)" else "Alçak (Dinamik)",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  color = if (weather.surfacePressureHpa >= 1013) SeaGreen else WarningAmber
                ),
                maxLines = 1
              )
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
              isDarkMode = isDark,
              modifier = Modifier.fillMaxWidth()
            )
        
            // ══════════════════════════════════════════════════════════════════════
            // 4. GERÇEKÇİ 3D AY EVRESİ & ASTRONOMİK GELGİT ÇEKİM GÖRSELİ
            // ══════════════════════════════════════════════════════════════════════
            RealisticMoonPhaseCard(
              analysis = uiState.analysis,
              isDarkMode = isDark,
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = if (isDark) MarineCyan else PrimaryBlueDark)
                      )
                    }
                  }
        
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isDark) MarineCyan else PrimaryBlueDark,
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
                        color = if (isCurrent) (if (isDark) Color(0xFF78350F) else WarningAmberLight) else subtleBg,
                        border = androidx.compose.foundation.BorderStroke(
                          1.dp,
                          if (isCurrent) (if (isDark) Color(0xFFFBBF24) else WarningAmberBorder) else subtleBorder
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
                                color = if (isCurrent) (if (isDark) Color(0xFFFBBF24) else WarningAmber) else (if (isDark) MarineCyan else PrimaryBlue)
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
      }
    }
    

    // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
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
              tint = if (isDark) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Koordinatlar",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
              color = textPrimary,
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
              Text("Aranıyor...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Color.White))
            } else if (uiState.isContinuousTrackingActive) {
              Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Canlı Takip", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
            } else {
              Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Konum Yenile", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
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
            label = { Text("Enlem (Lat °N)", fontWeight = FontWeight.Bold) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
            colors = inputTextFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_latitude")
          )
          OutlinedTextField(
            value = uiState.lonStr,
            onValueChange = { viewModel.updateLon(it) },
            label = { Text("Boylam (Lon °E)", fontWeight = FontWeight.Bold) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
            colors = inputTextFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_longitude")
          )
        }

        // Harita Derinliği
        OutlinedTextField(
          value = uiState.chartedDepthStr,
          onValueChange = { viewModel.updateChartedDepth(it) },
          label = { Text("Harita Derinliği (Charted Depth - metre)", fontWeight = FontWeight.Bold) },
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
          colors = inputTextFieldColors,
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
              if (uiState.mobEvent.isActive) {
                viewModel.cancelMob()
              } else {
                viewModel.triggerMob()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (uiState.mobEvent.isActive) Color(0xFF991B1B) else DangerRed,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("btn_quick_mob")
          ) {
            Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.mobEvent.isActive) "MOB AKTİF" else "MOB (Adam Düştü)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
              color = Color.White
            )
          }

          // Demir At Butonu
          Button(
            onClick = {
              if (uiState.anchorEvent.isAnchored) {
                viewModel.liftAnchor()
              } else {
                viewModel.dropAnchor()
                onNavigateToAnchor()
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
            Icon(Icons.Default.Anchor, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = Color.White
            )
          }
        }

        // MOB Sabit GPS Mevkii Penceresi
        if (uiState.mobEvent.isActive) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().testTag("card_mob_fixed_position")
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
                  Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "🚨 MOB SABİT MEVKİİ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp, letterSpacing = 0.5.sp),
                    color = Color(0xFFEF4444)
                  )
                }
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "Kayıt: ${uiState.mobEvent.timeFormatted}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Sabit Enlem", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLatDDM(uiState.mobEvent.latitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = Color(0xFFEF4444)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Sabit Boylam", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLonDDM(uiState.mobEvent.longitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = Color(0xFFEF4444)
                    )
                  }
                }
              }

              val currentLat = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
              val currentLon = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
              val distGomina = uiState.mobEvent.calculateDistanceGomina(currentLat, currentLon)
              val bearingDeg = uiState.mobEvent.calculateBearingDegrees(currentLat, currentLon)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Mesafe: ${String.format(Locale.US, "%.1f Gomina (%.2f NM)", distGomina, distGomina / 10.0)}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = textPrimary
                )
                Text(
                  text = "Kerteriz: ${String.format(Locale.US, "%03d°", bearingDeg)}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = textPrimary
                )
              }
            }
          }
        }

        // Demir Sabit GPS Mevkii Penceresi
        if (uiState.anchorEvent.isAnchored) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth().testTag("card_anchor_fixed_position")
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
                  Icon(Icons.Default.Anchor, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "⚓ DEMİRLEME SABİT MEVKİİ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.5.sp, letterSpacing = 0.5.sp),
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                  )
                }
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "Saat: ${uiState.anchorEvent.dropTimeFormatted}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Sabit Enlem", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLatDDM(uiState.anchorEvent.latitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color.White,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text("Sabit Boylam", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                    Text(
                      text = LocationPresets.formatMarineLonDDM(uiState.anchorEvent.longitude),
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                      color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Derinlik: ${String.format(Locale.US, "%.1f", uiState.anchorEvent.chartedDepthAtDropMeters)} m",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = textPrimary
                )
                Text(
                  text = "Emniyet: ${String.format(Locale.US, "%.1f Gomina", uiState.anchorEvent.safeSwingingRadiusGomina)}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = textPrimary
                )
              }
            }
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
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isVesselParamsExpanded) (if (isDark) MarineCyan else PrimaryBlue) else cardBorder),
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
                  if (isVesselParamsExpanded) (if (isDark) PrimaryBlue else PrimaryBlueDark) else (if (isDark) PrimaryBlueLight else Color(0xFFDBEAFE)),
                  RoundedCornerShape(10.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsBoat,
                contentDescription = null,
                tint = if (isVesselParamsExpanded) Color.White else (if (isDark) MarineCyan else PrimaryBlueDark),
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Gemi Özellikleri & Seyir Değerleri",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 14.5.sp
                ),
                color = textPrimary,
                softWrap = true
              )
              // Özet Gemi Değerleri Satırı
              Text(
                text = "${if (uiState.vesselName.isNotBlank()) "${uiState.vesselName} • " else ""}Draft: ${uiState.draftStr}m • Hız: ${uiState.speedStr}kn • Boy: ${uiState.loaStr}m",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (isVesselParamsExpanded) (if (isDark) MarineCyan else PrimaryBlueDark) else textSecondary
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
              color = if (isVesselParamsExpanded) (if (isDark) PrimaryBlueLight else Color(0xFFDBEAFE)) else subtleBg,
              modifier = Modifier.size(32.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = if (isVesselParamsExpanded) "Menüyü Kapat" else "Menüyü Aç",
                  tint = if (isVesselParamsExpanded) (if (isDark) MarineCyan else PrimaryBlueDark) else textSecondary,
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
              color = subtleBorder,
              thickness = 1.dp,
              modifier = Modifier.padding(bottom = 2.dp)
            )

            Text(
              text = "Gemi tipini, boyutlarını, MMSI numarasını ve anlık seyir değerlerini manuel olarak güncelleyebilir veya AIS üzerinden takip başlatabilirsiniz.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal, color = textSecondary)
            )

            // Gemi Adı ve Gemi Tipi
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = uiState.vesselName,
                onValueChange = { viewModel.updateVesselName(it) },
                label = { Text("Gemi Adı", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Gemi adı...") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                singleLine = true,
                modifier = Modifier.weight(1.2f).testTag("input_vessel_name")
              )
              OutlinedTextField(
                value = uiState.vesselTypeStr,
                onValueChange = { viewModel.updateVesselType(it) },
                label = { Text("Gemi Tipi", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Örn: Askeri / Kargo") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
                label = { Text("MMSI No (9 Hane)", fontWeight = FontWeight.Bold) },
                placeholder = { Text("271048179") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
                  containerColor = if (uiState.isMmsiTrackingActive) DangerRed else PrimaryBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).testTag("btn_mmsi_track")
              ) {
                if (uiState.isAisLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Aranıyor...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Color.White))
                } else if (uiState.isMmsiTrackingActive) {
                  Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Takibi Durdur", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
                } else {
                  Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("MMSI Takip Başlat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
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
                label = { Text("IMO No (7 Hane)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_imo")
              )
              OutlinedTextField(
                value = uiState.callSignStr,
                onValueChange = { viewModel.updateCallSign(it) },
                label = { Text("Çağrı İşareti (Call Sign)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
                label = { Text("Boy (LOA - m)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_loa")
              )
              OutlinedTextField(
                value = uiState.beamStr,
                onValueChange = { viewModel.updateBeam(it) },
                label = { Text("En (Beam - m)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
                label = { Text("Draft (m)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_draft")
              )
              OutlinedTextField(
                value = uiState.blockCoefficientStr,
                onValueChange = { viewModel.updateBlockCoefficient(it) },
                label = { Text("Blok Kats. (Cb)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_cb")
              )
              OutlinedTextField(
                value = uiState.ukcStr,
                onValueChange = { viewModel.updateUkc(it) },
                label = { Text("Min UKC (m)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
                label = { Text("Su Sürati (STW - kn)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_vessel_speed")
              )
              OutlinedTextField(
                value = uiState.headingDegreesStr,
                onValueChange = { viewModel.updateHeading(it) },
                label = { Text("Pruva Açısı (°)", fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp),
                colors = inputTextFieldColors,
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
              colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) MarineCyan else PrimaryBlueDark)
            ) {
              Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Menüyü Daralt / Kapat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }

    // 3. GPS SÜRATİ & YERE GÖRE SÜRAT ANALİZ KARTI
    SpeedVectorAnalysisCard(
      speedAnalysis = uiState.speedCalculationResult,
      onSyncGpsSpeed = { viewModel.syncGpsTelemetryToInputs() },
      onRequestGps = { requestLocationAndFetch() },
      isDarkMode = isDark
    )

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
      },
      isDarkMode = isDark
    )
  }
}
