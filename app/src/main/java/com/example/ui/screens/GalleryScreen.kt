package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MediaRecordEntity
import com.example.data.local.MediaType
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatFileSize
import com.example.ui.viewmodel.GalleryFilter
import com.example.ui.viewmodel.GalleryViewModel
import java.io.File

@Composable
fun GalleryScreen(
  viewModel: GalleryViewModel,
  onOpenMedia: (Long) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val columns = if (maxWidth >= 700.dp) 4 else 2

      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Title & Storage Summary Card
        item(span = { GridItemSpan(columns) }) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "คลังสื่อในเครื่อง", style = MaterialTheme.typography.headlineMedium)
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.large,
            ) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      MaterialTheme.shapes.large,
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier =
                      Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Folder,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Column {
                    Text("บันทึกเฉพาะในเครื่อง 100%", style = MaterialTheme.typography.titleSmall)
                    Text(
                      text =
                        "${uiState.totalPhotos} ภาพ • ${uiState.totalVideos} วิดีโอ • ${formatFileSize(uiState.totalBytes)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              GalleryFilter.entries.forEach { filter ->
                FilterChip(
                  selected = uiState.filter == filter,
                  onClick = { viewModel.setFilter(filter) },
                  label = { Text(filter.label) },
                  modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                )
              }
            }
          }
        }

        if (uiState.items.isEmpty()) {
          item(span = { GridItemSpan(columns) }) {
            Card(
              modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.PhotoLibrary,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(48.dp),
                )
                Text(
                  text = "ยังไม่มีภาพถ่ายหรือวิดีโอที่บันทึก",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text =
                    "เมื่อคุณกด 'ถ่ายภาพ' หรือ 'อัดวิดีโอ' จากกล้อง IP หรือกล้องตัวเครื่อง ไฟล์จะถูกบันทึกที่นี่และในแกลเลอรีของเครื่อง",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(uiState.items, key = { it.id }) { item ->
            MediaThumbnailCard(item = item, onClick = { onOpenMedia(item.id) })
          }
        }
      }
    }
  }
}

@Composable
private fun MediaThumbnailCard(item: MediaRecordEntity, onClick: () -> Unit) {
  val isVideo = item.type() == MediaType.VIDEO
  val imageModel: Any =
    if (item.thumbnailPath.isNotBlank() && File(item.thumbnailPath).exists()) {
      File(item.thumbnailPath)
    } else {
      Uri.parse(item.uriString)
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("media_item_${item.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column {
      Box(
        modifier =
          Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        AsyncImage(
          model = imageModel,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )

        if (isVideo) {
          Box(
            modifier =
              Modifier.align(Alignment.Center)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "เล่นวิดีโอ",
              tint = Color.White,
              modifier = Modifier.size(28.dp),
            )
          }
          Box(
            modifier =
              Modifier.align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = formatDurationMs(item.durationMs),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
            )
          }
        }

        Box(
          modifier =
            Modifier.align(Alignment.TopStart)
              .padding(8.dp)
              .clip(CircleShape)
              .background(Color(0x99000000))
              .padding(5.dp)
        ) {
          Icon(
            imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Photo,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
          )
        }
      }

      Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
        Text(
          text = item.cameraName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "${formatDateTime(item.createdAt)} • ${formatFileSize(item.fileSizeBytes)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
