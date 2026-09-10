package tech.p1neapplexpress.openfluxdesktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenFluxDesktop",
    ) {
        App()
    }
}