package io.github.aedev.flow.ui.settings

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
fun ProviderManagerScreen(
    onOpenCatalogue: () -> Unit = {}
) {
    val providers = remember { ProviderRegistry.getAll() }
    var enabledMap by remember { mutableStateOf(providers.associate { it.value to ProviderRegistry.isEnabled(it.value) }) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Provider Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Install, update, and test provider extensions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Button(
            onClick = onOpenCatalogue,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Open Provider Catalogue")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(providers) { provider ->
                ProviderManagerItem(
                    provider = provider,
                    isEnabled = enabledMap[provider.value] ?: true,
                    onToggle = { enabled ->
                        ProviderRegistry.setEnabled(provider.value, enabled)
                        enabledMap = enabledMap.toMutableMap().apply { put(provider.value, enabled) }
                    }
                )
            }
        }
    }
}

@Composable
fun ProviderManagerItem(
    provider: Provider,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
