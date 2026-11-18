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
    var warpPreferred by remember { mutableStateOf(false) }
    var isInstalled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        WarpManager.init(context)
        warpPreferred = WarpManager.isWarpPreferred()
        isInstalled = WarpManager.isWarpAppInstalled(context)
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cloudflare WARP",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Bypass ISP restrictions with Cloudflare WARP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isInstalled) {
                    Text(
                        text = "The official 1.1.1.1 app is required for WARP.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { WarpManager.openPlayStore(context) }) {
                        Text("Install 1.1.1.1 App")
                    }
                } else {
                    Text(
                        text = "1.1.1.1 app is installed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { WarpManager.openWarpApp(context) }) {
                        Text("Open 1.1.1.1 App")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Note: Cloudflare does not provide an embeddable SDK. WARP must be used through the official 1.1.1.1 app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
