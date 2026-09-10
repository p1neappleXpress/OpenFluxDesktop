package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.p1neapplexpress.openfluxdesktop.FluxColors
import tech.p1neapplexpress.openfluxdesktop.data.DownloadStatus

@Composable
fun DownloadOverlay(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FluxColors.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            when (status) {
                is DownloadStatus.Idle -> {
                    CircularProgressIndicator(color = FluxColors.accent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Initializing...",
                        color = FluxColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }

                is DownloadStatus.Checking -> {
                    CircularProgressIndicator(color = FluxColors.accent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Checking latest release...",
                        color = FluxColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }

                is DownloadStatus.Downloading -> {
                    val progress by animateFloatAsState(
                        targetValue = status.progress,
                        label = "download-progress",
                    )

                    val mbDone = "%.1f".format(status.bytesDownloaded / 1024f / 1024f)
                    val mbTotal = "%.1f".format(status.totalBytes / 1024f / 1024f)
                    val percent = (progress * 100).toInt()

                    Text(
                        "Downloading backend",
                        color = FluxColors.white,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$mbDone / $mbTotal MB  ($percent%)",
                        color = FluxColors.textSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = FluxColors.accent,
                        trackColor = Color(0xFF1A1F4B),
                        modifier = Modifier
                            .width(320.dp)
                            .height(6.dp),
                    )
                }

                is DownloadStatus.Ready -> Unit

                is DownloadStatus.Failed -> {
                    Text(
                        "Download failed",
                        color = FluxColors.red,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        status.error,
                        color = FluxColors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}