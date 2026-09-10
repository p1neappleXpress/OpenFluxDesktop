package tech.p1neapplexpress.openfluxdesktop.data

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionStats (
    val isConnected: Boolean,
    val rx: Int,
    val tx: Int,
    val pkts: Int,
)