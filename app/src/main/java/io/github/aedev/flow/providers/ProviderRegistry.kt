package io.github.aedev.flow.providers

object ProviderRegistry {
    private val providers = mutableListOf<Provider>()
    
    init {
        register(EpornerProvider())
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
