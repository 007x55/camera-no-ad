package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CameraEntity
import com.example.data.local.ClearCamRepository
import com.example.media.TextureVideoRecorder
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class IpLiveUiState(
  val isMuted: Boolean = true,
  val isRecording: Boolean = false,
  val recordingElapsedMs: Long = 0L,
  val isSavingMedia: Boolean = false,
  val streamError: String? = null,
  val statusMessage: String? = null,
  val isBuffering: Boolean = true,
  val isConnected: Boolean = false,
)

class IpCameraLiveViewModel(
  private val cameraId: Long,
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

  private val _uiState = MutableStateFlow(IpLiveUiState())
  val uiState: StateFlow<IpLiveUiState> = _uiState.asStateFlow()

  private var recorder: TextureVideoRecorder? = null
  private var activeTempFile: File? = null
  private var timerJob: Job? = null

  fun setStreamStatus(buffering: Boolean, connected: Boolean, error: String? = null) {
    _uiState.update {
      it.copy(isBuffering = buffering, isConnected = connected, streamError = error)
    }
  }

  fun toggleMute() {
    _uiState.update { it.copy(isMuted = !it.isMuted) }
  }

  fun toggleFlipVertical() {
    val current = camera.value ?: return
    viewModelScope.launch {
      repository.updateCamera(current.copy(flipVertical = !current.flipVertical))
    }
  }

  fun toggleForceTcp() {
    val current = camera.value ?: return
    viewModelScope.launch { repository.updateCamera(current.copy(forceTcp = !current.forceTcp)) }
  }

  fun toggleEnabled() {
    val current = camera.value ?: return
    viewModelScope.launch { repository.updateCamera(current.copy(isEnabled = !current.isEnabled)) }
  }

  fun clearMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  fun savePhotoSnapshot(bitmap: Bitmap?) {
    if (bitmap == null || bitmap.isRecycled) {
      _uiState.update { it.copy(statusMessage = "ยังไม่มีภาพจากสตรีมให้บันทึก") }
      return
    }
    val camName = camera.value?.name ?: "IPCam"
    viewModelScope.launch {
      _uiState.update { it.copy(isSavingMedia = true) }
      runCatching { repository.saveBitmapPhoto(bitmap, camName) }
        .onSuccess {
          _uiState.update {
            it.copy(isSavingMedia = false, statusMessage = "บันทึกภาพถ่ายลงในตัวเครื่องแล้ว")
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isSavingMedia = false,
              statusMessage = "บันทึกภาพไม่สำเร็จ: ${err.localizedMessage ?: "ข้อผิดพลาด"}",
            )
          }
        }
      runCatching { bitmap.recycle() }
    }
  }

  fun startVideoRecording(rawWidth: Int, rawHeight: Int, frameProvider: () -> Bitmap?) {
    if (_uiState.value.isRecording) return
    val w = if (rawWidth > 0) rawWidth else 960
    val h = if (rawHeight > 0) rawHeight else 540
    val tempFile = repository.createTempMp4File()
    val videoRecorder = TextureVideoRecorder(outputFile = tempFile, rawWidth = w, rawHeight = h)
    runCatching { videoRecorder.start(viewModelScope, frameProvider) }
      .onSuccess {
        recorder = videoRecorder
        activeTempFile = tempFile
        _uiState.update { it.copy(isRecording = true, recordingElapsedMs = 0L) }
        timerJob?.cancel()
        timerJob =
          viewModelScope.launch {
            val started = System.currentTimeMillis()
            while (isActive) {
              _uiState.update { it.copy(recordingElapsedMs = System.currentTimeMillis() - started) }
              delay(250)
            }
          }
      }
      .onFailure { err ->
        runCatching { tempFile.delete() }
        _uiState.update {
          it.copy(
            isRecording = false,
            statusMessage = "ไม่สามารถเริ่มบันทึกวิดีโอได้: ${err.localizedMessage ?: "Codec error"}",
          )
        }
      }
  }

  fun stopVideoRecording() {
    val activeRecorder = recorder ?: return
    val tempFile = activeTempFile ?: return
    recorder = null
    activeTempFile = null
    timerJob?.cancel()
    timerJob = null
    val camName = camera.value?.name ?: "IPCam"

    viewModelScope.launch {
      _uiState.update { it.copy(isRecording = false, isSavingMedia = true) }
      val durationMs = runCatching { activeRecorder.stop() }.getOrDefault(0L)
      if (tempFile.exists() && tempFile.length() > 0L) {
        runCatching {
            repository.commitTempVideo(
              tempFile = tempFile,
              cameraName = camName,
              fallbackDurationMs = durationMs,
              width = activeRecorder.width,
              height = activeRecorder.height,
            )
          }
          .onSuccess {
            _uiState.update {
              it.copy(
                isSavingMedia = false,
                recordingElapsedMs = 0L,
                statusMessage = "บันทึกวิดีโอ MP4 ลงในตัวเครื่องแล้ว",
              )
            }
          }
          .onFailure { err ->
            _uiState.update {
              it.copy(
                isSavingMedia = false,
                recordingElapsedMs = 0L,
                statusMessage = "บันทึกวิดีโอไม่สำเร็จ: ${err.localizedMessage}",
              )
            }
          }
      } else {
        runCatching { tempFile.delete() }
        _uiState.update {
          it.copy(
            isSavingMedia = false,
            recordingElapsedMs = 0L,
            statusMessage = "ไม่พบเฟรมวิดีโอสำหรับการบันทึก",
          )
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
    val activeRecorder = recorder
    val tempFile = activeTempFile
    if (activeRecorder != null && tempFile != null) {
      recorder = null
      activeTempFile = null
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        runCatching { activeRecorder.stop() }
        runCatching { tempFile.delete() }
      }
    }
  }

  companion object {
    fun factory(cameraId: Long, repository: ClearCamRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return IpCameraLiveViewModel(cameraId, repository) as T
        }
      }
  }
}
