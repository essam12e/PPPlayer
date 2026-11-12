package com.ppplayer.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.ppplayer.app.R
import com.ppplayer.app.data.HistoryEntry
import com.ppplayer.app.data.MediaFile
import com.ppplayer.app.data.MediaFolder
import com.ppplayer.app.data.Prefs
import com.ppplayer.app.data.Settings
import com.ppplayer.app.data.SortField
import com.ppplayer.app.data.groupIntoFolders
import com.ppplayer.app.data.matching
import com.ppplayer.app.data.sortedBy
import com.ppplayer.app.player.PlayerActivity
import com.ppplayer.app.player.PlayerHolder
import com.ppplayer.app.player.PlayerUiState
import com.ppplayer.app.player.QueueItem
import com.ppplayer.app.ui.theme.Brand
import com.ppplayer.app.ui.theme.TABULAR
import com.ppplayer.app.util.formatDuration
import com.ppplayer.app.util.normalizeStreamUrl

private enum class Tab(val label: Int, val icon: ImageVector) {
    VIDEOS(R.string.tab_videos, Icons.Rounded.VideoLibrary),
    MUSIC(R.string.tab_music, Icons.Rounded.LibraryMusic),
    RECENT(R.string.tab_recent, Icons.Rounded.History),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Tune),
}

private fun MediaFile.toQueueItem() = QueueItem(uri.toString(), title, isAudio)

private const val NEW_WINDOW_SEC = 3 * 24 * 3600L

@Composable
fun HomeScreen(library: LibraryViewModel, prefs: Prefs, actions: MediaActions) {
    val context = LocalContext.current
    val state by library.state.collectAsState()
    val settings by prefs.settings.collectAsState()
    val history by prefs.history.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.VIDEOS) }
    var openFolder by rememberSaveable { mutableStateOf<Long?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var streamDialog by rememberSaveable { mutableStateOf(false) }

    val sortedVideos = remember(state.videos, settings.sortField, settings.sortDescending) {
        state.videos.sortedBy(settings.sortField, settings.sortDescending)
    }
    val folders = remember(sortedVideos) { sortedVideos.groupIntoFolders() }
    val sortedAudio = remember(state.audio, settings.sortField, settings.sortDescending) {
        state.audio.sortedBy(settings.sortField, settings.sortDescending)
    }
    val progressByUri = remember(history) { history.associate { it.uri to it.progress } }
    val play: (List<MediaFile>, Int) -> Unit = { files, index ->
        PlayerActivity.start(context, files.map { it.toQueueItem() }, index)
    }

    BackHandler(enabled = searching || openFolder != null || tab != Tab.VIDEOS) {
        when {
            searching -> searching = false
            openFolder != null -> openFolder = null
            else -> tab = Tab.VIDEOS
        }
    }

    val background = MaterialTheme.colorScheme.background
    val glow = Brand.colors.glow
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .background(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.34f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(120f, -80f),
                    radius = 900f,
                )
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(Modifier.weight(1f)) {
                when {
                    searching -> SearchScreen(
                        videos = sortedVideos,
                        audio = sortedAudio,
                        progressByUri = progressByUri,
                        onClose = { searching = false },
                        onPlay = play,
                        actions = actions,
                    )
                    openFolder != null -> {
                        val folder = folders.firstOrNull { it.id == openFolder }
                        if (folder == null) {
                            LaunchedEffect(Unit) { openFolder = null }
                        } else {
                            FolderScreen(folder, settings, prefs, progressByUri, { openFolder = null }, play, actions)
                        }
                    }
                    else -> AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab",
                    ) { current ->
                        when (current) {
                            Tab.VIDEOS -> VideosTab(
                                state = state,
                                videos = sortedVideos,
                                folders = folders,
                                settings = settings,
                                prefs = prefs,
                                history = history,
                                progressByUri = progressByUri,
                                onSearch = { searching = true },
                                onStream = { streamDialog = true },
                                onOpenFolder = { openFolder = it },
                                onPlay = play,
                                actions = actions,
                            )
                            Tab.MUSIC -> MusicTab(state, sortedAudio, settings, prefs, { searching = true }, play, actions)
                            Tab.RECENT -> RecentTab(history, state, prefs)
                            Tab.SETTINGS -> SettingsScreen(settings, prefs)
                        }
                    }
                }
            }
            MiniPlayer()
            if (!searching) BottomBar(tab) {
                openFolder = null
                tab = it
            }
        }
        }
    }

    if (streamDialog) StreamDialog(
        onDismiss = { streamDialog = false },
        onOpen = { url ->
            streamDialog = false
            val title = url.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { url }
            PlayerActivity.start(context, listOf(QueueItem(url, title, false)), 0)
        },
    )
}

// ---- Videos ------------------------------------------------------------------------------------

@Composable
private fun VideosTab(
    state: LibraryState,
    videos: List<MediaFile>,
    folders: List<MediaFolder>,
    settings: Settings,
    prefs: Prefs,
    history: List<HistoryEntry>,
    progressByUri: Map<String, Float>,
    onSearch: () -> Unit,
    onStream: () -> Unit,
    onOpenFolder: (Long) -> Unit,
    onPlay: (List<MediaFile>, Int) -> Unit,
    actions: MediaActions,
) {
    Column(Modifier.fillMaxSize()) {
        BrandHeader(onSearch = onSearch, onStream = onStream)
        when {
            !state.hasPermission -> PermissionState(actions)
            state.loading -> Loading()
            videos.isEmpty() -> EmptyState(
                stringResource(R.string.empty_videos_title),
                stringResource(R.string.empty_videos_body),
            )
            else -> {
                val byUri = remember(videos) { videos.associateBy { it.uri.toString() } }
                val resume = remember(history, byUri) {
                    history.firstOrNull { !it.isAudio && it.positionMs > 0 && byUri.containsKey(it.uri) }
                }
                val columns = if (settings.gridView && !settings.showFolders) 2 else 1
                val nowSec = remember { System.currentTimeMillis() / 1000 }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (resume != null) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "resume") {
                            val file = byUri.getValue(resume.uri)
                            ContinueCard(file, resume) { onPlay(listOf(file), 0) }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "toolbar") {
                        LibraryToolbar(settings, prefs, showFolderToggle = true)
                    }
                    if (settings.showFolders) {
                        items(folders, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { folder ->
                            FolderRow(folder, onClick = { onOpenFolder(folder.id) })
                        }
                    } else if (columns == 1) {
                        itemsIndexed(videos, key = { _, f -> f.id }) { index, file ->
                            VideoListRow(
                                file, progressByUri[file.uri.toString()] ?: 0f,
                                isNew = nowSec - file.dateAddedSec < NEW_WINDOW_SEC && !progressByUri.containsKey(file.uri.toString()),
                                onClick = { onPlay(videos, index) }, actions = actions,
                            )
                        }
                    } else {
                        itemsIndexed(videos, key = { _, f -> f.id }) { index, file ->
                            VideoGridCard(
                                file, progressByUri[file.uri.toString()] ?: 0f,
                                isNew = nowSec - file.dateAddedSec < NEW_WINDOW_SEC && !progressByUri.containsKey(file.uri.toString()),
                                onClick = { onPlay(videos, index) }, actions = actions,
                                modifier = Modifier.padding(
                                    start = if (index % 2 == 0) 16.dp else 0.dp,
                                    end = if (index % 2 == 1) 16.dp else 0.dp,
                                    bottom = 12.dp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(onSearch: () -> Unit, onStream: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.brand_logo),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(brush = Brand.colors.gradient)) { append("PP") }
                append("Player")
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onStream) { Icon(Icons.Rounded.Link, stringResource(R.string.network_stream)) }
        IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.search)) }
    }
}

/** The screen's focal point: the last unfinished video, ready to resume. */
@Composable
private fun ContinueCard(file: MediaFile, entry: HistoryEntry, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .aspectRatio(16f / 8.6f)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        VideoThumbnail(file, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f),
                    )
                )
        )
        Text(
            stringResource(R.string.continue_watching),
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        file.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.time_left, formatDuration(entry.durationMs - entry.positionMs)),
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brand.colors.gradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        stringResource(R.string.resume_from, formatDuration(entry.positionMs)),
                        tint = Brand.colors.onGradient,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine(entry.progress, height = 4.dp)
        }
    }
}

@Composable
private fun LibraryToolbar(settings: Settings, prefs: Prefs, showFolderToggle: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFolderToggle) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp)
            ) {
                Segment(stringResource(R.string.folders), settings.showFolders) {
                    prefs.update { it.copy(showFolders = true) }
                }
                Segment(stringResource(R.string.all_videos), !settings.showFolders) {
                    prefs.update { it.copy(showFolders = false) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (!showFolderToggle || !settings.showFolders) {
            IconButton(onClick = { prefs.update { it.copy(gridView = !it.gridView) } }) {
                Icon(
                    if (settings.gridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                    stringResource(if (settings.gridView) R.string.view_list else R.string.view_grid),
                )
            }
        }
        SortMenu(settings, prefs)
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "segment"
    )
    Text(
        label,
        Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SortMenu(settings: Settings, prefs: Prefs) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                SortField.NAME to R.string.sort_name,
                SortField.DATE to R.string.sort_date,
                SortField.SIZE to R.string.sort_size,
                SortField.DURATION to R.string.sort_duration,
            ).forEach { (field, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    trailingIcon = { if (settings.sortField == field) Icon(Icons.Rounded.Check, null) },
                    onClick = {
                        prefs.update { it.copy(sortField = field) }
                        open = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_ascending)) },
                trailingIcon = { if (!settings.sortDescending) Icon(Icons.Rounded.Check, null) },
                onClick = {
                    prefs.update { it.copy(sortDescending = false) }
                    open = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_descending)) },
                trailingIcon = { if (settings.sortDescending) Icon(Icons.Rounded.Check, null) },
                onClick = {
                    prefs.update { it.copy(sortDescending = true) }
                    open = false
                },
            )
        }
    }
}

@Composable
private fun FolderScreen(
    folder: MediaFolder,
    settings: Settings,
    prefs: Prefs,
    progressByUri: Map<String, Float>,
    onBack: () -> Unit,
    onPlay: (List<MediaFile>, Int) -> Unit,
    actions: MediaActions,
) {
    val nowSec = remember { System.currentTimeMillis() / 1000 }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Column(Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.video_count, folder.files.size, folder.files.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { prefs.update { it.copy(gridView = !it.gridView) } }) {
                Icon(
                    if (settings.gridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                    stringResource(if (settings.gridView) R.string.view_list else R.string.view_grid),
                )
            }
            SortMenu(settings, prefs)
        }
        val files = folder.files
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (settings.gridView) 2 else 1),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(files, key = { _, f -> f.id }) { index, file ->
                val progress = progressByUri[file.uri.toString()] ?: 0f
                val isNew = nowSec - file.dateAddedSec < NEW_WINDOW_SEC && !progressByUri.containsKey(file.uri.toString())
                if (settings.gridView) {
                    VideoGridCard(
                        file, progress, isNew, { onPlay(files, index) }, actions,
                        Modifier.padding(
                            start = if (index % 2 == 0) 16.dp else 0.dp,
                            end = if (index % 2 == 1) 16.dp else 0.dp,
                            bottom = 12.dp,
                        ),
                    )
                } else {
                    VideoListRow(file, progress, isNew, { onPlay(files, index) }, actions)
                }
            }
        }
    }
}

// ---- Music -------------------------------------------------------------------------------------

@Composable
private fun MusicTab(
    state: LibraryState,
    audio: List<MediaFile>,
    settings: Settings,
    prefs: Prefs,
    onSearch: () -> Unit,
    onPlay: (List<MediaFile>, Int) -> Unit,
    actions: MediaActions,
) {
    Column(Modifier.fillMaxSize()) {
        TabHeader(
            title = stringResource(R.string.tab_music),
            subtitle = if (audio.isNotEmpty()) pluralStringResource(R.plurals.track_count, audio.size, audio.size) else null,
        ) {
            IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.search)) }
            SortMenu(settings, prefs)
        }
        when {
            !state.hasPermission -> PermissionState(actions)
            state.loading -> Loading()
            audio.isEmpty() -> EmptyState(
                stringResource(R.string.empty_music_title),
                stringResource(R.string.empty_music_body),
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(audio, key = { _, f -> f.id }) { index, file ->
                    AudioRow(file, onClick = { onPlay(audio, index) }, actions = actions)
                }
            }
        }
    }
}

@Composable
internal fun TabHeader(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 6.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

// ---- Recent ------------------------------------------------------------------------------------

@Composable
private fun RecentTab(history: List<HistoryEntry>, state: LibraryState, prefs: Prefs) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val videosByUri = remember(state.videos) { state.videos.associateBy { it.uri.toString() } }
    Column(Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.tab_recent)) {
            if (history.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.clear_history))
                }
            }
        }
        if (history.isEmpty()) {
            EmptyState(stringResource(R.string.empty_recent_title), stringResource(R.string.empty_recent_body))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.uri }) { entry ->
                    RecentRow(entry, videosByUri[entry.uri]) {
                        PlayerActivity.start(context, listOf(QueueItem(entry.uri, entry.title, entry.isAudio)), 0)
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.clear_history)) },
        text = { Text(stringResource(R.string.clear_history_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                confirmClear = false
                prefs.clearHistory()
            }) { Text(stringResource(R.string.clear_history), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RecentRow(entry: HistoryEntry, file: MediaFile?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(112.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (file != null) {
                VideoThumbnail(file, Modifier.fillMaxSize())
            } else {
                Icon(
                    if (entry.isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.progress > 0f) ProgressLine(entry.progress, Modifier.align(Alignment.BottomCenter))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                if (entry.durationMs > 0) formatDuration(entry.positionMs) + " / " + formatDuration(entry.durationMs)
                else formatDuration(entry.positionMs),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Search ------------------------------------------------------------------------------------

@Composable
private fun SearchScreen(
    videos: List<MediaFile>,
    audio: List<MediaFile>,
    progressByUri: Map<String, Float>,
    onClose: () -> Unit,
    onPlay: (List<MediaFile>, Int) -> Unit,
    actions: MediaActions,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val videoHits = remember(query, videos) { if (query.isBlank()) emptyList() else videos.matching(query) }
    val audioHits = remember(query, audio) { if (query.isBlank()) emptyList() else audio.matching(query) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.close))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }
        if (query.isNotBlank() && videoHits.isEmpty() && audioHits.isEmpty()) {
            EmptyState(stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_body))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(videoHits, key = { _, f -> "v${f.id}" }) { index, file ->
                    VideoListRow(
                        file, progressByUri[file.uri.toString()] ?: 0f, false,
                        onClick = { onPlay(videoHits, index) }, actions = actions,
                    )
                }
                itemsIndexed(audioHits, key = { _, f -> "a${f.id}" }) { index, file ->
                    AudioRow(file, onClick = { onPlay(audioHits, index) }, actions = actions)
                }
            }
        }
    }
}

// ---- Shared pieces -----------------------------------------------------------------------------

@Composable
private fun PermissionState(actions: MediaActions) {
    EmptyState(
        title = stringResource(R.string.permission_title),
        body = stringResource(R.string.permission_body),
        showLogo = true,
        actionLabel = stringResource(R.string.permission_grant),
        onAction = actions::requestLibraryAccess,
    )
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun StreamDialog(onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val submit = {
        val clean = normalizeStreamUrl(url)
        if (clean != null) onOpen(clean) else showError = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_stream)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    showError = false
                },
                placeholder = { Text("https://") },
                supportingText = {
                    Text(stringResource(if (showError) R.string.network_stream_invalid else R.string.network_stream_hint))
                },
                isError = showError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
            )
        },
        confirmButton = { TextButton(onClick = submit) { Text(stringResource(R.string.play)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shown while something keeps playing outside the player screen. */
@Composable
private fun MiniPlayer() {
    val context = LocalContext.current
    val player by PlayerHolder.player.collectAsState()
    val current: ExoPlayer = player ?: return
    val state = remember(current) { PlayerUiState(current) }
    DisposableEffect(current) {
        current.addListener(state)
        onDispose { current.removeListener(state) }
    }
    LaunchedEffect(current, state.isPlaying) {
        while (state.isPlaying) {
            state.tick()
            kotlinx.coroutines.delay(500)
        }
    }
    if (state.count == 0) return
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { PlayerActivity.resume(context) }
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { if (current.isPlaying) current.pause() else current.play() }) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                )
            }
            IconButton(onClick = { PlayerHolder.release() }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close))
            }
        }
        if (state.durationMs > 0) ProgressLine(state.positionMs.toFloat() / state.durationMs, height = 2.dp)
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(66.dp),
        ) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                val tint by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabTint",
                )
                val indicator by animateDpAsState(
                    if (active) 28.dp else 0.dp,
                    spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "tabIndicator",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(role = Role.Tab) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(indicator)
                            .height(3.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(Brand.colors.gradient)
                    )
                    Spacer(Modifier.height(9.dp))
                    Icon(tab.icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(tab.label),
                        color = tint,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
