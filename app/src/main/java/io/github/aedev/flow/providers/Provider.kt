package io.github.aedev.flow.providers

data class Post(
    val title: String,
    val link: String,
    val image: String
)

data class Stream(
    val server: String,
    val link: String,
    val type: String,
    val headers: Map<String, String>? = null
)

data class Info(
    val title: String,
    val synopsis: String,
    val image: String,
    val imdbId: String,
    val type: String,
    val linkList: List<LinkItem>
)

data class LinkItem(
    val title: String,
    val quality: String,
    val directLinks: List<DirectLink>
)

data class DirectLink(
    val title: String,
    val link: String,
    val type: String
)

interface Provider {
    val name: String
    val value: String
    val icon: String
    
    suspend fun getPosts(page: Int = 1): List<Post>
    suspend fun getSearchPosts(query: String, page: Int = 1): List<Post>
    suspend fun getMeta(link: String): Info
    suspend fun getStream(link: String): List<Stream>
}
