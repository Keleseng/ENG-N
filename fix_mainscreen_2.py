with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

import re

# Find "},              icon = {" and remove down to testTag("tab_weather")
content = re.sub(r'            \},\s*icon = \{\s*Icon\(\s*imageVector = if \(uiState.selectedTabIndex == 3.*?Modifier\.testTag\("tab_weather"\)\s*\)\s*', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

