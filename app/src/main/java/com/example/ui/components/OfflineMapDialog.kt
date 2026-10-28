package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.MarineSeaRoute
import com.example.offline.OfflineDownloadState
import com.example.offline.OfflineMapTileManager
import com.example.offline.OfflineRegionPreset
import java.util.Locale

@Composable
fun OfflineMapDialog(
  isDark: Boolean,
  isDeviceOnline: Boolean,
  forceOfflineMode: Boolean,
  downloadState: OfflineDownloadState,
  activeRoute: MarineSeaRoute?,
  currentLat: Double,
  currentLon: Double,
  onToggleForceOffline: (Boolean) -> Unit,
  onDownloadActiveRoute: () -> Unit,
  onDownloadAroundCurrent: (Double) -> Unit,
  onDownloadRegionPreset: (OfflineRegionPreset) -> Unit,
  onCancelDownload: () -> Unit,
  onClearCache: () -> Unit,
  onStartNavigationNow: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedRadiusNm by remember { mutableStateOf(15.0) }

  val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
  val sectionBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
  val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
  val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

  val cachedMb = String.format(Locale.US, "%.1f", downloadState.cachedSizeBytes / (1024.0 * 1024.0))
  val isEffectiveOffline = forceOfflineMode || !isDeviceOnline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = cardBg,
      border = BorderStroke(1.5.dp, Color(0xFF196DFF)),
      shadowElevation = 18.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .heightIn(max = 640.dp)
        .testTag("dialog_offline_map_manager")
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF196DFF).copy(alpha = 0.16f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DownloadForOffline,
                contentDescription = null,
                tint = Color(0xFF196DFF),
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "Çevrimdışı Harita & Navigasyon",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = textPrimary
              )
              Text(
                text = if (isEffectiveOffline) "📴 İnternetsiz Çevrimdışı Mod Aktif" else "🟢 Çevrimiçi • Otomatik Önbellek Açık",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp
                ),
                color = if (isEffectiveOffline) Color(0xFFF59E0B) else Color(0xFF10B981)
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = textSecondary)
          }
        }

        // Offline Mode Switch Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = sectionBg,
          border = BorderStroke(
            1.dp,
            if (isEffectiveOffline) Color(0xFFF59E0B) else Color(0xFF38BDF8).copy(alpha = 0.35f)
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "✈️ İnternetsiz Çalışma Modu",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textPrimary
              )
              Text(
                text = "Açık denizde veri harcamadan indirilen harita, uydu GPS'i ve yerel rota motoruyla çalışır.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                color = textSecondary
              )
            }
            Switch(
              checked = forceOfflineMode,
              onCheckedChange = onToggleForceOffline,
              modifier = Modifier.testTag("switch_force_offline_map")
            )
          }
        }

        // Active Download Progress Banner
        if (downloadState.isDownloading) {
          val progressFrac = if (downloadState.totalTiles > 0) {
            (downloadState.completedTiles.toFloat() / downloadState.totalTiles.toFloat()).coerceIn(0f, 1f)
          } else 0f
          val pct = (progressFrac * 100f).toInt()
          val dlMb = String.format(Locale.US, "%.1f", downloadState.downloadedBytes / (1024.0 * 1024.0))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF196DFF).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF196DFF))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "⬇️ ${downloadState.taskTitle} (%$pct)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = Color(0xFF38BDF8)
                )
                TextButton(
                  onClick = onCancelDownload,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp).testTag("btn_cancel_offline_download")
                ) {
                  Text("Durdur", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
              LinearProgressIndicator(
                progress = { progressFrac },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(7.dp),
                color = Color(0xFF196DFF),
                trackColor = Color.Gray.copy(alpha = 0.25f)
              )
              Text(
                text = "${downloadState.completedTiles} / ${downloadState.totalTiles} harita karosu • $dlMb MB kaydedildi",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = textSecondary
              )
            }
          }
        } else if (!downloadState.statusMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF10B981).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
          ) {
            Text(
              text = downloadState.statusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
              color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // 1. Active Route Corridor Download
        val validRoute = activeRoute?.takeIf { !it.isRejectedLandTarget && it.pathCoordinates.size >= 2 }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = sectionBg,
          border = BorderStroke(1.dp, Color(0xFF196DFF).copy(alpha = 0.45f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF196DFF), modifier = Modifier.size(18.dp))
              Text(
                text = "1. Aktif Rota Koridorunu İndir",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = textPrimary
              )
            }
            Text(
              text = if (validRoute != null) {
                "Seçili \"${validRoute.destinationName}\" rotası boyunca (±3.5 NM koridor, Zoom 7–14 + deniz feneri işaretleri) tüm haritayı çevrimdışı hafızaya indirir."
              } else {
                "Haritadan bir varış noktası seçtiğinizde tüm rota hattını tek dokunuşla çevrimdışı indirebilirsiniz."
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.5.sp),
              color = textSecondary
            )
            Button(
              onClick = onDownloadActiveRoute,
              enabled = validRoute != null && !downloadState.isDownloading && isDeviceOnline,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF196DFF)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_download_active_route_map")
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (validRoute != null) {
                  "Rota Haritasını İndir (${String.format(Locale.US, "%.1f", validRoute.totalDistanceNm)} NM)"
                } else {
                  "Önce Haritadan Rota Seçin"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // 2. Nearby Radius Download
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = sectionBg
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
              Text(
                text = "2. Bulunduğunuz Bölgeyi İndir",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = textPrimary
              )
            }
            Text(
              text = String.format(Locale.US, "Mevcut konum (%.3f°, %.3f°) çevresindeki deniz alanını indirir:", currentLat, currentLon),
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
              color = textSecondary
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(15.0 to "15 NM (Detaylı)", 35.0 to "35 NM (Orta)", 65.0 to "65 NM (Geniş)").forEach { (rad, label) ->
                FilterChip(
                  selected = selectedRadiusNm == rad,
                  onClick = { selectedRadiusNm = rad },
                  label = { Text(label, fontSize = 10.sp) },
                  modifier = Modifier.weight(1f)
                )
              }
            }
            OutlinedButton(
              onClick = { onDownloadAroundCurrent(selectedRadiusNm) },
              enabled = !downloadState.isDownloading && isDeviceOnline,
              border = BorderStroke(1.2.dp, Color(0xFF10B981)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_download_nearby_map")
            ) {
              Icon(Icons.Default.Downloading, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${selectedRadiusNm.toInt()} Deniz Mili Çevreyi İndir",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // 3. Preset Sea Region Packages
        Text(
          text = "3. HAZIR DENİZ BÖLGELERİ (TEK TUŞLA PAKET)",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
          color = textSecondary
        )

        OfflineMapTileManager.presetRegions.forEach { preset ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = sectionBg
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = preset.name,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = textPrimary
                )
                Text(
                  text = preset.subtitle,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                  color = textSecondary
                )
              }
              FilledTonalButton(
                onClick = { onDownloadRegionPreset(preset) },
                enabled = !downloadState.isDownloading && isDeviceOnline,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("btn_download_preset_${preset.id}")
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("İndir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.25f))

        // Cached Storage Info & Start Offline Navigation CTA
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Cihazdaki Harita Hafızası",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = textPrimary
            )
            Text(
              text = "${downloadState.cachedTileCount} karo kayıtlı ($cachedMb MB)",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
              color = textSecondary
            )
          }
          if (downloadState.cachedTileCount > 0) {
            TextButton(
              onClick = onClearCache,
              modifier = Modifier.testTag("btn_clear_offline_tiles")
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Temizle", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Button(
          onClick = onStartNavigationNow,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_start_offline_navigation")
        ) {
          Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Navigasyonu Şimdi Başlat (İnternetsiz GPS)",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}
