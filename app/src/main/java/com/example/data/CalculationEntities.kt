package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Demirleme ve Salma Dairesi Hesaplama Kaydı (Room Entity)
 */
@Entity(tableName = "anchor_calculations")
data class AnchorCalculationRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val vesselName: String,
  val shackleStandardLabel: String, // "27,5 m (Standart)" veya "25,0 m (Metrik)"
  val metersPerShackle: Double, // 27.5 veya 25.0
  val shacklesCount: Double, // Döşenen kilit sayısı
  val chainScopeMeters: Double, // a: Gemi demir zinciri kaloması (m)
  val depthMeters: Double, // b: Derinlik (m)
  val horizontalDistanceMeters: Double, // c: Loça - demir yatay mesafe (m)
  val distBridgeToHawseMeters: Double, // K/Ü - Loça mesafesi
  val distBridgeToSternMeters: Double, // K/Ü - Kıç mesafesi
  val vesselLoaMeters: Double, // LOA Gemi Tam Boyu
  val firstSwingingRadiusMeters: Double, // d = c + K/Ü->Loça (1. Salma Dairesi)
  val secondSwingingRadiusBridgeMeters: Double, // e = c + K/Ü->Kıç
  val secondSwingingRadiusTotalMeters: Double, // f = c + LOA (2. Salma Dairesi)
  val safetyStatus: String, // "GÜVENLİ", "SIĞ SU", "FIRTINA KALOMASI" vb.
  val note: String = ""
) {
  val formattedDate: String
    get() {
      val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }
}

/**
 * Gelgit ve Seyir Emniyeti Hesaplama Kaydı (Room Entity)
 */
@Entity(tableName = "tide_calculations")
data class TideCalculationRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val portName: String,
  val highTideHeightMeters: Double,
  val lowTideHeightMeters: Double,
  val highTideTime: String,
  val lowTideTime: String,
  val chartDatumDepthMeters: Double,
  val shipDraftMeters: Double,
  val requiredUkcMeters: Double,
  val currentInstantDepthMeters: Double,
  val currentInstantUkcMeters: Double,
  val isCurrentlySafe: Boolean,
  val safeWindowSummary: String,
  val note: String = ""
) {
  val formattedDate: String
    get() {
      val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }
}
