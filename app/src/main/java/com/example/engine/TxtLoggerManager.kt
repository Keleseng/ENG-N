package com.example.engine

import android.content.Context
import com.example.model.LocationPresets
import com.example.ui.LocationSource
import com.example.ui.TideUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TxtLogRecord(
  val id: String = UUID.randomUUID().toString(),
  val timestampMillis: Long = System.currentTimeMillis(),
  val timestampFormatted: String,
  val isAutoLog: Boolean = true,
  val fileName: String,
  val filePath: String,
  val fileSizeBytes: Long = 0,
  val summarySnippet: String = "",
  val txtContent: String = ""
)

object TxtLoggerManager {

  private const val LOG_DIR_NAME = "marine_txt_logs"

  private fun getLogDir(context: Context): File {
    val dir = File(context.filesDir, LOG_DIR_NAME)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun generateTxtContent(uiState: TideUiState, isAuto: Boolean): String {
    val now = Date()
    val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
    val formattedTime = sdfFull.format(now)

    val vesselName = if (uiState.vesselName.isNotBlank()) uiState.vesselName else uiState.selectedVessel.name
    val mmsi = if (uiState.mmsiStr.isNotBlank()) uiState.mmsiStr else "BİLİNMİYOR"
    val imo = if (uiState.imoStr.isNotBlank()) uiState.imoStr else "-"
    val callSign = if (uiState.callSignStr.isNotBlank()) uiState.callSignStr else "-"
    val loa = if (uiState.loaStr.isNotBlank()) "${uiState.loaStr} m" else "${uiState.selectedVessel.loaMeters} m"
    val beam = if (uiState.beamStr.isNotBlank()) "${uiState.beamStr} m" else "${uiState.selectedVessel.beamMeters} m"

    val latVal = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
    val lonVal = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
    val marineCoordStr = LocationPresets.formatMarineCoordinates(latVal, lonVal)
    val decimalCoordStr = String.format(Locale.US, "%.6f, %.6f", latVal, lonVal)

    val locSourceStr = when (uiState.activeLocationSource) {
      LocationSource.GPS -> "CANLI GPS DÜZELTMESİ"
      LocationSource.AIS -> "CANLI AIS SEYİR TELEMETRİSİ"
      LocationSource.NONE -> "MANUEL GİRİŞ / LİMAN PRESETİ"
    }

    val hdgStr = uiState.headingDegreesStr.ifBlank { "270" }
    val sogStr = uiState.speedStr.ifBlank { "0.0" }
    val cogStr = uiState.headingDegreesStr.ifBlank { "270" }

    val analysis = uiState.analysis
    val ukcVal = analysis.currentInstantUkcMeters
    val isSafe = analysis.isCurrentlySafe
    val safetyStr = if (isSafe) "EMNİYETLİ (UKC > Sınır)" else "DİKKAT / RİSKLİ (UKC < Sınır)"

    val wind = uiState.marineWeather.windSpeedKnots
    val windDir = uiState.marineWeather.windDirectionDegrees
    val windDirName = uiState.marineWeather.windDirectionCardinal
    val windBeaufort = uiState.marineWeather.beaufortDescription

    val current = uiState.marineWeather.oceanCurrentSpeedKnots
    val currentDir = uiState.marineWeather.oceanCurrentDirectionDegrees

    val anchor = uiState.anchorEvent
    val anchorStatusStr = if (anchor.isAnchored) "DEMİRDE (Demir Atıldı)" else "SEYİR HALİNDE / SERBEST"

    val etaDest = uiState.selectedSimpleEtaDestination
    val etaRes = uiState.simpleEtaResult
    val etaStr = if (etaDest != null && etaRes != null) {
      "${etaDest.name} (${String.format(Locale.US, "%.1f", etaRes.distanceNm)} NM) - Tahmini Varış: ${etaRes.etaStr}"
    } else {
      "Hedef Seçilmedi"
    }

    val tideStateStr = if (analysis.currentInstantTideHeightMeters >= 0) "Yükselen/Yüksek Su" else "Alçalan/Alçak Su"

    return buildString {
      appendLine("================================================================================")
      appendLine("                  DENİZCİ SEYİR/TELEMETRİ LOGU")
      appendLine("================================================================================")
      appendLine("Tarih / Saat       : $formattedTime")
      appendLine("Log Kayıt Tipi     : ${if (isAuto) "OTOMATİK (30 Dk Periyodik Kayıt)" else "MANUEL KAYIT (Kullanıcı İsteği)"}")
      appendLine("Rapor Kimliği      : LOG-${System.currentTimeMillis()}")
      appendLine("--------------------------------------------------------------------------------")
      appendLine("1. GEMİ BİLGİLERİ")
      appendLine("Gemi Adı           : $vesselName")
      appendLine("MMSI / IMO         : $mmsi / $imo")
      appendLine("Çağrı İşareti      : $callSign")
      appendLine("Boyutlar (LOA / En): $loa / $beam")
      appendLine("Statik Veri Kaynağı: ${if (uiState.activeAisVesselData != null) "AIS CANLI" else "LOKAL PROFİL"}")
      appendLine()
      appendLine("2. KONUM VE SEYİR TELEMETRİSİ")
      appendLine("Denizci Koordinatı : $marineCoordStr")
      appendLine("Ondalık Koordinat  : $decimalCoordStr")
      appendLine("Konum Kaynağı      : $locSourceStr")
      appendLine("Pruva (HDG)        : $hdgStr°")
      appendLine("Rota (COG)         : $cogStr°")
      appendLine("Hız (SOG)          : $sogStr Knot")
      appendLine("Hedef & ETA        : $etaStr")
      appendLine()
      appendLine("3. SU DERİNLİĞİ & UKC (NET SU ALTI AÇIKLIĞI)")
      appendLine("Harita Derinliği   : ${String.format(Locale.US, "%.2f", analysis.chartedDepthMeters)} m")
      appendLine("Gelgit Yüksekliği  : ${String.format(Locale.US, "%+.2f", analysis.currentInstantTideHeightMeters)} m ($tideStateStr)")
      appendLine("Toplam Su Derinliği: ${String.format(Locale.US, "%.2f", analysis.currentInstantTotalDepthMeters)} m")
      appendLine("Su Çekimi (Draft)  : Baş: ${String.format(Locale.US, "%.2f", analysis.actualDraftMeters)}m | Ort: ${String.format(Locale.US, "%.2f", analysis.actualDraftMeters)}m | Trim: 0.00m")
      appendLine("Çökelme (Squat)    : ${String.format(Locale.US, "%.2f", analysis.calculatedSquatMeters)} m")
      appendLine("Hesaplanan Net UKC : ${String.format(Locale.US, "%.2f", ukcVal)} m (Gerekli Min UKC: ${String.format(Locale.US, "%.2f", uiState.selectedVessel.minUkcMeters)} m)")
      appendLine("Emniyet Durumu     : $safetyStr")
      appendLine()
      appendLine("4. DENİZ VE HAVA DURUMU")
      appendLine("Rüzgar Hızı / Yönü : ${String.format(Locale.US, "%.1f", wind)} Knot / ${String.format(Locale.US, "%d", windDir)}° ($windDirName) - $windBeaufort")
      appendLine("Akıntı Hızı / Yönü : ${String.format(Locale.US, "%.1f", current)} Knot / ${String.format(Locale.US, "%d", currentDir)}°")
      appendLine("Hava Sıcaklığı     : ${String.format(Locale.US, "%.1f", uiState.marineWeather.temperatureC)} °C")
      appendLine("Deniz Basıncı      : ${String.format(Locale.US, "%.0f", uiState.marineWeather.surfacePressureHpa)} hPa")
      appendLine()
      appendLine("5. DEMİRLEME & ZİNCİR STATÜSÜ")
      appendLine("Demir Statüsü      : $anchorStatusStr")
      appendLine("Hesaplanan Kaloma  : ${String.format(Locale.US, "%.1f", uiState.anchorCalculationResult.a_chainScopeShackles)} Kilit (${String.format(Locale.US, "%.0f", uiState.anchorCalculationResult.a_chainScopeMeters)} m)")
      appendLine("Verilen Zincir     : ${uiState.anchorChainShacklesStr} Kilit (${uiState.anchorChainScopeStr} m)")
      appendLine("Demir Derinliği    : ${uiState.anchorDepthStr} m")
      appendLine("Zemin Tipi         : ${uiState.anchorBottomType.displayNameTr}")
      appendLine("================================================================================")
      appendLine("                 TideNav Otomatik Log Sistemi - Son Kayıt")
      appendLine("================================================================================")
    }
  }

  fun saveTxtLog(context: Context, uiState: TideUiState, isAuto: Boolean): TxtLogRecord {
    val dir = getLogDir(context)
    val now = Date()
    val sdfFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val typePrefix = if (isAuto) "AUTO" else "MANUAL"
    val fileName = "marine_log_${sdfFile.format(now)}_$typePrefix.txt"
    val file = File(dir, fileName)

    val content = generateTxtContent(uiState, isAuto)
    file.writeText(content, Charsets.UTF_8)

    val latVal = uiState.latStr.toDoubleOrNull() ?: uiState.selectedPort.latitude
    val lonVal = uiState.lonStr.toDoubleOrNull() ?: uiState.selectedPort.longitude
    val latLonSnippet = String.format(Locale.US, "%.4f, %.4f", latVal, lonVal)
    val ukcSnippet = String.format(Locale.US, "%.1fm", uiState.analysis.currentInstantUkcMeters)
    val sogSnippet = uiState.speedStr.ifBlank { "0.0" }
    val snippet = "Pos: $latLonSnippet | SOG: ${sogSnippet}kts | UKC: $ukcSnippet"

    return TxtLogRecord(
      id = UUID.randomUUID().toString(),
      timestampMillis = now.time,
      timestampFormatted = sdfDisplay.format(now),
      isAutoLog = isAuto,
      fileName = fileName,
      filePath = file.absolutePath,
      fileSizeBytes = file.length(),
      summarySnippet = snippet,
      txtContent = content
    )
  }

  fun loadAllLogs(context: Context): List<TxtLogRecord> {
    val dir = getLogDir(context)
    val files = dir.listFiles { _, name -> name.endsWith(".txt") } ?: return emptyList()

    val sdfDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    return files.map { file ->
      val isAuto = file.name.contains("_AUTO")
      val modifiedTime = file.lastModified()
      val content = try { file.readText(Charsets.UTF_8) } catch (e: Exception) { "" }

      var snippet = "TXT Log Dosyası (${file.length() / 1024} KB)"
      if (content.contains("Denizci Koordinatı")) {
        val line = content.lines().firstOrNull { it.contains("Denizci Koordinatı") }
        if (line != null) {
          snippet = line.replace("Denizci Koordinatı : ", "").trim()
        }
      }

      TxtLogRecord(
        id = file.name,
        timestampMillis = modifiedTime,
        timestampFormatted = sdfDisplay.format(Date(modifiedTime)),
        isAutoLog = isAuto,
        fileName = file.name,
        filePath = file.absolutePath,
        fileSizeBytes = file.length(),
        summarySnippet = snippet,
        txtContent = content
      )
    }.sortedByDescending { it.timestampMillis }
  }

  fun deleteLog(context: Context, filePath: String): Boolean {
    return try {
      val file = File(filePath)
      if (file.exists()) file.delete() else false
    } catch (e: Exception) {
      false
    }
  }

  fun clearAllLogs(context: Context): Boolean {
    return try {
      val dir = getLogDir(context)
      dir.listFiles()?.forEach { it.delete() }
      true
    } catch (e: Exception) {
      false
    }
  }
}
