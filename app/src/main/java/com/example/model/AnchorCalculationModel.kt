package com.example.model

import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

enum class ShackleLengthStandard(val metersPerShackle: Double, val labelTr: String, val shortDesc: String) {
  STANDARD_27_5(27.5, "27,5 m (Standart)", "1 Kilit = 27,5 m (Türk / İngiliz Denizcilik Standardı)"),
  METRIC_25_0(25.0, "25,0 m (Metrik Sistem)", "1 Kilit = 25,0 m (Metrik Donanım Standardı)")
}

/**
 * Denizcilik Demirleme ve Salma Dairesi Hesabı Veri Modeli
 *
 * Ders Kitabı Formül Sistemi:
 * a² = b² + c²
 *
 * a = Gemi demir zincirine verilen kaloma (metre / kilit)
 * b = Derinlik (metre)
 * c = Loçadan demir yerine olan yatay mesafe [c = √(a² - b²)]
 * d = Köprüüstünden loçaya olan mesafe + Loçadan demir yerine olan yatay mesafe (I. Salma Dairesi)
 * e = Köprüüstünden kıça kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi alternatifi)
 * f = Gemi Boyu kadar olan mesafe + Loçadan demir yerine olan yatay mesafe (II. Salma Dairesi için - Toplam Emniyet Salma Çemberi)
 */
data class AnchorCalculationParams(
  val chainScopeMeters: Double = 137.5, // a: 5 kilit = 137.5 m (1 kilit = 27.5 m)
  val depthMeters: Double = 40.0, // b: 40.0 m
  val shackleStandard: ShackleLengthStandard = ShackleLengthStandard.STANDARD_27_5, // 27.5m veya 25.0m kilit seçimi
  val isAutoCalculateHorizontal: Boolean = true, // c otomatik hesaplama (Pisagor)
  val customHorizontalDistanceMeters: Double? = null, // c manuel girilirse
  val distBridgeToHawseMeters: Double = 35.0, // Köprüüstü -> Loça mesafesi (35 m)
  val distBridgeToSternMeters: Double = 85.0, // Köprüüstü -> Kıç mesafesi (85 m)
  val loaMeters: Double = 120.0, // LOA (Gemi Tam Boyu = 35 + 85 = 120 m)
  val safetyMarginMeters: Double = 0.0, // İlave emniyet marjı (opsiyonel)
  val bottomType: AnchorBottomType = AnchorBottomType.MUD_SAND
)

enum class AnchorBottomType(val displayNameTr: String, val holdingFactorDesc: String, val recommendedScope: String) {
  MUD_SAND("Çamur / Kum (İyi Tutma)", "Yüksek tutma kuvveti (En ideal demirleme zemini)", "4 - 5 x Derinlik"),
  SOFT_MUD("Yumuşak Balçık / Çamur", "Orta tutma kuvveti (Demir derine gömülebilir)", "5 - 6 x Derinlik"),
  HARD_SAND("Sert Kum", "İyi tutma kuvveti, tutunma süresi alabilir", "4 - 5 x Derinlik"),
  GRAVEL_SHELL("Çakıl / Midye Kabuğu", "Düşük/Orta tutma kuvveti (Taramaya dikkat)", "6 - 7 x Derinlik"),
  ROCK_CORAL("Taşlık / Kayaç (Riskli)", "Zayıf tutma, demir takılma veya tarama riski yüksek", "7 - 10 x Derinlik + Nöbet")
}

data class AnchorCalculationResult(
  // a: Gemi demir zinciri ile verilen kaloma
  val a_chainScopeMeters: Double,
  val a_chainScopeShackles: Double, // Kilit (seçilen standarda göre)
  val a_chainScopeFathoms: Double, // Kulaç (1 kulaç = 1.8288 m)
  val shackleStandard: ShackleLengthStandard = ShackleLengthStandard.STANDARD_27_5,

  // b: Derinlik
  val b_depthMeters: Double,
  val b_depthFeet: Double,
  val b_depthFathoms: Double,

  // c: Loçadan demir yerine olan yatay mesafe
  val c_horizontalDistanceMeters: Double,
  val c_horizontalDistanceGomina: Double, // 1 Gomina = 185.2 m

  // d: Köprüüstünden loçaya mesafe + c (1. Salma Dairesi - Köprüüstü / Radar Salma Dairesi)
  val d_firstSwingingCircleMeters: Double,
  val d_firstSwingingCircleGomina: Double,
  val d_firstSwingingCircleNm: Double,

  // e: Köprüüstünden kıça kadar mesafe + c (2. Salma Dairesi alternatif gözlem)
  val e_bridgeSternSwingingCircleMeters: Double,
  val e_bridgeSternSwingingCircleGomina: Double,

  // f: Gemi boyu (LOA) + c (2. Salma Dairesi için - Toplam Emniyet Salma Çemberi)
  val f_secondSwingingCircleMeters: Double,
  val f_secondSwingingCircleGomina: Double,
  val f_secondSwingingCircleNm: Double,
  val f_withSafetyMarginMeters: Double,

  // Matematiksel Ara Adımlar (Kitap Formatı)
  val aSquared: Double,
  val bSquared: Double,
  val cSquared: Double,
  val calculatedShipLengthMeters: Double, // Köprüüstü-Loça + Köprüüstü-Kıç

  // Ek Seyir Değerlendirmeleri
  val scopeRatio: Double, // a / b oranı
  val scopeStatus: ScopeSafetyStatus,
  val distBridgeToHawseMeters: Double,
  val distBridgeToSternMeters: Double,
  val loaMeters: Double,
  val safetyMarginMeters: Double,
  val isChainShorterThanDepth: Boolean
) {
  val formattedA: String get() = String.format(Locale.US, "%.1f m (%.1f Kilit / %.1f Kulaç)", a_chainScopeMeters, a_chainScopeShackles, a_chainScopeFathoms)
  val formattedB: String get() = String.format(Locale.US, "%.1f m (%.1f ft / %.1f Kulaç)", b_depthMeters, b_depthFeet, b_depthFathoms)
  val formattedC: String get() = String.format(Locale.US, "%.1f m (%.2f Gomina)", c_horizontalDistanceMeters, c_horizontalDistanceGomina)
  val formattedD: String get() = String.format(Locale.US, "%.1f m (%.2f Gomina / %.3f NM)", d_firstSwingingCircleMeters, d_firstSwingingCircleGomina, d_firstSwingingCircleNm)
  val formattedE: String get() = String.format(Locale.US, "%.1f m (%.2f Gomina)", e_bridgeSternSwingingCircleMeters, e_bridgeSternSwingingCircleGomina)
  val formattedF: String get() = String.format(Locale.US, "%.1f m (%.2f Gomina / %.3f NM)", f_secondSwingingCircleMeters, f_secondSwingingCircleGomina, f_secondSwingingCircleNm)
}

enum class ScopeSafetyStatus(val labelTr: String, val isSafe: Boolean, val descriptionTr: String) {
  CRITICAL_SHORT("YETERSİZ KALOMA (Kritik Risk)", false, "Kaloma derinliğe eşit veya çok az! Demir kesinlikle tutmayacak ve tarayacaktır."),
  LOW_SCOPE("DÜŞÜK KALOMA (Hafif Hava)", false, "Kaloma oranı 3x-4x altında. Yalnızca çok sakin sularda ve kısa süreli beklemede uygundur."),
  NORMAL_SCOPE("UYGUN / STANDART KALOMA", true, "Standart emniyetli kaloma oranı (4x - 6x derinlik). İyi zemin ve normal hava koşulları için idealdir."),
  HEAVY_WEATHER_SCOPE("FIRTINA / AĞIR DENİZ KALOMASI", true, "Yüksek emniyetli kaloma oranı (> 6x derinlik). Sert rüzgar ve akıntıda demirin tutuşunu azamiye çıkarır.")
}

