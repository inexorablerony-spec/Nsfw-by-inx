package io.github.aedev.flow.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * WARP Proxy Manager for Cloudflare WARP integration.
 * 
 * Based on Vega's WarpModule implementation.
 * 
 * IMPORTANT: This is an app-level proxy, not a system VPN.
 * Connection is only reported after Cloudflare trace confirms "warp=on".
 * 
 * NOTE: This implementation requires libusque.so native binary.
 * The binary must be built from usque source and included in the APK.
 */
object WarpProxyManager {
    private const val TAG = "WarpProxyManager"
    private const val TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
    private const val MAX_RETRIES = 10
    private const val RETRY_DELAY_MS = 500L
    private const val SHUTDOWN_TIMEOUT_MS = 3000L
    private const val MAX_LOG_LINES = 100
    
    private val lock = Any()
    private var warpProcess: Process? = null
    private var currentPort: Int? = null
    private var startJob: Job? = null
    private var outputJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processLogs = mutableListOf<String>()
    
    enum class State {
        OFF,
        STARTING,
        CONNECTED,
        STOPPING,
        ERROR
    }
    
    private val _state = MutableStateFlow(State.OFF)
    val state: StateFlow<State> = _state
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    fun getState(): State = _state.value
    fun getErrorMessage(): String? = _errorMessage.value
    fun isConnected(): Boolean = _state.value == State.CONNECTED
    fun getPort(): Int? = currentPort
    fun getLogs(): List<String> = processLogs.toList()
    
    /**
     * Start WARP proxy.
     * Returns true only after Cloudflare trace confirms "warp=on".
     */
    suspend fun start(context: Context): Boolean = withContext(Dispatchers.IO) {
        val shouldStart = synchronized(lock) {
            if (_state.value == State.CONNECTED || _state.value == State.STARTING) {
                false
            } else {
                _state.value = State.STARTING
                _errorMessage.value = null
                true
            }
        }
        
        if (!shouldStart) return@withContext _state.value == State.CONNECTED
        
        startJob = coroutineContext[Job]
        
        try {
            val binary = getBinaryFile(context)
            if (!binary.exists()) {
                setError("libusque.so not found. Build from usque source.")
                return@withContext false
            }
            
            val port = allocatePort()
            val proc = launchProxy(binary, port)
            
            if (proc == null) {
                setError("Failed to start proxy process")
                return@withContext false
            }
            
            // Start output drain
            startOutputDrain(proc)
            
            // Check if cancelled
            if (!isActive) {
                cleanup(proc)
                return@withContext false
            }
            
            if (!verifyConnection(port, proc)) {
                cleanup(proc)
                setError("Connection verification failed")
                return@withContext false
            }
            
            // Check if cancelled before setting CONNECTED
            if (!isActive) {
                cleanup(proc)
                return@withContext false
            }
            
            synchronized(lock) {
                warpProcess = proc
                currentPort = port
            }
            _state.value = State.CONNECTED
            
            // Monitor process
            scope.launch {
                try {
                    proc.waitFor()
                } catch (_: Exception) {}
                synchronized(lock) {
                    if (warpProcess == proc) {
                        Log.i(TAG, "WARP process terminated unexpectedly")
                        warpProcess = null
                        currentPort = null
                        _state.value = State.ERROR
                        _errorMessage.value = "Process terminated unexpectedly"
                    }
                }
            }
            
            Log.i(TAG, "WARP connected on port $port")
            true
        } catch (e: CancellationException) {
            cleanup(warpProcess)
            _state.value = State.OFF
            false
        } catch (e: Exception) {
            setError(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Stop WARP proxy.
     */
    fun stop() {
        synchronized(lock) {
            if (_state.value == State.OFF) return
            
            // Cancel startup job
            startJob?.cancel()
            startJob = null
            
            _state.value = State.STOPPING
            
            val proc = warpProcess
            warpProcess = null
            currentPort = null
            
            if (proc != null) {
                cleanup(proc)
            }
        }
        _state.value = State.OFF
        _errorMessage.value = null
        Log.i(TAG, "WARP stopped")
    }
    
    private fun setError(message: String) {
        _state.value = State.ERROR
        _errorMessage.value = message
        Log.e(TAG, message)
    }
    
    private fun cleanup(proc: Process?) {
        proc ?: return
        outputJob?.cancel()
        outputJob = null
        
        proc.destroy()
        try {
            if (!proc.waitFor(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                proc.destroyForcibly()
            }
        } catch (_: Exception) {}
    }
    
    private fun startOutputDrain(proc: Process) {
        outputJob = scope.launch {
            try {
                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line: String?
                    while (isActive && reader.readLine().also { line = it } != null) {
                        line?.let { log ->
                            synchronized(processLogs) {
                                processLogs.add(log)
                                if (processLogs.size > MAX_LOG_LINES) {
                                    processLogs.removeAt(0)
                                }
                            }
                            Log.d(TAG, "usque: $log")
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    
    private fun getBinaryFile(context: Context): File {
        val nativeLib = File(context.applicationInfo.nativeLibraryDir, "libusque.so")
        if (nativeLib.exists()) return nativeLib
        
        val fallback = File(context.filesDir, "libusque.so")
        if (fallback.exists()) return fallback
        
        return nativeLib
    }
    
    private fun allocatePort(): Int {
        return try {
            ServerSocket(0).use { socket ->
                socket.localPort
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to allocate port: ${e.message}")
            8086
        }
    }
    
    private fun launchProxy(binary: File, port: Int): Process? {
        return try {
            // NOTE: CLI syntax needs to be verified against usque source
            // Current assumption: "usque http --port PORT"
            ProcessBuilder(binary.absolutePath, "http", "--port", port.toString())
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch proxy: ${e.message}")
            null
        }
    }
    
    private suspend fun verifyConnection(port: Int, proc: Process): Boolean = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES) { attempt ->
            if (!isActive || !proc.isAlive) {
                Log.e(TAG, "Process exited or cancelled during verification")
                return@withContext false
            }
            
            try {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", port))
                val url = URL(TRACE_URL)
                val conn = url.openConnection(proxy) as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                try {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    if (response.contains("warp=on")) {
                        Log.i(TAG, "Verification successful")
                        return@withContext true
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Verification attempt ${attempt + 1} failed: ${e.message}")
            }
            
            delay(RETRY_DELAY_MS)
        }
        false
    }
}
