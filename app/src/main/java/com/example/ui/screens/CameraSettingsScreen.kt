package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.StreamProtocol
import com.example.ui.viewmodel.CameraSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsScreen(
  viewModel: CameraSettingsViewModel,
  onBack: () -> Unit,
  onDeleted: () -> Unit,
) {
  val camera by viewModel.camera.collectAsStateWithLifecycle()
  var confirmDelete by remember { mutableStateOf(false) }

  var name by remember { mutableStateOf("") }
  var deviceId by remember { mutableStateOf("") }
  var ipAddress by remember { mutableStateOf("") }
  var portText by remember { mutableStateOf("554") }
  var streamPath by remember { mutableStateOf("/stream1") }
  var customFullUrl by remember { mutableStateOf("") }
  var protocol by remember { mutableStateOf(StreamProtocol.RTSP) }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var wifiSsid by remember { mutableStateOf("") }
  var macAddress by remember { mutableStateOf("") }
  var firmwareVersion by remember { mutableStateOf("") }
  var isEnabled by remember { mutableStateOf(true) }
  var flipVertical by remember { mutableStateOf(false) }
  var mirrorHorizontal by remember { mutableStateOf(false) }
  var forceTcp by remember { mutableStateOf(true) }
  var initializedForId by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(camera) {
    val cam = camera
    if (cam != null && initializedForId != cam.id) {
      initializedForId = cam.id
      name = cam.name
      deviceId = cam.deviceId
      ipAddress = cam.ipAddress
      portText = cam.port.toString()
      streamPath = cam.streamPath
      customFullUrl = cam.customFullUrl
      protocol = cam.streamProtocol()
      username = cam.username
      password = cam.password
      wifiSsid = cam.wifiSsid
      macAddress = cam.macAddress
      firmwareVersion = cam.firmwareVersion
      isEnabled = cam.isEnabled
      flipVertical = cam.flipVertical
      mirrorHorizontal = cam.mirrorHorizontal
      forceTcp = cam.forceTcp
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("ตั้งค่ากล้อง") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("cam_settings_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ย้อนกลับ")
          }
        },
      )
    }
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val currentCam = camera
      if (currentCam == null) {
        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
      } else {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 680.dp)
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Camera Identity & Power Switch
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("เปิด/ปิดกล้อง", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
              }
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ชื่อกล้อง") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = deviceId,
                onValueChange = { deviceId = it },
                label = { Text("ID อุปกรณ์") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          // Stream & Flip Settings
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("การเชื่อมต่อสตรีมและภาพ", style = MaterialTheme.typography.titleMedium)
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamProtocol.entries.forEach { item ->
                  FilterChip(
                    selected = protocol == item,
                    onClick = { protocol = item },
                    label = { Text(item.label) },
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                  value = ipAddress,
                  onValueChange = { ipAddress = it },
                  label = { Text("IP Address") },
                  singleLine = true,
                  modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                  value = portText,
                  onValueChange = { portText = it.filter { c -> c.isDigit() } },
                  label = { Text("Port") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
              }
              OutlinedTextField(
                value = streamPath,
                onValueChange = { streamPath = it },
                label = { Text("Stream Path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = customFullUrl,
                onValueChange = { customFullUrl = it },
                label = { Text("Full Stream URL (ถ้ากำหนดเอง)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                  value = username,
                  onValueChange = { username = it },
                  label = { Text("Username") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                  value = password,
                  onValueChange = { password = it },
                  label = { Text("Password") },
                  singleLine = true,
                  visualTransformation = PasswordVisualTransformation(),
                  modifier = Modifier.weight(1f),
                )
              }
              HorizontalDivider()
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("กลับภาพ 180° (แนวตั้ง)")
                Switch(checked = flipVertical, onCheckedChange = { flipVertical = it })
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("กลับภาพซ้าย-ขวา (Mirror)")
                Switch(checked = mirrorHorizontal, onCheckedChange = { mirrorHorizontal = it })
              }
              if (protocol == StreamProtocol.RTSP) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text("ใช้ RTSP ผ่าน TCP (ลดอาการภาพค้าง)")
                  Switch(checked = forceTcp, onCheckedChange = { forceTcp = it })
                }
              }
            }
          }

          // Network & Firmware Info (Clean diagnostics)
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("ข้อมูล Network & เฟิร์มแวร์", style = MaterialTheme.typography.titleMedium)
              OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it },
                label = { Text("Wi-Fi SSID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = macAddress,
                onValueChange = { macAddress = it },
                label = { Text("MAC Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = firmwareVersion,
                onValueChange = { firmwareVersion = it },
                label = { Text("เวอร์ชั่นเฟิร์มแวร์") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          Button(
            onClick = {
              val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: protocol.defaultPort
              viewModel.updateCamera(
                currentCam.copy(
                  name = name.trim().ifBlank { currentCam.name },
                  deviceId = deviceId.trim(),
                  ipAddress = ipAddress.trim(),
                  port = port,
                  streamPath = streamPath.trim(),
                  customFullUrl = customFullUrl.trim(),
                  protocol = protocol.name,
                  username = username.trim(),
                  password = password,
                  wifiSsid = wifiSsid.trim(),
                  macAddress = macAddress.trim(),
                  firmwareVersion = firmwareVersion.trim(),
                  isEnabled = isEnabled,
                  flipVertical = flipVertical,
                  mirrorHorizontal = mirrorHorizontal,
                  forceTcp = forceTcp,
                ),
                onDone = onBack,
              )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_cam_settings_button"),
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("บันทึกการตั้งค่า")
          }

          OutlinedButton(
            onClick = { confirmDelete = true },
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("delete_camera_button"),
          ) {
            Icon(Icons.Rounded.Delete, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("ลบอุปกรณ์กล้องนี้")
          }
        }
      }
    }
  }

  if (confirmDelete && camera != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("ลบกล้อง ${camera?.name}?") },
      text = {
        Text("การลบกล้องจะนำรายการนี้ออกจากแอป (ภาพและวิดีโอที่บันทึกไว้ในเครื่องจะยังคงอยู่)")
      },
      confirmButton = {
        Button(
          onClick = {
            confirmDelete = false
            camera?.let { viewModel.deleteCamera(it.id, onDeleted) }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("ลบอุปกรณ์")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก") }
      },
    )
  }
}
