package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AnchorCalculationEngine
import com.example.model.*
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Denizcilik Demirleme & Salma Dairesi Hesabı Kartı
 *
 * Ders Kitabı Formül ve Çözüm Sistemi:
 * a² = b² + c²
 *
 * a = Gemi zincirine verilen kaloma (metre / kilit)
 * b = Derinlik (metre)
 * c = Loçadan demir yerine olan yatay mesafe [c = √(a² - b²)]
 * d = Köprüüstünden loçaya olan mesafe + Loçadan demir yerine olan yatay mesafe (I. Salma Dairesi için)
 * e = Köprüüstünden kıça kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi için)
 * f = Gemi Boyu kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi için)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorCalculationCard(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToMap: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showAdvancedInputs by rememberSaveable { mutableStateOf(false) }

  val isDark = uiState.isDarkMode
  val result = uiState.anchorCalculationResult

  val cardBg = getMarineCardBg(isDark)
  val cardBorder = getMarineCardBorder(isDark)
  val subtleBg = getMarineSubtleBg(isDark)
  val subtleBorder = getMarineSubtleBorder(isDark)
  val textPrimary = getMarineTextPrimary(isDark)
  val textSecondary = getMarineTextSecondary(isDark)
  val textMuted = getMarineTextMuted(isDark)
  val inputBg = getMarineInputBg(isDark)
  val inputBorder = getMarineInputBorder(isDark)
  val inputLabel = getMarineInputLabel(isDark)
  val inputTextColor = getMarineInputTextColor(isDark)

  val anchorInputColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = inputBg,
    unfocusedContainerColor = inputBg,
    focusedBorderColor = if (isDark) MarineYellow else Color(0xFFD97706),
    unfocusedBorderColor = inputBorder,
    focusedTextColor = if (isDark) MarineYellow else Color(0xFF78350F),
    unfocusedTextColor = inputTextColor,
    focusedLabelColor = if (isDark) MarineYellowBold else Color(0xFF92400E),
    unfocusedLabelColor = inputLabel,
    cursorColor = if (isDark) MarineYellowBold else Color(0xFFD97706)
  )

  // Alarm sound logic for Anchor Dragging
  val context = LocalContext.current
  val ringtone = remember {
    try {
      val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
      android.media.RingtoneManager.getRingtone(context, uri)
    } catch (e: Exception) {
      null
    }
  }

  val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: 0.0
  val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: 0.0
  val isAnchored = uiState.anchorEvent.isAnchored
  val maxRadius = result.f_secondSwingingCircleMeters

  LaunchedEffect(isAnchored, currentLat, currentLon, maxRadius) {
    if (isAnchored) {
      val distance = uiState.anchorEvent.calculateDistanceMeters(currentLat, currentLon)
      if (distance > maxRadius && maxRadius > 0) {
        if (ringtone?.isPlaying == false) {
          ringtone.play()
        }
      } else {
        if (ringtone?.isPlaying == true) {
          ringtone.stop()
        }
      }
    } else {
      if (ringtone?.isPlaying == true) {
        ringtone.stop()
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      ringtone?.stop()
    }
  }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
      .testTag("card_anchor_calculation")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // ══════════════════════════════════════════════════════════════════
      // 1. BAŞLIK & FORMÜL ŞERİDİ (DEMİRLEME: Salma Dairesi Hesabı)
      // ══════════════════════════════════════════════════════════════════
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .background(
                Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF0284C7))),
                RoundedCornerShape(10.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Anchor,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "DEMİRLEME & KALOMA",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 12.sp
                ),
                color = textPrimary
              )
            }
            Text(
              text = "I. Salma: ${String.format(Locale.US, "%.1fm", result.d_firstSwingingCircleMeters)} • II. Salma: ${String.format(Locale.US, "%.1fm", result.f_secondSwingingCircleMeters)}",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MarineCyan else PrimaryBlue
              )
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (result.scopeStatus.isSafe) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (result.scopeStatus.isSafe) (if (isDark) Color(0xFF059669) else Color(0xFF10B981)) else (if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)))
          ) {
            Text(
              text = if (result.scopeStatus.isSafe) "GÜVENLİ" else "DİKKAT",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp,
                color = if (result.scopeStatus.isSafe) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)) else (if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B))
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HorizontalDivider(color = cardBorder.copy(alpha = 0.6f))

          // ══════════════════════════════════════════════════════════════
          // 2. GİRİŞ PARAMETRELERİ (Kilit Standardı, Kilit Sayısı, b, c, Gemi Boyutları)
          // ══════════════════════════════════════════════════════════════
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Hesaplama",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  color = textPrimary
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                  border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF93C5FD))
                ) {
                  Text(
                    text = "1 Kilit = ${String.format(Locale.US, "%.1f", uiState.anchorShackleStandard.metersPerShackle)} m",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Black,
                      color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF1E40AF)
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // ─── 1 KİLİT ZİNCİR BOYU STANDARDI SEÇİCİ (27,5m / 25m) ───
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "1 Kilit Zincir Boyu Standardı:",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    color = textSecondary
                  )
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  ShackleLengthStandard.values().forEach { standard ->
                    val isSelected = uiState.anchorShackleStandard == standard
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF0B172B) else Color(0xFFF1F5F9)),
                      border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) (if (isDark) Color(0xFF38BDF8) else PrimaryBlue) else subtleBorder
                      ),
                      modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setAnchorShackleStandard(standard) }
                        .testTag("btn_shackle_standard_${if (standard == ShackleLengthStandard.STANDARD_27_5) "27_5" else "25_0"}")
                    ) {
                      Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                        Text(
                          text = standard.labelTr,
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = if (isSelected) (if (isDark) Color(0xFFF0F9FF) else Color(0xFF1E40AF)) else textPrimary
                          )
                        )
                        Text(
                          text = if (standard == ShackleLengthStandard.STANDARD_27_5) "1 Kilit = 27,5 m (Standart)" else "1 Kilit = 25,0 m (Metrik)",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (isSelected) (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)) else textMuted
                          )
                        )
                      }
                    }
                  }
                }
              }

              // ─── DEMİRLEME GİRİŞLERİ (KİLİT, DERİNLİK, KALOMA) ───
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF0C192E) else Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1D4ED8) else Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  // Otomatik Zincir Hesabı Başlığı ve Durum Göstergesi
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      Icon(
                        imageVector = if (uiState.isAnchorChainAuto) Icons.Default.AutoAwesome else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (uiState.isAnchorChainAuto) SeaGreen else (if (isDark) MarineYellow else Color(0xFFD97706)),
                        modifier = Modifier.size(11.dp)
                      )
                      Text(
                        text = if (uiState.isAnchorChainAuto) "Otomatik Zincir Boyu (Hava / Deniz / Batimetri)" else "Elle Girilmiş Kaloma (Manuel)",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 8.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (uiState.isAnchorChainAuto) SeaGreen else textSecondary
                        )
                      )
                    }

                    if (!uiState.isAnchorChainAuto) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SeaGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, SeaGreen),
                        modifier = Modifier.clickable { viewModel.setAnchorChainAuto(true) }
                      ) {
                        Text(
                          text = "⚡ Otomatiğe Dön",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SeaGreen
                          ),
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    OutlinedTextField(
                      value = uiState.anchorChainShacklesStr,
                      onValueChange = { viewModel.setAnchorChainShackles(it) },
                      label = { Text("Kilit", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                      trailingIcon = {
                        Text(
                          text = "Klt",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            color = if (isDark) MarineYellow else Color(0xFFD97706)
                          ),
                          modifier = Modifier.padding(end = 4.dp)
                        )
                      },
                      textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 11.sp),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      singleLine = true,
                      modifier = Modifier
                        .weight(1f)
                        .testTag("input_anchor_shackles_count")
                        
                    )

                    OutlinedTextField(
                      value = uiState.anchorDepthStr,
                      onValueChange = { viewModel.updateAnchorCalculation(depth = it) },
                      label = { Text("Derinlik", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                      trailingIcon = {
                        Text(
                          text = "m",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            color = if (isDark) MarineYellow else Color(0xFFD97706)
                          ),
                          modifier = Modifier.padding(end = 4.dp)
                        )
                      },
                      textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 11.sp),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      singleLine = true,
                      modifier = Modifier
                        .weight(1f)
                        .testTag("input_anchor_depth")
                        
                    )

                    OutlinedTextField(
                      value = uiState.anchorChainScopeStr,
                      onValueChange = { viewModel.updateAnchorCalculation(chainScope = it) },
                      label = { Text("Zincir Boyu", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                      trailingIcon = {
                        Text(
                          text = "m",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = textSecondary
                          ),
                          modifier = Modifier.padding(end = 4.dp)
                        )
                      },
                      textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 11.sp),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      singleLine = true,
                      modifier = Modifier
                        .weight(1f)
                        .testTag("input_anchor_chain_scope")
                        
                    )
                  }

                  // Hızlı Kilit Seçiciler (Kompakt Chips)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Oto Kaloma Butonu
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (uiState.isAnchorChainAuto) SeaGreen else (if (isDark) Color(0xFF0F1B2F) else Color(0xFFE2E8F0)),
                      border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (uiState.isAnchorChainAuto) SeaGreen else subtleBorder
                      ),
                      modifier = Modifier.clickable {
                        viewModel.setAnchorChainAuto(true)
                      }
                    ) {
                      Text(
                        text = "⚡ Oto Kaloma",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = if (uiState.isAnchorChainAuto) FontWeight.Black else FontWeight.Bold,
                          fontSize = 8.5.sp,
                          color = if (uiState.isAnchorChainAuto) Color.White else textPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                      )
                    }

                    listOf(3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 10.0).forEach { shackles ->
                      val isCurrent = !uiState.isAnchorChainAuto && kotlin.math.abs(result.a_chainScopeShackles - shackles) < 0.15
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCurrent) (if (isDark) Color(0xFF1D4ED8) else PrimaryBlue) else (if (isDark) Color(0xFF0F1B2F) else Color(0xFFE2E8F0)),
                        border = androidx.compose.foundation.BorderStroke(
                          0.8.dp,
                          if (isCurrent) (if (isDark) Color(0xFF60A5FA) else PrimaryBlue) else subtleBorder
                        ),
                        modifier = Modifier.clickable {
                          viewModel.setAnchorChainScopeByShackles(shackles)
                        }
                      ) {
                        Text(
                          text = "${shackles.toInt()} Klt",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = if (isCurrent) Color.White else textPrimary
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                      }
                    }
                  }
                }
              }

              if (uiState.verifiedMarineDepth != null) {
                val emodnetDepth = uiState.verifiedMarineDepth
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (emodnetDepth.isOnlineVerified) "🇪🇺" else "🗺️", fontSize = 9.5.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "EMODnet Derinliği: ${String.format(Locale.US, "%.1f", emodnetDepth.depthMeters)} m",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (emodnetDepth.isOnlineVerified) Color(0xFF10B981) else textSecondary
                      )
                    )
                  }
                  if (uiState.anchorDepthStr != String.format(Locale.US, "%.1f", emodnetDepth.depthMeters)) {
                    TextButton(
                      onClick = {
                        viewModel.updateAnchorCalculation(depth = String.format(Locale.US, "%.1f", emodnetDepth.depthMeters))
                      },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                      modifier = Modifier.height(24.dp)
                    ) {
                      Text("Demire Aktar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MarineCyan)
                    }
                  }
                }
              }

              // ══════════════════════════════════════════════════════════════
              // HAVA, DALGA VE DERİNLİĞE GÖRE TAVSİYE EDİLEN KİLİT / KALOMA
              // ══════════════════════════════════════════════════════════════
              val recScope = result.recommendedChainScope
              if (recScope != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                  border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    when (recScope.status) {
                      ChainRecommendationStatus.DEFICIENT -> if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)
                      ChainRecommendationStatus.OPTIMAL -> if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                      ChainRecommendationStatus.EXCESSIVE -> if (isDark) Color(0xFF0284C7) else Color(0xFF0EA5E9)
                    }
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_anchor_recommendation")
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // 1. Başlık ve Durum Rozeti
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = Icons.Default.Anchor,
                          contentDescription = null,
                          tint = when (recScope.status) {
                            ChainRecommendationStatus.DEFICIENT -> Color(0xFFEF4444)
                            ChainRecommendationStatus.OPTIMAL -> Color(0xFF10B981)
                            ChainRecommendationStatus.EXCESSIVE -> Color(0xFF38BDF8)
                          },
                          modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "HAVA & DERİNLİK KALOMA TAVSİYESİ",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                          ),
                          color = textPrimary
                        )
                      }

                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (recScope.status) {
                          ChainRecommendationStatus.DEFICIENT -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                          ChainRecommendationStatus.OPTIMAL -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                          ChainRecommendationStatus.EXCESSIVE -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                        }
                      ) {
                        Text(
                          text = when (recScope.status) {
                            ChainRecommendationStatus.DEFICIENT -> "YETERSİZ ZİNCİR"
                            ChainRecommendationStatus.OPTIMAL -> "İDEAL KALOMA"
                            ChainRecommendationStatus.EXCESSIVE -> "GENİŞ KALOMA"
                          },
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            color = when (recScope.status) {
                              ChainRecommendationStatus.DEFICIENT -> if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                              ChainRecommendationStatus.OPTIMAL -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                              ChainRecommendationStatus.EXCESSIVE -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                            }
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    // 2. Deniz Hava Durumu Parametreleri Özeti
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White,
                      border = androidx.compose.foundation.BorderStroke(0.6.dp, subtleBorder)
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        // Derinlik
                        Column {
                          Text("Derinlik", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, color = textSecondary))
                          Text("${String.format(Locale.US, "%.1f", result.b_depthMeters)} m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = if (isDark) MarineYellow else Color(0xFFD97706)))
                        }
                        // Rüzgar
                        Column {
                          Text("Rüzgar", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, color = textSecondary))
                          Text("${String.format(Locale.US, "%.0f", uiState.marineWeather.windSpeedKnots)} kn", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = textPrimary))
                        }
                        // Dalga
                        Column {
                          Text("Dalga", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, color = textSecondary))
                          Text("${String.format(Locale.US, "%.1f", uiState.marineWeather.waveHeightMeters)} m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = textPrimary))
                        }
                        // Beaufort
                        Column {
                          Text("Beaufort", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, color = textSecondary))
                          Text("Bft ${uiState.marineWeather.beaufortScale}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = if (isDark) MarineCyan else PrimaryBlue))
                        }
                        // Zemin
                        Column {
                          Text("Zemin", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, color = textSecondary))
                          Text(uiState.anchorBottomType.displayNameTr.take(8), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = if (isDark) MarineCyan else PrimaryBlue))
                        }
                      }
                    }

                    // 3. Hava Durumu Senaryo Seçici (Canlı / Sakin / Orta / Sert / Fırtına)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                      Text(
                        text = "Hava Simülasyonu / Durumu:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                      )
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        AnchorWeatherScenario.values().forEach { scn ->
                          val isSelected = uiState.anchorWeatherScenario == scn
                          Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isSelected) (if (isDark) Color(0xFF1E3A8A) else PrimaryBlue) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                            border = androidx.compose.foundation.BorderStroke(
                              0.8.dp,
                              if (isSelected) (if (isDark) Color(0xFF60A5FA) else PrimaryBlue) else subtleBorder
                            ),
                            modifier = Modifier.clickable {
                              viewModel.setAnchorWeatherScenario(scn)
                            }
                          ) {
                            Text(
                              text = when (scn) {
                                AnchorWeatherScenario.LIVE -> "🛰️ Canlı Hava"
                                AnchorWeatherScenario.CALM -> "🌤️ Sakin (<15kn)"
                                AnchorWeatherScenario.MODERATE -> "🌊 Orta (20kn)"
                                AnchorWeatherScenario.ROUGH -> "💨 Sert (30kn)"
                                AnchorWeatherScenario.STORM -> "🌪️ Fırtına (42kn)"
                              },
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = if (isSelected) Color.White else textPrimary
                              ),
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                          }
                        }
                      }
                    }

                    // 4. Ana Tavsiye Göstergesi (Öne Çıkan Kart)
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                      border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFF93C5FD))
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
                          Column {
                            Text(
                              text = "TAVSİYE EDİLEN ZİNCİR (KALOMA)",
                              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                              Text(
                                text = "${String.format(Locale.US, "%.1f", recScope.recommendedShackles)} KİLİT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                  fontWeight = FontWeight.Black,
                                  fontSize = 15.sp,
                                  color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                                )
                              )
                              Spacer(modifier = Modifier.width(6.dp))
                              Text(
                                text = "(${String.format(Locale.US, "%.1f", recScope.recommendedMeters)} m)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 11.sp,
                                  color = if (isDark) MarineYellow else Color(0xFFB45309)
                                ),
                                modifier = Modifier.padding(bottom = 1.dp)
                              )
                            }
                          }

                          Column(horizontalAlignment = Alignment.End) {
                            Text(
                              text = "Güvenli Aralık: ${String.format(Locale.US, "%.1f", recScope.recommendedShacklesMin)}-${String.format(Locale.US, "%.1f", recScope.recommendedShacklesMax)} Klt",
                              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            )
                            Text(
                              text = "Oran: ${String.format(Locale.US, "%.1f", recScope.recommendedScopeRatio)}x Derinlik",
                              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                            )
                          }
                        }

                        // 5. Durum Detayı ve Karşılaştırma Uyarısı
                        Text(
                          text = recScope.recommendationDetailTr,
                          style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            color = textPrimary
                          )
                        )

                        // 6. Tek Tıkla Uygulama Butonu (Mevcut kilit tavsiyeden farklıysa)
                        if (kotlin.math.abs(result.a_chainScopeShackles - recScope.recommendedShackles) >= 0.2) {
                          Spacer(modifier = Modifier.height(2.dp))
                          Button(
                            onClick = { viewModel.applyRecommendedChainScope() },
                            colors = ButtonDefaults.buttonColors(
                              containerColor = if (recScope.status == ChainRecommendationStatus.DEFICIENT) Color(0xFFDC2626) else PrimaryBlue
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                              .fillMaxWidth()
                              .height(30.dp)
                              .testTag("btn_apply_recommended_shackles")
                          ) {
                            Icon(
                              imageVector = Icons.Default.Check,
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = "Tavsiye Edilen Kalomayı Uygula (${String.format(Locale.US, "%.1f", recScope.recommendedShackles)} Kilit / ${String.format(Locale.US, "%.0f", recScope.recommendedMeters)} m)",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color.White
                              )
                            )
                          }
                        }
                      }
                    }

                    // Dip Kuralı Bilgi Notu
                    Text(
                      text = recScope.seamanshipRuleText,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.5.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = textMuted
                      )
                    )
                  }
                }
              }

              // Gelişmiş Gemi Boyutları & Mesafeler (Köprüüstü-Loça, Köprüüstü-Kıç)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showAdvancedInputs = !showAdvancedInputs }
                  .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Gemi Mesafeleri (K/Ü-Loça: ${String.format(Locale.US, "%.0fm", result.distBridgeToHawseMeters)}, K/Ü-Kıç: ${String.format(Locale.US, "%.0fm", result.distBridgeToSternMeters)})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                    color = if (isDark) Color(0xFF38BDF8) else PrimaryBlue
                  )
                }
                Icon(
                  imageVector = if (showAdvancedInputs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
                  modifier = Modifier.size(14.dp)
                )
              }

              AnimatedVisibility(visible = showAdvancedInputs) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    // Köprüüstü -> Loça
                    OutlinedTextField(
                      value = uiState.anchorBridgeToHawseStr,
                      onValueChange = { viewModel.updateAnchorCalculation(bridgeToHawse = it) },
                      label = { Text("Köprüüstü -> Loça", fontSize = 8.sp) },
                      placeholder = { Text("35.0 m", fontSize = 8.sp) },
                      textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.weight(1f)
                    )

                    // Köprüüstü -> Kıç
                    OutlinedTextField(
                      value = uiState.anchorBridgeToSternStr,
                      onValueChange = { viewModel.updateAnchorCalculation(bridgeToStern = it) },
                      label = { Text("Köprüüstü -> Kıç", fontSize = 8.sp) },
                      placeholder = { Text("85.0 m", fontSize = 8.sp) },
                      textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    // Gemi Tam Boyu (LOA)
                    OutlinedTextField(
                      value = uiState.anchorLoaStr,
                      onValueChange = { viewModel.updateAnchorCalculation(loa = it) },
                      label = { Text("Gemi Boyu LOA", fontSize = 8.sp) },
                      placeholder = { Text("120.0 m", fontSize = 8.sp) },
                      textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.weight(1f)
                    )

                    // Emniyet Marjı
                    OutlinedTextField(
                      value = uiState.anchorSafetyMarginStr,
                      onValueChange = { viewModel.updateAnchorCalculation(safetyMargin = it) },
                      label = { Text("İlave Emniyet (m)", fontSize = 8.sp) },
                      placeholder = { Text("0.0 m", fontSize = 8.sp) },
                      textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }
          }

          // ══════════════════════════════════════════════════════════════
          // 6. EYLEM BUTONLARI (Haritaya Aktar / Demir At / MOB)
          // ══════════════════════════════════════════════════════════════

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // MOB (Denize Adam Düştü) Butonu ("II. Salmayı Hesapla" butonu yerine)
            Button(
              onClick = {
                if (uiState.mobEvent.isActive) {
                  viewModel.cancelMob()
                } else {
                  viewModel.triggerMob()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.mobEvent.isActive) Color.Black else Color(0xFFDC2626),
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_mob_trigger")
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (uiState.mobEvent.isActive) "MOB İptal" else "🚨 MOB",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
              )
            }

            Button(
              onClick = {
                viewModel.applyAnchorCalculationToSwingingCircle()
                if (uiState.anchorEvent.isAnchored) {
                  viewModel.liftAnchor()
                } else {
                  viewModel.dropAnchor()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else PrimaryBlueDark,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_anchor_watch_start")
            ) {
              Icon(Icons.Default.Anchor, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At & Nöbet",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp)
              )
            }
          }

          // MOB Sabit GPS Mevki Penceresi (Demir At ile Aynı Şekilde Ekranda Gösterilir)
          if (uiState.mobEvent.isActive) {
            val currentLat = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.mobEvent.latitude
            val currentLon = com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.mobEvent.longitude
            val mobDistNm = uiState.mobEvent.calculateDistanceNm(currentLat, currentLon)
            val mobDistGomina = uiState.mobEvent.calculateDistanceGomina(currentLat, currentLon)
            val mobBearing = uiState.mobEvent.calculateBearingDegrees(currentLat, currentLon)

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isDark) Color(0xFF450A0A).copy(alpha = 0.5f) else Color(0xFFFEF2F2),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
              modifier = Modifier.fillMaxWidth().testTag("card_mob_screen_fixed_position")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "🚨 MOB (Denize adam düştü)",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                      color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFDC2626).copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = "Saat: ${uiState.mobEvent.timeFormatted}",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                      Text("Sabit Enlem", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                      Text(
                        text = LocationPresets.formatMarineLatDDM(uiState.mobEvent.latitude),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                      Text("Sabit Boylam", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                      Text(
                        text = LocationPresets.formatMarineLonDDM(uiState.mobEvent.longitude),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Düşüş Hızı: ${String.format(Locale.US, "%.1f", uiState.mobEvent.vesselSpeedAtDropKnots)} kn • Rota: ${String.format(Locale.US, "%03d°", uiState.mobEvent.vesselHeadingAtDrop)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = textPrimary
                  )
                  Text(
                    text = "Mesafe: ${String.format(Locale.US, "%.2f NM (%.1f Gom)", mobDistNm, mobDistGomina)} • Kerteriz: ${String.format(Locale.US, "%03d°", mobBearing)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                  )
                }
              }
            }
          }

          // Demirleme Sabit GPS Mevki Penceresi
          if (uiState.anchorEvent.isAnchored) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
              modifier = Modifier.fillMaxWidth().testTag("card_anchor_screen_fixed_position")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Anchor, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "⚓ SABİTLENEN DEMİR MEVKİİ",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp),
                      color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = "Saat: ${uiState.anchorEvent.dropTimeFormatted}",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                      color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                      Text("Sabit Enlem", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                      Text(
                        text = LocationPresets.formatMarineLatDDM(uiState.anchorEvent.latitude),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                      Text("Sabit Boylam", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), color = textSecondary)
                      Text(
                        text = LocationPresets.formatMarineLonDDM(uiState.anchorEvent.longitude),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Derinlik: ${String.format(Locale.US, "%.1f", uiState.anchorEvent.chartedDepthAtDropMeters)} m",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = textPrimary
                  )
                  Text(
                    text = "Emniyet: ${String.format(Locale.US, "%.1f Gomina", uiState.anchorEvent.safeSwingingRadiusGomina)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = textPrimary
                  )
                }
                
                // TARAMA UYARISI
                if (uiState.anchorEvent.isAnchored) {
                  val currentDistMeters = uiState.anchorEvent.calculateDistanceMeters(currentLat, currentLon)
                  if (currentDistMeters > maxRadius && maxRadius > 0) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Color(0xFFFEF2F2),
                      border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
                      modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                      Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alarm", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(
                            text = "DİKKAT: GEMİ DEMİR TARAMAKTADIR!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                          )
                          Text(
                            text = "Mevcut Sapma: ${String.format(Locale.US, "%.0f", currentDistMeters)}m / Sınır: ${String.format(Locale.US, "%.0f", maxRadius)}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // ══════════════════════════════════════════════════════════════
          // YAN KESİT VE KUŞBAKIŞI ŞEMALARI (CANVAS)
          // ══════════════════════════════════════════════════════════════
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().testTag("card_anchor_diagram_schema")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Başlık: Demirleme Yan Profil Kesiti
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
              ) {
                Icon(
                  imageVector = Icons.Default.Straighten,
                  contentDescription = null,
                  tint = if (isDark) MarineCyan else PrimaryBlue,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "DEMİRLEME YAN PROFİL KESİTİ (PİSAGOR GEOMETRİSİ)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                  )
                )
              }

              // Diyagram Alanı: Sadece Yan Profil
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp)
                  .background(if (isDark) Color(0xFF070D1E) else Color(0xFFF0F9FF), RoundedCornerShape(6.dp))
                  .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                  .clip(RoundedCornerShape(6.dp))
              ) {
                SideProfileAnchorDiagram(result = result, isDark = isDark)
              }
            }
          }
        }
    }
  }
}

/**
 * Kitaptaki Yan Kesit Şeması Çizimi (Fotoğrafın dijital karşılığı)
 */
@Composable
private fun SideProfileAnchorDiagram(
  result: AnchorCalculationResult,
  isDark: Boolean = true
) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Su seviyesi ve Deniz tabanı Y koordinatları
    val waterY = h * 0.38f
    val seabedY = h * 0.85f

    // Gemi koordinatları (Sol tarafta)
    val shipStartX = w * 0.05f // Kıç
    val bridgeX = w * 0.22f // Köprüüstü
    val hawseX = w * 0.38f // Loça / Baş bodoslama
    val anchorX = w * 0.88f // Demir Yeri

    // 0. Gökyüzü Alanı (Su seviyesi üstü)
    drawRect(
      color = if (isDark) Color(0xFF070D1E) else Color(0xFFE0F2FE),
      topLeft = Offset(0f, 0f),
      size = Size(w, waterY)
    )

    // 1. Deniz Alanı Arka Planı (Mavi ton)
    drawRect(
      color = if (isDark) Color(0xFF0F2942) else Color(0xFFBAE6FD),
      topLeft = Offset(0f, waterY),
      size = Size(w, seabedY - waterY)
    )

    // 2. Deniz Tabanı Arka Planı (Kum/Çamur tonu)
    drawRect(
      color = if (isDark) Color(0xFF2A2015) else Color(0xFFE2C499),
      topLeft = Offset(0f, seabedY),
      size = Size(w, h - seabedY)
    )

    // Su Yüzeyi Çizgisi
    drawLine(
      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
      start = Offset(0f, waterY),
      end = Offset(w, waterY),
      strokeWidth = 1.5f
    )

    // Deniz Tabanı Çizgisi
    drawLine(
      color = if (isDark) Color(0xFFF59E0B) else Color(0xFFB45309),
      start = Offset(0f, seabedY),
      end = Offset(w, seabedY),
      strokeWidth = 2f
    )

    // 3. Gemi Silueti Çizimi
    val shipPath = Path().apply {
      // Gövde
      moveTo(shipStartX, waterY + 4f)
      lineTo(shipStartX - 4f, waterY - 14f)
      lineTo(hawseX - 6f, waterY - 16f)
      lineTo(hawseX, waterY + 4f)
      close()
    }
    drawPath(shipPath, color = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
    drawPath(shipPath, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155), style = Stroke(width = 1.5f))

    // Köprüüstü Üstyapısı
    val bridgeRect = androidx.compose.ui.geometry.Rect(bridgeX - 10f, waterY - 32f, bridgeX + 8f, waterY - 14f)
    drawRect(color = if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1), topLeft = Offset(bridgeRect.left, bridgeRect.top), size = Size(bridgeRect.width, bridgeRect.height))
    drawRect(color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569), topLeft = Offset(bridgeRect.left, bridgeRect.top), size = Size(bridgeRect.width, bridgeRect.height), style = Stroke(1.5f))
    // Radar direği
    drawLine(color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B), start = Offset(bridgeX, waterY - 32f), end = Offset(bridgeX, waterY - 42f), strokeWidth = 2f)

    // 4. ÖLÇÜ ÇİZGİLERİ (e ve d: Gemi üstündeki boyutlar)
    val dimTopY = waterY - 50f
    val dimGuideColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    // e: Köprüüstünden Kıça Mesafe
    drawLine(color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155), start = Offset(shipStartX, dimTopY), end = Offset(bridgeX, dimTopY), strokeWidth = 1.5f)
    drawLine(color = dimGuideColor, start = Offset(shipStartX, dimTopY - 5f), end = Offset(shipStartX, waterY - 14f), strokeWidth = 1f)
    drawLine(color = dimGuideColor, start = Offset(bridgeX, dimTopY - 5f), end = Offset(bridgeX, waterY - 32f), strokeWidth = 1f)

    // d: Köprüüstünden Loçaya Mesafe
    drawLine(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), start = Offset(bridgeX, dimTopY), end = Offset(hawseX, dimTopY), strokeWidth = 1.5f)
    drawLine(color = dimGuideColor, start = Offset(hawseX, dimTopY - 5f), end = Offset(hawseX, waterY - 16f), strokeWidth = 1f)

    // 5. PİSAGOR DİK ÜÇGENİ: a, b, c
    // c: Loçadan demir yerine yatay mesafe (Su yüzeyi üzerinde)
    val cY = waterY
    drawLine(
      color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
      start = Offset(hawseX, cY),
      end = Offset(anchorX, cY),
      strokeWidth = 2.5f
    )

    // b: Derinlik (Düşey dik çizgi)
    drawLine(
      color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
      start = Offset(anchorX, cY),
      end = Offset(anchorX, seabedY),
      strokeWidth = 2.5f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
    )

    // Dik açı işareti (90°)
    val cornerSize = 10f
    val cornerColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    drawLine(color = cornerColor, start = Offset(anchorX - cornerSize, cY), end = Offset(anchorX - cornerSize, cY + cornerSize), strokeWidth = 1f)
    drawLine(color = cornerColor, start = Offset(anchorX - cornerSize, cY + cornerSize), end = Offset(anchorX, cY + cornerSize), strokeWidth = 1f)

    // a: Verilen Kaloma (Hipotenüs - Zincir çizgisi)
    drawLine(
      color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
      start = Offset(hawseX, cY),
      end = Offset(anchorX, seabedY),
      strokeWidth = 3f
    )

    // Demir (Çapa) Noktası
    drawCircle(color = if (isDark) Color.White else Color(0xFF0F172A), radius = 5f, center = Offset(anchorX, seabedY))
    drawCircle(color = Color(0xFFE11D48), radius = 3.5f, center = Offset(anchorX, seabedY))

    // Loça Noktası
    drawCircle(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), radius = 4f, center = Offset(hawseX, cY))

    // Köprüüstü Noktası
    drawCircle(color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706), radius = 4f, center = Offset(bridgeX, waterY - 14f))

    // Kıç Noktası
    drawCircle(color = Color(0xFFEF4444), radius = 3.5f, center = Offset(shipStartX, waterY - 14f))

    // Metin Etiketleri (Native Canvas ile)
    val smallPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 20f
      color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.parseColor("#334155")
    }

    val bluePaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 22f
      color = if (isDark) android.graphics.Color.parseColor("#38BDF8") else android.graphics.Color.parseColor("#0284C7")
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val greenPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 22f
      color = if (isDark) android.graphics.Color.parseColor("#34D399") else android.graphics.Color.parseColor("#047857")
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val yellowPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 22f
      color = if (isDark) android.graphics.Color.parseColor("#FBBF24") else android.graphics.Color.parseColor("#92400E")
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    drawContext.canvas.nativeCanvas.apply {
      // e etiketi (Köprüüstü - Kıç)
      drawText("e (${String.format(Locale.US, "%.0fm", result.distBridgeToSternMeters)})", (shipStartX + bridgeX) / 2f - 24f, dimTopY - 6f, smallPaint)

      // d etiketi (Köprüüstü - Loça)
      drawText("d (${String.format(Locale.US, "%.0fm", result.distBridgeToHawseMeters)})", (bridgeX + hawseX) / 2f - 24f, dimTopY - 6f, smallPaint)

      // Loça etiketi
      drawText("loça", hawseX - 12f, waterY - 8f, smallPaint)

      // Demir yeri etiketi
      drawText("demir yeri", anchorX - 50f, waterY - 10f, smallPaint)

      // c etiketi (Yatay taban)
      drawText("c = ${String.format(Locale.US, "%.1fm", result.c_horizontalDistanceMeters)}", (hawseX + anchorX) / 2f - 40f, waterY + 22f, bluePaint)

      // b etiketi (Derinlik)
      drawText("b = ${String.format(Locale.US, "%.0fm", result.b_depthMeters)}", anchorX + 10f, (waterY + seabedY) / 2f + 6f, yellowPaint)

      // a etiketi (Kaloma Hipotenüs)
      drawText("a = ${String.format(Locale.US, "%.1fm", result.a_chainScopeMeters)} (${String.format(Locale.US, "%.1f", result.a_chainScopeShackles)} kilit)", (hawseX + anchorX) / 2f - 60f, (waterY + seabedY) / 2f + 26f, greenPaint)

      // I. ve II. Salma Özet Notu
      drawText("1. Salma: c+d = ${String.format(Locale.US, "%.1fm", result.d_firstSwingingCircleMeters)}", 20f, h - 12f, bluePaint)
      drawText("2. Salma: c+LOA = ${String.format(Locale.US, "%.1fm", result.f_secondSwingingCircleMeters)}", w - 240f, h - 12f, yellowPaint)
    }
  }
}
