package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClearCamRepository
import com.example.data.local.MediaRecordEntity
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DeviceCameraUiState(
  val isFrontCamera: Boolean = false,
  val isTorchEnabled: Boolean = false,
  val zoomLevel: Float = 1f,
  val isRecording: Boolean = false,
  val recordingElapsedMs: Long = 0L,
  val isCapturingPhoto: Boolean = false,
  val statusMessage: String? = null,
)

class DeviceCameraViewModel(private val repository: ClearCamRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(DeviceCameraUiState())
  val uiState: StateFlow<DeviceCameraUiState> = _uiState.asStateFlow()

  val latestMedia: StateFlow<MediaRecordEntity?> =
    repository.allMedia
      .map { it.firstOrNull() }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
      )

  private var timerJob: Job? = null

  fun toggleLensFacing() {
    if (_uiState.value.isRecording) return
    _uiState.update {
      it.copy(
        isFrontCamera = !it.isFrontCamera,
        isTorchEnabled = if (!it.isFrontCamera) false else it.isTorchEnabled,
      )
    }
  }

  fun toggleTorch() {
    if (_uiState.value.isFrontCamera) return
    _uiState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
  }

  fun setZoomLevel(zoom: Float) {
    _uiState.update { it.copy(zoomLevel = zoom.coerceIn(1f, 5f)) }
  }

  fun setCapturingPhoto(capturing: Boolean) {
    _uiState.update { it.copy(isCapturingPhoto = capturing) }
  }

  fun onRecordingStarted() {
    _uiState.update { it.copy(isRecording = true, recordingElapsedMs = 0L) }
    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        val start = System.currentTimeMillis()
        while (isActive) {
          _uiState.update { it.copy(recordingElapsedMs = System.currentTimeMillis() - start) }
          delay(250)
        }
      }
  }

  fun onRecordingStoppedOrError(message: String? = null) {
    timerJob?.cancel()
    timerJob = null
    _uiState.update {
      it.copy(isRecording = false, recordingElapsedMs = 0L, statusMessage = message ?: it.statusMessage)
    }
  }

  fun onPhotoSaved(uri: Uri) {
    viewModelScope.launch {
      runCatching { repository.indexCameraXPhoto(uri, "กล้องตัวเครื่อง") }
      _uiState.update {
        it.copy(isCapturingPhoto = false, statusMessage = "บันทึกภาพถ่ายลงในตัวเครื่องแล้ว")
      }
    }
  }

  fun onVideoFinalized(uri: Uri, durationMs: Long, privateFile: File? = null) {
    timerJob?.cancel()
    timerJob = null
    viewModelScope.launch {
      runCatching {
        repository.indexCameraXVideo(
          uri = uri,
          cameraName = "กล้องตัวเครื่อง",
          fallbackDurationMs = durationMs,
          privateFile = privateFile,
        )
      }
      _uiState.update {
        it.copy(
          isRecording = false,
          recordingElapsedMs = 0L,
          statusMessage = "บันทึกวิดีโอลงในตัวเครื่องแล้ว",
        )
      }
    }
  }

  fun showMessage(msg: String) {
    _uiState.update { it.copy(statusMessage = msg) }
  }

  fun clearMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }

  companion object {
    fun factory(repository: ClearCamRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return DeviceCameraViewModel(repository) as T
        }
      }
  }
}
