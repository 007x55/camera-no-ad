package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
  PHOTO,
  VIDEO,
}

@Entity(tableName = "media_records")
data class MediaRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val cameraName: String,
  val mediaType: String = MediaType.PHOTO.name,
  val uriString: String,
  val filePath: String = "",
  val thumbnailPath: String = "",
  val fileSizeBytes: Long = 0L,
  val durationMs: Long = 0L,
  val width: Int = 0,
  val height: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
) {
  fun type(): MediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.PHOTO)
}
