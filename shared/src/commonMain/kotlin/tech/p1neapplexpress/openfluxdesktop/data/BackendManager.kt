package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackendStatus(
    val isRunning: Boolean = false,
    val pid: Long? = null,
    val output: String = "",
    val error: String = "",
    val exitCode: Int? = null,
)

class BackendManager {
    private val _status = MutableStateFlow(BackendStatus())
    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    val downloader = BinaryDownloader()
    val downloadStatus: StateFlow<DownloadStatus> = downloader.status

    private var process: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun ensureBinary(): String = downloader.ensureBinary()

    fun start(command: String): Boolean {
        if (process?.isAlive == true) return false

        return try {
            val binaryPath = downloader.getBinaryPath()
            val isWindows = PlatformInfo.os == "windows"

            val actualCommand = if (command.startsWith("./universal-bypass-tool") ||
                command.startsWith("universal-bypass-tool")) {
                val spaceIdx = command.indexOf(' ')
                if (spaceIdx > 0) {
                    binaryPath + command.substring(spaceIdx)
                } else {
                    binaryPath
                }
            } else {
                command
            }

            val processBuilder = if (isWindows) {
                ProcessBuilder("cmd", "/c", actualCommand)
            } else {
                ProcessBuilder(parseCommand(actualCommand))
            }

            processBuilder.redirectErrorStream(false)
            val newProcess = processBuilder.start()
            process = newProcess

            scope.launch {
                val reader = BufferedReader(InputStreamReader(newProcess.inputStream))
                val output = StringBuilder()
                reader.forEachLine { line ->
                    output.appendLine(line)
                    _status.value = _status.value.copy(
                        output = output.toString(),
                        isRunning = newProcess.isAlive,
                    )
                }
            }

            scope.launch {
                val reader = BufferedReader(InputStreamReader(newProcess.errorStream))
                val error = StringBuilder()
                reader.forEachLine { line ->
                    error.appendLine(line)
                    _status.value = _status.value.copy(
                        error = error.toString(),
                        isRunning = newProcess.isAlive,
                    )
                }
            }

            scope.launch {
                val exitCode = newProcess.waitFor()
                _status.value = _status.value.copy(isRunning = false, exitCode = exitCode)
                process = null
            }

            _status.value = BackendStatus(isRunning = true, pid = newProcess.pid())
            true
        } catch (e: Exception) {
            _status.value = BackendStatus(isRunning = false, error = e.message ?: "Unknown error")
            false
        }
    }

    fun stop() {
        process?.let { p ->
            if (p.isAlive) {
                p.destroy()
                if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    p.destroyForcibly()
                }
            }
        }
        process = null
        _status.value = BackendStatus(isRunning = false)
    }

    fun isRunning(): Boolean = process?.isAlive == true

    private fun parseCommand(command: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in command) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        parts.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        return parts
    }
}