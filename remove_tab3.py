import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Remove the rail item for tab 3
rail_pattern = r'// 4\. DENİZ & HAVA DURUMU\s*NavigationRailItem\(\s*selected = uiState\.selectedTabIndex == 3,.*?\)\s*'
content = re.sub(rail_pattern, '', content, flags=re.DOTALL)

# Remove the bar item for tab 3
bar_pattern = r'// 4\. DENİZ & HAVA DURUMU\s*NavigationBarItem\(\s*selected = uiState\.selectedTabIndex == 3,.*?\)\s*'
content = re.sub(bar_pattern, '', content, flags=re.DOTALL)

# Remove MarineWeatherView from the switch statement
# 3 -> MarineWeatherView(uiState = uiState, viewModel = viewModel, modifier = modifier.padding(innerPadding))
switch_pattern = r'3 -> MarineWeatherView\([^)]*\)\s*'
content = re.sub(switch_pattern, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

print("Tabs removed.")
