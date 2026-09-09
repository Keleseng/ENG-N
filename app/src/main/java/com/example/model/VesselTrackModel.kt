package com.example.model

/**
 * Gemi Geçmiş Rota / İz Noktası Modeli
 */
data class VesselTrackPoint(
  val latitude: Double,
  val longitude: Double,
  val speedKnots: Double = 0.0,
  val headingDegrees: Int = 0,
  val timestampEpochMs: Long = System.currentTimeMillis(),
  val timeFormatted: String = ""
)

/**
 * Deniz Seyir Haritası Katman Tipleri
 */
enum class MarineMapLayer(
  val displayName: String,
  val shortBadge: String,
  val description: String
) {
  OPEN_SEA_MAP_WEB(
    displayName = "Resmi OpenSeaMap Canlı Portalı",
    shortBadge = "OpenSeaMap Canlı",
    description = "Resmi OpenSeaMap Web Portalı (map2.openseamap.org - Canlı Fener & Şamandıra Haritası)"
  ),
  OPEN_SEA_MAP(
    displayName = "OpenSeaMap ECDIS",
    shortBadge = "ECDIS Taktik Harita",
    description = "Uluslararası Açık Deniz Seyir Haritası, Fenerler, Şamandıralar ve Seyir Hatları"
  ),
  MARINE_TRAFFIC_LIVE(
    displayName = "Canlı Harita & AIS Trafiği",
    shortBadge = "AIS Canlı",
    description = "Gerçek Zamanlı Küresel Gemi Trafiği, Canlı Pozisyonlar ve Seyir Rotaları"
  ),
  NAVIONICS_CMAP(
    displayName = "Navionics / C-MAP Batimetri",
    shortBadge = "C-MAP / Navionics",
    description = "Hidrografik Derinlik Eğrileri, Sığlık Konturları ve Güvenli Su Koridorları"
  ),
  NOAA_ENC(
    displayName = "NOAA ENC Resmi Seyir Haritası",
    shortBadge = "NOAA ENC",
    description = "Elektronik Seyir Haritaları (ENC), Seyir Emniyet Sınırları & Ayrım Hatları"
  ),
  SATELLITE_SEAMARKS(
    displayName = "Uydu Hibrit + Deniz İşaretleri",
    shortBadge = "Uydu + Seamark",
    description = "Yüksek Çözünürlüklü Uydu Görüntüsü + OpenSeaMap Seyir Katmanı"
  ),
  VESSEL_FINDER(
    displayName = "VesselFinder Canlı Trafik",
    shortBadge = "VesselFinder AIS",
    description = "Küresel Canlı Gemi Trafiği ve Çevre Gemi AIS Pozisyonları"
  )
}
