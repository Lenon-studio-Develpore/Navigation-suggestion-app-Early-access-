package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ui.DetailScreen
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TravelUiState
import com.example.viewmodel.TravelViewModel
import com.example.worker.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    setupPeriodicSync()
    
    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TravelApp()
        }
      }
    }
  }
  
  private fun setupPeriodicSync() {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      // Enqueue sync every 5 hours
      val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(5, TimeUnit.HOURS)
          .setConstraints(constraints)
          .build()

      WorkManager.getInstance(this).enqueueUniquePeriodicWork(
          "TravelDataSync",
          ExistingPeriodicWorkPolicy.KEEP,
          syncRequest
      )
  }
}

@Composable
fun TravelApp(viewModel: TravelViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { location ->
                    navController.navigate("detail/${location.id}")
                }
            )
        }
        composable("detail/{locationId}") { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId")
            val currentState = viewModel.uiState.value
            if (currentState is TravelUiState.Success) {
                val location = currentState.locations.find { it.id.toString() == locationId }
                if (location != null) {
                    DetailScreen(
                        location = location,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

