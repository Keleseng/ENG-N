import re
with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'r') as f:
    content = f.read()

pattern = r"""          // 1\. Mevki Kaynak Seçim Sekmeleri \(GPS / AIS\).*?            \}"""

replacement = """          // 1. Mevki Kaynak Seçim Sekmeleri (GPS / AIS)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (activeSourceTab == 0) Color(0xFF38BDF8) else Color.Transparent,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (activeSourceTab == 0) Color.Transparent else Color(0xFF1E293B)),
              modifier = Modifier
                .weight(1f)
                .clickable { activeSourceTab = 0 }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  Icons.Default.GpsFixed,
                  contentDescription = null,
                  tint = if (activeSourceTab == 0) Color(0xFF0F172A) else Color(0xFF94A3B8),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "GPS",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (activeSourceTab == 0) Color(0xFF0F172A) else Color(0xFF94A3B8)
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (activeSourceTab == 1) Color(0xFF38BDF8) else Color.Transparent,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (activeSourceTab == 1) Color.Transparent else Color(0xFF1E293B)),
              modifier = Modifier
                .weight(1f)
                .clickable { activeSourceTab = 1 }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  Icons.Default.DirectionsBoat,
                  contentDescription = null,
                  tint = if (activeSourceTab == 1) Color(0xFF0F172A) else Color(0xFF94A3B8),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "AIS",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (activeSourceTab == 1) Color(0xFF0F172A) else Color(0xFF94A3B8)
                )
              }
            }
          }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'w') as f:
    f.write(new_content)
