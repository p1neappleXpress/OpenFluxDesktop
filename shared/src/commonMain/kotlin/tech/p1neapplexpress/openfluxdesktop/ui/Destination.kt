package tech.p1neapplexpress.openfluxdesktop.ui

import kotlinx.serialization.Serializable


sealed class Destination {
    @Serializable
    data object Welcome : Destination()

    @Serializable
    data object Transport : Destination()

    @Serializable
    data class ConnectionConfig(val transportType: String) : Destination()

    @Serializable
    data object Back : Destination()
}
