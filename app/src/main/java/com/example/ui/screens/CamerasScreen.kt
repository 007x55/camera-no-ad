package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CameraEntity
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldOnline
import com.example.ui.viewmodel.CameraListViewModel

@Composable
fun CamerasScreen(
  viewModel: CameraListViewModel,
  defaultForceTcp: Boolean,
  onOpenDeviceCamera: () -> Unit,
  onOpenLiveCamera: (Long) -> Unit,
  onOpenCameraSettings: (Long) -> Unit,
) {
  val cameras by viewModel.cameras.collectAsStateWithLifecycle()
  val lanScanState by viewModel.lanScanState.collectAsStateWithLifecycle()
  var showAddSheet by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddSheet = true },
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text("เพิ่มกล้อง IP") },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("fab_add_camera"),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val columns = if (maxWidth >= 700.dp) 2 else 1

      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Header Row
        item(span = { GridItemSpan(columns) }) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(text = "ClearCam", style = MaterialTheme.typography.headlineMedium)
              Text(
                text = "ดูกล้องสด & บันทึกลงเครื่องโดยตรง",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            StatusPill(
              text = "ปลอดโฆษณา",
              dotColor = EmeraldOnline,
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            )
          }
        }

        // Built-in Device Camera Quick Launch Card
        item(span = { GridItemSpan(columns) }) {
          DeviceCameraHeroCard(onClick = onOpenDeviceCamera)
        }

        // Section Header
        item(span = { GridItemSpan(columns) }) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "กล้อง IP / Wi-Fi (${cameras.size})",
              style = MaterialTheme.typography.titleMedium,
            )
            if (cameras.isNotEmpty()) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                  text = "เก็บค่าในเครื่องเท่านั้น",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        if (cameras.isEmpty()) {
          item(span = { GridItemSpan(columns) }) {
            EmptyIpCameraCard(onAddCamera = { showAddSheet = true })
          }
        } else {
          items(cameras, key = { it.id }) { cam ->
            IpCameraCard(
              camera = cam,
              onWatchLive = { onOpenLiveCamera(cam.id) },
              onToggleEnabled = { viewModel.toggleCameraEnabled(cam) },
              onOpenSettings = { onOpenCameraSettings(cam.id) },
            )
          }
        }
      }
    }
  }

  if (showAddSheet) {
    AddCameraSheet(
      lanScanState = lanScanState,
      defaultForceTcp = defaultForceTcp,
      onStartScan = { viewModel.scanLan() },
      onDismiss = {
        showAddSheet = false
        viewModel.resetScanState()
      },
      onSaveCamera = {
        name,
        ipAddress,
        port,
        streamPath,
        customFullUrl,
        protocol,
        username,
        password,
        deviceId,
        wifiSsid,
        macAddress,
        firmwareVersion,
        flipVertical,
        forceTcp ->
        viewModel.addCamera(
          name = name,
          ipAddress = ipAddress,
          port = port,
          streamPath = streamPath,
          customFullUrl = customFullUrl,
          protocol = protocol,
          username = username,
          password = password,
          deviceId = deviceId,
          wifiSsid = wifiSsid,
          macAddress = macAddress,
          firmwareVersion = firmwareVersion,
          flipVertical = flipVertical,
          forceTcp = forceTcp,
        ) {
          showAddSheet = false
          viewModel.resetScanState()
        }
      },
    )
  }
}

@Composable
private fun DeviceCameraHeroCard(onClick: () -> Unit) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick)
        .testTag("card_device_camera"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .border(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            MaterialTheme.shapes.large,
          )
          .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.CameraAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "กล้องตัวเครื่อง (พร้อมใช้งาน)",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "ถ่ายภาพ & บันทึกวิดีโอ MP4 ลงในตัวเครื่องทันที",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Button(
          onClick = onClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("open_device_cam_button"),
        ) {
          Text("เปิดกล้อง")
        }
      }
    }
  }
}

@Composable
private fun EmptyIpCameraCard(onAddCamera: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_camera_empty_banner),
        contentDescription = "ภาพประกอบเชื่อมต่อกล้อง IP และบันทึกลงตัวเครื่อง",
        modifier = Modifier.fillMaxWidth().height(175.dp),
        contentScale = ContentScale.Crop,
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "ยังไม่มีกล้อง IP / Wi-Fi ที่เพิ่มไว้",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text =
            "เพิ่มกล้องวงจรปิดในบ้านผ่าน IP / RTSP / HTTP หรือสแกนหาใน Wi-Fi เพื่อดูภาพสด ถ่ายรูป และอัดวิดีโอเก็บลงในเครื่องได้โดยตรง ไม่มีโฆษณาคั่น และไม่ต้องสมัครคลาวด์",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = onAddCamera,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("empty_add_camera_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("เพิ่มกล้อง IP / Wi-Fi แรกของคุณ")
        }
      }
    }
  }
}

@Composable
private fun IpCameraCard(
  camera: CameraEntity,
  onWatchLive: () -> Unit,
  onToggleEnabled: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onWatchLive)
        .testTag("camera_card_${camera.id}"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (camera.isEnabled) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Videocam,
              contentDescription = null,
              tint =
                if (camera.isEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = camera.name,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (camera.customFullUrl.isNotBlank()) camera.customFullUrl
                else "${camera.streamProtocol().label} • ${camera.ipAddress}:${camera.port}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Switch(
          checked = camera.isEnabled,
          onCheckedChange = { onToggleEnabled() },
          modifier = Modifier.testTag("toggle_camera_${camera.id}"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(
          text = if (camera.isEnabled) "พร้อมดูสด" else "ปิดกล้อง",
          dotColor =
            if (camera.isEnabled) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (camera.wifiSsid.isNotBlank()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Wifi,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = camera.wifiSsid,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onWatchLive,
          modifier = Modifier.weight(1f).height(48.dp).testTag("watch_live_${camera.id}"),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(Modifier.size(6.dp))
          Text("ดูสด & บันทึก")
        }
        IconButton(
          onClick = onOpenSettings,
          modifier =
            Modifier.size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("settings_camera_${camera.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "ตั้งค่ากล้อง ${camera.name}",
          )
        }
      }
    }
  }
}
