package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.theme.RedPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val GradientPalettes = listOf(
    listOf(Color(0xFFE53935), Color(0xFF8E24AA)),
    listOf(Color(0xFF1E88E5), Color(0xFF00ACC1)),
    listOf(Color(0xFF43A047), Color(0xFF1B5E20)),
    listOf(Color(0xFFFB8C00), Color(0xFFD81B60)),
    listOf(Color(0xFF3949AB), Color(0xFF8E24AA))
)

@Composable
fun MediaThumbnailImage(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (item.mediaType == MediaType.VIDEO) {
        val mediaUri = remember(item.uriString, item.path) {
            item.uriString.ifEmpty { item.path }
        }
        val request = remember(mediaUri) {
            ImageRequest.Builder(context)
                .data(mediaUri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(1000)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = "Video Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        var audioArtBitmap by remember(item.uriString, item.path) { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(item.uriString, item.path) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    val targetPath = item.uriString.ifEmpty { item.path }
                    if (targetPath.startsWith("content://")) {
                        retriever.setDataSource(context, android.net.Uri.parse(targetPath))
                    } else if (targetPath.startsWith("http://") || targetPath.startsWith("https://")) {
                        retriever.setDataSource(targetPath, HashMap())
                    } else if (targetPath.isNotBlank()) {
                        retriever.setDataSource(targetPath)
                    }
                    val artBytes = retriever.embeddedPicture
                    retriever.release()
                    if (artBytes != null) {
                        audioArtBitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    }
                } catch (_: Exception) {}
            }
        }

        audioArtBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Audio Art",
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMoveToFolder: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val gradientColors = GradientPalettes[item.thumbnailGradientIndex % GradientPalettes.size]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("media_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail Box
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                // Real Media Thumbnail (Video Frame or Audio Art)
                MediaThumbnailImage(item = item)
                // Diagonal stripes if locked
                if (item.isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }

                // Type Badge (top left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.mediaType == MediaType.VIDEO) RedPrimary else Color(
                                0xFF1E88E5
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.mediaType == MediaType.VIDEO) "VIDEO" else "AUDIO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Centered Icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isLocked) Icons.Default.Lock
                        else if (item.mediaType == MediaType.VIDEO) Icons.Default.PlayArrow
                        else Icons.Default.MusicNote,
                        contentDescription = "Play/Type",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration Badge (bottom right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.formattedDuration,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Thin progress bar at bottom of thumbnail
                if (item.progressRatio > 0f && !item.isLocked) {
                    LinearProgressIndicator(
                        progress = { item.progressRatio },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = RedPrimary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Meta Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedSize,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = " · ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = item.dateGroup,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Quality Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.resolutionOrBitrate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = RedPrimary
                    )
                }
            }

            // Three Dot Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("menu_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveToFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isLocked) "Unlock" else "Lock (Safe Box)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onLockToggle()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = RedPrimary) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedPrimary) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
