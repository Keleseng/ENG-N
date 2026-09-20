package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.TxtLogRecord
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*

@Composable
fun TxtLogManagerView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val isDark = uiState.isDarkMode
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var showSelectiveDeleteDialog by remember { mutableStateOf(false) }

  // Toast / Snackbar notification for log events
  LaunchedEffect(uiState.txtLogMessage) {
    uiState.txtLogMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.dismissTxtLogMessage()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // 1. ÜST KART: OTOMATİK 30 DK LOG KONTROL VE STATÜ (SADE VE KÜÇÜK)
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
      border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) PrimaryBlue.copy(alpha = 0.4f) else Color(0xFF93C5FD)),
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth().testTag("card_txt_log_status")
    ) {
      Column(
        modifier = Modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.TextSnippet,
              contentDescription = null,
              tint = if (isDark) MarineCyan else PrimaryBlue,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Otomatik TXT Log Kayıt Merkezi",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
              color = if (isDark) Color.White else Color(0xFF0F172A)
            )
          }

          // Otomatik Durum Rozeti
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SeaGreen.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, SeaGreen)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(SeaGreen, RoundedCornerShape(50))
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "30 DK OTOMATİK",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.sp),
                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
              )
            }
          }
        }

        // Bilgi Şeridi: Sonraki Kayıt Saati & Toplam Kayıt
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MarineYellow,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Sonraki Kayıt: ",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = getMarineTextMuted(isDark))
            )
            Text(
              text = if (uiState.nextAutoLogTimeStr.isNotBlank()) uiState.nextAutoLogTimeStr else "30 dk içerisinde",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = MarineYellow)
            )
          }

          Text(
            text = "Toplam: ${uiState.txtLogHistory.size} Log",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (isDark) MarineCyan else PrimaryBlue)
          )
        }

        // Aksiyon Butonları: Anlık TXT Kaydet & Kayıt Sil
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Button(
            onClick = { viewModel.performManualTxtLogSave() },
            colors = ButtonDefaults.buttonColors(
              containerColor = PrimaryBlue,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
            modifier = Modifier.weight(1f).height(32.dp).testTag("btn_manual_txt_log")
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Anlık TXT Log Kaydet",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
            )
          }

          Button(
            onClick = { showSelectiveDeleteDialog = true },
            enabled = uiState.txtLogHistory.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
              containerColor = DangerRed,
              contentColor = Color.White,
              disabledContainerColor = DangerRed.copy(alpha = 0.25f),
              disabledContentColor = Color.White.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
            modifier = Modifier.height(32.dp).testTag("btn_delete_txt_log")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Kayıt Sil",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
            )
          }

          if (uiState.txtLogHistory.isNotEmpty()) {
            OutlinedButton(
              onClick = { showClearConfirmDialog = true },
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
              border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Tümünü Sil", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 2. LOG DOSYALARI LİSTESİ
    Text(
      text = "Kayıtlı TXT Log Dosyaları",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
      color = if (isDark) Color.White else Color(0xFF0F172A),
      modifier = Modifier.padding(top = 2.dp, start = 2.dp)
    )

    if (uiState.txtLogHistory.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
          .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.TextSnippet,
            contentDescription = null,
            tint = getMarineTextMuted(isDark),
            modifier = Modifier.size(36.dp)
          )
          Text(
            text = "Henüz kaydedilmiş TXT log dosyası bulunmuyor.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = if (isDark) Color.White else Color(0xFF1E293B)
          )
          Text(
            text = "Uygulama otomatik olarak her 30 dakikada bir (saat başı ve :30'da) seyir telemetrinizi TXT formatında kaydedecektir. Dilerseniz 'Anlık TXT Log Kaydet' butonuna basarak hemen kayıt alabilirsiniz.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = getMarineTextMuted(isDark)),
            modifier = Modifier.padding(horizontal = 12.dp)
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(uiState.txtLogHistory, key = { it.id }) { record ->
          TxtLogItemCard(
            record = record,
            isDark = isDark,
            onView = { viewModel.selectTxtLogForView(record) },
            onCopy = {
              clipboardManager.setText(AnnotatedString(record.txtContent))
              Toast.makeText(context, "📋 Log metni panoya kopyalandı!", Toast.LENGTH_SHORT).show()
            },
            onShare = { shareTxtLogFile(context, record) },
            onDelete = { viewModel.deleteTxtLogRecord(record.filePath) }
          )
        }
      }
    }
  }

  // 3. TAM EKRAN KOD/TEXT VİEWER DİALOG
  uiState.selectedTxtLogForView?.let { record ->
    TxtLogViewerDialog(
      record = record,
      isDark = isDark,
      onDismiss = { viewModel.selectTxtLogForView(null) },
      onCopy = {
        clipboardManager.setText(AnnotatedString(record.txtContent))
        Toast.makeText(context, "📋 TXT Log metni kopyalandı!", Toast.LENGTH_SHORT).show()
      },
      onShare = { shareTxtLogFile(context, record) },
      onDelete = { viewModel.deleteTxtLogRecord(record.filePath) }
    )
  }

  // 4. TÜMÜNÜ SİLME ONAY DIALOGU
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed) },
      title = { Text("Tüm TXT Logları Silinsin mi?", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
      text = { Text("Disk üzerinde kayıtlı toplam ${uiState.txtLogHistory.size} adet TXT log dosyası kalıcı olarak silinecektir.", fontSize = 10.5.sp) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllTxtLogs()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
        ) {
          Text("Evet, Hepsini Sil", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("İptal", fontSize = 10.sp)
        }
      }
    )
  }

  // 5. SEÇMELİ LOG SİLME DIALOGU
  if (showSelectiveDeleteDialog) {
    SelectiveDeleteLogDialog(
      txtLogHistory = uiState.txtLogHistory,
      isDark = isDark,
      onDismiss = { showSelectiveDeleteDialog = false },
      onDeleteSelected = { filePaths ->
        viewModel.deleteTxtLogRecords(filePaths)
        showSelectiveDeleteDialog = false
      }
    )
  }
}

@Composable
private fun TxtLogItemCard(
  record: TxtLogRecord,
  isDark: Boolean,
  onView: () -> Unit,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
  val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = cardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = if (record.isAutoLog) SeaGreen else PrimaryBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = record.timestampFormatted,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
            color = if (isDark) Color.White else Color(0xFF0F172A)
          )
        }

        // Tipi Rozeti (Otomatik vs Manuel)
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (record.isAutoLog) SeaGreen.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (record.isAutoLog) SeaGreen else PrimaryBlue)
        ) {
          Text(
            text = if (record.isAutoLog) "⏰ 30 DK OTOMATİK" else "💾 MANUEL",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp),
            color = if (record.isAutoLog) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else (if (isDark) Color(0xFF93C5FD) else PrimaryBlue),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      // Dosya Adı ve Boyutu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = record.fileName,
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.5.sp),
          color = getMarineTextMuted(isDark)
        )
        Text(
          text = "${String.format("%.1f", record.fileSizeBytes / 1024.0)} KB",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
          color = getMarineTextMuted(isDark)
        )
      }

      // Snippet / Özet
      if (record.summarySnippet.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (isDark) Color(0xFF070D1E) else Color(0xFFF1F5F9)
        ) {
          Text(
            text = record.summarySnippet,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      // Buton Şeridi: Oku, Kopyala, Paylaş, Sil
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onView,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Oku", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        OutlinedButton(
          onClick = onCopy,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Kopyala", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        OutlinedButton(
          onClick = onShare,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Paylaş", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Sil", tint = DangerRed, modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@Composable
private fun TxtLogViewerDialog(
  record: TxtLogRecord,
  isDark: Boolean,
  onDismiss: () -> Unit,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = if (isDark) Color(0xFF0F172A) else Color.White,
      border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(10.dp)
      ) {
        // Başlık
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TextSnippet, contentDescription = null, tint = MarineCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = record.fileName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = if (isDark) Color.White else Color(0xFF0F172A)
              )
              Text(
                text = record.timestampFormatted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = getMarineTextMuted(isDark))
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = if (isDark) Color.White else Color.Black)
          }
        }

        Divider(color = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 6.dp))

        // TXT İçeriği Gösterim Alanı (Monospace, Scrollable)
        SelectionContainer(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(if (isDark) Color(0xFF070D1E) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(8.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
          ) {
            Text(
              text = record.txtContent,
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Alt Butonlar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              onDelete()
              onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sil", fontSize = 9.5.sp)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = onCopy,
              colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E3A8A) else PrimaryBlueLight)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Kopyala", fontSize = 9.5.sp)
            }

            Button(
              onClick = onShare,
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Paylaş / Dışa Aktar", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun shareTxtLogFile(context: Context, record: TxtLogRecord) {
  try {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TITLE, "TideNav Marine TXT Log - ${record.fileName}")
      putExtra(Intent.EXTRA_TEXT, record.txtContent)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "TXT Log Dosyasını Paylaş / Aktar")
    context.startActivity(shareIntent)
  } catch (e: Exception) {
    Toast.makeText(context, "Paylaşım başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
  }
}

@Composable
private fun SelectiveDeleteLogDialog(
  txtLogHistory: List<TxtLogRecord>,
  isDark: Boolean,
  onDismiss: () -> Unit,
  onDeleteSelected: (List<String>) -> Unit
) {
  var selectedFilePaths by remember { mutableStateOf(setOf<String>()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(0.92f),
    icon = {
      Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(28.dp))
    },
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Silinecek TXT Kayıtlarını Seçin",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
          color = if (isDark) Color.White else Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Silmek istediğiniz kayıtları seçin veya listeden doğrudan silin.",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = getMarineTextMuted(isDark))
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = {
              selectedFilePaths = if (selectedFilePaths.size == txtLogHistory.size) {
                emptySet()
              } else {
                txtLogHistory.map { it.filePath }.toSet()
              }
            },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (selectedFilePaths.size == txtLogHistory.size) "Seçimleri Kaldır" else "Tümünü Seç",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = PrimaryBlue
            )
          }

          Text(
            text = "${selectedFilePaths.size} / ${txtLogHistory.size} Seçili",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) MarineCyan else PrimaryBlue
          )
        }

        Divider(color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(txtLogHistory, key = { it.id }) { record ->
            val isChecked = selectedFilePaths.contains(record.filePath)
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isChecked) (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isChecked) PrimaryBlue else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedFilePaths = if (isChecked) {
                    selectedFilePaths - record.filePath
                  } else {
                    selectedFilePaths + record.filePath
                  }
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { checked ->
                    selectedFilePaths = if (checked) {
                      selectedFilePaths + record.filePath
                    } else {
                      selectedFilePaths - record.filePath
                    }
                  },
                  colors = CheckboxDefaults.colors(checkedColor = DangerRed)
                )

                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                ) {
                  Text(
                    text = record.timestampFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                  )
                  Text(
                    text = "${if (record.isAutoLog) "30 Dk Otomatik" else "Anlık Manuel"} • ${record.fileName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = getMarineTextMuted(isDark))
                  )
                }

                IconButton(
                  onClick = {
                    onDeleteSelected(listOf(record.filePath))
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Tek Kaydı Sil",
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDeleteSelected(selectedFilePaths.toList())
        },
        enabled = selectedFilePaths.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
          containerColor = DangerRed,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Seçilenleri Sil (${selectedFilePaths.size})",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal", fontSize = 11.sp)
      }
    }
  )
}
