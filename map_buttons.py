with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'r') as f:
    content = f.read()

import re

# We will inject the info panels just before the FAB Column.
# The FAB column starts with:
#    // ══════════════════════════════════════════════════════════════════════
#    // SAĞ ALT KONTROL BUTONLARI (AIS GEMİSİ / GPS AL / ODAKLAN / YENİLE)

info_panels = """    // ══════════════════════════════════════════════════════════════════════
    // MOB VE DEMİR BİLGİ PANELLERİ
    // ══════════════════════════════════════════════════════════════════════
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        .fillMaxWidth(0.9f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (uiState.mobEvent.isActive) {
        val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.mobEvent.latitude
        val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.mobEvent.longitude
        val mobDistNm = uiState.mobEvent.calculateDistanceNm(currentLat, currentLon)
        val mobDistGomina = uiState.mobEvent.calculateDistanceGomina(currentLat, currentLon)
        val mobBearing = uiState.mobEvent.calculateBearingDegrees(currentLat, currentLon)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF450A0A).copy(alpha = 0.85f) else Color(0xFFFEF2F2).copy(alpha = 0.95f),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
          modifier = Modifier.fillMaxWidth().clickable {
            // Haritayı MOB noktasına odakla
            webViewRef?.evaluateJavascript("if(typeof centerMap === 'function') { centerMap(${uiState.mobEvent.latitude}, ${uiState.mobEvent.longitude}, 16); }", null)
          }
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "🚨 MOB (Denize adam düştü)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                  color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                )
              }
              Text(
                text = "Saat: ${uiState.mobEvent.timeFormatted}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Mesafe: ${String.format(java.util.Locale.US, "%.2f NM (%.1f Gom)", mobDistNm, mobDistGomina)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
              Text(
                text = "Kerteriz: ${String.format(java.util.Locale.US, "%03d°", mobBearing)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
              )
            }
          }
        }
      }

      if (uiState.anchorEvent.isAnchored) {
        val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.anchorEvent.latitude
        val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.anchorEvent.longitude
        val anchorDistMeters = uiState.anchorEvent.calculateDistanceMeters(currentLat, currentLon)
        val limitMeters = uiState.anchorEvent.safeRadiusMeters
        val isDragging = anchorDistMeters > limitMeters
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDragging) (if(isDark) Color(0xFF450A0A).copy(alpha = 0.85f) else Color(0xFFFEF2F2).copy(alpha=0.95f)) else (if(isDark) Color(0xFF064E3B).copy(alpha=0.85f) else Color(0xFFECFDF5).copy(alpha=0.95f)),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDragging) Color(0xFFDC2626) else Color(0xFF10B981)),
          modifier = Modifier.fillMaxWidth().clickable {
            // Haritayı demir noktasına odakla
            webViewRef?.evaluateJavascript("if(typeof centerMap === 'function') { centerMap(${uiState.anchorEvent.latitude}, ${uiState.anchorEvent.longitude}, 16); }", null)
          }
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Anchor, contentDescription = null, tint = if(isDragging) Color(0xFFEF4444) else Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if(isDragging) "⚠️ DEMİR TARIYOR!" else "⚓ DEMİR NÖBETİ AKTİF",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                  color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
                )
              }
              Text(
                text = "Saat: ${uiState.anchorEvent.dropTimeFormatted}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Uzaklık: ${String.format(java.util.Locale.US, "%.1f", anchorDistMeters)}m",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
              Text(
                text = "Limit: ${String.format(java.util.Locale.US, "%.1f", limitMeters)}m",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                color = if(isDragging) (if(isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if(isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
            }
          }
        }
      }
    }

"""

pattern_insert = r"(\s*)// ══════════════════════════════════════════════════════════════════════\s*// SAĞ ALT KONTROL BUTONLARI"
content = re.sub(pattern_insert, "\n" + info_panels + r"\1// ══════════════════════════════════════════════════════════════════════\n\1// SAĞ ALT KONTROL BUTONLARI", content, 1)

# Now insert the FABs into the FAB column
buttons = """      // MOB Butonu
      ExtendedFloatingActionButton(
        onClick = {
          if (uiState.mobEvent.isActive) viewModel.cancelMob() else viewModel.triggerMob()
        },
        containerColor = if (uiState.mobEvent.isActive) Color.Black else Color(0xFFDC2626),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("fab_mob_toggle")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Warning, contentDescription = "MOB", modifier = Modifier.size(20.dp))
          Text(
            text = if (uiState.mobEvent.isActive) "MOB İptal" else "MOB",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
          )
        }
      }
      
      // Demirleme Butonu
      ExtendedFloatingActionButton(
        onClick = {
          if (uiState.anchorEvent.isAnchored) {
            viewModel.liftAnchor()
          } else {
            viewModel.applyAnchorCalculationToSwingingCircle()
            viewModel.dropAnchor()
          }
        },
        containerColor = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else Color(0xFFEAB308),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("fab_anchor_toggle")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Anchor, contentDescription = "Demir", modifier = Modifier.size(20.dp))
          Text(
            text = if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
          )
        }
      }
      """

pattern_fabs = r"(      // 1\. AIS Butonu - MMSI/IMO Numaralı Geminin Konumuna Git)"
content = re.sub(pattern_fabs, buttons + r"\n\1", content, 1)

with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'w') as f:
    f.write(content)
