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
  var isExpanded by rememberSaveable { mutableStateOf(true) }
  var showAdvancedInputs by rememberSaveable { mutableStateOf(false) }
  var activeDiagramTab by rememberSaveable { mutableStateOf(0) } // 0: Yan Kesit, 1: Kuşbakışı Radar

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

  val chevronRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    label = "anchorExpand"
  )

  var isFullScreenMap by rememberSaveable { mutableStateOf(false) }

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
          .clickable { isExpanded = !isExpanded }
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
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier
              .size(22.dp)
              .rotate(chevronRotation)
          )
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
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
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = subtleBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Hesaplama Parametreleri",
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
                      label = { Text("Kaloma", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
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
                    listOf(3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 10.0).forEach { shackles ->
                      val isCurrent = kotlin.math.abs(result.a_chainScopeShackles - shackles) < 0.15
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

              // Derinlik Tavsiye Kutusu
              val recDepth = result.b_depthMeters
              if (recDepth > 0) {
                  val recommendation = when {
                      recDepth < 10.0 -> "Sığ su: Çapalamanın iyi tutunması için en az 3-4 kilit veya derinliğin 4-5 katı kaloma uygulanması önerilir."
                      recDepth <= 20.0 -> "Orta derinlik: Güvenli demirleme için derinliğin 4-5 katı (genellikle 3-5 kilit) kaloma verilmesi tavsiye edilir."
                      recDepth <= 40.0 -> "Derin su: Rüzgar şiddetine bağlı olarak derinliğin 3-4 katı (genellikle 5-8 kilit) kaloma verilmesi uygundur."
                      else -> "Çok derin su: Demirleme zorluğu yaşanabilir, derinliğin en az 3 katı ve çevresel şartlara göre yeterli emniyet payı içeren kaloma sağlanmalıdır."
                  }
                  
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.Top
                    ) {
                      Icon(Icons.Default.Info, contentDescription = "Tavsiye", tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text(
                          text = "Derinlik Tavsiyesi",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = recommendation,
                          style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, color = textSecondary)
                        )
                      }
                    }
                  }
              }

              // c: Loçadan Demir Yerine Olan Yatay Mesafe
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF0C192E) else Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E355B) else Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(
                        text = "c = Loçadan Demir Yerine Yatay Mesafe",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                        color = textPrimary
                      )
                      Text(
                        text = if (uiState.isAnchorAutoHorizontal) "Formül: c = √(a² - b²)" else "Manuel Giriş Aktif",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = textMuted)
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = if (uiState.isAnchorAutoHorizontal) "Otomatik (Pisagor)" else "Manuel",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp,
                          color = if (uiState.isAnchorAutoHorizontal) (if (isDark) Color(0xFF38BDF8) else PrimaryBlue) else Color(0xFFF59E0B)
                        )
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Switch(
                        checked = uiState.isAnchorAutoHorizontal,
                        onCheckedChange = { viewModel.updateAnchorCalculation(isAutoHorizontal = it) },
                        colors = SwitchDefaults.colors(
                          checkedThumbColor = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
                          checkedTrackColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE),
                          uncheckedThumbColor = textMuted,
                          uncheckedTrackColor = subtleBorder
                        ),
                        modifier = Modifier.height(24.dp)
                      )
                    }
                  }

                  if (uiState.isAnchorAutoHorizontal) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isDark) Color(0xFF0F2445) else Color(0xFFDBEAFE),
                      border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1D4ED8) else Color(0xFF93C5FD))
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "c = √(a² - b²):",
                          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                          color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                        )
                        Text(
                          text = "${String.format(Locale.US, "%.1f", result.c_horizontalDistanceMeters)} m (${String.format(Locale.US, "%.2f", result.c_horizontalDistanceGomina)} Gomina)",
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = if (isDark) Color(0xFF38BDF8) else PrimaryBlue)
                        )
                      }
                    }
                  } else {
                    OutlinedTextField(
                      value = uiState.anchorCustomHorizontalDistStr,
                      onValueChange = { viewModel.updateAnchorCalculation(customHorizontal = it) },
                      label = { Text("c = Özel Yatay Mesafe (Metre)") },
                      placeholder = { Text(String.format(Locale.US, "%.1f", result.c_horizontalDistanceMeters)) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.fillMaxWidth()
                    )
                  }
                }
              }

              // Gelişmiş Gemi Boyutları & Mesafeler (Köprüüstü-Loça, Köprüüstü-Kıç)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showAdvancedInputs = !showAdvancedInputs }
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Gemi Mesafeleri (K/Ü-Loça: ${String.format(Locale.US, "%.0fm", result.distBridgeToHawseMeters)}, K/Ü-Kıç: ${String.format(Locale.US, "%.0fm", result.distBridgeToSternMeters)})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                    color = if (isDark) Color(0xFF38BDF8) else PrimaryBlue
                  )
                }
                Icon(
                  imageVector = if (showAdvancedInputs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = if (isDark) Color(0xFF38BDF8) else PrimaryBlue,
                  modifier = Modifier.size(16.dp)
                )
              }

              AnimatedVisibility(visible = showAdvancedInputs) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Köprüüstü -> Loça
                    OutlinedTextField(
                      value = uiState.anchorBridgeToHawseStr,
                      onValueChange = { viewModel.updateAnchorCalculation(bridgeToHawse = it) },
                      label = { Text("Köprüüstü -> Loça") },
                      placeholder = { Text("35.0 m") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    )

                    // Köprüüstü -> Kıç
                    OutlinedTextField(
                      value = uiState.anchorBridgeToSternStr,
                      onValueChange = { viewModel.updateAnchorCalculation(bridgeToStern = it) },
                      label = { Text("Köprüüstü -> Kıç") },
                      placeholder = { Text("85.0 m") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Gemi Tam Boyu (LOA)
                    OutlinedTextField(
                      value = uiState.anchorLoaStr,
                      onValueChange = { viewModel.updateAnchorCalculation(loa = it) },
                      label = { Text("Gemi Boyu LOA") },
                      placeholder = { Text("120.0 m") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    )

                    // Emniyet Marjı
                    OutlinedTextField(
                      value = uiState.anchorSafetyMarginStr,
                      onValueChange = { viewModel.updateAnchorCalculation(safetyMargin = it) },
                      label = { Text("İlave Emniyet (m)") },
                      placeholder = { Text("0.0 m") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      colors = anchorInputColors,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    )
                  }
                }
              }
            }
          }

          // ══════════════════════════════════════════════════════════════
          // 6. EYLEM BUTONLARI (Geçmişe Kaydet / Haritaya Aktar / Demir At)
          // ══════════════════════════════════════════════════════════════
          Button(
            onClick = {
              viewModel.saveCurrentAnchorCalculation()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF0F766E),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag("btn_save_anchor_to_history")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "Demirleme mevkii (Kaydet)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            )
          }

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
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().testTag("card_anchor_diagram_schema")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Başlık
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (activeDiagramTab == 0) Icons.Default.DirectionsBoat else Icons.Default.Radar,
                  contentDescription = null,
                  tint = if (isDark) MarineYellow else Color(0xFFD97706),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (activeDiagramTab == 0) "Demirleme & Kaloma Yan Kesit Şeması" else "Salma Dairesi",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                  )
                )
              }

              // Tab Değiştirici (2 Sekme: Yan Kesit, Kuşbakışı Radar)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                  .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (activeDiagramTab == 0) PrimaryBlue else Color.Transparent,
                  modifier = Modifier
                    .weight(1f)
                    .clickable { activeDiagramTab = 0 }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.DirectionsBoat,
                      contentDescription = null,
                      tint = if (activeDiagramTab == 0) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Yan Kesit Şeması",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = if (activeDiagramTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeDiagramTab == 0) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)
                      )
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (activeDiagramTab == 1) PrimaryBlue else Color.Transparent,
                  modifier = Modifier
                    .weight(1f)
                    .clickable { activeDiagramTab = 1 }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Map,
                      contentDescription = null,
                      tint = if (activeDiagramTab == 1) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Salma Dairesi (Canlı Harita)",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = if (activeDiagramTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeDiagramTab == 1) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)
                      )
                    )
                  }
                }
              }

              // Diyagram Alanı
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(240.dp)
                  .background(if (isDark) Color(0xFF070D1E) else Color(0xFFF0F9FF), RoundedCornerShape(8.dp))
                  .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                  .clip(RoundedCornerShape(8.dp))
              ) {
                if (activeDiagramTab == 0) {
                  SideProfileAnchorDiagram(result = result, isDark = isDark)
                } else {
                  AnchorSwingingCircleMap(
                    uiState = uiState,
                    result = result,
                    isFullScreen = false,
                    onToggleFullScreen = { isFullScreenMap = true }
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (isFullScreenMap) {
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { isFullScreenMap = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AnchorSwingingCircleMap(
          uiState = uiState,
          result = result,
          isFullScreen = true,
          onToggleFullScreen = { isFullScreenMap = false }
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AnchorSwingingCircleMap(
  uiState: TideUiState,
  result: AnchorCalculationResult,
  isFullScreen: Boolean,
  onToggleFullScreen: () -> Unit
) {
  // AIS Gerçek Harita Görünümü & Salma Dairesi
  val mapLat = when {
    uiState.mobEvent.isActive && uiState.mobEvent.latitude != 0.0 -> uiState.mobEvent.latitude
    uiState.anchorEvent.isAnchored && uiState.anchorEvent.latitude != 0.0 -> uiState.anchorEvent.latitude
    else -> com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: 40.836
  }
  val mapLon = when {
    uiState.mobEvent.isActive && uiState.mobEvent.longitude != 0.0 -> uiState.mobEvent.longitude
    uiState.anchorEvent.isAnchored && uiState.anchorEvent.longitude != 0.0 -> uiState.anchorEvent.longitude
    else -> com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: 29.301
  }
  val mtUrl = "https://www.marinetraffic.com/en/ais/home/centerx:${String.format(java.util.Locale.US, "%.4f", mapLon)}/centery:${String.format(java.util.Locale.US, "%.4f", mapLat)}/zoom:14"

  var isMapLoading by remember { mutableStateOf(true) }
  var mapLoadError by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Gerçek AIS Canlı Haritası
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        WebView(ctx).apply {
          setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MarineNav/1.0"
          }
          webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isMapLoading = true
              mapLoadError = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isMapLoading = false

              if (uiState.isDarkMode) {
                view?.evaluateJavascript(
                  """
                  (function() {
                    var style = document.createElement('style');
                    style.innerHTML = 'html { filter: invert(100%) hue-rotate(180deg) brightness(85%) contrast(85%); background: #121212 !important; } iframe, img, canvas { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                  })();
                  """.trimIndent(),
                  null
                )
              }
              
              view?.evaluateJavascript(com.example.engine.MapOverlayInjector.getInjectableJavascript(uiState), null)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
              super.onReceivedError(view, request, error)
              if (request?.isForMainFrame == true) {
                isMapLoading = false
                mapLoadError = true
              }
            }
          }
          loadUrl(mtUrl)
        }
      },
      update = { wv ->
        if (wv.url == null) {
          wv.loadUrl(mtUrl)
        }
      }
    )

    // Harita yüklenirken veya hata durumunda yedek deniz haritası arkaplanı
    if (mapLoadError) {
      Image(
        painter = painterResource(id = R.drawable.map_screenshot_1788370170131),
        contentDescription = "Map Fallback",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().alpha(0.7f)
      )
      // Sadece hata durumunda statik radar çizimini göster
      TopDownRadarDiagram(result = result, uiState = uiState, isDark = uiState.isDarkMode)
    }

    // Yükleme Göstergesi
    if (isMapLoading) {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
        color = MarineYellow,
        trackColor = Color(0xFF0F172A).copy(alpha = 0.3f)
      )
    }

    // Canlı AIS Rozeti & Tam Ekran Butonu
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, MarineCyan.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .background(Color(0xFF10B981), CircleShape)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "AIS Canlı",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE2E8F0)
            )
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(4.dp),
        color = PrimaryBlue.copy(alpha = 0.9f),
        modifier = Modifier.clickable { onToggleFullScreen() }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(if (isFullScreen) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull, contentDescription = "Tam Ekran", tint = Color.White, modifier = Modifier.size(11.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (isFullScreen) "Küçült" else "Tam Harita",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
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

/**
 * Kuşbakışı Radar Salma Dairesi Çizimi
 */
@Composable
private fun TopDownRadarDiagram(
  result: AnchorCalculationResult,
  uiState: TideUiState,
  isDark: Boolean = true
) {
  val vesselHeading = uiState.headingDegreesStr.toDoubleOrNull() ?: 45.0
  val vesselSpeed = uiState.speedStr.toDoubleOrNull() ?: 0.0

  Canvas(modifier = Modifier.fillMaxSize()) {
    val center = Offset(size.width / 2f, size.height / 2f + 10f)
    val maxDim = kotlin.math.min(size.width, size.height)
    val maxRadiusMeters = kotlin.math.max(result.f_secondSwingingCircleMeters * 1.28, 50.0)
    val scale = (maxDim / 2f * 0.78f) / maxRadiusMeters.toFloat()

    val r1 = (result.d_firstSwingingCircleMeters.toFloat() * scale).coerceAtLeast(18f)
    val r2 = (result.f_secondSwingingCircleMeters.toFloat() * scale).coerceAtLeast(r1 + 12f)

    // Arka plan
    drawRect(color = if (isDark) Color(0xFF070D1E) else Color(0xFFF0F9FF))

    // Grid ve pusula kerteriz halkaları
    val ringColor1 = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val ringColor2 = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
    drawCircle(color = ringColor1, radius = r2 * 1.15f, center = center, style = Stroke(width = 1f))
    drawCircle(color = ringColor2, radius = r2 * 1.15f, center = center, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))

    // Pusula Ana Yönleri
    val compassRad = r2 * 1.15f
    drawLine(color = ringColor2, start = Offset(center.x, center.y - compassRad), end = Offset(center.x, center.y + compassRad), strokeWidth = 1f)
    drawLine(color = ringColor2, start = Offset(center.x - compassRad, center.y), end = Offset(center.x + compassRad, center.y), strokeWidth = 1f)

    // 2. Salma Dairesi (f - Dış Emniyet Çemberi, Kırmızı)
    val circle2Color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    drawCircle(color = circle2Color.copy(alpha = if (isDark) 0.25f else 0.15f), radius = r2, center = center)
    drawCircle(color = circle2Color, radius = r2, center = center, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))))

    // 1. Salma Dairesi (d - Köprüüstü/Radar Gözlem Çemberi, Turuncu)
    val circle1Color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    drawCircle(color = circle1Color.copy(alpha = if (isDark) 0.20f else 0.12f), radius = r1, center = center)
    drawCircle(color = circle1Color, radius = r1, center = center, style = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 8f))))

    // Demirleme Noktası (Merkez)
    drawCircle(color = if (isDark) Color.White else Color(0xFF0F172A), radius = 6f, center = center)
    drawCircle(color = if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1), radius = 4f, center = center)

    // Gemi Konumu (Pruva ve Salma yönüne göre)
    val angleRad = Math.toRadians(vesselHeading)
    val cosA = cos(angleRad).toFloat()
    val sinA = sin(angleRad).toFloat()

    val cPix = (result.c_horizontalDistanceMeters.toFloat() * scale).coerceAtLeast(12f)
    val hawsePos = Offset(center.x + sinA * cPix, center.y - cosA * cPix)

    // Zincir hattı (Demir -> Loça)
    drawLine(
      color = if (isDark) MarineYellow else Color(0xFFB45309),
      start = center,
      end = hawsePos,
      strokeWidth = 2.5f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
    )

    // Gemi Gövdesi (Loça -> Köprüüstü -> Kıç)
    val bowHawseToBridge = (result.distBridgeToHawseMeters.toFloat() * scale).coerceAtLeast(6f)
    val bridgePos = Offset(hawsePos.x - sinA * bowHawseToBridge, hawsePos.y + cosA * bowHawseToBridge)

    val bridgeToStern = (result.distBridgeToSternMeters.toFloat() * scale).coerceAtLeast(6f)
    val sternPos = Offset(bridgePos.x - sinA * bridgeToStern, bridgePos.y + cosA * bridgeToStern)

    // Gemi silueti gövdesi
    val shipLineDark = if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1)
    val shipLineBright = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    drawLine(color = shipLineDark, start = hawsePos, end = sternPos, strokeWidth = 6f)
    drawLine(color = shipLineBright, start = hawsePos, end = sternPos, strokeWidth = 3f)

    drawCircle(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), radius = 4.5f, center = hawsePos) // Loça
    drawCircle(color = if (isDark) MarineYellow else Color(0xFFD97706), radius = 5f, center = bridgePos) // Köprüüstü
    drawCircle(color = Color(0xFFEF4444), radius = 4f, center = sternPos) // Kıç

    val yellowPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 21f
      color = if (isDark) android.graphics.Color.parseColor("#FDE047") else android.graphics.Color.parseColor("#92400E")
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val cyanPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 21f
      color = if (isDark) android.graphics.Color.parseColor("#38BDF8") else android.graphics.Color.parseColor("#0284C7")
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val infoPaint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 19f
      color = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#475569")
    }

    drawContext.canvas.nativeCanvas.apply {
      // Üst Bilgi Başlığı
      val vName = uiState.vesselName.ifBlank { uiState.selectedVessel.name }
      drawText(
        "$vName | Pruva: ${String.format(Locale.US, "%.0f°", vesselHeading)} | Sürat: ${String.format(Locale.US, "%.1f kn", vesselSpeed)}",
        16f,
        24f,
        infoPaint
      )
      // Alt Salma Etiketleri
      drawText(
        "I. Salma (d=${String.format(Locale.US, "%.0fm", result.d_firstSwingingCircleMeters)})",
        16f,
        size.height - 10f,
        cyanPaint
      )
      drawText(
        "II. Salma (f=${String.format(Locale.US, "%.0fm", result.f_secondSwingingCircleMeters)})",
        size.width - 240f,
        size.height - 10f,
        yellowPaint
      )
    }
  }
}
