package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.outlined.Air
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

          // 1. ANAMENÜ (Seyir Parametreleri & Dinamik Hesaplamalar)
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 0,
            onClick = { viewModel.setTab(0) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = "Anamenü"
              )
            },
            label = {
              Text(
                "Anamenü",
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_home")
          )

          // 2. DENİZ & HAVA DURUMU
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 3,
            onClick = { viewModel.setTab(3) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.Air else Icons.Outlined.Air,
                contentDescription = "Hava Durumu"
              )
            },
            label = {
              Text(
                "Hava",
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 3) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_weather")
          )

          // 3. AIS & KOORDİNATLAR
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 5,
            onClick = { viewModel.setTab(5) },
            icon = {
              BadgedBox(
                badge = {
                  if (uiState.mmsiStr.isNotBlank()) {
                    Badge(
                      containerColor = SeaGreen,
                      contentColor = Color.White
                    ) {
                      Text("AIS", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 5) Icons.Filled.DirectionsBoat else Icons.Outlined.DirectionsBoat,
                  contentDescription = "AIS"
                )
              }
            },
            label = {
              Text(
                "AIS",
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 5) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_ais")
          )

          // 3. DEMİRLEME & 10 GOMİNA
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 1,
            onClick = { viewModel.setTab(1) },
            icon = {
              BadgedBox(
                badge = {
                  if (uiState.anchorEvent.isAnchored) {
                    Badge(
                      containerColor = DangerRed,
                      contentColor = Color.White
                    ) {
                      Text("10G", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 1) Icons.Filled.Anchor else Icons.Outlined.Anchor,
                  contentDescription = "Demirleme"
                )
              }
            },
            label = {
              Text(
                "Demirleme",
                fontSize = 8.5.sp,
                fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_anchor")
          )

          // 4. GELGİT HESAPLAMA
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 2,
            onClick = { viewModel.setTab(2) },
            icon = {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = if (uiState.analysis.isCurrentlySafe) SeaGreen else DangerRed,
                    contentColor = Color.White
                  ) {
                    Text("UKC", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 2) Icons.Filled.Waves else Icons.Outlined.Waves,
                  contentDescription = "Gelgit"
                )
              }
            },
            label = {
              Text(
                "Gelgit",
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 2) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_tide")
          )

          // 5. CANLI HARİTA (Canlı AIS Haritası)
          NavigationRailItem(
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
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 4) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_map")
          )

          // 6. OTOMATİK TXT LOG KAYDI
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 6,
            onClick = { viewModel.setTab(6) },
            icon = {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = SeaGreen,
                    contentColor = Color.White
                  ) {
                    Text("30m", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 6) Icons.Filled.Save else Icons.Outlined.Save,
                  contentDescription = "Kaydet"
                )
              }
            },
            label = {
              Text(
                "Kaydet",
                fontSize = 9.sp,
                fontWeight = if (uiState.selectedTabIndex == 6) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_txt_log")
          )

          // 6. GECE / GÜNDÜZ MODU SEKMESİ (Sadece sembol)
          NavigationRailItem(
            selected = false,
            onClick = { viewModel.toggleDarkMode() },
            icon = {
              Icon(
                imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (uiState.isDarkMode) "Gündüz Moduna Geç" else "Gece Moduna Geç",
                tint = if (uiState.isDarkMode) Color(0xFFFDE047) else Color.White
              )
            },
            alwaysShowLabel = false,
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_dark_mode_toggle")
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
        // Yatay Ekran İçerik Alanı
        MainContentArea(uiState = uiState, viewModel = viewModel, modifier = Modifier.fillMaxSize().statusBarsPadding())
      }
    }
  } else {
    // ══════════════════════════════════════════════════════════════════════
    // DİKEY KONUMLANDIRMA (PORTRAIT MODE)
    // ══════════════════════════════════════════════════════════════════════
    Scaffold(
      modifier = modifier.fillMaxSize().statusBarsPadding(),
      bottomBar = {
        val navBg = getMarineCardBg(uiState.isDarkMode)
        val navBorder = getMarineCardBorder(uiState.isDarkMode)
        Surface(
          color = navBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, navBorder),
          shadowElevation = 10.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          NavigationBar(
            containerColor = navBg,
            tonalElevation = 0.dp,
            modifier = Modifier
              .testTag("bottom_nav_bar")
              .windowInsetsPadding(WindowInsets.navigationBars)
          ) {

          // 1. ANAMENÜ (Seyir Parametreleri & Dinamik Hesaplamalar)
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 0,
              onClick = { viewModel.setTab(0) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                  contentDescription = "Anamenü",
                  modifier = Modifier.size(20.dp)
                )
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_home")
            )

            // 2. DENİZ & HAVA DURUMU
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 3,
              onClick = { viewModel.setTab(3) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.Air else Icons.Outlined.Air,
                  contentDescription = "Hava Durumu",
                  modifier = Modifier.size(20.dp)
                )
              },
              alwaysShowLabel = false,
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_weather")
            )

            // 3. AIS & KOORDİNATLAR
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 5,
              onClick = { viewModel.setTab(5) },
              icon = {
                BadgedBox(
                  badge = {
                    if (uiState.mmsiStr.isNotBlank()) {
                      Badge(
                        containerColor = SeaGreen,
                        contentColor = Color.White
                      ) {
                        Text("AIS", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (uiState.selectedTabIndex == 5) Icons.Filled.DirectionsBoat else Icons.Outlined.DirectionsBoat,
                    contentDescription = "AIS",
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_ais")
            )

            // 3. DEMİRLEME & 10 GOMİNA
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 1,
              onClick = { viewModel.setTab(1) },
              icon = {
                BadgedBox(
                  badge = {
                    if (uiState.anchorEvent.isAnchored) {
                      Badge(
                        containerColor = DangerRed,
                        contentColor = Color.White
                      ) {
                        Text("10G", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (uiState.selectedTabIndex == 1) Icons.Filled.Anchor else Icons.Outlined.Anchor,
                    contentDescription = "Demirleme",
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_anchor")
            )

            // 4. GELGİT HESAPLAMA
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 2,
              onClick = { viewModel.setTab(2) },
              icon = {
                BadgedBox(
                  badge = {
                    Badge(
                      containerColor = if (uiState.analysis.isCurrentlySafe) SeaGreen else DangerRed,
                      contentColor = Color.White
                    ) {
                      Text("UKC", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (uiState.selectedTabIndex == 2) Icons.Filled.Waves else Icons.Outlined.Waves,
                    contentDescription = "Gelgit",
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_tide")
            )

            // 6. CANLI HARİTA (Canlı AIS Haritası)
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 4,
              onClick = { viewModel.setTab(4) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 4) Icons.Filled.Map else Icons.Outlined.Map,
                  contentDescription = "Harita",
                  modifier = Modifier.size(20.dp)
                )
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_map")
            )

            // 7. OTOMATİK TXT LOG KAYDI
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 6,
              onClick = { viewModel.setTab(6) },
              icon = {
                BadgedBox(
                  badge = {
                    Badge(
                      containerColor = SeaGreen,
                      contentColor = Color.White
                    ) {
                      Text("30m", fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (uiState.selectedTabIndex == 6) Icons.Filled.Save else Icons.Outlined.Save,
                    contentDescription = "Kaydet",
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_txt_log")
            )

            // 6. GECE / GÜNDÜZ AYAR SEKMESİ (Sadece sembol)
            NavigationBarItem(
              selected = false,
              onClick = { viewModel.toggleDarkMode() },
              icon = {
                Icon(
                  imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                  contentDescription = if (uiState.isDarkMode) "Gündüz Moduna Geç" else "Gece Moduna Geç",
                  tint = if (uiState.isDarkMode) Color(0xFFFDE047) else PrimaryBlue,
                  modifier = Modifier.size(22.dp)
                )
              },
              alwaysShowLabel = false,
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_dark_mode_toggle")
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
  val canvasBg = getMarineCanvasBg(uiState.isDarkMode)
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(canvasBg)
  ) {
    when (uiState.selectedTabIndex) {
      0 -> InputParametersView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToTide = { viewModel.setTab(2) },
        onNavigateToAnchor = { viewModel.setTab(1) },
        onNavigateToWeather = { viewModel.setTab(3) },
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToAis = { viewModel.setTab(5) }
      )
      5 -> AisCoordinatesView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToAnchor = { viewModel.setTab(1) },
        onNavigateToAnamenu = { viewModel.setTab(0) }
      )
      1 -> AnchorCalculationView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToTide = { viewModel.setTab(2) },
        onNavigateToMap = { viewModel.setTab(4) }
      )
      2 -> TideWindowsView(
        uiState = uiState,
        viewModel = viewModel
      )
      3 -> MarineWeatherView(
        uiState = uiState,
        viewModel = viewModel
      )
      4 -> MarineMapView(
        uiState = uiState,
        viewModel = viewModel
      )
      6 -> com.example.ui.screens.TxtLogManagerView(
        uiState = uiState,
        viewModel = viewModel
      )
      else -> InputParametersView(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToTide = { viewModel.setTab(2) },
        onNavigateToAnchor = { viewModel.setTab(1) },
        onNavigateToWeather = { viewModel.setTab(3) },
        onNavigateToMap = { viewModel.setTab(4) },
        onNavigateToAis = { viewModel.setTab(5) }
      )
    }
  }
}

@Composable
private fun navItemColors(isDarkMode: Boolean) = NavigationBarItemDefaults.colors(
  selectedIconColor = Color.White,
  selectedTextColor = if (isDarkMode) MarineCyan else PrimaryBlue,
  indicatorColor = if (isDarkMode) PrimaryBlueLight else PrimaryBlue,
  unselectedIconColor = getMarineTextSecondary(isDarkMode),
  unselectedTextColor = getMarineTextSecondary(isDarkMode)
)

@Composable
private fun railItemColors() = NavigationRailItemDefaults.colors(
  selectedIconColor = Color.White,
  selectedTextColor = Color(0xFFBAE6FD),
  indicatorColor = PrimaryBlue,
  unselectedIconColor = Color.White.copy(alpha = 0.7f),
  unselectedTextColor = Color.White.copy(alpha = 0.7f)
)
