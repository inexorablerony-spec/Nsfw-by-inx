package io.github.aedev.flow.ui.providers

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.aedev.flow.providers.Post
import io.github.aedev.flow.providers.Provider
import io.github.aedev.flow.providers.ProviderCatalogueItem
import io.github.aedev.flow.providers.ProviderRegistry

sealed class ProviderRoute(val route: String) {
    object UnifiedHome : ProviderRoute("unified_home")
    object ProviderList : ProviderRoute("providers")
    object VideoList : ProviderRoute("videos/{providerValue}") {
        fun createRoute(providerValue: String) = "videos/$providerValue"
    }
    object VideoPlayer : ProviderRoute("player/{videoLink}") {
        fun createRoute(videoLink: String) = "player/${videoLink.encodeURL()}"
    }
}

fun String.encodeURL(): String = java.net.URLEncoder.encode(this, "UTF-8")
fun String.decodeURL(): String = java.net.URLDecoder.decode(this, "UTF-8")

@Composable
fun ProviderNavHost(
    navController: NavHostController,
    onPlayVideo: (ProviderCatalogueItem) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ProviderRoute.UnifiedHome.route
    ) {
        composable(ProviderRoute.UnifiedHome.route) {
            UnifiedHomeScreen(
                onVideoClick = { item ->
                    onPlayVideo(item)
                }
            )
        }
        
        composable(ProviderRoute.ProviderList.route) {
            ProviderListScreen(
                onProviderClick = { provider ->
                    navController.navigate(ProviderRoute.VideoList.createRoute(provider.value))
                }
            )
        }
        
        composable(ProviderRoute.VideoList.route) { backStackEntry ->
            val providerValue = backStackEntry.arguments?.getString("providerValue") ?: return@composable
            val provider = ProviderRegistry.getByValue(providerValue) ?: return@composable
            
            VideoListScreen(
                provider = provider,
                onVideoClick = { post ->
                    onPlayVideo(ProviderCatalogueItem(provider.value, provider.name, post))
                }
            )
        }
    }
}
