package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import openfluxdesktop.shared.generated.resources.Res
import openfluxdesktop.shared.generated.resources.ic_launcher
import org.jetbrains.compose.resources.painterResource
import tech.p1neapplexpress.openfluxdesktop.FluxColors
import tech.p1neapplexpress.openfluxdesktop.OpenFlux.VERSION
import tech.p1neapplexpress.openfluxdesktop.data.ConnectionStats
import tech.p1neapplexpress.openfluxdesktop.data.DownloadStatus
import tech.p1neapplexpress.openfluxdesktop.fluxMaterialTheme
import tech.p1neapplexpress.openfluxdesktop.ui.Destination.*

@Composable
fun WelcomeScreen(
    downloadStatus: DownloadStatus,
    navigationHandler: (Destination) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focused = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused.value) 1.1f else 1f)

    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
        label = "logo-alpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.85f,
        animationSpec = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
        label = "logo-scale",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1600, easing = LinearOutSlowInEasing),
        label = "title-alpha",
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
        label = "button-alpha",
    )

    LaunchedEffect(Unit) {
        delay(300)
        logoVisible = true
        delay(1600)
        titleVisible = true
        delay(1000)
        buttonVisible = true
        focusRequester.requestFocus()
    }

    val ready = downloadStatus is DownloadStatus.Ready

    Column(
        modifier = Modifier
            .focusable()
            .focusProperties {
                right = focusRequester
                left = focusRequester
                down = focusRequester
                up = focusRequester
            }
            .background(color = fluxMaterialTheme.background)
            .safeContentPadding()
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
        ) {
            Column {
                Image(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .alpha(logoAlpha)
                        .scale(logoScale),
                    painter = painterResource(Res.drawable.ic_launcher),
                    contentDescription = null,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(titleAlpha),
                    text = "OpenFlux Desktop v$VERSION",
                    fontSize = 20.sp,
                    color = FluxColors.text,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(64.dp))

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .alpha(buttonAlpha)
                        .width(200.dp),
                ) {
                    if (ready) {
                        NextButton(
                            focusRequester = focusRequester,
                            focused = focused,
                            scale = scale,
                            onClick = { navigationHandler.invoke(Transport) },
                        )
                    } else {
                        LoadingBar(status = downloadStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextButton(
    focusRequester: FocusRequester,
    focused: androidx.compose.runtime.MutableState<Boolean>,
    scale: Float,
    onClick: () -> Unit,
) {
    val shape = RectangleShape
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .scale(scale)
            .onFocusChanged { focused.value = it.isFocused }
            .focusRequester(focusRequester)
            .border(1.dp, fluxMaterialTheme.primary, shape),
        onClick = onClick,
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = "Next",
        )
    }
}

@Composable
private fun LoadingBar(status: DownloadStatus) {
    val progress: Float = when (status) {
        is DownloadStatus.Downloading -> status.progress
        is DownloadStatus.Checking -> 0.05f
        is DownloadStatus.Idle -> 0.02f
        else -> 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "loading-bar",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Фоновая полоска
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FluxColors.surfaceDark),
        ) {
            // Заполненная часть
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(FluxColors.accent),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val label = when (status) {
            is DownloadStatus.Idle -> "Initializing"
            is DownloadStatus.Checking -> "Checking updates"
            is DownloadStatus.Downloading -> {
                val mbDone = "%.1f".format(status.bytesDownloaded / 1024f / 1024f)
                val mbTotal = "%.1f".format(status.totalBytes / 1024f / 1024f)
                "Downloading $mbDone / $mbTotal MB"
            }
            is DownloadStatus.Failed -> "Download failed"
            else -> ""
        }

        Text(
            text = label,
            color = FluxColors.textSecondary,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
    }
}