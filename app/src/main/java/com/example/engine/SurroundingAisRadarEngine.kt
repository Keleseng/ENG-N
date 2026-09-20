package com.example.engine

import com.example.model.RadarAisTarget
import kotlin.math.*
import kotlin.random.Random

/**
 * Çevredeki AIS gemilerini hesaplayan ve yöneten Radar Simülasyon / Telemetri Motoru.
 * Kullanıcının anlık konumuna göre menzil içindeki gemilerin relatif kerteriz,
 * mesafe ve çatışma parametrelerini (CPA/TCPA) üretir ve günceller.
 */
object SurroundingAisRadarEngine {

  // Önceden tanımlı gerçekçi gemi şablonları
  private val baseVesselTemplates = listOf(
    Template(
      name = "M/T MARMARA STAR",
      mmsi = "271043819",
      imo = "9451234",
      callSign = "TCA81",
      flag = "TR",
      shipType = "Petrol/Kimyasal Tanker",
      status = "Yolda Motorla Seyrediyor",
      relDistanceNm = 2.4,
      relBearingDeg = 42.0,
      sogKnots = 11.2,
      cogDegrees = 228.0,
      loa = 175.0,
      beam = 28.0,
      draft = 8.5,
      destination = "TUZLA TERSANELER",
      eta = "Bugün 16:30"
    ),
    Template(
      name = "M/V OSMAN BEY",
      mmsi = "271002341",
      imo = "9128876",
      callSign = "TCB44",
      flag = "TR",
      shipType = "Genel Kargo",
      status = "Yolda Motorla Seyrediyor",
      relDistanceNm = 3.8,
      relBearingDeg = 115.0,
      sogKnots = 9.4,
      cogDegrees = 295.0,
      loa = 128.0,
      beam = 19.5,
      draft = 6.4,
      destination = "AMBARLI LİMANI",
      eta = "Bugün 20:00"
    ),
    Template(
      name = "C/V BOSPHORUS EXP",
      mmsi = "248912000",
      imo = "9634567",
      callSign = "9HA43",
      flag = "MT (Malta)",
      shipType = "Konteyner Gemisi",
      status = "Yolda Motorla Seyrediyor",
      relDistanceNm = 5.2,
      relBearingDeg = 310.0,
      sogKnots = 15.6,
      cogDegrees = 135.0,
      loa = 225.0,
      beam = 32.2,
      draft = 10.8,
      destination = "İSTANBUL BOĞAZI",
      eta = "Bugün 17:45"
    ),
    Template(
      name = "KURTARMA 9",
      mmsi = "271000109",
      imo = "9345601",
      callSign = "TCK09",
      flag = "TR",
      shipType = "Eskort & Çekici Römorkör",
      status = "Kılavuz/Römorkör Hizmeti",
      relDistanceNm = 1.1,
      relBearingDeg = 185.0,
      sogKnots = 6.5,
      cogDegrees = 010.0,
      loa = 34.0,
      beam = 11.0,
      draft = 4.2,
      destination = "TUZLA DEMİR SAHASI",
      eta = "Sürekli Görev"
    ),
    Template(
      name = "KILAVUZ 4",
      mmsi = "271015040",
      imo = "0000000",
      callSign = "TCL04",
      flag = "TR",
      shipType = "Pilot Botu",
      status = "Kılavuz Kaptan İntikalinde",
      relDistanceNm = 0.9,
      relBearingDeg = 75.0,
      sogKnots = 14.8,
      cogDegrees = 255.0,
      loa = 16.5,
      beam = 4.8,
      draft = 1.5,
      destination = "PILOT İSTASYONU",
      eta = "Anlık"
    ),
    Template(
      name = "DENİZ OTOBÜSÜ BURAK REİS",
      mmsi = "271001890",
      imo = "9201944",
      callSign = "TCD02",
      flag = "TR",
      shipType = "Yüksek Hızlı Yolcu Gemisi (HSC)",
      status = "Yolda Motorla Seyrediyor",
      relDistanceNm = 4.1,
      relBearingDeg = 245.0,
      sogKnots = 24.2,
      cogDegrees = 062.0,
      loa = 42.0,
      beam = 10.5,
      draft = 1.8,
      destination = "YENİKAPI / KADIKÖY",
      eta = "Bugün 16:15"
    ),
    Template(
      name = "SG 71 SAHİL GÜVENLİK",
      mmsi = "271999071",
      imo = "0000000",
      callSign = "TCSG71",
      flag = "TR",
      shipType = "Karakol & Arama Kurtarma",
      status = "Askeri / Güvenlik Devriyesi",
      relDistanceNm = 2.9,
      relBearingDeg = 350.0,
      sogKnots = 18.0,
      cogDegrees = 175.0,
      loa = 40.0,
      beam = 7.5,
      draft = 2.4,
      destination = "DEVRİYE BÖLGESİ",
      eta = "Görevde"
    ),
    Template(
      name = "M/V ANATOLIA PEARL",
      mmsi = "271008765",
      imo = "9081239",
      callSign = "TCP88",
      flag = "TR",
      shipType = "Dökme Yük Gemisi",
      status = "Demirde (At Anchor)",
      relDistanceNm = 1.7,
      relBearingDeg = 140.0,
      sogKnots = 0.1,
      cogDegrees = 045.0,
      loa = 185.0,
      beam = 30.0,
      draft = 9.2,
      destination = "DEMİR SAHASI BEKLEME",
      eta = "Demirde"
    ),
    Template(
      name = "S/Y RÜZGAR GÜLÜ",
      mmsi = "271049912",
      imo = "0000000",
      callSign = "TCY12",
      flag = "TR",
      shipType = "Yelkenli Yat",
      status = "Yelken / Motorla Seyrediyor",
      relDistanceNm = 1.4,
      relBearingDeg = 290.0,
      sogKnots = 5.2,
      cogDegrees = 110.0,
      loa = 14.5,
      beam = 4.2,
      draft = 2.1,
      destination = "KALAMIŞ MARİNA",
      eta = "Bugün 18:30"
    ),
    Template(
      name = "M/T BALTIC PHOENIX",
      mmsi = "255805900",
      imo = "9488310",
      callSign = "CQAV",
      flag = "PT (Portekiz)",
      shipType = "Ham Petrol Tankeri",
      status = "Yolda Motorla Seyrediyor",
      relDistanceNm = 6.8,
      relBearingDeg = 205.0,
      sogKnots = 12.8,
      cogDegrees = 025.0,
      loa = 248.0,
      beam = 43.0,
      draft = 13.5,
      destination = "NOVOROSSIYSK",
      eta = "14 Eylül 12:00"
    )
  )

  private data class Template(
    val name: String,
    val mmsi: String,
    val imo: String,
    val callSign: String,
    val flag: String,
    val shipType: String,
    val status: String,
    val relDistanceNm: Double,
    val relBearingDeg: Double,
    val sogKnots: Double,
    val cogDegrees: Double,
    val loa: Double,
    val beam: Double,
    val draft: Double,
    val destination: String,
    val eta: String
  )

  /**
   * Belirtilen merkez konuma (kendi gemimiz) göre çevre AIS hedeflerini üretir.
   */
  fun generateSurroundingVessels(
    centerLat: Double,
    centerLon: Double,
    ownSog: Double = 0.0,
    ownCog: Double = 0.0
  ): List<RadarAisTarget> {
    return baseVesselTemplates.mapIndexed { index, t ->
      // Relatif kerteriz ve mesafeden enlem/boylam koordinatını hesapla (Great Circle / Rhumb line)
      val (tgtLat, tgtLon) = calculateCoordinateFromBearingAndDistance(
        centerLat,
        centerLon,
        t.relBearingDeg,
        t.relDistanceNm
      )

      val target = RadarAisTarget(
        id = "ais_tgt_${t.mmsi}",
        name = t.name,
        mmsi = t.mmsi,
        imo = t.imo,
        callSign = t.callSign,
        flag = t.flag,
        shipType = t.shipType,
        status = t.status,
        latitude = tgtLat,
        longitude = tgtLon,
        sogKnots = t.sogKnots,
        cogDegrees = t.cogDegrees,
        headingDegrees = t.cogDegrees.toInt(),
        loaMeters = t.loa,
        beamMeters = t.beam,
        draftMeters = t.draft,
        destination = t.destination,
        eta = t.eta
      )

      val (cpa, tcpa) = target.calculateCpaTcpa(centerLat, centerLon, ownSog, ownCog)
      val isHazardous = cpa < 0.8 && tcpa in 0.0..20.0 && t.sogKnots > 1.0

      target.copy(isHazardous = isHazardous)
    }
  }

  /**
   * Hareket simülasyonu adımı: Gemileri hız ve rotalarına göre küçük zaman diliminde (örn: 3 saniyede) ilerletir.
   */
  fun stepVessels(
    targets: List<RadarAisTarget>,
    centerLat: Double,
    centerLon: Double,
    ownSog: Double,
    ownCog: Double,
    deltaSeconds: Double = 2.0
  ): List<RadarAisTarget> {
    return targets.map { target ->
      if (target.sogKnots < 0.2) return@map target // Demirde

      // İlerleme mesafesi (NM) = Hız (kn) * (saniye / 3600)
      val distStepNm = target.sogKnots * (deltaSeconds / 3600.0)
      val (newLat, newLon) = calculateCoordinateFromBearingAndDistance(
        target.latitude,
        target.longitude,
        target.cogDegrees,
        distStepNm
      )

      val updated = target.copy(latitude = newLat, longitude = newLon)
      val (cpa, tcpa) = updated.calculateCpaTcpa(centerLat, centerLon, ownSog, ownCog)
      val isHazardous = cpa < 0.8 && tcpa in 0.0..20.0 && updated.sogKnots > 1.0

      updated.copy(isHazardous = isHazardous)
    }
  }

  /**
   * Başlangıç noktası, kerteriz (derece) ve mesafeden (Deniz Mili) varış koordinatı bulma.
   */
  fun calculateCoordinateFromBearingAndDistance(
    startLat: Double,
    startLon: Double,
    bearingDeg: Double,
    distanceNm: Double
  ): Pair<Double, Double> {
    val rEarthKm = 6371.0
    val distKm = distanceNm * 1.852
    val δ = distKm / rEarthKm
    val θ = Math.toRadians(bearingDeg)
    val φ1 = Math.toRadians(startLat)
    val λ1 = Math.toRadians(startLon)

    val sinφ2 = sin(φ1) * cos(δ) + cos(φ1) * sin(δ) * cos(θ)
    val φ2 = asin(sinφ2)
    val y = sin(θ) * sin(δ) * cos(φ1)
    val x = cos(δ) - sin(φ1) * sin(φ2)
    val λ2 = λ1 + atan2(y, x)

    val latDeg = Math.toDegrees(φ2)
    val lonDeg = (Math.toDegrees(λ2) + 540.0) % 360.0 - 180.0
    return Pair(latDeg, lonDeg)
  }
}
