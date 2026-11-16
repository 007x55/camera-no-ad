package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClearCamRepository
import com.example.data.local.MediaRecordEntity
import com.example.data.local.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryFilter(val label: String) {
  ALL("ทั้งหมด"),
  PHOTOS("ภาพถ่าย"),
  VIDEOS("วิดีโอ"),
}

data class GalleryUiState(
  val items: List<MediaRecordEntity> = emptyList(),
  val filter: GalleryFilter = GalleryFilter.ALL,
  val totalPhotos: Int = 0,
  val totalVideos: Int = 0,
  val totalBytes: Long = 0L,
)

class GalleryViewModel(private val repository: ClearCamRepository) : ViewModel() {

  private val filterFlow = MutableStateFlow(GalleryFilter.ALL)

  val uiState: StateFlow<GalleryUiState> =
    combine(repository.allMedia, filterFlow) { all, filter ->
        val filtered =
          when (filter) {
            GalleryFilter.ALL -> all
            GalleryFilter.PHOTOS -> all.filter { it.type() == MediaType.PHOTO }
            GalleryFilter.VIDEOS -> all.filter { it.type() == MediaType.VIDEO }
          }
        GalleryUiState(
          items = filtered,
          filter = filter,
          totalPhotos = all.count { it.type() == MediaType.PHOTO },
          totalVideos = all.count { it.type() == MediaType.VIDEO },
          totalBytes = all.sumOf { it.fileSizeBytes },
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState(),
      )

  fun setFilter(filter: GalleryFilter) {
    filterFlow.value = filter
  }

  fun observeMedia(mediaId: Long) = repository.observeMedia(mediaId)

  fun deleteMedia(record: MediaRecordEntity) {
    viewModelScope.launch { repository.deleteMedia(record) }
  }

  fun shareMedia(record: MediaRecordEntity) {
    repository.shareMedia(record)
  }

  companion object {
    fun factory(repository: ClearCamRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return GalleryViewModel(repository) as T
        }
      }
  }
}
