package io.github.aedev.flow.providers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class EpornerProvider : Provider {
    override val name = "Eporner"
    override val value = "eporner"
    override val description = "Eporner provider"
    
    private val baseUrl = "https://www.eporner.com"
    
    override fun getInfo(enabled: Boolean) = ProviderInfo(name, value, description, enabled)
    
    override suspend fun getPosts(page: Int): List<Post> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/page/$page/"
            val html = fetchHtml(url)
            parsePosts(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Eporner getPosts failed: ${e.message}", e)
        }
    }
    
    override suspend fun getSearchPosts(query: String, page: Int): List<Post> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/search/${query.replace(" ", "-")}/$page/"
            val html = fetchHtml(url)
            parsePosts(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Eporner search failed: ${e.message}", e)
        }
    }
    
    override suspend fun getMeta(url: String): Info = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(url)
            Info(
                title = extractTitle(html),
                image = extractImage(html),
                synopsis = "",
                imdbId = "",
                type = "movie",
                linkList = emptyList()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Eporner getMeta failed: ${e.message}", e)
        }
    }
    
    override suspend fun getStream(url: String): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val html = fetchHtml(url)
            extractStreams(html, url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Eporner getStream failed: ${e.message}", e)
        }
    }
    
    private fun fetchHtml(urlString: String): String {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw Exception("HTTP $responseCode")
            }
            
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }
    
    private fun parsePosts(html: String): List<Post> {
        val posts = mutableListOf<Post>()
        val seen = mutableSetOf<String>()
        val pattern = """href="(/hd-porn/[^"]+)"[^>]*>.*?alt="([^"]*)".*?src="([^"]+)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        pattern.findAll(html).forEach { match ->
            val link = baseUrl + match.groupValues[1]
            if (link !in seen) {
                seen.add(link)
                posts.add(Post(
                    title = match.groupValues[2],
                    link = link,
                    image = match.groupValues[3]
                ))
            }
        }
        
        return posts
    }
    
    private fun extractTitle(html: String): String {
        return """<title>([^<]+)</title>""".toRegex().find(html)?.groupValues?.get(1) ?: "Eporner"
    }
    
    private fun extractImage(html: String): String {
        return """og:image" content="([^"]+)"""".toRegex().find(html)?.groupValues?.get(1) ?: ""
    }
    
    private fun extractStreams(html: String, referer: String): List<Stream> {
        val streams = mutableListOf<Stream>()
        val pattern = """https?://[^"'\s]+\.(mp4|m3u8)[^"'\s]*""".toRegex()
        
        pattern.findAll(html).forEach { match ->
            val url = match.value
            streams.add(Stream(
                server = "Eporner",
                link = url,
                type = if (url.contains("m3u8")) "m3u8" else "mp4",
                headers = mapOf("Referer" to referer)
            ))
        }
        
        return streams
    }
}
