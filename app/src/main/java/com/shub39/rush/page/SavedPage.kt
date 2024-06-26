package com.shub39.rush.page

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.ImageLoader
import com.shub39.rush.component.Empty
import com.shub39.rush.component.SongCard
import com.shub39.rush.viewmodel.RushViewModel

@Composable
fun SavedPage(
    navController: NavController,
    rushViewModel: RushViewModel,
    imageLoader: ImageLoader
) {
    val songs = rushViewModel.songs.collectAsState()

    if (songs.value.isEmpty()) {
        Empty()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
        ) {
            items(songs.value, key = { it.id }) {
                SongCard(
                    result = it,
                    onDelete = {
                        rushViewModel.deleteSong(it)
                    },
                    onClick = {
                        rushViewModel.changeCurrentSong(it.id)
                        navController.navigate(Screens.LyricsPage.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    imageLoader = imageLoader
                )
            }
        }
    }
}