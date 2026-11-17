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
import io.github.aedev.flow.providers.Post
import io.github.aedev.flow.providers.Provider
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    provider: Provider,
    onVideoClick: (Post) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    posts = provider.getSearchPosts(searchQuery)
                    isLoading = false
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Search")
        }
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        onClick = { onVideoClick(post) }
                    ) {
                        Column {
                            AsyncImage(
                                model = post.image,
                                contentDescription = post.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
