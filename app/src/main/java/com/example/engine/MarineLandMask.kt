package com.example.engine

import com.example.model.Coordinate
import com.example.model.LocationPresets
import java.util.PriorityQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Deniz Haritası Kara Maskesi, Kıyı Çarpışma Denetimi ve Yerel Deniz Yolu Bulucu (Theta*).
 *
 * Gemilerin karadan geçmesini %100 engelleyen geometrik motor:
 * - Noktanın kara/ada poligonu içinde olup olmadığını denetler (Ray-Casting).
 * - İki mevki arasındaki doğru parçasının herhangi bir kıyı, burun, yarımada veya adayı
 *   kesip kesmediğini hem tam kenar kesişimi hem yoğun örnekleme ile denetler.
 * - Yerel koy, burun veya adalar etrafında açık koridora çıkarken dinamik Deniz-Izgarası
 *   Theta* (Any-Angle Sea Pathfinder) algoritmasıyla %100 deniz üzerinden dönüş noktaları üretir.
 */
object MarineLandMask {

  private const val DEG_TO_RAD = PI / 180.0

  /**
   * Verilen koordinatın herhangi bir kara kütlesi veya ada poligonu üzerinde olup olmadığını döner.
   */
  fun isPointOnLand(lat: Double, lon: Double): Boolean {
    val polygons = MarineLandPolygons.allPolygons
    for (i in polygons.indices) {
      val poly = polygons[i]
      if (lat < poly.minLat || lat > poly.maxLat || lon < poly.minLon || lon > poly.maxLon) {
        continue
      }
      if (isPointInPolygon(lat, lon, poly.coords)) {
        return true
      }
    }
    return false
  }

  /**
   * Verilen koordinatın karada olup olmadığını veya kıyıya [clearanceNm] deniz milinden daha yakın olup olmadığını denetler.
   */
  fun isPointTooCloseToLand(lat: Double, lon: Double, clearanceNm: Double): Boolean {
    if (isPointOnLand(lat, lon)) return true
    if (clearanceNm <= 0.001) return false

    val cosLat = cos(lat * DEG_TO_RAD).coerceAtLeast(0.25)
    val dLat = clearanceNm / 60.0
    val dLon = clearanceNm / (60.0 * cosLat)
    val diagLat = dLat * 0.7071
    val diagLon = dLon * 0.7071

    return isPointOnLand(lat + dLat, lon) ||
      isPointOnLand(lat - dLat, lon) ||
      isPointOnLand(lat, lon + dLon) ||
      isPointOnLand(lat, lon - dLon) ||
      isPointOnLand(lat + diagLat, lon + diagLon) ||
      isPointOnLand(lat + diagLat, lon - diagLon) ||
      isPointOnLand(lat - diagLat, lon + diagLon) ||
      isPointOnLand(lat - diagLat, lon - diagLon)
  }

  /**
   * Harita üzerinde tıklanan bir noktanın reddedilmesi gereken bir kara mevkii olup olmadığını belirler.
   * Kıyı çizgisi poligon toleransı (~0.06 NM / ~110 m iskele/rıhtım kenarı) dışındaki tüm kara tıklamaları reddedilir.
   */
  fun isRejectedLandClick(lat: Double, lon: Double): Boolean {
    if (!isPointOnLand(lat, lon)) return false

    // İskele/rıhtım pikseli toleransı: 0.06 NM (~110m) çevrede açık deniz varsa kıyı iskelesi kabul edilir
    val cosLat = cos(lat * DEG_TO_RAD).coerceAtLeast(0.25)
    val tolNm = 0.06
    val dLat = tolNm / 60.0
    val dLon = tolNm / (60.0 * cosLat)
    val directions = 16
    for (dir in 0 until directions) {
      val angle = (2.0 * PI * dir) / directions
      val testLat = lat + dLat * sin(angle)
      val testLon = lon + dLon * cos(angle)
      if (!isPointOnLand(testLat, testLon)) {
        return false
      }
    }
    return true
  }

  /**
   * Kıyı çizgisine çok yakın (örn. liman/iskele koordinatı) bir noktayı en yakın emniyetli deniz suyuna taşır.
   */
  fun snapToNearestSeaWater(lat: Double, lon: Double, clearanceNm: Double = 0.06): Coordinate {
    if (!isPointTooCloseToLand(lat, lon, clearanceNm)) {
      return Coordinate(lat, lon)
    }

    val cosLat = cos(lat * DEG_TO_RAD).coerceAtLeast(0.25)
    val radiiNm = doubleArrayOf(
      0.05, 0.10, 0.16, 0.24, 0.35, 0.50, 0.70, 0.95, 1.25, 1.65, 2.20, 3.00, 4.20,
      5.80, 8.00, 10.80, 14.50
    )
    val directions = 24

    for (rNm in radiiNm) {
      val dLat = rNm / 60.0
      val dLon = rNm / (60.0 * cosLat)
      for (dir in 0 until directions) {
        val angle = (2.0 * PI * dir) / directions
        val candLat = lat + dLat * sin(angle)
        val candLon = lon + dLon * cos(angle)
        if (!isPointTooCloseToLand(candLat, candLon, clearanceNm)) {
          return Coordinate(candLat, candLon)
        }
      }
    }

    // Son çare: sadece kara dışında olma şartıyla ara
    for (rNm in radiiNm) {
      val dLat = rNm / 60.0
      val dLon = rNm / (60.0 * cosLat)
      for (dir in 0 until directions) {
        val angle = (2.0 * PI * dir) / directions
        val candLat = lat + dLat * sin(angle)
        val candLon = lon + dLon * cos(angle)
        if (!isPointOnLand(candLat, candLon)) {
          return Coordinate(candLat, candLon)
        }
      }
    }

    return Coordinate(lat, lon)
  }

  /**
   * İki koordinat arasındaki düz seyir bacağının herhangi bir kara poligonunu veya adayı
   * kesip kesmediğini (veya [clearanceNm] emniyet şeridine girip girmediğini) denetler.
   */
  fun doesSegmentCrossLand(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
    clearanceNm: Double = 0.0
  ): Boolean {
    if (isPointOnLand(lat1, lon1) || isPointOnLand(lat2, lon2)) return true

    val midLat = (lat1 + lat2) * 0.5
    val cosLat = cos(midLat * DEG_TO_RAD).coerceAtLeast(0.25)
    val padLat = (clearanceNm / 60.0) + 0.002
    val padLon = (clearanceNm / (60.0 * cosLat)) + 0.002

    val segMinLat = min(lat1, lat2) - padLat
    val segMaxLat = max(lat1, lat2) + padLat
    val segMinLon = min(lon1, lon2) - padLon
    val segMaxLon = max(lon1, lon2) + padLon

    val allPolys = MarineLandPolygons.allPolygons
    val candidates = ArrayList<LandPolygon>(4)
    for (i in allPolys.indices) {
      val p = allPolys[i]
      if (p.maxLat >= segMinLat && p.minLat <= segMaxLat &&
          p.maxLon >= segMinLon && p.minLon <= segMaxLon) {
        candidates.add(p)
      }
    }
    if (candidates.isEmpty()) return false

    // 1. Tam Poligon Kenarı Kesişim Denetimi (İnce burun ve adaları %100 yakalar)
    for (cIdx in candidates.indices) {
      val coords = candidates[cIdx].coords
      val n = coords.size / 2
      var prevLat = coords[(n - 1) * 2]
      var prevLon = coords[(n - 1) * 2 + 1]
      for (v in 0 until n) {
        val currLat = coords[v * 2]
        val currLon = coords[v * 2 + 1]
        if (segmentsIntersect(lat1, lon1, lat2, lon2, prevLat, prevLon, currLat, currLon)) {
          return true
        }
        prevLat = currLat
        prevLon = currLon
      }
    }

    // 2. Yoğun Ara Nokta ve Yanal Emniyet Koridoru (Clearance) Denetimi
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val approxDistNm = hypot(dLat * 60.0, dLon * 60.0 * cosLat)
    val steps = ceil(approxDistNm / 0.12).toInt().coerceIn(2, 500)

    val useClearance = clearanceNm > 0.005
    var perpLat = 0.0
    var perpLon = 0.0
    if (useClearance && approxDistNm > 1e-5) {
      val dirNmLat = (dLat * 60.0) / approxDistNm
      val dirNmLon = (dLon * 60.0 * cosLat) / approxDistNm
      // Dik birim vektör: (-dirNmLon, dirNmLat)
      perpLat = (-dirNmLon * clearanceNm) / 60.0
      perpLon = (dirNmLat * clearanceNm) / (60.0 * cosLat)
    }

    for (s in 1 until steps) {
      val t = s.toDouble() / steps.toDouble()
      val sLat = lat1 + dLat * t
      val sLon = lon1 + dLon * t
      if (isPointInAnyCandidate(sLat, sLon, candidates)) {
        return true
      }
      if (useClearance && t > 0.06 && t < 0.94) {
        if (isPointInAnyCandidate(sLat + perpLat, sLon + perpLon, candidates) ||
            isPointInAnyCandidate(sLat - perpLat, sLon - perpLon, candidates)) {
          return true
        }
      }
    }

    return false
  }

  /**
   * İki deniz noktası arasında karasal engel (burun, yarımada, ada, koy) bulunduğunda
   * 2B Deniz Izgarası üzerinde Any-Angle Theta* algoritmasıyla %100 deniz üzerinden geçen en kısa yolu bulur.
   */
  fun findLocalCoastalSeaPath(
    startLat: Double,
    startLon: Double,
    goalLat: Double,
    goalLon: Double
  ): List<Coordinate>? {
    val cleanStart = snapToNearestSeaWater(startLat, startLon, clearanceNm = 0.05)
    val cleanGoal = snapToNearestSeaWater(goalLat, goalLon, clearanceNm = 0.05)

    if (!doesSegmentCrossLand(cleanStart.latitude, cleanStart.longitude, cleanGoal.latitude, cleanGoal.longitude, 0.05)) {
      return listOf(cleanStart, cleanGoal)
    }

    val spanLat = abs(cleanGoal.latitude - cleanStart.latitude)
    val spanLon = abs(cleanGoal.longitude - cleanStart.longitude)
    val maxSpan = max(spanLat, spanLon)

    // Kademeli arama kutusu: önce emniyetli açık su payıyla, dar geçitlerde kademeli toleransla ara
    val configs = arrayOf(
      Triple(max(0.11, maxSpan * 0.50), 62, 0.12),
      Triple(max(0.14, maxSpan * 0.60), 64, 0.06),
      Triple(max(0.32, maxSpan * 0.95), 72, 0.04)
    )

    for ((padDeg, gridRes, marginNm) in configs) {
      val result = runThetaStarSeaGrid(
        start = cleanStart,
        goal = cleanGoal,
        padDeg = padDeg,
        gridSize = gridRes,
        clearanceNm = marginNm
      )
      if (result != null && result.size >= 2) {
        return result
      }
    }
    return null
  }

  private fun runThetaStarSeaGrid(
    start: Coordinate,
    goal: Coordinate,
    padDeg: Double,
    gridSize: Int,
    clearanceNm: Double
  ): List<Coordinate>? {
    val minLat = min(start.latitude, goal.latitude) - padDeg
    val maxLat = max(start.latitude, goal.latitude) + padDeg
    val minLon = min(start.longitude, goal.longitude) - padDeg * 1.25
    val maxLon = max(start.longitude, goal.longitude) + padDeg * 1.25

    val rows = gridSize
    val cols = gridSize
    val stepLat = (maxLat - minLat) / (rows - 1)
    val stepLon = (maxLon - minLon) / (cols - 1)

    val totalCells = rows * cols
    val navigable = BooleanArray(totalCells)

    for (r in 0 until rows) {
      val cLat = minLat + r * stepLat
      for (c in 0 until cols) {
        val cLon = minLon + c * stepLon
        navigable[r * cols + c] = !isPointTooCloseToLand(cLat, cLon, clearanceNm)
      }
    }

    fun nearestNavigableCell(targetLat: Double, targetLon: Double): Int {
      val r0 = ((targetLat - minLat) / stepLat).toInt().coerceIn(0, rows - 1)
      val c0 = ((targetLon - minLon) / stepLon).toInt().coerceIn(0, cols - 1)
      if (navigable[r0 * cols + c0] &&
          !doesSegmentCrossLand(targetLat, targetLon, minLat + r0 * stepLat, minLon + c0 * stepLon, 0.0)) {
        return r0 * cols + c0
      }
      var bestIdx = -1
      var bestDist = Double.MAX_VALUE
      for (rad in 1..10) {
        val rMin = (r0 - rad).coerceAtLeast(0)
        val rMax = (r0 + rad).coerceAtMost(rows - 1)
        val cMin = (c0 - rad).coerceAtLeast(0)
        val cMax = (c0 + rad).coerceAtMost(cols - 1)
        for (r in rMin..rMax) {
          for (c in cMin..cMax) {
            val idx = r * cols + c
            if (!navigable[idx]) continue
            val cellLat = minLat + r * stepLat
            val cellLon = minLon + c * stepLon
            if (!doesSegmentCrossLand(targetLat, targetLon, cellLat, cellLon, 0.0)) {
              val d = LocationPresets.calculateDistanceNm(targetLat, targetLon, cellLat, cellLon)
              if (d < bestDist) {
                bestDist = d
                bestIdx = idx
              }
            }
          }
        }
        if (bestIdx != -1) return bestIdx
      }
      return -1
    }

    val startIdx = nearestNavigableCell(start.latitude, start.longitude)
    val goalIdx = nearestNavigableCell(goal.latitude, goal.longitude)
    if (startIdx == -1 || goalIdx == -1) return null

    val gScore = DoubleArray(totalCells) { Double.POSITIVE_INFINITY }
    val parent = IntArray(totalCells) { -1 }
    val closed = BooleanArray(totalCells)

    fun cellLat(idx: Int): Double = minLat + (idx / cols) * stepLat
    fun cellLon(idx: Int): Double = minLon + (idx % cols) * stepLon

    val goalCellLat = cellLat(goalIdx)
    val goalCellLon = cellLon(goalIdx)

    val openQueue = PriorityQueue<Pair<Int, Double>>(compareBy { it.second })
    gScore[startIdx] = 0.0
    parent[startIdx] = startIdx
    openQueue.add(
      Pair(startIdx, LocationPresets.calculateDistanceNm(cellLat(startIdx), cellLon(startIdx), goalCellLat, goalCellLon))
    )

    val dRows = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)
    val dCols = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)

    while (openQueue.isNotEmpty()) {
      val (u, _) = openQueue.poll() ?: break
      if (closed[u]) continue
      closed[u] = true
      if (u == goalIdx) break

      val uR = u / cols
      val uC = u % cols
      val uLat = minLat + uR * stepLat
      val uLon = minLon + uC * stepLon
      val pU = parent[u]
      val pULat = cellLat(pU)
      val pULon = cellLon(pU)

      for (dir in 0 until 8) {
        val vR = uR + dRows[dir]
        val vC = uC + dCols[dir]
        if (vR < 0 || vR >= rows || vC < 0 || vC >= cols) continue
        val v = vR * cols + vC
        if (closed[v] || !navigable[v]) continue

        val vLat = minLat + vR * stepLat
        val vLon = minLon + vC * stepLon

        // Komşu geçişinin karayı kesmediğini doğrula
        if (doesSegmentCrossLand(uLat, uLon, vLat, vLon, clearanceNm * 0.5)) continue

        // Any-Angle Theta*: Ebeveyn düğümden doğrudan engelsiz deniz hattı varsa düzleştir
        if (pU != u && !doesSegmentCrossLand(pULat, pULon, vLat, vLon, clearanceNm)) {
          val candG = gScore[pU] + LocationPresets.calculateDistanceNm(pULat, pULon, vLat, vLon)
          if (candG < gScore[v]) {
            gScore[v] = candG
            parent[v] = pU
            val h = LocationPresets.calculateDistanceNm(vLat, vLon, goalCellLat, goalCellLon)
            openQueue.add(Pair(v, candG + h))
          }
        } else {
          val candG = gScore[u] + LocationPresets.calculateDistanceNm(uLat, uLon, vLat, vLon)
          if (candG < gScore[v]) {
            gScore[v] = candG
            parent[v] = u
            val h = LocationPresets.calculateDistanceNm(vLat, vLon, goalCellLat, goalCellLon)
            openQueue.add(Pair(v, candG + h))
          }
        }
      }
    }

    if (!closed[goalIdx] && gScore[goalIdx].isInfinite()) return null

    val gridCoords = ArrayList<Coordinate>()
    var curr = goalIdx
    var safetyCounter = 0
    while (curr != -1 && safetyCounter < totalCells) {
      gridCoords.add(0, Coordinate(cellLat(curr), cellLon(curr)))
      if (curr == startIdx) break
      curr = parent[curr]
      safetyCounter++
    }
    if (gridCoords.isEmpty() || curr != startIdx) return null

    // Başlangıç ve varış gerçek deniz noktalarını uçlara ekle
    val fullPath = ArrayList<Coordinate>(gridCoords.size + 2)
    fullPath.add(start)
    for (pt in gridCoords) {
      if (LocationPresets.calculateDistanceNm(fullPath.last().latitude, fullPath.last().longitude, pt.latitude, pt.longitude) > 0.03) {
        fullPath.add(pt)
      }
    }
    if (LocationPresets.calculateDistanceNm(fullPath.last().latitude, fullPath.last().longitude, goal.latitude, goal.longitude) > 0.03) {
      fullPath.add(goal)
    } else {
      fullPath[fullPath.lastIndex] = goal
    }

    return fullPath
  }

  private fun isPointInAnyCandidate(lat: Double, lon: Double, candidates: List<LandPolygon>): Boolean {
    for (i in candidates.indices) {
      val poly = candidates[i]
      if (lat < poly.minLat || lat > poly.maxLat || lon < poly.minLon || lon > poly.maxLon) continue
      if (isPointInPolygon(lat, lon, poly.coords)) return true
    }
    return false
  }

  /**
   * Ray-Casting algoritması ile (lat, lon) noktasının poligon içinde olup olmadığını belirler.
   */
  private fun isPointInPolygon(lat: Double, lon: Double, coords: DoubleArray): Boolean {
    var inside = false
    val n = coords.size / 2
    var j = n - 1
    for (i in 0 until n) {
      val latI = coords[i * 2]
      val lonI = coords[i * 2 + 1]
      val latJ = coords[j * 2]
      val lonJ = coords[j * 2 + 1]

      val intersects = ((latI > lat) != (latJ > lat)) &&
        (lon < (lonJ - lonI) * (lat - latI) / (latJ - latI + 1e-15) + lonI)
      if (intersects) {
        inside = !inside
      }
      j = i
    }
    return inside
  }

  private fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Int {
    val cross = (by - ay) * (cx - bx) - (bx - ax) * (cy - by)
    if (abs(cross) < 1e-12) return 0
    return if (cross > 0.0) 1 else 2
  }

  private fun onSegment(ax: Double, ay: Double, px: Double, py: Double, bx: Double, by: Double): Boolean {
    return px <= max(ax, bx) + 1e-9 && px >= min(ax, bx) - 1e-9 &&
      py <= max(ay, by) + 1e-9 && py >= min(ay, by) - 1e-9
  }

  private fun segmentsIntersect(
    p1Lat: Double, p1Lon: Double,
    p2Lat: Double, p2Lon: Double,
    q1Lat: Double, q1Lon: Double,
    q2Lat: Double, q2Lon: Double
  ): Boolean {
    if (max(p1Lon, p2Lon) < min(q1Lon, q2Lon) || min(p1Lon, p2Lon) > max(q1Lon, q2Lon) ||
        max(p1Lat, p2Lat) < min(q1Lat, q2Lat) || min(p1Lat, p2Lat) > max(q1Lat, q2Lat)) {
      return false
    }

    val o1 = orientation(p1Lon, p1Lat, p2Lon, p2Lat, q1Lon, q1Lat)
    val o2 = orientation(p1Lon, p1Lat, p2Lon, p2Lat, q2Lon, q2Lat)
    val o3 = orientation(q1Lon, q1Lat, q2Lon, q2Lat, p1Lon, p1Lat)
    val o4 = orientation(q1Lon, q1Lat, q2Lon, q2Lat, p2Lon, p2Lat)

    if (o1 != o2 && o3 != o4) return true
    if (o1 == 0 && onSegment(p1Lon, p1Lat, q1Lon, q1Lat, p2Lon, p2Lat)) return true
    if (o2 == 0 && onSegment(p1Lon, p1Lat, q2Lon, q2Lat, p2Lon, p2Lat)) return true
    if (o3 == 0 && onSegment(q1Lon, q1Lat, p1Lon, p1Lat, q2Lon, q2Lat)) return true
    if (o4 == 0 && onSegment(q1Lon, q1Lat, p2Lon, p2Lat, q2Lon, q2Lat)) return true
    return false
  }
}
