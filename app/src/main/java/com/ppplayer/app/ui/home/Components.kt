package com.ppplayer.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.ppplayer.app.R
import com.ppplayer.app.data.MediaFile
import com.ppplayer.app.data.MediaFolder
import com.ppplayer.app.ui.theme.Brand
import com.ppplayer.app.ui.theme.TABULAR
import com.ppplayer.app.util.formatDuration
import com.ppplayer.app.util.formatSize
import com.ppplayer.app.util.ltr
import java.text.DateFormat
import java.util.Date

/** File operations the screens delegate to the activity. */
interface MediaActions {
    fun requestLibraryAccess()
    fun openAppSettings()
    fun share(file: MediaFile)
    fun delete(file: MediaFile)
    fun rename(file: MediaFile, newTitle: String)
    val canRename: Boolean
}

/** A video frame thumbnail with a soft placeholder while it loads. */
@Composable
fun VideoThumbnail(file: MediaFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(file.uri) {
        ImageRequest.Builder(context)
            .data(file.uri)
            .videoFrameMillis((file.durationMs / 8).coerceIn(0L, 15_000L))
            .crossfade(true)
            .build()
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun DurationBadge(durationMs: Long, modifier: Modifier = Modifier) {
    if (durationMs <= 0) return
    Text(
        formatDuration(durationMs),
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR),
    )
}

/** Watched-progress line drawn in the brand gradient. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    val gradient = Brand.colors.gradient
    val track = Color.White.copy(alpha = 0.28f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(gradient, size = Size(size.width * progress.coerceIn(0f, 1f), size.height), cornerRadius = radius)
    }
}

@Composable
fun VideoListRow(
    file: MediaFile,
    progress: Float,
    isNew: Boolean,
    onClick: () -> Unit,
    actions: MediaActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
        ) {
            VideoThumbnail(file, Modifier.fillMaxSize())
            DurationBadge(
                file.durationMs,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp),
            )
            if (progress > 0f) ProgressLine(progress, Modifier.align(Alignment.BottomCenter))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isNew) NewBadge()
                Text(
                    listOfNotNull(file.resolutionLabel, ltr(formatSize(file.sizeBytes))).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        MediaMenu(file, onClick, actions)
    }
}

@Composable
fun VideoGridCard(
    file: MediaFile,
    progress: Float,
    isNew: Boolean,
    onClick: () -> Unit,
    actions: MediaActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(MaterialTheme.shapes.medium)
        ) {
            VideoThumbnail(file, Modifier.fillMaxSize())
            DurationBadge(
                file.durationMs,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            )
            if (isNew) NewBadge(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
            if (progress > 0f) ProgressLine(progress, Modifier.align(Alignment.BottomCenter))
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 8.dp)
            ) {
                Text(file.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(file.resolutionLabel, ltr(formatSize(file.sizeBytes))).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            MediaMenu(file, onClick, actions)
        }
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.new_badge),
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        color = MaterialTheme.colorScheme.onTertiary,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun FolderRow(folder: MediaFolder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stacked covers: the folder reads as a pile of videos rather than a generic folder glyph.
        Box(Modifier.size(width = 92.dp, height = 62.dp)) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 72.dp, height = 44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 5.dp)
                    .size(width = 82.dp, height = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
            VideoThumbnail(
                folder.files.first(),
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 92.dp, height = 52.dp)
                    .clip(RoundedCornerShape(11.dp)),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                pluralStringResource(R.plurals.video_count, folder.files.size, folder.files.size) +
                    " · " + ltr(formatSize(folder.totalSize)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AudioRow(
    file: MediaFile,
    onClick: () -> Unit,
    actions: MediaActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(file.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                (file.artist ?: stringResource(R.string.unknown_artist)) + " · " + formatDuration(file.durationMs),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MediaMenu(file, onClick, actions)
    }
}

@Composable
private fun MediaMenu(file: MediaFile, onPlay: () -> Unit, actions: MediaActions) {
    var open by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(0) } // 1 details, 2 delete, 3 rename
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun item(icon: ImageVector, label: Int, action: () -> Unit) = DropdownMenuItem(
                text = { Text(stringResource(label)) },
                leadingIcon = { Icon(icon, null) },
                onClick = {
                    open = false
                    action()
                },
            )
            item(Icons.Rounded.PlayArrow, R.string.action_play, onPlay)
            item(Icons.Rounded.Share, R.string.action_share) { actions.share(file) }
            if (actions.canRename) item(Icons.Rounded.Edit, R.string.action_rename) { dialog = 3 }
            item(Icons.Rounded.Delete, R.string.action_delete) { dialog = 2 }
            item(Icons.Rounded.Info, R.string.action_info) { dialog = 1 }
        }
    }
    when (dialog) {
        1 -> DetailsDialog(file) { dialog = 0 }
        2 -> AlertDialog(
            onDismissRequest = { dialog = 0 },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.delete_confirm, file.title)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = 0
                    actions.delete(file)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = 0 }) { Text(stringResource(R.string.cancel)) } },
        )
        3 -> {
            var name by remember { mutableStateOf(file.title) }
            AlertDialog(
                onDismissRequest = { dialog = 0 },
                title = { Text(stringResource(R.string.rename_title)) },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
                confirmButton = {
                    TextButton(
                        enabled = name.isNotBlank() && name.trim() != file.title,
                        onClick = {
                            dialog = 0
                            actions.rename(file, name)
                        },
                    ) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = { TextButton(onClick = { dialog = 0 }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun DetailsDialog(file: MediaFile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine(R.string.info_name, file.name)
                if (file.path.isNotEmpty()) DetailLine(R.string.info_location, file.path.substringBeforeLast('/'))
                DetailLine(R.string.info_size, ltr(formatSize(file.sizeBytes)))
                if (file.durationMs > 0) DetailLine(R.string.info_duration, formatDuration(file.durationMs))
                if (file.width > 0 && file.height > 0) DetailLine(R.string.info_resolution, ltr("${file.width} × ${file.height}"))
                if (file.dateAddedSec > 0) {
                    DetailLine(
                        R.string.info_date,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(file.dateAddedSec * 1000)),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun DetailLine(label: Int, value: String) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Centered message for empty and permission states, with an optional call to action. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showLogo) {
            Image(
                painterResource(R.drawable.brand_logo),
                null,
                Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(30.dp)),
            )
        } else {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            )
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            Modifier.widthIn(max = 320.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 50.dp)) {
                Text(actionLabel, Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}
