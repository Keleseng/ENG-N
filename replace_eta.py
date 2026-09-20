import re

with open('app/src/main/java/com/example/ui/components/CompactEtaCard.kt', 'r') as f:
    content = f.read()

pattern = r"""      Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*Text\(\s*text = "DMS:",[\s\S]*?innerTextField\(\)\s*\}\s*\)\s*\}\s*\}"""

replacement = """      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
          Text(
            text = "Enlem",
            color = Color(0xFFFACC15),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
              .offset(x = (-4).dp, y = (-22).dp)
              .background(cardBg)
              .padding(horizontal = 4.dp)
          )
          BasicTextField(
            value = manualLat,
            onValueChange = { 
              manualLat = it
              activeField = 0
              val lat = parseCoordinate(it)
              val lon = parseCoordinate(manualLon)
              if (lat != null && lon != null) {
                onDestinationSelected(SimpleDestination("Manuel: ${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lon)}", lat, lon))
              }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Color.Black),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
              if (manualLat.isEmpty()) Text("41°00'49\"K", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textMuted, maxLines = 1)
              innerTextField()
            }
          )
        }
        
        Box(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
          Text(
            text = "Boylam",
            color = Color(0xFFFACC15),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
              .offset(x = (-4).dp, y = (-22).dp)
              .background(cardBg)
              .padding(horizontal = 4.dp)
          )
          BasicTextField(
            value = manualLon,
            onValueChange = { 
              manualLon = it
              activeField = 1
              val lat = parseCoordinate(manualLat)
              val lon = parseCoordinate(it)
              if (lat != null && lon != null) {
                onDestinationSelected(SimpleDestination("Manuel: ${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lon)}", lat, lon))
              }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Color.Black),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
              if (manualLon.isEmpty()) Text("028°58'33\"D", fontSize = 13.sp, fontWeight = FontWeight.Black, color = textMuted, maxLines = 1)
              innerTextField()
            }
          )
        }
      }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
if content != new_content:
    with open('app/src/main/java/com/example/ui/components/CompactEtaCard.kt', 'w') as f:
        f.write(new_content)
    print("Replaced successfully")
else:
    print("Pattern not found")
