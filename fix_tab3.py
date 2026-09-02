import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Add WindyView to when block:
# We know the when block uses uiState.selectedTabIndex
# 0 -> InputParametersView(...)
# 1 -> AnchorRecommendationView(...)
# 2 -> TideCalculationView(...)
# 4 -> MarineMapView(...)
# Let's add 3 -> WindyView(...)
content = content.replace(
    '      4 -> MarineMapView(uiState = uiState, viewModel = viewModel, modifier = modifier.padding(innerPadding))',
    '      3 -> WindyView(uiState = uiState, viewModel = viewModel, modifier = modifier.padding(innerPadding))\n      4 -> MarineMapView(uiState = uiState, viewModel = viewModel, modifier = modifier.padding(innerPadding))'
)

# Add NavigationRailItem for tab 3
rail_target = '          // 3. GELGİT HESAPLAMA\n          NavigationRailItem('
rail_insert = '''          // 4. WINDY
          NavigationRailItem(
            selected = uiState.selectedTabIndex == 3,
            onClick = { viewModel.setTab(3) },
            icon = {
              Icon(
                imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.Air else Icons.Outlined.Air,
                contentDescription = "Windy"
              )
            },
            label = {
              Text(
                "Windy",
                fontSize = 11.sp,
                fontWeight = if (uiState.selectedTabIndex == 3) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = railItemColors(),
            modifier = Modifier.testTag("rail_tab_windy")
          )
'''
# Actually wait, I need to insert it *after* tab 2 closing.
# Let's just find `// 1. CANLI HARİTA` inside rail block
# It's at the end of the rail items.
content = content.replace('          // 1. CANLI HARİTA (MarineTraffic AIS)', rail_insert + '          // 1. CANLI HARİTA (MarineTraffic AIS)')


# Add NavigationBarItem for tab 3
bar_insert = '''            // 4. WINDY
            NavigationBarItem(
              selected = uiState.selectedTabIndex == 3,
              onClick = { viewModel.setTab(3) },
              icon = {
                Icon(
                  imageVector = if (uiState.selectedTabIndex == 3) Icons.Filled.Air else Icons.Outlined.Air,
                  contentDescription = "Windy"
                )
              },
              label = {
                Text(
                  "Windy",
                  fontSize = 11.sp,
                  fontWeight = if (uiState.selectedTabIndex == 3) FontWeight.ExtraBold else FontWeight.Medium
                )
              },
              colors = navItemColors(uiState.isDarkMode),
              modifier = Modifier.testTag("tab_windy")
            )
'''
content = content.replace('            // 1. CANLI HARİTA (MarineTraffic AIS)', bar_insert + '            // 1. CANLI HARİTA (MarineTraffic AIS)')


with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
