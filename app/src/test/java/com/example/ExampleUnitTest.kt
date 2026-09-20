package com.example

import com.example.engine.TideCalculatorEngine
import com.example.model.LocationPresets
import com.example.model.VesselPresets
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ExampleUnitTest {

  @Test
  fun testSquatCalculation() {
    // Cb = 0.75, Speed = 6.0 knot
    val squat = TideCalculatorEngine.calculateSquat(0.75, 6.0, isConfinedChannel = true)
    assertTrue("Squat should be positive", squat > 0.0)
    assertEquals(0.54, squat, 0.05)
  }

  @Test
  fun testTideAnalysisProducesSafeWindows() {
    val vessel = com.example.model.VesselProfile(
      id = "test_ship",
      name = "Test Ship",
      blockCoefficient = 0.75,
      draftMeters = 8.0,
      defaultSpeedKnots = 6.0,
      minUkcMeters = 1.0
    )
    val port = LocationPresets.defaultPorts[0] // Dover
    val cal = Calendar.getInstance()

    val analysis = TideCalculatorEngine.analyzeNavigation(
      vessel = vessel,
      location = port,
      customLat = port.latitude,
      customLon = port.longitude,
      chartedDepth = 10.0,
      actualDraft = 8.0,
      minUkc = 1.0,
      speedKnots = 6.0,
      selectedCalendar = cal
    )

    assertNotNull(analysis)
    assertTrue(analysis.curvePoints.isNotEmpty())
    assertEquals(289, analysis.curvePoints.size)
    assertTrue("Should have calculated required depth", analysis.totalRequiredDepthMeters > 8.0)
    assertTrue("Rule of twelfths should have 6 steps", analysis.ruleOfTwelfths.size == 6)
    assertTrue("Instant total depth should equal charted depth + instant tide", analysis.currentInstantTotalDepthMeters > 0.0)
    assertNotNull(analysis.currentInfo)
    assertNotNull(analysis.windInfo)
    assertTrue(analysis.vesselHeadingCardinal.isNotEmpty())
  }

  @Test
  fun testMarineCoordinatesFormatting() {
    val formatted = LocationPresets.formatMarineCoordinates(38.4410, 27.1438)
    assertTrue("Should contain degree symbol", formatted.contains("°"))
    assertTrue("Should contain N and E", formatted.contains("N") && formatted.contains("E"))
  }

  @Test
  fun testHeadingCardinal() {
    assertEquals("K / Kuzey (000°)", TideCalculatorEngine.calculateHeadingCardinal(0))
    assertEquals("KD / Kuzeydoğu (045°)", TideCalculatorEngine.calculateHeadingCardinal(45))
    assertEquals("D / Doğu (090°)", TideCalculatorEngine.calculateHeadingCardinal(90))
    assertEquals("G / Güney (180°)", TideCalculatorEngine.calculateHeadingCardinal(180))
    assertEquals("B / Batı (270°)", TideCalculatorEngine.calculateHeadingCardinal(270))
  }

  @Test
  fun testNearestPortsCalculation() {
    // Tuzla Askeri Tersanesi & DHO coordinates: 40.8283, 29.2540
    val nearest = LocationPresets.getNearestPorts(40.8283, 29.2540, speedKnots = 12.0)
    assertTrue("Nearest ports list should not be empty", nearest.isNotEmpty())
    assertEquals("First nearest should be Tuzla naval base with distance 0 NM", "tuzla_askeri_tersane_dho", nearest[0].port.id)
    assertEquals(0.0, nearest[0].distanceNm, 0.5)

    // Check Gölcük is close to Tuzla location
    val golcukPort = nearest.find { it.port.id == "golcuk_deniz_ana_ussu" }
    assertNotNull(golcukPort)
    assertTrue("Gölcük should be within 35 NM from Tuzla", golcukPort!!.distanceNm < 35.0)
    assertTrue("Bearing degrees should be valid (0..359)", golcukPort.bearingDegrees in 0..359)
    assertTrue("ETA formatting should be populated", golcukPort.estimatedTimeFormatted.isNotEmpty())
  }

  @Test
  fun testAnchorChainRecommendationCalmVsStorm() {
    // Derinlik 40m, Mevcut zincir 5 kilit (137.5m), Kilit 27.5m
    val calmRec = com.example.engine.AnchorCalculationEngine.calculateRecommendedChainScope(
      depthMeters = 40.0,
      currentChainMeters = 137.5,
      metersPerShackle = 27.5,
      bottomType = com.example.model.AnchorBottomType.MUD_SAND,
      windSpeedKnots = 10.0,
      waveHeightMeters = 0.4,
      beaufortScale = 3
    )
    assertNotNull(calmRec)
    assertEquals(com.example.model.WeatherSeverityLevel.CALM, calmRec.weatherSeverity)
    assertTrue("Tavsiye edilen kilit sayısı asgari güvenli kilit sayısından (3.0) büyük veya eşit olmalı", calmRec.recommendedShackles >= 3.0)

    // Ağır fırtına şartları (38 kn rüzgar, 3.0m dalga)
    val stormRec = com.example.engine.AnchorCalculationEngine.calculateRecommendedChainScope(
      depthMeters = 40.0,
      currentChainMeters = 137.5,
      metersPerShackle = 27.5,
      bottomType = com.example.model.AnchorBottomType.MUD_SAND,
      windSpeedKnots = 38.0,
      waveHeightMeters = 3.0,
      beaufortScale = 8
    )
    assertNotNull(stormRec)
    assertEquals(com.example.model.WeatherSeverityLevel.STORM, stormRec.weatherSeverity)
    assertTrue("Fırtınada tavsiye edilen kilit sakin havadakinden fazla olmalı", stormRec.recommendedShackles > calmRec.recommendedShackles)
    assertEquals(com.example.model.ChainRecommendationStatus.DEFICIENT, stormRec.status)
    assertTrue(stormRec.recommendationHeading.contains("İlave Kaloma Veriniz"))
  }
}
