package io.github.aedev.flow.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.providers.Provider
import io.github.aedev.flow.providers.ProviderRegistry

@Composable
fun ProviderListScreen(
    onProviderClick: (Provider) -> Unit
) {
    val providers = remember { ProviderRegistry.getEnabled() }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Providers",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn {
            items(providers) { provider ->
                ProviderItem(
                    provider = provider,
                    onClick = { onProviderClick(provider) }
                )
            }
        }
    }
}

@Composable
fun ProviderItem(
    provider: Provider,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = provider.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
