package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.PlaylistEntity
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCard
import com.example.ui.theme.RedPrimary

@Composable
fun PlaylistTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val mediaItems by viewModel.unlockedMedia.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showAddMediaDialog by remember { mutableStateOf(false) }

    // Synchronize selected playlist state if lists change
    val activeSelectedPlaylist = playlists.find { it.id == selectedPlaylist?.id } ?: selectedPlaylist

    if (activeSelectedPlaylist != null) {
        // PLAYLIST DETAILS VIEW
        val playlist = activeSelectedPlaylist
        val itemIds = remember(playlist.mediaIdsCsv) {
            playlist.mediaIdsCsv.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }
        }
        val playlistMediaItems = remember(itemIds, mediaItems) {
            if (itemIds.isEmpty()) emptyList()
            else mediaItems.filter { itemIds.contains(it.id) }
        }

        val totalDurationMs = remember(playlistMediaItems) {
            playlistMediaItems.sumOf { it.durationMs }
        }
        val formattedTotalDuration = remember(totalDurationMs) {
            val minutes = totalDurationMs / (1000 * 60)
            val seconds = (totalDurationMs / 1000) % 60
            String.format("%02d:%02d", minutes, seconds)
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Back Arrow + Playlist Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playlist.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Button(
                    onClick = { showAddMediaDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Media", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playlist Info Banner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(android.graphics.Color.parseColor(playlist.coverColorHex))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${playlistMediaItems.size} Media Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Duration: $formattedTotalDuration",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    if (playlistMediaItems.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.playMedia(playlistMediaItems.first()) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playlistMediaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No items added to this playlist yet.", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddMediaDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("Select Media from Storage")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlistMediaItems, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { viewModel.playMedia(item) },
                            onRename = {},
                            onMoveToFolder = {},
                            onAddToPlaylist = {},
                            onLockToggle = { viewModel.lockMedia(item) },
                            onDelete = { viewModel.removeMediaFromPlaylist(playlist.id, item.id) },
                            onShare = {}
                        )
                    }
                }
            }
        }

        // Add Media Modal Dialog
        if (showAddMediaDialog) {
            AlertDialog(
                onDismissRequest = { showAddMediaDialog = false },
                title = { Text("Add Media to Playlist") },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        if (mediaItems.isEmpty()) {
                            Text("No unlocked media available. Scan device storage first.")
                        } else {
                            LazyColumn {
                                items(mediaItems, key = { it.id }) { item ->
                                    val isAdded = itemIds.contains(item.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isAdded) {
                                                    viewModel.removeMediaFromPlaylist(playlist.id, item.id)
                                                } else {
                                                    viewModel.addMediaToPlaylist(playlist.id, item.id)
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isAdded,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    viewModel.addMediaToPlaylist(playlist.id, item.id)
                                                } else {
                                                    viewModel.removeMediaFromPlaylist(playlist.id, item.id)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.mediaType.name} · ${item.formattedDuration} · ${item.resolutionOrBitrate}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAddMediaDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    } else {
        // MAIN PLAYLIST LIST
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: My Playlist (count) + "+ New" red pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Playlist (${playlists.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ New", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    val itemIds = remember(playlist.mediaIdsCsv) {
                        playlist.mediaIdsCsv.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }
                    }
                    val playlistMediaItems = remember(itemIds, mediaItems) {
                        if (itemIds.isEmpty()) emptyList()
                        else mediaItems.filter { itemIds.contains(it.id) }
                    }
                    val totalDurationMs = remember(playlistMediaItems) {
                        playlistMediaItems.sumOf { it.durationMs }
                    }
                    val formattedDuration = remember(totalDurationMs) {
                        if (totalDurationMs == 0L) "00:00"
                        else {
                            val mins = totalDurationMs / (1000 * 60)
                            val secs = (totalDurationMs / 1000) % 60
                            String.format("%02d:%02d", mins, secs)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaylist = playlist }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colored rounded square thumbnail
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(android.graphics.Color.parseColor(playlist.coverColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )

                                // Item count badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${itemIds.size} items",
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = playlist.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RedPrimary.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = playlist.type,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Total $formattedDuration",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Play Button
                            IconButton(
                                onClick = {
                                    if (playlistMediaItems.isNotEmpty()) {
                                        viewModel.playMedia(playlistMediaItems.first())
                                    } else {
                                        selectedPlaylist = playlist
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Playlist",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Options Menu
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open Playlist") },
                                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            selectedPlaylist = playlist
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", color = RedPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedPrimary) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.deletePlaylist(playlist.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Dashed CTA Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                            .clickable { showCreateDialog = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = RedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create New Playlist",
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Create New Playlist Dialog
        if (showCreateDialog) {
            var playlistName by remember { mutableStateOf("") }
            val colors = listOf("#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA")
            var selectedColor by remember { mutableStateOf(colors[0]) }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Playlist") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Cover Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            colors.forEach { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .clickable { selectedColor = colorHex }
                                        .border(
                                            width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                            color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                viewModel.createPlaylist(playlistName, selectedColor, "MIXED")
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
