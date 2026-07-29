package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.MainViewModel
import com.example.ui.components.GradientPalettes
import com.example.ui.theme.RedPrimary

@OptIn(UnstableApi::class)
@Composable
fun ClipVideoPlayer(
    clip: MediaItem,
    isPlaying: Boolean,
    onPositionUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context) { ExoPlayer.Builder(context).build() }

    LaunchedEffect(clip.id, clip.uriString, clip.path) {
        val uriStr = clip.uriString.ifEmpty { clip.path }
        val media3Item = Media3Item.fromUri(uriStr)
        exoPlayer.setMediaItem(media3Item)
        exoPlayer.prepare()
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            kotlinx.coroutines.delay(300)
            if (exoPlayer.duration > 0) {
                val progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                onPositionUpdate(progress.coerceIn(0f, 1f))
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun ClipsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val mediaItems by viewModel.unlockedMedia.collectAsState()
    val shuffleTrigger by viewModel.clipsShuffleTrigger.collectAsState()

    // 1. Filter videos: 1 min (60,000 ms) to 4 mins (240,000 ms)
    val allVideoItems = remember(mediaItems) {
        mediaItems.filter { it.mediaType == MediaType.VIDEO }
    }
    val eligibleClips = remember(allVideoItems) {
        val filtered = allVideoItems.filter { item ->
            item.durationMs == 0L || item.durationMs in 60_000L..240_000L
        }
        if (filtered.isNotEmpty()) filtered else allVideoItems
    }

    // Top Tabs: 0 = "All Video", 1 = "Favorite", 2 = "For You" (Default is 2: "For You")
    var selectedTopTab by remember { mutableIntStateOf(2) }
    val topTabs = listOf("All Video", "Favorite", "For You")

    // Random shuffle state for For You tab
    var shuffledForYouIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Re-shuffle For You list on app open, bottom nav tab click (shuffleTrigger), or pull refresh
    LaunchedEffect(shuffleTrigger, eligibleClips) {
        val forYouPortraitClips = eligibleClips.filter { it.isPortrait }
        val clipsToShuffle = if (forYouPortraitClips.isNotEmpty()) forYouPortraitClips else eligibleClips
        shuffledForYouIds = clipsToShuffle.map { it.id }.shuffled()
    }

    // Active list based on selected top tab
    val currentDisplayClips = remember(selectedTopTab, eligibleClips, shuffledForYouIds) {
        when (selectedTopTab) {
            0 -> eligibleClips // All Video (Portrait + Landscape, 1-4 mins)
            1 -> eligibleClips.filter { it.isFavorite } // Favorite tab
            2 -> { // For You (Portrait, 1-4 mins, Shuffled)
                val map = eligibleClips.associateBy { it.id }
                val shuffledList = shuffledForYouIds.mapNotNull { map[it] }
                if (shuffledList.isNotEmpty()) shuffledList else eligibleClips.filter { it.isPortrait }.ifEmpty { eligibleClips }
            }
            else -> eligibleClips
        }
    }

    if (currentDisplayClips.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (selectedTopTab == 1) "No favorite clips saved yet." else "No video clips available (1-4 min).",
                    color = Color.White,
                    fontSize = 14.sp
                )
                if (selectedTopTab == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the bookmark icon on any clip to save it here.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { currentDisplayClips.size })

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("clips_screen")
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val clip = currentDisplayClips.getOrNull(page) ?: return@VerticalPager
                var showUIControls by remember { mutableStateOf(true) }
                var isPlaying by remember { mutableStateOf(true) }
                var sliderPosition by remember { mutableFloatStateOf(0.0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (!showUIControls) {
                                showUIControls = true
                            } else {
                                isPlaying = !isPlaying
                            }
                        }
                ) {
                    // Real ExoPlayer Video Player
                    ClipVideoPlayer(
                        clip = clip,
                        isPlaying = isPlaying,
                        onPositionUpdate = { pos -> sliderPosition = pos }
                    )

                    if (showUIControls) {
                        // Top & Bottom Gradient Overlays for high readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )

                        // Right Action Rail (Heart Like, Save Favorite, Hide UI, Refresh)
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp, bottom = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. Heart (Like) Icon - Starts at 0 or stored count, each click adds +1 to cumulative likes
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        viewModel.incrementLikes(clip.id)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                ) {
                                    Icon(
                                        imageVector = if (clip.likesCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (clip.likesCount > 0) RedPrimary else Color.White
                                    )
                                }
                                Text(
                                    text = "${clip.likesCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 2. Save / Bookmark Icon - Toggles favorite status for Favorite Tab
                            IconButton(
                                onClick = {
                                    viewModel.toggleFavorite(clip)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = if (clip.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save Favorite",
                                    tint = if (clip.isFavorite) RedPrimary else Color.White
                                )
                            }

                            // 3. Eye Icon (Hide UI) - Clears screen UI controls for unobstructed video viewing
                            IconButton(
                                onClick = { showUIControls = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide UI",
                                    tint = Color.White
                                )
                            }

                            // 4. Refresh / Reshuffle Button (For You Tab Refresh)
                            if (selectedTopTab == 2) {
                                IconButton(
                                    onClick = { viewModel.onClipsTabSelected() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Feed",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Share & More
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }

                        // Bottom-Left Video Title & Description Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, end = 90.dp, bottom = 48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RedPrimary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "@Clips By AL-NASHiHA",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = clip.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = clip.clipDescription.ifEmpty { "High quality offline clip playback." },
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }

                        // Bottom Seek Progress Bar
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(20.dp)
                                .padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = RedPrimary,
                                activeTrackColor = RedPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        // Floating Show UI button when UI is hidden
                        IconButton(
                            onClick = { showUIControls = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Show UI",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Top Navigation Bar with Tabs in Order: All Video | Favorite | For You
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                topTabs.forEachIndexed { index, title ->
                    val isSelected = selectedTopTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTopTab = index }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RedPrimary)
                        )
                    }
                }
            }
        }
    }
}
