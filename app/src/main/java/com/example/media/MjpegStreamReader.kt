package com.example.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

class MjpegStreamReader {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()

  fun streamFrames(url: String, username: String = "", password: String = ""): Flow<Bitmap> =
    flow {
        val requestBuilder = Request.Builder().url(url)
        if (username.isNotBlank()) {
          requestBuilder.header("Authorization", Credentials.basic(username, password))
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
          if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: เชื่อมต่อกล้องไม่สำเร็จ")
          }
          val body = response.body ?: throw IllegalStateException("ไม่พบข้อมูลภาพจากกล้อง")
          val input = BufferedInputStream(body.byteStream(), 8192)
          val frameBuffer = ByteArrayOutputStream(65536)
          var prevByte = 0
          var capturing = false

          while (currentCoroutineContext().isActive) {
            val b = input.read()
            if (b == -1) break
            if (!capturing) {
              if (prevByte == 0xFF && b == 0xD8) {
                capturing = true
                frameBuffer.reset()
                frameBuffer.write(0xFF)
                frameBuffer.write(0xD8)
              }
            } else {
              frameBuffer.write(b)
              if (prevByte == 0xFF && b == 0xD9) {
                capturing = false
                val bytes = frameBuffer.toByteArray()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                  emit(bmp)
                }
              } else if (frameBuffer.size() > 2_500_000) {
                capturing = false
                frameBuffer.reset()
              }
            }
            prevByte = b
          }
        }
      }
      .flowOn(Dispatchers.IO)
}
