package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
  @Query("SELECT * FROM cameras ORDER BY createdAt DESC")
  fun observeAllCameras(): Flow<List<CameraEntity>>

  @Query("SELECT * FROM cameras WHERE id = :id LIMIT 1")
  fun observeCameraById(id: Long): Flow<CameraEntity?>

  @Query("SELECT * FROM cameras WHERE id = :id LIMIT 1")
  suspend fun getCameraById(id: Long): CameraEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCamera(camera: CameraEntity): Long

  @Update
  suspend fun updateCamera(camera: CameraEntity)

  @Query("DELETE FROM cameras WHERE id = :id")
  suspend fun deleteCameraById(id: Long)
}
