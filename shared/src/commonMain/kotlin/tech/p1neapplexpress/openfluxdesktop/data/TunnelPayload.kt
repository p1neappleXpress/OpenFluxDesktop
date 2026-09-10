package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TunnelPayload(
    val id: Long,
    val name: String,
    @SerialName("transportType") val transportType: String,
    @SerialName("transportConnPayload") val transportConnPayload: List<String>,
)

private val qrJson = Json { prettyPrint = false }

fun TunnelPayload.toQrString(): String = qrJson.encodeToString(this)