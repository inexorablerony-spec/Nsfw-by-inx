package io.github.aedev.flow.webview

import android.annotation.SuppressLint
import android.webkit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebViewExtractor(private val context: android.content.Context) {
    
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractStreams(url: String): List<String> = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("""
                            (function() {
                                var streams = [];
                                // Check for video elements
                                document.querySelectorAll('video source, video').forEach(function(el) {
                                    var src = el.src || el.getAttribute('src');
                                    if (src && (src.includes('.mp4') || src.includes('.m3u8'))) {
                                        streams.push(src);
                                    }
                                });
                                // Check for window.playlist
                                if (window.playlist && window.playlist.sources) {
                                    window.playlist.sources.forEach(function(s) {
                                        if (s.file) streams.push(s.file);
                                    });
                                }
                                // Check for JSON data
                                var scripts = document.querySelectorAll('script');
                                scripts.forEach(function(s) {
                                    var text = s.textContent;
                                    var mp4Match = text.match(/https?:\/\/[^"'\s]+\.mp4[^"'\s]*/g);
                                    var m3u8Match = text.match(/https?:\/\/[^"'\s]+\.m3u8[^"'\s]*/g);
                                    if (mp4Match) streams.push.apply(streams, mp4Match);
                                    if (m3u8Match) streams.push.apply(streams, m3u8Match);
                                });
                                return JSON.stringify(streams);
                            })();
                        """.trimIndent()) { result ->
                            val streams = result?.removeSurrounding("\"")?.replace("\\\"", "\"")?.let {
                                try { org.json.JSONArray(it).let { arr -> (0 until arr.length()).map { i -> arr.getString(i) } } } catch (e: Exception) { emptyList() }
                            } ?: emptyList()
                            continuation.resume(streams)
                        }
                    }
                }
            }
            webView.loadUrl(url)
        }
    }
    
    suspend fun extractPlaylist(url: String): String? = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("window.playlist ? JSON.stringify(window.playlist) : null") { result ->
                            continuation.resume(result?.removeSurrounding("\""))
                        }
                    }
                }
            }
            webView.loadUrl(url)
        }
    }
}
