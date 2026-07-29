package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.MainViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.MediaPlayerOverlay
import com.example.ui.components.TopNavBar
import com.example.ui.components.TopTabBar

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val topTab by viewModel.currentTopTab.collectAsState()
    val bottomNav by viewModel.currentBottomNav.collectAsState()
    val activePlayingMedia by viewModel.activePlayingMedia.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    if (isSearchActive) {
        SearchScreen(
            viewModel = viewModel,
            onBack = { isSearchActive = false }
        )
    } else {
        Scaffold(
            topBar = {
                if (bottomNav == 0) { // Only show Top Nav & Top Tabs on Home screen
                    Column {
                        TopNavBar(onSearchClick = { isSearchActive = true })
                        TopTabBar(
                            selectedTab = topTab,
                            onTabSelected = { viewModel.currentTopTab.value = it }
                        )
                    }
                }
            },
            bottomBar = {
                BottomNavBar(
                    selectedNav = bottomNav,
                    onNavSelected = { viewModel.currentBottomNav.value = it }
                )
            },
            modifier = modifier.testTag("home_screen")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (bottomNav) {
                    0 -> { // Home Tab
                        when (topTab) {
                            0 -> MediaTab(viewModel = viewModel)
                            1 -> SafeBoxTab(viewModel = viewModel)
                            2 -> PlaylistTab(viewModel = viewModel)
                            3 -> HistoryTab(viewModel = viewModel)
                        }
                    }
                    1 -> ClipsScreen(viewModel = viewModel)
                    2 -> FolderScreen(viewModel = viewModel)
                    3 -> SettingsScreen(viewModel = viewModel)
                }

                // Fullscreen Player Overlay if media is playing
                if (activePlayingMedia != null) {
                    MediaPlayerOverlay(viewModel = viewModel)
                }
            }
        }
    }
}
