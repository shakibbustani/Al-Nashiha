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
            settingsDao.saveSettings(AppSettingsEntity(id = 1, safeBoxPin = "1234"))
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
            MediaStore.Video.Media.DATE_ADDED
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
                            resolutionOrBitrate = "1080p · Local",
                            folderName = folderName,
                            thumbnailGradientIndex = (id % 5).toInt(),
                            clipDescription = "Local video file"
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
                            resolutionOrBitrate = "Local Audio",
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

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)

    suspend fun createFolder(name: String, colorHex: String) {
        folderDao.insertFolder(FolderEntity(name = name, colorHex = colorHex, isDefault = false))
    }

    suspend fun deleteFolder(id: Long) = folderDao.deleteFolder(id)

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
                clipDescription = "Beautiful Tilawat of Surah Ar-Rahman with English subtitles. Listen for peace of heart."
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
                clipDescription = "An inspiring discourse on faith, gratitude, and moral excellence in everyday life."
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
                title = "Seerat-un-Nabi Episode 1 - Light of Guidance",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 930000,
                formattedDuration = "15:30",
                sizeBytes = 88000000,
                formattedSize = "83.9 MB",
                dateAdded = now - oneDay,
                dateGroup = "YESTERDAY",
                resolutionOrBitrate = "1080p · 30fps",
                progressRatio = 0.5f,
                lastPositionMs = 465000,
                isLocked = false,
                folderName = "Lectures & Bayan",
                thumbnailGradientIndex = 3,
                clipDescription = "First chapter exploring the noble life and character of Prophet Muhammad (PBUH)."
            ),
            MediaItem(
                title = "Surah Al-Kahf - Friday Special Recitation",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 1260000,
                formattedDuration = "21:00",
                sizeBytes = 110000000,
                formattedSize = "104.9 MB",
                dateAdded = now - (3 * oneDay),
                dateGroup = "3 DAYS AGO",
                resolutionOrBitrate = "1080p · 60fps",
                progressRatio = 0.0f,
                lastPositionMs = 0L,
                isLocked = false,
                folderName = "Quran Recitations",
                thumbnailGradientIndex = 4,
                clipDescription = "Complete Tilawat of Surah Al-Kahf for Jummah blessing."
            ),
            MediaItem(
                title = "Private Financial & Personal Notes Video",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                mediaType = MediaType.VIDEO,
                durationMs = 240000,
                formattedDuration = "04:00",
                sizeBytes = 22000000,
                formattedSize = "21.0 MB",
                dateAdded = now - (4 * oneDay),
                dateGroup = "EARLIER",
                resolutionOrBitrate = "720p · 30fps",
                progressRatio = 0.1f,
                lastPositionMs = 24000,
                isLocked = true, // SAFE BOX CONTENT
                folderName = "Safe Box",
                thumbnailGradientIndex = 0,
                clipDescription = "Confidential document overview record."
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
                clipDescription = "A quick 60-second video reminder on maintaining gentle speech and helping neighbours."
            )
        )
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
