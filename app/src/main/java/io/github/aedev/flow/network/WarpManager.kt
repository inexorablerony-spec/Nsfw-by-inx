package io.github.aedev.flow.network

import android.content.Context
import android.content.SharedPreferences

object WarpManager {
    private var prefs: SharedPreferences? = null
    private var isEnabled = false
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("warp", Context.MODE_PRIVATE)
        isEnabled = prefs?.getBoolean("enabled", false) ?: false
    }
    
    fun isWarpEnabled(): Boolean = isEnabled
    
    fun setWarpEnabled(enabled: Boolean) {
        isEnabled = enabled
        prefs?.edit()?.putBoolean("enabled", enabled)?.apply()
    }
    
    fun getProxyHost(): String = if (isEnabled) "127.0.0.1" else ""
    
    fun getProxyPort(): Int = if (isEnabled) 1080 else 0
}
