package tech.p1neapplexpress.openfluxdesktop.ext

import androidx.navigation.NavHostController
import tech.p1neapplexpress.openfluxdesktop.ui.Destination

fun NavHostController.handleNavigation(action: Destination) {
    when (action) {
        Destination.Back -> popBackStack()
        else -> navigate(action)
    }
}