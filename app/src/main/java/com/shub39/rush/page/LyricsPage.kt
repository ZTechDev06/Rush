package com.shub39.rush.page

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shub39.rush.R
import com.shub39.rush.component.ArtFromUrl
import com.shub39.rush.component.Empty
import com.shub39.rush.database.SettingsDataStore
import com.shub39.rush.listener.NotificationListener
import com.shub39.rush.viewmodel.RushViewModel
import kotlinx.coroutines.launch

@Composable
fun LyricsPage(
    rushViewModel: RushViewModel,
    lazyListState: LazyListState,
    bottomSheet: () -> Unit,
    bottomSheetAutofill: () -> Unit
) {
    val song by rushViewModel.currentSong.collectAsState()
    val fetching by rushViewModel.isFetchingLyrics.collectAsState()
    val context = LocalContext.current
    var isSharePageVisible by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf("") }
    var selectedLines by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val maxLinesFlow by SettingsDataStore.getMaxLinesFlow(context).collectAsState(initial = 6)
    val coroutineScope = rememberCoroutineScope()

    if (isSharePageVisible) {
        SharePage(
            onShare = { isSharePageVisible = false },
            onDismiss = { isSharePageVisible = false },
            song = song!!,
            selectedLines = selectedLines
        )
    }

    if (fetching) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeCap = StrokeCap.Round
                )
            }
        }
    } else if (song == null) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Empty()
        }
    } else {
        val nonNullSong = song!!

        LaunchedEffect(nonNullSong) {
            if (nonNullSong.lyrics.isNotEmpty()) {
                source = "LrcLib"
            } else if (nonNullSong.geniusLyrics != null) {
                source = "Genius"
            }
        }

        LaunchedEffect(key1 = source) {
            selectedLines = emptyMap()
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
            ) {
                ArtFromUrl(
                    imageUrl = nonNullSong.artUrl,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = nonNullSong.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nonNullSong.artists,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    nonNullSong.album?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                if (selectedLines.isEmpty()) {
                                    copyToClipBoard(
                                        context,
                                        nonNullSong.lyrics,
                                        "Complete Lyrics"
                                    )
                                } else {
                                    copyToClipBoard(
                                        context,
                                        selectedLines.toSortedMap().values.joinToString("\n"),
                                        "Selected Lyrics"
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.round_content_copy_24),
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = selectedLines.isEmpty()) {
                            IconButton(onClick = {
                                source = if (source == "LrcLib") "Genius" else "LrcLib"
                            }) {
                                if (source == "Genius") {
                                    Icon(
                                        painter = painterResource(id = R.drawable.round_lyrics_24),
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.genius),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(visible = selectedLines.isNotEmpty()) {
                            Row {
                                IconButton(onClick = { isSharePageVisible = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.round_share_24),
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = { selectedLines = emptyMap() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.round_delete_forever_24),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val lyrics =
                if (source == "LrcLib" && nonNullSong.lyrics.isNotEmpty()) {
                    breakLyrics(nonNullSong.lyrics).entries.toList()
                } else if (source == "Genius" && nonNullSong.geniusLyrics != null) {
                    breakLyrics(nonNullSong.geniusLyrics).entries.toList()
                } else {
                    emptyList()
                }

            LazyColumn(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp),
                state = lazyListState
            ) {
                items(lyrics, key = { it.key }) {
                    if (it.value.isNotBlank()) {
                        val isSelected = selectedLines.contains(it.key)
                        val color = if (!isSelected) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(3.dp),
                                onClick = {
                                    selectedLines = updateSelectedLines(
                                        selectedLines,
                                        it.key,
                                        it.value,
                                        maxLinesFlow
                                    )
                                    isSelected != isSelected
                                },
                                shape = MaterialTheme.shapes.small,
                                colors = color
                            ) {
                                Text(
                                    text = it.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }

                if (lyrics.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.padding(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        lazyListState.scrollToItem(0)
                                    }
                                },
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.round_arrow_upward_24),
                                    contentDescription = null,
                                )
                            }

                            FloatingActionButton(
                                onClick = { openLinkInBrowser(context, nonNullSong.sourceUrl) },
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.genius),
                                    contentDescription = null
                                )
                            }

                            if (NotificationListener.canAccessNotifications(context)) {
                                FloatingActionButton(
                                    onClick = { bottomSheetAutofill() },
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.round_play_arrow_24),
                                        contentDescription = null
                                    )
                                }
                            }

                            FloatingActionButton(
                                onClick = { bottomSheet() },
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.round_search_24),
                                    contentDescription = null
                                )
                            }

                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                    }
                }

                if (lyrics.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.padding(10.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.round_warning_24),
                                contentDescription = null,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.padding(10.dp))
                            Text(text = stringResource(id = R.string.no_lyrics))
                        }
                    }
                }

            }
        }

    }
}

private fun breakLyrics(lyrics: String): Map<Int, String> {
    val lines = lyrics.lines()
    val map = mutableMapOf<Int, String>()
    for (i in lines.indices) {
        map[i] = lines[i]
    }
    return map
}

private fun updateSelectedLines(
    selectedLines: Map<Int, String>,
    key: Int,
    value: String,
    maxSelections: Int = 6
): Map<Int, String> {
    return if (!selectedLines.contains(key) && selectedLines.size < maxSelections) {
        selectedLines.plus(key to value)
    } else {
        selectedLines.minus(key)
    }
}

private fun copyToClipBoard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}