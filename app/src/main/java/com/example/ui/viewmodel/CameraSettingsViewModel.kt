package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CameraEntity
import com.example.data.local.ClearCamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraSettingsViewModel(
  cameraId: Long,
  private val repository: ClearCamRepository,
) : ViewModel() {

  val camera: StateFlow<CameraEntity?> =
    repository
      .observeCamera(cameraId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
      )

  fun updateCamera(updated: CameraEntity, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      repository.updateCamera(updated)
      onDone()
    }
  }

  fun deleteCamera(cameraId: Long, onDeleted: () -> Unit) {
    viewModelScope.launch {
      repository.deleteCamera(cameraId)
      onDeleted()
    }
  }

  companion object {
    fun factory(cameraId: Long, repository: ClearCamRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return CameraSettingsViewModel(cameraId, repository) as T
        }
      }
  }
}
