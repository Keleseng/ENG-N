import re

with open('app/src/main/java/com/example/ui/components/AnchorCalculationCard.kt', 'r') as f:
    content = f.read()

target = "              // c: Loçadan Demir Yerine Olan Yatay Mesafe"

replacement = """              // Derinlik Tavsiye Kutusu
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
                          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = textSecondary)
                        )
                      }
                    }
                  }
              }

              // c: Loçadan Demir Yerine Olan Yatay Mesafe"""

new_content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/components/AnchorCalculationCard.kt', 'w') as f:
    f.write(new_content)
