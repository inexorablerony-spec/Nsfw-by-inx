package io.github.aedev.flow.webview

import android.annotation.SuppressLint
import android.os.SystemClock
import android.webkit.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONTokener
import kotlin.coroutines.resume

data class MediaCandidate(
    val url: String,
    val type: String,
    val sourcePage: String,
    val userAgent: String,
    val referer: String,
    val cookies: String? = null
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
        private const val EXTRACTION_TIMEOUT_MS = 20000L
        private const val OBSERVATION_PERIOD_MS = 8000L
        private const val POLL_INTERVAL_MS = 500L
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
            throw e
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Unknown error", ExtractionResult.ErrorType.PAGE_LOAD_FAILED)
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractMediaInternal(url: String): ExtractionResult = suspendCancellableCoroutine { continuation ->
        var completed = false
        val webView = WebView(context)
        val candidates = mutableSetOf<MediaCandidate>()
        
        fun complete(result: ExtractionResult) {
            if (!completed) {
                completed = true
                webView.evaluateJavascript("window.__flowExtractorCleanup && window.__flowExtractorCleanup()", null)
                webView.destroy()
                continuation.resume(result)
            }
        }
        
        continuation.invokeOnCancellation {
            if (!completed) {
                completed = true
                webView.evaluateJavascript("window.__flowExtractorCleanup && window.__flowExtractorCleanup()", null)
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
                    injectObserver(view ?: return)
                    startObservation(view, url, candidates) { result ->
                        complete(result)
                    }
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true && !completed) {
                        complete(ExtractionResult.Error("Page load failed: ${error?.description}", ExtractionResult.ErrorType.PAGE_LOAD_FAILED))
                    }
                }
            }
        }
        
        webView.loadUrl(url)
    }
    
    private fun injectObserver(webView: WebView) {
        val js = """
            (function () {
                if (window.__flowExtractorInstalled) return;
                window.__flowExtractorInstalled = true;
                window.__flowMediaCandidates = [];
                const seen = new Set();
                
                function addCandidate(url, type) {
                    if (!url || typeof url !== "string") return;
                    try {
                        const parsed = new URL(url, location.href);
                        if (!["http:", "https:"].includes(parsed.protocol)) return;
                        const normalized = parsed.href;
                        if (seen.has(normalized)) return;
                        seen.add(normalized);
                        const lower = normalized.toLowerCase();
                        if (lower.includes("thumbnail") || lower.includes("preview") || lower.includes("poster") || lower.includes("analytics") || lower.includes("tracking")) return;
                        if (!/\.(mp4|m3u8)(?:$|[?#])/i.test(lower)) return;
                        window.__flowMediaCandidates.push({url: normalized, type: type || "unknown"});
                    } catch (_) {}
                }
                
                function inspectVideo(video) {
                    if (!video) return;
                    addCandidate(video.currentSrc, "video");
                    addCandidate(video.src, "video");
                    video.querySelectorAll("source").forEach(source => {
                        addCandidate(source.src, source.type || "source");
                    });
                }
                
                function inspectDocument() {
                    document.querySelectorAll("video").forEach(inspectVideo);
                    if (window.playlist && Array.isArray(window.playlist.sources)) {
                        window.playlist.sources.forEach(source => {
                            if (source.file) addCandidate(source.file, source.type);
                        });
                    }
                    try {
                        performance.getEntriesByType("resource").forEach(entry => {
                            addCandidate(entry.name, "resource");
                        });
                    } catch (_) {}
                }
                
                const observer = new MutationObserver(() => { inspectDocument(); });
                observer.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ["src"]});
                window.__flowExtractorObserver = observer;
                inspectDocument();
                window.__flowExtractorCleanup = function () {
                    observer.disconnect();
                    window.__flowExtractorObserver = null;
                };
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
    }
    
    private fun startObservation(
        webView: WebView,
        sourcePage: String,
        candidates: MutableSet<MediaCandidate>,
        onComplete: (ExtractionResult) -> Unit
    ) {
        val deadline = SystemClock.elapsedRealtime() + OBSERVATION_PERIOD_MS
        
        fun poll() {
            if (SystemClock.elapsedRealtime() >= deadline) {
                if (candidates.isNotEmpty()) {
                    onComplete(ExtractionResult.Success(candidates.toList()))
                } else {
                    onComplete(ExtractionResult.Error("No media found", ExtractionResult.ErrorType.NO_MEDIA_FOUND))
                }
                return
            }
            
            webView.evaluateJavascript("JSON.stringify(window.__flowMediaCandidates || [])") { result ->
                val newCandidates = parseJsonResult(result, sourcePage)
                candidates.addAll(newCandidates)
                
                webView.postDelayed({ poll() }, POLL_INTERVAL_MS)
            }
        }
        
        webView.postDelayed({ poll() }, POLL_INTERVAL_MS)
    }
    
    private fun parseJsonResult(result: String?, sourcePage: String): List<MediaCandidate> {
        val array = parseJavascriptArray(result)
        val cookieManager = CookieManager.getInstance()
        
        return (0 until array.length()).mapNotNull { i ->
            try {
                val obj = array.getJSONObject(i)
                val url = obj.optString("url", "")
                val type = obj.optString("type", "unknown")
                
                if (url.isNotBlank() && url.startsWith("http")) {
                    val cookies = cookieManager.getCookie(url)
                    MediaCandidate(
                        url = url,
                        type = determineMediaType(url, type),
                        sourcePage = sourcePage,
                        userAgent = USER_AGENT,
                        referer = sourcePage,
                        cookies = cookies
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun parseJavascriptArray(result: String?): JSONArray {
        if (result.isNullOrBlank() || result == "null") return JSONArray()
        
        return try {
            val decoded = JSONTokener(result).nextValue()
            when (decoded) {
                is JSONArray -> decoded
                is String -> JSONArray(decoded)
                else -> JSONArray()
            }
        } catch (_: Exception) {
            JSONArray()
        }
    }
    
    private fun determineMediaType(url: String, hint: String): String {
        return when {
            url.contains(".m3u8") -> "m3u8"
            url.contains(".mp4") -> "mp4"
            else -> hint
        }
    }
}
