package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimpleDestination
import com.example.model.SimpleEtaResult
import com.example.model.TurkishPorts
import com.example.ui.theme.*

@Composable
fun CompactEtaCard(
  selectedDestination: SimpleDestination?,
  etaResult: SimpleEtaResult?,
  onDestinationSelected: (SimpleDestination?) -> Unit,
  isDarkMode: Boolean = true,
  modifier: Modifier = Modifier
) {
  var cityExpanded by remember { mutableStateOf(false) }
  var pierExpanded by remember { mutableStateOf(false) }

  var manualLat by remember { mutableStateOf("") }
  var manualLon by remember { mutableStateOf("") }

  var selectedCity by remember(selectedDestination) {
    mutableStateOf(
      if (selectedDestination == null) null
      else TurkishPorts.regions.entries.find { it.value.any { port -> port.name == selectedDestination.name } }?.key
    )
  }

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)
  val accentColor = if (isDarkMode) MarineCyan else PrimaryBlueDark

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.35f)) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "ETA",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = textPrimary
          )
        }

        Row(
          modifier = Modifier.weight(0.65f),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // City Dropdown
          Box {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
              modifier = Modifier.clickable { cityExpanded = true }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              ) {
                Text(
                  text = selectedCity ?: "Şehir",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = if (selectedCity != null) textPrimary else textMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = textMuted,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            
            DropdownMenu(
              expanded = cityExpanded,
              onDismissRequest = { cityExpanded = false },
              modifier = Modifier.background(cardBg).heightIn(max = 300.dp)
            ) {
              DropdownMenuItem(
                text = { Text("Temizle", fontSize = 11.sp, color = textMuted) },
                onClick = {
                  selectedCity = null
                  onDestinationSelected(null)
                  cityExpanded = false
                  manualLat = ""
                  manualLon = ""
                }
              )
              HorizontalDivider(color = cardBorder)
              TurkishPorts.regions.keys.forEach { city ->
                DropdownMenuItem(
                  text = { 
                    Text(
                      text = city, 
                      fontSize = 11.sp, 
                      color = textPrimary,
                      fontWeight = if (selectedCity == city) FontWeight.Bold else FontWeight.Normal
                    ) 
                  },
                  onClick = {
                    selectedCity = city
                    onDestinationSelected(null)
                    cityExpanded = false
                    pierExpanded = true
                    manualLat = ""
                    manualLon = ""
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Pier Dropdown
          Box {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
              modifier = Modifier.clickable { 
                if (selectedCity != null) pierExpanded = true 
              }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              ) {
                Text(
                  text = selectedDestination?.name ?: "İskele/Liman",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = if (selectedDestination != null) textPrimary else textMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = textMuted,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            
            DropdownMenu(
              expanded = pierExpanded,
              onDismissRequest = { pierExpanded = false },
              modifier = Modifier.background(cardBg).heightIn(max = 300.dp)
            ) {
              if (selectedCity != null) {
                TurkishPorts.regions[selectedCity]?.forEach { port ->
                  DropdownMenuItem(
                    text = { 
                      Text(
                        text = port.name, 
                        fontSize = 11.sp, 
                        color = textPrimary,
                        fontWeight = if (selectedDestination?.name == port.name) FontWeight.Bold else FontWeight.Normal
                      ) 
                    },
                    onClick = {
                      onDestinationSelected(port)
                      pierExpanded = false
                      manualLat = ""
                      manualLon = ""
                    }
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "GPS:",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
          color = textMuted,
          modifier = Modifier.weight(0.15f)
        )
        Row(
          modifier = Modifier.weight(0.85f),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          BasicTextField(
            value = manualLat,
            onValueChange = { 
              manualLat = it
              val lat = parseCoordinate(it)
              val lon = parseCoordinate(manualLon)
              if (lat != null && lon != null) {
                onDestinationSelected(SimpleDestination("Manuel: ${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lon)}", lat, lon))
              }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textPrimary),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .background(subtleBg, RoundedCornerShape(4.dp))
              .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 4.dp),
            decorationBox = { innerTextField ->
              if (manualLat.isEmpty()) Text("Enlem (41°00'49\"K)", fontSize = 9.sp, color = textMuted, maxLines = 1)
              innerTextField()
            }
          )
          
          BasicTextField(
            value = manualLon,
            onValueChange = { 
              manualLon = it
              val lat = parseCoordinate(manualLat)
              val lon = parseCoordinate(it)
              if (lat != null && lon != null) {
                onDestinationSelected(SimpleDestination("Manuel: ${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lon)}", lat, lon))
              }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = textPrimary),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .background(subtleBg, RoundedCornerShape(4.dp))
              .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 4.dp),
            decorationBox = { innerTextField ->
              if (manualLon.isEmpty()) Text("Boylam (28°58'33\"D)", fontSize = 9.sp, color = textMuted, maxLines = 1)
              innerTextField()
            }
          )
        }
      }

      if (etaResult != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isDarkMode) PrimaryBlueLight.copy(alpha=0.3f) else Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueBorder.copy(alpha=0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Mesafe: ${etaResult.distanceNm} NM",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                color = textPrimary
              )
              Text(
                text = "Seyir Süresi: ${etaResult.durationStr} (${etaResult.speedKnots} kn)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = textMuted
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = if (isDarkMode) SeaGreen else Color(0xFF059669),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "ETA",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                  color = if (isDarkMode) SeaGreen else Color(0xFF059669)
                )
              }
              Text(
                text = etaResult.etaStr,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                color = textPrimary
              )
            }
          }
        }
      }
    }
  }
}

private fun parseCoordinate(input: String): Double? {
  // Try DMS format (e.g., 41°00'49"K or 28° 58' 33" D)
  val dmsRegex = Regex("""(\d+)[°\s]+(\d+)['\s]+([\d.]+)["]\s*([KkGgDdBbNnSsEeWw])""")
  val match = dmsRegex.find(input)
  if (match != null) {
    val degrees = match.groupValues[1].toDoubleOrNull() ?: 0.0
    val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
    val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
    val direction = match.groupValues[4].uppercase()
    
    var decimal = degrees + (minutes / 60.0) + (seconds / 3600.0)
    if (direction == "G" || direction == "S" || direction == "B" || direction == "W") {
      decimal *= -1.0
    }
    return decimal
  }
  
  // Fallback to plain decimal
  return input.trim().replace(',', '.').toDoubleOrNull()
}
