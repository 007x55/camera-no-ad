package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CameraEntity
import com.example.data.local.ClearCamDatabase
import com.example.data.local.MediaRecordEntity
import com.example.data.local.MediaType
import com.example.data.local.StreamProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ClearCam", appName)
  }

  @Test
  fun `camera entity builds rtsp and http urls cleanly`() {
    val garageCam =
      CameraEntity(
        name = "โรงรถ",
        ipAddress = "192.168.1.51",
        port = 554,
        streamPath = "/live/ch00_0",
        protocol = StreamProtocol.RTSP.name,
        username = "admin",
        password = "secret",
      )
    assertEquals("rtsp://admin:secret@192.168.1.51:554/live/ch00_0", garageCam.buildStreamUrl())

    val customCam =
      garageCam.copy(customFullUrl = "http://192.168.1.88:8080/video")
    assertEquals("http://192.168.1.88:8080/video", customCam.buildStreamUrl())
  }

  @Test
  fun `room database persists cameras and local media records`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, ClearCamDatabase::class.java).build()
    try {
      val camId =
        db.cameraDao()
          .insertCamera(
            CameraEntity(
              name = "โรงรถ",
              deviceId = "A1369004VLVYJR210402",
              ipAddress = "192.168.1.51",
              port = 554,
              streamPath = "/live/ch00_0",
              wifiSsid = "124G",
              macAddress = "30:8e:7a:4a:38:c2",
            )
          )
      val loadedCam = db.cameraDao().getCameraById(camId)
      assertNotNull(loadedCam)
      assertEquals("โรงรถ", loadedCam?.name)

      db.mediaRecordDao()
        .insertMedia(
          MediaRecordEntity(
            title = "ClearCam_Garage_test.mp4",
            cameraName = "โรงรถ",
            mediaType = MediaType.VIDEO.name,
            uriString = "content://media/external/video/media/1",
            fileSizeBytes = 204800L,
            durationMs = 12000L,
          )
        )
      val media = db.mediaRecordDao().observeAllMedia().first()
      assertEquals(1, media.size)
      assertEquals(MediaType.VIDEO, media.first().type())
    } finally {
      db.close()
    }
  }
}
