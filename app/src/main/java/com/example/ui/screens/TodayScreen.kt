package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab

@Composable
fun TodayScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  PeeweeBlockDashboardScreen(
    onOpenSettings = { viewModel.setTab(MainTab.SETTINGS) },
    onOpenHealthLog = { viewModel.setTab(MainTab.PROTECTION_HEALTH) },
    modifier = modifier
  )
}
