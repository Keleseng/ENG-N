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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AisVesselDetailDialog
import com.example.ui.components.AnchorCalculationCard
import com.example.ui.components.BridgeTelemetryCard
import com.example.ui.components.EtaPositionCard
import com.example.ui.components.InstantWaterDepthCard

import com.example.ui.components.MarineInclinometerCard
import com.example.ui.components.SpeedVectorAnalysisCard
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
  onNavigateToAis: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider(context) }
  var isVesselParamsExpanded by rememberSaveable { mutableStateOf(false) }

  val attitude = uiState.marineAttitude
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

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWideScreen = isLandscape || maxWidth > 600.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 6.dp, vertical = 3.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
    // GPS & KÖPRÜÜSTÜ TELEMETRİ KARTI - TÜM VERİLER AYRI KUTUCUKLARDA
    val activeGps = uiState.lastGpsFix
    val displayLat = activeGps?.latitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.selectedPort.latitude)
    val displayLon = activeGps?.longitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.selectedPort.longitude)
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
      modifier = Modifier.fillMaxWidth().testTag("card_gps_marine_telemetry")
    ) {
      Column(
        modifier = Modifier.padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        // Canlı Telemetri Başlığı
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (activeGps != null) Icons.Default.GpsFixed else Icons.Default.Explore,
              contentDescription = null,
              tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (activeGps != null) "GPS TELEMETRİSİ" else "KÖPRÜÜSTÜ TELEMETRİSİ",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp),
              color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
            )
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
          ) {
            Text(
              text = if (activeGps != null) "Hassasiyet: ±${String.format(Locale.US, "%.1f", activeGps.accuracyMeters)}m" else "STATİK / KÖPRÜÜSTÜ",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
              color = if (activeGps != null) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // 1. Satır Kutucuklar: Enlem & Boylam
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
              .padding(horizontal = 12.dp, vertical = 14.dp)
          ) {
            Text(
              text = "Enlem",
              color = Color(0xFFFACC15),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
              modifier = Modifier
                .offset(x = (-4).dp, y = (-22).dp)
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                .padding(horizontal = 4.dp)
            )
            Text(
              text = LocationPresets.formatMarineLatDMS(displayLat),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
              color = if (isDark) Color.White else Color.Black
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
              .padding(horizontal = 12.dp, vertical = 14.dp)
          ) {
            Text(
              text = "Boylam",
              color = Color(0xFFFACC15),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
              modifier = Modifier
                .offset(x = (-4).dp, y = (-22).dp)
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                .padding(horizontal = 4.dp)
            )
            Text(
              text = LocationPresets.formatMarineLonDMS(displayLon),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
              color = if (isDark) Color.White else Color.Black
            )
          }
        }

        // PUSULA (COMPASS / HDG) - Koordinatların hemen altında tek olarak yerleştirilmiş pencere
        val compassDeg = attitude.compassDegrees.toInt()
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
          modifier = Modifier.fillMaxWidth().testTag("card_compass_heading")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MiniShipHeadingIcon(
                headingDegrees = attitude.compassDegrees,
                isDark = isDark
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "PUSULA (HDG)",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp),
                  color = textSecondary
                )
                Text(
                  text = attitude.compassCardinal,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                  color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${String.format(Locale.US, "%03d", compassDeg)}°",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.5.sp),
                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
              )
              Text(
                text = if (attitude.isHoldActive) "🔒 Sabitlendi" else if (attitude.isSensorActive) "Canlı Sensör (Kararlı)" else "Cayro / Manyetik",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = textSecondary)
              )
            }
          }
        }

        // 2. Satır Kutucuklar (İkili): Rota (COG) & Hız (SOG)
        val rotDeg = activeGps?.bearingDegrees?.toInt() ?: (uiState.headingDegreesStr.toIntOrNull() ?: uiState.analysis.vesselHeadingDegrees)
        val sogKnots = activeGps?.speedKnots ?: (uiState.speedStr.toDoubleOrNull() ?: uiState.analysis.vesselSpeedKnots)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // ROTA (COG)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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
                  color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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
                  color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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
                  color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text("Derinlik", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                      if (uiState.verifiedMarineDepth != null) {
                        Surface(
                          shape = RoundedCornerShape(3.dp),
                          color = if (uiState.verifiedMarineDepth.isOnlineVerified) Color(0xFF059669).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f)
                        ) {
                          Text(
                            text = if (uiState.verifiedMarineDepth.isOnlineVerified) "🇪🇺 EMODnet" else "🗺️ HARİTA",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Black),
                            color = if (uiState.verifiedMarineDepth.isOnlineVerified) Color(0xFF10B981) else MarineCyan,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                          )
                        }
                      }
                    }
                    val currentDepth = uiState.verifiedMarineDepth?.depthMeters ?: (uiState.chartedDepthStr.toDoubleOrNull() ?: 18.0)
                    Text(
                      text = "${String.format(Locale.US, "%.1f", currentDepth)} m",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                      color = if (currentDepth < 5.0) WarningAmber else (if (isDark) MarineCyan else PrimaryBlueDark)
                    )
                    Text(
                      text = "Harita (CD)",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = textSecondary),
                      maxLines = 1
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
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

    // 4. ETA & MEVKİ GİRİŞİ PENCERESİ (DMS Koordinat Girişi, Hızlı Limanlar, Haritada Göster & ETA Hesabı)
    EtaPositionCard(
      uiState = uiState,
      viewModel = viewModel,
      onShowOnMap = { coord ->
        viewModel.setMapFocusCoordinate(coord)
        onNavigateToMap()
      },
      isDarkMode = isDark
    )

    // 5. GPS SÜRATİ & YERE GÖRE SÜRAT ANALİZ KARTI
    SpeedVectorAnalysisCard(
      speedAnalysis = uiState.speedCalculationResult,
      onSyncGpsSpeed = { viewModel.syncGpsTelemetryToInputs() },
      onRequestGps = { requestLocationAndFetch() },
      isDarkMode = isDark
    )

    // 5. ANLIK SU DERİNLİĞİ HESAPLAMASI KARTI (ANLIK DURUM PENCERESİ KALDIRILDI)
    InstantWaterDepthCard(
      analysis = uiState.analysis,
      isDarkMode = isDark
    )

    // 6. MEHİL \ TRİM (INCLINOMETER) PENCERESİ
    MarineInclinometerCard(
      attitude = uiState.marineAttitude,
      isDark = isDark,
      onTare = { viewModel.tareAttitude() },
      onToggleHold = { viewModel.toggleAttitudeHold() },
      modifier = Modifier.fillMaxWidth()
    )
  }
}

  // AIS Gemi Detayları ve Canlı Bilgi İletişim Kutusu
  if (uiState.showAisDetailDialog) {
    val aisToShow = uiState.activeAisVesselData ?: com.example.engine.AisTrackingEngine.getNb252ShipData()
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

/**
 * Anamenüdeki pusula kısmında gidilen açıya (HDG / Pusula derecesi) doğru dönen çok küçük gemi simgesi
 */
@Composable
fun MiniShipHeadingIcon(
  headingDegrees: Float,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val animatedHeading by animateFloatAsState(
    targetValue = headingDegrees,
    label = "miniShipHeading"
  )

  Surface(
    shape = CircleShape,
    color = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF),
    border = androidx.compose.foundation.BorderStroke(
      0.8.dp,
      if (isDark) Color(0xFF38BDF8).copy(alpha = 0.45f) else Color(0xFF0284C7).copy(alpha = 0.35f)
    ),
    modifier = modifier.size(28.dp).testTag("mini_ship_heading_icon")
  ) {
    Box(contentAlignment = Alignment.Center) {
      // Pusula kadranı minik kuzey (K / 000°) referans noktası
      Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(
          color = if (isDark) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFFDC2626).copy(alpha = 0.6f),
          radius = 1.5f,
          center = Offset(size.width / 2f, 3.2f)
        )
      }

      // Gidilen açıya doğru dönen çok küçük gemi simgesi
      Canvas(
        modifier = Modifier
          .size(19.dp)
          .rotate(animatedHeading)
      ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Denizci Gemi Gövdesi Silueti (Pruva sivri, bordalar oval, kıç düz ayna)
        val shipHull = Path().apply {
          moveTo(cx, h * 0.08f) // Pruva (Baş bodoslama)
          // Sancak (sağ) borda
          cubicTo(
            cx + w * 0.30f, h * 0.26f,
            cx + w * 0.38f, h * 0.54f,
            cx + w * 0.30f, h * 0.90f
          )
          // Kıç (Ayna)
          lineTo(cx - w * 0.30f, h * 0.90f)
          // İskele (sol) borda
          cubicTo(
            cx - w * 0.38f, h * 0.54f,
            cx - w * 0.30f, h * 0.26f,
            cx, h * 0.08f
          )
          close()
        }

        val hullColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        val strokeColor = if (isDark) Color(0xFFE0F2FE) else Color(0xFF075985)
        val cabinColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)

        // Gövde dolgusu
        drawPath(path = shipHull, color = hullColor)
        // Gövde bordür konturu
        drawPath(
          path = shipHull,
          color = strokeColor,
          style = Stroke(width = 1.2f)
        )

        // Köprüüstü / Yaşam mahalli (Kabin)
        val cabinW = w * 0.32f
        val cabinH = h * 0.20f
        val cabinTop = h * 0.58f
        drawRoundRect(
          color = cabinColor,
          topLeft = Offset(cx - cabinW / 2f, cabinTop),
          size = Size(cabinW, cabinH),
          cornerRadius = CornerRadius(1.5f, 1.5f)
        )

        // Pruva kerteriz yön çizgisi (Baş bodoslamadan ileriye doğru)
        drawLine(
          color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0369A1),
          start = Offset(cx, h * 0.08f),
          end = Offset(cx, 0f),
          strokeWidth = 1.5f
        )
      }
    }
  }
}
