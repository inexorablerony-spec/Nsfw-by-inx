package io.github.aedev.flow.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.aedev.flow.network.WarpManager

@Composable
fun WarpModeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInstalled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    fun refreshStatus() {
        isInstalled = WarpManager.isWarpAppInstalled(context)
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    LaunchedEffect(Unit) {
        refreshStatus()
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
                    text = "Use Cloudflare's official 1.1.1.1 app for WARP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isInstalled) {
                    Text(
                        text = "The official 1.1.1.1 app is required. Install it and enable WARP inside the app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (!WarpManager.openPlayStore(context)) {
                            errorMessage = "Could not open Play Store"
                        }
                    }) {
                        Text("Install 1.1.1.1 App")
                    }
                } else {
                    Text(
                        text = "1.1.1.1 app is installed. Open it to enable WARP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (!WarpManager.openWarpApp(context)) {
                            errorMessage = "Could not open 1.1.1.1 app"
                        }
                    }) {
                        Text("Open 1.1.1.1 App")
                    }
                }
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Note: Cloudflare does not provide an embeddable SDK. You must install the official 1.1.1.1 app and enable WARP inside it. This app cannot establish a WARP connection directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
