package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class SimpleDestination(val name: String, val lat: Double, val lon: Double)

data class Coordinate(
  val latitude: Double,
  val longitude: Double
)

fun dmsToDecimal(
  degrees: Int,
  minutes: Int,
  seconds: Double,
  direction: Char
): Double {
  var decimal = degrees +
      minutes / 60.0 +
      seconds / 3600.0

  val d = direction.uppercaseChar()
  if (d == 'S' || d == 'G' || d == 'W' || d == 'B') {
    decimal *= -1
  }

  return decimal
}

data class DmsTriple(
  val degrees: String,
  val minutes: String,
  val seconds: String,
  val direction: String
)

fun decimalToDmsParts(decimal: Double, isLatitude: Boolean): DmsTriple {
  val dir = if (isLatitude) {
    if (decimal >= 0) "Kuzey" else "Güney"
  } else {
    if (decimal >= 0) "Doğu" else "Batı"
  }
  val absVal = kotlin.math.abs(decimal)
  val d = absVal.toInt()
  val remMin = (absVal - d) * 60.0
  val m = remMin.toInt()
  val s = (remMin - m) * 60.0
  val secRounded = Math.round(s * 10.0) / 10.0
  return DmsTriple(
    degrees = if (isLatitude) String.format(Locale.US, "%02d", d) else String.format(Locale.US, "%03d", d),
    minutes = String.format(Locale.US, "%02d", m),
    seconds = String.format(Locale.US, "%.1f", secRounded),
    direction = dir
  )
}

data class EtaSummaryReceipt(
  val targetName: String? = null,
  val latDms: String,
  val lonDms: String,
  val distanceNm: Double,
  val bearingDegrees: Int,
  val speedKnots: Double,
  val etaTime: String,
  val fullEtaDate: String = ""
)

data class SimpleEtaResult(
  val destinationName: String,
  val distanceNm: Double,
  val speedKnots: Double,
  val durationStr: String,
  val etaStr: String,
  val bearingDegrees: Int = 0,
  val latDms: String = "",
  val lonDms: String = "",
  val etaTimeOnly: String = ""
)

object TurkishPorts {
  val regions = linkedMapOf(
    "Adalar" to listOf(
      SimpleDestination("Büyükada Limanı (İstanbul)", 40.8744, 29.1286),
      SimpleDestination("Heybeliada İskelesi (İstanbul)", 40.8767, 29.0983),
      SimpleDestination("Kınalıada İskelesi (İstanbul)", 40.9097, 29.0533),
      SimpleDestination("Burgazada İskelesi (İstanbul)", 40.8806, 29.0625),
      SimpleDestination("Sedef Adası (İstanbul)", 40.8533, 29.1417),
      SimpleDestination("Yassıada (Demokrasi & Özgürlükler)", 40.8650, 28.9950),
      SimpleDestination("Bozcaada Limanı (Çanakkale)", 39.8333, 26.0667),
      SimpleDestination("Gökçeada (Kuzu Limanı)", 40.2333, 25.9000),
      SimpleDestination("Gökçeada (Uğurlu Limanı)", 40.1283, 25.6883),
      SimpleDestination("Gökçeada (Kaleköy İskelesi)", 40.2300, 25.8900),
      SimpleDestination("Marmara Adası (Merkez)", 40.5833, 27.5500),
      SimpleDestination("Marmara Adası (Saraylar)", 40.6550, 27.6667),
      SimpleDestination("Avşa Adası (Türkeli)", 40.5050, 27.5083),
      SimpleDestination("Paşalimanı Adası", 40.4850, 27.6167),
      SimpleDestination("Ekinlik Adası", 40.5483, 27.4833),
      SimpleDestination("Cunda / Alibey Adası (Ayvalık)", 39.3333, 26.6567),
      SimpleDestination("Uzunada Limanı (İzmir)", 38.4550, 26.7250),
      SimpleDestination("Karaada (Bodrum)", 36.9750, 27.4667),
      SimpleDestination("Sedir Adası (Gökova)", 36.9950, 28.2050),
      SimpleDestination("Şövalye Adası (Fethiye)", 36.6583, 29.0983),
      SimpleDestination("Tersane Adası (Göcek)", 36.6717, 28.9167),
      SimpleDestination("Kekova / Kaleköy (Simena)", 36.1900, 29.8600),
      SimpleDestination("Suluada (Adrasan)", 36.2417, 30.5050),
      SimpleDestination("Kefken Adası (Kocaeli)", 41.2150, 30.2583),
      SimpleDestination("Giresun Adası (Karadeniz)", 40.9283, 38.4350)
    ),
    "İstanbul" to listOf(
      SimpleDestination("Tuzla", 40.8166, 29.2666),
      SimpleDestination("Ambarlı", 40.9666, 28.6833),
      SimpleDestination("Kadıköy", 40.9933, 29.0236),
      SimpleDestination("Bostancı", 40.9525, 29.0944),
      SimpleDestination("Kartal", 40.8847, 29.1866),
      SimpleDestination("Pendik", 40.8753, 29.2319),
      SimpleDestination("Yenikapı", 41.0025, 28.9547),
      SimpleDestination("Karaköy", 41.0225, 28.9772),
      SimpleDestination("Beşiktaş", 41.0422, 29.0069),
      SimpleDestination("Üsküdar", 41.0269, 29.0153),
      SimpleDestination("Bakırköy", 40.9767, 28.8733),
      SimpleDestination("Boğaz (Ahırkapı)", 40.9833, 28.9833),
      SimpleDestination("Sarıyer", 41.1683, 29.0558),
      SimpleDestination("Büyükada", 40.8744, 29.1286),
      SimpleDestination("Heybeliada", 40.8767, 29.0983),
      SimpleDestination("Kınalıada", 40.9097, 29.0533),
      SimpleDestination("Burgazada", 40.8806, 29.0625),
      SimpleDestination("Sedef Adası", 40.8533, 29.1417),
      SimpleDestination("Yassıada", 40.8650, 28.9950),
      SimpleDestination("Şile", 41.1764, 29.6108)
    ),
    "Kocaeli" to listOf(
      SimpleDestination("İzmit", 40.7616, 29.9394),
      SimpleDestination("Derince", 40.7516, 29.8294),
      SimpleDestination("Körfez", 40.7666, 29.7500),
      SimpleDestination("Gölcük", 40.7166, 29.8166),
      SimpleDestination("Diliskelesi", 40.7666, 29.5333),
      SimpleDestination("Eskihisar", 40.7666, 29.4166),
      SimpleDestination("Karamürsel", 40.6933, 29.6167),
      SimpleDestination("Kefken", 41.1744, 30.2158)
    ),
    "Yalova" to listOf(
      SimpleDestination("Merkez", 40.6500, 29.2666),
      SimpleDestination("Topçular", 40.7066, 29.4000),
      SimpleDestination("Çınarcık", 40.6433, 29.1233),
      SimpleDestination("Armutlu", 40.5211, 28.8263),
      SimpleDestination("Altınova", 40.6983, 29.5083)
    ),
    "Bursa" to listOf(
      SimpleDestination("Gemlik", 40.4166, 29.1500),
      SimpleDestination("Mudanya", 40.3833, 28.8833),
      SimpleDestination("Güzelyalı", 40.3700, 28.9050),
      SimpleDestination("Kurşunlu", 40.3800, 29.0400)
    ),
    "Balıkesir" to listOf(
      SimpleDestination("Bandırma", 40.3556, 27.9739),
      SimpleDestination("Erdek", 40.3983, 27.7917),
      SimpleDestination("Ayvalık", 39.3183, 26.6917),
      SimpleDestination("Cunda / Alibey Adası", 39.3333, 26.6567),
      SimpleDestination("Marmara Adası (Merkez)", 40.5833, 27.5500),
      SimpleDestination("Marmara Adası (Saraylar)", 40.6550, 27.6667),
      SimpleDestination("Avşa Adası (Türkeli)", 40.5050, 27.5083),
      SimpleDestination("Paşalimanı Adası", 40.4850, 27.6167),
      SimpleDestination("Ekinlik Adası", 40.5483, 27.4833),
      SimpleDestination("Burhaniye", 39.5033, 26.9683),
      SimpleDestination("Akçay / Edremit", 39.5850, 26.9250)
    ),
    "Tekirdağ" to listOf(
      SimpleDestination("Merkez (Ceyport)", 40.9700, 27.5250),
      SimpleDestination("Marmaraereğlisi", 40.9650, 27.9550),
      SimpleDestination("Şarköy", 40.6133, 27.1150),
      SimpleDestination("Kumbağ", 40.8667, 27.4667)
    ),
    "Çanakkale" to listOf(
      SimpleDestination("Merkez Liman", 40.1500, 26.4000),
      SimpleDestination("Bozcaada Limanı", 39.8333, 26.0667),
      SimpleDestination("Gökçeada (Kuzu Limanı)", 40.2333, 25.9000),
      SimpleDestination("Gökçeada (Uğurlu)", 40.1283, 25.6883),
      SimpleDestination("Gökçeada (Kaleköy)", 40.2300, 25.8900),
      SimpleDestination("Gelibolu", 40.4075, 26.6666),
      SimpleDestination("Lapseki", 40.3452, 26.6852),
      SimpleDestination("Eceabat", 40.1833, 26.3556),
      SimpleDestination("Karabiga", 40.4000, 27.3000)
    ),
    "İzmir" to listOf(
      SimpleDestination("Alsancak", 38.4333, 27.1333),
      SimpleDestination("Karşıyaka", 38.4550, 27.1186),
      SimpleDestination("Bostanlı", 38.4566, 27.0944),
      SimpleDestination("Aliağa (Nemrut)", 38.8333, 26.9666),
      SimpleDestination("Çeşme", 38.3225, 26.3050),
      SimpleDestination("Foça", 38.6683, 26.7550),
      SimpleDestination("Dikili", 39.0717, 26.8850),
      SimpleDestination("Urla", 38.3617, 26.7717),
      SimpleDestination("Sığacık", 38.1967, 26.7867),
      SimpleDestination("Çandarlı", 38.9333, 27.0167)
    ),
    "Aydın" to listOf(
      SimpleDestination("Kuşadası Liman", 37.8633, 27.2567),
      SimpleDestination("Didim (D-Marin)", 37.3367, 27.2567)
    ),
    "Muğla" to listOf(
      SimpleDestination("Bodrum", 37.0333, 27.4333),
      SimpleDestination("Karaada (Bodrum)", 36.9750, 27.4667),
      SimpleDestination("Marmaris", 36.8500, 28.2666),
      SimpleDestination("Sedir Adası (Gökova)", 36.9950, 28.2050),
      SimpleDestination("Fethiye", 36.6217, 29.1111),
      SimpleDestination("Şövalye Adası (Fethiye)", 36.6583, 29.0983),
      SimpleDestination("Göcek", 36.7533, 28.9417),
      SimpleDestination("Tersane Adası (Göcek)", 36.6717, 28.9167),
      SimpleDestination("Datça", 36.7217, 27.6883),
      SimpleDestination("Yalıkavak", 37.1067, 27.2917),
      SimpleDestination("Turgutreis", 37.0017, 27.2550),
      SimpleDestination("Güllük", 37.2483, 27.6017),
      SimpleDestination("Bozburun", 36.6917, 28.0433)
    ),
    "Antalya" to listOf(
      SimpleDestination("Merkez (Port Akdeniz)", 36.8333, 30.6166),
      SimpleDestination("Alanya Limanı", 36.5333, 32.0000),
      SimpleDestination("Kaş Limanı", 36.1995, 29.6385),
      SimpleDestination("Kekova / Simena Adası", 36.1900, 29.8600),
      SimpleDestination("Kemer Marina", 36.6017, 30.5650),
      SimpleDestination("Finike Marina", 36.2950, 30.1517),
      SimpleDestination("Suluada (Adrasan)", 36.2417, 30.5050),
      SimpleDestination("Manavgat", 36.7833, 31.4333),
      SimpleDestination("Kalkan", 36.2650, 29.4150)
    ),
    "Mersin" to listOf(
      SimpleDestination("Merkez Liman (MIP)", 36.7833, 34.6333),
      SimpleDestination("Taşucu", 36.3166, 33.8833),
      SimpleDestination("Anamur", 36.0750, 32.8367),
      SimpleDestination("Erdemli", 36.6050, 34.3083)
    ),
    "Adana" to listOf(
      SimpleDestination("BOTAŞ / Ceyhan", 36.8833, 35.9167),
      SimpleDestination("Yumurtalık", 36.7667, 35.7833),
      SimpleDestination("Karataş", 36.5800, 35.3900)
    ),
    "Hatay" to listOf(
      SimpleDestination("İskenderun Limanı", 36.5950, 36.1750),
      SimpleDestination("Dörtyol Terminali", 36.8450, 36.1783),
      SimpleDestination("Arsuz", 36.4167, 35.8833),
      SimpleDestination("Samandağ (Çevlik)", 36.1267, 35.9183)
    ),
    "Zonguldak" to listOf(
      SimpleDestination("Merkez Liman", 41.4567, 31.7883),
      SimpleDestination("Kdz. Ereğli", 41.2833, 31.4166),
      SimpleDestination("Filyos Limanı", 41.5733, 32.0250)
    ),
    "Bartın" to listOf(
      SimpleDestination("Merkez Liman", 41.6850, 32.2250),
      SimpleDestination("Amasra", 41.7500, 32.3867),
      SimpleDestination("Kurucaşile", 41.8483, 32.7217)
    ),
    "Kastamonu" to listOf(
      SimpleDestination("İnebolu Limanı", 41.9783, 33.7667),
      SimpleDestination("Cide Limanı", 41.8950, 32.9617),
      SimpleDestination("Abana", 41.9783, 34.0150)
    ),
    "Sinop" to listOf(
      SimpleDestination("Merkez Liman", 42.0267, 35.1550),
      SimpleDestination("Gerze", 41.8017, 35.1967),
      SimpleDestination("Ayancık", 41.9483, 34.5867)
    ),
    "Samsun" to listOf(
      SimpleDestination("Merkez (Samsunport)", 41.2967, 36.3450),
      SimpleDestination("Tekkeköy", 41.2400, 36.4550),
      SimpleDestination("Bafra", 41.5667, 35.9083)
    ),
    "Ordu" to listOf(
      SimpleDestination("Merkez Liman", 40.9883, 37.8867),
      SimpleDestination("Ünye Limanı", 41.1350, 37.2883),
      SimpleDestination("Fatsa", 41.0333, 37.5017)
    ),
    "Giresun" to listOf(
      SimpleDestination("Merkez Liman", 40.9200, 38.3900),
      SimpleDestination("Giresun Adası", 40.9283, 38.4350),
      SimpleDestination("Espiye", 40.9517, 38.7117),
      SimpleDestination("Tirebolu", 41.0083, 38.8150),
      SimpleDestination("Görele", 41.0350, 39.0383)
    ),
    "Trabzon" to listOf(
      SimpleDestination("Merkez Liman", 41.0050, 39.7367),
      SimpleDestination("Akçaabat", 41.0217, 39.5717),
      SimpleDestination("Sürmene", 40.9133, 40.1183),
      SimpleDestination("Of", 40.9450, 40.2633)
    ),
    "Rize" to listOf(
      SimpleDestination("Merkez Liman", 41.0317, 40.5233),
      SimpleDestination("Çayeli", 41.0917, 40.7283),
      SimpleDestination("Pazar", 41.1783, 40.8850),
      SimpleDestination("Fındıklı", 41.2683, 41.1400)
    ),
    "Artvin" to listOf(
      SimpleDestination("Hopa Limanı", 41.4083, 41.4317),
      SimpleDestination("Arhavi", 41.3533, 41.3067)
    ),
    "Kırklareli" to listOf(
      SimpleDestination("İğneada Limanı", 41.8783, 27.9867),
      SimpleDestination("Kıyıköy", 41.6350, 28.0933)
    ),
    "KKTC / Kıbrıs" to listOf(
      SimpleDestination("Girne Turizm Limanı", 35.3417, 33.3283),
      SimpleDestination("Gazimağusa Ticaret Limanı", 35.1233, 33.9483)
    )
  )

  val list: List<SimpleDestination> get() = regions.values.flatten()
}

fun calculateSimpleEta(
  startLat: Double,
  startLon: Double,
  speed: Double,
  dest: SimpleDestination
): SimpleEtaResult {
  val dist = com.example.model.calculateHaversineDistanceNm(startLat, startLon, dest.lat, dest.lon)
  val bearing = com.example.model.LocationPresets.calculateBearingDegrees(startLat, startLon, dest.lat, dest.lon)
  val isDefaultSpeed = speed < 0.1
  val safeSpeed = if (isDefaultSpeed) 12.0 else speed

  val totalHours = dist / safeSpeed
  val totalMins = (totalHours * 60).roundToInt()
  val hrs = totalMins / 60
  val mins = totalMins % 60
  val durStr = if (isDefaultSpeed) "${hrs}sa ${mins}dk (12 kn)" else "${hrs}sa ${mins}dk"

  val etaMillis = System.currentTimeMillis() + (totalMins * 60L * 1000L)
  val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
  val timeOnlySdf = SimpleDateFormat("HH:mm", Locale.US)
  val etaStr = sdf.format(Date(etaMillis))
  val timeOnlyStr = timeOnlySdf.format(Date(etaMillis))

  val latParts = decimalToDmsParts(dest.lat, true)
  val lonParts = decimalToDmsParts(dest.lon, false)
  val latDms = "${latParts.degrees}°${latParts.minutes}'${latParts.seconds}\"${latParts.direction}"
  val lonDms = "${lonParts.degrees}°${lonParts.minutes}'${lonParts.seconds}\"${lonParts.direction}"

  return SimpleEtaResult(
    destinationName = dest.name,
    distanceNm = Math.round(dist * 10.0) / 10.0,
    speedKnots = Math.round(safeSpeed * 10.0) / 10.0,
    durationStr = durStr,
    etaStr = etaStr,
    bearingDegrees = bearing,
    latDms = latDms,
    lonDms = lonDms,
    etaTimeOnly = timeOnlyStr
  )
}

fun computeEtaSummary(
  startLat: Double,
  startLon: Double,
  speedKnots: Double,
  targetCoord: Coordinate,
  targetName: String? = null
): EtaSummaryReceipt {
  val dist = com.example.model.calculateHaversineDistanceNm(startLat, startLon, targetCoord.latitude, targetCoord.longitude)
  val bearing = com.example.model.LocationPresets.calculateBearingDegrees(startLat, startLon, targetCoord.latitude, targetCoord.longitude)
  val safeSpeed = if (speedKnots < 0.5) 12.0 else speedKnots

  val totalHours = dist / safeSpeed
  val totalMins = (totalHours * 60).roundToInt()

  val etaMillis = System.currentTimeMillis() + (totalMins * 60L * 1000L)
  val timeSdf = SimpleDateFormat("HH:mm", Locale.US)
  val fullDateSdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
  val etaTimeStr = timeSdf.format(Date(etaMillis))
  val fullDateStr = fullDateSdf.format(Date(etaMillis))

  val latParts = decimalToDmsParts(targetCoord.latitude, true)
  val lonParts = decimalToDmsParts(targetCoord.longitude, false)
  val latDms = "${latParts.degrees}°${latParts.minutes}'${latParts.seconds}\"${latParts.direction}"
  val lonDms = "${lonParts.degrees}°${lonParts.minutes}'${lonParts.seconds}\"${lonParts.direction}"

  return EtaSummaryReceipt(
    targetName = targetName,
    latDms = latDms,
    lonDms = lonDms,
    distanceNm = Math.round(dist * 10.0) / 10.0,
    bearingDegrees = bearing,
    speedKnots = Math.round(safeSpeed * 10.0) / 10.0,
    etaTime = etaTimeStr,
    fullEtaDate = fullDateStr
  )
}
