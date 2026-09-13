package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tech.p1neapplexpress.openfluxdesktop.FluxColors
import tech.p1neapplexpress.openfluxdesktop.data.BackendManager
import tech.p1neapplexpress.openfluxdesktop.data.BackendStatus
import tech.p1neapplexpress.openfluxdesktop.data.ConnectionConfig
import tech.p1neapplexpress.openfluxdesktop.data.PlatformInfo
import tech.p1neapplexpress.openfluxdesktop.data.TransportType
import tech.p1neapplexpress.openfluxdesktop.data.UiState
import tech.p1neapplexpress.openfluxdesktop.data.shortName
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

    val tt = TransportType.valueOf(transportType)
    UiState.lastTransport = transportType

    val locked = backendStatus.isConnected || backendStatus.isRunning

    val pageScroll = rememberScrollState()

    // При старте — плавно в самый низ. При остановке — плавно наверх.
    LaunchedEffect(backendStatus.isRunning) {
        if (backendStatus.isRunning) {
            // ждём, пока layout стабилизируется (maxValue перестанет расти)
            var last = -1
            var stable = 0
            while (stable < 3) {
                kotlinx.coroutines.delay(50)
                val now = pageScroll.maxValue
                if (now == last) stable++ else { stable = 0; last = now }
            }
            pageScroll.animateScrollTo(
                pageScroll.maxValue,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 700,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                )
            )
        } else {
            pageScroll.animateScrollTo(
                0,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 600,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                )
            )
        }
    }

    var showQr by remember { mutableStateOf(false) }
    var qrPayload by remember { mutableStateOf("") }

    LaunchedEffect(backendStatus.exitNodePayload) {
        val p = backendStatus.exitNodePayload
        if (p != null && UiState.mode == "exit-node" && tt == TransportType.CUPSONLINE) {
            qrPayload = p
            showQr = true
        }
    }

    val canConnect: Boolean = when (tt) {
        TransportType.YANDEX, TransportType.VYANDEX -> UiState.url.trim().isNotEmpty()
        TransportType.ONEME -> UiState.maxToken.trim().isNotEmpty() && UiState.maxUid.trim().isNotEmpty()
        TransportType.CUPSONLINE -> if (UiState.mode == "exit-node") true else UiState.url.trim().isNotEmpty()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FluxColors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ───────── Header ─────────
            Box(modifier = Modifier.fillMaxWidth()) {
                // тонкое свечение сверху
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    FluxColors.accent.copy(alpha = 0.06f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackButton(
                            modifier = Modifier.size(36.dp),
                            onBackButtonClick = {
                                if (!locked) {
                                    backendManager.stop()
                                    navigationHandler.invoke(Destination.Back)
                                }
                            },
                            color = if (locked) FluxColors.textHint else FluxColors.white,
                        )
                        Spacer(Modifier.weight(1f))
                        StatusBadge(backendStatus.isRunning, backendStatus.isConnected)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = tt.shortName,
                            color = FluxColors.white,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.6).sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Row(
                            modifier = Modifier
                                .padding(bottom = 7.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(FluxColors.accent.copy(alpha = 0.10f))
                                .border(1.dp, FluxColors.accent.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(FluxColors.accent),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "CONNECTION",
                                color = FluxColors.accent,
                                fontSize = 9.sp,
                                letterSpacing = 2.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // тонкая градиентная линия под шапкой
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        FluxColors.accent.copy(alpha = 0.0f),
                                        FluxColors.accent.copy(alpha = 0.35f),
                                        FluxColors.accent.copy(alpha = 0.0f),
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ───────── Scrollable body ─────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(pageScroll, enabled = !locked)
                    .padding(horizontal = 22.dp),
            ) {

                // ── MODE ──
                Section("MODE")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Client",
                        selected = UiState.mode == "client",
                        onClick = { if (!locked) UiState.mode = "client" },
                        modifier = Modifier.weight(1f),
                        enabled = !locked,
                    )
                    PillButton(
                        text = "Exit Node",
                        selected = UiState.mode == "exit-node",
                        onClick = { if (!locked) UiState.mode = "exit-node" },
                        modifier = Modifier.weight(1f),
                        enabled = !locked,
                    )
                }

                Spacer(Modifier.height(22.dp))

                // ── Transport-specific ──
                when (tt) {
                    TransportType.YANDEX, TransportType.VYANDEX -> {
                        Section("DOCUMENT")
                        Field(
                            value = UiState.url,
                            onValueChange = { UiState.url = it },
                            placeholder = "https://disk.yandex.ru/...",
                            enabled = !locked,
                        )
                    }
                    TransportType.ONEME -> {
                        Section("CREDENTIALS")
                        Field(
                            value = UiState.maxToken,
                            onValueChange = { UiState.maxToken = it },
                            placeholder = "MAX Token",
                            enabled = !locked,
                        )
                        Spacer(Modifier.height(10.dp))
                        Field(
                            value = UiState.maxUid,
                            onValueChange = { UiState.maxUid = it },
                            placeholder = "MAX User ID",
                            enabled = !locked,
                        )
                    }
                    TransportType.CUPSONLINE -> {
                        if (UiState.mode == "client") {
                            Section("ROOM LIST · BASE64")
                            Field(
                                value = UiState.url,
                                onValueChange = { UiState.url = it },
                                placeholder = "paste base64 from exit node",
                                enabled = !locked,
                            )
                        }
                    }
                }

                // ── SOCKS5 (client only) ──
                if (UiState.mode == "client") {
                    Spacer(Modifier.height(22.dp))
                    Section("SOCKS5")
                    Field(
                        value = UiState.socksAddr,
                        onValueChange = { UiState.socksAddr = it },
                        placeholder = ":1080",
                        enabled = !locked,
                    )
                }

                // ── EXIT MODE (linux exit-node only) ──
                if (UiState.mode == "exit-node" && PlatformInfo.os == "linux") {
                    Spacer(Modifier.height(22.dp))
                    Section("EXIT MODE")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            text = "proxy",
                            selected = UiState.exitMode == "proxy",
                            onClick = { if (!locked) UiState.exitMode = "proxy" },
                            modifier = Modifier.weight(1f),
                            enabled = !locked,
                        )
                        PillButton(
                            text = "raw",
                            selected = UiState.exitMode == "raw",
                            onClick = { if (!locked) UiState.exitMode = "raw" },
                            modifier = Modifier.weight(1f),
                            enabled = !locked,
                        )
                    }
                    if (UiState.exitMode == "raw") {
                        Spacer(Modifier.height(10.dp))
                        Field(
                            value = UiState.localIp,
                            onValueChange = { UiState.localIp = it },
                            placeholder = "egress IP (optional)",
                            enabled = !locked,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── Debug toggle ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FluxColors.cardBg.copy(alpha = 0.5f))
                        .border(1.dp, FluxColors.stroke, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Debug logging",
                            color = FluxColors.white,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "verbose output in the log panel",
                            color = FluxColors.textHint,
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp,
                        )
                    }
                    Switch(
                        checked = UiState.debug,
                        onCheckedChange = { if (!locked) UiState.debug = it },
                        enabled = !locked,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FluxColors.accent,
                            checkedTrackColor = FluxColors.accent.copy(alpha = 0.30f),
                            uncheckedThumbColor = FluxColors.textHint,
                            uncheckedTrackColor = FluxColors.surfaceDark,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }

                // ── Speed ──
                if (backendStatus.isRunning && backendStatus.isConnected) {
                    Spacer(Modifier.height(22.dp))
                    SpeedPanel(backendStatus)
                }

                // ── Log ──
                if (backendStatus.isRunning) {
                    Spacer(Modifier.height(18.dp))
                    LogPanel(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        output = backendStatus.output,
                        error = backendStatus.error,
                    )
                }

                Spacer(Modifier.height(22.dp))
            }

            // ───────── Footer ─────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!backendStatus.isRunning && canConnect)
                                Brush.horizontalGradient(
                                    listOf(FluxColors.accent, FluxColors.accentDim)
                                )
                            else if (backendStatus.isRunning)
                                SolidColor(FluxColors.red.copy(alpha = 0.08f))
                            else
                                SolidColor(FluxColors.surfaceDark)
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                backendStatus.isRunning -> FluxColors.red.copy(alpha = 0.45f)
                                canConnect -> Color.Transparent
                                else -> FluxColors.stroke
                            },
                            shape = RoundedCornerShape(16.dp),
                        ),
                ) {
                    if (backendStatus.isRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    backendManager.stop()
                                    scope.launch { snackbarHostState.showSnackbar("Disconnected") }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Disconnect",
                                color = FluxColors.red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = canConnect) {
                                    val config = ConnectionConfig(
                                        mode = UiState.mode,
                                        transport = tt,
                                        url = UiState.url,
                                        maxToken = UiState.maxToken,
                                        maxUid = UiState.maxUid,
                                        socksAddr = UiState.socksAddr,
                                        exitMode = UiState.exitMode,
                                        localIp = UiState.localIp,
                                        debug = UiState.debug,
                                    )
                                    val cmd = config.generateCommand()
                                    if (backendManager.start(cmd)) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (UiState.mode == "exit-node") "Exit node started"
                                                else "Client started"
                                            )
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to start backend")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (UiState.mode == "exit-node") "Start Exit Node" else "Connect",
                                color = if (canConnect) FluxColors.background else FluxColors.textHint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showQr) {
        QrDialog(payloadJson = qrPayload, onDismiss = { showQr = false })
    }
}

// ═══════════════════════════════════════════════════════════════

@Composable
private fun Section(title: String) {
    Row(
        modifier = Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FluxColors.accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = FluxColors.textHint,
            fontSize = 10.sp,
            letterSpacing = 2.6.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(placeholder, color = FluxColors.textHint, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = FluxColors.white,
            unfocusedTextColor = FluxColors.white,
            disabledTextColor = FluxColors.textSecondary,
            cursorColor = FluxColors.accent,
            focusedIndicatorColor = FluxColors.accent.copy(alpha = 0.7f),
            unfocusedIndicatorColor = FluxColors.strokeStrong,
            disabledIndicatorColor = FluxColors.stroke,
            focusedContainerColor = FluxColors.cardBg,
            unfocusedContainerColor = FluxColors.cardBg,
            disabledContainerColor = FluxColors.cardBg.copy(alpha = 0.5f),
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
    )
}

@Composable
private fun PillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bg by animateColorAsState(
        if (selected) FluxColors.accent.copy(alpha = if (enabled) 0.14f else 0.06f) else Color.Transparent,
        label = "pill-bg",
    )
    val border by animateColorAsState(
        when {
            selected && enabled -> FluxColors.accent
            selected && !enabled -> FluxColors.accent.copy(alpha = 0.35f)
            else -> FluxColors.stroke
        },
        label = "pill-border",
    )
    val fg by animateColorAsState(
        when {
            !enabled -> FluxColors.textHint
            selected -> FluxColors.accent
            else -> FluxColors.textSecondary
        },
        label = "pill-fg",
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun StatusBadge(isRunning: Boolean, isConnected: Boolean) {
    val color = when {
        isConnected -> FluxColors.success
        isRunning -> FluxColors.accent
        else -> FluxColors.textHint
    }
    val label = when {
        isConnected -> "CONNECTED"
        isRunning -> "CONNECTING"
        else -> "OFFLINE"
    }

    val infinite = rememberInfiniteTransition(label = "badge-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.5f else 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "badge-scale",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isConnected || isRunning) pulse.coerceIn(0.6f, 1f) else 1f)),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SpeedPanel(status: BackendStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(FluxColors.cardBg, FluxColors.cardBgElevated)
                )
            )
            .border(1.dp, FluxColors.stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            SpeedCell("↑ UP", "%.1f".format(status.upKbps), "KB/s", "${status.pktUp} pkt/s", FluxColors.accent)
        }
        VerticalDivider()
        Box(Modifier.weight(1f)) {
            SpeedCell("↓ DOWN", "%.1f".format(status.downKbps), "KB/s", "${status.pktDown} pkt/s", FluxColors.success)
        }
        VerticalDivider()
        Box(Modifier.weight(1f)) {
            SpeedCell("UPTIME", formatUptime(status.uptimeSec), "", "", FluxColors.textSecondary)
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(38.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        FluxColors.strokeStrong,
                        Color.Transparent,
                    )
                )
            ),
    )
}

@Composable
private fun SpeedCell(title: String, big: String, unit: String, small: String, color: Color) {
    Column {
        Text(title, color = FluxColors.textHint, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                big,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (small.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(small, color = FluxColors.textHint, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun formatUptime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun LogPanel(modifier: Modifier = Modifier, output: String, error: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FluxColors.terminal),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FluxColors.stroke),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FluxColors.success),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "LOG",
                    color = FluxColors.textHint,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${output.lines().size} lines",
                    color = FluxColors.textHint,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                FluxColors.stroke,
                                Color.Transparent,
                            )
                        )
                    )
            )

            Spacer(Modifier.height(8.dp))

            // без внутреннего scroll — весь скролл страницы
            Column(modifier = Modifier.fillMaxWidth()) {
                if (output.isNotEmpty()) {
                    Text(
                        text = output.takeLast(4000),
                        color = FluxColors.terminalText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp,
                    )
                }
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error.takeLast(600),
                        color = FluxColors.red.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
    }
}
