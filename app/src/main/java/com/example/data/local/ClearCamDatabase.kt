package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [CameraEntity::class, MediaRecordEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ClearCamDatabase : RoomDatabase() {
  abstract fun cameraDao(): CameraDao

  abstract fun mediaRecordDao(): MediaRecordDao

  companion object {
    @Volatile private var instance: ClearCamDatabase? = null

    fun getInstance(context: Context): ClearCamDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                ClearCamDatabase::class.java,
                "clearcam_local.db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
