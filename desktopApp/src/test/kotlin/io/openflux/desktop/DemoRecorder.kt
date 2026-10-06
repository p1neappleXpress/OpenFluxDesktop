package io.openflux.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.openflux.desktop.core.CliCoreLinks
import io.openflux.desktop.core.CoreBinary
import io.openflux.desktop.model.CoreShareLinkCodec
import io.openflux.desktop.model.ShareLinkCodec
import io.openflux.desktop.model.AppSettings
import io.openflux.desktop.model.CaptchaPrompt
import io.openflux.desktop.model.ConnectionMode
import io.openflux.desktop.model.ConnectionState
import io.openflux.desktop.model.ExitAddress
import io.openflux.desktop.model.ExtraTransport
import io.openflux.desktop.model.LogLevel
import io.openflux.desktop.model.LogLine
import io.openflux.desktop.model.NewChannel
import io.openflux.desktop.model.NodePlan
import io.openflux.desktop.model.NodeTransport
import io.openflux.desktop.model.NodeWizardException
import io.openflux.desktop.model.Profile
import io.openflux.desktop.model.ProfileSource
import io.openflux.desktop.model.ServerProbe
import io.openflux.desktop.model.ShareConfig
import io.openflux.desktop.model.ShareTransport
import io.openflux.desktop.model.SshTarget
import io.openflux.desktop.model.ThemeMode
import io.openflux.desktop.model.TrafficStats
import io.openflux.desktop.model.TransportType
import io.openflux.desktop.platform.JvmPlatformServices
import io.openflux.desktop.service.AppContainer
import io.openflux.desktop.service.ConnectionService
import io.openflux.desktop.service.NodeWizardService
import io.openflux.desktop.service.PlatformKind
import io.openflux.desktop.service.PlatformServices
import io.openflux.desktop.service.ProfileRepository
import io.openflux.desktop.service.SettingsRepository
import io.openflux.desktop.ui.BrowserPage
import io.openflux.desktop.ui.NoScrollbars
import io.openflux.desktop.ui.Shortcuts
import io.openflux.desktop.ui.home.HomeTab
import io.openflux.desktop.ui.logs.LogsTab
import io.openflux.desktop.ui.profiles.ProfilesTab
import io.openflux.desktop.ui.settings.SettingsTab
import io.openflux.desktop.ui.shell.OpenFluxApp
import cafe.adriel.voyager.core.model.ScreenModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * Records the README demos from the real screens: OpenFluxApp with demo
 * services (no real profiles, keys or servers), driven like a user would,
 * one PNG per frame plus the pointer's path for the GIFs
 * (scripts/demo-gifs.py). Desktop is the Windows window; Android is the same
 * UI in its touch mode at phone size.
 *
 *   OPENFLUX_DEMO=<dir> ./gradlew :desktopApp:test --tests '*DemoRecorder*'
 */
@OptIn(ExperimentalTestApi::class)
class DemoRecorder {
    @Test
    fun record() {
        val out = System.getenv("OPENFLUX_DEMO") ?: return
        for (platform in listOf(PlatformKind.Desktop, PlatformKind.Android)) {
            val base = File(out, platform.name.lowercase())
            scene(File(base, "connect"), platform) { connect() }
            scene(File(base, "profiles"), platform) { import() }
            scene(File(base, "node"), platform) { node() }
            scene(File(base, "logs"), platform) { logs() }
        }
    }

    /**
     * Only the "без сервера" wizard, driven through its model (no typing): one PNG per step.
     * OPENFLUX_DEMO_PHP=<dir> ./gradlew :desktopApp:test --tests '*DemoRecorder.recordPhp'
     */
    @Test
    fun recordPhp() {
        val out = System.getenv("OPENFLUX_DEMO_PHP") ?: return
        for (platform in listOf(PlatformKind.Desktop, PlatformKind.Android)) {
            val dir = File(File(out, platform.name.lowercase()), "php").apply { deleteRecursively(); mkdirs() }
            val demo = Demo(platform)
            val desktop = platform == PlatformKind.Desktop
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
            val model = io.openflux.desktop.ui.node.PhpWizardModel(demo.container, scope)
            runSkikoComposeUiTest(size = if (desktop) Size(1100f, 900f) else Size(412f, 900f), density = Density(1f)) {
                mainClock.autoAdvance = false
                setContent {
                    io.openflux.desktop.ui.theme.OpenFluxTheme(dark = desktop, touch = !desktop) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            io.openflux.desktop.ui.LocalTouchUi provides !desktop,
                            io.openflux.desktop.service.LocalAppContainer provides demo.container,
                            io.openflux.desktop.ui.components.LocalToaster provides io.openflux.desktop.ui.components.Toaster(),
                            io.openflux.desktop.ui.LocalScrollbars provides NoScrollbars,
                        ) {
                            androidx.compose.foundation.layout.Box(
                                androidx.compose.ui.Modifier.fillMaxSize().background(io.openflux.desktop.ui.theme.AppTheme.colors.background),
                            ) { io.openflux.desktop.ui.node.PhpWizardPane(model, onClose = {}, onSaved = {}) }
                        }
                    }
                }
                var n = 0
                fun shot(name: String) {
                    repeat(3) { mainClock.advanceTimeBy(80); Thread.sleep(15) }
                    ImageIO.write(captureToImage().toAwtImage(), "png", File(dir, "%02d-%s.png".format(n++, name)))
                }
                fun waitFor(ms: Long = 20_000, cond: () -> Boolean) {
                    val end = System.currentTimeMillis() + ms
                    while (!cond() && System.currentTimeMillis() < end) { mainClock.advanceTimeBy(80); Thread.sleep(40) }
                }
                shot("hosting-empty")
                model.usePreset(0)
                model.ftpUser = "if0_38012345"
                model.ftpPassword = "demo-password"
                model.siteInput = "openflux-demo.42web.io"
                shot("hosting-filled")
                model.probeHosting()
                waitFor { model.step == io.openflux.desktop.ui.node.PhpStep.Channel }
                shot("channel-cups")
                model.carrier = TransportType.MAILRU
                shot("channel-mailru")
                model.carrier = TransportType.CUPSONLINE
                model.prepareChannel()
                waitFor { model.step == io.openflux.desktop.ui.node.PhpStep.Install }
                shot("install-ready")
                model.install()
                waitFor(5_000) { model.progress != null }
                shot("install-uploading")
                waitFor { model.step == io.openflux.desktop.ui.node.PhpStep.Verify || model.step == io.openflux.desktop.ui.node.PhpStep.Done }
                shot("verify")
                waitFor { model.step == io.openflux.desktop.ui.node.PhpStep.Done }
                shot("done")
            }
        }
    }

    /**
     * The Transports tab with a script that has settings, and the settings dialog: one PNG each.
     * OPENFLUX_DEMO_SCRIPTS=<dir> ./gradlew :desktopApp:test --tests '*DemoRecorder.recordScripts'
     */
    @Test
    fun recordScripts() {
        val out = System.getenv("OPENFLUX_DEMO_SCRIPTS") ?: return
        for (platform in listOf(PlatformKind.Desktop, PlatformKind.Android)) {
            val dir = File(File(out, platform.name.lowercase()), "scripts").apply { deleteRecursively(); mkdirs() }
            val demo = Demo(platform)
            val desktop = platform == PlatformKind.Desktop
            val repo = io.openflux.desktop.service.InMemoryScriptRepository().apply {
                upsert(
                    io.openflux.desktop.model.InstalledScript(
                        id = "my-transport", name = "Мой транспорт", version = "1.3.0", pubkeyHex = "aa", fingerprint = "ab".repeat(32),
                        fileName = "my-transport.flux", source = io.openflux.desktop.model.ScriptSource.GitHub,
                        params = listOf(
                            io.openflux.desktop.model.ScriptParam("url", "Ссылка", "url", true, scope = "profile"),
                            io.openflux.desktop.model.ScriptParam("token", "API-токен", "secret", true, scope = "settings"),
                            io.openflux.desktop.model.ScriptParam("retries", "Число повторов", "number", scope = "settings", default = "3"),
                            io.openflux.desktop.model.ScriptParam("compress", "Сжимать", "boolean", scope = "settings"),
                        ),
                    ),
                )
                upsert(
                    io.openflux.desktop.model.InstalledScript(
                        id = "mailru", name = "mailru", version = "1.1.0", pubkeyHex = "bb", fingerprint = "cd".repeat(32),
                        fileName = "mailru.js", official = true, source = io.openflux.desktop.model.ScriptSource.Bundled,
                        params = listOf(io.openflux.desktop.model.ScriptParam("url", "Ссылка", "url", true)),
                    ),
                )
            }
            val platformWithSettings = object : PlatformServices by demo.platform {
                override fun scriptSettings(data: ByteArray, sig: ByteArray, pubkeyHex: String, valuesJson: String, lang: String) =
                    """{"ok":false,"code":"bad_signature","error":"x"}"""
            }
            val container = AppContainer(
                demo.profiles, demo.settings, demo.connection, platformWithSettings, demo.codec, DemoNode(demo.codec), DemoPhp(), scripts = repo,
            )
            runSkikoComposeUiTest(size = if (desktop) Size(1100f, 900f) else Size(412f, 900f), density = Density(1f)) {
                mainClock.autoAdvance = false
                setContent {
                    io.openflux.desktop.ui.theme.OpenFluxTheme(dark = desktop, touch = !desktop) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            io.openflux.desktop.ui.LocalTouchUi provides !desktop,
                            io.openflux.desktop.service.LocalAppContainer provides container,
                            io.openflux.desktop.ui.components.LocalToaster provides io.openflux.desktop.ui.components.Toaster(),
                            io.openflux.desktop.ui.LocalScrollbars provides NoScrollbars,
                        ) {
                            androidx.compose.foundation.layout.Box(
                                androidx.compose.ui.Modifier.fillMaxSize().background(io.openflux.desktop.ui.theme.AppTheme.colors.background),
                            ) {
                                io.openflux.desktop.ui.scripts.ScriptsScreen(container)
                                io.openflux.desktop.ui.shell.ScriptSettingsDialog()
                            }
                        }
                    }
                }
                fun shot(name: String) {
                    repeat(4) { mainClock.advanceTimeBy(80); Thread.sleep(15) }
                    ImageIO.write(captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
                }
                shot("1-list")
                onAllNodesWithText("Настройки").onFirst().performClick()
                Thread.sleep(300)
                shot("2-dialog-error")
            }
        }
    }

    /**
     * The whole app with the experimental features off (the default) and on: the «Транспорты» tab is not
     * there until they are turned on, and the Settings page that turns them on. One PNG each; the
     * assertions run whenever the test does.
     * OPENFLUX_DEMO_EXPERIMENTAL=<dir> ./gradlew :desktopApp:test --tests '*DemoRecorder.recordExperimental'
     */
    @Test
    fun recordExperimental() {
        val out = System.getenv("OPENFLUX_DEMO_EXPERIMENTAL") ?: return
        listOf(HomeTab, ProfilesTab, LogsTab, SettingsTab).forEach(ScreenModelStore::onDispose)
        val dir = File(out, "experimental").apply { deleteRecursively(); mkdirs() }
        val demo = Demo(PlatformKind.Desktop)
        runSkikoComposeUiTest(size = Size(1100f, 760f), density = Density(1f)) {
            mainClock.autoAdvance = false
            setContent { OpenFluxApp(demo.container, NoScrollbars, Shortcuts()) }
            fun shot(name: String) {
                repeat(6) { mainClock.advanceTimeBy(80); Thread.sleep(15) }
                ImageIO.write(captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
            }
            repeat(8) { mainClock.advanceTimeBy(80) }
            // The sidebar's entries are on the left; "Транспорты" is also a row of the Home screen's details.
            fun sidebarHas(title: String) = onAllNodesWithText(title).fetchSemanticsNodes().any { it.boundsInRoot.left < 300f }
            // Off by default: the sidebar has Главная, Профили, Логи, Настройки, and no Транспорты.
            check(!demo.settings.settings.value.experimental)
            check(sidebarHas("Профили") && !sidebarHas("Транспорты")) { "the Transports tab is there by default" }
            shot("1-off-home")
            onAllNodesWithText("Настройки").onFirst().performClick()
            repeat(8) { mainClock.advanceTimeBy(80) }
            onAllNodesWithText("Экспериментальные функции").onFirst().performClick()
            repeat(8) { mainClock.advanceTimeBy(80) }
            shot("2-off-settings")
            // The switch turns it on: the tab appears.
            onAllNodesWithText("Экспериментальные функции").onLast().performClick()
            repeat(8) { mainClock.advanceTimeBy(80) }
            check(demo.settings.settings.value.experimental) { "the switch did not turn the features on" }
            check(sidebarHas("Транспорты")) { "the Transports tab did not appear" }
            shot("3-on-settings")
            // And off again.
            onAllNodesWithText("Экспериментальные функции").onLast().performClick()
            repeat(8) { mainClock.advanceTimeBy(80) }
            check(!demo.settings.settings.value.experimental)
            check(!sidebarHas("Транспорты")) { "the Transports tab stayed after the features were turned off" }
        }
    }

    private fun scene(dir: File, kind: PlatformKind, script: Rec.() -> Unit) {
        // Voyager keeps the tabs' screen models for the whole process: a scene
        // must not inherit the previous one's (and its demo connection).
        listOf(HomeTab, ProfilesTab, LogsTab, SettingsTab).forEach(ScreenModelStore::onDispose)
        dir.deleteRecursively()
        dir.mkdirs()
        val desktop = kind == PlatformKind.Desktop
        val demo = Demo(kind)
        runSkikoComposeUiTest(
            size = if (desktop) Size(1100f, 700f) else Size(412f, 870f),
            density = Density(1f),
        ) {
            mainClock.autoAdvance = false
            setContent { OpenFluxApp(demo.container, NoScrollbars, Shortcuts()) }
            val rec = Rec(this, dir, demo, desktop)
            rec.hold(6)
            rec.script()
            rec.finish()
        }
    }

    private class Rec(val test: SkikoComposeUiTest, val dir: File, val demo: Demo, val desktop: Boolean) {
        private var frame = 0
        private val size = test.onAllNodes(isRoot()).fetchSemanticsNodes().first().boundsInRoot
        private var x = (size.right * 0.62f)
        private var y = (size.bottom * 0.78f)
        private var pressed = false
        private val path = StringBuilder("[")

        /** One frame after [ms] of app time. */
        fun shot(ms: Long = FRAME_MS) {
            test.mainClock.advanceTimeBy(ms)
            Thread.sleep(12) // let the app's own coroutines (Swing) catch up
            val image = test.captureToImage().toAwtImage() // the whole scene, dialogs included
            ImageIO.write(image, "png", File(dir, "f%04d.png".format(frame++)))
            if (path.length > 1) path.append(',')
            path.append("[${x.roundToInt()},${y.roundToInt()},${if (pressed) 1 else 0}]")
        }

        fun hold(frames: Int) = repeat(frames) { shot() }

        fun moveTo(tx: Float, ty: Float, frames: Int = if (desktop) 9 else 5) {
            val sx = x
            val sy = y
            for (i in 1..frames) {
                val t = i / frames.toFloat()
                val e = t * t * (3 - 2 * t)
                x = sx + (tx - sx) * e
                y = sy + (ty - sy) * e
                shot()
            }
        }

        /** What [text] labels (a text or an icon's description): exact matches first, the biggest wins. */
        fun node(text: String, merged: Boolean = false, clickable: Boolean = false): SemanticsNodeInteraction {
            for (exact in listOf(true, false)) {
                for (nodes in listOf(
                    test.onAllNodesWithText(text, substring = !exact, useUnmergedTree = !merged),
                    test.onAllNodesWithContentDescription(text, substring = !exact, useUnmergedTree = !merged),
                )) {
                    val all = nodes.fetchSemanticsNodes()
                    val usable = all.indices.filter { !clickable || SemanticsActions.OnClick in all[it].config }
                    if (usable.isEmpty()) continue
                    return nodes[usable.maxBy { all[it].boundsInRoot.width * all[it].boundsInRoot.height }]
                }
            }
            error("no \"$text\" on screen")
        }

        /** Clicks what [text] labels through its click action, which also reaches dialogs. */
        fun click(text: String) {
            node(text, merged = true, clickable = true).performSemanticsAction(SemanticsActions.OnClick)
        }

        /** The text field [label] (its label or placeholder) belongs to. */
        fun field(label: String): SemanticsNodeInteraction {
            val labels = test.onAllNodesWithText(label, substring = false, useUnmergedTree = true).fetchSemanticsNodes()
                .ifEmpty { test.onAllNodesWithText(label, substring = true, useUnmergedTree = true).fetchSemanticsNodes() }
            val fields = test.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
            val boxes = fields.fetchSemanticsNodes().map { it.boundsInRoot }
            check(labels.isNotEmpty() && boxes.isNotEmpty()) { "no field \"$label\"" }
            val best = boxes.indices.minBy { i ->
                labels.minOf { l -> (boxes[i].center - l.boundsInRoot.center).getDistance() }
            }
            return fields[best]
        }

        fun tap(text: String, after: Int = 6) {
            val target = node(text, merged = true, clickable = true)
            val b = target.getBoundsInRoot()
            moveTo((b.left + b.right).value / 2, (b.top + b.bottom).value / 2)
            pressed = true
            shot()
            click(text)
            shot()
            pressed = false
            hold(after)
        }

        fun type(label: String, text: String) {
            val target = field(label)
            val b = target.getBoundsInRoot()
            moveTo((b.left + b.right).value / 2, (b.top + b.bottom).value / 2)
            pressed = true
            shot()
            pressed = false
            // A few characters per frame, like typing.
            val chunks = text.chunked(maxOf(3, text.length / 14))
            chunks.forEachIndexed { i, _ ->
                target.performTextReplacement(chunks.take(i + 1).joinToString(""))
                shot()
            }
        }

        fun until(frames: Int = 60, done: () -> Boolean) {
            var n = 0
            while (!done() && n++ < frames) shot()
        }

        fun finish() {
            hold(4)
            path.append(']')
            File(dir, "path.json").writeText("""{"desktop":$desktop,"width":${size.right.roundToInt()},"height":${size.bottom.roundToInt()},"frames":$path}""")
        }

        // ---- scenes ----

        fun connect() {
            hold(4)
            tap("Отключено", after = 2)
            until { demo.connection.state.value is ConnectionState.Connected }
            repeat(if (desktop) 30 else 26) { demo.connection.tick(); shot(120) }
        }

        fun import() {
            tap("Профили", after = 4)
            tap("Импорт ссылки или QR (Ctrl+I)", after = 3)
            type("openflux://v1/…", demo.link)
            hold(6)
            tap("Добавить профиль", after = 10)
            tap("Главная", after = 3)
            tap("Отключено", after = 2)
            until { demo.connection.state.value is ConnectionState.Connected }
            repeat(14) { demo.connection.tick(); shot(120) }
        }

        fun node() {
            tap("Профили", after = 3)
            tap("Добавить профиль", after = 3)
            tap("Создать свою ноду на VDS", after = 4)
            type("Адрес (IP или домен)", "203.0.113.10")
            type("Пароль", "••••••••")
            tap("Подключиться", after = 4)
            tap("Доверять", after = 2)
            until { runCatching { node("Проверить ссылку") }.isSuccess }
            hold(6)
            type("Ссылка с доступом", "https://docs.yandex.ru/edit/d/demoNewNodeDocument01234567890")
            tap("Проверить ссылку", after = 2)
            until { runCatching { node("Установить ноду") }.isSuccess }
            hold(10)
            tap("Установить ноду", after = 2)
            until(90) { runCatching { node("Нода готова") }.isSuccess }
            hold(18)
        }

        fun phpNode() {
            tap("Профили", after = 3)
            tap("Добавить профиль", after = 3)
            tap("Без сервера: нода на PHP-хостинге", after = 4)
            tap("InfinityFree и другие на iFastNet", after = 3)
            type("Логин FTP", "if0_38012345")
            type("Пароль FTP", "••••••••••")
            type("Адрес вашего сайта на хостинге", "https://openflux-demo.42web.io")
            hold(4)
            tap("Проверить вход", after = 2)
            until { runCatching { node("Далее") }.isSuccess }
            hold(8)
            tap("Далее", after = 2)
            until { runCatching { node("Установить и запустить") }.isSuccess }
            hold(8)
            tap("Установить и запустить", after = 2)
            until(120) { runCatching { node("Нода готова") }.isSuccess }
            hold(14)
        }

        fun logs() {
            tap("Отключено", after = 2)
            until { demo.connection.state.value is ConnectionState.Connected }
            hold(4)
            tap("Логи", after = 6)
            repeat(20) { demo.connection.tick(); shot(150) }
        }

        companion object {
            const val FRAME_MS = 80L
        }
    }

    /** Demo services: made-up profiles, a node on a documentation address (203.0.113.0/24). */
    private class Demo(kind: PlatformKind) {
        val profiles = DemoProfiles()
        val settings = DemoSettings(kind)
        val connection = DemoConnection()
        val platform = DemoPlatform(kind)
        // Links are the core's: the demo runs the bundled one.
        val codec = CoreShareLinkCodec(CliCoreLinks(settings, CoreBinary()))
        val container = AppContainer(profiles, settings, connection, platform, codec, DemoNode(codec), DemoPhp())
        val link = kotlinx.coroutines.runBlocking { codec.encode(
            ShareConfig(
                name = "Нода Франкфурт", negotiate = true, secret = "5f".repeat(32),
                context = "https://docs.yandex.ru/edit/d/demoFrankfurtDocument0123456789",
                transports = listOf(
                    ShareTransport(type = "vyandex", url = "https://docs.yandex.ru/edit/d/demoFrankfurtDocument0123456789", priority = 100),
                    ShareTransport(type = "direct", dial = "198.51.100.24:31337", priority = 50),
                ),
            ),
        ) }
    }

    /** The hosting steps with made-up answers and a little time, so the screens show their progress. */
    private class DemoPhp : io.openflux.desktop.service.PhpHostingService() {
        override suspend fun probe(ftp: io.openflux.desktop.model.FtpTarget) =
            io.openflux.desktop.model.PhpProbe(security = "none", dir = "htdocs", writable = true)

        override suspend fun deploy(
            ftp: io.openflux.desktop.model.FtpTarget,
            token: String,
            onProgress: (io.openflux.desktop.model.PhpProgress) -> Unit,
        ): io.openflux.desktop.model.PhpInstalled {
            val total = 2_200_000L
            for (i in 1..12) {
                kotlinx.coroutines.delay(180)
                onProgress(io.openflux.desktop.model.PhpProgress("upload", "lib/file$i.php", i, 12, total * i / 12, total))
            }
            return io.openflux.desktop.model.PhpInstalled(dir = "htdocs", token = "demo0token", files = 12, bytes = total, security = "none")
        }

        override suspend fun check(site: String, token: String, carrier: String) =
            io.openflux.desktop.model.PhpStatus(version = "0.4", carrier = carrier, php = "8.4.1")

        override suspend fun start(site: String, token: String, carrier: String, target: String, chain: Boolean, quiet: Boolean) =
            io.openflux.desktop.model.PhpNodeState(running = true, chain = chain)

        override suspend fun newRoom() = io.openflux.desktop.model.PhpRoom(
            "0a1b2c3d-1111-2222-3333-444455556666",
            "https://interview.cups.online/live-coding/?room=0a1b2c3d-1111-2222-3333-444455556666",
        )

        override suspend fun link(name: String, carrier: String, target: String) = "openflux://v1/demo-php-node"
    }

    private class DemoSettings(kind: PlatformKind) : SettingsRepository {
        override val settings = MutableStateFlow(
            AppSettings(
                selectedProfileId = "amsterdam",
                theme = if (kind == PlatformKind.Desktop) ThemeMode.Dark else ThemeMode.Light,
            ),
        )
        override fun update(transform: (AppSettings) -> AppSettings) { settings.value = transform(settings.value) }
    }

    private class DemoProfiles : ProfileRepository {
        private val now = System.currentTimeMillis()
        override val profiles = MutableStateFlow(
            listOf(
                Profile(
                    id = "amsterdam", name = "Нода Амстердам", icon = "ic_public", transport = TransportType.VYANDEX,
                    value = "https://docs.yandex.ru/edit/d/demoAmsterdamDocument0123456789", secret = "a1".repeat(32),
                    session = true, priority = 100, source = ProfileSource.Node, createdAt = now - 86_400_000L * 3,
                    extras = listOf(ExtraTransport(TransportType.DIRECT, "203.0.113.10:31337", priority = 50)),
                ),
                Profile(
                    id = "home", name = "Дом", icon = "ic_lock", transport = TransportType.YANDEX,
                    value = "https://docs.yandex.ru/edit/d/demoHomeDocument012345678901", secret = "b2".repeat(32),
                    source = ProfileSource.Qr, createdAt = now - 86_400_000L * 9,
                ),
            ),
        )
        private var next = 0
        override fun upsert(profile: Profile) { profiles.value = profiles.value.filterNot { it.id == profile.id } + profile }
        override fun delete(id: String) { profiles.value = profiles.value.filterNot { it.id == id } }
        override fun newId() = "demo${next++}"
    }

    private class DemoConnection : ConnectionService {
        override val state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
        override val traffic = MutableStateFlow(TrafficStats())
        override val exitAddress = MutableStateFlow<ExitAddress>(ExitAddress.Unknown)
        override val logs = MutableStateFlow<List<LogLine>>(emptyList())
        override val captcha: StateFlow<CaptchaPrompt?> = MutableStateFlow(null)
        override val exitShareLink: StateFlow<String?> = MutableStateFlow(null)
        override val socksAddress = MutableStateFlow<String?>(null)
        override val captchaPage: StateFlow<BrowserPage?> = MutableStateFlow(null)
        private var ticks = 0
        private var lines = 0L

        override fun connect(profile: Profile) {
            val since = System.currentTimeMillis()
            state.value = ConnectionState.Connecting(profile, ConnectionMode.Client, since)
            exitAddress.value = ExitAddress.Checking
            log(LogLevel.Info, "Запускаю ядро OpenFlux: ${profile.name}")
            thread(isDaemon = true) {
                Thread.sleep(900)
                log(LogLevel.Info, "Session: транспорт vyandex (Яндекс Документы), приоритет 100")
                log(LogLevel.Info, "Session: шифрование AES-256-GCM, согласование с нодой")
                Thread.sleep(500)
                state.value = ConnectionState.Connected(profile, ConnectionMode.Client, since)
                socksAddress.value = "127.0.0.1:1080"
                exitAddress.value = ExitAddress.Known(if (profile.name.contains("Франкфурт")) "198.51.100.24" else "203.0.113.10")
                traffic.value = TrafficStats(activeTransport = "vyandex", live = true)
                log(LogLevel.Success, "Подключено: канал через Яндекс поднят, SOCKS5 127.0.0.1:1080")
            }
        }

        /** Traffic moves on, as it would under a browser. */
        fun tick() {
            ticks++
            val down = 380_000L + (ticks * 97_331L) % 1_900_000L
            val up = 42_000L + (ticks * 13_117L) % 160_000L
            val t = traffic.value
            traffic.value = t.copy(downBytesPerSec = down, upBytesPerSec = up, totalDown = t.totalDown + down, totalUp = t.totalUp + up)
            if (ticks % 3 == 0) {
                val sites = listOf("youtube.com", "github.com", "t.me", "wikipedia.org", "chatgpt.com")
                log(LogLevel.Debug, "[SOCKS5] CONNECT ${sites[(ticks / 3) % sites.size]}:443 через vyandex")
            }
        }

        private fun log(level: LogLevel, text: String) {
            logs.value = logs.value + LogLine(++lines, System.currentTimeMillis(), text, level)
        }

        override fun disconnect() {
            state.value = ConnectionState.Idle
            exitAddress.value = ExitAddress.Unknown
            traffic.value = TrafficStats()
        }

        override fun refreshExitAddress() = Unit
        override fun clearLogs() { logs.value = emptyList() }
        override fun openCaptcha() = Unit
        override fun submitCaptcha() = Unit
        override fun dismissCaptcha() = Unit
        override fun shutdown() = Unit
    }

    private class DemoNode(private val codec: ShareLinkCodec) : NodeWizardService {
        override val logs = MutableStateFlow<List<LogLine>>(emptyList())
        override fun clearLogs() { logs.value = emptyList() }
        override fun note(text: String, level: LogLevel) = Unit

        override suspend fun connect(target: SshTarget): ServerProbe {
            if (target.hostKey.isEmpty()) {
                throw NodeWizardException("новый сервер", hostKey = "SHA256:q3Vx8Ld2pWm7aKc9Rt1YhZ0uNf5bGe4sJiOo6TzXvBw", trust = true)
            }
            Thread.sleep(700)
            return ServerProbe(arch = "amd64", os = "Debian 12", systemd = true, sudo = "root")
        }

        override suspend fun newChannel() = NewChannel("of-k3f9q2", "c3".repeat(32))

        override suspend fun plan(channel: String, transports: List<NodeTransport>, autoUpdate: Boolean): NodePlan {
            Thread.sleep(600)
            return NodePlan(
                channel = channel, port = 31337,
                actions = listOf(
                    "Скачать ядро OpenFlux node-v1.1.0 и сверить SHA-256",
                    "Создать канал $channel: документ, ключ, порт 31337",
                    "Запустить systemd-сервис openflux-node@$channel",
                    "Открыть порт 31337/tcp для резервного канала",
                ),
            )
        }

        override suspend fun apply(channel: NewChannel, transports: List<NodeTransport>, port: Int, autoUpdate: Boolean, sudoPassword: String) {
            Thread.sleep(1500)
        }

        override suspend fun remove(channel: String, sudoPassword: String) = Unit
        override suspend fun checkDocument(documentUrl: String) { Thread.sleep(400) }

        override suspend fun createCupsRooms() = "WyJyb29tLTEiXQ"

        override suspend fun shareLink(name: String, key: String, host: String, port: Int, transports: List<NodeTransport>) =
            codec.encode(
                ShareConfig(
                    name = name, negotiate = true, secret = key,
                    context = transports.firstOrNull { it.type != "cupsonline" }?.url ?: "http://#",
                    transports = transports.mapIndexed { i, t -> ShareTransport(type = t.type, url = t.url, priority = 100 - 10 * i) } +
                        ShareTransport(type = "direct", dial = "$host:$port", priority = 50),
                ),
            )

        override suspend fun resolve(host: String) = setOf(host)
        override fun close() = Unit
    }

    private class DemoPlatform(override val kind: PlatformKind) : PlatformServices {
        private val real = JvmPlatformServices("2.5.0", CoreBinary()) { "main@6d84e01" }
        override val appVersion = "2.5.0"
        override val coreVersion = "main@6d84e01"
        override val clientRepo = "p1neappleXpress/OpenFluxDesktop"
        override val systemProxySupported = kind == PlatformKind.Desktop
        override val fullTunnelSupported = true
        override val elevated = true
        override val cameraScanSupported = kind == PlatformKind.Android
        override fun restartElevated() = false
        override fun clipboardText(): String? = null
        override fun setClipboardText(text: String) = Unit
        override fun qrFromClipboardImage(): String? = null
        override fun qrFromFile(path: String): String? = null
        override suspend fun pickFile(title: String, extensions: List<String>): String? = null
        override fun readTextFile(path: String, maxBytes: Int): String? = null
        override fun qrMatrix(text: String) = real.qrMatrix(text)
        override fun openUrl(url: String) = Unit
        override fun newSecret() = "d4".repeat(32)
        override fun now() = System.currentTimeMillis()
        override suspend fun latestRelease(): String? = "2.5.0"
    }
}
