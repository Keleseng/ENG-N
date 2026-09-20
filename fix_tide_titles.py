import re
with open('app/src/main/java/com/example/ui/screens/TideWindowsView.kt', 'r') as f:
    content = f.read()

# Remove the safe entry windows title item
pattern1 = r"\s*// 5\. Güvenli Giriş-Çıkış Zaman Pencereleri Başlığı\s*item \{\s*Row\([\s\S]*?\}\s*\}\s*\}"
content = re.sub(pattern1, "", content)

# Remove the dynamic ship cross section title item
pattern2 = r"\s*// 6\. DİNAMİK 2D GEMİ KESİTİ & SQUAT SİMÜLASYONU\s*item \{\s*Spacer\(modifier = Modifier\.height\(2\.dp\)\)\s*Text\(\s*text = \"Dinamik Gemi Su Altı Kesiti & Squat\",[\s\S]*?\)\s*\}"
content = re.sub(pattern2, "", content)

with open('app/src/main/java/com/example/ui/screens/TideWindowsView.kt', 'w') as f:
    f.write(content)
