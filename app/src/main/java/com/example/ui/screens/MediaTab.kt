package com.example.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.FolderEntity
import com.example.data.model.MediaItem
import com.example.data.model.PlaylistEntity
import com.example.ui.FilterType
import com.example.ui.MainViewModel
import com.example.ui.SortOption
import com.example.ui.components.MediaCard
import com.example.ui.theme.RedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaList by viewModel.filteredMedia.collectAsState()
    val activeFilter by viewModel.selectedFilter.collectAsState()
    val activeSort by viewModel.selectedSort.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForRename by remember { mutableStateOf<MediaItem?>(null) }
    var selectedItemForMove by remember { mutableStateOf<MediaItem?>(null) }
    var selectedItemForPlaylist by remember { mutableStateOf<MediaItem?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.scanMediaStore()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scanMediaStore()
    }

    val requestPermissionsAndScan = {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
            else -> arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        val hasPermission = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) {
            viewModel.scanMediaStore()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Group items by dateGroup
    val groupedItems = remember(mediaList) {
        mediaList.groupBy { it.dateGroup }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: All Media (count) + Scan Storage & Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Media (${mediaList.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("media_count_header")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { requestPermissionsAndScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 34.dp)
                            .testTag("scan_storage_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Storage",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Scan Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showFilterBottomSheet = true },
                        border = BorderStroke(1.dp, RedPrimary),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 34.dp)
                            .testTag("filter_pill_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Filter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No media files found.",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { requestPermissionsAndScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("Scan Phone Media", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedItems.forEach { (dateHeader, items) ->
                        item {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 4.dp)
                                    .testTag("date_header_$dateHeader")
                            )
                        }

                        items(items, key = { it.id }) { mediaItem ->
                            MediaCard(
                                item = mediaItem,
                                onClick = { viewModel.playMedia(mediaItem) },
                                onRename = { selectedItemForRename = mediaItem },
                                onMoveToFolder = { selectedItemForMove = mediaItem },
                                onAddToPlaylist = { selectedItemForPlaylist = mediaItem },
                                onLockToggle = {
                                    if (mediaItem.isLocked) viewModel.unlockMedia(mediaItem)
                                    else viewModel.lockMedia(mediaItem)
                                },
                                onDelete = { viewModel.deleteMedia(mediaItem) },
                                onShare = {}
                            )
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterBottomSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Filter & Sort Media",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Media Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterType.entries.forEach { type ->
                            FilterChip(
                                selected = activeFilter == type,
                                onClick = { viewModel.selectedFilter.value = type },
                                label = { Text(type.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Sort By", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SortOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectedSort.value = option }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeSort == option,
                                onClick = { viewModel.selectedSort.value = option },
                                colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = when (option) {
                                SortOption.DATE -> "Date Added"
                                SortOption.NAME -> "Name (A-Z)"
                                SortOption.SIZE -> "Size (Largest)"
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showFilterBottomSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Filters", color = Color.White)
                    }
                }
            }
        }

        // Rename Dialog
        selectedItemForRename?.let { item ->
            var newName by remember { mutableStateOf(item.title) }
            AlertDialog(
                onDismissRequest = { selectedItemForRename = null },
                title = { Text("Rename Media") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Media Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.renameMedia(item, newName)
                            }
                            selectedItemForRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemForRename = null }) { Text("Cancel") }
                }
            )
        }

        // Move to Folder Dialog
        selectedItemForMove?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForMove = null },
                title = { Text("Move to Folder") },
                text = {
                    Column {
                        folders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveToFolder(item, folder.name)
                                        selectedItemForMove = null
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = RedPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = folder.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedItemForMove = null }) { Text("Cancel") }
                }
            )
        }

        // Add to Playlist Dialog
        selectedItemForPlaylist?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForPlaylist = null },
                title = { Text("Add to Playlist") },
                text = {
                    Column {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addMediaToPlaylist(playlist.id, item.id)
                                        selectedItemForPlaylist = null
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = RedPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = playlist.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedItemForPlaylist = null }) { Text("Cancel") }
                }
            )
        }
    }
}
