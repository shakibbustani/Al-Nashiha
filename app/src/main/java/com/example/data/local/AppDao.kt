package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettingsEntity
import com.example.data.model.FolderEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.MediaItem
import com.example.data.model.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaDirect(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE isLocked = 0 ORDER BY dateAdded DESC")
    fun getAllUnlockedMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isLocked = 1 ORDER BY dateAdded DESC")
    fun getAllLockedMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE folderName = :folderName AND isLocked = 0 ORDER BY dateAdded DESC")
    fun getMediaByFolder(folderName: String): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<MediaItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleMedia(item: MediaItem): Long

    @Update
    suspend fun updateMedia(item: MediaItem)

    @Query("UPDATE media_items SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLockStatus(id: Long, isLocked: Boolean)

    @Query("UPDATE media_items SET title = :newTitle WHERE id = :id")
    suspend fun renameMedia(id: Long, newTitle: String)

    @Query("UPDATE media_items SET folderName = :folderName WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderName: String)

    @Query("UPDATE media_items SET lastPositionMs = :positionMs, progressRatio = :progress WHERE id = :id")
    suspend fun updatePlaybackProgress(id: Long, positionMs: Long, progress: Float)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLikes(id: Long)

    @Query("UPDATE media_items SET likesCount = 0")
    suspend fun resetAllLikes()

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: Long)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isDefault DESC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id AND isDefault = 0")
    suspend fun deleteFolder(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}
