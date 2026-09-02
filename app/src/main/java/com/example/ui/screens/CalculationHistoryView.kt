package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnchorCalculationRecord
import com.example.data.TideCalculationRecord
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationHistoryView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToAnchor: () -> Unit = {},
  onNavigateToTide: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isDarkMode = uiState.isDarkMode
  var selectedSubTab by remember { mutableStateOf(0) } // 0: Demirleme, 1: Gelgit
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // ══════════════════════════════════════════════════════════════
    // 1. BAŞLIK VE GEÇMİŞ KONTROLLERİ
    // ══════════════════════════════════════════════════════════════
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = cardBg,
      shadowElevation = 2.dp,
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Hesaplama Geçmişi (Room DB)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
              color = textPrimary
            )
            Text(
              text = "Kayıtlı Demirleme ve Gelgit Verileri",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = textMuted
            )
          }
        }

        // Temizle Butonu
        val totalCount = if (selectedSubTab == 0) uiState.anchorHistory.size else uiState.tideHistory.size
        if (totalCount > 0) {
          IconButton(
            onClick = { showClearConfirmDialog = true },
            modifier = Modifier.testTag("btn_clear_history")
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteSweep,
              contentDescription = "Geçmişi Temizle",
              tint = DangerRed
            )
          }
        }
      }
    }

    // Başarı / Bilgi Mesajı
    AnimatedVisibility(visible = uiState.saveSuccessMessage != null) {
      uiState.saveSuccessMessage?.let { msg ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5),
          border = androidx.compose.foundation.BorderStroke(1.dp, SeaGreenBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (isDarkMode) SeaGreen else Color(0xFF059669), modifier = Modifier.size(18.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = textPrimary
              )
            }
            IconButton(
              onClick = { viewModel.clearSaveSuccessMessage() },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = null, tint = textMuted, modifier = Modifier.size(14.dp))
            }
          }
        }
      }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. SEKME SEÇİCİ (DEMİRLEME / GELGİT)
    // ══════════════════════════════════════════════════════════════
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Demirleme Geçmişi Sekmesi
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selectedSubTab == 0) PrimaryBlue else cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedSubTab == 0) PrimaryBlue else cardBorder),
        modifier = Modifier
          .weight(1f)
          .clickable { selectedSubTab = 0 }
          .testTag("tab_history_anchor")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Anchor,
            contentDescription = null,
            tint = if (selectedSubTab == 0) Color.White else (if (isDarkMode) MarineCyan else PrimaryBlue),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Demirleme (${uiState.anchorHistory.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selectedSubTab == 0) Color.White else textPrimary
          )
        }
      }

      // Gelgit Geçmişi Sekmesi
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selectedSubTab == 1) PrimaryBlue else cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedSubTab == 1) PrimaryBlue else cardBorder),
        modifier = Modifier
          .weight(1f)
          .clickable { selectedSubTab = 1 }
          .testTag("tab_history_tide")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Water,
            contentDescription = null,
            tint = if (selectedSubTab == 1) Color.White else (if (isDarkMode) MarineCyan else PrimaryBlue),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Gelgit & UKC (${uiState.tideHistory.size})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selectedSubTab == 1) Color.White else textPrimary
          )
        }
      }
    }

    // ══════════════════════════════════════════════════════════════
    // 3. LİSTE İÇERİĞİ
    // ══════════════════════════════════════════════════════════════
    if (selectedSubTab == 0) {
      // DEMİRLEME GEÇMİŞİ LİSTESİ
      if (uiState.anchorHistory.isEmpty()) {
        EmptyHistoryCard(
          icon = Icons.Default.Anchor,
          title = "Kayıtlı Demirleme Hesabı Yok",
          description = "Demirleme & Kaloma ekranında yaptığınız hesaplamaları '💾 Veritabanına Kaydet' butonu ile buraya kaydedebilirsiniz.",
          buttonText = "⚓ Demirleme Ekranına Git",
          isDarkMode = isDarkMode,
          onButtonClick = onNavigateToAnchor
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 20.dp)
        ) {
          items(uiState.anchorHistory, key = { it.id }) { record ->
            AnchorHistoryItemCard(
              record = record,
              isDarkMode = isDarkMode,
              onLoad = {
                viewModel.loadAnchorRecordIntoForm(record)
                onNavigateToAnchor()
              },
              onDelete = { viewModel.deleteAnchorRecord(record.id) }
            )
          }
        }
      }
    } else {
      // GELGİT GEÇMİŞİ LİSTESİ
      if (uiState.tideHistory.isEmpty()) {
        EmptyHistoryCard(
          icon = Icons.Default.Water,
          title = "Kayıtlı Gelgit Hesabı Yok",
          description = "Gelgit & UKC Pencereleri ekranından yaptığınız seyrüsefer analizlerini buraya kaydedebilirsiniz.",
          buttonText = "🌊 Gelgit Analizine Git",
          isDarkMode = isDarkMode,
          onButtonClick = onNavigateToTide
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 20.dp)
        ) {
          items(uiState.tideHistory, key = { it.id }) { record ->
            TideHistoryItemCard(
              record = record,
              isDarkMode = isDarkMode,
              onLoad = {
                viewModel.loadTideRecordIntoForm(record)
                onNavigateToTide()
              },
              onDelete = { viewModel.deleteTideRecord(record.id) }
            )
          }
        }
      }
    }
  }

  // Temizleme Onay Dialogu
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed) },
      title = {
        Text(
          text = if (selectedSubTab == 0) "Demirleme Geçmişi Temizlensin mi?" else "Gelgit Geçmişi Temizlensin mi?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = textPrimary
        )
      },
      text = {
        Text(
          text = "Tüm kayıtlı ${if (selectedSubTab == 0) "demirleme" else "gelgit"} hesaplama geçmişi kalıcı olarak silinecektir. Emin misiniz?",
          style = MaterialTheme.typography.bodyMedium,
          color = textPrimary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (selectedSubTab == 0) viewModel.clearAnchorHistory() else viewModel.clearTideHistory()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
        ) {
          Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showClearConfirmDialog = false }) {
          Text("İptal")
        }
      }
    )
  }
}

/**
 * Demirleme Geçmişi Tekil Kartı
 */
@Composable
private fun AnchorHistoryItemCard(
  record: AnchorCalculationRecord,
  isDarkMode: Boolean,
  onLoad: () -> Unit,
  onDelete: () -> Unit
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = cardBg,
    shadowElevation = 2.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Üst Başlık: Gemi Adı + Standart + Tarih + Sil
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (record.vesselName.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE)
            ) {
              Text(
                text = record.vesselName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                color = if (isDarkMode) MarineCyan else PrimaryBlueDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = subtleBg
          ) {
            Text(
              text = "1 Kilit = ${String.format(Locale.US, "%.1f", record.metersPerShackle)}m",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = if (isDarkMode) MarineCyan else PrimaryBlue,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = record.formattedDate,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textMuted
          )
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = DangerRed, modifier = Modifier.size(16.dp))
          }
        }
      }

      HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

      // a, b, c Değerleri
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("a = Kaloma / Kilit", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "${String.format(Locale.US, "%.1f", record.chainScopeMeters)} m (${String.format(Locale.US, "%.1f", record.shacklesCount)} Kilit)",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, color = textPrimary)
          )
        }

        Column {
          Text("b = Derinlik", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "${String.format(Locale.US, "%.1f", record.depthMeters)} m (${String.format(Locale.US, "%.1f", record.depthMeters / 1.8288)} Ku)",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = textPrimary)
          )
        }

        Column {
          Text("c = Yatay Mesafe", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "${String.format(Locale.US, "%.1f", record.horizontalDistanceMeters)} m",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (isDarkMode) MarineCyan else PrimaryBlue)
          )
        }
      }

      // Salma Daireleri Özeti (d, e, f)
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = subtleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "d (1. Salma): ${String.format(Locale.US, "%.1f", record.firstSwingingRadiusMeters)} m",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = textPrimary)
          )
          Text(
            text = "f (2. Salma): ${String.format(Locale.US, "%.1f", record.secondSwingingRadiusTotalMeters)} m",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, color = if (isDarkMode) WarningAmber else Color(0xFFD97706))
          )
        }
      }

      // Alt Buton: Forma Aktar
      OutlinedButton(
        onClick = onLoad,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) MarineCyan else PrimaryBlue),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) MarineCyan else PrimaryBlue),
        modifier = Modifier
          .fillMaxWidth()
          .height(34.dp)
      ) {
        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDarkMode) MarineCyan else PrimaryBlue)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          "Bu Parametreleri Forma Yükle ve Hesapla",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isDarkMode) MarineCyan else PrimaryBlue)
        )
      }
    }
  }
}

/**
 * Gelgit Geçmişi Tekil Kartı
 */
@Composable
private fun TideHistoryItemCard(
  record: TideCalculationRecord,
  isDarkMode: Boolean,
  onLoad: () -> Unit,
  onDelete: () -> Unit
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val subtleBorder = getMarineSubtleBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = cardBg,
    shadowElevation = 2.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Üst Başlık: Liman Adı + Emniyet Rozeti + Tarih
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = record.portName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
            color = textPrimary
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (record.isCurrentlySafe) (if (isDarkMode) SeaGreenDark else Color(0xFFD1FAE5)) else (if (isDarkMode) DangerRedDark else Color(0xFFFEE2E2))
          ) {
            Text(
              text = if (record.isCurrentlySafe) "GÜVENLİ" else "DİKKAT",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                color = if (record.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = record.formattedDate,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textMuted
          )
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = DangerRed, modifier = Modifier.size(16.dp))
          }
        }
      }

      HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

      // Gelgit Değerleri Özeti
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Yüksek Su (HW)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "${record.highTideTime} • +${String.format(Locale.US, "%.2f", record.highTideHeightMeters)}m",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = textPrimary)
          )
        }

        Column {
          Text("Alçak Su (LW)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "${record.lowTideTime} • +${String.format(Locale.US, "%.2f", record.lowTideHeightMeters)}m",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = textPrimary)
          )
        }

        Column {
          Text("Anlık UKC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = textMuted))
          Text(
            "+${String.format(Locale.US, "%.2f", record.currentInstantUkcMeters)} m",
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Black,
              color = if (record.isCurrentlySafe) (if (isDarkMode) SeaGreen else Color(0xFF059669)) else DangerRed
            )
          )
        }
      }

      // Pencere Özeti
      if (record.safeWindowSummary.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Emniyet: ${record.safeWindowSummary}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = textPrimary),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
          )
        }
      }

      // Alt Buton: Forma Aktar
      OutlinedButton(
        onClick = onLoad,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) MarineCyan else PrimaryBlue),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) MarineCyan else PrimaryBlue),
        modifier = Modifier
          .fillMaxWidth()
          .height(34.dp)
      ) {
        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDarkMode) MarineCyan else PrimaryBlue)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          "Bu Gelgit Değerlerini Aç",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isDarkMode) MarineCyan else PrimaryBlue)
        )
      }
    }
  }
}

/**
 * Boş Geçmiş Kartı
 */
@Composable
private fun EmptyHistoryCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
  buttonText: String,
  isDarkMode: Boolean,
  onButtonClick: () -> Unit
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(if (isDarkMode) PrimaryBlueLight else Color(0xFFDBEAFE), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isDarkMode) MarineCyan else PrimaryBlueDark,
          modifier = Modifier.size(28.dp)
        )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = textPrimary
      )

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        color = textMuted,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Button(
        onClick = onButtonClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
      ) {
        Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}
