package io.github.aedev.flow.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class SettingsRoute(val route: String) {
    object Settings : SettingsRoute("settings")
    object ProviderManager : SettingsRoute("provider_manager")
    object WarpMode : SettingsRoute("warp_mode")
}

@Composable
fun SettingsScreen(
    navController: NavHostController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onClick = { navController.navigate(SettingsRoute.ProviderManager.route) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Provider Manager",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Install, update, and test provider extensions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onClick = { navController.navigate(SettingsRoute.WarpMode.route) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
        }
    }
}

@Composable
fun SettingsNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Settings.route
    ) {
        composable(SettingsRoute.Settings.route) {
            SettingsScreen(navController)
        }
        
        composable(SettingsRoute.ProviderManager.route) {
            ProviderManagerScreen()
        }
        
        composable(SettingsRoute.WarpMode.route) {
            WarpModeScreen()
        }
    }
}
