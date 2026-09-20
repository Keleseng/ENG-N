package com.example.model

import java.util.Locale

/**
 * Deniz Harita Standartlarına (INT 1 / IHO Bölüm J ve SHOD) uygun Zemin Tutuş Kalitesi
 */
enum class SeabedHoldingQuality(
  val displayNameTr: String,
  val stars: Int,
  val ratingColorHex: Long
) {
  EXCELLENT("Mükemmel Tutuş", 5, 0xFF10B981), // Yeşil
  GOOD("İyi Tutuş", 4, 0xFF0EA5E9),           // Açık Mavi
  MODERATE("Orta Tutuş", 3, 0xFFF59E0B),       // Sarı / Amber
  POOR("Zayıf / Taramaya Açık", 2, 0xFFF97316), // Turuncu
  HAZARDOUS("Riskli / Tehlikeli", 1, 0xFFEF4444) // Kırmızı
}

/**
 * Deniz Haritası Dip Tabiatı Sembolü ve Özellikleri (INT 1 / SHOD Standardı)
 */
data class SeabedChartSymbol(
  val symbol: String,                       // örn: "M.S", "M", "S", "Cy", "R", "Wd"
  val nameTr: String,                       // örn: "Çamur ve Kum"
  val nameEn: String,                       // örn: "Mud and Sand"
  val holdingQuality: SeabedHoldingQuality,
  val matchedAnchorBottomType: AnchorBottomType,
  val recommendedScopeRatio: String,         // örn: "4 - 5 x Derinlik"
  val descriptionTr: String,                 // Detaylı denizcilik ve tutma davranışı
  val seamanshipAdviceTr: String,            // Köprüüstü ve manevra tavsiyesi
  val int1Code: String                       // örn: "J 1 / J 2"
)

/**
 * Deniz Mevkilerinin (Demir Sahaları, Boğazlar, Körfezler) Resmi Dip Tabiatı Bilgileri
 */
data class MarineSeabedLocation(
  val id: String,
  val nameTr: String,                        // örn: "Ahırkapı Demir Sahası"
  val regionTr: String,                      // örn: "Marmara & Boğazlar"
  val chartSymbol: String,                   // örn: "M.S"
  val seabedNameTr: String,                  // örn: "Çamur ve Kum"
  val latitude: Double,
  val longitude: Double,
  val typicalDepthMeters: Double,
  val depthRangeText: String,                // örn: "15 - 35 m"
  val holdingQuality: SeabedHoldingQuality,
  val anchorBottomType: AnchorBottomType,
  val shodChartNumber: String,               // örn: "TR 2921"
  val navigationalNotesTr: String            // Navigasyonel emniyet ve akıntı/hava notları
)

/**
 * Uluslararası Deniz Haritaları (INT 1 / SHOD) Zemin Sembolleri ve Türk Denizleri Mevki Veritabanı
 */
object SeabedNatureDatabase {

  /**
   * INT 1 Bölüm J Deniz Harita Zemin Sembolleri Kataloğu
   */
  val chartSymbols: List<SeabedChartSymbol> = listOf(
    SeabedChartSymbol(
      symbol = "M.S",
      nameTr = "Çamur ve Kum (En İdeal Zemin)",
      nameEn = "Mud & Sand",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      matchedAnchorBottomType = AnchorBottomType.MUD_SAND,
      recommendedScopeRatio = "4 - 5 x Derinlik",
      descriptionTr = "Denizcilikte en yüksek tutma katsayısına sahip zemindir. Çamur elastikiyet sağlarken kum çapanın kaymasını önler.",
      seamanshipAdviceTr = "Standart kaloma (4-5x) yeterlidir. Fırtına beklentisinde 6x derinlik kaloma verilmelidir.",
      int1Code = "INT 1: J 1 / J 2"
    ),
    SeabedChartSymbol(
      symbol = "M",
      nameTr = "Çamur (Mud)",
      nameEn = "Mud",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      matchedAnchorBottomType = AnchorBottomType.MUD_SAND,
      recommendedScopeRatio = "4 - 5 x Derinlik",
      descriptionTr = "Çok iyi tutuş sağlar. Çapa tabana tamamen gömülerek maksimum tutma kuvveti oluşturur.",
      seamanshipAdviceTr = "Demir vira edilirken loçada tazyikli yıkama suyu gerektirir. Ağır çamur loçayı tıkayabilir.",
      int1Code = "INT 1: J 2"
    ),
    SeabedChartSymbol(
      symbol = "soM",
      nameTr = "Yumuşak Çamur / Balçık",
      nameEn = "Soft Mud",
      holdingQuality = SeabedHoldingQuality.GOOD,
      matchedAnchorBottomType = AnchorBottomType.SOFT_MUD,
      recommendedScopeRatio = "5 - 6 x Derinlik",
      descriptionTr = "Organik içerikli yumuşak balçık tabakasıdır. Çapa derine dalar ancak çok kuvvetli rüzgarda yarıp tarayabilir.",
      seamanshipAdviceTr = "Tornistanla çapanın tuttuğu teyit edilmelidir. İlave kaloma tavsiye edilir.",
      int1Code = "INT 1: J 2 (Qual. so)"
    ),
    SeabedChartSymbol(
      symbol = "Cy",
      nameTr = "Kil (Clay)",
      nameEn = "Clay",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      matchedAnchorBottomType = AnchorBottomType.MUD_SAND,
      recommendedScopeRatio = "4 - 5 x Derinlik",
      descriptionTr = "Olağanüstü yüksek tutuş. Çapa kile saplandığında neredeyse hiç taramaz.",
      seamanshipAdviceTr = "Demir alırken ırgat üzerinde aşırı yük binebilir. Gemi başı doğrudan demir üzerine getirilerek vira edilmelidir.",
      int1Code = "INT 1: J 3"
    ),
    SeabedChartSymbol(
      symbol = "Si",
      nameTr = "Silt (İnce Balçık / Alüvyon)",
      nameEn = "Silt",
      holdingQuality = SeabedHoldingQuality.GOOD,
      matchedAnchorBottomType = AnchorBottomType.SOFT_MUD,
      recommendedScopeRatio = "5 - 6 x Derinlik",
      descriptionTr = "Nehir ağızlarında ve lagünlerde görülen çok ince taneli tortul zemin.",
      seamanshipAdviceTr = "Geniş tırnaklı çapalarda iyi sonuç verir, dar tırnaklar batma eğilimindedir.",
      int1Code = "INT 1: J 4"
    ),
    SeabedChartSymbol(
      symbol = "S",
      nameTr = "Kum (Sand)",
      nameEn = "Sand",
      holdingQuality = SeabedHoldingQuality.GOOD,
      matchedAnchorBottomType = AnchorBottomType.HARD_SAND,
      recommendedScopeRatio = "4 - 5 x Derinlik",
      descriptionTr = "Temiz ve homojen kum tabakası. Çapa tırnakları hızla kavrar ve güvenilir tutuş sağlar.",
      seamanshipAdviceTr = "Demir berrak suda kolaylıkla gözle kontrol edilebilir. Zincir temiz gelir.",
      int1Code = "INT 1: J 1"
    ),
    SeabedChartSymbol(
      symbol = "hS",
      nameTr = "Sert Sıkışmış Kum",
      nameEn = "Hard Sand",
      holdingQuality = SeabedHoldingQuality.MODERATE,
      matchedAnchorBottomType = AnchorBottomType.HARD_SAND,
      recommendedScopeRatio = "5 - 6 x Derinlik",
      descriptionTr = "Dalga veya akıntıyla preslenmiş sert zemin. Çapanın zemini ilk yarması biraz zaman alabilir.",
      seamanshipAdviceTr = "Demir atıldıktan sonra yavaş torna ile tırnakların açılıp gömüldüğünden emin olunmalıdır.",
      int1Code = "INT 1: J 1 (Qual. h)"
    ),
    SeabedChartSymbol(
      symbol = "G",
      nameTr = "Çakıl (Gravel)",
      nameEn = "Gravel",
      holdingQuality = SeabedHoldingQuality.POOR,
      matchedAnchorBottomType = AnchorBottomType.GRAVEL_SHELL,
      recommendedScopeRatio = "6 - 7 x Derinlik",
      descriptionTr = "İri taneli taşçık ve çakıl tabakası. Çapa tırnakları arasına çakıl dolup kayma riski yüksektir.",
      seamanshipAdviceTr = "Rüzgar arttığında tarama alarmı açık tutulmalı, mümkünse daha emniyetli demir sahasına geçilmelidir.",
      int1Code = "INT 1: J 6"
    ),
    SeabedChartSymbol(
      symbol = "Sh",
      nameTr = "Midye Kabuğu / Kavkı",
      nameEn = "Shells",
      holdingQuality = SeabedHoldingQuality.POOR,
      matchedAnchorBottomType = AnchorBottomType.GRAVEL_SHELL,
      recommendedScopeRatio = "6 - 7 x Derinlik",
      descriptionTr = "Deniz kabuğu kırıkları içeren gevşek tabaka. Tırnaklar kavrama sağlamakta zorlanabilir.",
      seamanshipAdviceTr = "Düşük tutuş marjı. Kaloma en az 6x verilmeli, radar çemberi sürekli izlenmelidir.",
      int1Code = "INT 1: J 11"
    ),
    SeabedChartSymbol(
      symbol = "P",
      nameTr = "Yuvarlak Çakıl Taşları (Pebbles)",
      nameEn = "Pebbles",
      holdingQuality = SeabedHoldingQuality.POOR,
      matchedAnchorBottomType = AnchorBottomType.GRAVEL_SHELL,
      recommendedScopeRatio = "6 - 7 x Derinlik",
      descriptionTr = "Akıntıyla yuvarlanmış taşlar. Çapanın ağırlığı yeterli sürtünme üretmeyebilir.",
      seamanshipAdviceTr = "Zorunlu kalınmadıkça uzun süreli demirleme önerilmez.",
      int1Code = "INT 1: J 7"
    ),
    SeabedChartSymbol(
      symbol = "St",
      nameTr = "İri Taşlık Taban",
      nameEn = "Stones",
      holdingQuality = SeabedHoldingQuality.POOR,
      matchedAnchorBottomType = AnchorBottomType.ROCK_CORAL,
      recommendedScopeRatio = "7 - 8 x Derinlik",
      descriptionTr = "Büyük ve dağınık taşlar. Çapa tırnakları taşların arasından kayarak aniden taratabilir.",
      seamanshipAdviceTr = "Ağır demir ve uzun zincir gerektirir, demir başında çift vardiya tutulmalıdır.",
      int1Code = "INT 1: J 8"
    ),
    SeabedChartSymbol(
      symbol = "R",
      nameTr = "Kaya / Taşlık (Tehlikeli)",
      nameEn = "Rock",
      holdingQuality = SeabedHoldingQuality.HAZARDOUS,
      matchedAnchorBottomType = AnchorBottomType.ROCK_CORAL,
      recommendedScopeRatio = "7 - 10 x Derinlik + Nöbet",
      descriptionTr = "Sert kaya tabanı. Çapa tutunamaz, zincir kayalara sürtünerek aşınabilir veya demir takılıp kopabilir.",
      seamanshipAdviceTr = "Demir kurtarma teli (şimal) bağlanmadan demir atılmamalıdır. Mümkünse kaçınılmalıdır.",
      int1Code = "INT 1: J 9"
    ),
    SeabedChartSymbol(
      symbol = "Co",
      nameTr = "Mercan (Coral)",
      nameEn = "Coral",
      holdingQuality = SeabedHoldingQuality.HAZARDOUS,
      matchedAnchorBottomType = AnchorBottomType.ROCK_CORAL,
      recommendedScopeRatio = "8 - 10 x Derinlik",
      descriptionTr = "Mercan resifi tabanı. Hem çapa tutmaz hem de zincir ve çapa resifleri geri dönülemez biçimde kırar.",
      seamanshipAdviceTr = "Uluslararası denizcilik hukuku gereği ekolojik olarak demirleme kesinlikle yasaktır veya kısıtlıdır.",
      int1Code = "INT 1: J 10"
    ),
    SeabedChartSymbol(
      symbol = "Wd",
      nameTr = "Erişte / Deniz Yosunu",
      nameEn = "Weed / Seagrass",
      holdingQuality = SeabedHoldingQuality.HAZARDOUS,
      matchedAnchorBottomType = AnchorBottomType.SOFT_MUD,
      recommendedScopeRatio = "6 - 7 x Derinlik",
      descriptionTr = "Kalın deniz eriştesi (Posidonia) tabakası. Çapa yosunun üzerinde kayar, tırnaklar toprağa saplanamaz.",
      seamanshipAdviceTr = "Çapanın yosun köklerini delip altındaki çamura oturduğu tornistan testi ile kesinleştirilmelidir.",
      int1Code = "INT 1: J 13"
    )
  )

  /**
   * Türk Denizleri ve Çevre Sulardaki Stratejik Demirleme Sahaları ve Resmi Dip Tabiatları
   */
  val strategicSeabedLocations: List<MarineSeabedLocation> = listOf(
    // ══════════════════════════════════════════════════════════════════════
    // MARMARA & TÜRK BOĞAZLARI
    // ══════════════════════════════════════════════════════════════════════
    MarineSeabedLocation(
      id = "ahirkapi_anchorage",
      nameTr = "Ahırkapı Demir Sahası (İstanbul)",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M.S",
      seabedNameTr = "Çamur ve İnce Kum",
      latitude = 40.9850,
      longitude = 28.9800,
      typicalDepthMeters = 24.0,
      depthRangeText = "16 - 32 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 2923",
      navigationalNotesTr = "İstanbul Boğazı güney girişi A, B, C demirleme sahaları. Mükemmel tutuş sağlar. Boğaz çıkışındaki akıntıya ve lodos soluğanına dikkat edilmelidir."
    ),
    MarineSeabedLocation(
      id = "kartal_kumcular_anchorage",
      nameTr = "Kartal & Kumcular Demir Sahası",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M.Sh",
      seabedNameTr = "Çamur ve Midye Kavkısı",
      latitude = 40.8750,
      longitude = 29.1900,
      typicalDepthMeters = 18.0,
      depthRangeText = "12 - 25 m",
      holdingQuality = SeabedHoldingQuality.GOOD,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 2921",
      navigationalNotesTr = "Adalar arkasında lodos havalarında korunaklı iyi demir sahasıdır."
    ),
    MarineSeabedLocation(
      id = "golcuk_tupras_anchorage",
      nameTr = "Gölcük & İzmit Körfezi Demir Sahası",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "soM",
      seabedNameTr = "Yumuşak Çamur ve Balçık",
      latitude = 40.7300,
      longitude = 29.8100,
      typicalDepthMeters = 22.0,
      depthRangeText = "15 - 30 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.SOFT_MUD,
      shodChartNumber = "TR 293",
      navigationalNotesTr = "Tüpraş ve Gölcük Donanma üssü civarı. Ağır fırtınalara karşı kapalı havuz gibidir. Balçık çapa tutuşu mükemmeldir."
    ),
    MarineSeabedLocation(
      id = "buyukdere_anchorage",
      nameTr = "Büyükdere Koyu Demir Yeri (Boğaziçi)",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M",
      seabedNameTr = "Koyu Koyu Çamur",
      latitude = 41.1600,
      longitude = 29.0450,
      typicalDepthMeters = 16.0,
      depthRangeText = "10 - 22 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 2921",
      navigationalNotesTr = "İstanbul Boğazı içi acil ve emniyet sığınma mevkii. Boğaz üst akıntısı koy içinde zayıflar."
    ),
    MarineSeabedLocation(
      id = "turkeli_kilyos_anchorage",
      nameTr = "Türkeli & Kilyos Demir Sahası",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "S",
      seabedNameTr = "Kum ve İnce Çamur",
      latitude = 41.2600,
      longitude = 29.1100,
      typicalDepthMeters = 30.0,
      depthRangeText = "20 - 45 m",
      holdingQuality = SeabedHoldingQuality.GOOD,
      anchorBottomType = AnchorBottomType.HARD_SAND,
      shodChartNumber = "TR 181",
      navigationalNotesTr = "Karadeniz girişi bekleme sahası. Kuzeyli sert rüzgarlarda ağır soluğan alır. Ekstra kaloma şarttır."
    ),
    MarineSeabedLocation(
      id = "karanlik_liman_anchorage",
      nameTr = "Karanlık Liman Demir Sahası (Çanakkale)",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M.S",
      seabedNameTr = "Çamur ve İnce Kum",
      latitude = 40.0250,
      longitude = 26.2950,
      typicalDepthMeters = 25.0,
      depthRangeText = "18 - 36 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 212",
      navigationalNotesTr = "Çanakkale Boğazı güney girişi. Kuvvetli boğaz çıkış akıntısına karşı olağanüstü sağlam tutuş tabanı."
    ),
    MarineSeabedLocation(
      id = "sevketiye_anchorage",
      nameTr = "Şevketiye Demir Sahası (Gelibolu)",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M",
      seabedNameTr = "Çamur",
      latitude = 40.3800,
      longitude = 26.8300,
      typicalDepthMeters = 28.0,
      depthRangeText = "20 - 38 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 2121",
      navigationalNotesTr = "Çanakkale Boğazı kuzey girişi bekleme sahası. Emniyetli ve korunaklı."
    ),
    MarineSeabedLocation(
      id = "bandirma_anchorage",
      nameTr = "Bandırma Körfezi Demir Sahası",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M.Sh",
      seabedNameTr = "Çamur ve Kavkı",
      latitude = 40.3800,
      longitude = 27.9900,
      typicalDepthMeters = 18.0,
      depthRangeText = "14 - 28 m",
      holdingQuality = SeabedHoldingQuality.GOOD,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 294",
      navigationalNotesTr = "Kuzey rüzgarlarına açık ancak taban tutuşu kuvvetlidir."
    ),
    MarineSeabedLocation(
      id = "asyaport_tekirdag_anchorage",
      nameTr = "Tekirdağ & Asyaport Dış Demir Sahası",
      regionTr = "Marmara & Boğazlar",
      chartSymbol = "M.S",
      seabedNameTr = "Kum ve Çamur",
      latitude = 40.9400,
      longitude = 27.5300,
      typicalDepthMeters = 20.0,
      depthRangeText = "15 - 32 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 291",
      navigationalNotesTr = "Geniş manevra alanı, konteyner gemileri için ideal demirleme tabiatı."
    ),

    // ══════════════════════════════════════════════════════════════════════
    // EGE DENİZİ
    // ══════════════════════════════════════════════════════════════════════
    MarineSeabedLocation(
      id = "izmir_pelikan_anchorage",
      nameTr = "İzmir Körfezi & Pelikan Bankı",
      regionTr = "Ege Denizi",
      chartSymbol = "soM",
      seabedNameTr = "Yumuşak Balçık Çamur",
      latitude = 38.4500,
      longitude = 27.0800,
      typicalDepthMeters = 14.0,
      depthRangeText = "10 - 18 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.SOFT_MUD,
      shodChartNumber = "TR 2212",
      navigationalNotesTr = "Alsancak Limanı dış demir mevkii. Taban derin balçık olup çapanın tutuş kuvveti çok yüksektir."
    ),
    MarineSeabedLocation(
      id = "nemrut_bay_anchorage",
      nameTr = "Nemrut Körfezi Demir Sahası (Aliağa)",
      regionTr = "Ege Denizi",
      chartSymbol = "M.S",
      seabedNameTr = "Çamur ve İnce Kum",
      latitude = 38.7700,
      longitude = 26.9100,
      typicalDepthMeters = 28.0,
      depthRangeText = "18 - 40 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 215",
      navigationalNotesTr = "Tüpraş Aliağa ve Nemrut Limanları dış demirleme alanı. Ağır kargo gemileri için korunaklı tabiat."
    ),
    MarineSeabedLocation(
      id = "bodrum_gulluk_anchorage",
      nameTr = "Güllük Körfezi Demir Sahası (Maden/Yat)",
      regionTr = "Ege Denizi",
      chartSymbol = "M.S",
      seabedNameTr = "Çamur ve Kum",
      latitude = 37.2300,
      longitude = 27.5600,
      typicalDepthMeters = 20.0,
      depthRangeText = "12 - 32 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 224",
      navigationalNotesTr = "Maden ihracat gemileri ve transit yatlar için korunaklı derin zemin."
    ),
    MarineSeabedLocation(
      id = "gocek_fethiye_anchorage",
      nameTr = "Fethiye & Göcek Körfezi Koyları",
      regionTr = "Ege Denizi",
      chartSymbol = "M.Wd",
      seabedNameTr = "Çamur ve Erişte (Yosun)",
      latitude = 36.6500,
      longitude = 28.9800,
      typicalDepthMeters = 15.0,
      depthRangeText = "8 - 25 m",
      holdingQuality = SeabedHoldingQuality.MODERATE,
      anchorBottomType = AnchorBottomType.SOFT_MUD,
      shodChartNumber = "TR 312",
      navigationalNotesTr = "Koy tabanlarında Posidonia deniz eriştesi yoğundur. Çapanın yosun tabakasını delip alttaki çamura gömüldüğünden emin olunmalıdır."
    ),
    MarineSeabedLocation(
      id = "marmaris_icmeler_anchorage",
      nameTr = "Marmaris Limanı & İçmeler Demir Mevkii",
      regionTr = "Ege Denizi",
      chartSymbol = "soM",
      seabedNameTr = "Yumuşak Balçık Çamur",
      latitude = 36.8300,
      longitude = 28.2700,
      typicalDepthMeters = 18.0,
      depthRangeText = "12 - 26 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.SOFT_MUD,
      shodChartNumber = "TR 311",
      navigationalNotesTr = "Ege'nin en korunaklı doğal limanı. Her havada emniyetli demirleme."
    ),

    // ══════════════════════════════════════════════════════════════════════
    // AKDENİZ
    // ══════════════════════════════════════════════════════════════════════
    MarineSeabedLocation(
      id = "mersin_outer_anchorage",
      nameTr = "Mersin Limanı Dış Demir Sahası",
      regionTr = "Akdeniz",
      chartSymbol = "fS.M",
      seabedNameTr = "İnce Kum ve Çamur",
      latitude = 36.7800,
      longitude = 34.6800,
      typicalDepthMeters = 19.0,
      depthRangeText = "14 - 30 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 331",
      navigationalNotesTr = "Geniş demirleme sahası. Lodos havalarında açık deniz soluğanı artar; 5-6 kilit kaloma önerilir."
    ),
    MarineSeabedLocation(
      id = "iskenderun_dortyol_anchorage",
      nameTr = "İskenderun & Dörtyol Demir Sahası",
      regionTr = "Akdeniz",
      chartSymbol = "M",
      seabedNameTr = "Koyu Çamur",
      latitude = 36.6300,
      longitude = 36.1400,
      typicalDepthMeters = 25.0,
      depthRangeText = "16 - 35 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 334",
      navigationalNotesTr = "Körfez içi Yarıkkaya fırtınalarına karşı çamur zemin çok yüksek tutuş sağlar."
    ),
    MarineSeabedLocation(
      id = "antalya_bay_anchorage",
      nameTr = "Antalya Körfezi & Liman Açıkları",
      regionTr = "Akdeniz",
      chartSymbol = "S.G",
      seabedNameTr = "Kum ve Çakıl (Kıyı Taşlık)",
      latitude = 36.8400,
      longitude = 30.6200,
      typicalDepthMeters = 26.0,
      depthRangeText = "18 - 45 m",
      holdingQuality = SeabedHoldingQuality.MODERATE,
      anchorBottomType = AnchorBottomType.GRAVEL_SHELL,
      shodChartNumber = "TR 321",
      navigationalNotesTr = "Derinlik aniden düşer. Kıyıya yakın yerlerde kayaç zeminler (R) bulunabileceğinden demir atılacak nokta haritadan dikkatle incelenmelidir."
    ),

    // ══════════════════════════════════════════════════════════════════════
    // KARADENİZ
    // ══════════════════════════════════════════════════════════════════════
    MarineSeabedLocation(
      id = "kzk_eregli_anchorage",
      nameTr = "Karadeniz Ereğli Demir Sahası (Erdemir)",
      regionTr = "Karadeniz",
      chartSymbol = "M",
      seabedNameTr = "Koyu Gri Çamur",
      latitude = 41.2700,
      longitude = 31.4000,
      typicalDepthMeters = 22.0,
      depthRangeText = "16 - 32 m",
      holdingQuality = SeabedHoldingQuality.EXCELLENT,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 113",
      navigationalNotesTr = "Batı Karadeniz'in en emniyetli liman sahası. Çamur taban çapanın kaymasına izin vermez."
    ),
    MarineSeabedLocation(
      id = "samsun_outer_anchorage",
      nameTr = "Samsun Limanı Dış Demir Sahası",
      regionTr = "Karadeniz",
      chartSymbol = "M.fS",
      seabedNameTr = "Çamur ve İnce Kum",
      latitude = 41.3200,
      longitude = 36.3700,
      typicalDepthMeters = 16.0,
      depthRangeText = "12 - 25 m",
      holdingQuality = SeabedHoldingQuality.GOOD,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 131",
      navigationalNotesTr = "Kuzey ve kuzeydoğu fırtınalarında sert dalga alır. Demir taramasına karşı sürekli radar gözetimi gerekir."
    ),
    MarineSeabedLocation(
      id = "trabzon_anchorage",
      nameTr = "Trabzon Limanı Dış Demir Sahası",
      regionTr = "Karadeniz",
      chartSymbol = "S.M",
      seabedNameTr = "Kum ve Çamur",
      latitude = 41.0100,
      longitude = 39.7500,
      typicalDepthMeters = 20.0,
      depthRangeText = "14 - 30 m",
      holdingQuality = SeabedHoldingQuality.GOOD,
      anchorBottomType = AnchorBottomType.MUD_SAND,
      shodChartNumber = "TR 141",
      navigationalNotesTr = "Doğu Karadeniz geçiş demirleme sahası. Güvenli tutuş sağlar."
    )
  )
}
