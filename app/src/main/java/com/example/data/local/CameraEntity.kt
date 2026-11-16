package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StreamProtocol(val label: String, val defaultPort: Int) {
  RTSP("RTSP", 554),
  HTTP_MJPEG("HTTP MJPEG", 80),
  HLS_HTTP("HTTP/HLS", 8080),
}

@Entity(tableName = "cameras")
data class CameraEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val deviceId: String = "",
  val ipAddress: String,
  val port: Int = 554,
  val streamPath: String = "/stream1",
  val customFullUrl: String = "",
  val protocol: String = StreamProtocol.RTSP.name,
  val username: String = "",
  val password: String = "",
  val wifiSsid: String = "",
  val macAddress: String = "",
  val firmwareVersion: String = "",
  val isEnabled: Boolean = true,
  val flipVertical: Boolean = false,
  val mirrorHorizontal: Boolean = false,
  val forceTcp: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
) {
  fun streamProtocol(): StreamProtocol =
    runCatching { StreamProtocol.valueOf(protocol) }.getOrDefault(StreamProtocol.RTSP)

  fun buildStreamUrl(): String {
    if (customFullUrl.isNotBlank()) return customFullUrl.trim()
    val scheme =
      when (streamProtocol()) {
        StreamProtocol.RTSP -> "rtsp"
        StreamProtocol.HTTP_MJPEG,
        StreamProtocol.HLS_HTTP -> "http"
      }
    val auth =
      if (username.isNotBlank()) {
        val pass = if (password.isNotBlank()) ":$password" else ""
        "${username.trim()}$pass@"
      } else {
        ""
      }
    val cleanPath =
      if (streamPath.isBlank()) ""
      else if (streamPath.startsWith("/")) streamPath.trim() else "/${streamPath.trim()}"
    val cleanIp = ipAddress.trim()
    return "$scheme://$auth$cleanIp:$port$cleanPath"
  }
}
