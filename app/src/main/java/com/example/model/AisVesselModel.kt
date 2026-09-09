package com.example.model

/**
 * Otomatik Tanımlama Sistemi (AIS) Gemi Veri Modeli
 */
data class AisVesselData(
  val shipId: String = "",
  val name: String = "",
  val mmsi: String = "",
  val imo: String = "",
  val callSign: String = "",
  val flag: String = "",
  val shipType: String = "",
  val status: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val sogKnots: Double = 0.0,
  val cogDegrees: Double = 0.0,
  val headingDegrees: Int = 0,
  val loaMeters: Double = 0.0,
  val beamMeters: Double = 0.0,
  val draftMeters: Double = 0.0,
  val grossTonnage: Int = 0,
  val deadweightTon: Int = 0,
  val yearBuilt: Int = 0,
  val destination: String = "",
  val eta: String = "",
  val lastReportedTime: String = "",
  val marineTrafficUrl: String = "",
  val myShipTrackingUrl: String = "",
  val isLiveAis: Boolean = false,
  val isApiKeyActive: Boolean = false,
  val apiProvider: String = "AIS Canlı Veri Servisi"
)


