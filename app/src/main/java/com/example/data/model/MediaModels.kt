package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    VIDEO, AUDIO
}

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val path: String,
    val uriString: String,
    val mediaType: MediaType,
    val durationMs: Long,
    val formattedDuration: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val dateAdded: Long,
    val dateGroup: String, // "TODAY", "YESTERDAY", "3 DAYS AGO", "EARLIER"
    val resolutionOrBitrate: String, // e.g. "1080p · 60fps" or "320 kbps"
    val progressRatio: Float = 0f, // 0.0 to 1.0
    val lastPositionMs: Long = 0L,
    val isLocked: Boolean = false, // True if moved to Safe Box
    val folderName: String = "Default",
    val isFavorite: Boolean = false,
    val likesCount: Int = 124,
    val thumbnailGradientIndex: Int = 0,
    val clipDescription: String = ""
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverColorHex: String = "#E53935",
    val type: String = "MIXED", // "VIDEO", "AUDIO", "MIXED"
    val mediaIdsCsv: String = "" // Comma separated IDs
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val title: String,
    val mediaType: MediaType,
    val durationMs: Long,
    val lastPositionMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateGroup: String = "TODAY"
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#E53935",
    val isDefault: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val language: String = "English",
    val themeMode: String = "Light",
    val autoPlayNext: Boolean = true,
    val resumeVideo: Boolean = true,
    val videoMode: String = "Sensor",
    val pinchToZoom: Boolean = true,
    val slideForSound: Boolean = true,
    val slideForBrightness: Boolean = true,
    val bgPlay: Boolean = false,
    val safeBoxPin: String = "",
    val autoLockOnBg: Boolean = true,
    val fingerprintEnabled: Boolean = false,
    val recoveryEmail: String = ""
)
