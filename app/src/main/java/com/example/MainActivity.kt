package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.WorkScheduler

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Initialize background WorkManager engines automatically on launch
    WorkScheduler.scheduleAll(applicationContext)

    // 2. Initialize simple constructor-injected Viewmodel with custom Factory
    val app = application as ProductivityApplication
    val mainViewModel: DashboardViewModel by viewModels {
      DashboardViewModel.Factory(app)
    }

    setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = mainViewModel)
      }
    }
  }
}

