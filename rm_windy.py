with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    content = f.read()

import re

# Remove WindyVerificationCard
content = re.sub(r'            // ══════════════════════════════════════════════════════════════════════\s*// 3\. WINDY CANLI DENİZ & HAVA DURUMU DOĞRULAMA KARTI\s*// ══════════════════════════════════════════════════════════════════════\s*WindyVerificationCard\([^)]*\)\s*', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(content)

