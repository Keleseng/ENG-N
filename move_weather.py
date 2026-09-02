import re

# 1. Read MarineWeatherView.kt
with open('app/src/main/java/com/example/ui/screens/MarineWeatherView.kt', 'r') as f:
    mw_content = f.read()

# Find the start of MarineWeatherCard
mw_card_start = mw_content.find('    // 2. ANA DENİZ & HAVA DURUMU KARTI')
if mw_card_start == -1:
    print("Could not find start of MarineWeatherCard")
    exit(1)

# Find the end of Beaufort Guide
beaufort_end_marker = '    Spacer(modifier = Modifier.height(16.dp))\n  }\n}'
beaufort_end = mw_content.find(beaufort_end_marker)
if beaufort_end == -1:
    print("Could not find end of Beaufort Guide")
    exit(1)

cards_to_move = mw_content[mw_card_start:beaufort_end].strip()
cards_to_move = "\n\n        " + cards_to_move.replace("\n", "\n        ")

# 2. Add imports in InputParametersView.kt if needed
with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    ip_content = f.read()

if 'import com.example.ui.components.MarineWeatherCard' not in ip_content:
    ip_content = ip_content.replace('import com.example.ui.components.SpeedVectorAnalysisCard', 'import com.example.ui.components.SpeedVectorAnalysisCard\nimport com.example.ui.components.MarineWeatherCard\nimport com.example.ui.components.RealisticMoonPhaseCard\nimport com.example.ui.components.WindyVerificationCard')

# 3. Add showBeaufortGuide variable
if 'var showBeaufortGuide' not in ip_content:
    ip_content = ip_content.replace('var isVesselParamsExpanded', 'var showBeaufortGuide by rememberSaveable { mutableStateOf(false) }\n  var isVesselParamsExpanded')

# 4. Find injection point
# We want to inject it inside `card_weather_extra_metrics`, or just right below it but inside the GPS card.
# The `card_weather_extra_metrics` is in a Column.
# The end of the GPS Card is:
#     // ══════════════════════════════════════════════════════════════════════
#       }
#     }
#
#         // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI

injection_target = """    // ══════════════════════════════════════════════════════════════════════
      }
    }

        // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI"""

if injection_target not in ip_content:
    print("Could not find injection target")
    # try another way
    insertion_idx = ip_content.find('        // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI')
    if insertion_idx != -1:
        # Search backwards for the two closing braces of the GPS Card
        idx_end_gps = ip_content.rfind('      }\n    }\n', 0, insertion_idx)
        if idx_end_gps != -1:
            ip_content = ip_content[:idx_end_gps] + cards_to_move + "\n" + ip_content[idx_end_gps:]
            print("Injected via fallback")
        else:
            print("Could not fallback inject")
else:
    ip_content = ip_content.replace(injection_target, cards_to_move + '\n' + injection_target)
    print("Injected via primary target")

# Replace isDarkMode with isDark in the pasted cards since InputParametersView uses isDark
# But let's only replace it within the cards_to_move portion we just injected
# Actually, the python script injected it, so let's just do a global replace of isDarkMode to isDark within cards_to_move
cards_to_move_fixed = cards_to_move.replace('isDarkMode', 'isDark')
ip_content = ip_content.replace(cards_to_move, cards_to_move_fixed)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(ip_content)

print("Migration complete")

