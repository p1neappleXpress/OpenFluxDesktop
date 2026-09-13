package tech.p1neapplexpress.openfluxdesktop.ext

import androidx.navigation.NavHostController
import tech.p1neapplexpress.openfluxdesktop.ui.Destination


fun NavHostController.handleNavigation(action: Destination) {
    when (action) {
        Destination.Back -> {
            if (previousBackStackEntry != null) {
                popBackStack()
            }
        }
        is Destination.Transport -> {
            navigate(Destination.Transport) {
                popUpTo(Destination.Welcome) { inclusive = false }
                launchSingleTop = true
            }
        }
        is Destination.ConnectionConfig -> {
            navigate(action) {
                launchSingleTop = true
            }
        }
        else -> navigate(action) {
            launchSingleTop = true
        }
    }
}
