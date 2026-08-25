package com.example.model

data class VesselProfile(
  val id: String,
  val name: String,
  val typeName: String,
  val loaMeters: Double, // Gemi Boyu (LOA)
  val beamMeters: Double, // Gemi Eni (Beam)
  val draftMeters: Double, // Draft / Su Çekimi
  val blockCoefficient: Double = 0.75, // Cb (Blok katsayısı)
  val defaultSpeedKnots: Double = 8.0, // Seyir hızı
  val minUkcMeters: Double = 1.0 // Minimum Omurga Altı Açıklığı (UKC)
)

object VesselPresets {
  val defaultVessels = listOf(
    VesselProfile(
      id = "tcg_izmir_military",
      name = "",
      typeName = "Military Ops",
      loaMeters = 98.0,
      beamMeters = 13.5,
      draftMeters = 3.8,
      blockCoefficient = 0.55,
      defaultSpeedKnots = 0.0,
      minUkcMeters = 1.0
    ),
    VesselProfile(
      id = "tcg_izmir",
      name = "TCG İZMİR",
      typeName = "MİLGEM İstif Sınıfı Fırkateyn",
      loaMeters = 113.2,
      beamMeters = 14.4,
      draftMeters = 4.05,
      blockCoefficient = 0.52,
      defaultSpeedKnots = 15.0,
      minUkcMeters = 1.2
    ),
    VesselProfile(
      id = "general_cargo",
      name = "MV Ege Star",
      typeName = "Genel Kargo / Koster",
      loaMeters = 110.0,
      beamMeters = 15.0,
      draftMeters = 5.5,
      blockCoefficient = 0.72,
      defaultSpeedKnots = 8.5,
      minUkcMeters = 0.8
    ),
    VesselProfile(
      id = "handymax",
      name = "MV Anatolia Carrier",
      typeName = "Handymax Dökme Yük",
      loaMeters = 185.0,
      beamMeters = 30.0,
      draftMeters = 10.5,
      blockCoefficient = 0.82,
      defaultSpeedKnots = 10.0,
      minUkcMeters = 1.2
    ),
    VesselProfile(
      id = "container_vessel",
      name = "Marmara Express",
      typeName = "Konteyner Gemisi",
      loaMeters = 220.0,
      beamMeters = 32.0,
      draftMeters = 11.5,
      blockCoefficient = 0.65,
      defaultSpeedKnots = 14.0,
      minUkcMeters = 1.5
    ),
    VesselProfile(
      id = "chem_tanker",
      name = "MT Bosphorus Glory",
      typeName = "Kimyasal / Ürün Tankeri",
      loaMeters = 140.0,
      beamMeters = 22.0,
      draftMeters = 8.2,
      blockCoefficient = 0.78,
      defaultSpeedKnots = 9.5,
      minUkcMeters = 1.0
    ),
    VesselProfile(
      id = "yacht_boat",
      name = "Poseidon Blue",
      typeName = "Özel Yat / Tekne",
      loaMeters = 24.0,
      beamMeters = 6.2,
      draftMeters = 2.2,
      blockCoefficient = 0.45,
      defaultSpeedKnots = 12.0,
      minUkcMeters = 0.5
    ),
    VesselProfile(
      id = "custom_ship",
      name = "Özel Gemi Tanımı",
      typeName = "Kullanıcı Tanımlı",
      loaMeters = 100.0,
      beamMeters = 16.0,
      draftMeters = 6.0,
      blockCoefficient = 0.70,
      defaultSpeedKnots = 8.0,
      minUkcMeters = 1.0
    )
  )
}

