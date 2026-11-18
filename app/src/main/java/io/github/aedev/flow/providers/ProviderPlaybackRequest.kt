package io.github.aedev.flow.providers

data class ProviderPlaybackRequest(
    val url: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val cookie: String? = null,
    val headers: Map<String, String> = emptyMap()
) {
    fun allHeaders(): Map<String, String> {
        val result = headers.toMutableMap()
        userAgent?.let { result["User-Agent"] = it }
        referer?.let { result["Referer"] = it }
        cookie?.let { result["Cookie"] = it }
        return result
    }
}
