package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.location.GpsLocationProvider
import com.example.ui.TideNavViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: TideNavViewModel by viewModels()
  private lateinit var gpsProvider: GpsLocationProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
      if (!webViewCacheDir.exists()) webViewCacheDir.mkdirs()
      val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
      if (!wasmCacheDir.exists()) wasmCacheDir.mkdirs()
    } catch (e: Exception) {
      // Ignored
    }
    gpsProvider = GpsLocationProvider(this)
    enableEdgeToEdge()

    setContent {
      val uiState by viewModel.uiState.collectAsState()
      MyApplicationTheme(darkTheme = uiState.isDarkMode) {
        var showSplash by rememberSaveable { mutableStateOf(true) }

        // Otomatik Konum İzni İsteme & Sürekli Güncelleme Başlatma
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
          val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
          val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

          if (fineGranted || coarseGranted) {
            viewModel.startContinuousLocationUpdates(gpsProvider)
          } else {
            viewModel.setGpsError("Canlı konum takibi için konum izni gereklidir.")
          }
        }

        LaunchedEffect(Unit) {
          val hasFine = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
          ) == PackageManager.PERMISSION_GRANTED

          val hasCoarse = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
          ) == PackageManager.PERMISSION_GRANTED

          if (hasFine || hasCoarse) {
            viewModel.startContinuousLocationUpdates(gpsProvider)
          } else {
            permissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          }
        }

        if (showSplash) {
          SplashScreen(
            onSplashFinished = { showSplash = false },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          MainScreen(
            viewModel = viewModel,
            onRequestPermission = {
              permissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                )
              )
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel.stopContinuousLocationUpdates()
  }
}
