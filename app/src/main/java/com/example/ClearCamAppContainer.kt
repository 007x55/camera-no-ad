package com.example

import android.content.Context
import com.example.data.local.ClearCamDatabase
import com.example.data.local.ClearCamRepository
import com.example.data.local.UserPreferencesRepository
import com.example.media.LanCameraScanner
import com.example.media.LocalMediaStorageManager

class ClearCamAppContainer(context: Context) {
  private val appContext = context.applicationContext
  private val database = ClearCamDatabase.getInstance(appContext)
  private val storageManager = LocalMediaStorageManager(appContext)
  private val lanScanner = LanCameraScanner()
  val preferencesRepository = UserPreferencesRepository(appContext)

  val repository =
    ClearCamRepository(
      cameraDao = database.cameraDao(),
      mediaRecordDao = database.mediaRecordDao(),
      storageManager = storageManager,
      lanScanner = lanScanner,
      preferencesRepository = preferencesRepository,
    )
}
