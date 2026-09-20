package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.example.model.MarineAttitude
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gemi Meyil (Inclinometer / Pitch & Roll) ve Pruva (Compass / HDG) Sağlayıcısı.
 *
 * Denizcilik Kuralları (Seamanship):
 * - Göstergeler kısa süreler ile ASLA sıfırlanmaz veya rastgele merkezlenmez.
 * - Donanım sensörleri (Jiroskop / İvmeölçer / Manyetik) durakladığında veya cihaz sabit durduğunda
 *   son kararlı ölçüm değerleri korunur.
 * - Düşük geçiren deniz sönümleme filtresi (Low-pass damping filter) ani sıçramaları ve titreşimleri eler.
 * - Donanım sensörü bulunmadığında (emülatör vb.) geminin doğal kıça trim ve sancak meyli etrafında
 *   uzun periyotlu (10-12 sn) nazik açık deniz dalga salınımı yapılır; asla her 1-2 saniyede sıfıra düşmez.
 */
class MarineAttitudeProvider(private val context: Context) {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

  // Hold (Dondur/Sabitle) ve Tare (Sıfırla/Kalibre Et) durumları
  @Volatile private var isHoldActive: Boolean = false
  @Volatile private var tarePitch: Float = 0f
  @Volatile private var tareRoll: Float = 0f

  // Kararlı iç durum değişkenleri (kısa sürelerle sıfırlanmaz)
  @Volatile private var smoothedAzimuth: Float = 270f
  @Volatile private var smoothedPitch: Float = -0.8f // Tipik kıça trim (-0.8°)
  @Volatile private var smoothedRoll: Float = 1.4f  // Tipik sancak meyil (+1.4°)
  @Volatile private var hasHardwareSensor: Boolean = false

  fun toggleHold(): Boolean {
    isHoldActive = !isHoldActive
    return isHoldActive
  }

  fun isHold(): Boolean = isHoldActive

  fun tare() {
    tarePitch += smoothedPitch
    tareRoll += smoothedRoll
  }

  fun resetTare() {
    tarePitch = 0f
    tareRoll = 0f
  }

  @Suppress("DEPRECATION")
  private fun getDisplayRotation(): Int {
    return try {
      val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    } catch (_: Exception) {
      Surface.ROTATION_0
    }
  }

  fun startAttitudeUpdates(
    getHeadingDegrees: () -> Float = { 270f },
    updateIntervalMs: Long = 120L
  ): Flow<MarineAttitude> = callbackFlow {
    var lastSensorEventTime = 0L

    val rotationMatrix = FloatArray(9)
    val orientationValues = FloatArray(3)

    var gravity: FloatArray? = null
    var geomagnetic: FloatArray? = null

    fun emitCurrentAttitude(isHardware: Boolean) {
      val displayPitch = smoothedPitch - tarePitch
      val displayRoll = smoothedRoll - tareRoll
      val pitchLabel = MarineAttitude.formatPitch(displayPitch)
      val rollLabel = MarineAttitude.formatRoll(displayRoll)
      val cardinal = MarineAttitude.calculateCardinal(smoothedAzimuth)
      val stability = MarineAttitude.determineStability(displayRoll, displayPitch)

      trySend(
        MarineAttitude(
          compassDegrees = smoothedAzimuth,
          compassCardinal = cardinal,
          pitchDegrees = displayPitch,
          rollDegrees = displayRoll,
          isSensorActive = isHardware,
          pitchLabel = pitchLabel,
          rollLabel = rollLabel,
          stabilityStatus = stability,
          isHoldActive = isHoldActive,
          isDampedFilterActive = true
        )
      )
    }

    fun processSensorAttitude(rawAzimuth: Float, rawPitch: Float, rawRoll: Float) {
      if (isHoldActive) return // Dondurulduğunda sensör verisiyle ezme

      // Denizcilik düşük geçiren sönümleme filtresi (Marine low-pass damping filter)
      // Ani sarsıntı, motor titreşimi ve ani sıfırlanmaları engeller.
      val alpha = 0.12f

      var diffAzimuth = rawAzimuth - smoothedAzimuth
      if (diffAzimuth > 180f) diffAzimuth -= 360f
      if (diffAzimuth < -180f) diffAzimuth += 360f
      smoothedAzimuth = (smoothedAzimuth + diffAzimuth * alpha + 360f) % 360f

      smoothedPitch += (rawPitch - smoothedPitch) * alpha
      smoothedRoll += (rawRoll - smoothedRoll) * alpha

      emitCurrentAttitude(isHardware = true)
    }

    fun remapAndProcessOrientation(sourceMatrix: FloatArray) {
      val remappedMatrix = FloatArray(9)
      when (getDisplayRotation()) {
        Surface.ROTATION_90 -> {
          SensorManager.remapCoordinateSystem(
            sourceMatrix,
            SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            remappedMatrix
          )
        }
        Surface.ROTATION_180 -> {
          SensorManager.remapCoordinateSystem(
            sourceMatrix,
            SensorManager.AXIS_MINUS_X,
            SensorManager.AXIS_MINUS_Y,
            remappedMatrix
          )
        }
        Surface.ROTATION_270 -> {
          SensorManager.remapCoordinateSystem(
            sourceMatrix,
            SensorManager.AXIS_MINUS_Y,
            SensorManager.AXIS_X,
            remappedMatrix
          )
        }
        else -> {
          System.arraycopy(sourceMatrix, 0, remappedMatrix, 0, 9)
        }
      }

      SensorManager.getOrientation(remappedMatrix, orientationValues)

      // Azimuth (yaw): orientationValues[0] (-PI..PI) -> 0..360
      var rawAzimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
      rawAzimuth = (rawAzimuth + 360f) % 360f

      // Pitch (Baş/Kıç): orientationValues[1] (-PI..PI)
      val rawPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()

      // Roll (Sancak/İskele): orientationValues[2] (-PI/2..PI/2)
      val rawRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

      processSensorAttitude(rawAzimuth, rawPitch, rawRoll)
    }

    fun computeFromAccelerometerOnly(grav: FloatArray) {
      val ax = grav[0]
      val ay = grav[1]
      val az = grav[2]

      val screenX: Float
      val screenY: Float
      when (getDisplayRotation()) {
        Surface.ROTATION_90 -> {
          screenX = -ay
          screenY = ax
        }
        Surface.ROTATION_180 -> {
          screenX = -ax
          screenY = -ay
        }
        Surface.ROTATION_270 -> {
          screenX = ay
          screenY = -ax
        }
        else -> {
          screenX = ax
          screenY = ay
        }
      }

      val pitchRad = atan2(-screenY.toDouble(), sqrt((screenX * screenX + az * az).toDouble())).toFloat()
      val rollRad = atan2(screenX.toDouble(), az.toDouble()).toFloat()

      val rawPitch = Math.toDegrees(pitchRad.toDouble()).toFloat()
      val rawRoll = Math.toDegrees(rollRad.toDouble()).toFloat()

      processSensorAttitude(smoothedAzimuth, rawPitch, rawRoll)
    }

    val sensorListener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        lastSensorEventTime = System.currentTimeMillis()
        hasHardwareSensor = true

        when (event.sensor.type) {
          Sensor.TYPE_ROTATION_VECTOR -> {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            remapAndProcessOrientation(rotationMatrix)
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
            remapAndProcessOrientation(rotationMatrix)
          }
        } else if (grav != null) {
          computeFromAccelerometerOnly(grav)
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Sensörleri kaydet
    var registered = false
    if (rotationVectorSensor != null) {
      sensorManager?.registerListener(sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
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

    // Kararlı Arka Plan Güncelleme Döngüsü
    val backgroundLoopJob = launch {
      var step = 0L
      while (isActive) {
        delay(updateIntervalMs)
        if (isHoldActive) {
          emitCurrentAttitude(isHardware = hasHardwareSensor)
          continue
        }

        if (hasHardwareSensor) {
          // Gerçek donanım sensörü aktifse:
          // Cihaz masa üstünde sabit dursa veya sensör aralıkları uzasa BİLE
          // ASLA SIFIRLANMAZ ve yapay dalgaya geçilmez!
          // Son ölçülen gerçek meyil ve pruva kararlı şekilde korunur.
          emitCurrentAttitude(isHardware = true)
        } else {
          // Donanım sensörü bulunmayan ortamlarda (emülatör / tarayıcı önizlemesi):
          // Kısa sürelerle (1-2 saniye) sıfırlanmayan, gerçekçi açık deniz uzun periyotlu (10-12 saniye)
          // gemi salınımı (swell) ve sabit pruva simülasyonu.
          step++
          val t = step * 0.05 // Daha yavaş, doğal deniz periyodu
          val targetHeading = getHeadingDegrees()

          // Pruva: Hedef rotaya doğru kararlı yumuşak sönümleme (asla sıfıra veya rastgele açılara sıfırlanmaz)
          var diffHdg = targetHeading - smoothedAzimuth
          if (diffHdg > 180f) diffHdg -= 360f
          if (diffHdg < -180f) diffHdg += 360f
          smoothedAzimuth = (smoothedAzimuth + diffHdg * 0.08f + (sin(t * 0.4) * 0.2).toFloat() + 360f) % 360f

          // Meyil (Pitch): Geminin doğal kıça trim durumu (örn. -0.8°) etrafında nazik salınım
          // Asla sürekli 0.0°'ye düşüp sıfırlanmaz!
          val baseTrim = -0.8f
          val wavePitch = (sin(t * 0.5) * 0.5 + sin(t * 0.2) * 0.2).toFloat()
          smoothedPitch = baseTrim + wavePitch

          // Yalpa (Roll): Geminin sancak/iskele meyli (örn. +1.4°) etrafında 10-12 sn periyotlu nazik deniz salınımı
          // Asla her 1-2 saniyede sıfırlanmaz!
          val baseList = 1.4f
          val waveRoll = (sin(t * 0.6 + 0.5) * 1.1 + sin(t * 0.18) * 0.4).toFloat()
          smoothedRoll = baseList + waveRoll

          emitCurrentAttitude(isHardware = false)
        }
      }
    }

    awaitClose {
      if (registered) {
        sensorManager?.unregisterListener(sensorListener)
      }
      backgroundLoopJob.cancel()
    }
  }
}
