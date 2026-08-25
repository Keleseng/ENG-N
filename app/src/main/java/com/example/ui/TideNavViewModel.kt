package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnchorCalculationRecord
import com.example.data.AppDatabase
import com.example.data.CalculationRepository
import com.example.data.TideCalculationRecord
import com.example.engine.MarineWeatherProvider
import com.example.engine.RealMoonDataProvider
import com.example.engine.SpeedCalculationEngine
import com.example.engine.TideCalculatorEngine
import com.example.location.GpsFix
import com.example.location.GpsLocationProvider
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TideUiState(
  val selectedVessel: VesselProfile = VesselPresets.defaultVessels[0],
  val selectedPort: PortLocation = LocationPresets.defaultPorts[0],
  val isCustomPort: Boolean = false,
  val vesselName: String = "",
  val mmsiStr: String = "222111447",
  val imoStr: String = "7654320",
  val callSignStr: String = "TST7",
  val vesselTypeStr: String = "Military Ops",
  val loaStr: String = "98.0",
  val beamStr: String = "13.5",
  val draftStr: String = "3.8",
  val blockCoefficientStr: String = "0.55",
  val latStr: String = "40.7180",
  val lonStr: String = "29.8350",
  val chartedDepthStr: String = "18.0",
  val ukcStr: String = "1.0",
  val speedStr: String = "0.0",
  val headingDegreesStr: String = "270", // İstanbul limanı yönü
  val selectedDateOffsetDays: Int = 0, // 0 = Bugün, 1 = Yarın, 2 = +2 gün
  val selectedTabIndex: Int = 0, // Default open in Input Parameters
  val inspectedHour: Double? = null,
  val isGpsLoading: Boolean = false,
  val isGpsActive: Boolean = false,
  val isContinuousTrackingActive: Boolean = false,
  val lastGpsFix: GpsFix? = null,
  val gpsNearestPortInfo: String? = null,
  val gpsSuccessMessage: String? = null,
  val gpsErrorMessage: String? = null,
  val isMmsiTrackingActive: Boolean = false,
  val activeAisVesselData: AisVesselData? = null,
  val isAisLoading: Boolean = false,
  val aisSuccessMessage: String? = null,
  val aisErrorMessage: String? = null,
  val showAisDetailDialog: Boolean = false,
  val activeMarineTrafficUrl: String = com.example.engine.AisTrackingEngine.defaultMarineTrafficUrl,
  val mobEvent: MobEvent = MobEvent(),
  val anchorEvent: AnchorDropEvent = AnchorDropEvent(),
  val anchorShackleStandard: ShackleLengthStandard = ShackleLengthStandard.STANDARD_27_5,
  val anchorChainShacklesStr: String = "5.0",
  val anchorChainScopeStr: String = "137.5",
  val anchorDepthStr: String = "40.0",
  val anchorCustomHorizontalDistStr: String = "",
  val isAnchorAutoHorizontal: Boolean = true,
  val anchorBridgeToHawseStr: String = "35.0",
  val anchorBridgeToSternStr: String = "85.0",
  val anchorLoaStr: String = "120.0",
  val anchorSafetyMarginStr: String = "0.0",
  val anchorBottomType: AnchorBottomType = AnchorBottomType.MUD_SAND,
  val anchorCalculationResult: AnchorCalculationResult = calculateInitialAnchorResult(),
  val marineWeather: MarineWeather = calculateInitialWeather(),
  val speedCalculationResult: SpeedCalculationResult = calculateInitialSpeedResult(),
  val analysis: NavigationAnalysis = calculateInitialAnalysis(),
  val anchorHistory: List<AnchorCalculationRecord> = emptyList(),
  val tideHistory: List<TideCalculationRecord> = emptyList(),
  val saveSuccessMessage: String? = null
)

private fun calculateInitialAnchorResult(): AnchorCalculationResult {
  return com.example.engine.AnchorCalculationEngine.calculate(
    AnchorCalculationParams(
      chainScopeMeters = 137.5,
      depthMeters = 40.0,
      isAutoCalculateHorizontal = true,
      distBridgeToHawseMeters = 35.0,
      distBridgeToSternMeters = 85.0,
      loaMeters = 120.0,
      safetyMarginMeters = 0.0
    )
  )
}

private fun calculateInitialWeather(): MarineWeather {
  val port = LocationPresets.defaultPorts[0]
  return MarineWeatherProvider().generateFallbackMarineWeather(port.latitude, port.longitude)
}

private fun calculateInitialSpeedResult(): SpeedCalculationResult {
  val vessel = VesselPresets.defaultVessels[0]
  return SpeedCalculationEngine.calculateSpeeds(
    gpsFix = null,
    speedThroughWaterKnots = vessel.defaultSpeedKnots,
    vesselHeadingDegrees = 45,
    currentSpeedKnots = 1.2,
    currentDirectionDegrees = 220,
    windSpeedKnots = 12.0,
    windDirectionDegrees = 210
  )
}

private fun calculateInitialAnalysis(): NavigationAnalysis {
  val vessel = VesselPresets.defaultVessels[0]
  val port = LocationPresets.defaultPorts[0]
  val cal = Calendar.getInstance()
  return TideCalculatorEngine.analyzeNavigation(
    vessel = vessel,
    location = port,
    customLat = port.latitude,
    customLon = port.longitude,
    chartedDepth = port.defaultChartedDepthMeters,
    actualDraft = vessel.draftMeters,
    minUkc = vessel.minUkcMeters,
    speedKnots = vessel.defaultSpeedKnots,
    selectedCalendar = cal
  )
}

class TideNavViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(TideUiState())
  val uiState: StateFlow<TideUiState> = _uiState.asStateFlow()

  private val database = AppDatabase.getDatabase(application)
  private val historyRepository = CalculationRepository(database.calculationDao())

  private val weatherProvider = MarineWeatherProvider()
  private var continuousTrackingJob: Job? = null

  init {
    syncRealMoonData()
    refreshWeather()
    observeCalculationHistory()
  }

  private fun observeCalculationHistory() {
    viewModelScope.launch {
      historyRepository.allAnchorRecords.collect { list ->
        _uiState.update { it.copy(anchorHistory = list) }
      }
    }
    viewModelScope.launch {
      historyRepository.allTideRecords.collect { list ->
        _uiState.update { it.copy(tideHistory = list) }
      }
    }
  }

  fun syncRealMoonData() {
    viewModelScope.launch {
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, _uiState.value.selectedDateOffsetDays)
      val moon = RealMoonDataProvider.fetchRealMoonData(cal)
      _uiState.update { current ->
        val updatedAnalysis = current.analysis.copy(
          realMoonInfo = moon,
          moonPhaseName = "${moon.phaseName} (Yaş: ${moon.moonAgeDays} gün)",
          moonIlluminationPercent = moon.illuminationPercent,
          isSpringTide = moon.isSpringTide
        )
        current.copy(analysis = updatedAnalysis)
      }
    }
  }

  fun refreshWeather() {
    viewModelScope.launch {
      val lat = _uiState.value.latStr.toDoubleOrNull() ?: _uiState.value.selectedPort.latitude
      val lon = _uiState.value.lonStr.toDoubleOrNull() ?: _uiState.value.selectedPort.longitude
      val weather = weatherProvider.fetchMarineWeather(lat, lon)
      _uiState.update { current ->
        val updated = current.copy(marineWeather = weather)
        val speedRes = computeSpeedCalculation(updated)
        updated.copy(speedCalculationResult = speedRes)
      }
    }
  }

  fun startContinuousLocationUpdates(gpsProvider: GpsLocationProvider) {
    if (continuousTrackingJob?.isActive == true) return
    continuousTrackingJob = viewModelScope.launch {
      _uiState.update { it.copy(isContinuousTrackingActive = true) }
      try {
        gpsProvider.startLocationUpdates(intervalMs = 2000L).collect { fix ->
          applyGpsFix(fix)
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isContinuousTrackingActive = false, gpsErrorMessage = e.message) }
      }
    }
  }

  fun stopContinuousLocationUpdates() {
    continuousTrackingJob?.cancel()
    continuousTrackingJob = null
    _uiState.update { it.copy(isContinuousTrackingActive = false) }
  }

  fun setTab(index: Int) {
    _uiState.update { it.copy(selectedTabIndex = index) }
  }

  fun setInspectedHour(hour: Double?) {
    _uiState.update { it.copy(inspectedHour = hour) }
  }

  fun selectVesselPreset(preset: VesselProfile) {
    _uiState.update { current ->
      val newLoa = preset.loaMeters
      val newBridgeHawse = String.format(Locale.US, "%.1f", newLoa * 0.70)
      val newBridgeStern = String.format(Locale.US, "%.1f", newLoa * 0.30)
      val updated = current.copy(
        selectedVessel = preset,
        vesselName = preset.name,
        loaStr = newLoa.toString(),
        beamStr = preset.beamMeters.toString(),
        draftStr = preset.draftMeters.toString(),
        ukcStr = preset.minUkcMeters.toString(),
        speedStr = preset.defaultSpeedKnots.toString(),
        anchorLoaStr = newLoa.toString(),
        anchorBridgeToHawseStr = newBridgeHawse,
        anchorBridgeToSternStr = newBridgeStern
      )
      val speedRes = computeSpeedCalculation(updated)
      val anchorRes = computeAnchorCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes,
        anchorCalculationResult = anchorRes
      )
    }
  }

  fun selectPortPreset(port: PortLocation) {
    _uiState.update { current ->
      val newDepthStr = port.defaultChartedDepthMeters.toString()
      val updated = current.copy(
        selectedPort = port,
        isCustomPort = false,
        latStr = port.latitude.toString(),
        lonStr = port.longitude.toString(),
        chartedDepthStr = newDepthStr,
        anchorDepthStr = newDepthStr
      )
      val speedRes = computeSpeedCalculation(updated)
      val anchorRes = computeAnchorCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes,
        anchorCalculationResult = anchorRes
      )
    }
    refreshWeather()
  }

  fun setDateOffset(offsetDays: Int) {
    _uiState.update { current ->
      val updated = current.copy(selectedDateOffsetDays = offsetDays)
      updated.copy(analysis = computeAnalysis(updated))
    }
    syncRealMoonData()
  }

  fun updateLoa(value: String) {
    _uiState.update { current ->
      val updated = current.copy(loaStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateBeam(value: String) {
    _uiState.update { current ->
      val updated = current.copy(beamStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateDraft(value: String) {
    _uiState.update { current ->
      val updated = current.copy(draftStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateLat(value: String) {
    _uiState.update { current ->
      val updated = current.copy(latStr = value, isCustomPort = true)
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
    refreshWeather()
  }

  fun updateLon(value: String) {
    _uiState.update { current ->
      val updated = current.copy(lonStr = value, isCustomPort = true)
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
    refreshWeather()
  }

  fun updateChartedDepth(value: String) {
    _uiState.update { current ->
      val updated = current.copy(chartedDepthStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateUkc(value: String) {
    _uiState.update { current ->
      val updated = current.copy(ukcStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateSpeed(value: String) {
    _uiState.update { current ->
      val updated = current.copy(speedStr = value)
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
  }

  fun updateHeading(value: String) {
    _uiState.update { current ->
      val updated = current.copy(headingDegreesStr = value)
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
  }

  fun updateVesselName(value: String) {
    _uiState.update { current ->
      val updated = current.copy(vesselName = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateMmsi(value: String) {
    _uiState.update { it.copy(mmsiStr = value) }
  }

  fun updateImo(value: String) {
    _uiState.update { it.copy(imoStr = value) }
  }

  fun updateCallSign(value: String) {
    _uiState.update { it.copy(callSignStr = value) }
  }

  fun updateVesselType(value: String) {
    _uiState.update { it.copy(vesselTypeStr = value) }
  }

  fun updateBlockCoefficient(value: String) {
    _uiState.update { current ->
      val updated = current.copy(blockCoefficientStr = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun setShowAisDetailDialog(show: Boolean) {
    _uiState.update { it.copy(showAisDetailDialog = show) }
  }

  fun dismissAisMessages() {
    _uiState.update { it.copy(aisErrorMessage = null, aisSuccessMessage = null) }
  }

  /**
   * Girilen MMSI numarasından veya hedef gemiden AIS konum takibini başlatır.
   */
  fun startMmsiTracking(mmsiOverride: String? = null) {
    val mmsiToTrack = mmsiOverride ?: _uiState.value.mmsiStr
    if (mmsiToTrack.isBlank()) {
      _uiState.update { it.copy(aisErrorMessage = "Lütfen geçerli bir MMSI numarası giriniz (örn: 222111447).") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isAisLoading = true, aisErrorMessage = null, aisSuccessMessage = null) }
      try {
        val aisData = com.example.engine.AisTrackingEngine.fetchAisDataByMmsi(mmsiToTrack)
        applyAisVesselData(aisData)
        _uiState.update {
          it.copy(
            isAisLoading = false,
            isMmsiTrackingActive = true,
            activeAisVesselData = aisData,
            aisSuccessMessage = "🛰️ MMSI (${aisData.mmsi} - ${aisData.name}) AIS Canlı Konum Takibi Başlatıldı!"
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isAisLoading = false,
            aisErrorMessage = "AIS konumu alınamadı: ${e.localizedMessage ?: "Bağlantı hatası"}"
          )
        }
      }
    }
  }

  fun stopMmsiTracking() {
    _uiState.update {
      it.copy(
        isMmsiTrackingActive = false,
        aisSuccessMessage = "MMSI konum takibi durduruldu."
      )
    }
  }

  /**
   * MarineTraffic Atlantik Bölgesi (centerx:-12.0/centery:25.0/zoom:4) AIS Canlı Bilgilerini Getirir.
   */
  fun loadAtlanticMarineTrafficZone() {
    viewModelScope.launch {
      _uiState.update { it.copy(isAisLoading = true, aisErrorMessage = null) }
      val aisData = com.example.engine.AisTrackingEngine.getAtlanticZoneAisData()
      applyAisVesselData(aisData)
      _uiState.update {
        it.copy(
          isAisLoading = false,
          isMmsiTrackingActive = true,
          activeAisVesselData = aisData,
          showAisDetailDialog = true,
          activeMarineTrafficUrl = com.example.engine.AisTrackingEngine.userAtlanticZoneUrl,
          aisSuccessMessage = "🌊 VesselFinder (MMSI: 222111447) Canlı Harita ve Seyir Bilgileri Yüklendi!"
        )
      }
    }
  }

  /**
   * VesselFinder Bölgesi AIS Canlı Bilgilerini Getirir.
   */
  fun loadUserMarineTrafficZone() {
    viewModelScope.launch {
      _uiState.update { it.copy(isAisLoading = true, aisErrorMessage = null) }
      val aisData = com.example.engine.AisTrackingEngine.getTuzlaIzmitAisData()
      applyAisVesselData(aisData)
      _uiState.update {
        it.copy(
          isAisLoading = false,
          isMmsiTrackingActive = true,
          activeAisVesselData = aisData,
          showAisDetailDialog = true,
          activeMarineTrafficUrl = com.example.engine.AisTrackingEngine.userMarineTrafficZoneUrl,
          aisSuccessMessage = "🌊 VesselFinder (MMSI: 222111447) Canlı Harita Bilgileri Yüklendi!"
        )
      }
    }
  }

  /**
   * VesselFinder özel gemi AIS bilgilerini çeker ve uygulamaya yükler.
   */
  fun loadMarineTrafficShip10481795() {
    viewModelScope.launch {
      _uiState.update { it.copy(isAisLoading = true, aisErrorMessage = null) }
      val shipData = com.example.engine.AisTrackingEngine.getMarineTraffic10481795ShipData()
      applyAisVesselData(shipData)
      _uiState.update {
        it.copy(
          isAisLoading = false,
          isMmsiTrackingActive = true,
          activeAisVesselData = shipData,
          showAisDetailDialog = true,
          activeMarineTrafficUrl = shipData.marineTrafficUrl,
          aisSuccessMessage = "⚓ VesselFinder (MMSI: 222111447) Harita ve AIS verileri başarıyla yüklendi!"
        )
      }
    }
  }

  /**
   * Gelen AIS verilerini ve telemetriyi form girdilerine, haritaya ve hesaplamalara uygular.
   */
  fun applyAisVesselData(data: AisVesselData) {
    val latFormatted = String.format(Locale.US, "%.5f", data.latitude)
    val lonFormatted = String.format(Locale.US, "%.5f", data.longitude)
    val sogFormatted = String.format(Locale.US, "%.1f", data.sogKnots)
    val headingFormatted = String.format(Locale.US, "%03d", data.headingDegrees)

    _uiState.update { current ->
      val updated = current.copy(
        vesselName = data.name,
        mmsiStr = data.mmsi,
        imoStr = data.imo,
        callSignStr = data.callSign,
        vesselTypeStr = data.shipType,
        loaStr = data.loaMeters.toString(),
        beamStr = data.beamMeters.toString(),
        draftStr = data.draftMeters.toString(),
        latStr = latFormatted,
        lonStr = lonFormatted,
        speedStr = sogFormatted,
        headingDegreesStr = headingFormatted,
        isCustomPort = true,
        activeMarineTrafficUrl = data.marineTrafficUrl,
        activeAisVesselData = data
      )
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
    refreshWeather()
  }

  /**
   * Tüm GPS verilerini (Mevki, SOG, COG, İrtifa, Hassasiyet) eksiksiz deniz parametrelerine dönüştürür ve uygular.
   */
  fun syncAllGpsToMarineParameters() {
    val currentFix = _uiState.value.lastGpsFix
    if (currentFix != null) {
      applyGpsFix(currentFix, isExplicitSync = true)
    } else {
      _uiState.update {
        it.copy(
          isGpsLoading = true,
          gpsErrorMessage = null,
          gpsSuccessMessage = "🛰️ GPS uyduları aranıyor, deniz parametreleri için anlık konum alınıyor..."
        )
      }
    }
  }

  fun syncGpsTelemetryToInputs() {
    syncAllGpsToMarineParameters()
  }

  fun setGpsLoading(loading: Boolean) {
    _uiState.update { it.copy(isGpsLoading = loading, gpsErrorMessage = if (loading) null else it.gpsErrorMessage) }
  }

  fun setGpsError(error: String?) {
    _uiState.update { it.copy(isGpsLoading = false, gpsErrorMessage = error, gpsSuccessMessage = null) }
  }

  fun dismissGpsMessages() {
    _uiState.update { it.copy(gpsErrorMessage = null, gpsSuccessMessage = null) }
  }

  fun applyGpsFix(fix: GpsFix, isExplicitSync: Boolean = false) {
    val latFormatted = String.format(Locale.US, "%.5f", fix.latitude)
    val lonFormatted = String.format(Locale.US, "%.5f", fix.longitude)

    // En yakın stratejik liman / boğazı bul
    var nearestLocationName: String? = null
    var minDistanceNm = Double.MAX_VALUE
    for (port in com.example.model.LocationPresets.strategicMarineLocations) {
      val dist = com.example.model.calculateHaversineDistanceNm(fix.latitude, fix.longitude, port.latitude, port.longitude)
      if (dist < minDistanceNm) {
        minDistanceNm = dist
        nearestLocationName = "${port.name} (${String.format(Locale.US, "%.1f", dist)} NM)"
      }
    }

    val marinePos = LocationPresets.formatMarineCoordinates(fix.latitude, fix.longitude)
    val sogKnots = fix.speedKnots ?: 0.0
    val cogDeg = fix.bearingDegrees?.toInt() ?: 0
    val altitudeStr = if (fix.altitudeMeters != null) "${String.format(Locale.US, "%.1f", fix.altitudeMeters)} m" else "0.0 m (Deniz Seviyesi)"
    val accuracyStr = "±${String.format(Locale.US, "%.1f", fix.accuracyMeters)} m"

    val successMsg = if (isExplicitSync) {
      "🌊 TÜM GPS BİLGİLERİ DENİZ PARAMETRELERİNE AKTARILDI!\n" +
      "• Mevki (Deniz GPS): $marinePos\n" +
      "• SOG (Hız): ${String.format(Locale.US, "%.1f", sogKnots)} kn | COG (Rota): ${String.format(Locale.US, "%03d", cogDeg)}°\n" +
      "• İrtifa: $altitudeStr | GPS Hassasiyeti: $accuracyStr\n" +
      "• En Yakın Deniz Bölgesi: ${nearestLocationName ?: "Açık Deniz"}"
    } else {
      "🛰️ Canlı GPS: $marinePos • SOG: ${String.format(Locale.US, "%.1f", sogKnots)} kn • COG: ${String.format(Locale.US, "%03d", cogDeg)}° • $accuracyStr"
    }

    _uiState.update { current ->
      val speedStrUpdated = if (fix.speedKnots != null && fix.speedKnots >= 0.1) {
        String.format(Locale.US, "%.1f", fix.speedKnots)
      } else {
        current.speedStr
      }

      val headingStrUpdated = if (fix.bearingDegrees != null) {
        String.format(Locale.US, "%03d", fix.bearingDegrees.toInt())
      } else {
        current.headingDegreesStr
      }

      val updated = current.copy(
        isGpsLoading = false,
        isGpsActive = true,
        lastGpsFix = fix,
        latStr = latFormatted,
        lonStr = lonFormatted,
        speedStr = speedStrUpdated,
        headingDegreesStr = headingStrUpdated,
        isCustomPort = true,
        gpsNearestPortInfo = nearestLocationName,
        gpsSuccessMessage = successMsg,
        gpsErrorMessage = null
      )
      val speedRes = computeSpeedCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes
      )
    }
    // Refresh weather asynchronously on new fix
    refreshWeather()
  }

  fun refreshGps() {
    _uiState.update { it.copy(isGpsLoading = true) }
  }

  fun triggerMob() {
    val lat = _uiState.value.latStr.toDoubleOrNull() ?: _uiState.value.selectedPort.latitude
    val lon = _uiState.value.lonStr.toDoubleOrNull() ?: _uiState.value.selectedPort.longitude
    val heading = _uiState.value.headingDegreesStr.toIntOrNull() ?: 0
    val speed = _uiState.value.speedStr.toDoubleOrNull() ?: 0.0
    val timeNow = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    val mob = MobEvent(
      isActive = true,
      timestampEpochMs = System.currentTimeMillis(),
      timeFormatted = timeNow,
      latitude = lat,
      longitude = lon,
      vesselHeadingAtDrop = heading,
      vesselSpeedAtDropKnots = speed
    )
    _uiState.update {
      it.copy(
        mobEvent = mob,
        gpsSuccessMessage = "🚨 DENİZE ADAM DÜŞTÜ (MOB) KAYDEDİLDİ! Mevki: ${LocationPresets.formatMarineCoordinates(lat, lon)} ($timeNow)"
      )
    }
  }

  fun cancelMob() {
    _uiState.update {
      it.copy(
        mobEvent = MobEvent(isActive = false),
        gpsSuccessMessage = "MOB alarmı kapatıldı / kurtarma sonlandırıldı."
      )
    }
  }

  fun dropAnchor(customLat: Double? = null, customLon: Double? = null) {
    val lat = customLat ?: (_uiState.value.latStr.toDoubleOrNull() ?: _uiState.value.selectedPort.latitude)
    val lon = customLon ?: (_uiState.value.lonStr.toDoubleOrNull() ?: _uiState.value.selectedPort.longitude)
    val depth = _uiState.value.chartedDepthStr.toDoubleOrNull() ?: 10.0
    val timeNow = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    val anchorSafeGomina = _uiState.value.anchorCalculationResult.f_secondSwingingCircleGomina.coerceIn(0.5, 9.5)
    val anchor = AnchorDropEvent(
      isAnchored = true,
      dropTimestampEpochMs = System.currentTimeMillis(),
      dropTimeFormatted = timeNow,
      latitude = lat,
      longitude = lon,
      chartedDepthAtDropMeters = depth,
      totalGominaRings = 10,
      safeSwingingRadiusGomina = anchorSafeGomina
    )
    _uiState.update {
      it.copy(
        anchorEvent = anchor,
        gpsSuccessMessage = "⚓ DEMİR ATILDI! Mevki: ${LocationPresets.formatMarineCoordinates(lat, lon)}. 2. Salma Dairesi (${String.format(Locale.US, "%.2f Gomina / %.1f m", anchorSafeGomina, _uiState.value.anchorCalculationResult.f_secondSwingingCircleMeters)}) emniyet sınırı olarak ayarlandı."
      )
    }
  }

  fun liftAnchor() {
    _uiState.update {
      it.copy(
        anchorEvent = AnchorDropEvent(isAnchored = false),
        gpsSuccessMessage = "⚓ DEMİR ALINDI! Salma dairesi takibi durduruldu."
      )
    }
  }

  fun updateAnchorSafeRadius(gomina: Double) {
    _uiState.update { current ->
      if (current.anchorEvent.isAnchored) {
        current.copy(anchorEvent = current.anchorEvent.copy(safeSwingingRadiusGomina = gomina.coerceIn(0.5, 9.5)))
      } else {
        current
      }
    }
  }

  fun updateAnchorCalculation(
    chainScope: String? = null,
    chainShackles: String? = null,
    shackleStandard: ShackleLengthStandard? = null,
    depth: String? = null,
    customHorizontal: String? = null,
    isAutoHorizontal: Boolean? = null,
    bridgeToHawse: String? = null,
    bridgeToStern: String? = null,
    loa: String? = null,
    safetyMargin: String? = null,
    bottomType: AnchorBottomType? = null
  ) {
    _uiState.update { current ->
      val activeStandard = shackleStandard ?: current.anchorShackleStandard
      val metersPerShackle = activeStandard.metersPerShackle

      // Senkronizasyon: Kilit girildiyse metreyi güncelle, metre girildiyse kilidi güncelle
      val (newMetersStr, newShacklesStr) = when {
        chainShackles != null -> {
          val sh = chainShackles.toDoubleOrNull()
          val mStr = if (sh != null) String.format(Locale.US, "%.1f", sh * metersPerShackle) else current.anchorChainScopeStr
          Pair(mStr, chainShackles)
        }
        chainScope != null -> {
          val m = chainScope.toDoubleOrNull()
          val shStr = if (m != null) String.format(Locale.US, "%.2f", m / metersPerShackle) else current.anchorChainShacklesStr
          Pair(chainScope, shStr)
        }
        shackleStandard != null -> {
          // Standart değiştiğinde mevcut kilit sayısına göre metreyi yeniden hesapla
          val sh = current.anchorChainShacklesStr.toDoubleOrNull() ?: 5.0
          val mStr = String.format(Locale.US, "%.1f", sh * metersPerShackle)
          Pair(mStr, current.anchorChainShacklesStr)
        }
        else -> Pair(current.anchorChainScopeStr, current.anchorChainShacklesStr)
      }

      val updated = current.copy(
        anchorShackleStandard = activeStandard,
        anchorChainShacklesStr = newShacklesStr,
        anchorChainScopeStr = newMetersStr,
        anchorDepthStr = depth ?: current.anchorDepthStr,
        anchorCustomHorizontalDistStr = customHorizontal ?: current.anchorCustomHorizontalDistStr,
        isAnchorAutoHorizontal = isAutoHorizontal ?: current.isAnchorAutoHorizontal,
        anchorBridgeToHawseStr = bridgeToHawse ?: current.anchorBridgeToHawseStr,
        anchorBridgeToSternStr = bridgeToStern ?: current.anchorBridgeToSternStr,
        anchorLoaStr = loa ?: current.anchorLoaStr,
        anchorSafetyMarginStr = safetyMargin ?: current.anchorSafetyMarginStr,
        anchorBottomType = bottomType ?: current.anchorBottomType
      )
      val anchorRes = computeAnchorCalculation(updated)
      updated.copy(anchorCalculationResult = anchorRes)
    }
  }

  fun setAnchorShackleStandard(standard: ShackleLengthStandard) {
    updateAnchorCalculation(shackleStandard = standard)
  }

  fun setAnchorChainShackles(shacklesStr: String) {
    updateAnchorCalculation(chainShackles = shacklesStr)
  }

  fun setAnchorChainScopeByShackles(shackles: Double) {
    val formattedShackles = String.format(Locale.US, "%.1f", shackles)
    updateAnchorCalculation(chainShackles = formattedShackles)
  }

  fun loadTextbookExample1() {
    // Fotoğraftaki Kitap Örneği: 40m derinlik, 5 kilit zincir (1 kilit = 27.5m -> 137.5m), K/Ü-Loça 35m, K/Ü-Kıç 85m (LOA = 120m)
    updateAnchorCalculation(
      shackleStandard = ShackleLengthStandard.STANDARD_27_5,
      chainShackles = "5.0",
      chainScope = "137.5",
      depth = "40.0",
      isAutoHorizontal = true,
      customHorizontal = "",
      bridgeToHawse = "35.0",
      bridgeToStern = "85.0",
      loa = "120.0",
      safetyMargin = "0.0"
    )
  }

  fun loadTextbookExample2() {
    // Fotoğraftaki El Yazısı Örnek: 50m derinlik, 3 kilit zincir (1 kilit = 27.5m -> 82.5m), K/Ü-Loça 40m, K/Ü-Kıç 115m (LOA = 155m)
    updateAnchorCalculation(
      shackleStandard = ShackleLengthStandard.STANDARD_27_5,
      chainShackles = "3.0",
      chainScope = "82.5",
      depth = "50.0",
      isAutoHorizontal = true,
      customHorizontal = "",
      bridgeToHawse = "40.0",
      bridgeToStern = "115.0",
      loa = "155.0",
      safetyMargin = "0.0"
    )
  }

  fun applyAnchorCalculationToSwingingCircle() {
    val result = _uiState.value.anchorCalculationResult
    val gominaRadius = result.f_secondSwingingCircleGomina
    _uiState.update { current ->
      val updatedAnchor = current.anchorEvent.copy(
        safeSwingingRadiusGomina = gominaRadius.coerceIn(0.2, 9.5)
      )
      current.copy(
        anchorEvent = updatedAnchor,
        gpsSuccessMessage = "⚓ 2. Salma Dairesi (${String.format(Locale.US, "%.1f m / %.2f Gomina", result.f_secondSwingingCircleMeters, gominaRadius)}) canlı harita emniyet dairesine uygulandı!"
      )
    }
  }

  // ══════════════════════════════════════════════════════════════
  // ROOM DATABASE GEÇMİŞ İŞLEMLERİ (ROOM PERSISTENCE)
  // ══════════════════════════════════════════════════════════════

  fun saveCurrentAnchorCalculation(note: String = "") {
    viewModelScope.launch {
      val state = _uiState.value
      val res = state.anchorCalculationResult
      val record = AnchorCalculationRecord(
        vesselName = state.vesselName,
        shackleStandardLabel = state.anchorShackleStandard.labelTr,
        metersPerShackle = state.anchorShackleStandard.metersPerShackle,
        shacklesCount = res.a_chainScopeShackles,
        chainScopeMeters = res.a_chainScopeMeters,
        depthMeters = res.b_depthMeters,
        horizontalDistanceMeters = res.c_horizontalDistanceMeters,
        distBridgeToHawseMeters = res.distBridgeToHawseMeters,
        distBridgeToSternMeters = res.distBridgeToSternMeters,
        vesselLoaMeters = res.loaMeters,
        firstSwingingRadiusMeters = res.d_firstSwingingCircleMeters,
        secondSwingingRadiusBridgeMeters = res.e_bridgeSternSwingingCircleMeters,
        secondSwingingRadiusTotalMeters = res.f_secondSwingingCircleMeters,
        safetyStatus = res.scopeStatus.labelTr,
        note = note
      )
      historyRepository.saveAnchorCalculation(record)
      _uiState.update {
        it.copy(saveSuccessMessage = "⚓ Demirleme hesabı (${String.format(Locale.US, "%.1f", res.a_chainScopeShackles)} Kilit / ${String.format(Locale.US, "%.1f", res.a_chainScopeMeters)}m) Room veritabanına kaydedildi.")
      }
    }
  }

  fun deleteAnchorRecord(id: Long) {
    viewModelScope.launch {
      historyRepository.deleteAnchorRecord(id)
      _uiState.update { it.copy(saveSuccessMessage = "Demirleme hesabı silindi.") }
    }
  }

  fun clearAnchorHistory() {
    viewModelScope.launch {
      historyRepository.clearAnchorHistory()
      _uiState.update { it.copy(saveSuccessMessage = "Tüm demirleme geçmişi temizlendi.") }
    }
  }

  fun loadAnchorRecordIntoForm(record: AnchorCalculationRecord) {
    val standard = if (record.metersPerShackle <= 26.0) ShackleLengthStandard.METRIC_25_0 else ShackleLengthStandard.STANDARD_27_5
    updateAnchorCalculation(
      shackleStandard = standard,
      chainShackles = String.format(Locale.US, "%.1f", record.shacklesCount),
      chainScope = String.format(Locale.US, "%.1f", record.chainScopeMeters),
      depth = String.format(Locale.US, "%.1f", record.depthMeters),
      isAutoHorizontal = true,
      customHorizontal = "",
      bridgeToHawse = String.format(Locale.US, "%.1f", record.distBridgeToHawseMeters),
      bridgeToStern = String.format(Locale.US, "%.1f", record.distBridgeToSternMeters),
      loa = String.format(Locale.US, "%.1f", record.vesselLoaMeters)
    )
    _uiState.update {
      it.copy(
        vesselName = record.vesselName,
        saveSuccessMessage = "${record.vesselName.ifBlank { "Kayıtlı" }} demirleme parametreleri forma yüklendi."
      )
    }
  }

  fun saveCurrentTideCalculation(note: String = "") {
    viewModelScope.launch {
      val state = _uiState.value
      val analysis = state.analysis
      val portName = if (state.isCustomPort) "Özel Mevki (${state.latStr}°, ${state.lonStr}°)" else state.selectedPort.name
      val hwExtremum = analysis.extrema.find { it.type == ExtremumType.HIGH_WATER }
      val lwExtremum = analysis.extrema.find { it.type == ExtremumType.LOW_WATER }
      val record = TideCalculationRecord(
        portName = portName,
        highTideHeightMeters = hwExtremum?.tideHeightMeters ?: analysis.maxTideHeight24h,
        lowTideHeightMeters = lwExtremum?.tideHeightMeters ?: analysis.minTideHeight24h,
        highTideTime = hwExtremum?.timeFormatted ?: "--:--",
        lowTideTime = lwExtremum?.timeFormatted ?: "--:--",
        chartDatumDepthMeters = analysis.chartedDepthMeters,
        shipDraftMeters = analysis.actualDraftMeters,
        requiredUkcMeters = analysis.minUkcMeters,
        currentInstantDepthMeters = analysis.currentInstantTotalDepthMeters,
        currentInstantUkcMeters = analysis.currentInstantUkcMeters,
        isCurrentlySafe = analysis.isCurrentlySafe,
        safeWindowSummary = analysis.advisorySummary,
        note = note
      )
      historyRepository.saveTideCalculation(record)
      _uiState.update {
        it.copy(saveSuccessMessage = "🌊 $portName gelgit hesabı Room veritabanına kaydedildi.")
      }
    }
  }

  fun deleteTideRecord(id: Long) {
    viewModelScope.launch {
      historyRepository.deleteTideRecord(id)
      _uiState.update { it.copy(saveSuccessMessage = "Gelgit hesabı kaydı silindi.") }
    }
  }

  fun clearTideHistory() {
    viewModelScope.launch {
      historyRepository.clearTideHistory()
      _uiState.update { it.copy(saveSuccessMessage = "Tüm gelgit geçmişi temizlendi.") }
    }
  }

  fun loadTideRecordIntoForm(record: TideCalculationRecord) {
    updateDraft(String.format(Locale.US, "%.1f", record.shipDraftMeters))
    updateUkc(String.format(Locale.US, "%.1f", record.requiredUkcMeters))
    updateChartedDepth(String.format(Locale.US, "%.1f", record.chartDatumDepthMeters))
    _uiState.update {
      it.copy(
        saveSuccessMessage = "${record.portName} gelgit parametreleri yüklendi."
      )
    }
  }

  fun clearSaveSuccessMessage() {
    _uiState.update { it.copy(saveSuccessMessage = null) }
  }

  private fun computeAnchorCalculation(state: TideUiState): AnchorCalculationResult {
    val a = state.anchorChainScopeStr.toDoubleOrNull() ?: 137.5
    val b = state.anchorDepthStr.toDoubleOrNull() ?: (state.chartedDepthStr.toDoubleOrNull() ?: 40.0)
    val cCustom = state.anchorCustomHorizontalDistStr.toDoubleOrNull()
    val distBow = state.anchorBridgeToHawseStr.toDoubleOrNull() ?: 35.0
    val distStern = state.anchorBridgeToSternStr.toDoubleOrNull() ?: 85.0
    val loaVal = state.anchorLoaStr.toDoubleOrNull() ?: (state.loaStr.toDoubleOrNull() ?: 120.0)
    val safetyVal = state.anchorSafetyMarginStr.toDoubleOrNull() ?: 0.0

    val params = AnchorCalculationParams(
      chainScopeMeters = a,
      depthMeters = b,
      shackleStandard = state.anchorShackleStandard,
      isAutoCalculateHorizontal = state.isAnchorAutoHorizontal,
      customHorizontalDistanceMeters = cCustom,
      distBridgeToHawseMeters = distBow,
      distBridgeToSternMeters = distStern,
      loaMeters = loaVal,
      safetyMarginMeters = safetyVal,
      bottomType = state.anchorBottomType
    )
    return com.example.engine.AnchorCalculationEngine.calculate(params)
  }

  private fun computeSpeedCalculation(state: TideUiState): SpeedCalculationResult {
    val speed = state.speedStr.toDoubleOrNull() ?: state.selectedVessel.defaultSpeedKnots
    val heading = state.headingDegreesStr.toIntOrNull() ?: 45
    val currentSpeed = state.analysis.currentInfo.speedKnots
    val currentDirection = state.analysis.currentInfo.directionDegrees
    val windSpeed = state.marineWeather.windSpeedKnots
    val windDir = state.marineWeather.windDirectionDegrees

    return SpeedCalculationEngine.calculateSpeeds(
      gpsFix = state.lastGpsFix,
      speedThroughWaterKnots = speed,
      vesselHeadingDegrees = heading,
      currentSpeedKnots = currentSpeed,
      currentDirectionDegrees = currentDirection,
      windSpeedKnots = windSpeed,
      windDirectionDegrees = windDir
    )
  }

  private fun computeAnalysis(state: TideUiState): NavigationAnalysis {
    val loa = state.loaStr.toDoubleOrNull() ?: state.selectedVessel.loaMeters
    val beam = state.beamStr.toDoubleOrNull() ?: state.selectedVessel.beamMeters
    val draft = state.draftStr.toDoubleOrNull() ?: state.selectedVessel.draftMeters
    val lat = state.latStr.toDoubleOrNull() ?: state.selectedPort.latitude
    val lon = state.lonStr.toDoubleOrNull() ?: state.selectedPort.longitude
    val chartedDepth = state.chartedDepthStr.toDoubleOrNull() ?: state.selectedPort.defaultChartedDepthMeters
    val ukc = state.ukcStr.toDoubleOrNull() ?: state.selectedVessel.minUkcMeters
    val speed = state.speedStr.toDoubleOrNull() ?: state.selectedVessel.defaultSpeedKnots
    val heading = state.headingDegreesStr.toIntOrNull() ?: 45

    val blockCoeff = state.blockCoefficientStr.toDoubleOrNull() ?: state.selectedVessel.blockCoefficient

    val customVessel = state.selectedVessel.copy(
      name = state.vesselName,
      typeName = state.vesselTypeStr,
      loaMeters = loa,
      beamMeters = beam,
      draftMeters = draft,
      blockCoefficient = blockCoeff,
      minUkcMeters = ukc,
      defaultSpeedKnots = speed
    )

    val customLocation = if (state.isCustomPort) {
      PortLocation(
        id = "custom",
        name = "Özel Koordinat Konumu",
        country = "Gemi Girişi",
        latitude = lat,
        longitude = lon,
        defaultChartedDepthMeters = chartedDepth,
        typicalTideRangeMeters = 3.0,
        description = "Kullanıcı tanımlı enlem/boylam koordinatları"
      )
    } else {
      state.selectedPort.copy(
        latitude = lat,
        longitude = lon,
        defaultChartedDepthMeters = chartedDepth
      )
    }

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, state.selectedDateOffsetDays)

    return TideCalculatorEngine.analyzeNavigation(
      vessel = customVessel,
      location = customLocation,
      customLat = lat,
      customLon = lon,
      chartedDepth = chartedDepth,
      actualDraft = draft,
      minUkc = ukc,
      speedKnots = speed,
      headingDegrees = heading,
      selectedCalendar = cal
    )
  }
}
