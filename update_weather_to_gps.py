import re

with open('app/src/main/java/com/example/ui/screens/MarineWeatherView.kt', 'r') as f:
    mw_content = f.read()

# Extract Beaufort Guide variables
show_beaufort = re.search(r'var showBeaufortGuide by rememberSaveable \{ mutableStateOf\(false\) \}', mw_content)

# Extract everything from MarineWeatherCard to the end of Beaufort Guide
# Find the start of MarineWeatherCard
mw_card_start = mw_content.find('    // 2. ANA DENİZ & HAVA DURUMU KARTI')
if mw_card_start == -1:
    print("Could not find start of MarineWeatherCard")
    exit(1)

# Find the end of Beaufort Guide
beaufort_end = mw_content.find('    Spacer(modifier = Modifier.height(16.dp))\n  }\n}')
if beaufort_end == -1:
    print("Could not find end of Beaufort Guide")
    exit(1)

cards_to_move = mw_content[mw_card_start:beaufort_end].strip()

# Now update InputParametersView.kt
with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    ip_content = f.read()

# Find where to inject variables
ip_var_target = 'val uiState by viewModel.uiState.collectAsState()'
ip_var_idx = ip_content.find(ip_var_target)
if ip_var_idx != -1:
    ip_content = ip_content[:ip_var_idx] + 'var showBeaufortGuide by rememberSaveable { mutableStateOf(false) }\n  ' + ip_content[ip_var_idx:]
else:
    print("Could not find var target")
    
# We need `animateFloatAsState` in InputParametersView.kt.
# Let's just add it to imports
imports_target = 'import androidx.compose.ui.unit.sp'
if imports_target in ip_content:
    ip_content = ip_content.replace(imports_target, imports_target + '\nimport androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.expandVertically\nimport androidx.compose.animation.shrinkVertically\nimport androidx.compose.animation.fadeIn\nimport androidx.compose.animation.fadeOut\nimport androidx.compose.ui.draw.rotate\nimport com.example.ui.components.MarineWeatherCard\nimport com.example.ui.components.RealisticMoonPhaseCard\nimport com.example.ui.components.WindyVerificationCard')

# Find the end of card_weather_extra_metrics
# It ends with:
#           }
#         }
#       }
#     }
#     // ══════════════════════════════════════════════════════════════════════
#       }
#     }
#
#         // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI
target_insertion = '    // ══════════════════════════════════════════════════════════════════════\n      }\n    }\n\n        // 1. SEYİR MEVKİİ & GPS KOORDİNATLARI'
insert_idx = ip_content.find(target_insertion)
if insert_idx != -1:
    # we insert before the closure of the GPS Column
    # The target_insertion starts with the divider for the end of card_weather_extra_metrics, then the closing braces for GPS column and card.
    # Actually wait, let's see exactly what target_insertion looks like.
    pass

