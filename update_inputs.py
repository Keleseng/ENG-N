import re
with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    content = f.read()

pattern = r"""        // 1\. Satır Kutucuklar: Enlem & Boylam \(Denizci DDM Formatında\).*?          \}"""

replacement = """        // 1. Satır Kutucuklar: Enlem & Boylam
        Row(
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
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .padding(horizontal = 4.dp)
            )
            Text(
              text = LocationPresets.formatMarineLatDMS(displayLat),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
              color = if (isDark) Color.White else Color.Black
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
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .padding(horizontal = 4.dp)
            )
            Text(
              text = LocationPresets.formatMarineLonDMS(displayLon),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
              color = if (isDark) Color.White else Color.Black
            )
          }
        }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(new_content)
