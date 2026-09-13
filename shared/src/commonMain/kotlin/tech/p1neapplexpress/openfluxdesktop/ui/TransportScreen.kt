package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openfluxdesktop.shared.generated.resources.Res
import openfluxdesktop.shared.generated.resources.cups
import openfluxdesktop.shared.generated.resources.max_msg
import openfluxdesktop.shared.generated.resources.yandex_docs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tech.p1neapplexpress.openfluxdesktop.FluxColors
import tech.p1neapplexpress.openfluxdesktop.data.TransportType
import tech.p1neapplexpress.openfluxdesktop.fluxMaterialTheme
import tech.p1neapplexpress.openfluxdesktop.ui.views.BackButton

@Composable
fun TransportScreen(navigationHandler: (Destination) -> Unit) {
    val selectedTransport = remember { mutableStateOf<TransportType?>(null) }

    Column(
        modifier = Modifier
            .background(color = fluxMaterialTheme.background)
            .safeContentPadding()
            .fillMaxSize()
    ) {
        BackButton(
            modifier = Modifier.size(48.dp),
            onBackButtonClick = { navigationHandler.invoke(Destination.Back) }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Select Transport",
                color = FluxColors.white,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransportCard(
                    modifier = Modifier.weight(1f),
                    title = "Yandex Docs",
                    icon = Res.drawable.yandex_docs,
                    onClick = {
                        selectedTransport.value = TransportType.YANDEX
                        navigationHandler.invoke(
                            Destination.ConnectionConfig(TransportType.YANDEX.name)
                        )
                    }
                )

                TransportCard(
                    modifier = Modifier.weight(1f),
                    title = "Volga",
                    icon = Res.drawable.yandex_docs,
                    onClick = {
                        selectedTransport.value = TransportType.VYANDEX
                        navigationHandler.invoke(
                            Destination.ConnectionConfig(TransportType.VYANDEX.name)
                        )
                    }
                )

                TransportCard(
                    modifier = Modifier.weight(1f),
                    title = "MAX",
                    icon = Res.drawable.max_msg,
                    onClick = {
                        selectedTransport.value = TransportType.ONEME
                        navigationHandler.invoke(
                            Destination.ConnectionConfig(TransportType.ONEME.name)
                        )
                    }
                )

                TransportCard(
                    modifier = Modifier.weight(1f),
                    title = "Cups",
                    icon = Res.drawable.cups,
                    onClick = {
                        selectedTransport.value = TransportType.CUPSONLINE
                        navigationHandler.invoke(
                            Destination.ConnectionConfig(TransportType.CUPSONLINE.name)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TransportCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: DrawableResource,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(140.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = FluxColors.background.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, FluxColors.white.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(56.dp),
                tint = Color.Unspecified,
                painter = painterResource(icon),
                contentDescription = title
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = FluxColors.white,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
