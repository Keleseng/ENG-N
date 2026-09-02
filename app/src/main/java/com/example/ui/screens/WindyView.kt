package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.WindyVerificationCard

@Composable
fun WindyView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val weather = uiState.marineWeather
  val isDarkMode = uiState.isDarkMode

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp)
  ) {
    WindyVerificationCard(
      weather = weather,
      onRefreshWeather = { viewModel.refreshWeather() },
      isDarkMode = isDarkMode,
      modifier = Modifier.fillMaxWidth()
    )
  }
}
