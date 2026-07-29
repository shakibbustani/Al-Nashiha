package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettingsEntity
import com.example.data.model.FolderEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.PlaylistEntity
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterType { ALL, VIDEO, AUDIO }
enum class SortOption { DATE, NAME, SIZE }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    val unlockedMedia: StateFlow<List<MediaItem>> = repository.unlockedMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedMedia: StateFlow<List<MediaItem>> = repository.lockedMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI state controls
    val selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedSort = MutableStateFlow(SortOption.DATE)
    val searchQuery = MutableStateFlow("")
    val activeFolderFilter = MutableStateFlow<String?>(null)

    val safeBoxUnlocked = MutableStateFlow(false)
    val enteredPin = MutableStateFlow("")
    val pinErrorState = MutableStateFlow(false)

    val activePlayingMedia = MutableStateFlow<MediaItem?>(null)
    val isPlaying = MutableStateFlow(false)
    val playbackPosition = MutableStateFlow(0L)
    val playbackDuration = MutableStateFlow(1L)
    val playbackSpeed = MutableStateFlow(1.0f)

    val currentTopTab = MutableStateFlow(0) // 0=Media, 1=SafeBox, 2=Playlist, 3=History
    val currentBottomNav = MutableStateFlow(0) // 0=Home, 1=Clips, 2=Folder, 3=Setting

    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        unlockedMedia,
        selectedFilter,
        selectedSort,
        searchQuery,
        activeFolderFilter
    ) { items, filter, sort, query, folder ->
        var list = items

        // Folder filter
        if (folder != null) {
            list = list.filter { it.folderName.equals(folder, ignoreCase = true) }
        }

        // Type filter
        list = when (filter) {
            FilterType.VIDEO -> list.filter { it.mediaType == MediaType.VIDEO }
            FilterType.AUDIO -> list.filter { it.mediaType == MediaType.AUDIO }
            FilterType.ALL -> list
        }

        // Search query
        if (query.isNotBlank()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }

        // Sort
        when (sort) {
            SortOption.DATE -> list.sortedByDescending { it.dateAdded }
            SortOption.NAME -> list.sortedBy { it.title }
            SortOption.SIZE -> list.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
        }
    }

    fun scanMediaStore() {
        viewModelScope.launch {
            repository.scanMediaStore()
        }
    }

    fun lockMedia(item: MediaItem) {
        viewModelScope.launch {
            repository.lockMedia(item.id)
        }
    }

    fun unlockMedia(item: MediaItem) {
        viewModelScope.launch {
            repository.unlockMedia(item.id)
        }
    }

    fun renameMedia(item: MediaItem, newName: String) {
        viewModelScope.launch {
            repository.renameMedia(item.id, newName)
        }
    }

    fun moveToFolder(item: MediaItem, folderName: String) {
        viewModelScope.launch {
            repository.moveToFolder(item.id, folderName)
        }
    }

    fun deleteMedia(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMedia(item.id)
            if (activePlayingMedia.value?.id == item.id) {
                activePlayingMedia.value = null
            }
        }
    }

    fun playMedia(item: MediaItem) {
        activePlayingMedia.value = item
        isPlaying.value = true
        playbackPosition.value = item.lastPositionMs
        playbackDuration.value = if (item.durationMs > 0) item.durationMs else 1L

        viewModelScope.launch {
            repository.addToHistory(item, item.lastPositionMs)
        }
    }

    fun togglePlayPause() {
        isPlaying.value = !isPlaying.value
    }

    fun updatePlaybackPosition(positionMs: Long) {
        playbackPosition.value = positionMs
        val media = activePlayingMedia.value
        if (media != null) {
            viewModelScope.launch {
                repository.updatePlaybackProgress(media.id, positionMs, playbackDuration.value)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed.value = speed
    }

    fun closePlayer() {
        activePlayingMedia.value = null
        isPlaying.value = false
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun createPlaylist(name: String, colorHex: String, type: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, colorHex, type)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolder(id)
        }
    }

    fun verifyPin(entered: String, targetPin: String): Boolean {
        if (entered == targetPin) {
            safeBoxUnlocked.value = true
            pinErrorState.value = false
            return true
        } else {
            pinErrorState.value = true
            return false
        }
    }

    fun updateSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }
}
