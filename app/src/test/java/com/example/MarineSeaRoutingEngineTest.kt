package com.example

import com.example.engine.MarineLandMask
import com.example.engine.MarineSeaRoute
import com.example.engine.MarineSeaRoutingEngine
import com.example.model.LocationPresets
import com.example.model.TurkishPorts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarineSeaRoutingEngineTest {

  private fun collectRouteIssues(route: MarineSeaRoute, label: String): List<String> {
    val issues = ArrayList<String>()
    if (route.isRejectedLandTarget) {
      issues.add("$label unexpectedly rejected as land")
      return issues
    }
    if (route.pathCoordinates.size < 2) {
      issues.add("$label has < 2 pathCoordinates")
      return issues
    }

    route.pathCoordinates.forEachIndexed { idx, pt ->
      if (MarineLandMask.isPointOnLand(pt.latitude, pt.longitude)) {
        issues.add("$label pathCoordinate[$idx] (${pt.latitude}, ${pt.longitude}) is ON LAND")
      }
    }

    var prevMinutes = -1
    route.waypoints.forEachIndexed { idx, wp ->
      if (wp.id != "DN${idx + 1}") {
        issues.add("$label expected DN${idx + 1} but got ${wp.id}")
      }
      if (MarineLandMask.isPointOnLand(wp.latitude, wp.longitude)) {
        issues.add("$label waypoint ${wp.id} (${wp.latitude}, ${wp.longitude}) is ON LAND")
      }
      if (wp.etaTimeStr.isBlank() || wp.durationStr.isBlank()) {
        issues.add("$label ${wp.id} has blank ETA/duration")
      }
      if (wp.cumulativeMinutes < prevMinutes) {
        issues.add("$label ${wp.id} non-monotonic ETA minutes")
      }
      prevMinutes = wp.cumulativeMinutes
    }

    for (i in 0 until route.pathCoordinates.size - 1) {
      val p1 = route.pathCoordinates[i]
      val p2 = route.pathCoordinates[i + 1]
      val crossesLand = MarineLandMask.doesSegmentCrossLand(
        p1.latitude,
        p1.longitude,
        p2.latitude,
        p2.longitude,
        clearanceNm = 0.0
      )
      if (crossesLand) {
        issues.add(
          "$label leg[$i->${i + 1}] (${p1.latitude},${p1.longitude})->(${p2.latitude},${p2.longitude}) CROSSES LAND (totalPts=${route.pathCoordinates.size})"
        )
      }
    }
    return issues
  }

  @Test
  fun corridorGraphIsConnectedAndEveryEdgeIsOnSea() {
    val issues = MarineSeaRoutingEngine.debugVerifyGraphIntegrity()
    assertTrue("Corridor graph integrity issues:\n" + issues.joinToString("\n"), issues.isEmpty())
  }

  @Test
  fun defaultStartupTuzlaToGolcukRouteNeverCrossesLand() {
    val route = MarineSeaRoutingEngine.calculateSeaRoute(
      originLat = 40.82833,
      originLon = 29.25399,
      destLat = 40.7180,
      destLon = 29.8350,
      speedKnots = 12.0,
      targetName = "Gölcük Deniz Ana Üssü"
    )
    assertTrue(
      "Tuzla -> Gölcük must follow İzmit Körfezi TSS fairway waypoints (got ${route.waypoints.size})",
      route.waypoints.size >= 4
    )
    val issues = collectRouteIssues(route, "Startup Tuzla -> Gölcük")
    assertTrue(issues.joinToString("\n"), issues.isEmpty())
  }

  @Test
  fun routesFollowNavigableSeaCorridorsNotCrowFliesLines() {
    val corridorVoyages = listOf(
      Triple("Tuzla -> Çınarcık", Pair(40.82833, 29.25399), Pair(40.6480, 29.1200)),
      Triple("Tuzla -> Bandırma", Pair(40.82833, 29.25399), Pair(40.3556, 27.9739)),
      Triple("Tuzla -> Çanakkale", Pair(40.82833, 29.25399), Pair(40.1553, 26.4142)),
      Triple("Tuzla -> Kdz. Ereğli", Pair(40.82833, 29.25399), Pair(41.2820, 31.4180)),
      Triple("Büyükçekmece -> Tuzla", Pair(41.0050, 28.5750), Pair(40.82833, 29.25399))
    )
    for ((label, orig, dest) in corridorVoyages) {
      val route = MarineSeaRoutingEngine.calculateSeaRoute(
        originLat = orig.first,
        originLon = orig.second,
        destLat = dest.first,
        destLon = dest.second,
        speedKnots = 14.0,
        targetName = label
      )
      assertTrue(
        "$label should follow navigable sea-lane turning points rather than crow-flies direct line (waypoints=${route.waypoints.size})",
        route.waypoints.size >= 2
      )
      val issues = collectRouteIssues(route, label)
      assertTrue(issues.joinToString("\n"), issues.isEmpty())
    }
  }

  @Test
  fun challengingPeninsulasStraitsAndIslandsNeverCrossLand() {
    val testPairs = listOf(
      Triple("Tuzla -> Kdz. Ereğli (İstanbul Boğazı)", Pair(40.82833, 29.25399), Pair(41.2820, 31.4180)),
      Triple("Tuzla -> İğneada (Boğaz & Batı Karadeniz)", Pair(40.82833, 29.25399), Pair(41.8783, 27.9867)),
      Triple("Tuzla -> Aksaz (Çanakkale & Ege & Bozburun)", Pair(40.82833, 29.25399), Pair(36.8380, 28.4000)),
      Triple("Tuzla -> İskenderun (Tüm Kıyılar)", Pair(40.82833, 29.25399), Pair(36.5950, 36.1750)),
      Triple("Bandırma -> Erdek (Kapıdağ Yarımadası Etrafı)", Pair(40.3556, 27.9739), Pair(40.3983, 27.7917)),
      Triple("Yalova -> Gemlik (Armutlu Bozburun Etrafı)", Pair(40.6500, 29.2666), Pair(40.4166, 29.1500)),
      Triple("Yalova -> Mudanya (Armutlu Yarımadası)", Pair(40.6500, 29.2666), Pair(40.3833, 28.8833)),
      Triple("İzmir Alsancak -> Çeşme (Uzunada & Karaburun)", Pair(38.4333, 27.1333), Pair(38.3225, 26.3050)),
      Triple("Çeşme -> Sığacık (Teke Burnu Etrafı)", Pair(38.3225, 26.3050), Pair(38.1967, 26.7867)),
      Triple("Kuşadası -> Didim (Dilek Boğazı)", Pair(37.8633, 27.2567), Pair(37.3367, 27.2567)),
      Triple("Bodrum -> Datça (Knidos Deveboynu Etrafı)", Pair(37.0333, 27.4333), Pair(36.7217, 27.6883)),
      Triple("Datça -> Marmaris (Bozburun & Sömbeki)", Pair(36.7217, 27.6883), Pair(36.8500, 28.2666)),
      Triple("Kaş -> Antalya (Kekova & Gelidonya)", Pair(36.1995, 29.6385), Pair(36.8333, 30.6166)),
      Triple("Sinop -> Samsun (Boztepe & Bafra Burnu)", Pair(42.0050, 35.1650), Pair(41.2967, 36.3450)),
      Triple("İnebolu -> Sinop (İnceburun & Boztepe)", Pair(41.9783, 33.7667), Pair(42.0050, 35.1650)),
      Triple("Büyükada -> Kınalıada -> Haliç", Pair(40.8744, 29.1286), Pair(41.0225, 28.9772)),
      Triple("Mersin -> Girne", Pair(36.7833, 34.6333), Pair(35.3417, 33.3283)),
      Triple("Girne -> Gazimağusa (Kıbrıs Karpaz Etrafı)", Pair(35.3417, 33.3283), Pair(35.1233, 33.9483))
    )

    val allIssues = ArrayList<String>()
    for ((name, orig, dest) in testPairs) {
      val route = MarineSeaRoutingEngine.calculateSeaRoute(
        originLat = orig.first,
        originLon = orig.second,
        destLat = dest.first,
        destLon = dest.second,
        speedKnots = 14.0,
        targetName = name
      )
      allIssues.addAll(collectRouteIssues(route, name))
    }
    assertTrue("Challenging route issues:\n" + allIssues.joinToString("\n"), allIssues.isEmpty())
  }

  @Test
  fun allStrategicLocationsRoute100PercentOnSea() {
    val shipLat = 40.82833
    val shipLon = 29.25399
    val allIssues = ArrayList<String>()
    for (port in LocationPresets.strategicMarineLocations) {
      val route = MarineSeaRoutingEngine.calculateSeaRoute(
        originLat = shipLat,
        originLon = shipLon,
        destLat = port.latitude,
        destLon = port.longitude,
        speedKnots = 15.0,
        targetName = port.name
      )
      allIssues.addAll(collectRouteIssues(route, "Strategic[${port.name}]"))
    }
    assertTrue("Strategic port route issues:\n" + allIssues.joinToString("\n"), allIssues.isEmpty())
  }

  @Test
  fun allTurkishPortsPresetsRoute100PercentOnSea() {
    val shipLat = 40.82833
    val shipLon = 29.25399
    val allIssues = ArrayList<String>()
    for (dest in TurkishPorts.list) {
      val route = MarineSeaRoutingEngine.calculateSeaRoute(
        originLat = shipLat,
        originLon = shipLon,
        destLat = dest.lat,
        destLon = dest.lon,
        speedKnots = 15.0,
        targetName = dest.name
      )
      allIssues.addAll(collectRouteIssues(route, "TurkishPort[${dest.name}]"))
    }
    assertTrue("TurkishPorts route issues:\n" + allIssues.joinToString("\n"), allIssues.isEmpty())
  }

  @Test
  fun inlandDryLandClicksAreStrictlyRejected() {
    val landPoints = listOf(
      Triple("Anadolu İç Kesim (Ankara)", 39.9208, 32.8541),
      Triple("Tuzla DHO Yarımadası İçi", 40.8120, 29.2660),
      Triple("Gelibolu Yarımadası Kara İçi", 40.3200, 26.5400),
      Triple("Kapıdağ Yarımadası Kara İçi", 40.4700, 27.8500),
      Triple("Armutlu Yarımadası Kara İçi", 40.5500, 28.9800),
      Triple("Karaburun Yarımadası Kara İçi", 38.5200, 26.5200),
      Triple("Büyükada Tepe Kara İçi", 40.8580, 29.1220),
      Triple("Marmara Adası Kara İçi", 40.6150, 27.6100),
      Triple("Gökçeada Kara İçi", 40.1650, 25.8200),
      Triple("Midilli Adası Kara İçi", 39.2000, 26.2500),
      Triple("Kıbrıs Kara İçi", 35.1500, 33.3500)
    )

    for ((label, lat, lon) in landPoints) {
      val route = MarineSeaRoutingEngine.calculateSeaRoute(
        originLat = 40.82833,
        originLon = 29.25399,
        destLat = lat,
        destLon = lon,
        speedKnots = 12.0,
        targetName = label,
        rejectLandDestination = true
      )
      assertTrue("$label MUST be rejected as dry land!", route.isRejectedLandTarget)
      assertTrue("$label rejected route must have empty waypoints", route.waypoints.isEmpty())
      assertTrue("$label rejected route must have empty pathCoordinates", route.pathCoordinates.isEmpty())
    }
  }

  @Test
  fun routeGeometryLruCacheAcceleratesRepeatedCalculations() {
    MarineSeaRoutingEngine.clearRouteCache()
    val hitsBefore = MarineSeaRoutingEngine.routeCacheHits

    val slowSpeedRoute = MarineSeaRoutingEngine.calculateSeaRoute(
      originLat = 40.82833,
      originLon = 29.25399,
      destLat = 40.1553,
      destLon = 26.4142,
      speedKnots = 10.0,
      targetName = "Çanakkale"
    )
    val fastSpeedRoute = MarineSeaRoutingEngine.calculateSeaRoute(
      originLat = 40.82833,
      originLon = 29.25399,
      destLat = 40.1553,
      destLon = 26.4142,
      speedKnots = 20.0,
      targetName = "Çanakkale"
    )

    assertTrue("LRU geometry cache must register hit on speed change", MarineSeaRoutingEngine.routeCacheHits > hitsBefore)
    assertEquals(slowSpeedRoute.pathCoordinates.size, fastSpeedRoute.pathCoordinates.size)
    assertTrue("20kn ETA minutes must be shorter than 10kn ETA minutes", fastSpeedRoute.totalMinutes < slowSpeedRoute.totalMinutes)
  }
}
