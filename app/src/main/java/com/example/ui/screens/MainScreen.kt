package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: TideNavViewModel,
  onRequestPermission: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  if (isLandscape) {
    // ══════════════════════════════════════════════════════════════════════
    // YATAY KONUMLANDIRMA (LANDSCAPE MODE / TABLET)
    // ══════════════════════════════════════════════════════════════════════
    Row(
      modifier = modifier
        .fillMaxSize()
        .background(HeaderNavy)
    ) {
      // 1. Sol Dikey Navigasyon Menüsü (Navigation Rail)
      Surface(
        color = HeaderNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder.copy(alpha = 0.2f)),
        shadowElevation = 8.dp,
        modifier = Modifier
          .fillMaxHeight()
          .widthIn(min = 96.dp)
      ) {
        NavigationRail(
          containerColor = HeaderNavy,
          header = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(PrimaryBlue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.DirectionsBoat,
                  contentDescription = "Gemi Navigasyonu",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "KÖPRÜÜSTÜ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                color = TextWhite
              )
            }
          },
          modifier = Modifier
            .fillMaxHeight()
            .testTag("landscape_nav_rail")
            .navigationBarsPadding()
        ) {
          Spacer(modifier = Modifier.weight(1f))

          // TAB 0: PARAMETRELER
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 0,
            onClick = { viewModel.setTab(0) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 0) Icons.Filled.Tune else Icons.Outlined.Tune,
                contentDescription = "Parametre"
              )
            },
            label = {
              Text(
                "Parametre",
                fontSize = 11.sp,
                fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_params")
          )

          // TAB 1: DEMİRLEME
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 1,
            onClick = { viewModel.setTab(1) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 1) Icons.Filled.Anchor else Icons.Outlined.Anchor,
                contentDescription = "Demirleme"
              )
            },
            label = {
              Text(
                "Demirleme",
                fontSize = 10.sp,
                fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_anchor")
          )

          // TAB 2: GELGİT
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 2,
            onClick = { viewModel.setTab(2) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 2) Icons.Filled.Waves else Icons.Outlined.Waves,
                contentDescription = "Gelgit"
              )
            },
            label = {
              Text(
                "Gelgit",
                fontSize = 11.sp,
                fontWeight = if (uiState.selectedTabIndex == 2) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_tide")
          )

          // TAB 3: DENİZ & HAVA DURUMU
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 3,
            onClick = { viewModel.setTab(3) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                contentDescription = "Hava Durumu"
              )
            },
            label = {
              Text(
                "Hava",
                fontSize = 11.sp,
                fontWeight = if (uiState.selectedTabIndex == 3) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_weather")
          )

          // TAB 4: CANLI HARİTA (MarineTraffic)
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 4,
            onClick = { viewModel.setTab(4) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 4) Icons.Filled.Map else Icons.Outlined.Map,
                contentDescription = "Canlı Harita"
              )
            },
            label = {
              Text(
                "Harita",
                fontSize = 11.sp,
                fontWeight = if (uiState.selectedTabIndex == 4) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_map")
          )

          Spacer(modifier = Modifier.weight(1f))

          // Canlı Güvenlik Durumu
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (uiState.analysis.isCurrentlySafe) SeaGreen.copy(alpha = 0.25f) else DangerRed.copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (uiState.analysis.isCurrentlySafe) Color(0xFF34D399) else Color(0xFFF87171)
            ),
            modifier = Modifier
              .padding(bottom = 12.dp)
              .widthIn(max = 84.dp)
          ) {
            Text(
              text = if (uiState.analysis.isCurrentlySafe) "GÜVENLİ" else "DİKKAT",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
              color = if (uiState.analysis.isCurrentlySafe) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      // 2. Sağ İçerik ve Üst Durum Şeridi
      Column(modifier = Modifier.fillMaxSize()) {
        Surface(
          color = HeaderNavy,
          shadowElevation = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (uiState.vesselName.isNotBlank()) {
                Text(
                  text = uiState.vesselName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                  color = TextWhite
                )
                Spacer(modifier = Modifier.width(8.dp))
              }
              Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
              ) {
                val locationTag = if (uiState.isGpsActive) "🛰️ GPS: ${uiState.latStr}°, ${uiState.lonStr}°" else "${uiState.latStr}°, ${uiState.lonStr}°"
                Text(
                  text = locationTag,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = if (uiState.isGpsActive) Color(0xFF6EE7B7) else Color(0xFFBAE6FD),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Mevki Suyu: ${uiState.analysis.currentInstantTotalDepthMeters}m (UKC: +${uiState.analysis.currentInstantUkcMeters}m)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = if (uiState.analysis.isCurrentlySafe) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
              )
              Text(
                text = "GPS SOG: ${uiState.speedCalculationResult.gpsSpeedKnots} kn • COG: ${String.format("%03d°", uiState.speedCalculationResult.groundCourseDegrees)}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.85f)
              )
            }
          }
        }

        // Yatay Ekran İçerik Alanı
        MainContentArea(uiState = uiState, viewModel = viewModel, modifier = Modifier.fillMaxSize())
      }
    }
  } else {
    // ══════════════════════════════════════════════════════════════════════
    // DİKEY KONUMLANDIRMA (PORTRAIT MODE)
    // ══════════════════════════════════════════════════════════════════════
    Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        Surface(
          color = HeaderNavy,
          shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // 1. Üst Başlık Satırı: Gemi İkonu + İsim + Emniyet Rozeti & Su Derinliği
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
                    .size(36.dp)
                    .background(PrimaryBlue, RoundedCornerShape(10.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.DirectionsBoat,
                    contentDescription = "Gemi",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = uiState.vesselName.ifBlank { uiState.vesselTypeStr.ifBlank { "Gemi Bilgisi" } },
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 15.sp,
                      letterSpacing = 0.5.sp
                    ),
                    color = TextWhite,
                    maxLines = 1,
                    softWrap = true
                  )
                  Text(
                    text = "MMSI: ${uiState.mmsiStr} • ${uiState.vesselTypeStr}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Anlık Durum Rozeti & Derinlik
              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = if (uiState.analysis.isCurrentlySafe) SeaGreen.copy(alpha = 0.25f) else DangerRed.copy(alpha = 0.25f),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.analysis.isCurrentlySafe) Color(0xFF34D399) else Color(0xFFF87171)
                  )
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(7.dp)
                        .background(
                          if (uiState.analysis.isCurrentlySafe) Color(0xFF34D399) else Color(0xFFF87171),
                          CircleShape
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (uiState.analysis.isCurrentlySafe) "GÜVENLİ" else "BEKLEMEDE",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp),
                      color = if (uiState.analysis.isCurrentlySafe) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                    )
                  }
                }

                Text(
                  text = "Su: ${uiState.analysis.currentInstantTotalDepthMeters}m (UKC: +${uiState.analysis.currentInstantUkcMeters}m)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                  color = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }

            // 2. Dikey Ekrana Özel İkinci Bilgi Şeridi: Deniz GPS Koordinatı & Güneş Saatleri
            val latNum = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
            val lonNum = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
            val sunInfo = uiState.marineWeather.sunTimes ?: com.example.engine.SunCalculator.calculateSunTimes(latNum, lonNum)
            val marineCoordStr = com.example.model.LocationPresets.formatMarineCoordinates(latNum, lonNum)

            Surface(
              color = Color.White.copy(alpha = 0.12f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = if (uiState.isGpsActive) "🛰️ GPS: $marineCoordStr" else "📍 $marineCoordStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = if (uiState.isGpsActive) Color(0xFF6EE7B7) else Color(0xFFBAE6FD),
                    softWrap = true,
                    maxLines = 1
                  )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                  text = "🌅 ${sunInfo.sunriseFormatted}  🌇 ${sunInfo.sunsetFormatted}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                  color = Color(0xFFFDE68A)
                )
              }
            }

            // MOB / Demirleme Hızlı Bildirim Şeridi
            if (uiState.mobEvent.isActive || uiState.anchorEvent.isAnchored) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (uiState.mobEvent.isActive) {
                  Surface(
                    color = DangerRed,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { viewModel.setTab(4) }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "🚨 MOB AKTİF (Haritaya Git)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                        color = Color.White
                      )
                    }
                  }
                }

                if (uiState.anchorEvent.isAnchored) {
                  Surface(
                    color = Color(0xFF059669),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { viewModel.setTab(1) }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Anchor, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "⚓ DEMİRDE (10 Gomina)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = Color.White
                      )
                    }
                  }
                }
              }
            }
          }
        }
      },
      bottomBar = {
        Surface(
          color = CardWhite,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          shadowElevation = 10.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          NavigationBar(
            containerColor = CardWhite,
            tonalElevation = 0.dp,
            modifier = Modifier
              .testTag("bottom_nav_bar")
              .windowInsetsPadding(WindowInsets.navigationBars)
          ) {
            // 1. PARAMETRE
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 0,
              onClick = { viewModel.setTab(0) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 0) Icons.Filled.Tune else Icons.Outlined.Tune,
                  contentDescription = "Parametre"
                )
              },
              label = {
                Text(
                  "Parametre",
                  fontSize = 11.sp,
                  fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(),
              modifier = Modifier.testTag("tab_params")
            )

            // 2. DEMİRLEME
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 1,
              onClick = { viewModel.setTab(1) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 1) Icons.Filled.Anchor else Icons.Outlined.Anchor,
                  contentDescription = "Demirleme"
                )
              },
              label = {
                Text(
                  "Demirleme",
                  fontSize = 10.sp,
                  fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(),
              modifier = Modifier.testTag("tab_anchor")
            )

            // 3. GELGİT
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 2,
              onClick = { viewModel.setTab(2) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 2) Icons.Filled.Waves else Icons.Outlined.Waves,
                  contentDescription = "Gelgit"
                )
              },
              label = {
                Text(
                  "Gelgit",
                  fontSize = 11.sp,
                  fontWeight = if (uiState.selectedTabIndex == 2) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(),
              modifier = Modifier.testTag("tab_tide")
            )

            // 4. DENİZ & HAVA DURUMU
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 3,
              onClick = { viewModel.setTab(3) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                  contentDescription = "Hava Durumu"
                )
              },
              label = {
                Text(
                  "Hava",
                  fontSize = 11.sp,
                  fontWeight = if (uiState.selectedTabIndex == 3) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(),
              modifier = Modifier.testTag("tab_weather")
            )

            // 5. CANLI HARİTA
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 4,
              onClick = { viewModel.setTab(4) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 4) Icons.Filled.Map else Icons.Outlined.Map,
                  contentDescription = "Harita"
                )
              },
              label = {
                Text(
                  "Harita",
                  fontSize = 11.sp,
                  fontWeight = if (uiState.selectedTabIndex == 4) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(),
              modifier = Modifier.testTag("tab_map")
            )
          }
        }
      }
    ) { innerPadding ->
      MainContentArea(
        uiState = uiState,
        viewModel = viewModel,
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      )
    }
  }
}

@Composable
private fun MainContentArea(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            BackgroundCanvas,
            HeaderNavy,
            Color(0xFF040A14)
          )
        )
      )
  ) {
    when (uiState.selectedTabIndex) {
      0 -> InputParametersView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToTide = { viewModel.setTab(2) },
        onNavigateToAnchor = { viewModel.setTab(1) },
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToWeather = { viewModel.setTab(3) }
      )
      1 -> AnchorCalculationView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToTide = { viewModel.setTab(2) }
      )
      2 -> TideWindowsView(
        uiState = uiState,
        viewModel = viewModel
      )
      3 -> MarineWeatherView(
        uiState = uiState,
        viewModel = viewModel
      )
      4 -> MarineWeatherMapView(
        uiState = uiState,
        viewModel = viewModel
      )
      else -> InputParametersView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToTide = { viewModel.setTab(2) },
        onNavigateToAnchor = { viewModel.setTab(1) },
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToWeather = { viewModel.setTab(3) }
      )
    }
  }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
  selectedIconColor = Color.White,
  selectedTextColor = PrimaryBlueBorder,
  indicatorColor = PrimaryBlue,
  unselectedIconColor = TextMuted,
  unselectedTextColor = TextMuted
)

@Composable
private fun railItemColors() = NavigationRailItemDefaults.colors(
  selectedIconColor = Color.White,
  selectedTextColor = Color(0xFFBAE6FD),
  indicatorColor = PrimaryBlue,
  unselectedIconColor = Color.White.copy(alpha = 0.7f),
  unselectedTextColor = Color.White.copy(alpha = 0.7f)
)
