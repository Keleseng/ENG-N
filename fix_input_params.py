with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    content = f.read()

import re

# There is a malformed block:
#            )
#        },
#              isDarkMode = isDark,
#              modifier = Modifier.fillMaxWidth()
#            )
# We need to remove the broken leftover parts from WindyVerificationCard removal

broken_pattern = r'            \)\s*},\s*isDarkMode = isDark,\s*modifier = Modifier\.fillMaxWidth\(\)\s*\)'
content = re.sub(broken_pattern, '            )', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(content)

