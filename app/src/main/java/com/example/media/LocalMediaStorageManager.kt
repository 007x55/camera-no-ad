package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.MediaRecordEntity
import com.example.data.local.MediaType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMediaStorageManager(private val context: Context) {

  private val thumbsDir: File by lazy {
    File(context.filesDir, "thumbs").apply { if (!exists()) mkdirs() }
  }

  private val privateMediaDir: File by lazy {
    File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
  }

  private val tempRecordingsDir: File by lazy {
    File(context.cacheDir, "recordings").apply { if (!exists()) mkdirs() }
  }

  fun createTempMp4File(): File {
    val stamp = timestampLabel()
    return File(tempRecordingsDir, "TMP_$stamp.mp4")
  }

  fun createPrivateVideoFile(cameraName: String): File {
    val stamp = timestampLabel()
    val safeCam = sanitizeName(cameraName)
    return File(privateMediaDir, "VID_${safeCam}_$stamp.mp4")
  }

  fun createVideoMediaStoreValues(cameraName: String): ContentValues {
    val stamp = timestampLabel()
    val safeCam = sanitizeName(cameraName)
    val fileName = "ClearCam_${safeCam}_$stamp.mp4"
    return ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClearCam")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }
  }

  suspend fun saveBitmapPhoto(bitmap: Bitmap, cameraName: String): MediaRecordEntity =
    withContext(Dispatchers.IO) {
      val stamp = timestampLabel()
      val safeCam = sanitizeName(cameraName)
      val fileName = "ClearCam_${safeCam}_$stamp.jpg"
      val thumbFile = File(thumbsDir, "THUMB_${stamp}_${System.nanoTime()}.jpg")
      saveThumbnail(bitmap, thumbFile)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values =
          ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
              MediaStore.Images.Media.RELATIVE_PATH,
              "${Environment.DIRECTORY_PICTURES}/ClearCam",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }
        val resolver = context.contentResolver
        val uri =
          resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("ไม่สามารถสร้างไฟล์ภาพใน MediaStore ได้")
        try {
          resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
          } ?: throw IllegalStateException("ไม่สามารถเปิดสตรีมบันทึกภาพได้")
          values.clear()
          values.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
          runCatching { resolver.delete(uri, null, null) }
          throw t
        }
        val size = queryUriSize(uri)
        MediaRecordEntity(
          title = fileName,
          cameraName = cameraName,
          mediaType = MediaType.PHOTO.name,
          uriString = uri.toString(),
          filePath = "Pictures/ClearCam/$fileName",
          thumbnailPath = thumbFile.absolutePath,
          fileSizeBytes = size,
          width = bitmap.width,
          height = bitmap.height,
        )
      } else {
        val outFile = File(privateMediaDir, fileName)
        FileOutputStream(outFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
        }
        val uri =
          FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile,
          )
        MediaRecordEntity(
          title = fileName,
          cameraName = cameraName,
          mediaType = MediaType.PHOTO.name,
          uriString = uri.toString(),
          filePath = outFile.absolutePath,
          thumbnailPath = thumbFile.absolutePath,
          fileSizeBytes = outFile.length(),
          width = bitmap.width,
          height = bitmap.height,
        )
      }
    }

  suspend fun commitTempVideoToStorage(
    tempMp4File: File,
    cameraName: String,
    fallbackDurationMs: Long,
    width: Int,
    height: Int,
  ): MediaRecordEntity =
    withContext(Dispatchers.IO) {
      val stamp = timestampLabel()
      val safeCam = sanitizeName(cameraName)
      val fileName = "ClearCam_${safeCam}_$stamp.mp4"
      val thumbFile = File(thumbsDir, "THUMB_${stamp}_${System.nanoTime()}.jpg")
      val extractedDuration = extractVideoThumbnailAndDuration(tempMp4File, thumbFile)
      val finalDuration = if (extractedDuration > 0L) extractedDuration else fallbackDurationMs

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = createVideoMediaStoreValues(cameraName).apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        }
        val resolver = context.contentResolver
        val uri =
          resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("ไม่สามารถสร้างไฟล์วิดีโอใน MediaStore ได้")
        try {
          resolver.openOutputStream(uri)?.use { out ->
            tempMp4File.inputStream().use { input -> input.copyTo(out) }
          } ?: throw IllegalStateException("ไม่สามารถเขียนไฟล์วิดีโอได้")
          values.clear()
          values.put(MediaStore.Video.Media.IS_PENDING, 0)
          resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
          runCatching { resolver.delete(uri, null, null) }
          throw t
        }
        val size = tempMp4File.length()
        runCatching { tempMp4File.delete() }
        MediaRecordEntity(
          title = fileName,
          cameraName = cameraName,
          mediaType = MediaType.VIDEO.name,
          uriString = uri.toString(),
          filePath = "Movies/ClearCam/$fileName",
          thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
          fileSizeBytes = size,
          durationMs = finalDuration,
          width = width,
          height = height,
        )
      } else {
        val targetFile = File(privateMediaDir, fileName)
        tempMp4File.copyTo(targetFile, overwrite = true)
        runCatching { tempMp4File.delete() }
        val uri =
          FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile,
          )
        MediaRecordEntity(
          title = fileName,
          cameraName = cameraName,
          mediaType = MediaType.VIDEO.name,
          uriString = uri.toString(),
          filePath = targetFile.absolutePath,
          thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
          fileSizeBytes = targetFile.length(),
          durationMs = finalDuration,
          width = width,
          height = height,
        )
      }
    }

  suspend fun indexExistingImageUri(uri: Uri, cameraName: String): MediaRecordEntity =
    withContext(Dispatchers.IO) {
      val stamp = timestampLabel()
      val fileName = "ClearCam_${sanitizeName(cameraName)}_$stamp.jpg"
      val thumbFile = File(thumbsDir, "THUMB_${stamp}_${System.nanoTime()}.jpg")
      var w = 0
      var h = 0
      runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
          val bmp = android.graphics.BitmapFactory.decodeStream(input)
          if (bmp != null) {
            w = bmp.width
            h = bmp.height
            saveThumbnail(bmp, thumbFile)
            bmp.recycle()
          }
        }
      }
      MediaRecordEntity(
        title = fileName,
        cameraName = cameraName,
        mediaType = MediaType.PHOTO.name,
        uriString = uri.toString(),
        filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "Pictures/ClearCam/$fileName" else uri.path.orEmpty(),
        thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
        fileSizeBytes = queryUriSize(uri),
        width = w,
        height = h,
      )
    }

  suspend fun indexExistingVideoUri(
    uri: Uri,
    cameraName: String,
    fallbackDurationMs: Long,
    privateFile: File? = null,
  ): MediaRecordEntity =
    withContext(Dispatchers.IO) {
      val stamp = timestampLabel()
      val fileName = privateFile?.name ?: "ClearCam_${sanitizeName(cameraName)}_$stamp.mp4"
      val thumbFile = File(thumbsDir, "THUMB_${stamp}_${System.nanoTime()}.jpg")
      var duration = fallbackDurationMs
      var w = 0
      var h = 0
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(context, uri)
        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        duration = durStr?.toLongOrNull() ?: fallbackDurationMs
        w = wStr?.toIntOrNull() ?: 0
        h = hStr?.toIntOrNull() ?: 0
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (frame != null) {
          saveThumbnail(frame, thumbFile)
          frame.recycle()
        }
      } catch (_: Throwable) {
      } finally {
        runCatching { retriever.release() }
      }
      val resolvedUri =
        if (privateFile != null && uri.scheme == "file") {
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", privateFile)
        } else {
          uri
        }
      MediaRecordEntity(
        title = fileName,
        cameraName = cameraName,
        mediaType = MediaType.VIDEO.name,
        uriString = resolvedUri.toString(),
        filePath = privateFile?.absolutePath ?: "Movies/ClearCam/$fileName",
        thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
        fileSizeBytes = privateFile?.length() ?: queryUriSize(resolvedUri),
        durationMs = duration,
        width = w,
        height = h,
      )
    }

  suspend fun deleteMediaFiles(record: MediaRecordEntity) =
    withContext(Dispatchers.IO) {
      runCatching {
        val uri = Uri.parse(record.uriString)
        if (uri.scheme == "content") {
          context.contentResolver.delete(uri, null, null)
        }
      }
      if (record.filePath.startsWith("/")) {
        runCatching { File(record.filePath).delete() }
      }
      if (record.thumbnailPath.isNotBlank()) {
        runCatching { File(record.thumbnailPath).delete() }
      }
    }

  fun shareMedia(record: MediaRecordEntity) {
    val uri = Uri.parse(record.uriString)
    val mime = if (record.type() == MediaType.VIDEO) "video/mp4" else "image/jpeg"
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(intent, record.title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }

  private fun extractVideoThumbnailAndDuration(videoFile: File, thumbFile: File): Long {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(videoFile.absolutePath)
      val dur =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      if (frame != null) {
        saveThumbnail(frame, thumbFile)
        frame.recycle()
      }
      dur
    } catch (_: Throwable) {
      0L
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun saveThumbnail(bitmap: Bitmap, target: File) {
    runCatching {
      val maxDim = 480
      val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
      val tw = (bitmap.width * scale).toInt().coerceAtLeast(1)
      val th = (bitmap.height * scale).toInt().coerceAtLeast(1)
      val scaled =
        if (tw != bitmap.width || th != bitmap.height) {
          Bitmap.createScaledBitmap(bitmap, tw, th, true)
        } else {
          bitmap
        }
      FileOutputStream(target).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 82, out) }
      if (scaled !== bitmap) scaled.recycle()
    }
  }

  private fun queryUriSize(uri: Uri): Long {
    return runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd -> afd.length }
          ?: 0L
      }
      .getOrDefault(0L)
  }

  private fun timestampLabel(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

  private fun sanitizeName(name: String): String =
    name.trim().replace(Regex("[^a-zA-Z0-9ก-ฮะ-์_-]"), "_").take(18).ifBlank { "CAM" }
}
