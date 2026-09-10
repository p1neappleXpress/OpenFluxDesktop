package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.serialization.Serializable


@Serializable
enum class TransportType {
    YANDEX,
    ONEME
}

@Serializable
data class ConnectionConfig(
    val mode: String,
    val transport: TransportType,
    val url: String = "",
    val maxToken: String = "",
    val maxUid: String = "",
    val socksAddr: String = ":1080",
    val debug: Boolean = true
) {
    fun toArgsList(): List<String> = buildList {
        if (mode == "exit-node") add("--exit-node") else add("--client")

        add("--transport")
        add(if (transport == TransportType.YANDEX) "yandex" else "oneme")

        when (transport) {
            TransportType.YANDEX -> {
                if (url.isNotEmpty()) {
                    add("--url")
                    add(url)
                }
            }
            TransportType.ONEME -> {
                if (maxToken.isNotEmpty()) {
                    add("--maxToken")
                    add(maxToken)
                }
                if (maxUid.isNotEmpty()) {
                    add("--maxUid")
                    add(maxUid)
                }
            }
        }

        if (mode == "client" && socksAddr.isNotEmpty()) {
            add("--socks5")
            add(socksAddr)
        }

        if (debug) add("--debug")
    }

    fun generateCommand(): String {
        val parts = mutableListOf<String>()

        // Используем правильное имя бинарника для текущей ОС
        val binaryName = getBinaryNameForCurrentOS()
        parts.add(binaryName)

        // Mode
        if (mode == "exit-node") {
            parts.add("--exit-node")
        } else {
            parts.add("--client")
        }

        // Transport type
        parts.add("--transport")
        parts.add(if (transport == TransportType.YANDEX) "yandex" else "oneme")

        // Transport-specific parameters
        when (transport) {
            TransportType.YANDEX -> {
                if (url.isNotEmpty()) {
                    parts.add("--url")
                    parts.add("\"$url\"")
                }
            }
            TransportType.ONEME -> {
                if (maxToken.isNotEmpty()) {
                    parts.add("--maxToken")
                    parts.add("\"$maxToken\"")
                }
                if (maxUid.isNotEmpty()) {
                    parts.add("--maxUid")
                    parts.add(maxUid)
                }
            }
        }

        // SOCKS5 (client only)
        if (mode == "client" && socksAddr.isNotEmpty()) {
            parts.add("--socks5")
            parts.add(socksAddr)
        }

        // Debug
        if (debug) {
            parts.add("--debug")
        }

        return parts.joinToString(" ")
    }

    private fun getBinaryNameForCurrentOS(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        return when {
            os.contains("windows") -> "universal-bypass-tool.exe"
            os.contains("mac") || os.contains("darwin") -> {
                when (arch) {
                    "aarch64", "arm64" -> "./universal-bypass-tool-darwin-arm64"
                    else -> "./universal-bypass-tool-darwin-amd64"
                }
            }
            os.contains("linux") -> {
                when (arch) {
                    "aarch64", "arm64" -> "./universal-bypass-tool-linux-arm64"
                    "arm" -> "./universal-bypass-tool-linux-arm"
                    else -> "./universal-bypass-tool-linux-amd64"
                }
            }
            os.contains("android") -> "./universal-bypass-tool-android"
            else -> "./universal-bypass-tool"
        }
    }
}