package io.github.aedev.flow.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnifiedCatalogueRepository {
    suspend fun getHomeFeed(page: Int = 1): CatalogueResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ProviderCatalogueItem>()
        val errors = mutableListOf<String>()
        
        ProviderRegistry.getEnabled().forEach { provider ->
            try {
                val posts = provider.getPosts(page)
                results.addAll(posts.map { 
                    ProviderCatalogueItem(provider.value, provider.name, it)
                })
            } catch (e: Exception) {
                errors.add("${provider.name}: ${e.message}")
            }
        }
        
        CatalogueResult(results, errors)
    }
    
    suspend fun search(query: String, page: Int = 1): CatalogueResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ProviderCatalogueItem>()
        val errors = mutableListOf<String>()
        
        ProviderRegistry.getEnabled().forEach { provider ->
            try {
                val posts = provider.getSearchPosts(query, page)
                results.addAll(posts.map { 
                    ProviderCatalogueItem(provider.value, provider.name, it)
                })
            } catch (e: Exception) {
                errors.add("${provider.name}: ${e.message}")
            }
        }
        
        CatalogueResult(results, errors)
    }
    
    suspend fun getDetails(item: ProviderCatalogueItem): Info = withContext(Dispatchers.IO) {
        val provider = ProviderRegistry.getByValue(item.providerId)
        provider?.getMeta(item.post.link) ?: Info("", "", "", "", "movie", emptyList())
    }
    
    suspend fun getStreams(item: ProviderCatalogueItem): List<Stream> = withContext(Dispatchers.IO) {
        val provider = ProviderRegistry.getByValue(item.providerId)
        provider?.getStream(item.post.link) ?: emptyList()
    }
}
