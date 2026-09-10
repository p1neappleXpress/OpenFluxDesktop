package tech.p1neapplexpress.openfluxdesktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import tech.p1neapplexpress.openfluxdesktop.data.BackendManager
import tech.p1neapplexpress.openfluxdesktop.ext.handleNavigation
import tech.p1neapplexpress.openfluxdesktop.ui.ConnectionConfigScreen
import tech.p1neapplexpress.openfluxdesktop.ui.Destination.ConnectionConfig
import tech.p1neapplexpress.openfluxdesktop.ui.Destination.Transport
import tech.p1neapplexpress.openfluxdesktop.ui.Destination.Welcome
import tech.p1neapplexpress.openfluxdesktop.ui.TransportScreen
import tech.p1neapplexpress.openfluxdesktop.ui.WelcomeScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    val backendManager = remember { BackendManager() }
    val downloadStatus by backendManager.downloadStatus.collectAsState()

    LaunchedEffect(Unit) {
        try {
            backendManager.ensureBinary()
        } catch (_: Exception) {
        }
    }

    MaterialTheme(colorScheme = fluxMaterialTheme) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = Welcome) {
            composable<Welcome> {
                WelcomeScreen(
                    downloadStatus = downloadStatus,
                    navigationHandler = navController::handleNavigation,
                )
            }

            composable<Transport> {
                TransportScreen(navController::handleNavigation)
            }

            composable<ConnectionConfig> { backStackEntry ->
                val config = backStackEntry.toRoute<ConnectionConfig>()
                ConnectionConfigScreen(
                    transportType = config.transportType,
                    backendManager = backendManager,
                    navigationHandler = navController::handleNavigation,
                )
            }
        }
    }
}