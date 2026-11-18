package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.aedev.flow.providers.ProviderCatalogueItem
import io.github.aedev.flow.providers.UnifiedCatalogueRepository
import kotlinx.coroutines.CancellationException

@Composable
fun UnifiedHomeScreen(
    onVideoClick: (ProviderCatalogueItem) -> Unit
) {
    var items by remember { mutableStateOf<List<ProviderCatalogueItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var catalogueError by remember { mutableStateOf<String?>(null) }
    val repository = remember { UnifiedCatalogueRepository() }
    
    LaunchedEffect(Unit) {
        isLoading = true
        catalogueError = null
        try {
            val result = repository.getHomeFeed()
            items = result.items
            errors = result.errors
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            catalogueError = e.message ?: "Failed to load catalogue"
        }
        isLoading = false
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "All Providers",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Loading...", modifier = Modifier.padding(16.dp))
            }
            catalogueError != null -> {
                Text("Error: $catalogueError", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            items.isEmpty() && errors.isEmpty() -> {
                Text("No videos found", modifier = Modifier.padding(16.dp))
            }
            items.isEmpty() && errors.isNotEmpty() -> {
                Text("All providers failed", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                errors.forEach { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(items) { item ->
                        VideoCard(item = item, onClick = { onVideoClick(item) })
                    }
                }
            }
        }
        
        if (errors.isNotEmpty() && items.isNotEmpty()) {
            Text(
                text = "Some providers failed: ${errors.size}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun VideoCard(
    item: ProviderCatalogueItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            AsyncImage(
                model = item.post.image,
                contentDescription = item.post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = item.post.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = item.providerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
