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
import com.example.sensor.MarineAttitudeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class LocationSource {
  NONE,
  GPS,
  AIS
}

data class TideUiState(
  val selectedVessel: VesselProfile = VesselProfile(),
  val selectedPort: PortLocation = LocationPresets.defaultPorts[0],
  val isCustomPort: Boolean = false,
  val vesselName: String = "",
  val mmsiStr: String = "",
  val imoStr: String = "",
  val callSignStr: String = "",
  val vesselTypeStr: String = "",
  val loaStr: String = "",
  val beamStr: String = "",
  val draftStr: String = "",
  val blockCoefficientStr: String = "0.70",
  val latStr: String = "40.82833",
  val lonStr: String = "29.25399",
  val activeLocationSource: LocationSource = LocationSource.NONE,
  val chartedDepthStr: String = "31.1",
  val ukcStr: String = "1.0",
  val speedStr: String = "0.0",
  val headingDegreesStr: String = "270", // İstanbul limanı yönü
  val marineAttitude: MarineAttitude = MarineAttitude(),
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
  val isAnchorChainAuto: Boolean = true,
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
  val anchorWeatherScenario: AnchorWeatherScenario = AnchorWeatherScenario.LIVE,
  val anchorCalculationResult: AnchorCalculationResult = calculateInitialAnchorResult(),
  val marineWeather: MarineWeather = calculateInitialWeather(),
  val speedCalculationResult: SpeedCalculationResult = calculateInitialSpeedResult(),
  val analysis: NavigationAnalysis = calculateInitialAnalysis(),
  val anchorHistory: List<AnchorCalculationRecord> = emptyList(),
  val tideHistory: List<TideCalculationRecord> = emptyList(),
  val saveSuccessMessage: String? = null,
  val isDarkMode: Boolean = true,
  val activeMapLayer: MarineMapLayer = MarineMapLayer.VESSEL_FINDER,
  val trackHistory: List<VesselTrackPoint> = emptyList(),
  val isAutoFollowShip: Boolean = true,
  val isShowRangeRings: Boolean = true,
  val isShowSpeedVector: Boolean = true,
  val speedVectorMinutes: Int = 12,
  val isMeasureRulerActive: Boolean = false,
  val isNightChartMode: Boolean = true,
  val verifiedMarineDepth: com.example.engine.MarineDepthResult? = null,
  val isDepthLoading: Boolean = false,
  val selectedSimpleEtaDestination: com.example.model.SimpleDestination? = null,
  val simpleEtaResult: com.example.model.SimpleEtaResult? = null,
  val etaSummaryReceipt: com.example.model.EtaSummaryReceipt? = null,
  val mapFocusCoordinate: com.example.model.Coordinate? = null,
  val txtLogHistory: List<com.example.engine.TxtLogRecord> = emptyList(),
  val nextAutoLogTimeStr: String = "",
  val selectedTxtLogForView: com.example.engine.TxtLogRecord? = null,
  val txtLogMessage: String? = null,
  val isAisRadarOpen: Boolean = false,
  val surroundingAisTargets: List<com.example.model.RadarAisTarget> = emptyList(),
  val selectedRadarTarget: com.example.model.RadarAisTarget? = null,
  val radarRangeNm: Double = 6.0,
  val isRadarHeadUp: Boolean = true
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
  return SpeedCalculationEngine.calculateSpeeds(
    gpsFix = null,
    speedThroughWaterKnots = 0.0,
    vesselHeadingDegrees = 45,
    currentSpeedKnots = 1.2,
    currentDirectionDegrees = 220,
    windSpeedKnots = 12.0,
    windDirectionDegrees = 210
  )
}

private fun calculateInitialAnalysis(): NavigationAnalysis {
  val vessel = VesselProfile()
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
  private val attitudeProvider = MarineAttitudeProvider(application.applicationContext)
  private var continuousTrackingJob: Job? = null
  private var autoTxtLogJob: Job? = null

  init {
    syncRealMoonData()
    fetchDepthFromMmsiVessel()
    observeCalculationHistory()
    startAttitudeMonitoring()
    startAuto30MinTxtLogging()
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


  /**
   * MMSI bilgileri girilen geminin (uiState.mmsiStr) AIS mevkisinden koordinatları alır,
   * koordinatlar penceresini günceller ve derinlik penceresi bilgilerini (EMODnet Bathymetry)
   * doğrudan bu geminin bulunduğu konumdan çeker.
   */
  fun fetchDepthFromMmsiVessel(mmsiOverride: String? = null) {
    val mmsiToTrack = (mmsiOverride ?: _uiState.value.mmsiStr).trim()
    viewModelScope.launch {
      _uiState.update { it.copy(isDepthLoading = true) }
      try {
        val aisData = com.example.engine.AisTrackingEngine.fetchAisDataByMmsi(mmsiToTrack)
        applyAisVesselData(aisData)
      } catch (e: Exception) {
        _uiState.update { it.copy(isDepthLoading = false) }
      }
    }
  }

  fun fetchVerifiedDepth(lat: Double? = null, lon: Double? = null) {
    viewModelScope.launch {
      val targetLat = lat
        ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.latStr)
        ?: _uiState.value.activeAisVesselData?.latitude
        ?: _uiState.value.selectedPort.latitude
      val targetLon = lon
        ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.lonStr)
        ?: _uiState.value.activeAisVesselData?.longitude
        ?: _uiState.value.selectedPort.longitude

      _uiState.update { it.copy(isDepthLoading = true) }
      try {
        val result = com.example.engine.MarineDepthProvider.fetchMarineDepth(targetLat, targetLon)
        val depthFormatted = String.format(Locale.US, "%.1f", result.depthMeters)
        _uiState.update { current ->
          val updated = current.copy(
            verifiedMarineDepth = result,
            isDepthLoading = false,
            // Seyir haritası derinliği ve demirleme su derinliğini AIS/GPS koordinatının derinliğine senkronize et
            chartedDepthStr = depthFormatted,
            anchorDepthStr = depthFormatted
          )
          val speedRes = computeSpeedCalculation(updated)
          val anchorRes = computeAnchorCalculation(updated)
          updated.copy(
            analysis = computeAnalysis(updated),
            speedCalculationResult = speedRes,
            anchorCalculationResult = anchorRes
          )
        }
      } catch (e: Exception) {
        val fallback = com.example.engine.MarineDepthProvider.calculateRegionalDepthFallback(targetLat, targetLon)
        val fallbackDepthFormatted = String.format(Locale.US, "%.1f", fallback.depthMeters)
        _uiState.update { current ->
          val updated = current.copy(
            verifiedMarineDepth = fallback,
            isDepthLoading = false,
            chartedDepthStr = fallbackDepthFormatted,
            anchorDepthStr = fallbackDepthFormatted
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
    }
  }

  fun refreshWeather() {
    viewModelScope.launch {
      val lat = com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.latStr) ?: _uiState.value.selectedPort.latitude
      val lon = com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.lonStr) ?: _uiState.value.selectedPort.longitude
      val weather = weatherProvider.fetchMarineWeather(lat, lon)
      fetchVerifiedDepth(lat, lon)
      _uiState.update { current ->
        val updated = current.copy(marineWeather = weather)
        val speedRes = computeSpeedCalculation(updated)
        val anchorRes = computeAnchorCalculation(updated)
        updated.copy(speedCalculationResult = speedRes, anchorCalculationResult = anchorRes)
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
        mmsiStr = preset.mmsi,
        vesselTypeStr = preset.typeName,
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

  private fun startAttitudeMonitoring() {
    viewModelScope.launch {
      attitudeProvider.startAttitudeUpdates(
        getHeadingDegrees = {
          _uiState.value.lastGpsFix?.bearingDegrees ?: (_uiState.value.headingDegreesStr.toFloatOrNull() ?: 270f)
        }
      ).collect { att ->
        _uiState.update { it.copy(marineAttitude = att) }
      }
    }
  }

  fun updateMarineAttitude(attitude: MarineAttitude) {
    _uiState.update { it.copy(marineAttitude = attitude) }
  }

  fun tareAttitude() {
    attitudeProvider.tare()
  }

  fun resetAttitudeTare() {
    attitudeProvider.resetTare()
  }

  fun toggleAttitudeHold() {
    attitudeProvider.toggleHold()
  }

  fun updateVesselName(value: String) {
    _uiState.update { current ->
      val updated = current.copy(vesselName = value)
      updated.copy(analysis = computeAnalysis(updated))
    }
  }

  fun updateMmsi(value: String) {
    _uiState.update { it.copy(mmsiStr = value) }
    val clean = value.trim().filter { it.isDigit() }
    if (clean.length == 9) {
      fetchDepthFromMmsiVessel(clean)
    }
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

  fun setSimpleEtaDestination(dest: com.example.model.SimpleDestination?) {
    _uiState.update { current ->
      val updated = current.copy(selectedSimpleEtaDestination = dest)
      val simpleEta = computeSimpleEta(updated)
      val receipt = if (dest != null) {
        val lat = current.lastGpsFix?.latitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(current.latStr) ?: 40.8360
        val lon = current.lastGpsFix?.longitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(current.lonStr) ?: 29.3010
        val speed = current.lastGpsFix?.speedKnots?.takeIf { it > 0.5 }
          ?: current.activeAisVesselData?.sogKnots?.takeIf { it > 0.5 }
          ?: current.speedStr.toDoubleOrNull()?.takeIf { it > 0.5 }
          ?: 12.0
        com.example.model.computeEtaSummary(lat, lon, speed, com.example.model.Coordinate(dest.lat, dest.lon), dest.name)
      } else null
      updated.copy(
        simpleEtaResult = simpleEta,
        etaSummaryReceipt = receipt
      )
    }
  }

  fun calculateAndSetEta(coord: com.example.model.Coordinate, targetName: String? = null) {
    _uiState.update { current ->
      val lat = current.lastGpsFix?.latitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(current.latStr) ?: 40.8360
      val lon = current.lastGpsFix?.longitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(current.lonStr) ?: 29.3010
      val speed = current.lastGpsFix?.speedKnots?.takeIf { it > 0.5 }
        ?: current.activeAisVesselData?.sogKnots?.takeIf { it > 0.5 }
        ?: current.speedStr.toDoubleOrNull()?.takeIf { it > 0.5 }
        ?: 12.0
      val receipt = com.example.model.computeEtaSummary(lat, lon, speed, coord, targetName)
      val dest = com.example.model.SimpleDestination(targetName ?: "Varış Mevkii", coord.latitude, coord.longitude)
      val simpleEta = com.example.model.calculateSimpleEta(lat, lon, speed, dest)
      current.copy(
        selectedSimpleEtaDestination = dest,
        simpleEtaResult = simpleEta,
        etaSummaryReceipt = receipt
      )
    }
  }

  fun setMapFocusCoordinate(coord: com.example.model.Coordinate?) {
    _uiState.update { it.copy(mapFocusCoordinate = coord) }
  }

  private fun computeSimpleEta(state: TideUiState): com.example.model.SimpleEtaResult? {
    val dest = state.selectedSimpleEtaDestination ?: return null
    val lat = state.lastGpsFix?.latitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(state.latStr) ?: 40.8360
    val lon = state.lastGpsFix?.longitude ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(state.lonStr) ?: 29.3010
    val speed = state.lastGpsFix?.speedKnots ?: state.activeAisVesselData?.sogKnots ?: state.speedStr.toDoubleOrNull() ?: 0.0
    return com.example.model.calculateSimpleEta(lat, lon, speed, dest)
  }

  fun setShowAisDetailDialog(show: Boolean) {
    _uiState.update { it.copy(showAisDetailDialog = show) }
  }

  fun dismissAisMessages() {
    _uiState.update { it.copy(aisErrorMessage = null, aisSuccessMessage = null) }
  }

  fun getEffectiveShipCoordinates(): Pair<Double, Double> {
    val state = _uiState.value
    val lat = state.lastGpsFix?.latitude
      ?: state.activeAisVesselData?.latitude
      ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(state.latStr)
      ?: state.selectedPort.latitude
    val lon = state.lastGpsFix?.longitude
      ?: state.activeAisVesselData?.longitude
      ?: com.example.model.LocationPresets.parseCoordinateOrDecimal(state.lonStr)
      ?: state.selectedPort.longitude
    return Pair(lat, lon)
  }

  fun openAisRadar() {
    val state = _uiState.value
    val (lat, lon) = getEffectiveShipCoordinates()
    val ownSpeed = state.lastGpsFix?.speedKnots?.takeIf { it > 0 } ?: state.activeAisVesselData?.sogKnots ?: 0.0
    val ownCog = state.lastGpsFix?.bearingDegrees?.toDouble()?.takeIf { it > 0 } ?: state.activeAisVesselData?.cogDegrees ?: 270.0
    val targets = com.example.engine.SurroundingAisRadarEngine.generateSurroundingVessels(lat, lon, ownSpeed, ownCog)
    _uiState.update {
      it.copy(
        isAisRadarOpen = true,
        surroundingAisTargets = targets,
        selectedRadarTarget = targets.firstOrNull { t -> t.isHazardous } ?: targets.firstOrNull()
      )
    }
  }

  fun closeAisRadar() {
    _uiState.update { it.copy(isAisRadarOpen = false, selectedRadarTarget = null) }
  }

  fun refreshSurroundingAisTargets() {
    val state = _uiState.value
    val (lat, lon) = getEffectiveShipCoordinates()
    val ownSpeed = state.lastGpsFix?.speedKnots?.takeIf { it > 0 } ?: state.activeAisVesselData?.sogKnots ?: 0.0
    val ownCog = state.lastGpsFix?.bearingDegrees?.toDouble()?.takeIf { it > 0 } ?: state.activeAisVesselData?.cogDegrees ?: 270.0
    val targets = com.example.engine.SurroundingAisRadarEngine.generateSurroundingVessels(lat, lon, ownSpeed, ownCog)
    _uiState.update {
      it.copy(
        surroundingAisTargets = targets,
        selectedRadarTarget = targets.find { t -> t.id == it.selectedRadarTarget?.id } ?: targets.firstOrNull()
      )
    }
  }

  fun updateSurroundingAisTargetsDirect(targets: List<com.example.model.RadarAisTarget>) {
    _uiState.update {
      it.copy(
        surroundingAisTargets = targets,
        selectedRadarTarget = targets.find { t -> t.id == it.selectedRadarTarget?.id } ?: it.selectedRadarTarget
      )
    }
  }

  fun selectRadarTarget(target: com.example.model.RadarAisTarget?) {
    _uiState.update { it.copy(selectedRadarTarget = target) }
  }

  fun setRadarRange(rangeNm: Double) {
    _uiState.update { it.copy(radarRangeNm = rangeNm) }
  }

  fun toggleRadarOrientation() {
    _uiState.update { it.copy(isRadarHeadUp = !it.isRadarHeadUp) }
  }

  /**
   * Girilen MMSI numarasından veya hedef gemiden AIS konum takibini başlatır.
   */
  fun startMmsiTracking(mmsiOverride: String? = null) {
    val mmsiToTrack = (mmsiOverride ?: _uiState.value.mmsiStr).trim()
    if (mmsiToTrack.isBlank()) {
      _uiState.update { it.copy(aisErrorMessage = "Lütfen geçerli bir MMSI veya IMO numarası giriniz.") }
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
            aisSuccessMessage = "🛰️ ${aisData.name} (MMSI: ${aisData.mmsi}, IMO: ${aisData.imo}) bilgileri başarıyla çekildi!"
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isAisLoading = false,
            aisErrorMessage = "AIS bilgileri alınamadı: ${e.localizedMessage ?: "Bağlantı hatası"}"
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
          aisSuccessMessage = "🌊 AIS Canlı Harita ve Seyir Bilgileri Yüklendi!"
        )
      }
    }
  }

  /**
   * AIS Bölgesi Canlı Bilgilerini Getirir.
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
          aisSuccessMessage = "🌊 AIS Canlı Harita Bilgileri Yüklendi!"
        )
      }
    }
  }

  /**
   * AIS özel gemi bilgilerini çeker ve uygulamaya yükler.
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
          aisSuccessMessage = "⚓ AIS Harita ve Seyir Verileri Başarıyla Yüklendi!"
        )
      }
    }
  }

  /**
   * Gelen AIS verilerini ve telemetriyi form girdilerine, haritaya ve hesaplamalara uygular.
   * Gemi mevkiinin GPS koordinatlarına (enlem/boylam) ait deniz derinliğini anında getirir.
   */
  fun applyAisVesselData(data: AisVesselData) {
    val latFormatted = com.example.model.LocationPresets.formatMarineLatDMS(data.latitude)
    val lonFormatted = com.example.model.LocationPresets.formatMarineLonDMS(data.longitude)
    val sogFormatted = String.format(Locale.US, "%.1f", data.sogKnots)
    val headingFormatted = String.format(Locale.US, "%03d", data.headingDegrees)
    val bridgeToHawse = String.format(Locale.US, "%.1f", (data.loaMeters * 0.25).coerceAtLeast(10.0))
    val bridgeToStern = String.format(Locale.US, "%.1f", (data.loaMeters * 0.75).coerceAtLeast(20.0))

    // AIS gemi mevkisinin GPS koordinatlarına göre anlık batimetri / derinlik verisi
    val initialDepthResult = com.example.engine.MarineDepthProvider.calculateRegionalDepthFallback(data.latitude, data.longitude)
    val initialDepthFormatted = String.format(Locale.US, "%.1f", initialDepthResult.depthMeters)

    _uiState.update { current ->
      val updated = current.copy(
        vesselName = data.name.ifBlank { current.vesselName },
        mmsiStr = data.mmsi,
        imoStr = data.imo,
        callSignStr = data.callSign,
        vesselTypeStr = data.shipType,
        loaStr = data.loaMeters.toString(),
        beamStr = data.beamMeters.toString(),
        draftStr = data.draftMeters.toString(),
        anchorLoaStr = data.loaMeters.toString(),
        anchorBridgeToHawseStr = bridgeToHawse,
        anchorBridgeToSternStr = bridgeToStern,
        latStr = latFormatted,
        lonStr = lonFormatted,
        speedStr = sogFormatted,
        headingDegreesStr = headingFormatted,
        isCustomPort = true,
        activeMarineTrafficUrl = data.marineTrafficUrl,
        activeAisVesselData = data,
        activeLocationSource = LocationSource.AIS,
        isDepthLoading = true,
        chartedDepthStr = initialDepthFormatted,
        anchorDepthStr = initialDepthFormatted,
        verifiedMarineDepth = initialDepthResult
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
    fetchVerifiedDepth(data.latitude, data.longitude)
  }

  /**
   * NB252 (MMSI: 222111447) Askeri Gemisinin Canlı AIS / Demirleme Mevki ve Gemi Parametrelerini Alır.
   */
  fun loadNb252AnchorParameters() {
    viewModelScope.launch {
      _uiState.update { it.copy(isAisLoading = true, aisErrorMessage = null) }
      val shipData = com.example.engine.AisTrackingEngine.getNb252ShipData()
      applyAisVesselData(shipData)
      _uiState.update { current ->
        val bridgeToHawse = String.format(Locale.US, "%.1f", shipData.loaMeters * 0.25)
        val bridgeToStern = String.format(Locale.US, "%.1f", shipData.loaMeters * 0.75)
        val updated = current.copy(
          isAisLoading = false,
          isMmsiTrackingActive = true,
          activeAisVesselData = shipData,
          anchorLoaStr = shipData.loaMeters.toString(),
          anchorBridgeToHawseStr = bridgeToHawse,
          anchorBridgeToSternStr = bridgeToStern,
          anchorChainShacklesStr = "5.0",
          anchorChainScopeStr = "137.5",
          anchorEvent = if (current.anchorEvent.isAnchored) {
            current.anchorEvent.copy(
              latitude = shipData.latitude,
              longitude = shipData.longitude
            )
          } else {
            current.anchorEvent
          },
          aisSuccessMessage = "⚓ NB252 (MMSI: 222111447) Demirleme Koordinatları (${LocationPresets.formatMarineCoordinates(shipData.latitude, shipData.longitude)}) ve Gemi Parametreleri Yüklendi!"
        )
        val anchorRes = computeAnchorCalculation(updated)
        updated.copy(anchorCalculationResult = anchorRes)
      }
    }
  }

  /**
   * Tüm GPS verilerini (Mevki, SOG, COG, İrtifa, Hassasiyet) eksiksiz deniz parametrelerine dönüştürür ve uygular.
   */
  fun syncAllGpsToMarineParameters(formatAsDms: Boolean = true) {
    _uiState.update { it.copy(activeLocationSource = LocationSource.GPS) }
    val currentFix = _uiState.value.lastGpsFix
    if (currentFix != null) {
      applyGpsFix(currentFix, isExplicitSync = true, formatAsDms = formatAsDms)
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

  fun syncAisShipPosition() {
    val aisVessel = _uiState.value.activeAisVesselData ?: com.example.engine.AisTrackingEngine.getNb252ShipData()
    val latFormatted = com.example.model.LocationPresets.formatMarineLatDMS(aisVessel.latitude)
    val lonFormatted = com.example.model.LocationPresets.formatMarineLonDMS(aisVessel.longitude)

    val fallbackDepth = com.example.engine.MarineDepthProvider.calculateRegionalDepthFallback(aisVessel.latitude, aisVessel.longitude)
    val depthFormatted = String.format(Locale.US, "%.1f", fallbackDepth.depthMeters)

    _uiState.update { current ->
      val updated = current.copy(
        latStr = latFormatted,
        lonStr = lonFormatted,
        speedStr = String.format(Locale.US, "%.1f", aisVessel.sogKnots),
        headingDegreesStr = String.format(Locale.US, "%03d", aisVessel.headingDegrees),
        vesselName = aisVessel.name.ifBlank { current.vesselName },
        mmsiStr = aisVessel.mmsi.ifBlank { current.mmsiStr },
        imoStr = aisVessel.imo.ifBlank { current.imoStr },
        callSignStr = aisVessel.callSign.ifBlank { current.callSignStr },
        vesselTypeStr = aisVessel.shipType.ifBlank { current.vesselTypeStr },
        loaStr = if (aisVessel.loaMeters > 0) String.format(Locale.US, "%.1f", aisVessel.loaMeters) else current.loaStr,
        beamStr = if (aisVessel.beamMeters > 0) String.format(Locale.US, "%.1f", aisVessel.beamMeters) else current.beamStr,
        draftStr = if (aisVessel.draftMeters > 0) String.format(Locale.US, "%.1f", aisVessel.draftMeters) else current.draftStr,
        isCustomPort = true,
        activeAisVesselData = aisVessel,
        activeLocationSource = LocationSource.AIS,
        isDepthLoading = true,
        chartedDepthStr = depthFormatted,
        anchorDepthStr = depthFormatted,
        verifiedMarineDepth = fallbackDepth,
        aisSuccessMessage = "⚓ AIS Verileri (${aisVessel.name}) ve Mevkii Derinlik Bilgisi (${depthFormatted}m) Aktarıldı! (AIS Aktif)"
      )
      val speedRes = computeSpeedCalculation(updated)
      val simpleEta = computeSimpleEta(updated)
      val anchorRes = computeAnchorCalculation(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes,
        simpleEtaResult = simpleEta,
        anchorCalculationResult = anchorRes
      )
    }
    fetchVerifiedDepth(aisVessel.latitude, aisVessel.longitude)
  }

  fun syncGpsTelemetryToInputs(asDms: Boolean = true) {
    val currentFix = _uiState.value.lastGpsFix
    if (currentFix != null) {
      applyGpsFix(currentFix, isExplicitSync = true, formatAsDms = asDms)
    } else {
      syncAllGpsToMarineParameters(formatAsDms = asDms)
    }
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

  fun applyGpsFix(fix: GpsFix, isExplicitSync: Boolean = false, formatAsDms: Boolean = true) {
    // AIS aktifken kullanıcı açıkça "GPS VERİSİ" butonuna basana kadar GPS verilerine kendiliğinden dönülmez
    if (!isExplicitSync && _uiState.value.activeLocationSource == LocationSource.AIS) {
      _uiState.update { current ->
        current.copy(
          isGpsLoading = false,
          lastGpsFix = fix
        )
      }
      return
    }

    fetchVerifiedDepth(fix.latitude, fix.longitude)
    val latFormatted = if (formatAsDms) {
      com.example.model.LocationPresets.formatMarineLatDMS(fix.latitude)
    } else {
      String.format(Locale.US, "%.5f", fix.latitude)
    }
    val lonFormatted = if (formatAsDms) {
      com.example.model.LocationPresets.formatMarineLonDMS(fix.longitude)
    } else {
      String.format(Locale.US, "%.5f", fix.longitude)
    }

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

    val marinePos = com.example.model.LocationPresets.formatMarineDmsCoordinates(fix.latitude, fix.longitude)
    val sogKnots = fix.speedKnots ?: 0.0
    val cogDeg = fix.bearingDegrees?.toInt() ?: 0
    val altitudeStr = if (fix.altitudeMeters != null) "${String.format(Locale.US, "%.1f", fix.altitudeMeters)} m" else "0.0 m (Deniz Seviyesi)"
    val accuracyStr = "±${String.format(Locale.US, "%.1f", fix.accuracyMeters)} m"

    val successMsg = if (isExplicitSync) {
      "🌊 DENİZ GPS KOORDİNATLARI (DMS) VE TELEMETRİ AKTARILDI!\n" +
      "• Mevki (Deniz GPS): $marinePos\n" +
      "• SOG (Hız): ${String.format(Locale.US, "%.1f", sogKnots)} kn | COG (Rota): ${String.format(Locale.US, "%03d", cogDeg)}°\n" +
      "• İrtifa: $altitudeStr | GPS Hassasiyeti: $accuracyStr\n" +
      "• EMODnet Bathymetry: Koordinata göre derinlik doğrulanıyor...\n" +
      "• En Yakın Deniz Bölgesi: ${nearestLocationName ?: "Açık Deniz"}"
    } else {
      "🛰️ Canlı GPS (DMS): $marinePos • SOG: ${String.format(Locale.US, "%.1f", sogKnots)} kn • COG: ${String.format(Locale.US, "%03d", cogDeg)}° • $accuracyStr"
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

      val speedVal = fix.speedKnots ?: (speedStrUpdated.toDoubleOrNull() ?: 0.0)
      val headingVal = fix.bearingDegrees?.toInt() ?: (headingStrUpdated.toIntOrNull() ?: 0)
      val timeNow = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
      val newPoint = VesselTrackPoint(
        latitude = fix.latitude,
        longitude = fix.longitude,
        speedKnots = speedVal,
        headingDegrees = headingVal,
        timestampEpochMs = System.currentTimeMillis(),
        timeFormatted = timeNow
      )
      val updatedTrack = (current.trackHistory + newPoint).takeLast(500)

      val updated = current.copy(
        isGpsLoading = false,
        isGpsActive = true,
        lastGpsFix = fix,
        latStr = latFormatted,
        lonStr = lonFormatted,
        speedStr = speedStrUpdated,
        headingDegreesStr = headingStrUpdated,
        isCustomPort = true,
        activeLocationSource = LocationSource.GPS,
        gpsNearestPortInfo = nearestLocationName,
        gpsSuccessMessage = successMsg,
        gpsErrorMessage = null,
        trackHistory = updatedTrack
      )
      val speedRes = computeSpeedCalculation(updated)
      val simpleEta = computeSimpleEta(updated)
      updated.copy(
        analysis = computeAnalysis(updated),
        speedCalculationResult = speedRes,
        simpleEtaResult = simpleEta
      )
    }
    // Refresh weather asynchronously on new fix
    refreshWeather()
  }

  fun refreshGps() {
    _uiState.update { it.copy(isGpsLoading = true) }
  }

  fun triggerMob() {
    val currentFix = _uiState.value.lastGpsFix
    val lat = currentFix?.latitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.latStr) ?: _uiState.value.selectedPort.latitude)
    val lon = currentFix?.longitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.lonStr) ?: _uiState.value.selectedPort.longitude)
    val heading = currentFix?.bearingDegrees?.toInt() ?: (_uiState.value.headingDegreesStr.toIntOrNull() ?: 0)
    val speed = currentFix?.speedKnots ?: (_uiState.value.speedStr.toDoubleOrNull() ?: 0.0)
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
        gpsSuccessMessage = "MOB alarmı kapatıldı / normal seyir moduna dönüldü."
      )
    }
  }

  fun dropAnchor(customLat: Double? = null, customLon: Double? = null) {
    val lat = customLat ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.latStr) ?: _uiState.value.selectedPort.latitude)
    val lon = customLon ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.lonStr) ?: _uiState.value.selectedPort.longitude)
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

  fun setAnchorChainAuto(isAuto: Boolean) {
    _uiState.update { current ->
      val updated = current.copy(isAnchorChainAuto = isAuto)
      val (finalState, _) = syncAndComputeAnchorCalculation(updated)
      finalState
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
    bottomType: AnchorBottomType? = null,
    isAutoChain: Boolean? = null
  ) {
    _uiState.update { current ->
      val isUserExplicitlyEnteringChain = (chainScope != null || chainShackles != null)
      val newIsAutoChain = when {
        isAutoChain != null -> isAutoChain
        isUserExplicitlyEnteringChain -> false
        else -> current.isAnchorChainAuto
      }

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
        shackleStandard != null && !newIsAutoChain -> {
          // Standart değiştiğinde mevcut kilit sayısına göre metreyi yeniden hesapla
          val sh = current.anchorChainShacklesStr.toDoubleOrNull() ?: 5.0
          val mStr = String.format(Locale.US, "%.1f", sh * metersPerShackle)
          Pair(mStr, current.anchorChainShacklesStr)
        }
        else -> Pair(current.anchorChainScopeStr, current.anchorChainShacklesStr)
      }

      var newBridgeToHawseStr = bridgeToHawse ?: current.anchorBridgeToHawseStr
      var newBridgeToSternStr = bridgeToStern ?: current.anchorBridgeToSternStr
      var newLoaStr = loa ?: current.anchorLoaStr

      if (bridgeToHawse != null || bridgeToStern != null) {
        val bh = newBridgeToHawseStr.toDoubleOrNull() ?: 0.0
        val bs = newBridgeToSternStr.toDoubleOrNull() ?: 0.0
        newLoaStr = String.format(Locale.US, "%.1f", bh + bs)
      } else if (loa != null) {
        val l = newLoaStr.toDoubleOrNull() ?: 0.0
        val bh = newBridgeToHawseStr.toDoubleOrNull() ?: 35.0
        if (l >= bh) {
            newBridgeToSternStr = String.format(Locale.US, "%.1f", l - bh)
        } else {
            // LOA is less than Bridge to Hawse, so adjust Bridge to Hawse and make Stern 0
            newBridgeToHawseStr = String.format(Locale.US, "%.1f", l)
            newBridgeToSternStr = "0.0"
        }
      }

      val updated = current.copy(
        isAnchorChainAuto = newIsAutoChain,
        anchorShackleStandard = activeStandard,
        anchorChainShacklesStr = newShacklesStr,
        anchorChainScopeStr = newMetersStr,
        anchorDepthStr = depth ?: current.anchorDepthStr,
        anchorCustomHorizontalDistStr = customHorizontal ?: current.anchorCustomHorizontalDistStr,
        isAnchorAutoHorizontal = isAutoHorizontal ?: current.isAnchorAutoHorizontal,
        anchorBridgeToHawseStr = newBridgeToHawseStr,
        anchorBridgeToSternStr = newBridgeToSternStr,
        anchorLoaStr = newLoaStr,
        anchorSafetyMarginStr = safetyMargin ?: current.anchorSafetyMarginStr,
        anchorBottomType = bottomType ?: current.anchorBottomType
      )
      val (finalState, _) = syncAndComputeAnchorCalculation(updated)
      finalState
    }
  }

  fun setAnchorShackleStandard(standard: ShackleLengthStandard) {
    updateAnchorCalculation(shackleStandard = standard)
  }

  fun setAnchorBottomType(bottomType: AnchorBottomType) {
    updateAnchorCalculation(bottomType = bottomType)
  }

  fun applySeabedLocation(location: com.example.model.MarineSeabedLocation) {
    updateAnchorCalculation(
      bottomType = location.anchorBottomType,
      depth = String.format(Locale.US, "%.1f", location.typicalDepthMeters)
    )
    _uiState.update {
      it.copy(
        gpsSuccessMessage = "⚓ ${location.nameTr} (${location.chartSymbol} - ${location.seabedNameTr}) Dip Tabiatı ve Derinliği (${location.typicalDepthMeters}m) Uygulandı!"
      )
    }
  }

  fun setAnchorChainShackles(shacklesStr: String) {
    updateAnchorCalculation(chainShackles = shacklesStr)
  }

  fun setAnchorChainScopeByShackles(shackles: Double) {
    val formattedShackles = String.format(Locale.US, "%.1f", shackles)
    updateAnchorCalculation(chainShackles = formattedShackles)
  }

  fun setAnchorWeatherScenario(scenario: AnchorWeatherScenario) {
    _uiState.update { current ->
      val updated = current.copy(anchorWeatherScenario = scenario)
      val (finalState, _) = syncAndComputeAnchorCalculation(updated)
      finalState
    }
  }

  fun applyRecommendedChainScope() {
    setAnchorChainAuto(true)
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

  private fun syncAndComputeAnchorCalculation(state: TideUiState): Pair<TideUiState, AnchorCalculationResult> {
    var currentState = state
    val metersPerShackle = currentState.anchorShackleStandard.metersPerShackle

    if (currentState.isAnchorChainAuto) {
      val b = currentState.anchorDepthStr.toDoubleOrNull() ?: (currentState.chartedDepthStr.toDoubleOrNull() ?: 40.0)
      val windKts = when (currentState.anchorWeatherScenario) {
        AnchorWeatherScenario.LIVE -> currentState.marineWeather.windSpeedKnots
        AnchorWeatherScenario.CALM -> 10.0
        AnchorWeatherScenario.MODERATE -> 20.0
        AnchorWeatherScenario.ROUGH -> 30.0
        AnchorWeatherScenario.STORM -> 42.0
      }
      val waveMeters = when (currentState.anchorWeatherScenario) {
        AnchorWeatherScenario.LIVE -> currentState.marineWeather.waveHeightMeters
        AnchorWeatherScenario.CALM -> 0.4
        AnchorWeatherScenario.MODERATE -> 1.2
        AnchorWeatherScenario.ROUGH -> 2.0
        AnchorWeatherScenario.STORM -> 3.5
      }
      val bft = when (currentState.anchorWeatherScenario) {
        AnchorWeatherScenario.LIVE -> currentState.marineWeather.beaufortScale
        AnchorWeatherScenario.CALM -> 3
        AnchorWeatherScenario.MODERATE -> 5
        AnchorWeatherScenario.ROUGH -> 7
        AnchorWeatherScenario.STORM -> 9
      }
      val seaState = when (currentState.anchorWeatherScenario) {
        AnchorWeatherScenario.LIVE -> currentState.marineWeather.seaStateDescription
        AnchorWeatherScenario.CALM -> "Sakin / Hafif Deniz"
        AnchorWeatherScenario.MODERATE -> "Orta Çalkantılı Deniz"
        AnchorWeatherScenario.ROUGH -> "Sert Rüzgar / Kaba Deniz"
        AnchorWeatherScenario.STORM -> "Fırtına / Ağır Deniz"
      }

      val recommended = com.example.engine.AnchorCalculationEngine.calculateRecommendedChainScope(
        depthMeters = b,
        currentChainMeters = currentState.anchorChainScopeStr.toDoubleOrNull() ?: 137.5,
        metersPerShackle = metersPerShackle,
        bottomType = currentState.anchorBottomType,
        windSpeedKnots = windKts,
        waveHeightMeters = waveMeters,
        beaufortScale = bft,
        seaStateDescription = seaState
      )

      val autoMetersStr = String.format(Locale.US, "%.1f", recommended.recommendedMeters)
      val autoShacklesStr = String.format(Locale.US, "%.1f", recommended.recommendedShackles)

      currentState = currentState.copy(
        anchorChainScopeStr = autoMetersStr,
        anchorChainShacklesStr = autoShacklesStr
      )
    }

    val res = computeAnchorCalculation(currentState)
    return Pair(currentState.copy(anchorCalculationResult = res), res)
  }

  private fun computeAnchorCalculation(state: TideUiState): AnchorCalculationResult {
    val a = state.anchorChainScopeStr.toDoubleOrNull() ?: 137.5
    val b = state.anchorDepthStr.toDoubleOrNull() ?: (state.chartedDepthStr.toDoubleOrNull() ?: 40.0)
    val cCustom = state.anchorCustomHorizontalDistStr.toDoubleOrNull()
    val distBow = state.anchorBridgeToHawseStr.toDoubleOrNull() ?: 35.0
    val distStern = state.anchorBridgeToSternStr.toDoubleOrNull() ?: 85.0
    val loaVal = state.anchorLoaStr.toDoubleOrNull() ?: (state.loaStr.toDoubleOrNull() ?: 120.0)
    val safetyVal = state.anchorSafetyMarginStr.toDoubleOrNull() ?: 0.0

    val windKts = when (state.anchorWeatherScenario) {
      AnchorWeatherScenario.LIVE -> state.marineWeather.windSpeedKnots
      AnchorWeatherScenario.CALM -> 10.0
      AnchorWeatherScenario.MODERATE -> 20.0
      AnchorWeatherScenario.ROUGH -> 30.0
      AnchorWeatherScenario.STORM -> 42.0
    }
    val waveMeters = when (state.anchorWeatherScenario) {
      AnchorWeatherScenario.LIVE -> state.marineWeather.waveHeightMeters
      AnchorWeatherScenario.CALM -> 0.4
      AnchorWeatherScenario.MODERATE -> 1.2
      AnchorWeatherScenario.ROUGH -> 2.0
      AnchorWeatherScenario.STORM -> 3.5
    }
    val bft = when (state.anchorWeatherScenario) {
      AnchorWeatherScenario.LIVE -> state.marineWeather.beaufortScale
      AnchorWeatherScenario.CALM -> 3
      AnchorWeatherScenario.MODERATE -> 5
      AnchorWeatherScenario.ROUGH -> 7
      AnchorWeatherScenario.STORM -> 9
    }
    val seaState = when (state.anchorWeatherScenario) {
      AnchorWeatherScenario.LIVE -> state.marineWeather.seaStateDescription
      AnchorWeatherScenario.CALM -> "Sakin / Hafif Deniz"
      AnchorWeatherScenario.MODERATE -> "Orta Çalkantılı Deniz"
      AnchorWeatherScenario.ROUGH -> "Sert Rüzgar / Kaba Deniz"
      AnchorWeatherScenario.STORM -> "Fırtına / Ağır Deniz"
    }

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
      bottomType = state.anchorBottomType,
      windSpeedKnots = windKts,
      waveHeightMeters = waveMeters,
      beaufortScale = bft,
      seaStateDescription = seaState
    )
    return com.example.engine.AnchorCalculationEngine.calculate(params)
  }

  private fun computeSpeedCalculation(state: TideUiState): SpeedCalculationResult {
    val dynamicSpeed = state.lastGpsFix?.speedKnots?.takeIf { it > 0 } ?: state.activeAisVesselData?.sogKnots?.takeIf { it > 0 }
    val speed = dynamicSpeed ?: (state.speedStr.toDoubleOrNull() ?: state.selectedVessel.defaultSpeedKnots)
    val dynamicHeading = state.lastGpsFix?.bearingDegrees?.toInt()?.takeIf { it > 0 } ?: state.activeAisVesselData?.cogDegrees?.toInt()?.takeIf { it > 0 }
    val heading = dynamicHeading ?: (state.headingDegreesStr.toIntOrNull() ?: 45)
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
    val lat = com.example.model.LocationPresets.parseCoordinateOrDecimal(state.latStr) ?: state.selectedPort.latitude
    val lon = com.example.model.LocationPresets.parseCoordinateOrDecimal(state.lonStr) ?: state.selectedPort.longitude
    val chartedDepth = state.chartedDepthStr.toDoubleOrNull() ?: state.selectedPort.defaultChartedDepthMeters
    val ukc = state.ukcStr.toDoubleOrNull() ?: state.selectedVessel.minUkcMeters
    val dynamicSpeed = state.lastGpsFix?.speedKnots?.takeIf { it > 0 } ?: state.activeAisVesselData?.sogKnots?.takeIf { it > 0 }
    val speed = dynamicSpeed ?: (state.speedStr.toDoubleOrNull() ?: state.selectedVessel.defaultSpeedKnots)
    val dynamicHeading = state.lastGpsFix?.bearingDegrees?.toInt()?.takeIf { it > 0 } ?: state.activeAisVesselData?.cogDegrees?.toInt()?.takeIf { it > 0 }
    val heading = dynamicHeading ?: (state.headingDegreesStr.toIntOrNull() ?: 45)

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

  fun toggleDarkMode() {
    _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
  }

  fun setDarkMode(enabled: Boolean) {
    _uiState.update { it.copy(isDarkMode = enabled) }
  }

  fun setActiveMapLayer(layer: MarineMapLayer) {
    _uiState.update { it.copy(activeMapLayer = layer) }
  }

  fun toggleAutoFollowShip() {
    _uiState.update { it.copy(isAutoFollowShip = !it.isAutoFollowShip) }
  }

  fun setAutoFollowShip(enabled: Boolean) {
    _uiState.update { it.copy(isAutoFollowShip = enabled) }
  }

  fun toggleRangeRings() {
    _uiState.update { it.copy(isShowRangeRings = !it.isShowRangeRings) }
  }

  fun toggleSpeedVector() {
    _uiState.update { it.copy(isShowSpeedVector = !it.isShowSpeedVector) }
  }

  fun setSpeedVectorMinutes(minutes: Int) {
    _uiState.update { it.copy(speedVectorMinutes = minutes) }
  }

  fun toggleMeasureRuler() {
    _uiState.update { it.copy(isMeasureRulerActive = !it.isMeasureRulerActive) }
  }

  fun toggleNightChartMode() {
    _uiState.update { it.copy(isNightChartMode = !it.isNightChartMode) }
  }

  fun clearTrackHistory() {
    _uiState.update { it.copy(trackHistory = emptyList()) }
  }

  fun addManualTrackPoint() {
    val lat = com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.latStr) ?: _uiState.value.selectedPort.latitude
    val lon = com.example.model.LocationPresets.parseCoordinateOrDecimal(_uiState.value.lonStr) ?: _uiState.value.selectedPort.longitude
    val speed = _uiState.value.speedStr.toDoubleOrNull() ?: 0.0
    val heading = _uiState.value.headingDegreesStr.toIntOrNull() ?: 0
    val timeNow = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    val pt = VesselTrackPoint(
      latitude = lat,
      longitude = lon,
      speedKnots = speed,
      headingDegrees = heading,
      timestampEpochMs = System.currentTimeMillis(),
      timeFormatted = timeNow
    )
    _uiState.update {
      it.copy(trackHistory = (it.trackHistory + pt).takeLast(500))
    }
  }

  // ═════════════════════════════════════════════════════════════════
  // OTOMATİK 30 DAKİKALIK VE MANUEL TXT LOG KAYDI İŞLEMLERİ
  // ═════════════════════════════════════════════════════════════════

  private fun startAuto30MinTxtLogging() {
    autoTxtLogJob?.cancel()
    autoTxtLogJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      // Disk üzerindeki tüm logları ilk açılışta yükle
      val logs = com.example.engine.TxtLoggerManager.loadAllLogs(getApplication())
      _uiState.update { it.copy(txtLogHistory = logs) }

      while (coroutineContext.isActive) {
        val now = Calendar.getInstance()
        val min = now.get(Calendar.MINUTE)
        val sec = now.get(Calendar.SECOND)
        val ms = now.get(Calendar.MILLISECOND)

        // Bir sonraki log saati :00 veya :30 dakikalarıdır
        val targetMinute = if (min < 30) 30 else 60
        val minutesToWait = targetMinute - min - 1
        val secondsToWait = 60 - sec - 1
        val millisToWait = 1000 - ms

        val delayMs = (minutesToWait * 60 * 1000L) + (secondsToWait * 1000L) + millisToWait

        val nextCal = Calendar.getInstance().apply {
          add(Calendar.MILLISECOND, delayMs.toInt())
        }
        val sdfNext = SimpleDateFormat("HH:mm", Locale.getDefault())
        _uiState.update { it.copy(nextAutoLogTimeStr = sdfNext.format(nextCal.time)) }

        kotlinx.coroutines.delay(delayMs)

        // :00 ve :30 saat başı / yarım saatlerde otomatik TXT log kaydı al
        performTxtLogSaveInternal(isAuto = true)
      }
    }
  }

  private fun performTxtLogSaveInternal(isAuto: Boolean) {
    val record = com.example.engine.TxtLoggerManager.saveTxtLog(
      context = getApplication(),
      uiState = _uiState.value,
      isAuto = isAuto
    )
    val updatedLogs = com.example.engine.TxtLoggerManager.loadAllLogs(getApplication())
    _uiState.update { current ->
      current.copy(
        txtLogHistory = updatedLogs,
        txtLogMessage = if (isAuto) "⏰ OTOMATİK 30 DK TXT LOG KAYDEDİLDİ (${record.timestampFormatted})"
                        else "💾 MANUEL TXT LOG KAYDEDİLDİ (${record.fileName})"
      )
    }
  }

  fun performManualTxtLogSave() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      performTxtLogSaveInternal(isAuto = false)
    }
  }

  fun selectTxtLogForView(record: com.example.engine.TxtLogRecord?) {
    _uiState.update { it.copy(selectedTxtLogForView = record) }
  }

  fun deleteTxtLogRecord(filePath: String) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      com.example.engine.TxtLoggerManager.deleteLog(getApplication(), filePath)
      val updatedLogs = com.example.engine.TxtLoggerManager.loadAllLogs(getApplication())
      _uiState.update { current ->
        val newSelected = if (current.selectedTxtLogForView?.filePath == filePath) null else current.selectedTxtLogForView
        current.copy(
          txtLogHistory = updatedLogs,
          selectedTxtLogForView = newSelected,
          txtLogMessage = "Log dosyası silindi."
        )
      }
    }
  }

  fun deleteTxtLogRecords(filePaths: List<String>) {
    if (filePaths.isEmpty()) return
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      filePaths.forEach { filePath ->
        com.example.engine.TxtLoggerManager.deleteLog(getApplication(), filePath)
      }
      val updatedLogs = com.example.engine.TxtLoggerManager.loadAllLogs(getApplication())
      _uiState.update { current ->
        val newSelected = if (current.selectedTxtLogForView != null && filePaths.contains(current.selectedTxtLogForView.filePath)) null else current.selectedTxtLogForView
        current.copy(
          txtLogHistory = updatedLogs,
          selectedTxtLogForView = newSelected,
          txtLogMessage = "${filePaths.size} adet log dosyası silindi."
        )
      }
    }
  }

  fun clearAllTxtLogs() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      com.example.engine.TxtLoggerManager.clearAllLogs(getApplication())
      _uiState.update { current ->
        current.copy(
          txtLogHistory = emptyList(),
          selectedTxtLogForView = null,
          txtLogMessage = "Tüm TXT log kayıtları temizlendi."
        )
      }
    }
  }

  fun dismissTxtLogMessage() {
    _uiState.update { it.copy(txtLogMessage = null) }
  }
}

