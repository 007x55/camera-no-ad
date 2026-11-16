package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ClearCamRepository
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CrimsonRec
import com.example.ui.theme.EmeraldOnline
import com.example.ui.viewmodel.DeviceCameraViewModel
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun DeviceCameraScreen(
  viewModel: DeviceCameraViewModel,
  repository: ClearCamRepository,
  onOpenGalleryItem: (Long) -> Unit,
  onOpenGalleryTab: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val latestMedia by viewModel.latestMedia.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val snackbarHostState = remember { SnackbarHostState() }

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  var pendingVideoStartAfterAudio by remember { mutableStateOf(false) }

  var boundCamera by remember { mutableStateOf<Camera?>(null) }
  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
  var activeRecording by remember { mutableStateOf<Recording?>(null) }
  var activePrivateVideoFile by remember { mutableStateOf<File?>(null) }
  var shutterFlash by remember { mutableStateOf(false) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

  fun startOrStopVideo(includeAudio: Boolean) {
    val currentRec = activeRecording
    if (currentRec != null) {
      currentRec.stop()
      activeRecording = null
      return
    }
    val vc = videoCapture ?: return
    val exec = ContextCompat.getMainExecutor(context)

    val pendingRecording =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = repository.createVideoMediaStoreValues("กล้องตัวเครื่อง")
        val options =
          MediaStoreOutputOptions.Builder(
              context.contentResolver,
              MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            )
            .setContentValues(values)
            .build()
        activePrivateVideoFile = null
        vc.output.prepareRecording(context, options)
      } else {
        val file = repository.createPrivateVideoFile("กล้องตัวเครื่อง")
        activePrivateVideoFile = file
        val options = FileOutputOptions.Builder(file).build()
        vc.output.prepareRecording(context, options)
      }

    val configured =
      if (includeAudio && hasAudioPermission) {
        pendingRecording.withAudioEnabled()
      } else {
        pendingRecording
      }

    activeRecording =
      configured.start(exec) { event ->
        when (event) {
          is VideoRecordEvent.Start -> viewModel.onRecordingStarted()
          is VideoRecordEvent.Finalize -> {
            activeRecording = null
            if (!event.hasError()) {
              val uri = event.outputResults.outputUri
              val durMs = event.recordingStats.recordedDurationNanos / 1_000_000L
              viewModel.onVideoFinalized(uri, durMs, activePrivateVideoFile)
            } else {
              viewModel.onRecordingStoppedOrError("บันทึกวิดีโอไม่สำเร็จ (${event.error})")
            }
          }
        }
      }
  }

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasAudioPermission = granted
      if (pendingVideoStartAfterAudio) {
        pendingVideoStartAfterAudio = false
        startOrStopVideo(includeAudio = granted)
      }
    }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  LaunchedEffect(shutterFlash) {
    if (shutterFlash) {
      delay(120)
      shutterFlash = false
    }
  }

  // Bind CameraX whenever lens facing or previewView changes
  LaunchedEffect(hasCameraPermission, uiState.isFrontCamera, previewViewRef) {
    val pv = previewViewRef
    if (!hasCameraPermission || pv == null) return@LaunchedEffect
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
        val imgCap =
          ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        val recorder =
          Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
        val vidCap = VideoCapture.withOutput(recorder)
        val selector =
          if (uiState.isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
          else CameraSelector.DEFAULT_BACK_CAMERA
        runCatching {
            provider.unbindAll()
            val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, imgCap, vidCap)
            boundCamera = cam
            imageCapture = imgCap
            videoCapture = vidCap
          }
          .onFailure { err ->
            viewModel.showMessage("ไม่สามารถเปิดกล้องได้: ${err.localizedMessage}")
          }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  LaunchedEffect(uiState.isTorchEnabled, boundCamera) {
    boundCamera?.cameraControl?.enableTorch(uiState.isTorchEnabled)
  }

  LaunchedEffect(uiState.zoomLevel, boundCamera) {
    boundCamera?.cameraControl?.setZoomRatio(uiState.zoomLevel)
  }

  DisposableEffect(Unit) {
    onDispose {
      activeRecording?.stop()
      activeRecording = null
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    if (!hasCameraPermission) {
      Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.CameraAlt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = "เปิดสิทธิ์การใช้งานกล้องตัวเครื่อง",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "ClearCam ใช้สิทธิ์กล้องเพื่อแสดงภาพสดและบันทึกภาพถ่าย/วิดีโอลงในตัวเครื่องของคุณโดยตรง",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF94A3B8),
        )
        Spacer(Modifier.height(20.dp))
        Button(
          onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
          modifier = Modifier.height(50.dp).testTag("request_camera_permission_button"),
        ) {
          Text("อนุญาตใช้งานกล้อง")
        }
      }
    } else {
      AndroidView(
        factory = { ctx ->
          PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            previewViewRef = this
          }
        },
        modifier = Modifier.fillMaxSize(),
      )

      // Top HUD
      Row(
        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (uiState.isRecording) {
          StatusPill(
            text = "REC ${formatDurationMs(uiState.recordingElapsedMs)}",
            dotColor = CrimsonRec,
            containerColor = Color(0xCC3A0D12),
            textColor = Color.White,
          )
        } else {
          StatusPill(
            text = if (uiState.isFrontCamera) "กล้องหน้า" else "กล้องหลัง",
            dotColor = EmeraldOnline,
            containerColor = Color(0xAA0B0F14),
            textColor = Color.White,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (!uiState.isFrontCamera) {
            IconButton(
              onClick = { viewModel.toggleTorch() },
              modifier = Modifier.clip(CircleShape).background(Color(0x88000000)),
            ) {
              Icon(
                imageVector =
                  if (uiState.isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "เปิดหรือปิดไฟฉาย",
                tint = if (uiState.isTorchEnabled) Color(0xFFFDE047) else Color.White,
              )
            }
          }
          IconButton(
            onClick = { viewModel.toggleLensFacing() },
            enabled = !uiState.isRecording,
            modifier =
              Modifier.clip(CircleShape)
                .background(Color(0x88000000))
                .testTag("switch_lens_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Cameraswitch,
              contentDescription = "สลับกล้องหน้าหลัง",
              tint = Color.White,
            )
          }
        }
      }

      // Bottom Control Deck
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(Color(0xAA0B0F14))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Zoom chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(1f, 2f, 3f).forEach { zoom ->
            FilterChip(
              selected = uiState.zoomLevel == zoom,
              onClick = { viewModel.setZoomLevel(zoom) },
              label = { Text("${zoom.toInt()}x") },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Latest capture thumbnail shortcut
          Box(
            modifier =
              Modifier.size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable {
                  val id = latestMedia?.id
                  if (id != null) onOpenGalleryItem(id) else onOpenGalleryTab()
                }
                .testTag("latest_media_thumb"),
            contentAlignment = Alignment.Center,
          ) {
            val thumb = latestMedia?.thumbnailPath?.takeIf { it.isNotBlank() }
            if (thumb != null) {
              AsyncImage(
                model = File(thumb),
                contentDescription = "เปิดไฟล์ล่าสุด",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = "เปิดคลังสื่อ",
                tint = Color.White,
              )
            }
          }

          // Photo Capture
          Button(
            onClick = {
              val cap = imageCapture ?: return@Button
              viewModel.setCapturingPhoto(true)
              shutterFlash = true
              val exec = ContextCompat.getMainExecutor(context)
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values =
                  android.content.ContentValues().apply {
                    put(
                      MediaStore.Images.Media.DISPLAY_NAME,
                      "ClearCam_Device_${System.currentTimeMillis()}.jpg",
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                      MediaStore.Images.Media.RELATIVE_PATH,
                      "${android.os.Environment.DIRECTORY_PICTURES}/ClearCam",
                    )
                  }
                val outputOptions =
                  ImageCapture.OutputFileOptions.Builder(
                      context.contentResolver,
                      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                      values,
                    )
                    .build()
                cap.takePicture(
                  outputOptions,
                  exec,
                  object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                      outputFileResults.savedUri?.let { viewModel.onPhotoSaved(it) }
                        ?: viewModel.setCapturingPhoto(false)
                    }

                    override fun onError(exception: ImageCaptureException) {
                      viewModel.setCapturingPhoto(false)
                      viewModel.showMessage("ถ่ายภาพไม่สำเร็จ: ${exception.localizedMessage}")
                    }
                  },
                )
              } else {
                val file =
                  File(
                    File(context.filesDir, "media").apply { if (!exists()) mkdirs() },
                    "ClearCam_Device_${System.currentTimeMillis()}.jpg",
                  )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                cap.takePicture(
                  outputOptions,
                  exec,
                  object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                      val uri =
                        androidx.core.content.FileProvider.getUriForFile(
                          context,
                          "${context.packageName}.fileprovider",
                          file,
                        )
                      viewModel.onPhotoSaved(uri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                      viewModel.setCapturingPhoto(false)
                      viewModel.showMessage("ถ่ายภาพไม่สำเร็จ: ${exception.localizedMessage}")
                    }
                  },
                )
              }
            },
            enabled = !uiState.isCapturingPhoto,
            modifier = Modifier.height(54.dp).testTag("device_capture_photo_button"),
          ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("ถ่ายภาพ")
          }

          // Video Record
          Button(
            onClick = {
              if (activeRecording != null) {
                startOrStopVideo(includeAudio = hasAudioPermission)
              } else if (!hasAudioPermission) {
                pendingVideoStartAfterAudio = true
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              } else {
                startOrStopVideo(includeAudio = true)
              }
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (uiState.isRecording) CrimsonRec else Color(0xFF1E293B),
                contentColor = Color.White,
              ),
            modifier = Modifier.height(54.dp).testTag("device_record_video_button"),
          ) {
            Icon(
              imageVector = if (uiState.isRecording) Icons.Rounded.Stop else Icons.Rounded.Videocam,
              contentDescription = null,
            )
            Spacer(Modifier.size(6.dp))
            Text(if (uiState.isRecording) "หยุด" else "อัดวิดีโอ")
          }
        }
      }

      AnimatedVisibility(visible = shutterFlash, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
    )
  }
}
