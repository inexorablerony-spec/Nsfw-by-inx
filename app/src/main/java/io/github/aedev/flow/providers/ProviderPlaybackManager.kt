package io.github.aedev.flow.providers

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@UnstableApi
object ProviderPlaybackManager {
    private var player: ExoPlayer? = null
    private val mutex = Mutex()
    
    suspend fun play(context: Context, request: ProviderPlaybackRequest) {
        mutex.withLock {
            release()
            
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(request.userAgent ?: "Mozilla/5.0")
                .setDefaultRequestProperties(request.allHeaders().filterKeys { it != "User-Agent" })
            
            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            
            player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
            
            val mediaItem = MediaItem.fromUri(request.url)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        }
    }
    
    fun getPlayer(): ExoPlayer? = player
    
    suspend fun release() {
        mutex.withLock {
            player?.release()
            player = null
        }
    }
}
