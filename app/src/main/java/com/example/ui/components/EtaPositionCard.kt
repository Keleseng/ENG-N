package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.model.Coordinate
import com.example.model.DmsTriple
import com.example.model.EtaSummaryReceipt
import com.example.model.SimpleDestination
import com.example.model.TurkishPorts
import com.example.model.decimalToDmsParts
import com.example.model.dmsToDecimal
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*

/**
 * Ana Menüye kibar şekilde sığdırılan ve bilinen şehir/ilçe limanlarını içeren
 * ETA / Mevki Girişi ve Seyir Hesabı Kartı.
 */
@Composable
fun EtaPositionCard(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onShowOnMap: (Coordinate) -> Unit,
  isDarkMode: Boolean = true,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  val activeDest = uiState.selectedSimpleEtaDestination

  // Önceden seçilmiş hedef varsa onun koordinatlarını, yoksa kullanıcının varsayılan mevkisini al
  var latDeg by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lat, true).degrees else "41"
    )
  }
  var latMin by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lat, true).minutes else "02"
    )
  }
  var latSec by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lat, true).seconds else "35.4"
    )
  }
  var latDir by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lat, true).direction else "Kuzey"
    )
  }

  var lonDeg by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lon, false).degrees else "029"
    )
  }
  var lonMin by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lon, false).minutes else "12"
    )
  }
  var lonSec by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lon, false).seconds else "18.2"
    )
  }
  var lonDir by remember(activeDest) {
    mutableStateOf(
      if (activeDest != null) decimalToDmsParts(activeDest.lon, false).direction else "Doğu"
    )
  }

  var selectedCity by remember(activeDest) {
    mutableStateOf(
      if (activeDest == null) "İstanbul"
      else TurkishPorts.regions.entries.find { it.value.any { port -> port.name == activeDest.name } }?.key ?: "İstanbul"
    )
  }
  var selectedPortName by remember(activeDest) {
    mutableStateOf(activeDest?.name)
  }

  var cityDropdownExpanded by remember { mutableStateOf(false) }
  var pierDropdownExpanded by remember { mutableStateOf(false) }
  var isManualInputVisible by remember { mutableStateOf(true) }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    modifier = modifier.fillMaxWidth().testTag("card_eta_position_entry")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      // 1. Kart Başlığı ve Hızlı Liman Seçimi Şeridi
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(0.42f)
        ) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = if (isDarkMode) MarineCyan else PrimaryBlue,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "ETA / MEVKİ GİRİŞİ",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 11.5.sp,
              letterSpacing = 0.3.sp
            ),
            color = if (isDarkMode) MarineCyan else PrimaryBlueDark
          )
        }

        // Hızlı Şehir & Liman Seçimi Dropdownları
        Row(
          modifier = Modifier.weight(0.58f),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Şehir Seçimi Dropdown
          Box {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(0.6.dp, cardBorder),
              modifier = Modifier.clickable { cityDropdownExpanded = true }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
              ) {
                Text(
                  text = selectedCity,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                  color = textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = textSecondary,
                  modifier = Modifier.size(13.dp)
                )
              }
            }

            DropdownMenu(
              expanded = cityDropdownExpanded,
              onDismissRequest = { cityDropdownExpanded = false },
              modifier = Modifier.background(cardBg).heightIn(max = 280.dp)
            ) {
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
                    cityDropdownExpanded = false
                    pierDropdownExpanded = true
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // İskele / Liman Seçimi Dropdown
          Box {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = subtleBg,
              border = androidx.compose.foundation.BorderStroke(0.6.dp, cardBorder),
              modifier = Modifier.clickable { pierDropdownExpanded = true }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
              ) {
                Text(
                  text = selectedPortName ?: "Liman Seç",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                  color = if (selectedPortName != null) textPrimary else textMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 85.dp)
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = null,
                  tint = textSecondary,
                  modifier = Modifier.size(13.dp)
                )
              }
            }

            DropdownMenu(
              expanded = pierDropdownExpanded,
              onDismissRequest = { pierDropdownExpanded = false },
              modifier = Modifier.background(cardBg).heightIn(max = 280.dp)
            ) {
              TurkishPorts.regions[selectedCity]?.forEach { port ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = port.name,
                      fontSize = 10.5.sp,
                      color = textPrimary,
                      fontWeight = if (selectedPortName == port.name) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    selectedPortName = port.name
                    // Liman koordinatlarını DMS kutularına doldur
                    val latParts = decimalToDmsParts(port.lat, true)
                    val lonParts = decimalToDmsParts(port.lon, false)
                    latDeg = latParts.degrees
                    latMin = latParts.minutes
                    latSec = latParts.seconds
                    latDir = latParts.direction
                    lonDeg = lonParts.degrees
                    lonMin = lonParts.minutes
                    lonSec = lonParts.seconds
                    lonDir = lonParts.direction

                    viewModel.calculateAndSetEta(Coordinate(port.lat, port.lon), port.name)
                    pierDropdownExpanded = false
                  }
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = cardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

      // 2. VARIŞ MEVKİİ VE ELLE GİRİŞ BAŞLIĞI / AÇ-KAPA (GİZLENEBİLİR MENÜ)
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = subtleBg,
        border = BorderStroke(0.7.dp, cardBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isManualInputVisible = !isManualInputVisible }
          .testTag("btn_toggle_manual_dms")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Icon(
              imageVector = if (isManualInputVisible) Icons.Default.EditLocation else Icons.Default.EditLocationAlt,
              contentDescription = null,
              tint = if (isDarkMode) MarineYellow else Color(0xFFD97706),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (selectedPortName != null) "Mevki: $selectedPortName" else "Varış Mevkii (Elle Giriş)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
              ),
              color = if (isDarkMode) MarineYellow else Color(0xFF92400E),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
          ) {
            if (!isManualInputVisible) {
              Text(
                text = "${latDeg}°${latMin}'${latDir} ${lonDeg}°${lonMin}'${lonDir}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
              )
              Spacer(modifier = Modifier.width(4.dp))
              // Kapalı durumdayken hızlı işlem butonları
              Button(
                onClick = {
                  val latitude = dmsToDecimal(
                    latDeg.toIntOrNull() ?: 0,
                    latMin.toIntOrNull() ?: 0,
                    latSec.toDoubleOrNull() ?: 0.0,
                    latDir.firstOrNull() ?: 'K'
                  )
                  val longitude = dmsToDecimal(
                    lonDeg.toIntOrNull() ?: 0,
                    lonMin.toIntOrNull() ?: 0,
                    lonSec.toDoubleOrNull() ?: 0.0,
                    lonDir.firstOrNull() ?: 'D'
                  )
                  val coord = Coordinate(latitude, longitude)
                  viewModel.calculateAndSetEta(coord, selectedPortName)
                },
                modifier = Modifier.height(26.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDarkMode) SeaGreen else Color(0xFF059669),
                  contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
              ) {
                Text("ETA", fontSize = 9.sp, fontWeight = FontWeight.Black)
              }
              Spacer(modifier = Modifier.width(3.dp))
              OutlinedButton(
                onClick = {
                  val latitude = dmsToDecimal(
                    latDeg.toIntOrNull() ?: 0,
                    latMin.toIntOrNull() ?: 0,
                    latSec.toDoubleOrNull() ?: 0.0,
                    latDir.firstOrNull() ?: 'K'
                  )
                  val longitude = dmsToDecimal(
                    lonDeg.toIntOrNull() ?: 0,
                    lonMin.toIntOrNull() ?: 0,
                    lonSec.toDoubleOrNull() ?: 0.0,
                    lonDir.firstOrNull() ?: 'D'
                  )
                  val coord = Coordinate(latitude, longitude)
                  viewModel.calculateAndSetEta(coord, selectedPortName)
                  onShowOnMap(coord)
                },
                modifier = Modifier.height(26.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (isDarkMode) Color(0xFF0369A1).copy(alpha = 0.2f) else Color(0xFFE0F2FE),
                  contentColor = if (isDarkMode) MarineCyan else PrimaryBlueDark
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
              ) {
                Text("Harita", fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.width(3.dp))
            } else {
              Text(
                text = "Gizle",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
              )
              Spacer(modifier = Modifier.width(3.dp))
            }
            Icon(
              imageVector = if (isManualInputVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = if (isManualInputVisible) "Gizle" else "Genişlet",
              tint = textSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Gizlenebilir DMS Koordinat Giriş Alanı ve Sonuçlar
      AnimatedVisibility(
        visible = isManualInputVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        val receipt = uiState.etaSummaryReceipt
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Sol Blok: ENLEM ve BOYLAM Giriş Pencereleri
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // ENLEM Girişi
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "ENLEM",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                  color = textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                DmsRow(
                  degrees = latDeg, onDegreesChange = { if (it.length <= 2 && it.all(Char::isDigit)) latDeg = it },
                  minutes = latMin, onMinutesChange = { if (it.length <= 2 && it.all(Char::isDigit)) latMin = it },
                  seconds = latSec, onSecondsChange = { if (it.length <= 4 && it.matches(Regex("""\d{0,2}(\.\d{0,1})?"""))) latSec = it },
                  direction = latDir, onDirectionChange = { latDir = it },
                  directions = listOf("Kuzey", "Güney"), isDarkMode = isDarkMode
                )
              }
              // BOYLAM Girişi
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "BOYLAM",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                  color = textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                DmsRow(
                  degrees = lonDeg, onDegreesChange = { if (it.length <= 3 && it.all(Char::isDigit)) lonDeg = it },
                  minutes = lonMin, onMinutesChange = { if (it.length <= 2 && it.all(Char::isDigit)) lonMin = it },
                  seconds = lonSec, onSecondsChange = { if (it.length <= 4 && it.matches(Regex("""\d{0,2}(\.\d{0,1})?"""))) lonSec = it },
                  direction = lonDir, onDirectionChange = { lonDir = it },
                  directions = listOf("Doğu", "Batı"), isDarkMode = isDarkMode
                )
              }
            }
            
            // Sağ Blok: Butonlar
            Column(
              modifier = Modifier.width(122.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Button(
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_calculate_eta"),
                onClick = {
                  val latitude = dmsToDecimal(latDeg.toIntOrNull() ?: 0, latMin.toIntOrNull() ?: 0, latSec.toDoubleOrNull() ?: 0.0, latDir.firstOrNull() ?: 'K')
                  val longitude = dmsToDecimal(lonDeg.toIntOrNull() ?: 0, lonMin.toIntOrNull() ?: 0, lonSec.toDoubleOrNull() ?: 0.0, lonDir.firstOrNull() ?: 'D')
                  val coord = Coordinate(latitude, longitude)
                  viewModel.calculateAndSetEta(coord, selectedPortName)
                },
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) SeaGreen else Color(0xFF059669), contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                  Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("ETA HESAPLA", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
              }
              OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_show_on_map"),
                onClick = {
                  val latitude = dmsToDecimal(latDeg.toIntOrNull() ?: 0, latMin.toIntOrNull() ?: 0, latSec.toDoubleOrNull() ?: 0.0, latDir.firstOrNull() ?: 'K')
                  val longitude = dmsToDecimal(lonDeg.toIntOrNull() ?: 0, lonMin.toIntOrNull() ?: 0, lonSec.toDoubleOrNull() ?: 0.0, lonDir.firstOrNull() ?: 'D')
                  val coord = Coordinate(latitude, longitude)
                  viewModel.calculateAndSetEta(coord, selectedPortName)
                  onShowOnMap(coord)
                },
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isDarkMode) Color(0xFF0369A1).copy(alpha = 0.2f) else Color(0xFFE0F2FE), contentColor = if (isDarkMode) MarineCyan else PrimaryBlueDark),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                  Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("HARİTADA GÖSTER", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // Ayrı Pencerede Sonuç Paneli (GPS hizalamasının benzeri)
          if (receipt != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isDarkMode) Color(0xFF030712) else Color(0xFFF1F5F9),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
              modifier = Modifier.fillMaxWidth().testTag("eta_receipt_panel")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Sol Taraf: Koordinatlar & Hedef
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = if (!receipt.targetName.isNullOrBlank() && receipt.targetName != "Varış Mevkii") "VARIŞ: ${receipt.targetName}" else "VARIŞ MEVKİİ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = if (isDarkMode) MarineCyan else PrimaryBlueDark
                  )
                  Text(
                    text = "${receipt.latDms}\n${receipt.lonDms}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = textSecondary,
                    lineHeight = 14.sp
                  )
                }

                // Sağ Taraf: Detaylar (Mesafe, Kerteriz, Sürat, ETA)
                Column(
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                  horizontalAlignment = Alignment.End
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MESAFE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("${String.format(Locale.US, "%.1f", receipt.distanceNm)} NM", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KERTERİZ", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text(String.format(Locale.US, "%03d°", receipt.bearingDegrees), fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) MarineYellow else Color(0xFFD97706))
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SÜRAT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("${String.format(Locale.US, "%.1f", receipt.speedKnots)} KT", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ETA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669))
                    Text(receipt.etaTime, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Kullanıcı tarafından belirtilen ve tam ekran veya dialog olarak da kullanılabilen
 * EtaPositionScreen bileşeni.
 */
@Composable
fun EtaPositionScreen(
  onShowOnMap: (Coordinate) -> Unit,
  onCalculateEta: (Coordinate) -> Unit,
  modifier: Modifier = Modifier
) {
  var latDeg by remember { mutableStateOf("41") }
  var latMin by remember { mutableStateOf("02") }
  var latSec by remember { mutableStateOf("35.4") }
  var latDir by remember { mutableStateOf("Kuzey") }

  var lonDeg by remember { mutableStateOf("029") }
  var lonMin by remember { mutableStateOf("12") }
  var lonSec by remember { mutableStateOf("18.2") }
  var lonDir by remember { mutableStateOf("Doğu") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "ETA / MEVKİ GİRİŞİ",
      style = MaterialTheme.typography.headlineSmall
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "VARIŞ MEVKİİ",
      style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("ENLEM")

    DmsRow(
      degrees = latDeg,
      onDegreesChange = {
        if (it.length <= 2 && it.all(Char::isDigit))
          latDeg = it
      },
      minutes = latMin,
      onMinutesChange = {
        if (it.length <= 2 && it.all(Char::isDigit))
          latMin = it
      },
      seconds = latSec,
      onSecondsChange = {
        if (it.length <= 4 &&
          it.matches(Regex("""\d{0,2}(\.\d{0,1})?"""))
        )
          latSec = it
      },
      direction = latDir,
      onDirectionChange = {
        latDir = it
      },
      directions = listOf("Kuzey", "Güney")
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text("BOYLAM")

    DmsRow(
      degrees = lonDeg,
      onDegreesChange = {
        if (it.length <= 3 && it.all(Char::isDigit))
          lonDeg = it
      },
      minutes = lonMin,
      onMinutesChange = {
        if (it.length <= 2 && it.all(Char::isDigit))
          lonMin = it
      },
      seconds = lonSec,
      onSecondsChange = {
        if (it.length <= 4 &&
          it.matches(Regex("""\d{0,2}(\.\d{0,1})?"""))
        )
          lonSec = it
      },
      direction = lonDir,
      onDirectionChange = {
        lonDir = it
      },
      directions = listOf("Doğu", "Batı")
    )

    Spacer(modifier = Modifier.height(30.dp))

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = {
        val latitude = dmsToDecimal(
          latDeg.toIntOrNull() ?: 0,
          latMin.toIntOrNull() ?: 0,
          latSec.toDoubleOrNull() ?: 0.0,
          latDir.firstOrNull() ?: 'K'
        )

        val longitude = dmsToDecimal(
          lonDeg.toIntOrNull() ?: 0,
          lonMin.toIntOrNull() ?: 0,
          lonSec.toDoubleOrNull() ?: 0.0,
          lonDir.firstOrNull() ?: 'D'
        )

        onShowOnMap(
          Coordinate(latitude, longitude)
        )
      }
    ) {
      Text("HARİTADA GÖSTER")
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = {
        val latitude = dmsToDecimal(
          latDeg.toIntOrNull() ?: 0,
          latMin.toIntOrNull() ?: 0,
          latSec.toDoubleOrNull() ?: 0.0,
          latDir.firstOrNull() ?: 'K'
        )

        val longitude = dmsToDecimal(
          lonDeg.toIntOrNull() ?: 0,
          lonMin.toIntOrNull() ?: 0,
          lonSec.toDoubleOrNull() ?: 0.0,
          lonDir.firstOrNull() ?: 'D'
        )

        onCalculateEta(
          Coordinate(latitude, longitude)
        )
      }
    ) {
      Text("ETA HESAPLA")
    }
  }
}

/**
 * Derece, Dakika, Saniye ve Yön (DMS) giriş satırı.
 */
@Composable
fun DmsRow(
  degrees: String,
  onDegreesChange: (String) -> Unit,
  minutes: String,
  onMinutesChange: (String) -> Unit,
  seconds: String,
  onSecondsChange: (String) -> Unit,
  direction: String,
  onDirectionChange: (String) -> Unit,
  directions: List<String>,
  isDarkMode: Boolean = true
) {
  Row(
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Derece kutucuğu
    NumberBox(
      value = degrees,
      onValueChange = onDegreesChange,
      modifier = Modifier.width(38.dp),
      isDarkMode = isDarkMode
    )

    Spacer(modifier = Modifier.width(1.dp))
    Text("°", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color.Black)
    Spacer(modifier = Modifier.width(2.dp))

    // Dakika kutucuğu
    NumberBox(
      value = minutes,
      onValueChange = onMinutesChange,
      modifier = Modifier.width(34.dp),
      isDarkMode = isDarkMode
    )

    Spacer(modifier = Modifier.width(1.dp))
    Text("'", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color.Black)
    Spacer(modifier = Modifier.width(2.dp))

    // Saniye kutucuğu
    NumberBox(
      value = seconds,
      onValueChange = onSecondsChange,
      modifier = Modifier.width(44.dp),
      isDarkMode = isDarkMode
    )

    Spacer(modifier = Modifier.width(1.dp))
    Text("\"", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isDarkMode) Color.White else Color.Black)
    Spacer(modifier = Modifier.width(3.dp))

    var expanded by remember { mutableStateOf(false) }

    Box {
      Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF38BDF8) else PrimaryBlue),
        color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFEFF6FF),
        modifier = Modifier.height(34.dp).defaultMinSize(minWidth = 52.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = direction,
            fontWeight = FontWeight.Black,
            fontSize = 10.5.sp,
            maxLines = 1,
            color = if (isDarkMode) MarineCyan else PrimaryBlueDark
          )
          Spacer(modifier = Modifier.width(2.dp))
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
            modifier = Modifier.size(13.dp)
          )
        }
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
          expanded = false
        },
        modifier = Modifier.background(if (isDarkMode) Color(0xFF0F172A) else Color.White)
      ) {
        directions.forEach { dir ->
          DropdownMenuItem(
            text = { Text(dir, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black) },
            onClick = {
              onDirectionChange(dir)
              expanded = false
            }
          )
        }
      }
    }
  }
}

/**
 * Sayısal veri giriş kutucuğu.
 * BasicTextField kullanılarak dikey kırpılma engellenmiş,
 * sayılar kutucuğa tam ortalanmış ve okunaklı puntoya getirilmiştir.
 */
@Composable
fun NumberBox(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  isDarkMode: Boolean = true
) {
  val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
  val borderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
  val textColor = if (isDarkMode) MarineYellow else Color(0xFF1E293B)
  val cursorColor = if (isDarkMode) MarineYellow else Color(0xFFD97706)

  Box(
    modifier = modifier
      .height(34.dp)
      .background(bgColor, RoundedCornerShape(4.dp))
      .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center
  ) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      textStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        color = textColor
      ),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Decimal
      ),
      cursorBrush = SolidColor(cursorColor),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp)
    )
  }
}
