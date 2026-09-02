package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RuleOfTwelfthStep
import com.example.ui.theme.*

@Composable
fun RuleOfTwelfthsCard(
  steps: List<RuleOfTwelfthStep> = emptyList(),
  isDarkMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val cardBg = getMarineCardBg(isDarkMode)
  val cardBorder = getMarineCardBorder(isDarkMode)
  val subtleBg = getMarineSubtleBg(isDarkMode)
  val canvasBg = getMarineCanvasBg(isDarkMode)
  val textPrimary = getMarineTextPrimary(isDarkMode)
  val textSecondary = getMarineTextSecondary(isDarkMode)
  val textMuted = getMarineTextMuted(isDarkMode)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("rule_of_twelfths_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "12'de Birler Kuralı Gelgit Yükseliş Tablosu",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = textPrimary
      )
      Text(
        text = "Denizcilikte saatlik su artış modeli (1/12, 2/12, 3/12, 3/12, 2/12, 1/12)",
        style = MaterialTheme.typography.bodySmall,
        color = textSecondary
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Tablo Dış Çerçevesi
      Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        color = cardBg,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Tablo Başlıkları
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(subtleBg)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Saat Dilimi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textMuted, modifier = Modifier.weight(1.3f))
            Text("Oran", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textMuted, modifier = Modifier.weight(0.7f))
            Text("Artış", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textMuted, modifier = Modifier.weight(0.9f))
            Text("Kümülatif", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textMuted, modifier = Modifier.weight(1.1f))
          }

          // Satırlar
          steps.forEachIndexed { index, step ->
            val bg = if (index % 2 == 0) cardBg else canvasBg
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = step.timeRangeFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = textPrimary,
                modifier = Modifier.weight(1.3f)
              )
              Text(
                text = step.fractionLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkMode) MarineCyan else PrimaryBlue,
                modifier = Modifier.weight(0.7f)
              )
              Text(
                text = "+${step.intervalRiseMeters}m",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = textPrimary,
                modifier = Modifier.weight(0.9f)
              )
              Text(
                text = "${step.cumulativeHeightMeters}m",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkMode) SeaGreen else Color(0xFF059669),
                modifier = Modifier.weight(1.1f)
              )
            }
          }
        }
      }
    }
  }
}

