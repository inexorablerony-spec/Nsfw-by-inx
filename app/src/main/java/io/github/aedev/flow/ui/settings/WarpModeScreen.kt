package io.github.aedev.flow.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.network.WarpManager

@Composable
fun WarpModeScreen() {
    val context = LocalContext.current
    var warpEnabled by remember { mutableStateOf(WarpManager.isWarpEnabled()) }
    
    LaunchedEffect(Unit) {
        WarpManager.init(context)
        warpEnabled = WarpManager.isWarpEnabled()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Network",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
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
                        text = "WARP Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Bypass ISP restrictions with Cloudflare WARP VPN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = warpEnabled,
                    onCheckedChange = { enabled ->
                        warpEnabled = enabled
                        WarpManager.setWarpEnabled(enabled)
                    }
                )
            }
        }
        
        if (warpEnabled) {
            Text(
                text = "WARP Mode is enabled. Traffic will be routed through Cloudflare.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
