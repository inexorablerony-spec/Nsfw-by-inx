package io.github.aedev.flow.providers

import android.content.Context

object ProviderRegistry {
    private val providers = mutableListOf<Provider>()
    private var context: Context? = null
    
    fun init(ctx: Context) {
        context = ctx
        if (providers.isEmpty()) {
            register(EpornerProvider())
            register(WebViewProvider(ctx))
        }
    }
    
    fun register(provider: Provider) {
        providers.add(provider)
    }
    
    fun getAll(): List<Provider> = providers.toList()
    
    fun getByValue(value: String): Provider? = providers.find { it.value == value }
    
    fun clear() {
        providers.clear()
    }
}
