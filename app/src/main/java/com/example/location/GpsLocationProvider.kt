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

  /**
   * Sürekli Canlı GPS Konum Akışı (Continuous Location Updates Flow)
   */
  @SuppressLint("MissingPermission")
  fun startLocationUpdates(intervalMs: Long = 2000L): Flow<GpsFix> = callbackFlow {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
      .setMinUpdateIntervalMillis(1000L)
      .setMinUpdateDistanceMeters(0.5f)
      .setWaitForAccurateLocation(false)
      .build()

    val locationCallback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val location = result.lastLocation ?: return

        var computedSpeedKnots = if (location.hasSpeed() && location.speed >= 0f) {
          location.speed * 1.94384 // m/s to knots
        } else null

        // Eğer GPS donanımı anlık hız vermediyse önceki koordinat farkından (haversine) hesapla
        val prev = previousFix
        if (computedSpeedKnots == null && prev != null) {
          val timeDeltaSec = (location.time - prev.timestampEpochMs) / 1000.0
          if (timeDeltaSec in 0.5..30.0) {
            val distNm = haversineDistanceNm(prev.latitude, prev.longitude, location.latitude, location.longitude)
            computedSpeedKnots = (distNm / (timeDeltaSec / 3600.0)).coerceIn(0.0, 50.0)
          }
        }

        val fix = GpsFix(
          latitude = location.latitude,
          longitude = location.longitude,
          accuracyMeters = location.accuracy,
          speedKnots = computedSpeedKnots,
          bearingDegrees = if (location.hasBearing()) location.bearing else null,
          altitudeMeters = if (location.hasAltitude()) location.altitude else null,
          timestampEpochMs = location.time
        )
        previousFix = fix
        trySend(fix)
      }
    }

    try {
      fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    } catch (e: Exception) {
      close(e)
    }

    awaitClose {
      fusedClient.removeLocationUpdates(locationCallback)
    }
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentGpsFix(): Result<GpsFix> {
    return suspendCancellableCoroutine { continuation ->
      try {
        val priority = Priority.PRIORITY_HIGH_ACCURACY
        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

        continuation.invokeOnCancellation {
          cancellationTokenSource.cancel()
        }

        fusedClient.getCurrentLocation(priority, cancellationTokenSource.token)
          .addOnSuccessListener { location: Location? ->
            if (location != null) {
              val fix = GpsFix(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                speedKnots = if (location.hasSpeed()) location.speed * 1.94384 else null, // m/s to knots
                bearingDegrees = if (location.hasBearing()) location.bearing else null,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                timestampEpochMs = location.time
              )
              previousFix = fix
              continuation.resume(Result.success(fix))
            } else {
              getLastKnownLocationFallback { fallbackFix ->
                if (fallbackFix != null) {
                  previousFix = fallbackFix
                  continuation.resume(Result.success(fallbackFix))
                } else {
                  continuation.resume(Result.failure(Exception("GPS konumu alınamadı. Lütfen cihaz konum servislerinin açık olduğundan emin olun.")))
                }
              }
            }
          }
          .addOnFailureListener { error ->
            getLastKnownLocationFallback { fallbackFix ->
              if (fallbackFix != null) {
                previousFix = fallbackFix
                continuation.resume(Result.success(fallbackFix))
              } else {
                continuation.resume(Result.failure(error))
              }
            }
          }
      } catch (e: SecurityException) {
        continuation.resume(Result.failure(Exception("Konum izni verilmedi.")))
      } catch (e: Exception) {
        continuation.resume(Result.failure(e))
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

