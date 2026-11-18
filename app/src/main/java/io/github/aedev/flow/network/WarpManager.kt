package io.github.aedev.flow.network

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

/**
 * WARP Manager for Cloudflare WARP integration.
 * 
 * IMPORTANT: Cloudflare does not provide an embeddable SDK for WARP.
 * The only supported way to use WARP is through the official 1.1.1.1 app.
 * 
 * This manager provides:
 * - Link to install the official 1.1.1.1 app
 * - VPN state monitoring (using existing VpnStateMonitor)
 * - No fake proxy implementation
 */
object WarpManager {
    private var prefs: SharedPreferences? = null
    private const val WARP_PACKAGE = "com.cloudflare.onedotonedotonedotone"
    private const val WARP_PLAY_STORE = "https://play.google.com/store/apps/details?id=$WARP_PACKAGE"
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("warp", Context.MODE_PRIVATE)
    }
    
    /**
     * Check if the official 1.1.1.1 app is installed.
     */
    fun isWarpAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WARP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Open the Play Store to install the 1.1.1.1 app.
     */
    fun openPlayStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WARP_PLAY_STORE))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Open the 1.1.1.1 app if installed.
     */
    fun openWarpApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(WARP_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Get the current WARP preference (user's intent to use WARP).
     * This does NOT indicate actual connection status.
     */
    fun isWarpPreferred(): Boolean = prefs?.getBoolean("preferred", false) ?: false
    
    /**
     * Set the user's preference for WARP.
     * This does NOT establish a connection.
     */
    fun setWarpPreferred(preferred: Boolean) {
        prefs?.edit()?.putBoolean("preferred", preferred)?.apply()
    }
}
