package com.example.data.local

import android.graphics.Bitmap
import android.net.Uri
import com.example.media.DiscoveredCameraHost
import com.example.media.LanCameraScanner
import com.example.media.LocalMediaStorageManager
import java.io.File
import kotlinx.coroutines.flow.Flow

class ClearCamRepository(
  private val cameraDao: CameraDao,
  private val mediaRecordDao: MediaRecordDao,
  private val storageManager: LocalMediaStorageManager,
  private val lanScanner: LanCameraScanner,
  val preferencesRepository: UserPreferencesRepository,
) {
  val allCameras: Flow<List<CameraEntity>> = cameraDao.observeAllCameras()
  val allMedia: Flow<List<MediaRecordEntity>> = mediaRecordDao.observeAllMedia()

  fun observeCamera(cameraId: Long): Flow<CameraEntity?> = cameraDao.observeCameraById(cameraId)

  suspend fun getCamera(cameraId: Long): CameraEntity? = cameraDao.getCameraById(cameraId)

  suspend fun saveCamera(camera: CameraEntity): Long = cameraDao.insertCamera(camera)

  suspend fun updateCamera(camera: CameraEntity) = cameraDao.updateCamera(camera)

  suspend fun deleteCamera(cameraId: Long) = cameraDao.deleteCameraById(cameraId)

  fun observeMedia(mediaId: Long): Flow<MediaRecordEntity?> =
    mediaRecordDao.observeMediaById(mediaId)

  suspend fun saveBitmapPhoto(bitmap: Bitmap, cameraName: String): MediaRecordEntity {
    val record = storageManager.saveBitmapPhoto(bitmap, cameraName)
    val id = mediaRecordDao.insertMedia(record)
    return record.copy(id = id)
  }

  suspend fun commitTempVideo(
    tempFile: File,
    cameraName: String,
    fallbackDurationMs: Long,
    width: Int,
    height: Int,
  ): MediaRecordEntity {
    val record =
      storageManager.commitTempVideoToStorage(
        tempMp4File = tempFile,
        cameraName = cameraName,
        fallbackDurationMs = fallbackDurationMs,
        width = width,
        height = height,
      )
    val id = mediaRecordDao.insertMedia(record)
    return record.copy(id = id)
  }

  suspend fun indexCameraXPhoto(uri: Uri, cameraName: String): MediaRecordEntity {
    val record = storageManager.indexExistingImageUri(uri, cameraName)
    val id = mediaRecordDao.insertMedia(record)
    return record.copy(id = id)
  }

  suspend fun indexCameraXVideo(
    uri: Uri,
    cameraName: String,
    fallbackDurationMs: Long,
    privateFile: File? = null,
  ): MediaRecordEntity {
    val record =
      storageManager.indexExistingVideoUri(uri, cameraName, fallbackDurationMs, privateFile)
    val id = mediaRecordDao.insertMedia(record)
    return record.copy(id = id)
  }

  suspend fun deleteMedia(record: MediaRecordEntity) {
    storageManager.deleteMediaFiles(record)
    mediaRecordDao.deleteMediaById(record.id)
  }

  fun shareMedia(record: MediaRecordEntity) {
    storageManager.shareMedia(record)
  }

  fun detectSubnetPrefix(): String? = lanScanner.detectLocalSubnetPrefix()

  suspend fun scanLanCameras(): List<DiscoveredCameraHost> = lanScanner.scanSubnet()

  fun createTempMp4File(): File = storageManager.createTempMp4File()

  fun createVideoMediaStoreValues(cameraName: String) =
    storageManager.createVideoMediaStoreValues(cameraName)

  fun createPrivateVideoFile(cameraName: String): File =
    storageManager.createPrivateVideoFile(cameraName)
}
