package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

class BackendManager {
    private val _status = MutableStateFlow(BackendStatus())
    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    val downloader = BinaryDownloader()
    val downloadStatus: StateFlow<DownloadStatus> = downloader.status

    private var process: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)
    private val pktsIn = AtomicLong(0)
    private val pktsOut = AtomicLong(0)
    private val startedAt = AtomicLong(0)

    suspend fun ensureBinary(): String = downloader.ensureBinary()

    fun start(command: String): Boolean {
        if (process?.isAlive == true) return false

        return try {
            val binaryPath = downloader.getBinaryPath()
            val isWindows = PlatformInfo.os == "windows"

            val actualCommand = if (command.startsWith("./openflux-") ||
                command.startsWith("openflux-") ||
                command.startsWith("./universal-bypass-tool") ||
                command.startsWith("universal-bypass-tool")) {
                val spaceIdx = command.indexOf(' ')
                if (spaceIdx > 0) binaryPath + command.substring(spaceIdx) else binaryPath
            } else command

            val pb = ProcessBuilder(parseCommand(actualCommand))
            pb.redirectErrorStream(true)
            val newProcess = pb.start()
            process = newProcess

            bytesIn.set(0); bytesOut.set(0); pktsIn.set(0); pktsOut.set(0)
            startedAt.set(System.currentTimeMillis())

            scope.launch {
                val reader = BufferedReader(InputStreamReader(newProcess.inputStream))
                val output = StringBuilder()
                var conn = false
                var payload: String? = null
                var inPayload = false
                var payloadLines = mutableListOf<String>()

                reader.forEachLine { raw ->
                    output.appendLine(raw)
                    if (output.length > 64_000) output.delete(0, output.length - 48_000)

                    val line = raw.trim()

                    if (!conn) {
                        if (line.contains("transport started") ||
                            line.contains("WS connected") ||
                            line.contains("WebSocket connected") ||
                            line.contains("Running as CLIENT") ||
                            line.contains("Running as EXIT NODE") ||
                            line.contains("L3 packet tunnel started") ||
                            line.contains("writer loop started") ||
                            line.contains("WS ready")) {
                            conn = true
                        }
                    }

                    val arrow = parseArrow(line)
                    if (arrow != null) {
                        val (isIn, bytes) = arrow
                        if (isIn) {
                            bytesIn.addAndGet(bytes.toLong())
                            pktsIn.incrementAndGet()
                        } else {
                            bytesOut.addAndGet(bytes.toLong())
                            pktsOut.incrementAndGet()
                        }
                    }

                    if (line.contains("=== COPY THIS TO CLIENT ===")) {
                        inPayload = true
                        payloadLines = mutableListOf()
                    } else if (inPayload) {
                        if (line.contains("===========================")) {
                            inPayload = false
                            payload = payloadLines.firstOrNull { it.isNotBlank() }
                        } else if (line.isNotBlank()) {
                            payloadLines.add(line)
                        }
                    }

                    _status.value = _status.value.copy(
                        isRunning = newProcess.isAlive,
                        isConnected = conn,
                        output = output.toString(),
                        uptimeSec = (System.currentTimeMillis() - startedAt.get()) / 1000,
                        exitNodePayload = payload,
                    )
                }
            }

            scope.launch {
                val exitCode = newProcess.waitFor()
                _status.value = _status.value.copy(
                    isRunning = false, isConnected = false,
                    exitCode = exitCode,
                    upKbps = 0f, downKbps = 0f, pktUp = 0, pktDown = 0,
                )
                process = null
            }

            scope.launch {
                while (newProcess.isAlive) {
                    delay(2000)
                    val inB = bytesIn.getAndSet(0)
                    val outB = bytesOut.getAndSet(0)
                    val inP = pktsIn.getAndSet(0)
                    val outP = pktsOut.getAndSet(0)

                    _status.value = _status.value.copy(
                        upKbps = inB / 1024f / 2f,
                        downKbps = outB / 1024f / 2f,
                        pktUp = (inP / 2).toInt(),
                        pktDown = (outP / 2).toInt(),
                    )
                }
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
                try {
                    if (PlatformInfo.os == "windows") {
                        // /T — убить дерево процессов, /F — force
                        ProcessBuilder(
                            "taskkill", "/F", "/T", "/PID", p.pid().toString()
                        ).redirectErrorStream(true).start().waitFor(3, TimeUnit.SECONDS)
                    } else {
                        p.destroy()
                        if (!p.waitFor(2, TimeUnit.SECONDS)) p.destroyForcibly()
                    }
                } catch (_: Exception) {
                    runCatching { p.destroyForcibly() }
                }
            }
        }
        process = null
        _status.value = BackendStatus(isRunning = false, isConnected = false)
    }

    fun isRunning(): Boolean = process?.isAlive == true

    fun clearPayload() {
        _status.value = _status.value.copy(exitNodePayload = null)
    }

    private fun parseArrow(line: String): Pair<Boolean, Int>? {
        val idxIn = line.indexOf("<- ")
        val idxOut = line.indexOf("-> ")
        val idx = when {
            idxIn >= 0 && (idxOut < 0 || idxIn < idxOut) -> idxIn
            idxOut >= 0 -> idxOut
            else -> return null
        }
        val isIn = idx == idxIn
        val rest = line.substring(idx + 3)
        val numStr = rest.substringBefore(' ').trim()
        val bytes = numStr.toIntOrNull() ?: return null
        return isIn to bytes
    }

    private fun parseCommand(command: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (char in command) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) { parts.add(current.toString()); current.clear() }
                }
                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        return parts
    }
}
