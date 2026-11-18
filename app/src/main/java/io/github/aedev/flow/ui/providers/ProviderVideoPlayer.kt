package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import io.github.aedev.flow.providers.Provider
import io.github.aedev.flow.providers.Stream
import kotlinx.coroutines.launch

@Composable
fun ProviderVideoPlayer(
    provider: Provider,
    videoLink: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(videoLink) {
        scope.launch {
            try {
                val streams = provider.getStream(videoLink)
                if (streams.isNotEmpty()) {
                    val stream = streams.first()
                    
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(stream.headers["User-Agent"] ?: "Mozilla/5.0")
                        .setDefaultRequestProperties(stream.headers.filterKeys { it != "User-Agent" })
                    
                    val mediaSourceFactory = DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(dataSourceFactory)
                    
                    val newPlayer = ExoPlayer.Builder(context)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build()
                    
                    val mediaItem = MediaItem.fromUri(stream.link)
                    newPlayer.setMediaItem(mediaItem)
                    newPlayer.prepare()
                    newPlayer.playWhenReady = true
                    
                    player = newPlayer
                } else {
                    error = "No streams found"
                }
            } catch (e: Exception) {
                error = e.message
            }
            isLoading = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error")
        } else if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
