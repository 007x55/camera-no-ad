package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDurationMs(durationMs: Long): String {
  val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0) {
    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0L) return "0 KB"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  return when {
    mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
    else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
  }
}

fun formatDateTime(epochMs: Long): String {
  val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  return sdf.format(Date(epochMs))
}

@Composable
fun StatusPill(
  text: String,
  dotColor: Color,
  containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(containerColor)
        .border(1.dp, dotColor.copy(alpha = 0.35f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = textColor)
  }
}
