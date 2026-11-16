package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.StreamProtocol
import com.example.ui.viewmodel.LanScanState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCameraSheet(
  lanScanState: LanScanState,
  defaultForceTcp: Boolean,
  onStartScan: () -> Unit,
  onDismiss: () -> Unit,
  onSaveCamera:
    (
      name: String,
      ipAddress: String,
      port: Int,
      streamPath: String,
      customFullUrl: String,
      protocol: StreamProtocol,
      username: String,
      password: String,
      deviceId: String,
      wifiSsid: String,
      macAddress: String,
      firmwareVersion: String,
      flipVertical: Boolean,
      forceTcp: Boolean,
    ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var name by remember { mutableStateOf("") }
  var ipAddress by remember { mutableStateOf("") }
  var portText by remember { mutableStateOf("554") }
  var streamPath by remember { mutableStateOf("/stream1") }
  var customUrl by remember { mutableStateOf("") }
  var protocol by remember { mutableStateOf(StreamProtocol.RTSP) }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var deviceId by remember { mutableStateOf("") }
  var wifiSsid by remember { mutableStateOf("") }
  var macAddress by remember { mutableStateOf("") }
  var firmwareVersion by remember { mutableStateOf("") }
  var flipVertical by remember { mutableStateOf(false) }
  var forceTcp by remember { mutableStateOf(defaultForceTcp) }
  var validationError by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "เพิ่มกล้อง IP / Wi-Fi",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "เชื่อมต่อตรงผ่าน LAN / RTSP / HTTP โดยไม่ผ่านเซิร์ฟเวอร์โฆษณา",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // LAN Discovery section
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
          )
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "สแกนหากล้องใน Wi-Fi",
                style = MaterialTheme.typography.titleSmall,
              )
            }
            OutlinedButton(
              onClick = onStartScan,
              enabled = lanScanState !is LanScanState.Scanning,
              modifier = Modifier.testTag("scan_lan_button"),
            ) {
              if (lanScanState is LanScanState.Scanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text("กำลังสแกน...")
              } else {
                Text("เริ่มสแกน")
              }
            }
          }

          when (lanScanState) {
            is LanScanState.Idle -> {
              Text(
                text = "ค้นหาอุปกรณ์ที่เปิดพอร์ต 554 (RTSP), 8554, 80, 8080, 8000 ในวงแลนเดียวกัน",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            is LanScanState.Scanning -> {
              Text(
                text = "กำลังตรวจสอบเครือข่าย ${lanScanState.subnet ?: "192.168.1"}.1 - 254...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            is LanScanState.Completed -> {
              if (lanScanState.hosts.isEmpty()) {
                Text(
                  text =
                    if (lanScanState.subnet == null) {
                      "ไม่พบการเชื่อมต่อ Wi-Fi LAN ในขณะนี้ กรุณากรอก IP หรือ URL ด้านล่าง"
                    } else {
                      "ไม่พบพอร์ตกล้องที่เปิดในวง ${lanScanState.subnet}.x — สามารถกรอก IP/URL เองได้"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                Text(
                  text = "พบ ${lanScanState.hosts.size} อุปกรณ์ (แตะเพื่อกรอกค่าอัตโนมัติ):",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary,
                )
                lanScanState.hosts.forEach { host ->
                  Card(
                    modifier =
                      Modifier.fillMaxWidth().clickable {
                        ipAddress = host.ipAddress
                        portText = host.preferredPort.toString()
                        protocol =
                          if (host.preferredPort == 554 || host.preferredPort == 8554) {
                            StreamProtocol.RTSP
                          } else {
                            StreamProtocol.HTTP_MJPEG
                          }
                        if (name.isBlank()) name = "กล้อง ${host.ipAddress.substringAfterLast('.')}"
                      },
                    colors =
                      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(Icons.Rounded.Router, contentDescription = null)
                        Column {
                          Text(host.ipAddress, style = MaterialTheme.typography.titleSmall)
                          Text(
                            "พอร์ตเปิด: ${host.openPorts.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                      }
                      Text("เลือก", color = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Quick Presets
      Text("เทมเพลตตั้งค่าด่วน", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        AssistChip(
          onClick = {
            name = "โรงรถ"
            ipAddress = "192.168.1.51"
            portText = "554"
            streamPath = "/live/ch00_0"
            protocol = StreamProtocol.RTSP
            deviceId = "A1369004VLVYJR210402"
            wifiSsid = "124G"
            macAddress = "30:8e:7a:4a:38:c2"
            firmwareVersion = "6.0.05.10_202607161334"
          },
          label = { Text("กล้องโรงรถ (192.168.1.51)") },
          modifier = Modifier.testTag("preset_garage_chip"),
        )
        AssistChip(
          onClick = {
            protocol = StreamProtocol.RTSP
            portText = "554"
            streamPath = "/stream1"
          },
          label = { Text("Tapo / RTSP (:554)") },
        )
        AssistChip(
          onClick = {
            protocol = StreamProtocol.RTSP
            portText = "554"
            streamPath = "/Streaming/Channels/101"
          },
          label = { Text("Hikvision (:554)") },
        )
        AssistChip(
          onClick = {
            protocol = StreamProtocol.HTTP_MJPEG
            portText = "8080"
            streamPath = "/video"
          },
          label = { Text("HTTP MJPEG (:8080)") },
        )
      }

      // Protocol selector
      Text("โปรโตคอลสตรีม", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StreamProtocol.entries.forEach { item ->
          FilterChip(
            selected = protocol == item,
            onClick = {
              protocol = item
              if (portText == "554" || portText == "80" || portText == "8080") {
                portText = item.defaultPort.toString()
              }
            },
            label = { Text(item.label) },
          )
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("ชื่อกล้อง (เช่น โรงรถ, หน้าบ้าน)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("input_camera_name"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = ipAddress,
          onValueChange = { ipAddress = it },
          label = { Text("IP Address") },
          placeholder = { Text("192.168.1.51") },
          singleLine = true,
          modifier = Modifier.weight(2f).testTag("input_camera_ip"),
        )
        OutlinedTextField(
          value = portText,
          onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Port") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("input_camera_port"),
        )
      }

      OutlinedTextField(
        value = streamPath,
        onValueChange = { streamPath = it },
        label = { Text("Stream Path") },
        placeholder = { Text("/stream1 หรือ /live/ch00_0") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("input_stream_path"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text("ชื่อผู้ใช้ (ถ้ามี)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("รหัสผ่าน (ถ้ามี)") },
          singleLine = true,
          visualTransformation =
            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(
                imageVector =
                  if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = "แสดงหรือซ่อนรหัสผ่าน",
              )
            }
          },
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = customUrl,
        onValueChange = { customUrl = it },
        label = { Text("หรือใส่ Full Stream URL เต็ม (ไม่บังคับ)") },
        placeholder = { Text("rtsp://admin:1234@192.168.1.51:554/stream1") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("input_custom_url"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = deviceId,
          onValueChange = { deviceId = it },
          label = { Text("Device ID (ไม่บังคับ)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = wifiSsid,
          onValueChange = { wifiSsid = it },
          label = { Text("Wi-Fi SSID") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("กลับภาพ 180° (กรณีติดหัวกลับ)", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = flipVertical, onCheckedChange = { flipVertical = it })
      }

      if (protocol == StreamProtocol.RTSP) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("ใช้ RTSP ผ่าน TCP (เสถียรกว่าผ่าน Wi-Fi)", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = forceTcp, onCheckedChange = { forceTcp = it })
        }
      }

      if (validationError != null) {
        Text(
          text = validationError!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Button(
        onClick = {
          if (ipAddress.isBlank() && customUrl.isBlank()) {
            validationError = "กรุณากรอก IP Address หรือ Stream URL ของกล้อง"
            return@Button
          }
          val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: protocol.defaultPort
          onSaveCamera(
            name,
            ipAddress,
            port,
            streamPath,
            customUrl,
            protocol,
            username,
            password,
            deviceId,
            wifiSsid,
            macAddress,
            firmwareVersion,
            flipVertical,
            forceTcp,
          )
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_camera_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("บันทึกกล้องลงในเครื่อง")
      }
    }
  }
}
