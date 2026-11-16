package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.example.data.local.StreamProtocol
import com.example.media.MjpegStreamReader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CrimsonRec
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldOnline
import com.example.ui.viewmodel.IpCameraLiveViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun IpCameraLiveScreen(
  viewModel: IpCameraLiveViewModel,
  showHudOverlay: Boolean,
  onBack: () -> Unit,
  onOpenSettings: (Long) -> Unit,
) {
  val camera by viewModel.camera.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var fullscreen by remember { mutableStateOf(false) }
  var shutterFlash by remember { mutableStateOf(false) }
  var reconnectKey by remember { mutableIntStateOf(0) }
  var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
  var latestMjpegBitmap by remember { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  LaunchedEffect(shutterFlash) {
    if (shutterFlash) {
      delay(140)
      shutterFlash = false
    }
  }

  val activeCam = camera
  val streamUrl = activeCam?.buildStreamUrl().orEmpty()
  val isMjpeg = activeCam?.streamProtocol() == StreamProtocol.HTTP_MJPEG

  // ExoPlayer for RTSP / HLS / HTTP video streams
  val exoPlayer =
    remember(activeCam?.id, streamUrl, activeCam?.forceTcp, activeCam?.isEnabled, reconnectKey) {
      if (activeCam == null || !activeCam.isEnabled || isMjpeg || streamUrl.isBlank()) {
        null
      } else {
        ExoPlayer.Builder(context).build().apply {
          volume = if (uiState.isMuted) 0f else 1f
          val item = MediaItem.fromUri(streamUrl)
          if (activeCam.streamProtocol() == StreamProtocol.RTSP) {
            val source =
              RtspMediaSource.Factory()
                .setForceUseRtpTcp(activeCam.forceTcp)
                .setTimeoutMs(6000L)
                .createMediaSource(item)
            setMediaSource(source)
          } else {
            setMediaItem(item)
          }
          playWhenReady = true
          prepare()
        }
      }
    }

  LaunchedEffect(uiState.isMuted, exoPlayer) {
    exoPlayer?.volume = if (uiState.isMuted) 0f else 1f
  }

  DisposableEffect(exoPlayer) {
    val player = exoPlayer
    if (player == null) {
      onDispose {}
    } else {
      viewModel.setStreamStatus(buffering = true, connected = false, error = null)
      val listener =
        object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              Player.STATE_BUFFERING ->
                viewModel.setStreamStatus(buffering = true, connected = false, error = null)
              Player.STATE_READY ->
                viewModel.setStreamStatus(buffering = false, connected = true, error = null)
              Player.STATE_ENDED ->
                viewModel.setStreamStatus(
                  buffering = false,
                  connected = false,
                  error = "สตรีมสิ้นสุดการส่งสัญญาณ",
                )
              else -> Unit
            }
          }

          override fun onPlayerError(error: PlaybackException) {
            viewModel.setStreamStatus(
              buffering = false,
              connected = false,
              error = error.localizedMessage ?: "ไม่สามารถเชื่อมต่อสตรีม RTSP/HTTP ได้",
            )
          }
        }
      player.addListener(listener)
      onDispose {
        player.removeListener(listener)
        textureViewRef?.let { player.clearVideoTextureView(it) }
        player.release()
      }
    }
  }

  // HTTP MJPEG reader when protocol == HTTP_MJPEG
  LaunchedEffect(activeCam?.id, streamUrl, activeCam?.isEnabled, isMjpeg, reconnectKey) {
    if (activeCam != null && activeCam.isEnabled && isMjpeg && streamUrl.isNotBlank()) {
      viewModel.setStreamStatus(buffering = true, connected = false, error = null)
      val reader = MjpegStreamReader()
      try {
        reader.streamFrames(streamUrl, activeCam.username, activeCam.password).collect { frame ->
          latestMjpegBitmap = frame
          viewModel.setStreamStatus(buffering = false, connected = true, error = null)
        }
      } catch (t: Throwable) {
        viewModel.setStreamStatus(
          buffering = false,
          connected = false,
          error = t.localizedMessage ?: "เชื่อมต่อ HTTP MJPEG ไม่สำเร็จ",
        )
      }
    }
  }

  fun captureOrientedBitmap(): Bitmap? {
    val raw: Bitmap? =
      if (isMjpeg) {
        latestMjpegBitmap?.let { it.copy(Bitmap.Config.ARGB_8888, false) }
      } else {
        textureViewRef?.let { tv ->
          if (tv.isAvailable && tv.width > 0 && tv.height > 0) tv.getBitmap(tv.width, tv.height)
          else null
        }
      }
    val bmp = raw ?: return null
    val flipV = activeCam?.flipVertical == true
    val mirrorH = activeCam?.mirrorHorizontal == true
    if (!flipV && !mirrorH) return bmp
    val matrix =
      Matrix().apply {
        postScale(if (mirrorH) -1f else 1f, if (flipV) -1f else 1f)
      }
    val transformed = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    if (transformed !== bmp) bmp.recycle()
    return transformed
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF06090D))
        .statusBarsPadding()
        .navigationBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Monitor Bar
      if (!fullscreen) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("live_back_button")) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "ย้อนกลับ",
                tint = Color.White,
              )
            }
            Column {
              Text(
                text = activeCam?.name ?: "กำลังโหลด...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
              if (activeCam != null) {
                Text(
                  text = "${activeCam.ipAddress}:${activeCam.port}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF94A3B8),
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!isMjpeg) {
              IconButton(onClick = { viewModel.toggleMute() }) {
                Icon(
                  imageVector =
                    if (uiState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff
                    else Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = if (uiState.isMuted) "เปิดเสียง" else "ปิดเสียง",
                  tint = Color.White,
                )
              }
            }
            if (activeCam != null) {
              IconButton(
                onClick = { onOpenSettings(activeCam.id) },
                modifier = Modifier.testTag("live_settings_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = "ตั้งค่ากล้อง",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }

      // Live Video Viewport
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (fullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 10f))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
      ) {
        if (activeCam == null) {
          CircularProgressIndicator(color = CyanAccent)
        } else if (!activeCam.isEnabled) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.PowerSettingsNew,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(48.dp),
            )
            Text("กล้องถูกปิดอยู่", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Button(onClick = { viewModel.toggleEnabled() }) { Text("เปิดกล้องตอนนี้") }
          }
        } else {
          if (isMjpeg) {
            val bmp = latestMjpegBitmap
            if (bmp != null && !bmp.isRecycled) {
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "ภาพสดจากกล้อง ${activeCam.name}",
                contentScale = ContentScale.Fit,
                modifier =
                  Modifier.fillMaxSize().graphicsLayer {
                    scaleY = if (activeCam.flipVertical) -1f else 1f
                    scaleX = if (activeCam.mirrorHorizontal) -1f else 1f
                  },
              )
            }
          } else {
            AndroidView(
              factory = { ctx ->
                TextureView(ctx).also { tv ->
                  textureViewRef = tv
                  exoPlayer?.setVideoTextureView(tv)
                }
              },
              update = { tv ->
                textureViewRef = tv
                exoPlayer?.setVideoTextureView(tv)
              },
              modifier =
                Modifier.fillMaxSize().graphicsLayer {
                  scaleY = if (activeCam.flipVertical) -1f else 1f
                  scaleX = if (activeCam.mirrorHorizontal) -1f else 1f
                },
            )
          }

          if (uiState.isBuffering && uiState.streamError == null) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(color = CyanAccent)
              Text(
                text = "กำลังเชื่อมต่อสตรีม...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
              )
            }
          }

          if (uiState.streamError != null) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(20.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "ยังรับภาพจากกล้องไม่ได้",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
              )
              Text(
                text = uiState.streamError ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { reconnectKey++ }) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null)
                  Spacer(Modifier.size(6.dp))
                  Text("ลองใหม่")
                }
                OutlinedButton(onClick = { onOpenSettings(activeCam.id) }) {
                  Text("แก้ URL / พอร์ต", color = Color.White)
                }
              }
            }
          }

          // HUD Badges
          if (showHudOverlay || uiState.isRecording) {
            Row(
              modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (uiState.isRecording) {
                StatusPill(
                  text = "REC ${formatDurationMs(uiState.recordingElapsedMs)}",
                  dotColor = CrimsonRec,
                  containerColor = Color(0xCC3A0D12),
                  textColor = Color.White,
                )
              } else if (uiState.isConnected) {
                StatusPill(
                  text = "LIVE • ${activeCam.streamProtocol().label}",
                  dotColor = EmeraldOnline,
                  containerColor = Color(0xAA0B0F14),
                  textColor = Color.White,
                )
              }
            }
          }

          // Fullscreen button overlay
          IconButton(
            onClick = { fullscreen = !fullscreen },
            modifier =
              Modifier.align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color(0x88000000)),
          ) {
            Icon(
              imageVector =
                if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
              contentDescription = if (fullscreen) "ออกจากเต็มจอ" else "ขยายเต็มจอ",
              tint = Color.White,
            )
          }

          // Shutter flash visual feedback
          androidx.compose.animation.AnimatedVisibility(
            visible = shutterFlash,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.65f)))
          }
        }
      }

      // Control Deck
      if (activeCam != null) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (!fullscreen) Modifier.weight(1f) else Modifier)
              .padding(16.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Primary Capture Buttons (Photo + Video)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Button(
              onClick = {
                shutterFlash = true
                viewModel.savePhotoSnapshot(captureOrientedBitmap())
              },
              enabled = activeCam.isEnabled && !uiState.isSavingMedia,
              modifier = Modifier.weight(1f).height(58.dp).testTag("ip_capture_photo_button"),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              Icon(Icons.Rounded.CameraAlt, contentDescription = null)
              Spacer(Modifier.size(8.dp))
              Text("ถ่ายภาพ", style = MaterialTheme.typography.labelLarge)
            }

            Button(
              onClick = {
                if (uiState.isRecording) {
                  viewModel.stopVideoRecording()
                } else {
                  val w = textureViewRef?.width ?: latestMjpegBitmap?.width ?: 960
                  val h = textureViewRef?.height ?: latestMjpegBitmap?.height ?: 540
                  viewModel.startVideoRecording(w, h) { captureOrientedBitmap() }
                }
              },
              enabled = activeCam.isEnabled && !uiState.isSavingMedia,
              modifier = Modifier.weight(1f).height(58.dp).testTag("ip_record_video_button"),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (uiState.isRecording) CrimsonRec
                    else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = if (uiState.isRecording) Color.White else Color.White,
                ),
            ) {
              Icon(
                imageVector =
                  if (uiState.isRecording) Icons.Rounded.Stop else Icons.Rounded.Videocam,
                contentDescription = null,
              )
              Spacer(Modifier.size(8.dp))
              Text(
                text = if (uiState.isRecording) "หยุดบันทึก" else "อัดวิดีโอ",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          if (!fullscreen) {
            // Secondary Quick Tools
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                QuickToolButton(
                  icon = Icons.Rounded.Flip,
                  label = if (activeCam.flipVertical) "กลับภาพ: เปิด" else "กลับภาพ 180°",
                  active = activeCam.flipVertical,
                  onClick = { viewModel.toggleFlipVertical() },
                )
                if (activeCam.streamProtocol() == StreamProtocol.RTSP) {
                  QuickToolButton(
                    icon = Icons.Rounded.Settings,
                    label = if (activeCam.forceTcp) "RTSP: TCP" else "RTSP: UDP",
                    active = activeCam.forceTcp,
                    onClick = { viewModel.toggleForceTcp() },
                  )
                }
                QuickToolButton(
                  icon = Icons.Rounded.Refresh,
                  label = "รีเฟรชสตรีม",
                  active = false,
                  onClick = { reconnectKey++ },
                )
              }
            }

            // Stream Endpoint Info Footer (Zero ads!)
            Text(
              text = "บันทึกภาพและวิดีโอลงในเครื่องอัตโนมัติ (Pictures/ClearCam & Movies/ClearCam)",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
              modifier = Modifier.padding(horizontal = 4.dp),
            )
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
    )
  }
}

@Composable
private fun QuickToolButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    IconButton(
      onClick = onClick,
      modifier =
        Modifier.size(48.dp)
          .clip(CircleShape)
          .background(
            if (active) CyanAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
          ),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) CyanAccent else Color.White,
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) CyanAccent else Color(0xFF94A3B8),
    )
  }
}
