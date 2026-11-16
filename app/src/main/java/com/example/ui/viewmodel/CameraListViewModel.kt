package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CameraEntity
import com.example.data.local.ClearCamRepository
import com.example.data.local.StreamProtocol
import com.example.media.DiscoveredCameraHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LanScanState {
  data object Idle : LanScanState

  data class Scanning(val subnet: String?) : LanScanState

  data class Completed(val hosts: List<DiscoveredCameraHost>, val subnet: String?) : LanScanState
}

class CameraListViewModel(private val repository: ClearCamRepository) : ViewModel() {

  val cameras: StateFlow<List<CameraEntity>> =
    repository.allCameras.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _lanScanState = MutableStateFlow<LanScanState>(LanScanState.Idle)
  val lanScanState: StateFlow<LanScanState> = _lanScanState.asStateFlow()

  fun scanLan() {
    if (_lanScanState.value is LanScanState.Scanning) return
    viewModelScope.launch {
      val subnet = repository.detectSubnetPrefix()
      _lanScanState.value = LanScanState.Scanning(subnet)
      val found = repository.scanLanCameras()
      _lanScanState.value = LanScanState.Completed(found, subnet)
    }
  }

  fun resetScanState() {
    _lanScanState.value = LanScanState.Idle
  }

  fun addCamera(
    name: String,
    ipAddress: String,
    port: Int,
    streamPath: String,
    customFullUrl: String,
    protocol: StreamProtocol,
    username: String,
    password: String,
    deviceId: String,
    wifiSsid: String,
    macAddress: String,
    firmwareVersion: String,
    flipVertical: Boolean,
    forceTcp: Boolean,
    onSaved: (Long) -> Unit = {},
  ) {
    viewModelScope.launch {
      val id =
        repository.saveCamera(
          CameraEntity(
            name = name.trim().ifBlank { "กล้องใหม่" },
            deviceId = deviceId.trim(),
            ipAddress = ipAddress.trim(),
            port = port,
            streamPath = streamPath.trim(),
            customFullUrl = customFullUrl.trim(),
            protocol = protocol.name,
            username = username.trim(),
            password = password,
            wifiSsid = wifiSsid.trim(),
            macAddress = macAddress.trim(),
            firmwareVersion = firmwareVersion.trim(),
            isEnabled = true,
            flipVertical = flipVertical,
            forceTcp = forceTcp,
          )
        )
      onSaved(id)
    }
  }

  fun toggleCameraEnabled(camera: CameraEntity) {
    viewModelScope.launch { repository.updateCamera(camera.copy(isEnabled = !camera.isEnabled)) }
  }

  fun deleteCamera(cameraId: Long) {
    viewModelScope.launch { repository.deleteCamera(cameraId) }
  }

  companion object {
    fun factory(repository: ClearCamRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return CameraListViewModel(repository) as T
        }
      }
  }
}
