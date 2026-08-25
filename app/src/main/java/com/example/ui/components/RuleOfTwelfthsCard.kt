package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RuleOfTwelfthStep
import com.example.ui.theme.*

@Composable
fun RuleOfTwelfthsCard(
  steps: List<RuleOfTwelfthStep> = emptyList(),
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("rule_of_twelfths_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
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
        color = TextPrimary
      )
      Text(
        text = "Denizcilikte saatlik su artış modeli (1/12, 2/12, 3/12, 3/12, 2/12, 1/12)",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Tablo Dış Çerçevesi
      Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        color = CardWhite,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Tablo Başlıkları
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(CardSubtle)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Saat Dilimi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted, modifier = Modifier.weight(1.3f))
            Text("Oran", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted, modifier = Modifier.weight(0.7f))
            Text("Artış", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted, modifier = Modifier.weight(0.9f))
            Text("Kümülatif", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted, modifier = Modifier.weight(1.1f))
          }

          // Satırlar
          steps.forEachIndexed { index, step ->
            val bg = if (index % 2 == 0) CardWhite else BackgroundCanvas
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
                color = TextPrimary,
                modifier = Modifier.weight(1.3f)
              )
              Text(
                text = step.fractionLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue,
                modifier = Modifier.weight(0.7f)
              )
              Text(
                text = "+${step.intervalRiseMeters}m",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                modifier = Modifier.weight(0.9f)
              )
              Text(
                text = "${step.cumulativeHeightMeters}m",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = SeaGreen,
                modifier = Modifier.weight(1.1f)
              )
            }
          }
        }
      }
    }
  }
}

