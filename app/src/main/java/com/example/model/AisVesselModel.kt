package com.example.model

/**
 * Otomatik Tanımlama Sistemi (AIS) ve MarineTraffic Gemi Veri Modeli
 */
data class AisVesselData(
  val shipId: String = "222111447",
  val name: String = "",
  val mmsi: String = "222111447",
  val imo: String = "7654320",
  val callSign: String = "TST7",
  val flag: String = "TR (Türkiye)",
  val shipType: String = "Military Ops",
  val status: String = "Demirde / Beklemede (Military Ops)",
  val latitude: Double = 40.82833,
  val longitude: Double = 29.25399,
  val sogKnots: Double = 0.0,
  val cogDegrees: Double = 270.0,
  val headingDegrees: Int = 270,
  val loaMeters: Double = 98.0,
  val beamMeters: Double = 13.5,
  val draftMeters: Double = 3.8,
  val grossTonnage: Int = 2450,
  val deadweightTon: Int = 3200,
  val yearBuilt: Int = 2022,
  val destination: String = "Marmara Denizi / İstanbul limanı yönü",
  val eta: String = "21 Ağustos 2026, 21:19 UTC",
  val lastReportedTime: String = "21 Ağustos 2026, 21:19 UTC",
  val marineTrafficUrl: String = "https://www.marinetraffic.com/en/ais/details/ships/shipid:222111447",
  val myShipTrackingUrl: String = "https://www.myshiptracking.com/?mmsi=222111447",
  val isLiveAis: Boolean = true,
  val isApiKeyActive: Boolean = false,
  val apiProvider: String = "MarineTraffic API"
)


