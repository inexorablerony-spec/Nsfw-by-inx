package io.github.aedev.flow.providers

import android.content.Context
import android.content.SharedPreferences

object ProviderRegistry {
    private val allProviders = mutableMapOf<String, Provider>()
    private val enabledProviders = mutableSetOf<String>()
    private var prefs: SharedPreferences? = null
    private var initialized = false
    
    fun init(ctx: Context) {
        if (initialized) return
        prefs = ctx.getSharedPreferences("providers", Context.MODE_PRIVATE)
        
        if (allProviders.isEmpty()) {
            register(EpornerProvider())
            register(WebViewProvider(ctx))
        }
        
        loadEnabledProviders()
        initialized = true
    }
    
    private fun loadEnabledProviders() {
        val saved = prefs?.getStringSet("enabled", null)
        if (saved != null) {
            enabledProviders.clear()
            enabledProviders.addAll(saved)
        } else {
            enabledProviders.addAll(allProviders.keys)
        }
    }
    
    private fun saveEnabledProviders() {
        prefs?.edit()?.putStringSet("enabled", enabledProviders)?.apply()
    }
    
    fun register(provider: Provider) {
        allProviders[provider.value] = provider
        if (!enabledProviders.contains(provider.value)) {
            enabledProviders.add(provider.value)
            saveEnabledProviders()
        }
    }
    
    fun unregister(value: String) {
        allProviders.remove(value)
        enabledProviders.remove(value)
        saveEnabledProviders()
    }
    
    fun getAll(): List<Provider> = allProviders.values.toList()
    
    fun getEnabled(): List<Provider> = allProviders.values.filter { enabledProviders.contains(it.value) }
    
    fun getAllInfo(): List<ProviderInfo> = allProviders.values.map { it.getInfo(enabledProviders.contains(it.value)) }
    
    fun isEnabled(value: String): Boolean = enabledProviders.contains(value)
    
    fun setEnabled(value: String, enabled: Boolean) {
        if (enabled) {
            enabledProviders.add(value)
        } else {
            enabledProviders.remove(value)
        }
        saveEnabledProviders()
    }
    
    fun getByValue(value: String): Provider? = allProviders[value]
    
    fun isInitialized(): Boolean = initialized
}
