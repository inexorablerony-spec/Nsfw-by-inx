package io.github.aedev.flow.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class EpornerProvider : Provider {
    override val name = "Eporner"
    override val value = "eporner"
    override val version = "1.0"
    override val description = "Eporner video provider"
    override val icon = "https://www.eporner.com/favicon.ico"
    
    private val baseUrl = "https://www.eporner.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val connectTimeout = 15000
    private val readTimeout = 15000
    
    override suspend fun getPosts(page: Int): List<Post> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/?page=$page" else baseUrl
        fetchAndParse(url)
    }
    
    override suspend fun getSearchPosts(query: String, page: Int): List<Post> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = if (page > 1) "$baseUrl/search/$encodedQuery/$page/" else "$baseUrl/search/$encodedQuery/"
        fetchAndParse(url)
    }
    
    override suspend fun getMeta(link: String): Info = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(link)
            val title = extractTitle(html)
            val image = extractThumbnail(html)
            Info(title, "", image, "", "movie", emptyList())
        } catch (e: Exception) {
            Info("", "", "", "", "movie", emptyList())
        }
    }
    
    override suspend fun getStream(link: String): List<Stream> = withContext(Dispatchers.IO) {
        if (link.contains(".mp4") || link.contains(".m3u8")) {
            return@withContext listOf(createStream(link))
        }
        
        try {
            val html = fetchHtml(link)
            extractStreams(html)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun fetchAndParse(url: String): List<Post> {
        return try {
            val html = fetchHtml(url)
            parsePosts(html)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun fetchHtml(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Referer", baseUrl)
        
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP $responseCode")
        }
        
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
    
    private fun parsePosts(html: String): List<Post> {
        val posts = mutableListOf<Post>()
        val seen = mutableSetOf<String>()
        
        val linkRegex = Regex("""href="(/video-[^"]+)"[^>]*>""")
        val titleRegex = Regex("""alt="([^"]*)"""")
        val imageRegex = Regex("""src="(https?://[^"]+\.(?:jpg|jpeg|png|webp)[^"]*)"""")
        
        linkRegex.findAll(html).forEach { match ->
            val link = baseUrl + match.groupValues[1]
            if (seen.contains(link)) return@forEach
            seen.add(link)
            
            val context = html.substring(match.range.first, minOf(match.range.last + 500, html.length))
            val title = titleRegex.find(context)?.groupValues?.get(1) ?: ""
            val image = imageRegex.find(context)?.groupValues?.get(1) ?: ""
            
            if (title.isNotEmpty()) {
                posts.add(Post(title, link, image))
            }
        }
        
        return posts.take(30)
    }
    
    private fun extractTitle(html: String): String {
        val titleMatch = Regex("""<title>([^<]+)</title>""").find(html)
        return titleMatch?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun extractThumbnail(html: String): String {
        val ogMatch = Regex("""property="og:image"\s+content="([^"]+)"""").find(html)
        return ogMatch?.groupValues?.get(1) ?: ""
    }
    
    private fun extractStreams(html: String): List<Stream> {
        val streams = mutableListOf<Stream>()
        val seen = mutableSetOf<String>()
        
        val mp4Regex = Regex("""https?://[^"'\s]+\.mp4[^"'\s]*""")
        val m3u8Regex = Regex("""https?://[^"'\s]+\.m3u8[^"'\s]*""")
        
        (mp4Regex.findAll(html) + m3u8Regex.findAll(html)).forEach { match ->
            val url = unescapeUrl(match.value)
            if (!seen.contains(url) && !isPreviewUrl(url)) {
                seen.add(url)
                streams.add(createStream(url))
            }
        }
        
        return streams
    }
    
    private fun unescapeUrl(url: String): String {
        return url.replace("\\/", "/").replace("\\u0026", "&")
    }
    
    private fun isPreviewUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("preview") || lower.contains("trailer") || lower.contains("thumb")
    }
    
    private fun createStream(url: String): Stream {
        val type = if (url.contains(".m3u8")) "m3u8" else "mp4"
        return Stream("Eporner", url, type, mapOf("User-Agent" to userAgent, "Referer" to baseUrl))
    }
}
