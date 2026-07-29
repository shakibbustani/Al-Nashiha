package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FilterType
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCard
import com.example.ui.theme.RedPrimary

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.selectedFilter.collectAsState()
    val searchResults by viewModel.filteredMedia.collectAsState()

    val recentSearches = remember {
        mutableStateListOf("Quran Recitation", "Friday Bayan", "Zikr Track", "Seerat Series")
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!spoken.isNullOrEmpty()) {
            viewModel.searchQuery.value = spoken[0]
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("search_screen")
    ) {
        // Top Search Bar Row (Padded below status bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search video, audio, playlist...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RedPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (query.isNotEmpty() || activeFilter != FilterType.ALL) {
            // Live Search Results & Filtered items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESULTS (${searchResults.size}) · ${activeFilter.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary,
                    letterSpacing = 1.sp
                )

                if (activeFilter != FilterType.ALL) {
                    TextButton(onClick = { viewModel.selectedFilter.value = FilterType.ALL }) {
                        Text("Reset Filter", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No results match query or active filter", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { viewModel.playMedia(item) },
                            onRename = {},
                            onMoveToFolder = {},
                            onAddToPlaylist = {},
                            onLockToggle = { viewModel.lockMedia(item) },
                            onDelete = { viewModel.deleteMedia(item) },
                            onShare = {}
                        )
                    }
                }
            }
        } else {
            // Default Search Landing Page: Recent Searches + Trending + Category Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Recent Searches
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT SEARCHES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary,
                                    letterSpacing = 1.sp
                                )

                                TextButton(onClick = { recentSearches.clear() }) {
                                    Text("Clear All", color = Color.Gray, fontSize = 12.sp)
                                }
                            }

                            recentSearches.forEach { term ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.searchQuery.value = term }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = term,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { recentSearches.remove(term) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Remove",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Trending Now
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TRENDING NOW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val trendingChips = listOf("Quran Recitation", "Jummah Bayan", "Seerat Series", "Dua & Zikr")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trendingChips.forEach { chip ->
                                Surface(
                                    onClick = { viewModel.searchQuery.value = chip },
                                    shape = RoundedCornerShape(20.dp),
                                    color = RedPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = chip,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RedPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search by Category (Interactive Grid with Highlighting)
                item {
                    Column {
                        Text(
                            text = "SEARCH BY CATEGORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val categories = listOf(
                            Triple("Video", Icons.Default.Videocam, listOf(Color(0xFFE53935), Color(0xFFD81B60))),
                            Triple("Audio", Icons.Default.MusicNote, listOf(Color(0xFF1E88E5), Color(0xFF00ACC1))),
                            Triple("Folder", Icons.Default.Folder, listOf(Color(0xFF43A047), Color(0xFF1B5E20))),
                            Triple("Playlist", Icons.Default.PlaylistPlay, listOf(Color(0xFFFB8C00), Color(0xFFF57C00)))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CategoryCard(
                                    title = categories[0].first,
                                    icon = categories[0].second,
                                    gradient = categories[0].third,
                                    isSelected = activeFilter == FilterType.VIDEO,
                                    onClick = {
                                        viewModel.selectedFilter.value = if (activeFilter == FilterType.VIDEO) FilterType.ALL else FilterType.VIDEO
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                CategoryCard(
                                    title = categories[1].first,
                                    icon = categories[1].second,
                                    gradient = categories[1].third,
                                    isSelected = activeFilter == FilterType.AUDIO,
                                    onClick = {
                                        viewModel.selectedFilter.value = if (activeFilter == FilterType.AUDIO) FilterType.ALL else FilterType.AUDIO
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CategoryCard(
                                    title = categories[2].first,
                                    icon = categories[2].second,
                                    gradient = categories[2].third,
                                    isSelected = viewModel.currentBottomNav.value == 2,
                                    onClick = {
                                        onBack()
                                        viewModel.currentBottomNav.value = 2
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                CategoryCard(
                                    title = categories[3].first,
                                    icon = categories[3].second,
                                    gradient = categories[3].third,
                                    isSelected = viewModel.currentTopTab.value == 2,
                                    onClick = {
                                        onBack()
                                        viewModel.currentBottomNav.value = 0
                                        viewModel.currentTopTab.value = 2
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(2.2f)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) RedPrimary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = Color.White
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = RedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
