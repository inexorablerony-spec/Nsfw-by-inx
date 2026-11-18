package io.github.aedev.flow.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.net.ServerSocket

/**
 * WARP Proxy Manager for Cloudflare WARP integration.
 * 
 * This manages a local WARP proxy using usque (libusque.so).
 * Based on Vega's WarpModule implementation.
 * 
 * IMPORTANT: This is an app-level proxy, not a system VPN.
 */
object WarpProxyManager {
    private const val TAG = "WarpProxyManager"
    private const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
    
    private var warpProcess: Process? = null
    private var currentPort: Int? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    enum class State {
        OFF,
        STARTING,
        CONNECTED,
        STOPPING,
        ERROR
    }
    
    private var state = State.OFF
    private var errorMessage: String? = null
    
    fun getState(): State = state
    fun getErrorMessage(): String? = errorMessage
    fun isConnected(): Boolean = isConnected
    fun getPort(): Int? = currentPort
    
    /**
     * Start WARP proxy.
     * Returns true if successfully connected and verified.
     */
    suspend fun start(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (state == State.CONNECTED) return@withContext true
        
        state = State.STARTING
        errorMessage = null
        
        try {
            val binary = getBinaryFile(context)
            if (!binary.exists()) {
                state = State.ERROR
                errorMessage = "libusque.so not found"
                return@withContext false
            }
            
            val port = allocatePort()
            val proc = launchProxy(binary, port)
            
            if (proc == null) {
                state = State.ERROR
                errorMessage = "Failed to start proxy"
                return@withContext false
            }
            
            if (!verifyConnection(port)) {
                proc.destroy()
                state = State.ERROR
                errorMessage = "Connection verification failed"
                return@withContext false
            }
            
            warpProcess = proc
            currentPort = port
            isConnected = true
            state = State.CONNECTED
            
            // Monitor process
            scope.launch {
                try {
                    proc.waitFor()
                } catch (_: Exception) {}
                if (warpProcess == proc) {
                    Log.i(TAG, "WARP process terminated")
                    stop()
                }
            }
            
            Log.i(TAG, "WARP connected on port $port")
            true
        } catch (e: Exception) {
            state = State.ERROR
            errorMessage = e.message
            false
        }
    }
    
    /**
     * Stop WARP proxy.
     */
    fun stop() {
        state = State.STOPPING
        warpProcess?.destroy()
        warpProcess = null
        currentPort = null
        isConnected = false
        state = State.OFF
        Log.i(TAG, "WARP stopped")
    }
    
    private fun getBinaryFile(context: Context): File {
        val nativeLib = File(context.applicationInfo.nativeLibraryDir, "libusque.so")
        if (nativeLib.exists()) return nativeLib
        
        // Fallback: extract from APK
        val fallback = File(context.filesDir, "libusque.so")
        if (fallback.exists()) return fallback
        
        return nativeLib
    }
    
    private fun allocatePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (e: Exception) {
            8086
        }
    }
    
    private fun launchProxy(binary: File, port: Int): Process? {
        return try {
            ProcessBuilder(binary.absolutePath, "http", "--port", port.toString())
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch proxy: ${e.message}")
            null
        }
    }
    
    private suspend fun verifyConnection(port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", port))
            val url = java.net.URL(TRACE_URL)
            val conn = url.openConnection(proxy) as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            response.contains("warp=")
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed: ${e.message}")
            false
        }
    }
}
