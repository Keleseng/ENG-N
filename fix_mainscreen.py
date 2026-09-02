with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

import re

# Clean up broken Rail fragment
content = re.sub(r'          },\s*icon = \{\s*Icon\(\s*imageVector = if \(uiState.selectedTabIndex == 3.*?Modifier\.testTag\("rail_tab_weather"\)\s*\)\s*', '', content, flags=re.DOTALL)

# Clean up broken Bar fragment
content = re.sub(r'            },\s*icon = \{\s*Icon\(\s*imageVector = if \(uiState.selectedTabIndex == 3.*?Modifier\.testTag\("bar_tab_weather"\)\s*\)\s*', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

