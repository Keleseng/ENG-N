package com.example

import com.example.engine.SurroundingAisRadarEngine
import com.example.model.RadarAisTarget
import org.junit.Assert.*
import org.junit.Test

class AisRadarUnitTest {

  @Test
  fun testDistanceAndBearingCalculation() {
    // İstanbul Boğazı giriş mevkii (yaklaşık 41.0° K, 29.0° D)
    val lat1 = 41.0
    val lon1 = 29.0
    val lat2 = 41.1
    val lon2 = 29.0

    val distNm = RadarAisTarget.calculateDistanceNm(lat1, lon1, lat2, lon2)
    // 0.1 derece enlem farkı yaklaşık 6 deniz milidir (1 derece enlem = 60 NM)
    assertEquals(6.0, distNm, 0.2)

    val bearing = RadarAisTarget.calculateBearingDegrees(lat1, lon1, lat2, lon2)
    // Doğrudan kuzey = 000°
    assertEquals(0.0, bearing, 1.0)
  }

  @Test
  fun testCpaTcpaCalculation() {
    // Kendi gemimiz (40.80, 29.20), SOG: 10 kn, COG: 000°
    // Karşıdan gelen hedef gemi (40.85, 29.20), SOG: 10 kn, COG: 180°
    val target = RadarAisTarget(
      id = "test_target",
      name = "TEST VESSEL",
      mmsi = "271000111",
      latitude = 40.85,
      longitude = 29.20,
      sogKnots = 10.0,
      cogDegrees = 180.0
    )

    val (cpaNm, tcpaMin) = target.calculateCpaTcpa(
      ownLat = 40.80,
      ownLon = 29.20,
      ownSogKnots = 10.0,
      ownCogDegrees = 0.0
    )

    // Karşılıklı rota ve aynı boylamda kafa kafaya gelme durumu: CPA sıfıra çok yakın olmalı
    assertTrue("CPA sıfıra yakın olmalı", cpaNm < 0.2)
    // Mesafe yaklaşık 3 NM, yaklaşma hızı 20 kn, süre yaklaşık 9 dk
    assertTrue("TCPA pozitif ve gerçekçi olmalı", tcpaMin in 5.0..15.0)
  }

  @Test
  fun testSurroundingAisRadarEngineGeneration() {
    val centerLat = 40.82833
    val centerLon = 29.25399

    val vessels = SurroundingAisRadarEngine.generateSurroundingVessels(centerLat, centerLon, 0.0, 0.0)
    assertFalse("Gemiler listesi boş olmamalı", vessels.isEmpty())
    assertTrue("En az 5 çevre gemisi bulunmalı", vessels.size >= 5)

    // Tüm gemilerin mesafesi pozitif ve makul olmalı
    vessels.forEach { v ->
      val dist = v.distanceNmFrom(centerLat, centerLon)
      assertTrue("Mesafe pozitif olmalı", dist > 0.0)
      assertTrue("Menzil 0.1 ile 30 NM arasında olmalı", dist in 0.1..30.0)
      assertTrue("SOG geçerli olmalı", v.sogKnots >= 0.0)
    }

    // Hareket simülasyonu testi
    val stepped = SurroundingAisRadarEngine.stepVessels(vessels, centerLat, centerLon, 0.0, 0.0, 5.0)
    assertEquals(vessels.size, stepped.size)
  }
}
