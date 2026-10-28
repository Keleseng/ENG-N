package com.example.model

import java.util.Locale
import kotlin.math.abs

data class PortLocation(
  val id: String = "custom_location",
  val name: String = "Seyir Mevkii",
  val country: String = "Türkiye",
  val category: String = "Mevki",
  val latitude: Double = 38.4410,
  val longitude: Double = 27.1438,
  val defaultChartedDepthMeters: Double = 10.0,
  val typicalTideRangeMeters: Double = 0.8,
  val description: String = "Genel Deniz Seyir Mevkii",
  val timeZoneOffsetHours: Double = 3.0
)

object LocationPresets {
  val strategicMarineLocations = listOf(
    PortLocation(
      id = "golcuk_deniz_ana_ussu",
      name = "Gölcük Deniz Ana Üssü (Kocaeli)",
      country = "Türkiye",
      category = "Marmara / Ana Deniz Üssü",
      latitude = 40.7180,
      longitude = 29.8350,
      defaultChartedDepthMeters = 18.0,
      typicalTideRangeMeters = 0.5,
      description = "Türk Deniz Kuvvetleri Ana Deniz Üssü Komutanlığı, Donanma Karargahı ve Gölcük Askeri Tersanesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "aksaz_deniz_ussu",
      name = "Aksaz Deniz Üs Komutanlığı (Marmaris)",
      country = "Türkiye",
      category = "Ege & Akdeniz / Ana Üs",
      latitude = 36.8380,
      longitude = 28.4000,
      defaultChartedDepthMeters = 25.0,
      typicalTideRangeMeters = 0.4,
      description = "Güney Görev Grup Komutanlığı, Ege ve Doğu Akdeniz Ana Harekat ve Harp Gemisi Limanı",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "foca_deniz_ussu",
      name = "Foça Deniz Üs Komutanlığı (İzmir)",
      country = "Türkiye",
      category = "Ege Denizi / Amfibi Görev Grup",
      latitude = 38.6650,
      longitude = 26.7550,
      defaultChartedDepthMeters = 16.0,
      typicalTideRangeMeters = 0.6,
      description = "Amfibi Görev Grup Komutanlığı, Amfibi Deniz Piyade Tugayı ve Çıkarma Gemileri Limanı",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "kdz_eregli_deniz_ussu",
      name = "Kdz. Ereğli Deniz Üs Komutanlığı (Zonguldak)",
      country = "Türkiye",
      category = "Karadeniz / Bölge Komutanlığı",
      latitude = 41.2820,
      longitude = 31.4180,
      defaultChartedDepthMeters = 16.5,
      typicalTideRangeMeters = 0.3,
      description = "Karadeniz Bölge Komutanlığı, Batı Karadeniz Askeri Liman Mendirek ve Harp Gemisi Tesisleri",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "erdek_deniz_ussu",
      name = "Erdek Deniz Üs Komutanlığı (Balıkesir)",
      country = "Türkiye",
      category = "Marmara / Mayın Filosu",
      latitude = 40.3950,
      longitude = 27.7900,
      defaultChartedDepthMeters = 14.0,
      typicalTideRangeMeters = 0.4,
      description = "Mayın Filosu Komutanlığı Ana Deniz Üssü, Mayın Harbi Harekat Merkezi ve İskeleleri",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "canakkale_nara_deniz_ussu",
      name = "Çanakkale / Nara Deniz Üs Komutanlığı",
      country = "Türkiye",
      category = "Boğazlar / Askeri Deniz Üssü",
      latitude = 40.1850,
      longitude = 26.4020,
      defaultChartedDepthMeters = 35.0,
      typicalTideRangeMeters = 0.6,
      description = "Çanakkale Boğaz Komutanlığı, Nara Burnu Askeri Limanı, Denizaltı ve Harp Gemisi İskelesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "iskenderun_deniz_ussu",
      name = "İskenderun Deniz Üs Komutanlığı (Hatay)",
      country = "Türkiye",
      category = "Doğu Akdeniz / Deniz Üssü",
      latitude = 36.5950,
      longitude = 36.1750,
      defaultChartedDepthMeters = 15.0,
      typicalTideRangeMeters = 0.4,
      description = "İskenderun Deniz Üs Komutanlığı, Doğu Akdeniz Güvenlik, Lojistik ve İkmal Askeri İskeleleri",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "mersin_deniz_ussu",
      name = "Mersin Akdeniz Bölge & Askeri Limanı",
      country = "Türkiye",
      category = "Akdeniz / Bölge Komutanlığı",
      latitude = 36.7900,
      longitude = 34.6400,
      defaultChartedDepthMeters = 14.5,
      typicalTideRangeMeters = 0.5,
      description = "Akdeniz Bölge Komutanlığı ve Mersin Askeri Deniz Güvenlik İskeleleri",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "tuzla_askeri_tersane_dho",
      name = "Tuzla Askeri Tersanesi & DHO (İstanbul)",
      country = "Türkiye",
      category = "Marmara / Askeri Tersane & Eğitim",
      latitude = 40.8283,
      longitude = 29.2540,
      defaultChartedDepthMeters = 20.0,
      typicalTideRangeMeters = 0.5,
      description = "İstanbul Askeri Tersane Komutanlığı, Deniz Harp Okulu Limanı ve Askeri Eğitim Mevkii",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "istanbul_kuzey_deniz_saha",
      name = "Kuzey Deniz Saha & Kasımpaşa Askeri Limanı",
      country = "Türkiye",
      category = "İstanbul Boğazı / Saha Komutanlığı",
      latitude = 41.0300,
      longitude = 28.9680,
      defaultChartedDepthMeters = 22.0,
      typicalTideRangeMeters = 0.5,
      description = "Kuzey Deniz Saha Komutanlığı Karargahı ve Haliç / Kasımpaşa Askeri İskelesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "beykoz_anadolu_kavagi_sat_sas",
      name = "Beykoz / Anadolu Kavağı Askeri Limanı (SAT/SAS)",
      country = "Türkiye",
      category = "İstanbul Boğazı / Özel İhtisas Üssü",
      latitude = 41.1780,
      longitude = 29.0880,
      defaultChartedDepthMeters = 28.0,
      typicalTideRangeMeters = 0.5,
      description = "Anadolu Kavağı Askeri Limanı, Kurtarma ve Sualtı Komutanlığı (SAS/SAT) ve Boğaz Girişi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "bartin_deniz_ussu",
      name = "Bartın Deniz Üs Komutanlığı",
      country = "Türkiye",
      category = "Karadeniz / Deniz Üssü",
      latitude = 41.6850,
      longitude = 32.2250,
      defaultChartedDepthMeters = 10.0,
      typicalTideRangeMeters = 0.3,
      description = "Bartın Deniz Üs Komutanlığı, Karadeniz Sahil Güvenlik ve Askeri Karakol İskelesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "surmene_camburnu_deniz_ussu",
      name = "Sürmene / Çamburnu Deniz Üssü (Trabzon)",
      country = "Türkiye",
      category = "Doğu Karadeniz / Yeni Deniz Üssü",
      latitude = 40.9180,
      longitude = 40.2100,
      defaultChartedDepthMeters = 15.0,
      typicalTideRangeMeters = 0.3,
      description = "Türk Deniz Kuvvetleri Doğu Karadeniz Sürmene Çamburnu Deniz Üssü ve Harp Gemisi Limanı",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "izmir_uzunada_leventler",
      name = "İzmir / Uzunada & Leventler Deniz Üssü",
      country = "Türkiye",
      category = "Ege Denizi / Deniz Bölge Üssü",
      latitude = 38.4550,
      longitude = 26.7250,
      defaultChartedDepthMeters = 18.0,
      typicalTideRangeMeters = 0.6,
      description = "Ege Deniz Bölge Komutanlığı, Leventler Limanı ve Uzunada Askeri Tesisleri",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "samsun_sahil_guvenlik_komutanligi",
      name = "Samsun Sahil Güvenlik Karadeniz Bölge Limanı",
      country = "Türkiye",
      category = "Karadeniz / Sahil Güvenlik Askeri İskele",
      latitude = 41.3050,
      longitude = 36.3450,
      defaultChartedDepthMeters = 12.0,
      typicalTideRangeMeters = 0.3,
      description = "Sahil Güvenlik Karadeniz Bölge Komutanlığı Askeri İskelesi ve Karakol Botları",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "bozcaada_limani",
      name = "Bozcaada Limanı & Askeri Gözetleme (Çanakkale)",
      country = "Türkiye",
      category = "Ege Adaları / Ada Limanı",
      latitude = 39.8333,
      longitude = 26.0667,
      defaultChartedDepthMeters = 9.0,
      typicalTideRangeMeters = 0.4,
      description = "Kuzey Ege Stratejik Bozcaada Limanı, Sahil Güvenlik ve Ada Barınağı",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "gokceada_kuzu_limani",
      name = "Gökçeada Kuzu Limanı (Çanakkale)",
      country = "Türkiye",
      category = "Ege Adaları / Ana Ada Limanı",
      latitude = 40.2333,
      longitude = 25.9000,
      defaultChartedDepthMeters = 12.0,
      typicalTideRangeMeters = 0.4,
      description = "Ege Denizi Gökçeada Ana Feribot ve Sahil Güvenlik İskelesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "marmara_adasi_limani",
      name = "Marmara Adası & Saraylar Limanı (Balıkesir)",
      country = "Türkiye",
      category = "Marmara Adaları / Ada Limanı",
      latitude = 40.5833,
      longitude = 27.5500,
      defaultChartedDepthMeters = 15.0,
      typicalTideRangeMeters = 0.4,
      description = "Marmara Denizi Ada Limanı, Mermer Sevkiyat ve Barınma Mevkii",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "buyukada_prens_adalari",
      name = "Büyükada & Prens Adaları (İstanbul)",
      country = "Türkiye",
      category = "Marmara / Prens Adaları",
      latitude = 40.8744,
      longitude = 29.1286,
      defaultChartedDepthMeters = 11.0,
      typicalTideRangeMeters = 0.5,
      description = "İstanbul Prens Adaları Ana Limanı ve Deniz Ulaşım İskelesi",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "kekova_adasi_kalekoy",
      name = "Kekova / Simena Adası & Kaleköy (Antalya)",
      country = "Türkiye",
      category = "Akdeniz Adaları / Doğal Liman",
      latitude = 36.1900,
      longitude = 29.8600,
      defaultChartedDepthMeters = 14.0,
      typicalTideRangeMeters = 0.4,
      description = "Kekova Batık Şehir, Doğal Korunaklı Ada Limanı ve Barınma Alanı",
      timeZoneOffsetHours = 3.0
    ),
    // --- DÜNYA ÇAPINDA STRATEJİK GEÇİTLER & BÜYÜK LİMANLAR ---
    PortLocation(
      id = "piraeus_port",
      name = "Pire Limanı (Atina)",
      country = "Yunanistan",
      category = "Doğu Akdeniz / Ege Limanı",
      latitude = 37.9420,
      longitude = 23.6370,
      defaultChartedDepthMeters = 19.0,
      typicalTideRangeMeters = 0.4,
      description = "Ege ve Doğu Akdeniz ana aktarma limanı",
      timeZoneOffsetHours = 2.0
    ),
    PortLocation(
      id = "suez_canal_port_said",
      name = "Süveyş Kanalı / Port Said",
      country = "Mısır",
      category = "Küresel Stratejik Geçit / Kanal",
      latitude = 31.2625,
      longitude = 32.3056,
      defaultChartedDepthMeters = 24.0,
      typicalTideRangeMeters = 0.8,
      description = "Akdeniz ile Kızıldeniz'i birbirine bağlayan dünya deniz ticaretinin ana stratejik kanalı",
      timeZoneOffsetHours = 2.0
    ),
    PortLocation(
      id = "strait_of_gibraltar",
      name = "Cebelitarık Boğazı (Gibraltar)",
      country = "İngiltere / İspanya",
      category = "Küresel Stratejik Boğaz",
      latitude = 35.9800,
      longitude = -5.6000,
      defaultChartedDepthMeters = 300.0,
      typicalTideRangeMeters = 1.2,
      description = "Akdeniz'in Atlas Okyanusu'na açılan ana stratejik boğazı",
      timeZoneOffsetHours = 1.0
    ),
    PortLocation(
      id = "bab_el_mandeb_djibouti",
      name = "Bâbülmendep Boğazı / Cibuti",
      country = "Cibuti",
      category = "Kızıldeniz Girişi",
      latitude = 11.5950,
      longitude = 43.1480,
      defaultChartedDepthMeters = 180.0,
      typicalTideRangeMeters = 1.5,
      description = "Kızıldeniz ile Hint Okyanusu Aden Körfezi bağlantı boğazı",
      timeZoneOffsetHours = 3.0
    ),
    PortLocation(
      id = "strait_of_hormuz_dubai",
      name = "Hürmüz Boğazı / Dubai (Jebel Ali)",
      country = "Birleşik Arap Emirlikleri",
      category = "Fars Körfezi / Petrol Hub",
      latitude = 25.0083,
      longitude = 55.0617,
      defaultChartedDepthMeters = 17.0,
      typicalTideRangeMeters = 1.8,
      description = "Dünya ham petrol sevkiyatının en kritik deniz geçidi ve Orta Doğu ana limanı",
      timeZoneOffsetHours = 4.0
    ),
    PortLocation(
      id = "english_channel_dover",
      name = "Manş Denizi / Dover & Calais",
      country = "İngiltere / Fransa",
      category = "Kuzey Avrupa Boğazı",
      latitude = 51.1275,
      longitude = 1.3134,
      defaultChartedDepthMeters = 45.0,
      typicalTideRangeMeters = 5.8,
      description = "Kuzey Denizi ile Atlas Okyanusu arasındaki yüksek gelgitli Manş Boğazı",
      timeZoneOffsetHours = 0.0
    ),
    PortLocation(
      id = "rotterdam_europort",
      name = "Rotterdam Europort Limanı",
      country = "Hollanda",
      category = "Avrupa Ana Liman",
      latitude = 51.9550,
      longitude = 4.1350,
      defaultChartedDepthMeters = 24.0,
      typicalTideRangeMeters = 2.2,
      description = "Avrupa'nın en büyük konteyner ve enerji terminali",
      timeZoneOffsetHours = 1.0
    ),
    PortLocation(
      id = "hamburg_port",
      name = "Hamburg Limanı (Elbe Nehri)",
      country = "Almanya",
      category = "Kuzey Avrupa / Ticaret Limanı",
      latitude = 53.5430,
      longitude = 9.9700,
      defaultChartedDepthMeters = 15.0,
      typicalTideRangeMeters = 3.6,
      description = "Almanya'nın ana deniz ticaret kapısı",
      timeZoneOffsetHours = 1.0
    ),
    PortLocation(
      id = "panama_canal_colon",
      name = "Panama Kanalı / Colon - Balboa",
      country = "Panama",
      category = "Küresel Stratejik Geçit / Kanal",
      latitude = 9.3592,
      longitude = -79.9014,
      defaultChartedDepthMeters = 18.0,
      typicalTideRangeMeters = 0.6,
      description = "Atlas Okyanusu ile Büyük Okyanus'u bağlayan kilit dünya kanalı",
      timeZoneOffsetHours = -5.0
    ),
    PortLocation(
      id = "singapore_malacca_strait",
      name = "Singapur / Malaka Boğazı",
      country = "Singapur",
      category = "Küresel Ana Yakıt & Konteyner Hub",
      latitude = 1.290270,
      longitude = 103.851959,
      defaultChartedDepthMeters = 22.0,
      typicalTideRangeMeters = 2.5,
      description = "Hint Okyanusu ile Pasifik'i bağlayan dünyanın en yoğun yakıt ikmal ve konteyner limanı",
      timeZoneOffsetHours = 8.0
    ),
    PortLocation(
      id = "shanghai_yangshan_port",
      name = "Şanghay / Yangshan Derin Su Limanı",
      country = "Çin",
      category = "Doğu Asya Mega Liman",
      latitude = 30.6260,
      longitude = 122.0650,
      defaultChartedDepthMeters = 16.5,
      typicalTideRangeMeters = 4.0,
      description = "Dünyanın en yüksek konteyner hacmine sahip mega limanı",
      timeZoneOffsetHours = 8.0
    ),
    PortLocation(
      id = "tokyo_yokohama_port",
      name = "Tokyo & Yokohama Limanı",
      country = "Japonya",
      category = "Pasifik / Asya Hub",
      latitude = 35.4430,
      longitude = 139.6500,
      defaultChartedDepthMeters = 16.0,
      typicalTideRangeMeters = 2.0,
      description = "Japonya Tokyo Körfezi ana deniz ticaret limanı",
      timeZoneOffsetHours = 9.0
    ),
    PortLocation(
      id = "new_york_nj_port",
      name = "New York & New Jersey Limanı",
      country = "ABD",
      category = "Kuzey Amerika / Doğu Yakası",
      latitude = 40.6680,
      longitude = -74.1350,
      defaultChartedDepthMeters = 15.2,
      typicalTideRangeMeters = 1.6,
      description = "ABD Doğu Yakası'nın en büyük konteyner limanı",
      timeZoneOffsetHours = -5.0
    ),
    PortLocation(
      id = "los_angeles_long_beach_port",
      name = "Los Angeles & Long Beach Limanı",
      country = "ABD",
      category = "Kuzey Amerika / Pasifik Yakası",
      latitude = 33.7400,
      longitude = -118.2700,
      defaultChartedDepthMeters = 16.0,
      typicalTideRangeMeters = 1.8,
      description = "ABD Batı Yakası'nın Pasifik deniz ticaret kapısı",
      timeZoneOffsetHours = -8.0
    ),
    PortLocation(
      id = "santos_port",
      name = "Santos Limanı (Sao Paulo)",
      country = "Brezilya",
      category = "Güney Amerika / Ana Liman",
      latitude = -23.9600,
      longitude = -46.3000,
      defaultChartedDepthMeters = 15.0,
      typicalTideRangeMeters = 1.5,
      description = "Güney Amerika Kıtası'nın en büyük deniz limanı",
      timeZoneOffsetHours = -3.0
    ),
    PortLocation(
      id = "cape_town_port",
      name = "Cape Town / Ümit Burnu Limanı",
      country = "Güney Afrika",
      category = "Afrika / Ümit Burnu",
      latitude = -33.9050,
      longitude = 18.4350,
      defaultChartedDepthMeters = 15.5,
      typicalTideRangeMeters = 1.8,
      description = "Atlas ve Hint Okyanusu kesişim noktası Ümit Burnu limanı",
      timeZoneOffsetHours = 2.0
    ),
    PortLocation(
      id = "sydney_port_jackson",
      name = "Sydney Limanı (Port Jackson)",
      country = "Avustralya",
      category = "Okyanusya / Ana Liman",
      latitude = -33.8568,
      longitude = 151.2153,
      defaultChartedDepthMeters = 14.0,
      typicalTideRangeMeters = 1.5,
      description = "Avustralya Doğu Kıyısı ana deniz limanı",
      timeZoneOffsetHours = 10.0
    )
  )

  val defaultPorts = strategicMarineLocations

  fun formatMarineLatDDM(lat: Double): String {
    val absLat = abs(lat)
    val deg = absLat.toInt()
    val min = (absLat - deg) * 60.0
    val dir = if (lat >= 0) "K" else "G"
    return String.format(Locale.US, "%s %02d° %06.3f'", dir, deg, min)
  }

  fun formatMarineLonDDM(lon: Double): String {
    val absLon = abs(lon)
    val deg = absLon.toInt()
    val min = (absLon - deg) * 60.0
    val dir = if (lon >= 0) "D" else "B"
    return String.format(Locale.US, "%s %03d° %06.3f'", dir, deg, min)
  }

  fun formatMarineDdmCoordinates(lat: Double, lon: Double): String {
    return "${formatMarineLatDDM(lat)} ${formatMarineLonDDM(lon)}"
  }

  /**
   * Deniz GPS Derece Dakika Saniye (DMS) formatı: örn. 41°00'49"K
   */
  fun formatMarineLatDMS(lat: Double, includeSecondsDecimals: Boolean = false): String {
    val absLat = abs(lat)
    val deg = absLat.toInt()
    val minFull = (absLat - deg) * 60.0
    val min = minFull.toInt()
    val sec = (minFull - min) * 60.0
    val dir = if (lat >= 0) "K" else "G"
    return if (includeSecondsDecimals) {
      String.format(Locale.US, "%02d°%02d'%04.1f\"%s", deg, min, sec, dir)
    } else {
      String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, min, kotlin.math.round(sec).toInt(), dir)
    }
  }

  /**
   * Deniz GPS Derece Dakika Saniye (DMS) formatı: örn. 28°58'33"D
   */
  fun formatMarineLonDMS(lon: Double, includeSecondsDecimals: Boolean = false): String {
    val absLon = abs(lon)
    val deg = absLon.toInt()
    val minFull = (absLon - deg) * 60.0
    val min = minFull.toInt()
    val sec = (minFull - min) * 60.0
    val dir = if (lon >= 0) "D" else "B"
    return if (includeSecondsDecimals) {
      String.format(Locale.US, "%03d°%02d'%04.1f\"%s", deg, min, sec, dir)
    } else {
      String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, min, kotlin.math.round(sec).toInt(), dir)
    }
  }

  fun formatMarineDmsCoordinates(lat: Double, lon: Double): String {
    return "${formatMarineLatDMS(lat)}, ${formatMarineLonDMS(lon)}"
  }

  /**
   * DMS (41°00'49"K), DDM (K 41° 00.817') veya ondalık (41.0136) koordinat dizgilerini Double değere ayrıştırır.
   */
  fun parseCoordinateOrDecimal(input: String?): Double? {
    if (input.isNullOrBlank()) return null
    val clean = input.trim()
    // 1. DMS Regex: 41°00'49"K veya 41° 00' 49" K veya 41 00 49 K
    val dmsRegex = Regex("""(\d+)[°\s]+(\d+)['\s]+([\d.]+)["]?\s*([KkGgDdBbNnSsEeWw])?""")
    val match = dmsRegex.find(clean)
    if (match != null && (clean.contains("\"") || (clean.contains("'") && clean.contains("°") && match.groupValues[3].isNotBlank()))) {
      val deg = match.groupValues[1].toDoubleOrNull() ?: 0.0
      val min = match.groupValues[2].toDoubleOrNull() ?: 0.0
      val sec = match.groupValues[3].toDoubleOrNull() ?: 0.0
      val dir = match.groupValues.getOrNull(4)?.uppercase() ?: ""
      var decimal = deg + (min / 60.0) + (sec / 3600.0)
      if (dir == "G" || dir == "S" || dir == "B" || dir == "W" || clean.startsWith("-")) {
        decimal = -abs(decimal)
      }
      return decimal
    }
    // 2. DDM Regex: 41° 00.817' K
    val ddmRegex = Regex("""([KkGgDdBbNnSsEeWw])?\s*(\d+)[°\s]+([\d.]+)[']?\s*([KkGgDdBbNnSsEeWw])?""")
    val ddmMatch = ddmRegex.find(clean)
    if (ddmMatch != null && clean.contains("'") && !clean.contains("\"")) {
      val dir1 = ddmMatch.groupValues[1].uppercase()
      val deg = ddmMatch.groupValues[2].toDoubleOrNull() ?: 0.0
      val min = ddmMatch.groupValues[3].toDoubleOrNull() ?: 0.0
      val dir2 = ddmMatch.groupValues[4].uppercase()
      val dir = if (dir1.isNotBlank()) dir1 else dir2
      var decimal = deg + (min / 60.0)
      if (dir == "G" || dir == "S" || dir == "B" || dir == "W" || clean.startsWith("-")) {
        decimal = -abs(decimal)
      }
      return decimal
    }
    // 3. Standart ondalık: 41.01361 veya 41,01361 (sonda K/G/D/B veya N/S/E/W olabilir)
    val plainWithDir = Regex("""^([+-]?[\d.]+)\s*([KkGgDdBbNnSsEeWw])?$""").find(clean.replace(',', '.'))
    if (plainWithDir != null) {
      var num = plainWithDir.groupValues[1].toDoubleOrNull() ?: return null
      val dir = plainWithDir.groupValues.getOrNull(2)?.uppercase() ?: ""
      if (dir == "G" || dir == "S" || dir == "B" || dir == "W") {
        num = -abs(num)
      }
      return num
    }
    val direct = clean.replace(',', '.').toDoubleOrNull()
    if (direct != null && abs(direct) <= 180.0) {
      return direct
    }
    // Rakam serisi (örn 405012 veya 0291805) kontrolü
    val parsed = parseAndFormatCoordinate(clean, isLatitude = true).first
    if (parsed != null) return parsed
    return parseAndFormatCoordinate(clean, isLatitude = false).first
  }

  /**
   * Sayısal girişleri (örn: 410049, 41 00 49, 41.00.49, 41.0136, 41 00.817)
   * otomatik olarak standart Denizci GPS formatına (DD°MM'SS"K / DDD°MM'SS"D) çevirir ve (Double?, FormattedString?) döner.
   * Enlem için: ilk 2 sayı derece, sonraki 2 sayı dakika, sonraki 2 sayı saniye (2-2-2).
   * Boylam için: ilk 3 sayı derece, sonraki 2 sayı dakika, sonraki 2 sayı saniye (3-2-2).
   */
  fun parseAndFormatCoordinate(input: String?, isLatitude: Boolean): Pair<Double?, String?> {
    if (input.isNullOrBlank()) return Pair(null, null)
    val clean = input.trim()
      .replace('”', '"')
      .replace('“', '"')
      .replace('’', '\'')
      .replace('‘', '\'')
      .replace('′', '\'')
      .replace('″', '"')
      .replace("''", "\"")
      .replace(',', '.')

    val upper = clean.uppercase()
    val isSouthOrWest = upper.contains("G") || upper.contains("S") || upper.contains("B") || upper.contains("W") || clean.startsWith("-")
    val defaultDir = if (isLatitude) {
      if (isSouthOrWest) "G" else "K"
    } else {
      if (isSouthOrWest) "B" else "D"
    }

    val numPart = clean.replace(Regex("""[KkGgDdBbNnSsEeWw\-\+]"""), "").trim()
    if (numPart.isBlank()) return Pair(null, null)

    // 1. Standart DMS formatı (örn: 41°00'49"K veya 41° 00' 49")
    val dmsRegex = Regex("""(\d+)[\s°dD]+(\d+)[\s'′]+([\d.]+)[\s"″]*""")
    val dmsMatch = dmsRegex.find(numPart)
    if (dmsMatch != null) {
      val deg = dmsMatch.groupValues[1].toIntOrNull() ?: 0
      val min = dmsMatch.groupValues[2].toIntOrNull() ?: 0
      val sec = dmsMatch.groupValues[3].toDoubleOrNull() ?: 0.0
      val secInt = kotlin.math.round(sec).toInt()
      var dec = deg + (min / 60.0) + (sec / 3600.0)
      if (isSouthOrWest) dec = -abs(dec)
      val formatted = if (isLatitude) {
        String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, min, secInt, defaultDir)
      } else {
        String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, min, secInt, defaultDir)
      }
      return Pair(dec, formatted)
    }

    // 2. DDM formatı (Derece ve ondalık dakika, örn: 41° 00.817' veya 41 00.817)
    val ddmRegex = Regex("""(\d+)[\s°dD]+([\d.]+)[\s'′]*""")
    val ddmMatch = ddmRegex.find(numPart)
    if (ddmMatch != null && (numPart.contains("°") || numPart.contains("'") || numPart.contains(" "))) {
      val deg = ddmMatch.groupValues[1].toIntOrNull() ?: 0
      val minDec = ddmMatch.groupValues[2].toDoubleOrNull() ?: 0.0
      val minInt = minDec.toInt()
      val sec = kotlin.math.round((minDec - minInt) * 60.0).toInt()
      var dec = deg + (minDec / 60.0)
      if (isSouthOrWest) dec = -abs(dec)
      val formatted = if (isLatitude) {
        String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
      } else {
        String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
      }
      return Pair(dec, formatted)
    }

    // 3. Boşluk, nokta, tire veya iki nokta ile ayrılmış sayılar (örn: 41 00 49 veya 41.00.49 veya 41:00:49)
    val parts = numPart.split(Regex("""[\s:;/\-_]+""")).filter { it.isNotBlank() }
    val dotParts = if (parts.size == 1 && numPart.count { it == '.' } > 1) {
      numPart.split('.').filter { it.isNotBlank() }
    } else parts

    if (dotParts.size >= 2) {
      val pNums = dotParts.mapNotNull { it.toDoubleOrNull() }
      if (pNums.size >= 3) {
        val deg = pNums[0].toInt()
        val min = pNums[1].toInt()
        val sec = kotlin.math.round(pNums[2]).toInt()
        var dec = deg + (min / 60.0) + (sec / 3600.0)
        if (isSouthOrWest) dec = -abs(dec)
        val formatted = if (isLatitude) {
          String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, min, sec, defaultDir)
        } else {
          String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, min, sec, defaultDir)
        }
        return Pair(dec, formatted)
      } else if (pNums.size == 2) {
        val deg = pNums[0].toInt()
        val minDec = pNums[1]
        val minInt = minDec.toInt()
        val sec = kotlin.math.round((minDec - minInt) * 60.0).toInt()
        var dec = deg + (minDec / 60.0)
        if (isSouthOrWest) dec = -abs(dec)
        val formatted = if (isLatitude) {
          String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
        } else {
          String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
        }
        return Pair(dec, formatted)
      }
    }

    // 4. Bitişik Saf Rakamlar:
    // Enlem: ilk 2 sayı derece, sonraki 2 sayı dakika, sonraki 2 sayı saniye (2-2-2)
    // Boylam: ilk 3 sayı derece, sonraki 2 sayı dakika, sonraki 2 sayı saniye (3-2-2)
    val digits = numPart.filter { it.isDigit() }
    if (!numPart.contains('.')) {
      if (isLatitude && digits.length in 2..6) {
        val deg = digits.take(2).toIntOrNull() ?: 0
        val min = if (digits.length >= 4) digits.substring(2, 4).toIntOrNull() ?: 0
                  else if (digits.length == 3) digits.substring(2, 3).toIntOrNull() ?: 0
                  else 0
        val sec = if (digits.length >= 6) digits.substring(4, 6).toIntOrNull() ?: 0
                  else if (digits.length == 5) digits.substring(4, 5).toIntOrNull() ?: 0
                  else 0
        var dec = deg + (min / 60.0) + (sec / 3600.0)
        if (isSouthOrWest) dec = -abs(dec)
        val formatted = String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, min, sec, defaultDir)
        return Pair(dec, formatted)
      } else if (!isLatitude && digits.length in 2..7) {
        // Boylam: ilk 3 hane derece (örn 029 veya 120), 6 hane girilmiş ve ilk 3 hane > 180 ise (örn 285833) başına 0 eklenir (0285833)
        val cleanLonDigits = if (digits.length == 6 && (digits.take(3).toIntOrNull() ?: 0) > 180) "0$digits" else digits
        val deg = if (cleanLonDigits.length >= 3) cleanLonDigits.take(3).toIntOrNull() ?: 0 else cleanLonDigits.toIntOrNull() ?: 0
        val min = if (cleanLonDigits.length >= 5) cleanLonDigits.substring(3, 5).toIntOrNull() ?: 0
                  else if (cleanLonDigits.length == 4) cleanLonDigits.substring(3, 4).toIntOrNull() ?: 0
                  else 0
        val sec = if (cleanLonDigits.length >= 7) cleanLonDigits.substring(5, 7).toIntOrNull() ?: 0
                  else if (cleanLonDigits.length == 6) cleanLonDigits.substring(5, 6).toIntOrNull() ?: 0
                  else 0
        var dec = deg + (min / 60.0) + (sec / 3600.0)
        if (isSouthOrWest) dec = -abs(dec)
        val formatted = String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, min, sec, defaultDir)
        return Pair(dec, formatted)
      }
    }

    // 5. Standart Ondalık Derece (Örn: 41.0136 veya 28.9758)
    val decVal = numPart.toDoubleOrNull()
    if (decVal != null) {
      val absVal = abs(decVal)
      if ((isLatitude && absVal <= 90.0) || (!isLatitude && absVal <= 180.0)) {
        val deg = absVal.toInt()
        val remMin = (absVal - deg) * 60.0
        val minInt = remMin.toInt()
        val sec = kotlin.math.round((remMin - minInt) * 60.0).toInt()
        val dec = if (isSouthOrWest) -absVal else absVal
        val formatted = if (isLatitude) {
          String.format(Locale.US, "%02d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
        } else {
          String.format(Locale.US, "%03d°%02d'%02d\"%s", deg, minInt, sec, defaultDir)
        }
        return Pair(dec, formatted)
      }
    }

    return Pair(null, null)
  }

  fun formatMarineLatitude(lat: Double): String {
    val absLat = abs(lat)
    val deg = absLat.toInt()
    val min = (absLat - deg) * 60.0
    val dir = if (lat >= 0) "N" else "S"
    return String.format(Locale.US, "%02d°%05.2f' %s", deg, min, dir)
  }

  fun formatMarineLongitude(lon: Double): String {
    val absLon = abs(lon)
    val deg = absLon.toInt()
    val min = (absLon - deg) * 60.0
    val dir = if (lon >= 0) "E" else "W"
    return String.format(Locale.US, "%03d°%05.2f' %s", deg, min, dir)
  }

  fun formatMarineCoordinates(lat: Double, lon: Double): String {
    return "${formatMarineLatitude(lat)}, ${formatMarineLongitude(lon)}"
  }

  fun formatDecimalCoordinates(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return String.format(Locale.US, "%.5f°%s, %.5f°%s", abs(lat), latDir, abs(lon), lonDir)
  }

  fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
      kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
      kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return (6371.0 * c) / 1.852
  }

  fun calculateBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val dLon = Math.toRadians(lon2 - lon1)
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2Rad)
    val x = kotlin.math.cos(lat1Rad) * kotlin.math.sin(lat2Rad) -
      kotlin.math.sin(lat1Rad) * kotlin.math.cos(lat2Rad) * kotlin.math.cos(dLon)
    val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    return ((bearing + 360) % 360).toInt()
  }

  fun calculateMinDistanceToPathNm(pLat: Double, pLon: Double, path: List<Coordinate>): Double {
    if (path.isEmpty()) return 0.0
    if (path.size == 1) return calculateDistanceNm(pLat, pLon, path[0].latitude, path[0].longitude)

    var minDistNm = Double.MAX_VALUE
    for (i in 0 until path.size - 1) {
      val p1 = path[i]
      val p2 = path[i + 1]
      for (step in 0..8) {
        val fraction = step / 8.0
        val sampleLat = p1.latitude + (p2.latitude - p1.latitude) * fraction
        val sampleLon = p1.longitude + (p2.longitude - p1.longitude) * fraction
        val dist = calculateDistanceNm(pLat, pLon, sampleLat, sampleLon)
        if (dist < minDistNm) {
          minDistNm = dist
        }
      }
    }
    return if (minDistNm == Double.MAX_VALUE) 0.0 else minDistNm
  }

  fun getNearestPorts(lat: Double, lon: Double, speedKnots: Double = 10.0): List<NearestPortResult> {
    val effectiveSpeed = if (speedKnots > 0.5) speedKnots else 10.0
    return defaultPorts.map { port ->
      val dist = calculateDistanceNm(lat, lon, port.latitude, port.longitude)
      val bearing = calculateBearingDegrees(lat, lon, port.latitude, port.longitude)
      val totalHours = dist / effectiveSpeed
      val hours = totalHours.toInt()
      val minutes = ((totalHours - hours) * 60).toInt()
      val etaStr = if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"
      NearestPortResult(
        port = port,
        distanceNm = dist,
        bearingDegrees = bearing,
        estimatedTimeFormatted = etaStr
      )
    }.sortedBy { it.distanceNm }
  }
}

data class NearestPortResult(
  val port: PortLocation,
  val distanceNm: Double,
  val bearingDegrees: Int,
  val estimatedTimeFormatted: String
)
