package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.MediaType
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatFileSize
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(UnstableApi::class)
@Composable
fun MediaPreviewScreen(
  mediaId: Long,
  viewModel: GalleryViewModel,
  onBack: () -> Unit,
) {
  val record by viewModel.observeMedia(mediaId).collectAsStateWithLifecycle(initialValue = null)
  val context = LocalContext.current
  var confirmDelete by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .statusBarsPadding()
        .navigationBarsPadding()
  ) {
    val active = record
    if (active == null) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    } else {
      val uri = remember(active.uriString) { Uri.parse(active.uriString) }
      if (active.type() == MediaType.VIDEO) {
        val exoPlayer =
          remember(uri) {
            ExoPlayer.Builder(context).build().apply {
              setMediaItem(MediaItem.fromUri(uri))
              playWhenReady = true
              prepare()
            }
          }
        DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = exoPlayer
              useController = true
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        AsyncImage(
          model = uri,
          contentDescription = active.title,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier.fillMaxSize()
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  val newScale = (scale * zoom).coerceIn(1f, 4f)
                  scale = newScale
                  offset = if (newScale > 1f) offset + pan else Offset.Zero
                }
              }
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
              },
        )
      }

      // Top overlay bar
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(Color(0x99000000))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "ย้อนกลับ",
              tint = Color.White,
            )
          }
          Column {
            Text(
              text = active.cameraName,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
            Text(
              text =
                "${formatDateTime(active.createdAt)} • ${formatFileSize(active.fileSizeBytes)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFCBD5E1),
            )
          }
        }
        Row {
          IconButton(
            onClick = { viewModel.shareMedia(active) },
            modifier = Modifier.testTag("share_media_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = "แชร์ไฟล์",
              tint = Color.White,
            )
          }
          IconButton(
            onClick = { confirmDelete = true },
            modifier = Modifier.testTag("delete_media_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "ลบไฟล์",
              tint = Color(0xFFF87171),
            )
          }
        }
      }
    }
  }

  val currentRecord = record
  if (confirmDelete && currentRecord != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("ลบไฟล์ออกจากเครื่อง?") },
      text = { Text("ไฟล์ ${currentRecord.title} จะถูกลบออกจากตัวเครื่องอย่างถาวร") },
      confirmButton = {
        Button(
          onClick = {
            confirmDelete = false
            viewModel.deleteMedia(currentRecord)
            onBack()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("ลบเลย")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก") }
      },
    )
  }
}
