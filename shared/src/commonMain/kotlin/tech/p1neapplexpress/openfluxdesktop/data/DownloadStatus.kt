package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

private const val GITHUB_REPO = "p1neappleXpress/OpenFlux"
private const val RELEASES_LATEST = "https://github.com/$GITHUB_REPO/releases/latest/download"

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Checking : DownloadStatus()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus() {
        val progress: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }
    data class Ready(val path: String, val version: String) : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

class BinaryDownloader {
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private val binDir: File get() = PlatformInfo.binDir
    private val binaryFile: File get() = File(binDir, PlatformInfo.binaryName + PlatformInfo.binaryExtension)
    private val versionFile: File get() = File(binDir, ".version")

    fun getBinaryPath(): String = binaryFile.absolutePath

    fun isBinaryReady(): Boolean = binaryFile.exists() && binaryFile.canExecute()

    suspend fun ensureBinary(): String = withContext(Dispatchers.IO) {
        if (isBinaryReady()) {
            _status.value = DownloadStatus.Ready(binaryFile.absolutePath, "cached")
            return@withContext binaryFile.absolutePath
        }

        _status.value = DownloadStatus.Checking

        try {
            val wantedName = PlatformInfo.binaryName + PlatformInfo.binaryExtension
            val directUrl = "$RELEASES_LATEST/$wantedName"

            if (binaryFile.exists()) binaryFile.delete()
            binaryFile.parentFile?.mkdirs()

            downloadFile(directUrl, binaryFile)

            if (PlatformInfo.os != "windows") {
                binaryFile.setExecutable(true, false)
            }

            versionFile.writeText("latest")
            _status.value = DownloadStatus.Ready(binaryFile.absolutePath, "latest")
            binaryFile.absolutePath
        } catch (e: Exception) {
            _status.value = DownloadStatus.Failed(e.message ?: "Unknown error")
            throw e
        }
    }

    private fun downloadFile(url: String, dest: File) {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "OpenFlux-Desktop")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000

        try {
            val code = conn.responseCode
            if (code != 200) throw IllegalStateException("Download returned $code for $url")

            val total = conn.contentLengthLong
            var downloaded = 0L

            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        _status.value = DownloadStatus.Downloading(downloaded, total)
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
