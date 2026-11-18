package io.github.aedev.flow.webview

import android.annotation.SuppressLint
import android.webkit.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MediaCandidate(
    val url: String,
    val type: String,
    val sourcePage: String,
    val userAgent: String,
    val referer: String
)

sealed class ExtractionResult {
    data class Success(val candidates: List<MediaCandidate>) : ExtractionResult()
    data class Error(val message: String, val type: ErrorType) : ExtractionResult()
    
    enum class ErrorType {
        TIMEOUT,
        PAGE_LOAD_FAILED,
        NO_MEDIA_FOUND,
        CANCELLED
    }
}

class WebViewExtractor(private val context: android.content.Context) {
    
    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val EXTRACTION_TIMEOUT_MS = 15000L
        private const val OBSERVATION_PERIOD_MS = 3000L
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractMedia(url: String): ExtractionResult = withContext(Dispatchers.Main) {
        try {
            withTimeout(EXTRACTION_TIMEOUT_MS) {
                extractMediaInternal(url)
            }
        } catch (e: TimeoutCancellationException) {
            ExtractionResult.Error("Extraction timed out", ExtractionResult.ErrorType.TIMEOUT)
        } catch (e: CancellationException) {
            ExtractionResult.Error("Extraction cancelled", ExtractionResult.ErrorType.CANCELLED)
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Unknown error", ExtractionResult.ErrorType.PAGE_LOAD_FAILED)
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractMediaInternal(url: String): ExtractionResult = suspendCancellableCoroutine { continuation ->
        var completed = false
        val webView = WebView(context)
        
        fun complete(result: ExtractionResult) {
            if (!completed) {
                completed = true
                webView.destroy()
                continuation.resume(result)
            }
        }
        
        continuation.invokeOnCancellation {
            if (!completed) {
                completed = true
                webView.destroy()
            }
        }
        
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = USER_AGENT
            settings.mediaPlaybackRequiresUserGesture = false
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    view?.postDelayed({
                        extractFromPage(view, url) { candidates ->
                            if (candidates.isNotEmpty()) {
                                complete(ExtractionResult.Success(candidates))
                            } else {
                                complete(ExtractionResult.Error("No media found", ExtractionResult.ErrorType.NO_MEDIA_FOUND))
                            }
                        }
                    }, OBSERVATION_PERIOD_MS)
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        complete(ExtractionResult.Error("Page load failed: ${error?.description}", ExtractionResult.ErrorType.PAGE_LOAD_FAILED))
                    }
                }
            }
        }
        
        webView.loadUrl(url)
    }
    
    private fun extractFromPage(webView: WebView, sourcePage: String, callback: (List<MediaCandidate>) -> Unit) {
        val js = """
            (function() {
                var candidates = [];
                var seen = {};
                
                function addCandidate(url, type) {
                    if (!url || seen[url]) return;
                    if (!url.startsWith('http')) return;
                    seen[url] = true;
                    candidates.push({url: url, type: type});
                }
                
                // Check video elements
                document.querySelectorAll('video').forEach(function(video) {
                    if (video.currentSrc) addCandidate(video.currentSrc, 'video');
                    if (video.src) addCandidate(video.src, 'video');
                    video.querySelectorAll('source').forEach(function(source) {
                        if (source.src) addCandidate(source.src, 'source');
                    });
                });
                
                // Check window.playlist
                if (window.playlist && window.playlist.sources) {
                    window.playlist.sources.forEach(function(s) {
                        if (s.file) addCandidate(s.file, s.type || 'playlist');
                    });
                }
                
                // Check for MP4/M3U8 in page
                var html = document.documentElement.innerHTML;
                var mp4Matches = html.match(/https?:\/\/[^"'\s<>]+\.mp4[^"'\s<>]*/gi) || [];
                var m3u8Matches = html.match(/https?:\/\/[^"'\s<>]+\.m3u8[^"'\s<>]*/gi) || [];
                
                mp4Matches.forEach(function(url) { addCandidate(url, 'mp4'); });
                m3u8Matches.forEach(function(url) { addCandidate(url, 'm3u8'); });
                
                return JSON.stringify(candidates);
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            val candidates = parseJsonResult(result, sourcePage)
            callback(candidates)
        }
    }
    
    private fun parseJsonResult(result: String?, sourcePage: String): List<MediaCandidate> {
        if (result == null || result == "null") return emptyList()
        
        return try {
            val json = result.removeSurrounding("\"").replace("\\\"", "\"").replace("\\\\", "\\")
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val url = obj.optString("url", "")
                val type = obj.optString("type", "unknown")
                
                if (isValidMediaUrl(url)) {
                    MediaCandidate(
                        url = url,
                        type = determineMediaType(url, type),
                        sourcePage = sourcePage,
                        userAgent = USER_AGENT,
                        referer = sourcePage
                    )
                } else null
            }.distinctBy { it.url }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun isValidMediaUrl(url: String): Boolean {
        if (url.isBlank() || !url.startsWith("http")) return false
        if (url.contains("thumbnail") || url.contains("preview") || url.contains("poster")) return false
        if (url.contains("tracking") || url.contains("analytics") || url.contains("ad")) return false
        return url.contains(".mp4") || url.contains(".m3u8")
    }
    
    private fun determineMediaType(url: String, hint: String): String {
        return when {
            url.contains(".m3u8") -> "m3u8"
            url.contains(".mp4") -> "mp4"
            else -> hint
        }
    }
}
