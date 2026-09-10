package tech.p1neapplexpress.openfluxdesktop.ui

import kotlinx.serialization.Serializable
import tech.p1neapplexpress.openfluxdesktop.data.TransportType


sealed class Destination {
    @Serializable
    data object Welcome: Destination()

    @Serializable
    data object Transport: Destination()

    @Serializable
    data class ConnectionConfig(
        val transportType: String  // Изменено с TransportType на String
    ): Destination()

    data object Back: Destination()
}