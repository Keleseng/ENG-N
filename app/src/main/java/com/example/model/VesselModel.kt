package com.example.model

data class VesselProfile(
  val id: String = "custom_ship",
  val name: String = "",
  val typeName: String = "",
  val mmsi: String = "",
  val loaMeters: Double = 0.0, // Gemi Boyu (LOA)
  val beamMeters: Double = 0.0, // Gemi Eni (Beam)
  val draftMeters: Double = 0.0, // Draft / Su Çekimi
  val blockCoefficient: Double = 0.70, // Cb (Blok katsayısı)
  val defaultSpeedKnots: Double = 0.0, // Seyir hızı
  val minUkcMeters: Double = 1.0 // Minimum Omurga Altı Açıklığı (UKC)
)

object VesselPresets {
  val defaultVessels = emptyList<VesselProfile>()
}


