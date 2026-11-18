package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.player.EnhancedPlayerManager
import io.github.aedev.flow.player.GlobalPlayerState
import io.github.aedev.flow.providers.Provider
import io.github.aedev.flow.providers.ProviderCatalogueItem
import io.github.aedev.flow.providers.ProviderRegistry
import kotlinx.coroutines.CancellationException

@Composable
fun ProviderVideoPlayer(
    item: ProviderCatalogueItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val playerManager = remember { EnhancedPlayerManager.getInstance() }
    
    LaunchedEffect(item.providerId, item.post.link) {
        isLoading = true
        error = null
        try {
            val provider = ProviderRegistry.getByValue(item.providerId)
            if (provider == null) {
                error = "Provider not found"
                isLoading = false
                return@LaunchedEffect
            }
            if (!ProviderRegistry.isEnabled(item.providerId)) {
                error = "This provider is disabled"
                isLoading = false
                return@LaunchedEffect
            }
            val streams = provider.getStream(item.post.link)
            if (streams.isNotEmpty()) {
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
            } else {
                error = "No streams found"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack) {
            Text("Back")
        }
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error")
        }
    }
}
