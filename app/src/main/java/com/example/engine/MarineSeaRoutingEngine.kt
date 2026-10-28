package com.example.engine

import com.example.model.Coordinate
import com.example.model.LocationPresets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deniz Rotası Dönüş Noktası (DN - Waypoint) Modeli.
 * Tüm dönüş noktaları %100 deniz üzerinde yer alır ve her birinin kendi ETA bilgisi bulunur.
 */
data class MarineWaypoint(
  val id: String,                    // Örn: "DN1", "DN2"
  val name: String,                  // Örn: "Dönüş Noktası 1 (DN1)"
  val description: String,           // Örn: "Tuzla Burnu Güneybatı Açıkları"
  val latitude: Double,
  val longitude: Double,
  val legDistanceNm: Double = 0.0,
  val legBearingDegrees: Int = 0,
  val nextLegBearingDegrees: Int = 0,
  val cumulativeDistanceNm: Double = 0.0,
  val cumulativeMinutes: Int = 0,
  val durationStr: String = "",
  val etaTimeStr: String = ""
)

/**
 * Deniz Rotası Seyir Bacağı (Leg) Modeli.
 */
data class MarineRouteLeg(
  val fromName: String,
  val toName: String,
  val fromLat: Double,
  val fromLon: Double,
  val toLat: Double,
  val toLon: Double,
  val distanceNm: Double,
  val bearingDegrees: Int,
  val legMinutes: Int,
  val etaTimeStr: String = ""
)

/**
 * Emniyetli Deniz Rotası ve ETA Sonuç Paketi.
 */
data class MarineSeaRoute(
  val originLat: Double,
  val originLon: Double,
  val destLat: Double,
  val destLon: Double,
  val destinationName: String,
  val isSeaRouteWithWaypoints: Boolean,
  val waypoints: List<MarineWaypoint>,
  val legs: List<MarineRouteLeg>,
  val totalDistanceNm: Double,
  val directDistanceNm: Double,
  val speedKnots: Double,
  val totalMinutes: Int,
  val durationStr: String,
  val etaTimeStr: String,
  val fullEtaDateStr: String,
  val pathCoordinates: List<Coordinate>,
  val initialBearingDegrees: Int = 0,
  val isRejectedLandTarget: Boolean = false,
  val rejectionReason: String? = null
)

/**
 * %100 Deniz Üzerinden Rota ve Dönüş Noktası (DN) Planlama Motoru.
 *
 * Karadan geçen rotaları kesin olarak engeller:
 * 1. Haritada karaya tıklanırsa (iskele toleransı dışı) rotayı reddeder ("Gemiler karadan gidemez").
 * 2. Boğazlar, burunlar, yarımadalar ve adalar etrafında doğrulanmış deniz koridoru grafı +
 *    yerel koy/adalar için Any-Angle Deniz Theta* algoritmasını birlikte kullanır.
 * 3. Her dönüş noktası (DN1, DN2...) için ayrı mesafe, tavsiye pusula rotası ve varış ETA'sı üretir.
 */
object MarineSeaRoutingEngine {

  private class SeaNode(
    val id: String,
    val name: String,
    val desc: String,
    val lat: Double,
    val lon: Double,
    val straitChannel: Boolean = false,
    val explicitNeighbors: List<String> = emptyList()
  )

  private fun n(
    id: String,
    name: String,
    desc: String,
    lat: Double,
    lon: Double,
    strait: Boolean = false,
    vararg nb: String
  ): SeaNode {
    val safe = MarineLandMask.snapToNearestSeaWater(lat, lon, clearanceNm = if (strait) 0.04 else 0.08)
    return SeaNode(id, name, desc, safe.latitude, safe.longitude, strait, nb.toList())
  }

  // Doğrulanmış Açık Deniz ve Boğaz Merkez Hattı Seyir Düğümleri
  private val seaNodes: Map<String, SeaNode> by lazy {
    listOf(
      // --- İZMİT KÖRFEZİ TSS & TUZLA & PRENS ADALARI KANALLARI ---
      n("TUZLA_INNER", "Tuzla Tersane Koyu", "Tuzla İç Liman Yaklaşma Kanalı", 40.8320, 29.2680, false, "TUZLA_WEST"),
      n("TUZLA_WEST", "Tuzla Batı Yaklaşma", "Pendik - Tuzla Kıyı Seyir Koridoru", 40.8250, 29.2380, false, "PENDIK_OUTER", "TUZLA_INNER", "TUZLA_CAPE_SW", "ADALAR_SOUTH"),
      n("TUZLA_CAPE_SW", "Tuzla Burnu Şamandırası", "DHO Burnu Güneybatı Seyir Şeridi", 40.7960, 29.2420, false, "TUZLA_WEST", "TUZLA_SOUTH", "YALOVA_OUTER", "ADALAR_SOUTH"),
      n("TUZLA_SOUTH", "Tuzla Güney Koridoru", "Tuzla Marina & İncir Adası Açıkları", 40.7830, 29.2880, false, "TUZLA_CAPE_SW", "BAYRAMOGLU_OUTER", "YALOVA_OUTER"),
      n("BAYRAMOGLU_OUTER", "Bayramoğlu Seyir Hattı", "Bayramoğlu Yarımadası Güney Koridoru", 40.7640, 29.3180, false, "TUZLA_SOUTH", "DARICA_OUTER", "YALOVA_OUTER"),
      n("DARICA_OUTER", "Darıca Yelkenkaya TSS", "İzmit Körfezi Batı TSS Girişi", 40.7420, 29.3520, false, "BAYRAMOGLU_OUTER", "ESKIHISAR_CH", "YALOVA_OUTER"),
      n("ESKIHISAR_CH", "Eskihisar - Topçular TSS", "İzmit Körfezi Dış Havza TSS Hattı", 40.7460, 29.4250, false, "DARICA_OUTER", "OSMANGAZI_BRG"),
      n("OSMANGAZI_BRG", "Dilburnu / Osmangazi TSS", "Osmangazi Köprüsü Merkez Seyir Kanalı", 40.7520, 29.5150, true, "ESKIHISAR_CH", "KARAMURSEL_CH"),
      n("KARAMURSEL_CH", "Hereke - Karamürsel TSS", "İzmit Körfezi Orta Havza TSS Ekseni", 40.7430, 29.6350, false, "OSMANGAZI_BRG", "KORFEZ_DERINCE"),
      n("KORFEZ_DERINCE", "Körfez - Derince TSS", "Tüpraş & Derince Ana Seyir Kanalı", 40.7390, 29.7750, false, "KARAMURSEL_CH", "GOLCUK_APP", "IZMIT_INNER"),
      n("GOLCUK_APP", "Gölcük Yaklaşma Kanalı", "Gölcük Deniz Ana Üssü Emniyet Hattı", 40.7280, 29.8320, false, "KORFEZ_DERINCE", "IZMIT_INNER"),
      n("IZMIT_INNER", "İzmit Doğu Havzası", "İzmit İç Körfez Seyir Sahası", 40.7450, 29.9150, false, "KORFEZ_DERINCE", "GOLCUK_APP"),

      n("PENDIK_OUTER", "Pendik / Kartal Koridoru", "Anadolu Yakası Deniz Ulaşım Hattı", 40.8580, 29.2150, false, "TUZLA_WEST", "MALTEPE_CH", "ADALAR_SOUTH"),
      n("MALTEPE_CH", "Maltepe - Büyükada Kuzey Kanalı", "Prens Adaları Doğu İç Geçidi", 40.8920, 29.1450, false, "PENDIK_OUTER", "ADALAR_NORTH", "HEYBELI_CH"),
      n("HEYBELI_CH", "Heybeliada - Burgazada Kuzeyi", "Prens Adaları Orta Yaklaşma Kanalı", 40.8960, 29.0850, false, "MALTEPE_CH", "ADALAR_NORTH"),
      n("ADALAR_SOUTH", "Adalar Güney TSS", "Prens Adaları Güney Ana Seyir Koridoru", 40.8220, 29.1150, false, "TUZLA_WEST", "TUZLA_CAPE_SW", "PENDIK_OUTER", "SIVRIADA_TSS", "YALOVA_OUTER", "CINARCIK_OUTER"),
      n("SIVRIADA_TSS", "Sivriada Batı TSS", "Doğu Marmara & Ahırkapı Güney TSS Kavşağı", 40.8550, 28.9300, false, "ADALAR_SOUTH", "ADALAR_WEST", "AHIRKAPI", "MARMARA_EAST", "CINARCIK_OUTER"),
      n("ADALAR_WEST", "Kınalıada Kuzeybatı Kanalı", "Prens Adaları Batı Yaklaşma Hattı", 40.9220, 29.0200, false, "SIVRIADA_TSS", "ADALAR_NORTH", "AHIRKAPI", "FENERBAHCE_OUT"),
      n("ADALAR_NORTH", "Bostancı İç Seyir Kanalı", "Bostancı - Adalar Kuzey Koridoru", 40.9300, 29.0750, false, "MALTEPE_CH", "HEYBELI_CH", "ADALAR_WEST", "FENERBAHCE_OUT"),
      n("FENERBAHCE_OUT", "Fenerbahçe Feneri Açıkları", "Kalamış & Fenerbahçe Yaklaşma Hattı", 40.9550, 29.0200, false, "ADALAR_NORTH", "ADALAR_WEST", "KADIKOY_MODA", "AHIRKAPI"),
      n("KADIKOY_MODA", "Kadıköy / Haydarpaşa Kanalı", "Moda - Haydarpaşa Emniyet Koridoru", 40.9820, 29.0050, false, "FENERBAHCE_OUT", "AHIRKAPI", "BOS_SARAYBURNU"),
      n("AHIRKAPI", "Ahırkapı TSS Kavşağı", "İstanbul Boğazı Güney Trafik Ayrım Düzeni", 40.9850, 28.9920, false, "KADIKOY_MODA", "FENERBAHCE_OUT", "ADALAR_WEST", "SIVRIADA_TSS", "YENIKAPI_OUT", "BOS_SARAYBURNU"),

      // --- İSTANBUL BOĞAZI (BOSPHORUS MERKEZ KANAL ZİNCİRİ) ---
      n("BOS_SARAYBURNU", "Sarayburnu / Kız Kulesi", "İstanbul Boğazı Güney Giriş Kanalı", 41.0175, 28.9965, true, "AHIRKAPI", "KADIKOY_MODA", "BOS_HALIC", "BOS_DOLMABAHCE"),
      n("BOS_HALIC", "Haliç / Karaköy Ağzı", "Galata & Kasımpaşa Yaklaşımı", 41.0215, 28.9845, true, "BOS_SARAYBURNU"),
      n("BOS_DOLMABAHCE", "Dolmabahçe / Üsküdar", "Beşiktaş - Üsküdar Boğaz Hattı", 41.0315, 29.0065, true, "BOS_SARAYBURNU", "BOS_BRIDGE_15T"),
      n("BOS_BRIDGE_15T", "15 Temmuz Köprüsü", "Ortaköy - Beylerbeyi Geçişi", 41.0445, 29.0335, true, "BOS_DOLMABAHCE", "BOS_KURUCESME"),
      n("BOS_KURUCESME", "Kuruçeşme / Çengelköy", "Çengelköy Açıkları Merkez Hat", 41.0560, 29.0440, true, "BOS_BRIDGE_15T", "BOS_AKINTIBURNU"),
      n("BOS_AKINTIBURNU", "Arnavutköy / Vaniköy", "Akıntıburnu Manevra Koridoru", 41.0665, 29.0492, true, "BOS_KURUCESME", "BOS_KANDILLI"),
      n("BOS_KANDILLI", "Bebek / Kandilli Dönüşü", "Kandilli Burnu Açıkları Merkez Kanal", 41.0760, 29.0508, true, "BOS_AKINTIBURNU", "BOS_HISARLAR"),
      n("BOS_HISARLAR", "Rumeli & Anadolu Hisarı", "Boğaz En Dar Geçit Merkez Hattı", 41.0835, 29.0612, true, "BOS_KANDILLI", "BOS_FSM_BRIDGE"),
      n("BOS_FSM_BRIDGE", "FSM Köprüsü Geçişi", "Fatih Sultan Mehmet Köprüsü Altı", 41.0920, 29.0608, true, "BOS_HISARLAR", "BOS_KANLICA"),
      n("BOS_KANLICA", "Emirgan / Kanlıca", "Kanlıca Burnu Dönüş Mevkii", 41.1015, 29.0602, true, "BOS_FSM_BRIDGE", "BOS_PASABAHCE"),
      n("BOS_PASABAHCE", "İstinye / Çubuklu", "İstinye - Çubuklu Orta Kanalı", 41.1120, 29.0720, true, "BOS_KANLICA", "BOS_YENIKOY"),
      n("BOS_YENIKOY", "Yeniköy Dönüş Mevkii", "Yeniköy Köybaşı Burnu Doğu Kanalı", 41.1225, 29.0805, true, "BOS_PASABAHCE", "BOS_TARABYA"),
      n("BOS_TARABYA", "Tarabya / Beykoz", "Tarabya - Beykoz Açıkları", 41.1380, 29.0715, true, "BOS_YENIKOY", "BOS_SELVIBURNU"),
      n("BOS_SELVIBURNU", "Kireçburnu / Selvi Burnu", "Umuryeri Sığlığı Batı Geçidi", 41.1505, 29.0635, true, "BOS_TARABYA", "BOS_SARIYER"),
      n("BOS_SARIYER", "Sarıyer / Büyükdere", "Sarıyer Dönüş Mevkii", 41.1645, 29.0645, true, "BOS_SELVIBURNU", "BOS_KAVAKLAR"),
      n("BOS_KAVAKLAR", "Rumeli & Anadolu Kavağı", "Kavaklar Boğaz Geçidi", 41.1775, 29.0815, true, "BOS_SARIYER", "BOS_FILBURNU"),
      n("BOS_FILBURNU", "Filburnu Geçişi", "Kuzey Boğaz Yaklaşım Kanalı", 41.1910, 29.0935, true, "BOS_KAVAKLAR", "BOS_YSS_BRIDGE"),
      n("BOS_YSS_BRIDGE", "YSS Köprüsü Altı", "Garipçe - Poyrazköy Geçişi", 41.2030, 29.1145, true, "BOS_FILBURNU", "BOS_BLACKSEA"),
      n("BOS_BLACKSEA", "Türkeli / Boğaz Kuzey TSS", "Karadeniz Boğaz Yaklaşma Şamandırası", 41.2520, 29.1380, false, "BOS_YSS_BRIDGE", "TERKOS_NORTH", "SILE_OUTER"),

      // --- MARMARA DENİZİ ANA TSS VE KÖRFEZ SEYİR KORİDORLARI ---
      n("YENIKAPI_OUT", "Yenikapı - Bakırköy Koridoru", "Tarihi Yarımada Güney Seyir Hattı", 40.9760, 28.9420, false, "AHIRKAPI", "YESILKOY_OUT"),
      n("YESILKOY_OUT", "Yeşilköy Feneri TSS", "Yeşilköy Açıkları Kuzey TSS Şeridi", 40.9320, 28.8250, false, "YENIKAPI_OUT", "AMBARLI_OUT", "MARMARA_EAST"),
      n("AMBARLI_OUT", "Ambarlı Yaklaşma Hattı", "Ambarlı Limanı Güney Seyir Koridoru", 40.9350, 28.6800, false, "YESILKOY_OUT", "BUYUKCEKMECE_OUT", "MARMARA_EAST"),
      n("BUYUKCEKMECE_OUT", "Baba Burnu Açıkları TSS", "Büyükçekmece Güney Seyir Şeridi", 40.9350, 28.4850, false, "AMBARLI_OUT", "SILIVRI_OUT", "MARMARA_CENTRAL"),
      n("SILIVRI_OUT", "Silivri Açıkları Koridoru", "Kuzey Marmara Kıyı Seyir Hattı", 41.0050, 28.2400, false, "BUYUKCEKMECE_OUT", "M_EREGLI_OUT", "MARMARA_CENTRAL"),
      n("M_EREGLI_OUT", "Marmaraereğlisi TSS", "Marmaraereğlisi Burnu Güney TSS Hattı", 40.9150, 27.9650, false, "SILIVRI_OUT", "TEKIRDAG_OUT", "MARMARA_CENTRAL", "MARMARA_WEST"),
      n("TEKIRDAG_OUT", "Tekirdağ Yaklaşma Koridoru", "Tekirdağ Ceyport & Asyaport Seyir Hattı", 40.9250, 27.5500, false, "M_EREGLI_OUT", "MARMARA_WEST", "MARMARA_ISL_N", "HORA_OUTER"),
      n("HORA_OUTER", "Hoşköy / Hora Feneri TSS", "Batı Marmara Ana TSS Dönüş Hattı", 40.6750, 27.3750, false, "TEKIRDAG_OUT", "SARKOY_OUT", "MARMARA_ISL_N", "MARMARA_ISL_W"),
      n("SARKOY_OUT", "Şarköy - Mürefte TSS", "Gelibolu Yaklaşma Trafik Ayrım Düzeni", 40.5750, 27.1300, false, "HORA_OUTER", "MARMARA_ISL_W", "DARD_MARMARA_IN"),

      n("MARMARA_EAST", "Doğu Marmara Ana TSS", "Doğu Marmara Trafik Ayrım Düzeni", 40.8250, 28.6800, false, "SIVRIADA_TSS", "YESILKOY_OUT", "AMBARLI_OUT", "ESENKOY_OUTER", "MARMARA_CENTRAL"),
      n("MARMARA_CENTRAL", "Orta Marmara Ana TSS", "Marmara Merkez Trafik Ayrım Düzeni", 40.8050, 28.2500, false, "MARMARA_EAST", "BUYUKCEKMECE_OUT", "SILIVRI_OUT", "M_EREGLI_OUT", "IMRALI_NORTH", "MARMARA_WEST"),
      n("MARMARA_WEST", "Batı Marmara Ana TSS", "Batı Marmara Trafik Ayrım Düzeni", 40.7550, 27.8500, false, "MARMARA_CENTRAL", "M_EREGLI_OUT", "TEKIRDAG_OUT", "KAPIDAG_NORTH", "MARMARA_ISL_E", "MARMARA_ISL_N"),

      n("YALOVA_OUTER", "Yalova Yaklaşma Koridoru", "Yalova - Topçular Kuzey Seyir Hattı", 40.6880, 29.2600, false, "DARICA_OUTER", "BAYRAMOGLU_OUTER", "TUZLA_SOUTH", "TUZLA_CAPE_SW", "ADALAR_SOUTH", "CINARCIK_OUTER"),
      n("CINARCIK_OUTER", "Çınarcık Seyir Koridoru", "Çınarcık Kuzey Kıyı Açık Su Hattı", 40.6720, 29.0800, false, "YALOVA_OUTER", "ADALAR_SOUTH", "SIVRIADA_TSS", "ESENKOY_OUTER"),
      n("ESENKOY_OUTER", "Esenköy Açıkları", "Armutlu Kuzeybatı Yaklaşma Hattı", 40.6350, 28.8950, false, "CINARCIK_OUTER", "MARMARA_EAST", "ARMUTLU_CAPE"),
      n("ARMUTLU_CAPE", "Bozburun / Armutlu Dönüşü", "Armutlu Yarımadası Batı Ucu Koridoru", 40.5450, 28.7350, false, "ESENKOY_OUTER", "IMRALI_NORTH", "IMRALI_SOUTH", "GEMLIK_ENT", "MUDANYA_OUT"),
      n("GEMLIK_ENT", "Gemlik Körfezi Girişi", "Armutlu - Mudanya Orta Kanal", 40.4550, 28.8800, false, "ARMUTLU_CAPE", "MUDANYA_OUT", "GEMLIK_MID"),
      n("GEMLIK_MID", "Gemlik Körfezi Orta Hat", "Fıstıklı - Kurşunlu Derin Su Ekseni", 40.4420, 29.0050, false, "GEMLIK_ENT", "GEMLIK_INNER"),
      n("GEMLIK_INNER", "Gemlik Liman Havzası", "Gemlik İç Körfez Yaklaşma Hattı", 40.4320, 29.1120, false, "GEMLIK_MID"),
      n("MUDANYA_OUT", "Mudanya / Trilye Koridoru", "Güney Marmara Bursa Yaklaşma Hattı", 40.4180, 28.8650, false, "ARMUTLU_CAPE", "GEMLIK_ENT", "IMRALI_SOUTH"),
      n("IMRALI_NORTH", "İmralı Kuzey Koridoru", "İmralı Adası Kuzeyi Açık Deniz Hattı", 40.6150, 28.5300, false, "MARMARA_CENTRAL", "ARMUTLU_CAPE", "KAPIDAG_NE"),
      n("IMRALI_SOUTH", "İmralı Güney Kanalı", "İmralı - Karacabey Boğazı Seyir Hattı", 40.4650, 28.5300, false, "ARMUTLU_CAPE", "MUDANYA_OUT", "KARACABEY_OUT", "KAPIDAG_NE"),
      n("KARACABEY_OUT", "Karacabey Boğazı Açıkları", "Güney Marmara Bandırma Yaklaşımı", 40.4450, 28.2700, false, "IMRALI_SOUTH", "BANDIRMA_OUT", "KAPIDAG_NE"),
      n("BANDIRMA_OUT", "Bandırma Körfezi Ekseni", "Bandırma Limanı Yaklaşma Kanalı", 40.4150, 28.0350, false, "KARACABEY_OUT", "KAPIDAG_NE"),
      n("KAPIDAG_NE", "Kapıdağ Kuzeydoğu Dönüşü", "Kapsül Burnu Açık Su Koridoru", 40.5550, 28.0150, false, "BANDIRMA_OUT", "KARACABEY_OUT", "IMRALI_NORTH", "IMRALI_SOUTH", "KAPIDAG_NORTH"),
      n("KAPIDAG_NORTH", "Kapıdağ Kuzey Seyir Hattı", "Kapıdağ Yarımadası Kuzeyi", 40.5750, 27.8400, false, "KAPIDAG_NE", "MARMARA_WEST", "MARMARA_ISL_E", "KAPIDAG_NW"),
      n("KAPIDAG_NW", "Kapıdağ Kuzeybatı Dönüşü", "Marmara Adası - Kapıdağ Kuzey Kanalı", 40.5550, 27.7250, false, "KAPIDAG_NORTH", "MARMARA_ISL_E", "MARMARA_ISL_S", "KAPIDAG_WEST_CH"),
      n("MARMARA_ISL_E", "Marmara Adası Doğu Geçidi", "Saraylar - Kapıdağ Açık Su Kanalı", 40.6580, 27.7480, false, "MARMARA_WEST", "MARMARA_ISL_N", "KAPIDAG_NW", "KAPIDAG_NORTH"),
      n("MARMARA_ISL_S", "Marmara Adası Güney Kanalı", "Marmara Adası - Paşalimanı Kuzey Geçidi", 40.5550, 27.5850, false, "KAPIDAG_NW", "AVSA_EAST_CH", "MARMARA_ISL_W"),
      n("AVSA_EAST_CH", "Avşa - Paşalimanı Kanalı", "Avşa Doğu & Koyun Adası Geçidi", 40.4950, 27.5450, true, "MARMARA_ISL_S", "AVSA_SOUTH"),
      n("KAPIDAG_WEST_CH", "Paşalimanı - Kapıdağ Geçidi", "Doğanlar - Paşalimanı Doğu Kanalı", 40.4700, 27.6640, true, "KAPIDAG_NW", "ERDEK_WEST"),
      n("ERDEK_WEST", "Ocaklar / Erdek Batı Girişi", "Erdek Körfezi Batı Yaklaşma Hattı", 40.4350, 27.6750, false, "KAPIDAG_WEST_CH", "ERDEK_GULF", "AVSA_SOUTH"),
      n("ERDEK_GULF", "Erdek Körfezi Ekseni", "Erdek Deniz Üssü Yaklaşma Kanalı", 40.4050, 27.7550, false, "ERDEK_WEST", "AVSA_SOUTH"),
      n("MARMARA_ISL_N", "Marmara Adası Kuzey TSS", "Marmara Adası Kuzey Ana Seyir Hattı", 40.6950, 27.6100, false, "MARMARA_WEST", "TEKIRDAG_OUT", "HORA_OUTER", "MARMARA_ISL_E", "MARMARA_ISL_W"),
      n("MARMARA_ISL_W", "Marmara Adası Batı Kanalı", "Ekinlik & Marmara Adası Batı Açıkları", 40.6000, 27.4250, false, "MARMARA_ISL_N", "MARMARA_ISL_S", "HORA_OUTER", "SARKOY_OUT", "AVSA_WEST", "KARABIGA_OUT", "DARD_MARMARA_IN"),
      n("AVSA_WEST", "Avşa / Ekinlik Batı Koridoru", "Avşa Adası Batı Derin Su Hattı", 40.5000, 27.4350, false, "MARMARA_ISL_W", "AVSA_SOUTH", "KARABIGA_OUT"),
      n("AVSA_SOUTH", "Avşa / Paşalimanı Güneyi", "Güney Marmara Adalar Geçidi", 40.4350, 27.5150, false, "AVSA_WEST", "AVSA_EAST_CH", "ERDEK_WEST", "ERDEK_GULF", "KARABIGA_OUT"),
      n("KARABIGA_OUT", "Karabiga Burnu Koridoru", "Karabiga - Kemer Güney Seyir Hattı", 40.4550, 27.3200, false, "AVSA_WEST", "AVSA_SOUTH", "MARMARA_ISL_W", "DARD_MARMARA_IN"),

      // --- ÇANAKKALE BOĞAZI (DARDANELLES MERKEZ KANAL ZİNCİRİ) ---
      n("DARD_MARMARA_IN", "Şarköy - Kemer Yaklaşımı", "Çanakkale Boğazı Kuzeydoğu Girişi", 40.4550, 26.8500, false, "SARKOY_OUT", "MARMARA_ISL_W", "KARABIGA_OUT", "DARD_GELIBOLU"),
      n("DARD_GELIBOLU", "Gelibolu - Çardak Geçidi", "Çanakkale Boğazı Marmara Ağzı", 40.3940, 26.6950, true, "DARD_MARMARA_IN", "DARD_BRIDGE_1915"),
      n("DARD_BRIDGE_1915", "1915 Çanakkale Köprüsü", "Sütlüce - Lapseki Köprü Altı Kanalı", 40.3380, 26.6420, true, "DARD_GELIBOLU", "DARD_KARAKOVA"),
      n("DARD_KARAKOVA", "Karakova - Çamburnu", "Çanakkale Boğazı Üst Merkez Kanal", 40.2820, 26.5580, true, "DARD_BRIDGE_1915", "DARD_AKBAS"),
      n("DARD_AKBAS", "Akbaş Burnu Geçişi", "Akbaş - Yapıldak Orta Kanalı", 40.2220, 26.4520, true, "DARD_KARAKOVA", "DARD_NARA_NORTH"),
      n("DARD_NARA_NORTH", "Nara Burnu Kuzey Dönüşü", "Nara Burnu Kuzeybatı Manevra Hattı", 40.2035, 26.3840, true, "DARD_AKBAS", "DARD_NARA_WEST"),
      n("DARD_NARA_WEST", "Nara - Eceabat Kanalı", "Nara Burnu Batı Derin Su Geçidi", 40.1865, 26.3780, true, "DARD_NARA_NORTH", "DARD_KILITBAHIR"),
      n("DARD_KILITBAHIR", "Çanakkale - Kilitbahir", "Çanakkale Boğazı En Dar Merkez Geçidi", 40.1475, 26.3898, true, "DARD_NARA_WEST", "DARD_KEPEZ"),
      n("DARD_KEPEZ", "Kepez Feneri Açıkları", "Kepez Burnu Batı Seyir Kanalı", 40.0980, 26.3500, true, "DARD_KILITBAHIR", "DARD_ERENKOY"),
      n("DARD_ERENKOY", "Erenköy - Morto Hattı", "Çanakkale Güney Yaklaşım Kanalı", 40.0450, 26.2750, true, "DARD_KEPEZ", "DARD_AEGEAN_OUT"),
      n("DARD_AEGEAN_OUT", "Mehmetçik - Kumkale Çıkışı", "Çanakkale Boğazı Ege Çıkışı", 40.0200, 26.1550, false, "DARD_ERENKOY", "GOKCEADA_EAST", "BOZCAADA_CH", "BOZCAADA_WEST"),

      // --- EGE DENİZİ KORİDORLARI ---
      n("SAROS_GULF", "Saros Körfezi Açıkları", "Saros Körfezi Orta Su Yolu", 40.5400, 26.4200, false, "GOKCEADA_NORTH"),
      n("GOKCEADA_NORTH", "Gökçeada Kuzey Açıkları", "Gökçeada - Semadirek Geçidi", 40.2850, 25.9200, false, "SAROS_GULF", "GOKCEADA_NE", "GOKCEADA_NW"),
      n("GOKCEADA_NW", "Gökçeada Kuzeybatı Dönüşü", "Gökçeada Kuzeybatı Açıkları", 40.2350, 25.7000, false, "GOKCEADA_NORTH", "GOKCEADA_WEST"),
      n("GOKCEADA_NE", "Gökçeada Kuzeydoğu Dönüşü", "Kefalos Burnu Açıkları", 40.2450, 26.0550, false, "GOKCEADA_NORTH", "GOKCEADA_EAST"),
      n("GOKCEADA_EAST", "Gökçeada Doğu Kanalı", "Kabatepe - Kuzu Limanı Yaklaşımı", 40.1650, 26.0850, false, "DARD_AEGEAN_OUT", "GOKCEADA_NE", "BOZCAADA_WEST"),
      n("GOKCEADA_WEST", "Gökçeada Batı Açıkları", "İmroz - Limni Açık Deniz Hattı", 40.1200, 25.5800, false, "GOKCEADA_NW", "BOZCAADA_WEST", "MIDILLI_WEST"),
      n("BOZCAADA_CH", "Bozcaada - Geyikli Boğazı", "Bozcaada Doğu Emniyetli Geçit", 39.8350, 26.1120, false, "DARD_AEGEAN_OUT", "BOZCAADA_SOUTH", "BABAKALE_WEST"),
      n("BOZCAADA_SOUTH", "Bozcaada Güney Dönüşü", "Bozcaada Güney Açıkları", 39.7650, 26.0250, false, "BOZCAADA_CH", "BOZCAADA_WEST", "BABAKALE_WEST"),
      n("BOZCAADA_WEST", "Bozcaada Batı Açıkları", "Bozcaada Batı Derin Su Hattı", 39.8250, 25.9050, false, "DARD_AEGEAN_OUT", "GOKCEADA_EAST", "GOKCEADA_WEST", "BOZCAADA_SOUTH", "BABAKALE_WEST"),
      n("BABAKALE_WEST", "Babakale Dönüş Mevkii", "Babakale Feneri Batı Açıkları", 39.4650, 25.9850, false, "BOZCAADA_CH", "BOZCAADA_SOUTH", "BOZCAADA_WEST", "MUSELLIM_CH", "MIDILLI_WEST"),
      n("MUSELLIM_CH", "Müsellim Boğazı", "Assos - Midilli Kuzey Geçidi", 39.4350, 26.3200, false, "BABAKALE_WEST", "EDREMIT_GULF", "CUNDA_NW", "AYVALIK_OUTER"),
      n("EDREMIT_GULF", "Edremit Körfezi Orta Hat", "Akçay & Burhaniye Yaklaşımı", 39.5200, 26.7800, false, "MUSELLIM_CH", "CUNDA_NW"),
      n("CUNDA_NW", "Cunda / Ayvalık Kuzeybatısı", "Edremit - Ayvalık Yaklaşma Kanalı", 39.4150, 26.5850, false, "MUSELLIM_CH", "EDREMIT_GULF", "AYVALIK_OUTER"),
      n("AYVALIK_OUTER", "Ayvalık / Cunda Açıkları", "Midilli Kuzeydoğu - Ayvalık Kanalı", 39.2950, 26.5550, false, "MUSELLIM_CH", "CUNDA_NW", "DIKILI_STRAIT"),
      n("DIKILI_STRAIT", "Dikili - Midilli Kanalı", "Midilli Doğu Seyir Koridoru", 39.1100, 26.7200, false, "AYVALIK_OUTER", "CANDARLI_GULF", "FOCA_OUTER"),
      n("MIDILLI_WEST", "Midilli Batı Açık Deniz", "Midilli Sigri Açıkları", 39.1800, 25.7200, false, "GOKCEADA_WEST", "BABAKALE_WEST", "KARABURUN_WEST", "CHIOS_NW"),
      n("CANDARLI_GULF", "Çandarlı / Aliağa Açıkları", "Nemrut & Çandarlı Körfez Girişi", 38.8850, 26.8200, false, "DIKILI_STRAIT", "FOCA_OUTER"),
      n("FOCA_OUTER", "Foça Açıkları", "Foça Deniz Üssü & İzmir Körfez Ağzı", 38.6850, 26.6800, false, "DIKILI_STRAIT", "CANDARLI_GULF", "KARABURUN_NORTH", "IZMIR_NORTH", "IZMIR_UZUNADA_E", "UZUNADA_NW"),
      n("IZMIR_NORTH", "Uzunada Kuzey Geçidi", "İzmir Körfezi Girişi", 38.5850, 26.7450, false, "FOCA_OUTER", "IZMIR_UZUNADA_E", "UZUNADA_NW"),
      n("UZUNADA_NW", "Uzunada Kuzeybatı Geçidi", "Mordoğan - Uzunada Batı Kanalı", 38.5650, 26.6650, false, "FOCA_OUTER", "IZMIR_NORTH", "URLA_CH"),
      n("IZMIR_UZUNADA_E", "Uzunada Doğu Kanalı", "İzmir Körfezi Ana Seyir Hattı", 38.5050, 26.7850, false, "FOCA_OUTER", "IZMIR_NORTH", "IZMIR_YENIKALE"),
      n("URLA_CH", "Urla / Gülbahçe Kanalı", "Uzunada Batı İç Körfez Suları", 38.4350, 26.6720, false, "UZUNADA_NW"),
      n("IZMIR_YENIKALE", "Yenikale Geçidi", "İzmir Orta Körfez Kanalı", 38.4260, 26.9950, false, "IZMIR_UZUNADA_E", "IZMIR_ALSANCAK"),
      n("IZMIR_ALSANCAK", "İzmir İç Liman Havzası", "Alsancak & Karşıyaka Yaklaşımı", 38.4380, 27.1150, false, "IZMIR_YENIKALE"),
      n("KARABURUN_NORTH", "Karaburun Kuzey Dönüşü", "Karaburun Kuzey Açıkları", 38.7050, 26.4600, false, "FOCA_OUTER", "KARABURUN_WEST"),
      n("KARABURUN_WEST", "Sarpıncık / Karaburun Batısı", "Karaburun Feneri Batı Açıkları", 38.6650, 26.3100, false, "KARABURUN_NORTH", "MIDILLI_WEST", "OINOUSSES_EAST", "CHIOS_NW"),
      n("OINOUSSES_EAST", "Koyun Adaları (Eğriliman) Geçidi", "Koyun Adaları - Karaburun Doğu Kanalı", 38.5200, 26.3420, true, "KARABURUN_WEST", "CESME_STRAIT"),
      n("CHIOS_NW", "Sakız Kuzeybatı Açık Deniz", "Sakız Adası Kuzeybatı Seyir Hattı", 38.6800, 25.7600, false, "MIDILLI_WEST", "KARABURUN_WEST", "CHIOS_SW"),
      n("CHIOS_SW", "Sakız Güneybatı Açık Deniz", "Sakız - Ahikerya Açık Deniz Koridoru", 38.0800, 25.7800, false, "CHIOS_NW", "CESME_SOUTH", "SAMOS_NW", "IKARIA_PASS"),
      n("CESME_STRAIT", "Çeşme - Sakız Boğazı", "Çeşme Limanı & Sakız Doğu Geçidi", 38.3450, 26.2250, true, "OINOUSSES_EAST", "CESME_SOUTH"),
      n("CESME_SOUTH", "Teke Burnu Güneyi", "Çeşme Yarımadası Güney Dönüşü", 38.1800, 26.2350, false, "CESME_STRAIT", "CHIOS_SW", "SIGACIK_OUTER", "KUSADASI_GULF", "SAMOS_NW"),
      n("SIGACIK_OUTER", "Sığacık Körfezi Açıkları", "Sığacık & Doğanbey Seyir Hattı", 38.1400, 26.7200, false, "CESME_SOUTH", "DOGANBEY_CAPE"),
      n("DOGANBEY_CAPE", "Doğanbey Burnu Açıkları", "Sığacık - Kuşadası Dönüş Mevkii", 37.9750, 26.8450, false, "SIGACIK_OUTER", "CESME_SOUTH", "SAMOS_NW", "KUSADASI_GULF"),
      n("KUSADASI_GULF", "Kuşadası Körfezi Açıkları", "Kuşadası Yaklaşma Sahası", 37.8750, 27.1200, false, "CESME_SOUTH", "DOGANBEY_CAPE", "SAMOS_NW", "DILEK_STRAIT"),
      n("SAMOS_NW", "Sisam Kuzeybatı Açıkları", "Sisam Adası Kuzey Açık Deniz", 37.8400, 26.6200, false, "CESME_SOUTH", "CHIOS_SW", "DOGANBEY_CAPE", "KUSADASI_GULF", "IKARIA_PASS"),
      n("IKARIA_PASS", "Sisam - Ahikerya Geçidi", "Orta-Güney Ege Açık Deniz Koridoru", 37.6500, 26.4600, false, "SAMOS_NW", "CHIOS_SW", "DIDIM_OUTER", "YALIKAVAK_OUT"),
      n("DILEK_STRAIT", "Dilek Boğazı (Darboğaz)", "Sisam - Dilek Yarımadası Merkez Geçidi", 37.6945, 27.0175, true, "KUSADASI_GULF", "DIDIM_OUTER"),
      n("DIDIM_OUTER", "Didim Tekağaç Açıkları", "Didim & Akbük Yaklaşımı", 37.3300, 27.1400, false, "DILEK_STRAIT", "IKARIA_PASS", "GULLUK_GULF", "YALIKAVAK_OUT"),
      n("GULLUK_GULF", "Güllük Körfezi Girişi", "Güllük & Didim İç Yaklaşımı", 37.2400, 27.4800, false, "DIDIM_OUTER", "YALIKAVAK_OUT"),
      n("YALIKAVAK_OUT", "Yalıkavak Kuzeybatı Açıkları", "Bodrum Yarımadası Batı Yaklaşımı", 37.1450, 27.2050, false, "DIDIM_OUTER", "GULLUK_GULF", "IKARIA_PASS", "BODRUM_KOS_N"),
      n("BODRUM_KOS_N", "Hüseyin Burnu Geçidi", "Bodrum Turgutreis - İstanköy Kanalı", 36.9420, 27.2550, true, "YALIKAVAK_OUT", "BODRUM_SOUTH"),
      n("BODRUM_SOUTH", "Bodrum - İstanköy Güney Kanalı", "Karaada Güneyi Derin Su Hattı", 36.9250, 27.3950, false, "BODRUM_KOS_N", "BODRUM_BAY", "KARAADA_EAST", "GOKOVA_GULF", "KNIDOS_CAPE"),
      n("BODRUM_BAY", "Bodrum Liman Yaklaşımı", "Bodrum Kale & Karaada Kuzey Girişi", 37.0050, 27.4350, false, "BODRUM_SOUTH", "KARAADA_EAST"),
      n("KARAADA_EAST", "Karaada Doğu Geçidi", "Karaada - Bodrum Doğu Yaklaşımı", 36.9680, 27.5180, false, "BODRUM_BAY", "BODRUM_SOUTH", "GOKOVA_GULF"),
      n("GOKOVA_GULF", "Gökova Körfezi Orta Hat", "Ören & Sedir Adası Seyir Ekseni", 36.9650, 27.9500, false, "BODRUM_SOUTH", "KARAADA_EAST"),
      n("KNIDOS_CAPE", "Deveboynu / Knidos Dönüşü", "Datça Yarımadası Batı Ucu Açıkları", 36.6350, 27.3350, false, "BODRUM_SOUTH", "PALAMUTBUKU_OUT", "SYMI_SOUTH"),
      n("PALAMUTBUKU_OUT", "Palamutbükü Güney Açıkları", "Datça Güney Kıyı Seyir Hattı", 36.6300, 27.5600, false, "KNIDOS_CAPE", "DATCA_HISARONU", "SYMI_SOUTH"),
      n("DATCA_HISARONU", "Datça / Hisarönü Girişi", "Datça Limanı & Hisarönü Körfez Hattı", 36.6850, 27.7400, false, "PALAMUTBUKU_OUT", "SYMI_NORTH"),
      n("SYMI_NORTH", "Sömbeki Kuzey Kanalı", "Datça - Bozburun - Sömbeki Geçidi", 36.6780, 27.9150, false, "DATCA_HISARONU", "BOZBURUN_SW"),
      n("SYMI_SOUTH", "Sömbeki Güney Açıkları", "Sömbeki - Rodos Kuzeybatı Geçidi", 36.4650, 27.8200, false, "KNIDOS_CAPE", "PALAMUTBUKU_OUT", "BOZBURUN_SW"),
      n("BOZBURUN_SW", "Bozukkale Batı Dönüşü", "Bozburun Yarımadası Güneybatı Açıkları", 36.5350, 27.9650, false, "SYMI_NORTH", "SYMI_SOUTH", "BOZBURUN_SOUTH"),
      n("BOZBURUN_SOUTH", "Bozukkale - Rodos Kuzey Kanalı", "Bozburun Güneydoğu Dönüş Koridoru", 36.5350, 28.0850, false, "BOZBURUN_SW", "MARMARIS_OUTER"),
      n("MARMARIS_OUTER", "Kadırga / Marmaris Boğazı", "Marmaris & Aksaz Ana Yaklaşma Sahası", 36.7350, 28.3350, false, "BOZBURUN_SOUTH", "MARMARIS_INNER", "AKSAZ_APP", "FETHIYE_OUTER", "IBLIS_CAPE"),
      n("MARMARIS_INNER", "Marmaris İç Körfezi", "Marmaris Liman Havzası", 36.8250, 28.2850, false, "MARMARIS_OUTER"),
      n("AKSAZ_APP", "Aksaz Deniz Üssü Kanalı", "Karaağaç / Aksaz Yaklaşımı", 36.8150, 28.3850, false, "MARMARIS_OUTER"),

      // --- AKDENİZ & KIBRIS KORİDORLARI ---
      n("FETHIYE_OUTER", "Fethiye / Göcek Körfez Ağzı", "Göcek & Fethiye Dış Su Yolu", 36.5650, 28.9500, false, "MARMARIS_OUTER", "GOCEK_CH_EAST", "FETHIYE_INNER", "IBLIS_CAPE"),
      n("GOCEK_CH_EAST", "Tersane Adası Doğu Kanalı", "Göcek Ana Giriş Geçidi", 36.6650, 28.9650, false, "FETHIYE_OUTER", "GOCEK_INNER", "FETHIYE_INNER"),
      n("GOCEK_INNER", "Göcek İç Körfezi", "Tersane Adası Kuzeyi Göcek Yaklaşımı", 36.7250, 28.9450, false, "GOCEK_CH_EAST"),
      n("FETHIYE_INNER", "Fethiye İç Liman Yaklaşımı", "Şövalye Adası Batısı Fethiye Kanalı", 36.6350, 29.0750, false, "FETHIYE_OUTER", "GOCEK_CH_EAST"),
      n("IBLIS_CAPE", "İblis Burnu Dönüşü", "Fethiye Güneyi Açık Deniz", 36.4750, 28.9850, false, "MARMARIS_OUTER", "FETHIYE_OUTER", "YEDI_BURUNLAR"),
      n("YEDI_BURUNLAR", "Yedi Burunlar Açıkları", "Yedi Burunlar Feneri Dönüş Mevkii", 36.3200, 29.0750, false, "IBLIS_CAPE", "KALKAN_OUTER", "KAS_CHANNEL", "MEIS_SOUTH"),
      n("KALKAN_OUTER", "Kalkan Açıkları", "Kalkan Körfezi Yaklaşımı", 36.2150, 29.3950, false, "YEDI_BURUNLAR", "KAS_CHANNEL"),
      n("KAS_CHANNEL", "Kaş - Meis Kuzey Geçidi", "Çukurbağ Yarımadası - Meis Kanalı", 36.1720, 29.5980, true, "KALKAN_OUTER", "YEDI_BURUNLAR", "KEKOVA_SOUTH"),
      n("MEIS_SOUTH", "Meis Adası Güneyi", "Meis Güney Açık Deniz Hattı", 36.0950, 29.5900, false, "YEDI_BURUNLAR", "KEKOVA_SOUTH", "GELIDONYA_CAPE"),
      n("KEKOVA_SOUTH", "Kekova Adası Güneyi", "Kekova Açık Deniz Seyir Hattı", 36.1350, 29.8850, false, "KAS_CHANNEL", "MEIS_SOUTH", "FINIKE_OUTER", "GELIDONYA_CAPE"),
      n("FINIKE_OUTER", "Finike Körfezi Açıkları", "Finike Marina Yaklaşımı", 36.2250, 30.1850, false, "KEKOVA_SOUTH", "GELIDONYA_CAPE"),
      n("GELIDONYA_CAPE", "Gelidonya / Taşlık Burnu", "Gelidonya Feneri Güneyi Dönüş Mevkii", 36.1750, 30.4250, false, "MEIS_SOUTH", "KEKOVA_SOUTH", "FINIKE_OUTER", "KEMER_OUTER", "ANTALYA_OFFSHORE"),
      n("ANTALYA_OFFSHORE", "Antalya Körfezi Açık Deniz Hattı", "Gelidonya - Alanya Açık Su Koridoru", 36.2800, 31.2500, false, "GELIDONYA_CAPE", "KEMER_OUTER", "ANTALYA_GULF", "MANAVGAT_OUTER", "ALANYA_OUTER", "ANAMUR_CAPE"),
      n("KEMER_OUTER", "Kemer / Suluada Doğusu", "Batı Antalya Körfezi Hattı", 36.5850, 30.6250, false, "GELIDONYA_CAPE", "ANTALYA_GULF", "ANTALYA_OFFSHORE"),
      n("ANTALYA_GULF", "Antalya Liman Açıkları", "Port Akdeniz Yaklaşma Sahası", 36.8150, 30.6550, false, "KEMER_OUTER", "MANAVGAT_OUTER", "ANTALYA_OFFSHORE"),
      n("MANAVGAT_OUTER", "Side / Manavgat Açıkları", "Doğu Antalya Körfezi Koridoru", 36.7150, 31.4000, false, "ANTALYA_GULF", "ANTALYA_OFFSHORE", "ALANYA_OUTER"),
      n("ALANYA_OUTER", "Alanya Burnu Açıkları", "Alanya Kalesi Güney Seyir Hattı", 36.4850, 31.9850, false, "MANAVGAT_OUTER", "ANTALYA_OFFSHORE", "GAZIPASA_OUTER"),
      n("GAZIPASA_OUTER", "Gazipaşa Açıkları", "Alanya - Anamur Kıyı Koridoru", 36.1800, 32.2200, false, "ALANYA_OUTER", "ANAMUR_CAPE"),
      n("ANAMUR_CAPE", "Anamur Burnu Dönüşü", "Anadolu En Güney Ucu Açıkları", 35.9750, 32.8050, false, "GAZIPASA_OUTER", "ANTALYA_OFFSHORE", "OVACIK_CAPE", "GIRNE_OUTER", "KORUCAM_NW"),
      n("OVACIK_CAPE", "Aydıncık / Boğsak Açıkları", "Anamur - Taşucu Açık Deniz Hattı", 36.0650, 33.6800, false, "ANAMUR_CAPE", "TASUCU_OUTER", "GIRNE_OUTER"),
      n("TASUCU_OUTER", "İncekum / Taşucu Açıkları", "Taşucu Körfezi Güney Girişi", 36.1850, 33.9800, false, "OVACIK_CAPE", "ERDEMLI_OUTER", "KARATAS_CAPE", "GIRNE_OUTER", "KARPAZ_CAPE"),
      n("ERDEMLI_OUTER", "Erdemli / Kızkalesi Açıkları", "Taşucu - Mersin Seyir Koridoru", 36.5200, 34.3600, false, "TASUCU_OUTER", "MERSIN_OUTER", "KARATAS_CAPE"),
      n("MERSIN_OUTER", "Mersin Liman Açıkları", "Mersin Uluslararası Liman Yaklaşımı", 36.7450, 34.6450, false, "ERDEMLI_OUTER", "KARATAS_CAPE"),
      n("KARATAS_CAPE", "Karataş Burnu Dönüşü", "İskenderun Körfezi Batı Yaklaşımı", 36.4750, 35.3600, false, "MERSIN_OUTER", "ERDEMLI_OUTER", "TASUCU_OUTER", "ISKENDERUN_IN", "KARPAZ_CAPE"),
      n("ISKENDERUN_IN", "İskenderun Körfezi Girişi", "Akıncı Burnu - Karataş Orta Hattı", 36.4850, 35.7500, false, "KARATAS_CAPE", "ISKENDERUN_MID", "AKINCI_CAPE", "KARPAZ_CAPE"),
      n("ISKENDERUN_MID", "İskenderun Körfezi Orta Ekseni", "Yumurtalık - Arsuz Derin Su Kanalı", 36.6150, 35.9450, false, "ISKENDERUN_IN", "ISKENDERUN_PORT", "CEYHAN_DORTYOL"),
      n("ISKENDERUN_PORT", "İskenderun Liman Sahası", "İskenderun Deniz Üssü & Liman Yaklaşımı", 36.6250, 36.1200, false, "ISKENDERUN_MID", "CEYHAN_DORTYOL"),
      n("CEYHAN_DORTYOL", "Ceyhan / Dörtyol Havzası", "BOTAŞ & Dörtyol Terminali Yaklaşımı", 36.8150, 36.0500, false, "ISKENDERUN_MID", "ISKENDERUN_PORT"),
      n("AKINCI_CAPE", "Akıncı (Hınzır) Burnu Açıkları", "İskenderun Körfezi Güney Dönüşü", 36.3150, 35.7150, false, "ISKENDERUN_IN", "SAMANDAG_OUTER", "KARPAZ_CAPE"),
      n("SAMANDAG_OUTER", "Samandağ / Çevlik Açıkları", "Hatay Güney Kıyı Hattı", 36.0600, 35.8500, false, "AKINCI_CAPE", "KARPAZ_CAPE"),
      n("KORUCAM_NW", "Koruçam Burnu Açıkları", "KKTC Batı Yaklaşımı", 35.4600, 32.8800, false, "ANAMUR_CAPE", "GIRNE_OUTER"),
      n("GIRNE_OUTER", "KKTC Girne Açıkları", "Girne Limanı Kuzey Yaklaşımı", 35.3850, 33.3300, false, "KORUCAM_NW", "ANAMUR_CAPE", "OVACIK_CAPE", "TASUCU_OUTER", "KARPAZ_CAPE"),
      n("KARPAZ_CAPE", "Zafer Burnu (Karpaz) Dönüşü", "Karpaz Yarımadası Ucu Açıkları", 35.7550, 34.6650, false, "GIRNE_OUTER", "TASUCU_OUTER", "KARATAS_CAPE", "ISKENDERUN_IN", "AKINCI_CAPE", "SAMANDAG_OUTER", "KARPAZ_SOUTH"),
      n("KARPAZ_SOUTH", "Karpaz Güneydoğu Açıkları", "Gazimağusa Körfezi Yaklaşım Hattı", 35.4200, 34.4500, false, "KARPAZ_CAPE", "GAZIMAGUSA_OUT"),
      n("GAZIMAGUSA_OUT", "KKTC Gazimağusa Açıkları", "Gazimağusa Körfezi Yaklaşımı", 35.1250, 34.0300, false, "KARPAZ_SOUTH"),

      // --- KARADENİZ KIYI BOYU AÇIK SU SEYİR KORİDORLARI ---
      n("IGNEADA_OUTER", "İğneada Açıkları", "Batı Karadeniz Trakya Seyir Şeridi", 41.8700, 28.0800, false, "KIYIKOY_OUTER"),
      n("KIYIKOY_OUTER", "Kıyıköy Açıkları", "Kıyıköy Kıyı Seyir Koridoru", 41.6450, 28.1800, false, "IGNEADA_OUTER", "TERKOS_NORTH"),
      n("TERKOS_NORTH", "Karaburun (Karadeniz) Açıkları", "Boğaz Kuzeybatı Yaklaşma Hattı", 41.3900, 28.7200, false, "KIYIKOY_OUTER", "BOS_BLACKSEA"),
      n("SILE_OUTER", "Şile Feneri Açıkları", "Şile Deniz Seyir Şeridi", 41.2250, 29.6200, false, "BOS_BLACKSEA", "AGVA_OUTER"),
      n("AGVA_OUTER", "Ağva Açıkları", "Şile - Kefken Seyir Koridoru", 41.2150, 29.9200, false, "SILE_OUTER", "KEFKEN_OUTER"),
      n("KEFKEN_OUTER", "Kefken Adası Kuzeyi", "Kefken Açık Deniz Koridoru", 41.2600, 30.2600, false, "AGVA_OUTER", "KARASU_OUTER"),
      n("KARASU_OUTER", "Karasu / Sakarya Açıkları", "Batı Karadeniz Seyir Hattı", 41.1850, 30.7200, false, "KEFKEN_OUTER", "AKCAKOCA_OUTER"),
      n("AKCAKOCA_OUTER", "Akçakoca Açıkları", "Akçakoca - Ereğli Yaklaşma Koridoru", 41.1500, 31.1000, false, "KARASU_OUTER", "EREGLI_KDZ"),
      n("EREGLI_KDZ", "Kdz. Ereğli Baba Burnu", "Karadeniz Bölge Komutanlığı Açıkları", 41.3150, 31.3600, false, "AKCAKOCA_OUTER", "ZONGULDAK_OUT"),
      n("ZONGULDAK_OUT", "Zonguldak / Filyos Açıkları", "Zonguldak Liman Yaklaşma Şeridi", 41.5000, 31.7600, false, "EREGLI_KDZ", "BARTIN_AMASRA"),
      n("BARTIN_AMASRA", "Bartın / Amasra Açıkları", "Amasra & Bartın Deniz Üssü Koridoru", 41.7950, 32.3500, false, "ZONGULDAK_OUT", "KEREMPE_CAPE"),
      n("KEREMPE_CAPE", "Kerempe Burnu Dönüşü", "Cide - Kerempe Feneri Açıkları", 42.0650, 33.3300, false, "BARTIN_AMASRA", "INEBOLU_OUTER"),
      n("INEBOLU_OUTER", "İnebolu Açıkları", "İnebolu & Abana Seyir Şeridi", 42.0350, 33.7800, false, "KEREMPE_CAPE", "AYANCIK_OUTER"),
      n("AYANCIK_OUTER", "Çatalzeytin / Ayancık Açıkları", "İnebolu - İnceburun Seyir Koridoru", 42.0450, 34.4800, false, "INEBOLU_OUTER", "SINOP_INCEBURUN"),
      n("SINOP_INCEBURUN", "Sinop İnceburun Dönüşü", "Türkiye En Kuzey Ucu Açıkları", 42.1450, 34.9500, false, "AYANCIK_OUTER", "SINOP_NE"),
      n("SINOP_NE", "Sinop Boztepe Kuzeydoğusu", "Boztepe Yarımadası Kuzey Dönüşü", 42.0650, 35.2250, false, "SINOP_INCEBURUN", "SINOP_BOZTEPE", "GERZE_OUTER"),
      n("SINOP_BOZTEPE", "Sinop Boztepe Güneydoğusu", "Sinop İç Liman Giriş Hattı", 42.0100, 35.2450, false, "SINOP_NE", "SINOP_INNER", "GERZE_OUTER"),
      n("SINOP_INNER", "Sinop İç Liman Körfezi", "Sinop Doğal Liman Demir Sahası", 42.0020, 35.1680, false, "SINOP_BOZTEPE"),
      n("GERZE_OUTER", "Gerze / Yakakent Açıkları", "Sinop - Bafra Seyir Koridoru", 41.8450, 35.3800, false, "SINOP_NE", "SINOP_BOZTEPE", "BAFRA_CAPE"),
      n("BAFRA_CAPE", "Bafra Burnu Açıkları", "Kızılırmak Deltası Kuzey Dönüşü", 41.7850, 35.9800, false, "GERZE_OUTER", "SAMSUN_OUTER", "CARSAMBA_CAPE"),
      n("SAMSUN_OUTER", "Samsunport Açıkları", "Samsun Körfezi Yaklaşma Sahası", 41.3450, 36.3800, false, "BAFRA_CAPE", "CARSAMBA_CAPE"),
      n("CARSAMBA_CAPE", "Çarşamba Burnu Açıkları", "Yeşilırmak Deltası Kuzeyi", 41.4350, 36.6800, false, "BAFRA_CAPE", "SAMSUN_OUTER", "YASON_CAPE"),
      n("YASON_CAPE", "Yason Burnu Dönüşü", "Ünye - Fatsa - Yason Açıkları", 41.1850, 37.6800, false, "CARSAMBA_CAPE", "ORDU_OUTER", "GIRESUN_OUTER"),
      n("ORDU_OUTER", "Ordu Açıkları", "Ordu Liman Yaklaşımı", 41.0350, 37.9200, false, "YASON_CAPE", "GIRESUN_OUTER"),
      n("GIRESUN_OUTER", "Giresun Adası Kuzeyi", "Giresun Seyir Koridoru", 40.9750, 38.4200, false, "YASON_CAPE", "ORDU_OUTER", "TIREBOLU_OUTER"),
      n("TIREBOLU_OUTER", "Tirebolu / Görele Açıkları", "Giresun - Yoros Seyir Şeridi", 41.0650, 38.8500, false, "GIRESUN_OUTER", "YOROS_CAPE"),
      n("YOROS_CAPE", "Yoros Burnu Açıkları", "Trabzon Batı Yaklaşımı", 41.1450, 39.4200, false, "TIREBOLU_OUTER", "TRABZON_OUTER"),
      n("TRABZON_OUTER", "Trabzon Liman Açıkları", "Trabzon Yaklaşma Sahası", 41.0450, 39.7600, false, "YOROS_CAPE", "SURMENE_OUTER"),
      n("SURMENE_OUTER", "Sürmene / Çamburnu Açıkları", "Çamburnu Deniz Üssü Yaklaşımı", 40.9650, 40.2100, false, "TRABZON_OUTER", "RIZE_OUTER"),
      n("RIZE_OUTER", "Rize Açıkları", "Rize Liman Yaklaşımı", 41.0750, 40.5500, false, "SURMENE_OUTER", "PAZAR_OUTER"),
      n("PAZAR_OUTER", "Pazar / Arhavi Açıkları", "Doğu Karadeniz Kıyı Seyir Koridoru", 41.2350, 40.9200, false, "RIZE_OUTER", "HOPA_OUTER"),
      n("HOPA_OUTER", "Hopa / Sarp Açıkları", "Doğu Karadeniz Liman Sahası", 41.4350, 41.4000, false, "PAZAR_OUTER")
    ).associateBy { it.id }
  }

  // Yalnızca doğrulanmış deniz seyir koridorları (TSS / kanal komşulukları) kullanılır;
  // kuş uçuşu uzun çapraz kısayollar kesinlikle eklenmez.
  private val verifiedAdjacency: Map<String, List<String>> by lazy {
    val adj = HashMap<String, MutableSet<String>>(seaNodes.size)
    for (id in seaNodes.keys) {
      adj[id] = LinkedHashSet()
    }
    val nodesList = seaNodes.values.toList()

    for (u in nodesList) {
      for (vId in u.explicitNeighbors) {
        val v = seaNodes[vId] ?: continue
        val clearance = if (u.straitChannel || v.straitChannel) 0.03 else 0.07
        if (!MarineLandMask.doesSegmentCrossLand(u.lat, u.lon, v.lat, v.lon, clearance)) {
          adj[u.id]?.add(v.id)
          adj[v.id]?.add(u.id)
        }
      }
    }

    adj.mapValues { it.value.toList() }
  }

  internal fun debugVerifyGraphIntegrity(): List<String> {
    val issues = ArrayList<String>()
    for (u in seaNodes.values) {
      if (MarineLandMask.isPointOnLand(u.lat, u.lon)) {
        issues.add("NODE_ON_LAND: ${u.id} (${u.lat}, ${u.lon})")
      }
      val verified = verifiedAdjacency[u.id].orEmpty().toSet()
      for (vId in u.explicitNeighbors) {
        if (!seaNodes.containsKey(vId)) {
          issues.add("MISSING_NODE: ${u.id} -> $vId")
        } else if (!verified.contains(vId)) {
          val v = seaNodes[vId]!!
          issues.add("BROKEN_EDGE: ${u.id} (${u.lat},${u.lon}) -> $vId (${v.lat},${v.lon})")
        }
      }
    }

    val visited = HashSet<String>()
    val queue = ArrayDeque<String>()
    val root = "TUZLA_WEST"
    visited.add(root)
    queue.add(root)
    while (queue.isNotEmpty()) {
      val curr = queue.removeFirst()
      for (nb in verifiedAdjacency[curr].orEmpty()) {
        if (visited.add(nb)) {
          queue.add(nb)
        }
      }
    }
    for (id in seaNodes.keys) {
      if (!visited.contains(id)) {
        issues.add("UNREACHABLE_NODE: $id")
      }
    }
    return issues
  }

  private data class EndpointConnection(
    val nodeId: String,
    val connectorCoords: List<Coordinate>,
    val distanceNm: Double
  )

  /**
   * Başlangıç veya varış noktasını kuş uçuşu uzak düğümlere atlatmadan,
   * yalnızca en yakın emniyetli deniz seyir koridoru düğümlerine bağlar.
   */
  private fun findAccessibleConnections(lat: Double, lon: Double): List<EndpointConnection> {
    val sortedByDist = seaNodes.values
      .map { it to LocationPresets.calculateDistanceNm(lat, lon, it.lat, it.lon) }
      .sortedBy { it.second }

    val visible = ArrayList<EndpointConnection>(4)

    // 1. Önce yerel seyir koridoruna (<= 16 NM) emniyetli açık su payıyla bağlan (en yakın en fazla 3 düğüm)
    for ((node, d) in sortedByDist) {
      if (d > 16.0) break
      val clearance = if (node.straitChannel) 0.03 else 0.18
      if (!MarineLandMask.doesSegmentCrossLand(lat, lon, node.lat, node.lon, clearance)) {
        visible.add(EndpointConnection(node.id, emptyList(), d))
        if (visible.size >= 3) return visible
      }
    }
    if (visible.isNotEmpty()) return visible

    // 2. Dar koy/iskele çıkışları için yerel koridora (<= 18 NM) standart kara-dışı geçiş kontrolü
    for ((node, d) in sortedByDist) {
      if (d > 18.0) break
      val clearance = if (node.straitChannel) 0.02 else 0.06
      if (!MarineLandMask.doesSegmentCrossLand(lat, lon, node.lat, node.lon, clearance)) {
        visible.add(EndpointConnection(node.id, emptyList(), d))
        if (visible.size >= 3) return visible
      }
    }
    if (visible.isNotEmpty()) return visible

    // 3. Kıyıdan çok uzakta açık denizde (örn. Karadeniz / Akdeniz ortası > 18 NM) seçilen noktalar:
    // En yakın düğüm uzaksa açık denizden en yakın 3 koridor düğümüne bağlan
    val nearestNodeDist = sortedByDist.firstOrNull()?.second ?: 0.0
    if (nearestNodeDist > 16.0) {
      for ((node, d) in sortedByDist) {
        if (d > 240.0) break
        if (!MarineLandMask.doesSegmentCrossLand(lat, lon, node.lat, node.lon, 0.12)) {
          visible.add(EndpointConnection(node.id, emptyList(), d))
          if (visible.size >= 3) return visible
        }
      }
      if (visible.isNotEmpty()) return visible
    }

    // 4. Yerel koy/ada/burun arkasında kalmış mevki: en yakın 4 koridor düğümüne Theta* kavisli deniz yolu ör
    for ((cand, _) in sortedByDist.take(4)) {
      val localPath = MarineLandMask.findLocalCoastalSeaPath(lat, lon, cand.lat, cand.lon)
      if (localPath != null && localPath.size >= 2) {
        val smoothedLocal = smoothLocalGridPath(localPath)
        val dist = computeChainDistanceNm(smoothedLocal)
        val interior = if (smoothedLocal.size > 2) smoothedLocal.subList(1, smoothedLocal.size - 1) else emptyList()
        visible.add(EndpointConnection(cand.id, interior, dist))
      }
    }
    return visible
  }

  private fun computeChainDistanceNm(pts: List<Coordinate>): Double {
    var total = 0.0
    for (i in 0 until pts.size - 1) {
      total += LocationPresets.calculateDistanceNm(
        pts[i].latitude, pts[i].longitude,
        pts[i + 1].latitude, pts[i + 1].longitude
      )
    }
    return total
  }

  private fun angleDiffDegrees(b1: Int, b2: Int): Int {
    val diff = abs(b1 - b2) % 360
    return if (diff > 180) 360 - diff else diff
  }

  /**
   * Çoklu giriş ve çıkış bağlantılı A* aramasıyla tüm deniz seyir koridorları (TSS) üzerinde
   * kuş uçuşu kısayol yapmadan emniyetli %100 deniz yolunu bulur.
   */
  private fun findBestCorridorChain(startSea: Coordinate, endSea: Coordinate): List<Coordinate>? {
    val startConns = findAccessibleConnections(startSea.latitude, startSea.longitude)
    val endConns = findAccessibleConnections(endSea.latitude, endSea.longitude)
    if (startConns.isEmpty() || endConns.isEmpty()) return null

    val endConnMap = endConns.associateBy { it.nodeId }
    val gScore = HashMap<String, Double>()
    val prevNode = HashMap<String, String>()
    val entryUsed = HashMap<String, EndpointConnection>()
    val queue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

    val VIRTUAL_GOAL = "__SEA_GOAL__"
    var bestGoalEntry: EndpointConnection? = null
    var bestGoalExit: EndpointConnection? = null
    var bestGoalPrevNode: String? = null

    for (sc in startConns) {
      val prev = gScore[sc.nodeId]
      if (prev == null || sc.distanceNm < prev) {
        gScore[sc.nodeId] = sc.distanceNm
        entryUsed[sc.nodeId] = sc
        val node = seaNodes[sc.nodeId]!!
        val h = LocationPresets.calculateDistanceNm(node.lat, node.lon, endSea.latitude, endSea.longitude)
        queue.add(Pair(sc.nodeId, sc.distanceNm + h))
      }
    }

    while (queue.isNotEmpty()) {
      val (uId, fVal) = queue.poll() ?: break
      if (uId == VIRTUAL_GOAL) break

      val uG = gScore[uId] ?: continue
      val currentGoalBest = gScore[VIRTUAL_GOAL]
      if (currentGoalBest != null && fVal >= currentGoalBest) continue

      val exitConn = endConnMap[uId]
      if (exitConn != null) {
        val totalToGoal = uG + exitConn.distanceNm
        if (currentGoalBest == null || totalToGoal < currentGoalBest) {
          gScore[VIRTUAL_GOAL] = totalToGoal
          bestGoalPrevNode = uId
          bestGoalExit = exitConn
          queue.add(Pair(VIRTUAL_GOAL, totalToGoal))
        }
      }

      val uNode = seaNodes[uId] ?: continue
      val neighbors = verifiedAdjacency[uId].orEmpty()
      for (vId in neighbors) {
        val vNode = seaNodes[vId] ?: continue
        val stepDist = LocationPresets.calculateDistanceNm(uNode.lat, uNode.lon, vNode.lat, vNode.lon)
        val candG = uG + stepDist
        val oldV = gScore[vId]
        if (oldV == null || candG < oldV) {
          gScore[vId] = candG
          prevNode[vId] = uId
          val h = LocationPresets.calculateDistanceNm(vNode.lat, vNode.lon, endSea.latitude, endSea.longitude)
          queue.add(Pair(vId, candG + h))
        }
      }
    }

    val finalNodeId = bestGoalPrevNode ?: return null
    val nodeChain = ArrayList<SeaNode>()
    var curr: String? = finalNodeId
    while (curr != null) {
      val nd = seaNodes[curr] ?: break
      nodeChain.add(0, nd)
      if (!prevNode.containsKey(curr)) {
        bestGoalEntry = entryUsed[curr]
        break
      }
      curr = prevNode[curr]
    }
    if (nodeChain.isEmpty()) return null

    // Koridor giriş ve çıkışlarında geriye keskin dönüş (U-dönüşü > 85°) varsa bir sonraki koridor şamandırasına bağla
    val entryPts = bestGoalEntry?.connectorCoords.orEmpty()
    if (entryPts.isEmpty() && nodeChain.size >= 2 && !nodeChain[0].straitChannel) {
      val n0 = nodeChain[0]
      val n1 = nodeChain[1]
      val bIn = LocationPresets.calculateBearingDegrees(startSea.latitude, startSea.longitude, n0.lat, n0.lon)
      val bNext = LocationPresets.calculateBearingDegrees(n0.lat, n0.lon, n1.lat, n1.lon)
      val distToN1 = LocationPresets.calculateDistanceNm(startSea.latitude, startSea.longitude, n1.lat, n1.lon)
      if (angleDiffDegrees(bIn, bNext) > 85 &&
          distToN1 <= 16.0 &&
          !MarineLandMask.doesSegmentCrossLand(startSea.latitude, startSea.longitude, n1.lat, n1.lon, 0.16)) {
        nodeChain.removeAt(0)
      }
    }

    val exitPts = bestGoalExit?.connectorCoords?.reversed().orEmpty()
    if (exitPts.isEmpty() && nodeChain.size >= 2 && !nodeChain.last().straitChannel) {
      val nPrev = nodeChain[nodeChain.size - 2]
      val nLast = nodeChain.last()
      val bLeg = LocationPresets.calculateBearingDegrees(nPrev.lat, nPrev.lon, nLast.lat, nLast.lon)
      val bOut = LocationPresets.calculateBearingDegrees(nLast.lat, nLast.lon, endSea.latitude, endSea.longitude)
      val distFromPrev = LocationPresets.calculateDistanceNm(nPrev.lat, nPrev.lon, endSea.latitude, endSea.longitude)
      if (angleDiffDegrees(bLeg, bOut) > 85 &&
          distFromPrev <= 16.0 &&
          !MarineLandMask.doesSegmentCrossLand(nPrev.lat, nPrev.lon, endSea.latitude, endSea.longitude, 0.16)) {
        nodeChain.removeAt(nodeChain.lastIndex)
      }
    }

    val result = ArrayList<Coordinate>()
    result.add(startSea)
    result.addAll(entryPts)
    for (nd in nodeChain) {
      val pt = Coordinate(nd.lat, nd.lon)
      if (LocationPresets.calculateDistanceNm(result.last().latitude, result.last().longitude, pt.latitude, pt.longitude) > 0.08) {
        result.add(pt)
      }
    }
    for (pt in exitPts) {
      if (LocationPresets.calculateDistanceNm(result.last().latitude, result.last().longitude, pt.latitude, pt.longitude) > 0.08) {
        result.add(pt)
      }
    }
    if (LocationPresets.calculateDistanceNm(result.last().latitude, result.last().longitude, endSea.latitude, endSea.longitude) > 0.05) {
      result.add(endSea)
    } else if (result.size >= 2) {
      result[result.lastIndex] = endSea
    } else {
      result.add(endSea)
    }
    return refineCollinearCorridor(result)
  }

  /**
   * Deniz seyir koridoru (TSS / kanal) dönüş noktalarını ASLA kuş uçuşu kısayolla silmez;
   * yalnızca aynı doğrultudaki (<= 4°) ardışık düz kanal noktalarını birleştirir.
   */
  private fun refineCollinearCorridor(points: List<Coordinate>): List<Coordinate> {
    if (points.size <= 2) return points
    val cleaned = ArrayList<Coordinate>(points.size)
    cleaned.add(points[0])
    for (i in 1 until points.size - 1) {
      val prev = cleaned.last()
      val curr = points[i]
      val next = points[i + 1]
      val b1 = LocationPresets.calculateBearingDegrees(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
      val b2 = LocationPresets.calculateBearingDegrees(curr.latitude, curr.longitude, next.latitude, next.longitude)
      val mergedDist = LocationPresets.calculateDistanceNm(prev.latitude, prev.longitude, next.latitude, next.longitude)
      if (angleDiffDegrees(b1, b2) <= 4 &&
          mergedDist <= 28.0 &&
          !MarineLandMask.doesSegmentCrossLand(prev.latitude, prev.longitude, next.latitude, next.longitude, 0.18)) {
        continue
      }
      cleaned.add(curr)
    }
    cleaned.add(points.last())
    return cleaned
  }

  /**
   * Yalnızca yerel koy/ada Theta* ızgara basamaklarını (maksimum 6 NM kavisli adımlarla) yumuşatır.
   */
  private fun smoothLocalGridPath(rawPoints: List<Coordinate>): List<Coordinate> {
    if (rawPoints.size <= 2) return rawPoints

    val smoothed = ArrayList<Coordinate>()
    var currentIndex = 0
    smoothed.add(rawPoints[0])

    while (currentIndex < rawPoints.size - 1) {
      var chosenNext = currentIndex + 1
      for (candidate in rawPoints.size - 1 downTo currentIndex + 2) {
        val p1 = rawPoints[currentIndex]
        val p2 = rawPoints[candidate]
        val stepNm = LocationPresets.calculateDistanceNm(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        if (stepNm <= 6.0 &&
            !MarineLandMask.doesSegmentCrossLand(p1.latitude, p1.longitude, p2.latitude, p2.longitude, clearanceNm = 0.14)) {
          chosenNext = candidate
          break
        }
      }
      smoothed.add(rawPoints[chosenNext])
      currentIndex = chosenNext
    }
    return refineCollinearCorridor(smoothed)
  }

  private fun formatDurationMinutes(totalMinutes: Int): String {
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hrs > 0) "${hrs} sa ${mins} dk" else "${mins} dk"
  }

  private const val MAX_GEOMETRY_CACHE = 64
  private val geometryCache = object : LinkedHashMap<String, List<Coordinate>>(MAX_GEOMETRY_CACHE, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Coordinate>>?): Boolean = size > MAX_GEOMETRY_CACHE
  }

  @Volatile var lastRouteComputeMs: Long = 1L
    private set
  @Volatile var routeCacheHits: Int = 0
    private set
  @Volatile var routeCacheMisses: Int = 0
    private set

  fun clearRouteCache() {
    synchronized(geometryCache) {
      geometryCache.clear()
    }
  }

  private fun buildGeometryCacheKey(start: Coordinate, end: Coordinate): String {
    val qSlat = (start.latitude * 2500.0).roundToInt()
    val qSlon = (start.longitude * 2500.0).roundToInt()
    val qElat = (end.latitude * 2500.0).roundToInt()
    val qElon = (end.longitude * 2500.0).roundToInt()
    return "${qSlat}_${qSlon}_${qElat}_${qElon}"
  }

  /**
   * Başlangıç mevkisinden hedef mevkisine kuş uçuşu düz çizgi yerine deniz üzerinde seyir yapılabilecek
   * emniyetli seyir koridorlarından (TSS / kanal hatları) geçen rotayı ve her dönüş noktasının ETA'sını hesaplar.
   */
  fun calculateSeaRoute(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    speedKnots: Double = 10.0,
    targetName: String? = null,
    rejectLandDestination: Boolean = false
  ): MarineSeaRoute {
    val startNs = System.nanoTime()
    val effectiveSpeed = if (speedKnots > 0.1) speedKnots else 10.0
    val destTitle = targetName ?: "Seçilen Varış Mevkii"

    // 1. Harita tıklamasında kara kontrolü: Gemiler karadan gidemez!
    if (rejectLandDestination && MarineLandMask.isRejectedLandClick(destLat, destLon)) {
      lastRouteComputeMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
      return MarineSeaRoute(
        originLat = originLat,
        originLon = originLon,
        destLat = destLat,
        destLon = destLon,
        destinationName = destTitle,
        isSeaRouteWithWaypoints = false,
        waypoints = emptyList(),
        legs = emptyList(),
        totalDistanceNm = 0.0,
        directDistanceNm = 0.0,
        speedKnots = effectiveSpeed,
        totalMinutes = 0,
        durationStr = "--",
        etaTimeStr = "--:--",
        fullEtaDateStr = "--",
        pathCoordinates = emptyList(),
        initialBearingDegrees = 0,
        isRejectedLandTarget = true,
        rejectionReason = "Seçilen mevki kara üzerindedir! Gemiler karadan gidemez. Lütfen deniz üzerinde bir mevki seçiniz."
      )
    }

    // Kıyı/iskele koordinatlarını en yakın emniyetli deniz suyuna oturt
    val startSea = MarineLandMask.snapToNearestSeaWater(originLat, originLon, clearanceNm = 0.05)
    val endSea = MarineLandMask.snapToNearestSeaWater(destLat, destLon, clearanceNm = 0.05)
    val directDist = LocationPresets.calculateDistanceNm(startSea.latitude, startSea.longitude, endSea.latitude, endSea.longitude)

    // Hız veya ufak GPS titreşimlerinde A*/Theta* poligon hesabını tekrarlamamak için LRU Geometri Önbelleği
    val cacheKey = buildGeometryCacheKey(startSea, endSea)
    val cachedGeometry = synchronized(geometryCache) { geometryCache[cacheKey] }

    val safeCoordinates: List<Coordinate> = if (cachedGeometry != null && cachedGeometry.size >= 2) {
      routeCacheHits++
      cachedGeometry
    } else {
      routeCacheMisses++
      // 2. Ana Deniz Seyir Koridoru (TSS / Kanal Ağı) Rotasını Hesapla
      val corridorPath = findBestCorridorChain(startSea, endSea)
      val corridorDist = corridorPath?.let { computeChainDistanceNm(it) }

      val isVeryShortOpenHop = directDist <= 2.2 && !MarineLandMask.doesSegmentCrossLand(
        startSea.latitude, startSea.longitude,
        endSea.latitude, endSea.longitude,
        clearanceNm = 0.28
      )

      val finalCoordinates: List<Coordinate> = when {
        isVeryShortOpenHop && (corridorDist == null || corridorDist > directDist * 1.35) -> {
          listOf(startSea, endSea)
        }
        directDist <= 6.5 && (corridorDist == null || corridorDist > directDist * 1.65) -> {
          val localSea = MarineLandMask.findLocalCoastalSeaPath(
            startSea.latitude, startSea.longitude,
            endSea.latitude, endSea.longitude
          )?.let { smoothLocalGridPath(it) }
          localSea ?: corridorPath ?: listOf(startSea, endSea)
        }
        corridorPath != null -> corridorPath
        else -> {
          MarineLandMask.findLocalCoastalSeaPath(
            startSea.latitude, startSea.longitude,
            endSea.latitude, endSea.longitude
          )?.let { smoothLocalGridPath(it) } ?: listOf(startSea, endSea)
        }
      }

      val verified = finalCoordinates.map { pt ->
        if (MarineLandMask.isPointOnLand(pt.latitude, pt.longitude)) {
          MarineLandMask.snapToNearestSeaWater(pt.latitude, pt.longitude, clearanceNm = 0.05)
        } else {
          pt
        }
      }
      synchronized(geometryCache) {
        geometryCache[cacheKey] = verified
      }
      verified
    }

    lastRouteComputeMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(0L)
    val nowMillis = System.currentTimeMillis()
    val timeSdf = SimpleDateFormat("HH:mm", Locale.US)
    val fullDateSdf = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("tr"))

    // 3. Dönüş Noktalarını (DN1, DN2...) ve Her Dönüş Noktasının Kendi ETA Bilgisini Üret
    val waypoints = ArrayList<MarineWaypoint>()
    val legs = ArrayList<MarineRouteLeg>()
    var cumulativeDistNm = 0.0

    for (i in 0 until safeCoordinates.size - 1) {
      val p1 = safeCoordinates[i]
      val p2 = safeCoordinates[i + 1]
      val legDist = LocationPresets.calculateDistanceNm(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
      val legBrg = LocationPresets.calculateBearingDegrees(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
      cumulativeDistNm += legDist

      val cumMinutes = ((cumulativeDistNm / effectiveSpeed) * 60.0).roundToInt()
      val legMinutes = ((legDist / effectiveSpeed) * 60.0).roundToInt()
      val pointEtaStr = timeSdf.format(Date(nowMillis + cumMinutes * 60_000L))
      val pointDurStr = formatDurationMinutes(cumMinutes)

      val fromLabel = if (i == 0) "Mevcut Konum" else "DN$i"
      val toLabel = if (i + 1 == safeCoordinates.size - 1) destTitle else "DN${i + 1}"

      legs.add(
        MarineRouteLeg(
          fromName = fromLabel,
          toName = toLabel,
          fromLat = p1.latitude,
          fromLon = p1.longitude,
          toLat = p2.latitude,
          toLon = p2.longitude,
          distanceNm = Math.round(legDist * 10.0) / 10.0,
          bearingDegrees = legBrg,
          legMinutes = legMinutes,
          etaTimeStr = pointEtaStr
        )
      )

      // Ara noktalar (1 .. size - 2) Dönüş Noktası (DN1, DN2...) olarak kaydedilir
      if (i + 1 < safeCoordinates.size - 1) {
        val wpIndex = i + 1
        val wpCoord = safeCoordinates[wpIndex]
        val nextCoord = safeCoordinates[wpIndex + 1]
        val nextLegBrg = LocationPresets.calculateBearingDegrees(
          wpCoord.latitude, wpCoord.longitude,
          nextCoord.latitude, nextCoord.longitude
        )

        val dnId = "DN$wpIndex"
        val dnName = "Dönüş Noktası $wpIndex ($dnId)"

        var closestDesc = "Emniyetli Açık Deniz Dönüş Mevkii"
        var minD = Double.MAX_VALUE
        for (node in seaNodes.values) {
          val d = LocationPresets.calculateDistanceNm(wpCoord.latitude, wpCoord.longitude, node.lat, node.lon)
          if (d < minD && d <= 3.5) {
            minD = d
            closestDesc = "${node.name} (${node.desc})"
          }
        }

        waypoints.add(
          MarineWaypoint(
            id = dnId,
            name = dnName,
            description = closestDesc,
            latitude = wpCoord.latitude,
            longitude = wpCoord.longitude,
            legDistanceNm = Math.round(legDist * 10.0) / 10.0,
            legBearingDegrees = legBrg,
            nextLegBearingDegrees = nextLegBrg,
            cumulativeDistanceNm = Math.round(cumulativeDistNm * 10.0) / 10.0,
            cumulativeMinutes = cumMinutes,
            durationStr = pointDurStr,
            etaTimeStr = pointEtaStr
          )
        )
      }
    }

    val totalMinutes = ((cumulativeDistNm / effectiveSpeed) * 60.0).roundToInt()
    val durStr = formatDurationMinutes(totalMinutes)
    val etaMillis = nowMillis + totalMinutes * 60_000L
    val etaTimeStr = timeSdf.format(Date(etaMillis))
    val fullDateStr = fullDateSdf.format(Date(etaMillis))
    val initialBearing = legs.firstOrNull()?.bearingDegrees
      ?: LocationPresets.calculateBearingDegrees(startSea.latitude, startSea.longitude, endSea.latitude, endSea.longitude)

    return MarineSeaRoute(
      originLat = startSea.latitude,
      originLon = startSea.longitude,
      destLat = endSea.latitude,
      destLon = endSea.longitude,
      destinationName = destTitle,
      isSeaRouteWithWaypoints = waypoints.isNotEmpty(),
      waypoints = waypoints,
      legs = legs,
      totalDistanceNm = Math.round(cumulativeDistNm * 100.0) / 100.0,
      directDistanceNm = Math.round(directDist * 100.0) / 100.0,
      speedKnots = Math.round(effectiveSpeed * 10.0) / 10.0,
      totalMinutes = totalMinutes,
      durationStr = durStr,
      etaTimeStr = etaTimeStr,
      fullEtaDateStr = fullDateStr,
      pathCoordinates = safeCoordinates,
      initialBearingDegrees = initialBearing,
      isRejectedLandTarget = false,
      rejectionReason = null
    )
  }
}
