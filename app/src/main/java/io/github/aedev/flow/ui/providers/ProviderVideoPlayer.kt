package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.player.EnhancedPlayerManager
import io.github.aedev.flow.player.GlobalPlayerState
import io.github.aedev.flow.providers.ProviderCatalogueItem
import io.github.aedev.flow.providers.ProviderRegistry
import kotlinx.coroutines.CancellationException

@Composable
fun ProviderVideoPlayer(
    item: ProviderCatalogueItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isResolving by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    
    val playerManager = remember { EnhancedPlayerManager.getInstance() }
    val playerState by playerManager.playerState.collectAsState()
    
    LaunchedEffect(item.providerId, item.post.link, retryTrigger) {
        isResolving = true
        error = null
        try {
            val provider = ProviderRegistry.getByValue(item.providerId)
            if (provider == null) {
                error = "Provider not found"
                isResolving = false
                return@LaunchedEffect
            }
            if (!ProviderRegistry.isEnabled(item.providerId)) {
                error = "This provider is disabled"
                isResolving = false
                return@LaunchedEffect
            }
            val streams = provider.getStream(item.post.link)
            if (streams.isEmpty()) {
                error = "No streams found"
                isResolving = false
                return@LaunchedEffect
            }
            val stream = streams.first()
            
            val video = Video(
                id = item.post.link,
                title = item.post.title,
                channelName = provider.name,
                channelId = provider.value,
                thumbnailUrl = item.post.image,
                duration = 0,
                viewCount = 0,
                uploadDate = ""
            )
            
            playerManager.playExternalStream(
                context = context,
                videoId = item.post.link,
                url = stream.link,
                headers = stream.headers
            )
            
            GlobalPlayerState.setCurrentVideo(video)
            GlobalPlayerState.expandMiniPlayer()
            isResolving = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
            isResolving = false
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack) {
            Text("Back")
        }
        
        when {
            isResolving -> {
                CircularProgressIndicator()
                Text("Resolving stream...")
            }
            error != null -> {
                Text("Error: $error")
                Button(onClick = { retryTrigger++ }) {
                    Text("Retry")
                }
            }
            playerState.isBuffering -> {
                CircularProgressIndicator()
                Text("Buffering...")
            }
            playerState.error != null -> {
                Text("Playback error: ${playerState.error}")
                Button(onClick = { retryTrigger++ }) {
                    Text("Retry")
                }
            }
            playerState.isPrepared && playerState.isPlaying -> {
                Text("Playing")
            }
            else -> {
                Text("Ready")
            }
        }
    }
}
