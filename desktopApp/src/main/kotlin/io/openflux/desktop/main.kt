package io.openflux.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import io.openflux.desktop.data.AppDirs
import io.openflux.desktop.model.isActive
import io.openflux.desktop.model.profile
import io.openflux.desktop.platform.RELAUNCHED_ARG
import io.openflux.app.account.AccountNotifier
import io.openflux.app.platform.DesktopScrollbars
import io.openflux.app.platform.Shortcuts
import io.openflux.app.shell.OpenFluxApp
import io.openflux.app.shell.connectChosen
import io.openflux.app.shell.stopConnection
import io.openflux.desktop.resources.Res
import io.openflux.desktop.resources.ic_openflux_foreground
import io.openflux.desktop.web.KcefBrowserViews
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlin.system.exitProcess

/** Set by the build (-Dopenflux.version); "dev" when run some other way. */
private val APP_VERSION = System.getProperty("openflux.version") ?: "dev"

/** Held for the life of the process so a second copy cannot fight over the port and the proxy. */
private fun acquireSingleInstance(): FileLock? {
    val file = AppDirs.runtime.resolve("instance.lock")
    file.parentFile.mkdirs()
    return runCatching { RandomAccessFile(file, "rw").channel.tryLock() }.getOrNull()
}

fun main(args: Array<String>) {
    // A copy started as administrator waits for the one that started it to exit.
    var lock = acquireSingleInstance()
    if (lock == null && RELAUNCHED_ARG in args) {
        repeat(40) {
            if (lock == null) {
                Thread.sleep(250)
                lock = acquireSingleInstance()
            }
        }
    }
    if (lock == null) {
        JOptionPane.showMessageDialog(null, "OpenFlux уже запущен — посмотрите значок в области уведомлений.", "OpenFlux", JOptionPane.INFORMATION_MESSAGE)
        exitProcess(0)
    }

    val container = createAppContainer(APP_VERSION)
    val stopped = AtomicBoolean(false)
    val stop = { if (stopped.compareAndSet(false, true)) { container.connection.shutdown(); container.phpHosting.close() } }
    // The core and the Windows proxy must not outlive the app, even on logoff or a kill from the task manager.
    Runtime.getRuntime().addShutdownHook(Thread({ stop() }, "openflux-shutdown"))

    // What the bot used to send as messages (the end of the subscription, a paused connection, a payment) the app
    // says by itself, through the tray.
    val notifier = AccountNotifier(container.bot.state, container.bot::setNoticed, System::currentTimeMillis) { container.platform.utcOffsetSeconds }
    val shortcuts = Shortcuts()
    application(exitProcessOnExit = true) {
        val settings by container.settings.settings.collectAsState()
        val state by container.connection.state.collectAsState()
        var visible by remember { mutableStateOf(true) }
        val quit = {
            stop()
            exitApplication()
        }
        val windowState = remember {
            val initial = initialWindow(container.settings.settings.value.window)
            WindowState(placement = initial.placement, position = initial.position, size = initial.size)
        }
        // Remember where the window is, for the next start; written once it settles.
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.placement, windowState.size, windowState.position) }.collectLatest {
                delay(700)
                container.settings.update { s ->
                    val bounds = windowState.boundsToSave(s.window)
                    if (bounds == null || bounds == s.window) s else s.copy(window = bounds)
                }
            }
        }

        LaunchedEffect(Unit) {
            if (settings.autoConnect) connectChosen(container)
        }

        val trayState = rememberTrayState()
        LaunchedEffect(Unit) {
            // The tray listens first: a notice said before anyone listens would be lost, yet kept as said.
            launch(start = CoroutineStart.UNDISPATCHED) { notifier.notices.collect { trayState.sendNotification(Notification(it.title, it.text, Notification.Type.Info)) } }
            notifier.start(this)
        }
        val icon = painterResource(Res.drawable.ic_openflux_foreground)
        Tray(
            state = trayState,
            icon = icon,
            tooltip = if (state.isActive) "OpenFlux — ${state.profile?.name ?: "подключено"}" else "OpenFlux — отключено",
            onAction = { visible = true },
            menu = {
                Item("Открыть OpenFlux", onClick = { visible = true })
                if (state.isActive) {
                    Item("Отключить", onClick = { stopConnection(container) })
                } else {
                    Item("Подключить", onClick = { connectChosen(container) })
                }
                Separator()
                Item("Выход", onClick = quit)
            },
        )

        Window(
            onCloseRequest = { if (settings.closeToTray) visible = false else quit() },
            state = windowState,
            visible = visible,
            title = "OpenFlux",
            icon = icon,
            onPreviewKeyEvent = shortcuts::handle,
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(MIN_WINDOW.width.value.toInt(), MIN_WINDOW.height.value.toInt()) }
            OpenFluxApp(container, DesktopScrollbars, shortcuts, KcefBrowserViews)
        }
    }
}
