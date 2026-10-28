package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.*

data class GpsFix(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val speedKnots: Double?, // knots
  val bearingDegrees: Float?,
  val altitudeMeters: Double?,
  val timestampEpochMs: Long
)

class GpsLocationProvider(private val context: Context) {

  private val fusedClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  private var previousFix: GpsFix? = null

  private fun buildFixFromLocation(location: Location): GpsFix {
    var computedSpeedKnots = if (location.hasSpeed() && location.speed >= 0f) {
      location.speed * 1.94384
    } else null

    val prev = previousFix
    if (computedSpeedKnots == null && prev != null) {
      val timeDeltaSec = (location.time - prev.timestampEpochMs) / 1000.0
      if (timeDeltaSec in 0.5..30.0) {
        val distNm = haversineDistanceNm(prev.latitude, prev.longitude, location.latitude, location.longitude)
        computedSpeedKnots = (distNm / (timeDeltaSec / 3600.0)).coerceIn(0.0, 50.0)
      }
    }

    var computedBearing = if (location.hasBearing()) location.bearing else null
    if (computedBearing == null && prev != null) {
      val distNm = haversineDistanceNm(prev.latitude, prev.longitude, location.latitude, location.longitude)
      if (distNm >= 0.004) {
        val phi1 = Math.toRadians(prev.latitude)
        val phi2 = Math.toRadians(location.latitude)
        val dLon = Math.toRadians(location.longitude - prev.longitude)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        computedBearing = (((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0)).toFloat()
      }
    }

    val fix = GpsFix(
      latitude = location.latitude,
      longitude = location.longitude,
      accuracyMeters = location.accuracy,
      speedKnots = computedSpeedKnots,
      bearingDegrees = computedBearing,
      altitudeMeters = if (location.hasAltitude()) location.altitude else null,
      timestampEpochMs = location.time
    )
    previousFix = fix
    return fix
  }

  /**
   * Sürekli Canlı GPS Konum Akışı (Hem Fused hem Doğrudan Uydu GNSS - %100 İnternetsiz Çalışır)
   */
  @SuppressLint("MissingPermission")
  fun startLocationUpdates(intervalMs: Long = 1500L): Flow<GpsFix> = callbackFlow {
    var lastEmittedMs = 0L

    fun emitDeduplicated(location: Location) {
      val now = System.currentTimeMillis()
      if (now - lastEmittedMs < 350L) return
      lastEmittedMs = now
      trySend(buildFixFromLocation(location))
    }

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
      .setMinUpdateIntervalMillis(800L)
      .setMinUpdateDistanceMeters(0.5f)
      .setWaitForAccurateLocation(false)
      .build()

    val locationCallback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val location = result.lastLocation ?: return
        emitDeduplicated(location)
      }
    }

    val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val rawGnssListener = android.location.LocationListener { location ->
      emitDeduplicated(location)
    }

    try {
      fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    } catch (_: Exception) {}

    try {
      if (locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
        locManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          1000L,
          0.5f,
          rawGnssListener,
          Looper.getMainLooper()
        )
      }
    } catch (_: Exception) {}

    awaitClose {
      try { fusedClient.removeLocationUpdates(locationCallback) } catch (_: Exception) {}
      try { locManager?.removeUpdates(rawGnssListener) } catch (_: Exception) {}
    }
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentGpsFix(): Result<GpsFix> {
    return suspendCancellableCoroutine { continuation ->
      val completed = java.util.concurrent.atomic.AtomicBoolean(false)
      val mainHandler = android.os.Handler(Looper.getMainLooper())

      fun finishOnce(res: Result<GpsFix>) {
        if (completed.compareAndSet(false, true)) {
          continuation.resume(res)
        }
      }

      try {
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var oneShotListener: android.location.LocationListener? = null

        val timeoutRunnable = Runnable {
          try { oneShotListener?.let { locManager?.removeUpdates(it) } } catch (_: Exception) {}
          getLastKnownLocationFallback { fallbackFix ->
            if (fallbackFix != null) {
              previousFix = fallbackFix
              finishOnce(Result.success(fallbackFix))
            } else {
              finishOnce(Result.failure(Exception("Uydu GPS sinyali bekleniyor. Açık alanda konum servislerinin açık olduğundan emin olun.")))
            }
          }
        }
        mainHandler.postDelayed(timeoutRunnable, 3800L)

        // Doğrudan donanımsal Uydu GPS dinleyicisi (İnternetsiz ortamda anında yanıt verir)
        if (locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
          oneShotListener = android.location.LocationListener { loc ->
            mainHandler.removeCallbacks(timeoutRunnable)
            try { oneShotListener?.let { locManager.removeUpdates(it) } } catch (_: Exception) {}
            finishOnce(Result.success(buildFixFromLocation(loc)))
          }
          try {
            locManager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER,
              500L,
              0f,
              oneShotListener,
              Looper.getMainLooper()
            )
          } catch (_: Exception) {}
        }

        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        continuation.invokeOnCancellation {
          mainHandler.removeCallbacks(timeoutRunnable)
          try { oneShotListener?.let { locManager?.removeUpdates(it) } } catch (_: Exception) {}
          cancellationTokenSource.cancel()
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
          .addOnSuccessListener { location: Location? ->
            mainHandler.removeCallbacks(timeoutRunnable)
            try { oneShotListener?.let { locManager?.removeUpdates(it) } } catch (_: Exception) {}
            if (location != null) {
              finishOnce(Result.success(buildFixFromLocation(location)))
            } else {
              getLastKnownLocationFallback { fallbackFix ->
                if (fallbackFix != null) {
                  previousFix = fallbackFix
                  finishOnce(Result.success(fallbackFix))
                } else {
                  finishOnce(Result.failure(Exception("GPS konumu alınamadı. Lütfen cihaz konum servislerinin açık olduğundan emin olun.")))
                }
              }
            }
          }
          .addOnFailureListener { error ->
            mainHandler.removeCallbacks(timeoutRunnable)
            try { oneShotListener?.let { locManager?.removeUpdates(it) } } catch (_: Exception) {}
            getLastKnownLocationFallback { fallbackFix ->
              if (fallbackFix != null) {
                previousFix = fallbackFix
                finishOnce(Result.success(fallbackFix))
              } else {
                finishOnce(Result.failure(error))
              }
            }
          }
      } catch (e: SecurityException) {
        finishOnce(Result.failure(Exception("Konum izni verilmedi.")))
      } catch (e: Exception) {
        finishOnce(Result.failure(e))
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun getLastKnownLocationFallback(onResult: (GpsFix?) -> Unit) {
    try {
      fusedClient.lastLocation
        .addOnSuccessListener { loc: Location? ->
          if (loc != null) {
            onResult(
              GpsFix(
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = loc.accuracy,
                speedKnots = if (loc.hasSpeed()) loc.speed * 1.94384 else null,
                bearingDegrees = if (loc.hasBearing()) loc.bearing else null,
                altitudeMeters = if (loc.hasAltitude()) loc.altitude else null,
                timestampEpochMs = loc.time
              )
            )
          } else {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val sysLoc = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
              ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (sysLoc != null) {
              onResult(
                GpsFix(
                  latitude = sysLoc.latitude,
                  longitude = sysLoc.longitude,
                  accuracyMeters = sysLoc.accuracy,
                  speedKnots = if (sysLoc.hasSpeed()) sysLoc.speed * 1.94384 else null,
                  bearingDegrees = if (sysLoc.hasBearing()) sysLoc.bearing else null,
                  altitudeMeters = if (sysLoc.hasAltitude()) sysLoc.altitude else null,
                  timestampEpochMs = sysLoc.time
                )
              )
            } else {
              onResult(null)
            }
          }
        }
        .addOnFailureListener {
          onResult(null)
        }
    } catch (e: Exception) {
      onResult(null)
    }
  }

  private fun haversineDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 3440.065 // Dünya yarıçapı deniz mili (NM)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
  }
}

