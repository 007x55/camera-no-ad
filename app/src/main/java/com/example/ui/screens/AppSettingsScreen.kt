package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AppSettingsViewModel

@Composable
fun AppSettingsScreen(viewModel: AppSettingsViewModel) {
  val prefs by viewModel.preferences.collectAsStateWithLifecycle()

  Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(text = "ตั้งค่าแอป", style = MaterialTheme.typography.headlineMedium)

        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
          shape = MaterialTheme.shapes.large,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("ปลอดโฆษณา & ไม่เก็บค่าบริการคลาวด์", style = MaterialTheme.typography.titleSmall)
              Text(
                text =
                  "ClearCam ทำงานแบบออฟไลน์/LAN บนตัวเครื่อง 100% ไม่มีโฆษณา ไม่มีป๊อปอัปขายแพ็กเกจ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.large,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text("การแสดงผลและสตรีม", style = MaterialTheme.typography.titleMedium)

            SettingSwitchRow(
              title = "โหมดมืด (Optic Dark)",
              subtitle = "เหมาะสำหรับดูภาพกล้องเวลากลางคืนและประหยัดสายตา",
              checked = prefs.isDarkTheme,
              onCheckedChange = viewModel::setDarkTheme,
              tag = "switch_dark_theme",
            )
            HorizontalDivider()
            SettingSwitchRow(
              title = "ใช้ RTSP ผ่าน TCP เป็นค่าเริ่มต้น",
              subtitle = "ลดปัญหาภาพแตกหรือหลุดเมื่อดูผ่าน Wi-Fi ภายในบ้าน",
              checked = prefs.defaultForceTcp,
              onCheckedChange = viewModel::setDefaultForceTcp,
              tag = "switch_default_tcp",
            )
            HorizontalDivider()
            SettingSwitchRow(
              title = "แสดงแถบสถานะ LIVE บนหน้าจอกล้อง",
              subtitle = "แสดงป้ายสถานะการเชื่อมต่อและโปรโตคอลที่มุมบนซ้าย",
              checked = prefs.showHudOverlay,
              onCheckedChange = viewModel::setShowHudOverlay,
              tag = "switch_hud_overlay",
            )
          }
        }

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.large,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Folder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("ตำแหน่งบันทึกไฟล์ในเครื่อง", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "ภาพถ่าย: Pictures/ClearCam\nวิดีโอ: Movies/ClearCam",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
  }
}
