package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(private val prefsRepo: UserPreferencesRepository) : ViewModel() {

  val preferences: StateFlow<AppPreferences> =
    prefsRepo.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppPreferences(),
    )

  fun setDarkTheme(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setDarkTheme(enabled) }
  }

  fun setDefaultForceTcp(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setDefaultForceTcp(enabled) }
  }

  fun setShowHudOverlay(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setShowHudOverlay(enabled) }
  }

  companion object {
    fun factory(prefsRepo: UserPreferencesRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return AppSettingsViewModel(prefsRepo) as T
        }
      }
  }
}
