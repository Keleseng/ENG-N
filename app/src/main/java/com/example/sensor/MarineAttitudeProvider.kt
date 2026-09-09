package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.MarineAttitude
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

class MarineAttitudeProvider(private val context: Context) {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

  fun startAttitudeUpdates(
    baseHeadingDegrees: Float = 270f,
    updateIntervalMs: Long = 100L
  ): Flow<MarineAttitude> = callbackFlow {
    var lastSensorEventTime = 0L
    var smoothedAzimuth = baseHeadingDegrees
    var smoothedPitch = 0.6f
    var smoothedRoll = 1.8f
    var hasHardwareSensor = false

    val rotationMatrix = FloatArray(9)
    val orientationValues = FloatArray(3)

    var gravity: FloatArray? = null
    var geomagnetic: FloatArray? = null

    val sensorListener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        lastSensorEventTime = System.currentTimeMillis()
        hasHardwareSensor = true

        when (event.sensor.type) {
          Sensor.TYPE_ROTATION_VECTOR -> {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            processOrientation(orientationValues)
          }
          Sensor.TYPE_ACCELEROMETER -> {
            gravity = event.values.clone()
            checkAndCompute()
          }
          Sensor.TYPE_MAGNETIC_FIELD -> {
            geomagnetic = event.values.clone()
            checkAndCompute()
          }
        }
      }

      private fun checkAndCompute() {
        val grav = gravity
        val geo = geomagnetic
        if (grav != null && geo != null) {
          if (SensorManager.getRotationMatrix(rotationMatrix, null, grav, geo)) {
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            processOrientation(orientationValues)
          }
        }
      }

      private fun processOrientation(orientation: FloatArray) {
        // Azimuth (yaw): orientation[0] (-PI to PI) -> degrees 0..360
        var rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        rawAzimuth = (rawAzimuth + 360f) % 360f

        // Pitch: orientation[1] (-PI to PI) -> top tilted down = positive, up = negative
        val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

        // Roll: orientation[2] (-PI/2 to PI/2) -> right tilt = positive (Sancak), left tilt = negative (İskele)
        val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // Exponential smoothing (low-pass)
        val alpha = 0.22f
        // Angular difference for azimuth to handle 0/360 wrap
        var diffAzimuth = rawAzimuth - smoothedAzimuth
        if (diffAzimuth > 180f) diffAzimuth -= 360f
        if (diffAzimuth < -180f) diffAzimuth += 360f
        smoothedAzimuth = (smoothedAzimuth + diffAzimuth * alpha + 360f) % 360f

        smoothedPitch += (rawPitch - smoothedPitch) * alpha
        smoothedRoll += (rawRoll - smoothedRoll) * alpha

        val pitchLabel = MarineAttitude.formatPitch(smoothedPitch)
        val rollLabel = MarineAttitude.formatRoll(smoothedRoll)
        val cardinal = MarineAttitude.calculateCardinal(smoothedAzimuth)
        val stability = MarineAttitude.determineStability(smoothedRoll, smoothedPitch)

        trySend(
          MarineAttitude(
            compassDegrees = smoothedAzimuth,
            compassCardinal = cardinal,
            pitchDegrees = smoothedPitch,
            rollDegrees = smoothedRoll,
            isSensorActive = true,
            pitchLabel = pitchLabel,
            rollLabel = rollLabel,
            stabilityStatus = stability
          )
        )
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Register hardware sensors if available
    var registered = false
    if (rotationVectorSensor != null) {
      sensorManager?.registerListener(
        sensorListener,
        rotationVectorSensor,
        SensorManager.SENSOR_DELAY_UI
      )
      registered = true
    } else {
      if (accelerometerSensor != null) {
        sensorManager?.registerListener(sensorListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        registered = true
      }
      if (magnetometerSensor != null) {
        sensorManager?.registerListener(sensorListener, magnetometerSensor, SensorManager.SENSOR_DELAY_UI)
        registered = true
      }
    }

    // Fallback animation loop: If hardware sensors are not available or not emitting (e.g., emulator)
    val fallbackJob = launch {
      var step = 0L
      while (isActive) {
        delay(updateIntervalMs)
        val now = System.currentTimeMillis()
        // If no hardware event received within last 1200ms, simulate natural ship motion on waves
        if (now - lastSensorEventTime > 1200L) {
          step++
          val t = step * 0.1
          // Realistic marine pitch & roll harmonic swell
          val simPitch = (sin(t * 0.7) * 1.4 + sin(t * 0.2) * 0.4).toFloat()
          val simRoll = (sin(t * 0.5 + 0.8) * 2.8 + sin(t * 0.15) * 0.8).toFloat()
          val simCompass = (baseHeadingDegrees + sin(t * 0.1) * 1.5).toFloat()
          val normalizedCompass = (simCompass + 360f) % 360f

          val pitchLabel = MarineAttitude.formatPitch(simPitch)
          val rollLabel = MarineAttitude.formatRoll(simRoll)
          val cardinal = MarineAttitude.calculateCardinal(normalizedCompass)
          val stability = MarineAttitude.determineStability(simRoll, simPitch)

          trySend(
            MarineAttitude(
              compassDegrees = normalizedCompass,
              compassCardinal = cardinal,
              pitchDegrees = simPitch,
              rollDegrees = simRoll,
              isSensorActive = false,
              pitchLabel = pitchLabel,
              rollLabel = rollLabel,
              stabilityStatus = stability
            )
          )
        }
      }
    }

    awaitClose {
      if (registered) {
        sensorManager?.unregisterListener(sensorListener)
      }
      fallbackJob.cancel()
    }
  }
}
