package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.FolderEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MediaRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val mediaDao = db.mediaDao()
    private val playlistDao = db.playlistDao()
    private val historyDao = db.historyDao()
    private val folderDao = db.folderDao()
    private val settingsDao = db.settingsDao()

    val unlockedMedia: Flow<List<MediaItem>> = mediaDao.getAllUnlockedMedia()
    val lockedMedia: Flow<List<MediaItem>> = mediaDao.getAllLockedMedia()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val history: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val settings: Flow<AppSettingsEntity?> = settingsDao.getSettings()

    suspend fun initializeSeedDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = mediaDao.getMediaCount()
        if (count == 0) {
            val sampleItems = createInitialSampleItems()
            mediaDao.insertMedia(sampleItems)

            // Seed default folders
            folderDao.insertFolder(FolderEntity(name = "Downloaded Video", colorHex = "#E53935", isDefault = true))
            folderDao.insertFolder(FolderEntity(name = "Downloaded Audio", colorHex = "#1E88E5", isDefault = true))
            folderDao.insertFolder(FolderEntity(name = "Lectures & Bayan", colorHex = "#43A047", isDefault = false))
            folderDao.insertFolder(FolderEntity(name = "Quran Recitations", colorHex = "#FB8C00", isDefault = false))

            // Seed sample playlists
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Daily Quran & Zikr",
                    coverColorHex = "#E53935",
                    type = "AUDIO",
                    mediaIdsCsv = "1,2,3"
                )
            )
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Islamic Knowledge Series",
                    coverColorHex = "#1E88E5",
                    type = "VIDEO",
                    mediaIdsCsv = "4,5,6"
                )
            )

            // Seed initial settings
            settingsDao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    safeBoxPin = "1234",
                    recoveryKey = com.example.util.generateRecoveryKey()
                )
            )
        } else {
            // Ensure settings entity row 1 exists and has a recovery key
            val existing = settingsDao.getSettingsDirect()
            if (existing == null) {
                settingsDao.saveSettings(
                    AppSettingsEntity(
                        id = 1,
                        safeBoxPin = "1234",
                        recoveryKey = com.example.util.generateRecoveryKey()
                    )
                )
            } else if (existing.recoveryKey.isBlank()) {
                settingsDao.saveSettings(
                    existing.copy(recoveryKey = com.example.util.generateRecoveryKey())
                )
            }
        }
    }

    suspend fun scanMediaStore() = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<MediaItem>()
        val contentResolver: ContentResolver = context.contentResolver

        // Get existing items to prevent duplicates
        val existingMedia = try {
            mediaDao.getAllMediaDirect()
        } catch (e: Exception) {
            emptyList()
        }
        val existingUris = existingMedia.map { it.uriString }.toSet()
        val existingPaths = existingMedia.map { it.path }.filter { it.isNotBlank() }.toSet()

        // 1. Scan Videos
        val videoProjection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    if (contentUri in existingUris || (path.isNotBlank() && path in existingPaths)) {
                        continue
                    }

                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "Video $id" else "Video $id"
                    val durationMs = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAddedSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val dateAdded = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0

                    val resolutionText = extractVideoResolution(width, height)

                    var folderName = "Downloaded Video"
                    if (bucketCol != -1) {
                        val bucket = cursor.getString(bucketCol)
                        if (!bucket.isNullOrBlank()) {
                            folderName = bucket
                        }
                    } else if (path.isNotBlank()) {
                        try {
                            val parent = java.io.File(path).parentFile?.name
                            if (!parent.isNullOrBlank()) folderName = parent
                        } catch (_: Exception) {}
                    }

                    scanned.add(
                        MediaItem(
                            title = title,
                            path = path.ifEmpty { contentUri },
                            uriString = contentUri,
                            mediaType = MediaType.VIDEO,
                            durationMs = durationMs,
                            formattedDuration = formatDuration(durationMs),
                            sizeBytes = sizeBytes,
                            formattedSize = formatFileSize(sizeBytes),
                            dateAdded = dateAdded,
                            dateGroup = getDateGroupLabel(dateAdded),
                            resolutionOrBitrate = resolutionText,
                            folderName = folderName,
                            thumbnailGradientIndex = (id % 5).toInt(),
                            clipDescription = "Local video file",
                            isPortrait = (height >= width) || (width == 0 && height == 0)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scan Audio
        val audioProjection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    if (contentUri in existingUris || (path.isNotBlank() && path in existingPaths)) {
                        continue
                    }

                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "Audio $id" else "Audio $id"
                    val durationMs = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAddedSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val dateAdded = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()

                    var folderName = "Downloaded Audio"
                    if (bucketCol != -1) {
                        val bucket = cursor.getString(bucketCol)
                        if (!bucket.isNullOrBlank()) {
                            folderName = bucket
                        }
                    } else if (path.isNotBlank()) {
                        try {
                            val parent = java.io.File(path).parentFile?.name
                            if (!parent.isNullOrBlank()) folderName = parent
                        } catch (_: Exception) {}
                    }

                    val qualityText = extractAudioQuality(sizeBytes, durationMs)

                    scanned.add(
                        MediaItem(
                            title = title,
                            path = path.ifEmpty { contentUri },
                            uriString = contentUri,
                            mediaType = MediaType.AUDIO,
                            durationMs = durationMs,
                            formattedDuration = formatDuration(durationMs),
                            sizeBytes = sizeBytes,
                            formattedSize = formatFileSize(sizeBytes),
                            dateAdded = dateAdded,
                            dateGroup = getDateGroupLabel(dateAdded),
                            resolutionOrBitrate = qualityText,
                            folderName = folderName,
                            thumbnailGradientIndex = (id % 5).toInt(),
                            clipDescription = "Local audio file"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (scanned.isNotEmpty()) {
            mediaDao.insertMedia(scanned)

            // Auto insert new folders if any
            val scannedFolders = scanned.map { it.folderName }.distinct()
            scannedFolders.forEach { fName ->
                folderDao.insertFolder(FolderEntity(name = fName, colorHex = "#1E88E5", isDefault = false))
            }
        }
    }

    suspend fun lockMedia(id: Long) = mediaDao.updateLockStatus(id, true)
    suspend fun unlockMedia(id: Long) = mediaDao.updateLockStatus(id, false)
    suspend fun renameMedia(id: Long, newTitle: String) = mediaDao.renameMedia(id, newTitle)
    suspend fun moveToFolder(id: Long, folderName: String) = mediaDao.moveToFolder(id, folderName)
    suspend fun deleteMedia(id: Long) = mediaDao.deleteMedia(id)

    suspend fun updatePlaybackProgress(id: Long, positionMs: Long, durationMs: Long) {
        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        mediaDao.updatePlaybackProgress(id, positionMs, progress)
    }

    suspend fun addToHistory(mediaItem: MediaItem, positionMs: Long) {
        historyDao.insertHistory(
            HistoryEntity(
                mediaId = mediaItem.id,
                title = mediaItem.title,
                mediaType = mediaItem.mediaType,
                durationMs = mediaItem.durationMs,
                lastPositionMs = positionMs,
                timestamp = System.currentTimeMillis(),
                dateGroup = "TODAY"
            )
        )
    }

    suspend fun clearAllHistory() = historyDao.clearAllHistory()
    suspend fun deleteHistory(id: Long) = historyDao.deleteHistory(id)

    suspend fun createPlaylist(name: String, colorHex: String, type: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name, coverColorHex = colorHex, type = type))
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)

    suspend fun createFolder(name: String, colorHex: String) {
        folderDao.insertFolder(FolderEntity(name = name, colorHex = colorHex, isDefault = false))
    }

    suspend fun deleteFolder(id: Long) = folderDao.deleteFolder(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    suspend fun incrementLikes(id: Long) {
        mediaDao.incrementLikes(id)
    }

    suspend fun resetLikesAndCache() {
        mediaDao.resetAllLikes()
    }

    suspend fun updateSettings(settings: AppSettingsEntity) = settingsDao.saveSettings(settings)

    private fun createInitialSampleItems(): List<MediaItem> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        return listOf(
            MediaItem(
                title = "Surah Ar-Rahman - Heart Soothing Recitation",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 582000,
                formattedDuration = "09:42",
                sizeBytes = 44668270,
                formattedSize = "42.6 MB",
                dateAdded = now,
                dateGroup = "TODAY",
                resolutionOrBitrate = "1080p · 60fps",
                progressRatio = 0.35f,
                lastPositionMs = 203700,
                isLocked = false,
                folderName = "Quran Recitations",
                thumbnailGradientIndex = 0,
                clipDescription = "Beautiful Tilawat of Surah Ar-Rahman with English subtitles. Listen for peace of heart.",
                isPortrait = false
            ),
            MediaItem(
                title = "Short Reminder: Power of Istighfar & Patience",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 120000,
                formattedDuration = "02:00",
                sizeBytes = 18000000,
                formattedSize = "17.1 MB",
                dateAdded = now - (1 * 3600 * 1000L),
                dateGroup = "TODAY",
                resolutionOrBitrate = "1080p · 60fps",
                progressRatio = 0.0f,
                lastPositionMs = 0L,
                isLocked = false,
                folderName = "Lectures & Bayan",
                thumbnailGradientIndex = 2,
                clipDescription = "A 2-minute inspiring portrait reminder on seeking forgiveness and trusting Allah's timing.",
                isPortrait = true
            ),
            MediaItem(
                title = "Understanding the Purpose of Life - Comprehensive Bayan",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 1540000,
                formattedDuration = "25:40",
                sizeBytes = 124780000,
                formattedSize = "119.0 MB",
                dateAdded = now - (3 * 3600 * 1000L),
                dateGroup = "TODAY",
                resolutionOrBitrate = "720p · 30fps",
                progressRatio = 0.8f,
                lastPositionMs = 1232000,
                isLocked = false,
                folderName = "Lectures & Bayan",
                thumbnailGradientIndex = 1,
                clipDescription = "An inspiring discourse on faith, gratitude, and moral excellence in everyday life.",
                isPortrait = false
            ),
            MediaItem(
                title = "Morning & Evening Zikr (Adhkar) Audio Track",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                mediaType = MediaType.AUDIO,
                durationMs = 372000,
                formattedDuration = "06:12",
                sizeBytes = 14200000,
                formattedSize = "13.5 MB",
                dateAdded = now - (5 * 3600 * 1000L),
                dateGroup = "TODAY",
                resolutionOrBitrate = "320 kbps",
                progressRatio = 0.15f,
                lastPositionMs = 55800,
                isLocked = false,
                folderName = "Downloaded Audio",
                thumbnailGradientIndex = 2,
                clipDescription = "Daily essential remembrance of Allah with clear acoustic sound."
            ),
            MediaItem(
                title = "Daily Quranic Wisdom - Surah Al-Baqarah Ayah 152",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 180000,
                formattedDuration = "03:00",
                sizeBytes = 25000000,
                formattedSize = "23.8 MB",
                dateAdded = now - (8 * 3600 * 1000L),
                dateGroup = "TODAY",
                resolutionOrBitrate = "1080p · 60fps",
                progressRatio = 0.0f,
                lastPositionMs = 0L,
                isLocked = false,
                folderName = "Quran Recitations",
                thumbnailGradientIndex = 3,
                clipDescription = "3-minute short clip explaining 'Remember Me and I will remember you'.",
                isPortrait = true
            ),
            MediaItem(
                title = "3 Min Bayan: Beauty of Sincerity (Ikhlas)",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 210000,
                formattedDuration = "03:30",
                sizeBytes = 31000000,
                formattedSize = "29.5 MB",
                dateAdded = now - oneDay,
                dateGroup = "YESTERDAY",
                resolutionOrBitrate = "1080p · 30fps",
                progressRatio = 0.0f,
                lastPositionMs = 0L,
                isLocked = false,
                folderName = "Lectures & Bayan",
                thumbnailGradientIndex = 4,
                clipDescription = "Deep spiritual message on purifying intention in all good deeds.",
                isPortrait = true
            ),
            MediaItem(
                title = "Short Reminders - Good Manners & Kindness in Islam",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 60000,
                formattedDuration = "01:00",
                sizeBytes = 8500000,
                formattedSize = "8.1 MB",
                dateAdded = now,
                dateGroup = "TODAY",
                resolutionOrBitrate = "1080p · 60fps",
                progressRatio = 0.0f,
                lastPositionMs = 0L,
                isLocked = false,
                folderName = "Downloaded Video",
                thumbnailGradientIndex = 1,
                clipDescription = "A quick 60-second video reminder on maintaining gentle speech and helping neighbours.",
                isPortrait = true
            )
        )
    }

    private fun extractVideoResolution(width: Int, height: Int): String {
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)
        return when {
            minDim >= 2160 || maxDim >= 3840 -> "4K · Ultra HD"
            minDim >= 1440 || maxDim >= 2560 -> "1440p · 2K"
            minDim >= 1080 || maxDim >= 1920 -> "1080p · FHD"
            minDim >= 720 || maxDim >= 1280 -> "720p · HD"
            minDim >= 480 || maxDim >= 854 -> "480p · SD"
            minDim >= 360 -> "360p · SD"
            minDim > 0 -> "${minDim}p · SD"
            else -> "1080p · FHD"
        }
    }

    private fun extractAudioQuality(sizeBytes: Long, durationMs: Long): String {
        if (sizeBytes > 0 && durationMs > 0) {
            val durationSec = durationMs / 1000.0
            val bps = (sizeBytes * 8.0) / durationSec
            val kbps = (bps / 1000.0).toInt()
            return when {
                kbps >= 320 -> "320 kbps · High Quality"
                kbps >= 256 -> "256 kbps · High Quality"
                kbps >= 192 -> "192 kbps · HQ Audio"
                kbps >= 128 -> "128 kbps · Standard"
                kbps > 0 -> "$kbps kbps"
                else -> "320 kbps · High Quality"
            }
        }
        return "320 kbps · High Quality"
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 MB"
        val mb = sizeBytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    private fun getDateGroupLabel(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            days == 0L -> "TODAY"
            days == 1L -> "YESTERDAY"
            days in 2..3 -> "$days DAYS AGO"
            else -> "EARLIER"
        }
    }
}
