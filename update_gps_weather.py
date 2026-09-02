import re

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    input_text = f.read()

# Extract GPS block
gps_start_idx = input_text.find('// GPS CANLI DENİZ TELEMETRİ KARTI')
gps_end_idx = input_text.find('// Koordinat Girişleri')

if gps_start_idx != -1 and gps_end_idx != -1:
    gps_block = input_text[gps_start_idx:gps_end_idx].strip()
    input_text = input_text[:gps_start_idx] + input_text[gps_end_idx:]
    
    # We also need to extract meteorology from MarineWeatherView
    with open('app/src/main/java/com/example/ui/screens/MarineWeatherView.kt', 'r') as f_mw:
        mw_text = f_mw.read()
    
    met_start = mw_text.find('// 5. DENİZ GÖRÜŞÜ')
    met_end = mw_text.find('// 5. BEAUFORT RÜZGAR')
    
    met_block = ""
    if met_start != -1 and met_end != -1:
        # Extract and remove from MarineWeatherView
        met_block = mw_text[met_start:met_end].strip()
        mw_text = mw_text[:met_start] + mw_text[met_end:]
        with open('app/src/main/java/com/example/ui/screens/MarineWeatherView.kt', 'w') as f_mw:
            f_mw.write(mw_text)
            
        # Change "Denizcilik Meteoroloji Detayları" to "Meteoroloji"
        met_block = met_block.replace('"Denizcilik Meteoroloji Detayları"', '"Meteoroloji"')
        # Replace 'weather' with 'uiState.weatherCalculationResult' if needed. Wait, does InputParametersView have access to weather? 
        # In MarineWeatherView, it uses `val weather = uiState.weatherCalculationResult`. Let's add that to met_block.
        met_block = met_block.replace("Card(", "val weather = uiState.weatherCalculationResult\n    Card(")
    
    new_gps_card = f"""
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_gps_section")
    ) {{
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {{
        Row(verticalAlignment = Alignment.CenterVertically) {{
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isDark) MarineCyan else PrimaryBlueDark, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GPS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp), color = textPrimary)
        }}
        {gps_block}
        {met_block}
      }}
    }}
    """
    
    # Insert before // 1. SEYİR MEVKİİ
    insert_idx = input_text.find('// 1. SEYİR MEVKİİ')
    if insert_idx != -1:
        input_text = input_text[:insert_idx] + new_gps_card + "\n\n    " + input_text[insert_idx:]
        
    with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
        f.write(input_text)
        
    print("Manipulation successful.")
else:
    print("Could not find boundaries.")

