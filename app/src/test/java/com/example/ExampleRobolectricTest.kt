package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Deniz Seyir & Gelgit", appName)
  }

  @Test
  fun `textbook example 1 printed in image solves correctly`() {
    // ÖRNEK : Gemimiz 40 metre derinliği olan mevkiye, 5 kilit zincir suda olacak şekilde demirliyor.
    // (1 kilit zincir 27,5 metre) Köprüüstü loça mesafesi 35 metre, köprüüstü kıç mesafesi 85 metre'dir.
    // a = 5 * 27.5 = 137.5 m
    // b = 40.0 m
    // c² = 137.5² - 40² = 18906.25 - 1600 = 17306.25 => c ≈ 131.55 m (kitapta 131.5)
    // I. Salma Dairesi = 131.55 + 35 = 166.55 m (kitapta 166.5)
    // Gemi Boyu = 35 + 85 = 120 m
    // II. Salma Dairesi = 120 + 131.55 = 251.55 m (kitapta 251.5)
    val params = com.example.model.AnchorCalculationParams(
      chainScopeMeters = 137.5,
      depthMeters = 40.0,
      isAutoCalculateHorizontal = true,
      distBridgeToHawseMeters = 35.0,
      distBridgeToSternMeters = 85.0,
      loaMeters = 120.0,
      safetyMarginMeters = 0.0
    )
    val result = com.example.engine.AnchorCalculationEngine.calculate(params)

    assertEquals(137.5, result.a_chainScopeMeters, 0.01)
    assertEquals(5.0, result.a_chainScopeShackles, 0.01)
    assertEquals(40.0, result.b_depthMeters, 0.01)
    assertEquals(131.55, result.c_horizontalDistanceMeters, 0.1)
    assertEquals(166.55, result.d_firstSwingingCircleMeters, 0.1)
    assertEquals(120.0, result.loaMeters, 0.01)
    assertEquals(251.55, result.f_secondSwingingCircleMeters, 0.1)
  }

  @Test
  fun `textbook example 2 handwritten in image solves correctly`() {
    // 50 metre derinlik, 3 kilit. K/Ü-loça = 40m, K/Ü-kıç = 115m.
    // a = 3 * 27.5 = 82.5 m
    // b = 50.0 m
    // c² = 82.5² - 50² = 6806.25 - 2500 = 4306.25 => c ≈ 65.62 m
    // I. Salma = 65.62 + 40 = 105.62 m
    // Gemi Boyu = 40 + 115 = 155 m
    // II. Salma = 155 + 65.62 = 220.62 m
    val params = com.example.model.AnchorCalculationParams(
      chainScopeMeters = 82.5,
      depthMeters = 50.0,
      isAutoCalculateHorizontal = true,
      distBridgeToHawseMeters = 40.0,
      distBridgeToSternMeters = 115.0,
      loaMeters = 155.0,
      safetyMarginMeters = 0.0
    )
    val result = com.example.engine.AnchorCalculationEngine.calculate(params)

    assertEquals(82.5, result.a_chainScopeMeters, 0.01)
    assertEquals(3.0, result.a_chainScopeShackles, 0.01)
    assertEquals(50.0, result.b_depthMeters, 0.01)
    assertEquals(65.62, result.c_horizontalDistanceMeters, 0.1)
    assertEquals(105.62, result.d_firstSwingingCircleMeters, 0.1)
    assertEquals(155.0, result.loaMeters, 0.01)
    assertEquals(220.62, result.f_secondSwingingCircleMeters, 0.1)
  }
}
