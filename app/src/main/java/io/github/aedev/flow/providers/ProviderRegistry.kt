package io.github.aedev.flow.providers

import android.content.Context
import android.content.SharedPreferences

object ProviderRegistry {
    private val allProviders = mutableListOf<Provider>()
    private val enabledProviders = mutableSetOf<String>()
    private var prefs: SharedPreferences? = null
    
    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("providers", Context.MODE_PRIVATE)
        if (allProviders.isEmpty()) {
            allProviders.add(EpornerProvider())
            allProviders.add(WebViewProvider(ctx))
        }
        loadEnabledProviders()
    }
    
    private fun loadEnabledProviders() {
        val saved = prefs?.getStringSet("enabled", null)
        if (saved != null) {
            enabledProviders.clear()
            enabledProviders.addAll(saved)
        } else {
            enabledProviders.addAll(allProviders.map { it.value })
        }
    }
    
    fun getAll(): List<Provider> = allProviders.toList()
    
    fun getEnabled(): List<Provider> = allProviders.filter { enabledProviders.contains(it.value) }
    
    fun isEnabled(value: String): Boolean = enabledProviders.contains(value)
    
    fun setEnabled(value: String, enabled: Boolean) {
        if (enabled) {
            enabledProviders.add(value)
        } else {
            enabledProviders.remove(value)
        }
        prefs?.edit()?.putStringSet("enabled", enabledProviders)?.apply()
    }
    
    fun getByValue(value: String): Provider? = allProviders.find { it.value == value }
}
