package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.aedev.flow.player.EnhancedPlayerManager
import io.github.aedev.flow.providers.Provider
import kotlinx.coroutines.CancellationException

@Composable
fun ProviderVideoPlayer(
    provider: Provider,
    videoLink: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val playerManager = remember { EnhancedPlayerManager.getInstance() }
    
    LaunchedEffect(videoLink) {
        isLoading = true
        error = null
        try {
            val streams = provider.getStream(videoLink)
            if (streams.isNotEmpty()) {
                val stream = streams.first()
                playerManager.playExternalStream(
                    context = context,
                    videoId = videoLink,
                    url = stream.link,
                    headers = stream.headers
                )
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
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error")
        } else {
            Text("Playing in Flow player")
        }
    }
}
