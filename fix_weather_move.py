with open('app/src/main/java/com/example/ui/screens/MarineWeatherView.kt', 'r') as f:
    mw_content = f.read()

# Extract from MarineWeatherCard to the end of Beaufort Guide
mw_card_start = mw_content.find('    // 2. ANA DENİZ & HAVA DURUMU KARTI')
beaufort_end_marker = '    Spacer(modifier = Modifier.height(16.dp))\n  }\n}'
beaufort_end = mw_content.find(beaufort_end_marker)

cards_to_move = mw_content[mw_card_start:beaufort_end].strip()
cards_to_move = "        " + cards_to_move.replace("\n", "\n        ")

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    ip_content = f.read()

insertion_idx = ip_content.find('    // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI')
if insertion_idx != -1:
    idx_end_gps = ip_content.rfind('      }\n    }\n', 0, insertion_idx)
    if idx_end_gps != -1:
        ip_content = ip_content[:idx_end_gps] + "\n" + cards_to_move + "\n" + ip_content[idx_end_gps:]
        print("Injected successfully!")
    else:
        print("idx_end_gps not found")
else:
    print("insertion_idx not found")

cards_to_move_fixed = cards_to_move.replace('isDarkMode', 'isDark')
ip_content = ip_content.replace(cards_to_move, cards_to_move_fixed)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(ip_content)

