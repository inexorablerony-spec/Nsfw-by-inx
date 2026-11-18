package io.github.aedev.flow.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EpornerProvider : Provider {
    override val name = "Eporner"
    override val value = "eporner"
    override val version = "1.0"
    override val description = "Eporner video provider"
    override val icon = "https://www.eporner.com/favicon.ico"
    
    private val baseUrl = "https://www.eporner.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    
    override suspend fun getPosts(page: Int): List<Post> = withContext(Dispatchers.IO) {
        try {
            val url = if (page > 1) "$baseUrl/?page=$page" else baseUrl
            val html = URL(url).readText()
            parsePosts(html)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getSearchPosts(query: String, page: Int): List<Post> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/search/$query/${if (page > 1) page else ""}"
            val html = URL(url).readText()
            parsePosts(html)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getMeta(link: String): Info = withContext(Dispatchers.IO) {
        try {
            val html = URL(link).readText()
            val title = html.substringAfter("<title>").substringBefore("</title>")
            val image = html.substringAfter("og:image\" content=\"").substringBefore("\"")
            Info(title, "", image, "", "movie", emptyList())
        } catch (e: Exception) {
            Info("", "", "", "", "movie", emptyList())
        }
    }
    
    override suspend fun getStream(link: String): List<Stream> = withContext(Dispatchers.IO) {
        try {
            if (link.contains(".mp4")) {
                return@withContext listOf(Stream("Eporner", link, "mp4", mapOf("User-Agent" to userAgent, "Referer" to baseUrl)))
            }
            
            val html = URL(link).readText()
            val mp4Regex = Regex("""https?://[^"'\s]+\.mp4[^"'\s]*""")
            val streams = mp4Regex.findAll(html).map { match ->
                Stream("Eporner", match.value, "mp4", mapOf("User-Agent" to userAgent, "Referer" to baseUrl))
            }.distinctBy { it.link }.toList()
            
            streams
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parsePosts(html: String): List<Post> {
        val posts = mutableListOf<Post>()
        val regex = Regex("""href="(/video-[^"]+)"[^>]*>.*?alt="([^"]*)".*?src="([^"]*)""")
        regex.findAll(html).forEach { match ->
            val link = baseUrl + match.groupValues[1]
            val title = match.groupValues[2]
            val image = match.groupValues[3]
            if (title.isNotEmpty()) {
                posts.add(Post(title, link, image))
            }
        }
        return posts.take(30)
    }
}
