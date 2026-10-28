package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isDark = uiState.isDarkMode
  val dialogBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val subtleBg = getMarineSubtleBg(isDark)
  val subtleBorder = getMarineSubtleBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = dialogBg,
      border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
      shadowElevation = 16.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
        .testTag("voice_settings_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // BAŞLIK
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = "Ses Ayarları",
              tint = PrimaryBlue,
              modifier = Modifier.size(24.dp)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Anons & Kadın Ses Ayarları",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textPrimary
              )
            )
            Text(
              text = "Sesli rehber ve Türkçe kadın ses tonu kontrolleri",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = textSecondary
              )
            )
          }
        }

        HorizontalDivider(color = subtleBorder, thickness = 1.dp)

        // 1. SESLİ REHBER AÇIK/KAPALI TOGGLE
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = subtleBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = if (uiState.isVoiceGuidanceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (uiState.isVoiceGuidanceEnabled) SeaGreen else DangerRed,
                modifier = Modifier.size(22.dp)
              )
              Column {
                Text(
                  text = "Sesli Anons Rehberi",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontSize = 13.5.sp
                  )
                )
                Text(
                  text = if (uiState.isVoiceGuidanceEnabled) "Sesli yönlendirme aktif" else "Tüm sesli anonslar sessizde",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (uiState.isVoiceGuidanceEnabled) SeaGreen else DangerRed,
                    fontSize = 11.sp
                  )
                )
              }
            }
            Switch(
              checked = uiState.isVoiceGuidanceEnabled,
              onCheckedChange = { viewModel.toggleVoiceGuidance() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SeaGreen
              )
            )
          }
        }

        // 2. KADIN SES TONU TİZLİĞİ (PITCH) SLIDER
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "👩 Kadın Ses Tizliği (Pitch)",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 13.sp
              )
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PrimaryBlue.copy(alpha = 0.2f)
            ) {
              Text(
                text = String.format(Locale.US, "%.2fx %s", uiState.ttsPitch, if (uiState.ttsPitch >= 1.25f) "(Kadın Tonu)" else "(Normal)"),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = PrimaryBlue
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
          Slider(
            value = uiState.ttsPitch,
            onValueChange = { viewModel.setTtsPitch(it) },
            valueRange = 0.8f..1.8f,
            steps = 10,
            modifier = Modifier.testTag("slider_tts_pitch")
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "1.0x (Normal)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = textSecondary),
              modifier = Modifier.clickable { viewModel.setTtsPitch(1.0f) }
            )
            Text(
              text = "✨ 1.32x (Kadın Ses Önerilen)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SeaGreen),
              modifier = Modifier.clickable { viewModel.setTtsPitch(1.32f) }
            )
            Text(
              text = "1.50x (İnce)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = textSecondary),
              modifier = Modifier.clickable { viewModel.setTtsPitch(1.50f) }
            )
          }
        }

        // 3. KONUŞMA HIZI (SPEECH RATE) SLIDER
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "🗣️ Konuşma Hızı",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 13.sp
              )
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PrimaryBlue.copy(alpha = 0.2f)
            ) {
              Text(
                text = String.format(Locale.US, "%.2fx", uiState.ttsSpeechRate),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = PrimaryBlue
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
          Slider(
            value = uiState.ttsSpeechRate,
            onValueChange = { viewModel.setTtsSpeechRate(it) },
            valueRange = 0.7f..1.5f,
            steps = 8,
            modifier = Modifier.testTag("slider_tts_rate")
          )
        }

        // 4. SESLİ ANONSU DİNLE / TEST ET
        Button(
          onClick = { viewModel.testVoiceAnnouncement() },
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("btn_test_voice"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            contentColor = Color.White
          )
        ) {
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "🔊 Sesli Anonsu Dinle (Test Et)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
          )
        }

        // 5. ANDROID SİSTEM SES / TTS AYARLARINI AÇ
        OutlinedButton(
          onClick = { viewModel.openSystemTtsSettings(context) },
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("btn_open_system_tts"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDark) MarineYellowBold else Color(0xFFD97706)
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MarineYellow else Color(0xFFD97706))
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "⚙️ Android Sistem Ses Ayarlarını Aç",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }

        // 6. BİLGİLENDİRME KUTUSU
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PrimaryBlue.copy(alpha = 0.1f),
          border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "💡 Telefonunuzda Ses Halen Erkek Çıkıyorsa:",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = textPrimary
              )
            )
            Text(
              text = "Yukarıdaki 'Android Sistem Ses Ayarlarını Aç' butonuna basarak Google Metin Okuma (TTS) ayarlarından 'Türkçe (Kadın)' ses paketini varsayılan yapabilirsiniz. Uygulamamız cihazdaki kadın ses tonlarını otomatik olarak önceliklendirir.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.5.sp,
                color = textSecondary,
                lineHeight = 15.sp
              )
            )
          }
        }

        // TAMAM / KAPAT
        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.End)
            .testTag("btn_close_voice_settings")
        ) {
          Text(
            text = "Kapat",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PrimaryBlue
          )
        }
      }
    }
  }
}
