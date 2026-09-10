package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.p1neapplexpress.openfluxdesktop.FluxColors
import tech.p1neapplexpress.openfluxdesktop.data.BackendManager
import tech.p1neapplexpress.openfluxdesktop.data.ConnectionConfig
import tech.p1neapplexpress.openfluxdesktop.data.TransportType
import tech.p1neapplexpress.openfluxdesktop.data.TunnelPayload
import tech.p1neapplexpress.openfluxdesktop.data.toQrString
import tech.p1neapplexpress.openfluxdesktop.ui.views.BackButton

@Composable
fun ConnectionConfigScreen(
    transportType: String,
    backendManager: BackendManager,
    navigationHandler: (Destination) -> Unit,
) {
    val backendStatus by backendManager.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("client") }
    var url by remember { mutableStateOf("") }
    var maxToken by remember { mutableStateOf("") }
    var maxUid by remember { mutableStateOf("") }
    var socksAddr by remember { mutableStateOf(":1080") }
    var debug by remember { mutableStateOf(true) }

    var showQr by remember { mutableStateOf(false) }
    var qrPayload by remember { mutableStateOf("") }

    val canConnect: Boolean = when (transportType) {
        TransportType.YANDEX.name -> url.trim().isNotEmpty()
        TransportType.ONEME.name -> maxToken.trim().isNotEmpty() && maxUid.trim().isNotEmpty()
        else -> false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FluxColors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // ── Header ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(
                    modifier = Modifier.size(40.dp),
                    onBackButtonClick = {
                        backendManager.stop()
                        navigationHandler.invoke(Destination.Back)
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusIndicator(isConnected = backendStatus.isRunning)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title ───────────────────────────────────────────
            Text(
                text = "Connection",
                color = FluxColors.white,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transportType.replaceFirstChar { it.uppercase() },
                color = FluxColors.textSecondary,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Mode ────────────────────────────────────────────
            SectionLabel("Mode")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    text = "Client",
                    selected = mode == "client",
                    onClick = { mode = "client" },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Exit Node",
                    selected = mode == "exit-node",
                    onClick = { mode = "exit-node" },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Transport fields ────────────────────────────────
            when (transportType) {
                TransportType.YANDEX.name -> {
                    SectionLabel("Document")
                    Spacer(modifier = Modifier.height(10.dp))
                    Field(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "https://disk.yandex.ru/...",
                    )
                }
                TransportType.ONEME.name -> {
                    SectionLabel("Credentials")
                    Spacer(modifier = Modifier.height(10.dp))
                    Field(
                        value = maxToken,
                        onValueChange = { maxToken = it },
                        placeholder = "MAX Token",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Field(
                        value = maxUid,
                        onValueChange = { maxUid = it },
                        placeholder = "MAX User ID",
                    )
                }
            }

            if (mode == "client") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionLabel("SOCKS5")
                Spacer(modifier = Modifier.height(10.dp))
                Field(
                    value = socksAddr,
                    onValueChange = { socksAddr = it },
                    placeholder = ":1080",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Debug ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Debug logging",
                    color = FluxColors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = debug,
                    onCheckedChange = { debug = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FluxColors.accent,
                        checkedTrackColor = FluxColors.accent.copy(alpha = 0.25f),
                        uncheckedThumbColor = FluxColors.textHint,
                        uncheckedTrackColor = FluxColors.surfaceDark,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Action ──────────────────────────────────────────
            if (!backendStatus.isRunning) {
                Button(
                    onClick = {
                        val config = ConnectionConfig(
                            mode = mode,
                            transport = TransportType.valueOf(transportType),
                            url = url,
                            maxToken = maxToken,
                            maxUid = maxUid,
                            socksAddr = socksAddr,
                            debug = debug,
                        )
                        val command = config.generateCommand()

                        val started = backendManager.start(command)

                        if (started) {
                            if (mode == "exit-node") {
                                val payload = TunnelPayload(
                                    id = System.currentTimeMillis(),
                                    name = "Exit Node",
                                    transportType = transportType.lowercase(),
                                    transportConnPayload = config.toArgsList(),
                                )
                                qrPayload = payload.toQrString()
                                showQr = true
                                scope.launch { snackbarHostState.showSnackbar("Exit node started") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Client started") }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Failed to start backend") }
                        }
                    },
                    enabled = canConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FluxColors.accent,
                        contentColor = FluxColors.background,
                        disabledContainerColor = FluxColors.surfaceDark,
                        disabledContentColor = FluxColors.textHint,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = if (mode == "exit-node") "Connect Device" else "Connect",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                }

                if (!canConnect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (transportType) {
                            TransportType.YANDEX.name -> "Enter document URL"
                            TransportType.ONEME.name -> "Enter MAX token and user ID"
                            else -> ""
                        },
                        color = FluxColors.textHint,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        backendManager.stop()
                        scope.launch { snackbarHostState.showSnackbar("Disconnected") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        FluxColors.red.copy(alpha = 0.6f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FluxColors.red,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "Disconnect",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                }
            }

            // ── Terminal (только при работе) ─────────────────────
            if (backendStatus.isRunning &&
                (backendStatus.output.isNotEmpty() || backendStatus.error.isNotEmpty())
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                TerminalBox(
                    output = backendStatus.output,
                    error = backendStatus.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showQr) {
        QrDialog(
            payloadJson = qrPayload,
            onDismiss = { showQr = false },
        )
    }
}

// ══════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = FluxColors.textSecondary,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = FluxColors.textHint, fontSize = 14.sp)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedTextColor = FluxColors.white,
            unfocusedTextColor = FluxColors.white,
            cursorColor = FluxColors.accent,
            focusedIndicatorColor = FluxColors.accent.copy(alpha = 0.6f),
            unfocusedIndicatorColor = FluxColors.textHint.copy(alpha = 0.4f),
            focusedContainerColor = FluxColors.surfaceDark,
            unfocusedContainerColor = FluxColors.surfaceDark,
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun PillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (selected) FluxColors.accent.copy(alpha = 0.15f) else Color.Transparent,
        label = "pill-bg",
    )
    val border by animateColorAsState(
        if (selected) FluxColors.accent else FluxColors.textHint.copy(alpha = 0.3f),
        label = "pill-border",
    )
    val fg by animateColorAsState(
        if (selected) FluxColors.accent else FluxColors.textSecondary,
        label = "pill-fg",
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            border = null,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = fg,
            ),
        ) {
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun StatusIndicator(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )

    val dotColor by animateColorAsState(
        if (isConnected) FluxColors.success else FluxColors.textHint,
        label = "dot-color",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(if (isConnected) pulse else 1f)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) "Connected" else "Offline",
            color = if (isConnected) FluxColors.success else FluxColors.textSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun TerminalBox(output: String, error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FluxColors.terminal),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "LOG",
                color = FluxColors.textHint,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (output.isNotEmpty()) {
                Text(
                    text = output.takeLast(400),
                    color = FluxColors.terminalText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                )
            }
            if (error.isNotEmpty()) {
                if (output.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error.takeLast(400),
                    color = FluxColors.red.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}