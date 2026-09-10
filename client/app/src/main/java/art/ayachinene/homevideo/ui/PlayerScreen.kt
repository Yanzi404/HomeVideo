package art.ayachinene.homevideo.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.compose.material3.AlertDialog
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import art.ayachinene.homevideo.HomeVideoApp
import art.ayachinene.homevideo.data.model.AudioTrack
import art.ayachinene.homevideo.data.model.SubtitleInfo
import art.ayachinene.homevideo.data.remote.ApiClient
import art.ayachinene.homevideo.ui.theme.CSGOColors
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Composable
fun PlayerScreen(
    videoPath: String,
    videoTitle: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(videoPath, videoTitle) {
        viewModel.init(videoPath, videoTitle)
    }

    val streamUrl = ApiClient.getBaseUrl() + "api/videos/stream?path=" + Uri.encode(videoPath)

    val player = remember {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(HomeVideoApp.instance)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply { playWhenReady = true }
    }

    val playerView = remember {
        androidx.media3.ui.PlayerView(HomeVideoApp.instance).apply {
            this.player = player
            useController = true
            controllerAutoShow = false
            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(player, streamUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> viewModel.showEndCard()
                    Player.STATE_READY -> {
                        viewModel.updateDuration(player.duration)
                        viewModel.updatePlaying(player.playWhenReady)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlaying(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.updatePosition(0)
            }
        }
        player.addListener(listener)

        val subtitleConfigs = uiState.videoInfo?.subtitles?.mapNotNull { sub ->
            val mime = when {
                sub.name.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
                sub.name.endsWith(".ass") || sub.name.endsWith(".ssa") -> MimeTypes.TEXT_SSA
                sub.name.endsWith(".vtt") -> MimeTypes.TEXT_VTT
                else -> return@mapNotNull null
            }
            val subtitleUrl = ApiClient.getBaseUrl() + "api/videos/subtitles?path=" +
                    Uri.encode(videoPath) + "&name=" + Uri.encode(sub.name)
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(mime)
                .setLanguage(sub.language.ifBlank { "und" })
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        } ?: emptyList()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        player.setMediaItem(mediaItem)
        if (uiState.resumePosition > 0) {
            player.seekTo(uiState.resumePosition)
        }
        player.prepare()
        viewModel.startHistoryTracking()

        onDispose {
            player.removeListener(listener)
            viewModel.saveFinalPosition()
            viewModel.stopHistoryTracking()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(1000)
            if (player.isPlaying) {
                viewModel.updatePosition(player.currentPosition)
            }
        }
    }

    LaunchedEffect(uiState.playbackSpeed) {
        player.setPlaybackSpeed(uiState.playbackSpeed)
    }

    LaunchedEffect(uiState.showSettingsDialog) {
        if (uiState.showSettingsDialog) {
            // Dialog handles its own focus
        } else {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.Back -> {
                        if (uiState.showSettingsDialog) {
                            viewModel.toggleSettingsDialog()
                        } else if (uiState.showControls || uiState.showEndCard) {
                            viewModel.hideControls()
                        } else {
                            onBack()
                        }
                        true
                    }
                    Key.Menu -> {
                        viewModel.toggleSettingsDialog()
                        true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isLoadingInfo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (uiState.showEndCard) {
            EndCard(
                videoTitle = videoTitle,
                onReplay = {
                    player.seekTo(0)
                    player.play()
                    viewModel.hideControls()
                },
                onBack = onBack
            )
        }

        if (uiState.showSettingsDialog && uiState.videoInfo != null) {
            val videoInfo = uiState.videoInfo!!
            SettingsDialog(
                audioTracks = videoInfo.audioTracks,
                subtitles = videoInfo.subtitles,
                selectedSubtitleIndex = uiState.selectedSubtitleIndex,
                playbackSpeed = uiState.playbackSpeed,
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                onSubtitleChange = { viewModel.setSelectedSubtitle(it) },
                onAudioTrackChange = { trackIndex ->
                    val audioGroups = player.currentTracks.groups.filter {
                        it.type == C.TRACK_TYPE_AUDIO
                    }
                    if (trackIndex >= 0 && trackIndex < audioGroups.size) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                TrackSelectionOverride(
                                    audioGroups[trackIndex].mediaTrackGroup,
                                    listOf(0)
                                )
                            )
                            .build()
                    } else if (trackIndex == -1) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .build()
                    }
                },
                onDismiss = { viewModel.toggleSettingsDialog() }
            )
        }
    }
}

@Composable
private fun EndCard(
    videoTitle: String,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Playback Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = CSGOColors.Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                videoTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = CSGOColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onReplay,
                modifier = Modifier.focusRequester(focusRequester),
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = CSGOColors.Primary,
                    contentColor = Color.White,
                    focusedContainerColor = CSGOColors.Primary.copy(alpha = 0.85f),
                    focusedContentColor = Color.White
                )
            ) { Text("Replay") }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBack,
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = CSGOColors.Surface,
                    contentColor = CSGOColors.OnBackground,
                    focusedContainerColor = CSGOColors.Secondary.copy(alpha = 0.3f),
                    focusedContentColor = Color.White
                )
            ) { Text("Back") }
        }
    }
}

@Composable
private fun SettingsDialog(
    audioTracks: List<AudioTrack>,
    subtitles: List<SubtitleInfo>,
    selectedSubtitleIndex: Int,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onSubtitleChange: (Int) -> Unit,
    onAudioTrackChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CSGOColors.Background,
        title = {
            Text("Playback Settings", color = CSGOColors.Primary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text("Audio Track", style = MaterialTheme.typography.titleSmall, color = CSGOColors.Secondary)
                Spacer(modifier = Modifier.height(4.dp))
                audioTracks.forEachIndexed { index, track ->
                    ListItem(
                        selected = false,
                        onClick = { onAudioTrackChange(index) },
                        colors = androidx.tv.material3.ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = CSGOColors.Surface
                        ),
                        headlineContent = {
                            Text(
                                "${track.language.ifBlank { "Track ${index + 1}" }} (${track.codec}, ${track.channels}ch)",
                                color = CSGOColors.OnBackground
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Subtitle", style = MaterialTheme.typography.titleSmall, color = CSGOColors.Secondary)
                Spacer(modifier = Modifier.height(4.dp))
                ListItem(
                    selected = selectedSubtitleIndex == -1,
                    onClick = { onSubtitleChange(-1) },
                    colors = androidx.tv.material3.ListItemDefaults.colors(
                        containerColor = if (selectedSubtitleIndex == -1) CSGOColors.Surface.copy(alpha = 0.5f) else Color.Transparent,
                        focusedContainerColor = CSGOColors.Surface,
                        selectedContainerColor = CSGOColors.Surface.copy(alpha = 0.5f)
                    ),
                    headlineContent = { Text("Off", color = CSGOColors.OnBackground) }
                )
                subtitles.forEachIndexed { index, sub ->
                    ListItem(
                        selected = selectedSubtitleIndex == index,
                        onClick = { onSubtitleChange(index) },
                        colors = androidx.tv.material3.ListItemDefaults.colors(
                            containerColor = if (selectedSubtitleIndex == index) CSGOColors.Surface.copy(alpha = 0.5f) else Color.Transparent,
                            focusedContainerColor = CSGOColors.Surface,
                            selectedContainerColor = CSGOColors.Surface.copy(alpha = 0.5f)
                        ),
                        headlineContent = {
                            Text(sub.language.ifBlank { sub.name }, color = CSGOColors.OnBackground)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Speed", style = MaterialTheme.typography.titleSmall, color = CSGOColors.Secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = playbackSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..3.0f,
                        steps = 9,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = CSGOColors.Primary,
                            activeTrackColor = CSGOColors.Primary,
                            inactiveTrackColor = CSGOColors.Surface
                        )
                    )
                    Text(
                        "${"%.1f".format(playbackSpeed)}x",
                        modifier = Modifier.padding(start = 8.dp),
                        color = CSGOColors.OnBackground
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = CSGOColors.Primary,
                    contentColor = Color.White,
                    focusedContainerColor = CSGOColors.Primary.copy(alpha = 0.85f),
                    focusedContentColor = Color.White
                )
            ) { Text("Done") }
        }
    )
}
