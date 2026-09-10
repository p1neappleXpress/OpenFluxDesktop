package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_REPO = "p1neappleXpress/OpenFlux"
private const val API_LATEST = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

private val json = Json { ignoreUnknownKeys = true }

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

    private val binDir: File
        get() = PlatformInfo.binDir

    private val binaryFile: File
        get() = File(binDir, PlatformInfo.binaryName + PlatformInfo.binaryExtension)

    private val versionFile: File
        get() = File(binDir, ".version")

    fun getBinaryPath(): String = binaryFile.absolutePath

    fun isBinaryReady(): Boolean = binaryFile.exists() && binaryFile.canExecute()

    suspend fun ensureBinary(): String = withContext(Dispatchers.IO) {
        _status.value = DownloadStatus.Checking

        try {
            val release = fetchLatestRelease()

            val localVersion = if (versionFile.exists()) versionFile.readText().trim() else null
            if (localVersion == release.tagName && binaryFile.exists() && binaryFile.length() > 0) {
                _status.value = DownloadStatus.Ready(binaryFile.absolutePath, release.tagName)
                return@withContext binaryFile.absolutePath
            }

            val wantedName = PlatformInfo.binaryName + PlatformInfo.binaryExtension
            val asset = release.assets.firstOrNull { it.name == wantedName }
                ?: throw IllegalStateException(
                    "Asset '$wantedName' not found in release ${release.tagName}. " +
                            "Available: ${release.assets.joinToString { it.name }}"
                )

            if (binaryFile.exists()) binaryFile.delete()
            binaryFile.parentFile?.mkdirs()

            downloadFile(asset.downloadUrl, binaryFile, asset.size)

            if (PlatformInfo.os != "windows") {
                binaryFile.setExecutable(true, false)
            }

            versionFile.writeText(release.tagName)

            _status.value = DownloadStatus.Ready(binaryFile.absolutePath, release.tagName)
            binaryFile.absolutePath
        } catch (e: Exception) {
            _status.value = DownloadStatus.Failed(e.message ?: "Unknown error")
            throw e
        }
    }

    private fun fetchLatestRelease(): GithubRelease {
        val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "OpenFlux-Desktop")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        try {
            val code = conn.responseCode
            if (code != 200) {
                throw IllegalStateException("GitHub API returned $code")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return json.decodeFromString<GithubRelease>(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(url: String, dest: File, expectedSize: Long) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "OpenFlux-Desktop")
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        try {
            val code = conn.responseCode
            if (code != 200) {
                throw IllegalStateException("Download returned $code for $url")
            }

            val total = if (expectedSize > 0) expectedSize else conn.contentLengthLong
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

            if (expectedSize > 0 && dest.length() != expectedSize) {
                throw IllegalStateException(
                    "Size mismatch: expected $expectedSize, got ${dest.length()}"
                )
            }
        } finally {
            conn.disconnect()
        }
    }
}