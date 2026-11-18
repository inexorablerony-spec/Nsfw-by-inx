package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import io.github.aedev.flow.providers.Provider
import io.github.aedev.flow.providers.ProviderPlaybackManager
import io.github.aedev.flow.providers.ProviderPlaybackRequest
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
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(videoLink) {
        scope.launch {
            try {
                val streams = provider.getStream(videoLink)
                if (streams.isNotEmpty()) {
                    val stream = streams.first()
                    val request = ProviderPlaybackRequest(
                        url = stream.link,
                        userAgent = stream.headers["User-Agent"],
                        referer = stream.headers["Referer"],
                        cookie = stream.headers["Cookie"],
                        headers = stream.headers
                    )
                    ProviderPlaybackManager.play(context, request)
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
            scope.launch {
                ProviderPlaybackManager.release()
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error")
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = ProviderPlaybackManager.getPlayer()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
