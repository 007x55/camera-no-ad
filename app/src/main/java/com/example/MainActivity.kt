package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.ClearCamNavRoot
import com.example.ui.theme.ClearCamTheme
import com.example.ui.viewmodel.AppSettingsViewModel

class MainActivity : ComponentActivity() {
  private val appContainer by lazy { ClearCamAppContainer(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settingsVm: AppSettingsViewModel =
        viewModel(factory = AppSettingsViewModel.factory(appContainer.preferencesRepository))
      val prefs by settingsVm.preferences.collectAsStateWithLifecycle()

      ClearCamTheme(darkTheme = prefs.isDarkTheme) {
        ClearCamNavRoot(
          appContainer = appContainer,
          preferences = prefs,
          appSettingsViewModel = settingsVm,
        )
      }
    }
  }
}
