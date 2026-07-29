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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
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
    val videoClips = remember(mediaItems) {
        mediaItems.filter { it.mediaType == MediaType.VIDEO }
    }

    var selectedTopTab by remember { mutableIntStateOf(0) } // 0=All Video, 1=Favorite, 2=For You
    val topTabs = listOf("All Video", "Favorite", "For You")

    if (videoClips.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No video clips available.", color = Color.White)
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { videoClips.size })

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
                val clip = videoClips[page]
                var isLiked by remember { mutableStateOf(clip.isFavorite) }
                var likeCount by remember { mutableIntStateOf(clip.likesCount) }
                var isSaved by remember { mutableStateOf(false) }
                var showUIControls by remember { mutableStateOf(true) }
                var isPlaying by remember { mutableStateOf(true) }
                var sliderPosition by remember { mutableFloatStateOf(0.35f) }

                val gradientColors = GradientPalettes[clip.thumbnailGradientIndex % GradientPalettes.size]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isPlaying = !isPlaying }
                ) {
                    // Real ExoPlayer Video Canvas
                    ClipVideoPlayer(
                        clip = clip,
                        isPlaying = isPlaying,
                        onPositionUpdate = { pos -> sliderPosition = pos }
                    )

                    if (showUIControls) {
                        // Gradient Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // Right Action Rail
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp, bottom = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Like
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        isLiked = !isLiked
                                        if (isLiked) likeCount++ else likeCount--
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) RedPrimary else Color.White
                                    )
                                }
                                Text(
                                    text = "$likeCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Save
                            IconButton(
                                onClick = { isSaved = !isSaved },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = if (isSaved) RedPrimary else Color.White
                                )
                            }

                            // Toggle UI Clear
                            IconButton(
                                onClick = { showUIControls = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide UI",
                                    tint = Color.White
                                )
                            }

                            // Share
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }

                            // More
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        }

                        // Bottom-Left Overlay
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

                        // Bottom Draggable Seek Bar
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
                        // Unhide UI floating button
                        IconButton(
                            onClick = { showUIControls = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
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

            // Top Translucent Navigation Tabs
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                topTabs.forEachIndexed { index, title ->
                    val isSelected = selectedTopTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTopTab = index }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.LightGray,
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
