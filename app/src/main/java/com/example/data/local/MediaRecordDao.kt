package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaRecordDao {
  @Query("SELECT * FROM media_records ORDER BY createdAt DESC")
  fun observeAllMedia(): Flow<List<MediaRecordEntity>>

  @Query("SELECT * FROM media_records WHERE id = :id LIMIT 1")
  fun observeMediaById(id: Long): Flow<MediaRecordEntity?>

  @Query("SELECT * FROM media_records WHERE id = :id LIMIT 1")
  suspend fun getMediaById(id: Long): MediaRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMedia(record: MediaRecordEntity): Long

  @Query("DELETE FROM media_records WHERE id = :id")
  suspend fun deleteMediaById(id: Long)

  @Query("DELETE FROM media_records")
  suspend fun deleteAllMedia()
}
