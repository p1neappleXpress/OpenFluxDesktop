package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.serialization.Serializable


@Serializable
enum class TransportType {
    YANDEX,
    VYANDEX,
    ONEME,
    CUPSONLINE
}

val TransportType.cliName: String
    get() = when (this) {
        TransportType.YANDEX -> "yandex"
        TransportType.VYANDEX -> "vyandex"
        TransportType.ONEME -> "oneme"
        TransportType.CUPSONLINE -> "cupsonline"
    }

val TransportType.displayName: String
    get() = when (this) {
        TransportType.YANDEX -> "Yandex Docs"
        TransportType.VYANDEX -> "Yandex Volga"
        TransportType.ONEME -> "MAX Messenger"
        TransportType.CUPSONLINE -> "Cups.online"
    }

val TransportType.shortName: String
    get() = when (this) {
        TransportType.YANDEX -> "Yandex"
        TransportType.VYANDEX -> "Volga"
        TransportType.ONEME -> "MAX"
        TransportType.CUPSONLINE -> "Cups"
    }


@Serializable
data class ConnectionConfig(
    val mode: String,
    val transport: TransportType,
    val url: String = "",
    val maxToken: String = "",
    val maxUid: String = "",
    val socksAddr: String = ":1080",
    val exitMode: String = "proxy",
    val localIp: String = "",
    val debug: Boolean = true,
) {
    fun toArgsList(): List<String> = buildList {
        if (mode == "exit-node") add("--exit-node") else add("--client")

        add("--transport")
        add(transport.cliName)

        // --mode / --local-ip: только exit-node + только Linux + raw
        if (mode == "exit-node" && PlatformInfo.os == "linux" && exitMode == "raw") {
            add("--mode"); add("raw")
            if (localIp.isNotBlank()) { add("--local-ip"); add(localIp) }
        }

        when (transport) {
            TransportType.YANDEX, TransportType.VYANDEX ->
                if (url.isNotBlank()) { add("--url"); add(url) }
            TransportType.ONEME -> {
                if (maxToken.isNotBlank()) { add("--maxToken"); add(maxToken) }
                if (maxUid.isNotBlank()) { add("--maxUid"); add(maxUid) }
            }
            TransportType.CUPSONLINE -> if (mode == "client" && url.isNotBlank()) {
                add("--url"); add(url)
            }
        }

        if (mode == "client" && socksAddr.isNotBlank()) {
            add("--socks5"); add(socksAddr)
        }

        if (debug) add("--debug")
    }

    fun generateCommand(): String {
        val parts = mutableListOf<String>()
        parts.add(PlatformInfo.binaryName)
        parts.addAll(toArgsList())
        return parts.joinToString(" ")
    }
}
