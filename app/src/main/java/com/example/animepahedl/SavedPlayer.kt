package com.example.animepahedl

import androidx.core.content.edit
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
internal fun SavedPlayer(id: String, rows: List<DownloadRow>, onClose: () -> Unit, onNext: (String) -> Unit) {
    val context = LocalContext.current
    val row = rows.firstOrNull { it.id == id }
    val positions = remember { context.getSharedPreferences("playback", android.content.Context.MODE_PRIVATE) }
    var failure by remember(id) { mutableStateOf<String?>(null) }
    val next = rows.filter { it.complete && it.showId.isNotBlank() && it.showId == row?.showId && it.audioCode == row.audioCode && it.quality == row.quality &&
        (it.episodeNumber.toDoubleOrNull() ?: -1.0) > (row.episodeNumber.toDoubleOrNull() ?: Double.MAX_VALUE) }
        .minByOrNull { it.episodeNumber.toDoubleOrNull() ?: Double.MAX_VALUE }
    val player = remember(id) {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) { failure = "Could not play this file. Relink it from Downloads if it moved. ${error.errorCodeName}" }
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) positions.edit { remove(id) } }
            })
            if (row?.savedUri?.isNotBlank() == true) { setMediaItem(MediaItem.fromUri(row.savedUri)); seekTo(positions.getLong(id, 0)); prepare(); playWhenReady = true }
            else failure = "The video is not saved yet."
        }
    }
    LaunchedEffect(player) { while (true) { delay(5000); if (player.playbackState != Player.STATE_ENDED) positions.edit { putLong(id, player.currentPosition) } } }
    DisposableEffect(player) {
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) player.pause() }
        lifecycle?.addObserver(observer)
        onDispose {
        lifecycle?.removeObserver(observer)
        if (player.playbackState != Player.STATE_ENDED) positions.edit { putLong(id, player.currentPosition) }
        player.release()
    } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding()) {
            Row { TextButton(onClick = onClose) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close_player)) }; next?.let { episode -> TextButton(onClick = { onNext(episode.id) }) { Text("Next episode ${episode.episodeNumber}") } } }
            Text(row?.title.orEmpty(), Modifier.padding(horizontal = 16.dp))
            failure?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            AndroidView(factory = { PlayerView(it).apply { this.player = player } }, update = { it.player = player }, modifier = Modifier.fillMaxWidth().weight(1f))
        } }
    }
}
