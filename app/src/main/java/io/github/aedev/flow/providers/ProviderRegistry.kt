package io.github.aedev.flow.providers

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

object ProviderRegistry {
    private val allProviders = ConcurrentHashMap<String, Provider>()
    private val enabledProviders = ConcurrentHashMap.newKeySet<String>()
    private val registrationOrder = mutableListOf<String>()
    private var prefs: SharedPreferences? = null
    private var initialized = false
    
    @Synchronized
    fun init(ctx: Context) {
        if (initialized) return
        prefs = ctx.getSharedPreferences("providers", Context.MODE_PRIVATE)
        
        if (allProviders.isEmpty()) {
            register(EpornerProvider(), isNew = true)
            register(WebViewProvider(ctx), isNew = true)
        }
        
        loadEnabledProviders()
        initialized = true
    }
    
    private fun loadEnabledProviders() {
        val saved = prefs?.getStringSet("enabled", null)
        if (saved != null) {
            enabledProviders.clear()
            enabledProviders.addAll(saved)
        }
    }
    
    private fun saveEnabledProviders() {
        val copy = enabledProviders.toSet()
        prefs?.edit()?.putStringSet("enabled", copy)?.apply()
    }
    
    @Synchronized
    fun register(provider: Provider, isNew: Boolean = false) {
        val isFirstRegistration = !allProviders.containsKey(provider.value)
        allProviders[provider.value] = provider
        
        if (isFirstRegistration) {
            registrationOrder.add(provider.value)
            if (isNew) {
                enabledProviders.add(provider.value)
                saveEnabledProviders()
            }
        }
    }
    
    @Synchronized
    fun unregister(value: String) {
        allProviders.remove(value)
        enabledProviders.remove(value)
        registrationOrder.remove(value)
        saveEnabledProviders()
    }
    
    fun getAll(): List<Provider> = registrationOrder.mapNotNull { allProviders[it] }
    
    fun getEnabled(): List<Provider> = registrationOrder.mapNotNull { 
        allProviders[it]?.takeIf { enabledProviders.contains(it.value) }
    }
    
    fun getAllInfo(): List<ProviderInfo> = registrationOrder.mapNotNull { 
        allProviders[it]?.getInfo(enabledProviders.contains(it))
    }
    
    fun isEnabled(value: String): Boolean = enabledProviders.contains(value)
    
    @Synchronized
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
