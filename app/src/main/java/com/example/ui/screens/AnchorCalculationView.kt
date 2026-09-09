package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.AnchorCalculationCard

@Composable
fun AnchorCalculationView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  onNavigateToTide: () -> Unit = {},
  onNavigateToMap: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("screen_anchor_calculation"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
  ) {
    // Ana Demirleme ve Salma Dairesi Hesaplama Kartı (Şema en altta yer alır)
    item {
      AnchorCalculationCard(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToMap = onNavigateToMap
      )
    }
  }
}

