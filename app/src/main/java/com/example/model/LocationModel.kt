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
