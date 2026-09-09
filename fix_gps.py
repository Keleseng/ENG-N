import re

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    content = f.read()

# Replace the start of the outer card with just a Row containing the Dark mode button
start_target = """    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("card_gps_section")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isDark) MarineCyan else PrimaryBlueDark, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GPS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp), color = textPrimary)
          }
          
          IconButton(
            onClick = { viewModel.toggleDarkMode() },
            modifier = Modifier
              .size(32.dp)
              .background(subtleBg, CircleShape)
              .testTag("btn_theme_toggle_inline")
          ) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = if (isDark) "Aydınlık Moda Geç" else "Koyu Moda Geç",
              tint = if (isDark) Color(0xFFFDE047) else Color(0xFF93C5FD),
              modifier = Modifier.size(16.dp)
            )
          }
        }"""

replacement_start = """    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { viewModel.toggleDarkMode() },
        modifier = Modifier
          .size(32.dp)
          .background(subtleBg, CircleShape)
          .testTag("btn_theme_toggle_inline")
      ) {
        Icon(
          imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
          contentDescription = if (isDark) "Aydınlık Moda Geç" else "Koyu Moda Geç",
          tint = if (isDark) Color(0xFFFDE047) else Color(0xFF93C5FD),
          modifier = Modifier.size(16.dp)
        )
      }
    }"""

content = content.replace(start_target, replacement_start)

# Now we need to remove the closing braces for the Card and Column
# The end of the block is around the gpsErrorMessage
end_target = """        if (uiState.gpsErrorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DangerRedLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRedBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = uiState.gpsErrorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (isDark) Color(0xFFFCA5A5) else DangerRedDark,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { viewModel.dismissGpsMessages() },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = DangerRed, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }"""

replacement_end = """        if (uiState.gpsErrorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DangerRedLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRedBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = uiState.gpsErrorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (isDark) Color(0xFFFCA5A5) else DangerRedDark,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { viewModel.dismissGpsMessages() },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = DangerRed, modifier = Modifier.size(14.dp))
              }
            }
          }
        }"""

content = content.replace(end_target, replacement_end)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(content)
