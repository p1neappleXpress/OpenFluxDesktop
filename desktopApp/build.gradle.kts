import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

/** The latest release tag reachable from this checkout (v3.4.1 -> 3.4.1), for a local run; null without git or tags. */
fun latestReleaseTag(): String? = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--match", "v[0-9]*", "--abbrev=0")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().removePrefix("v").takeIf { Regex("""\d+\.\d+\.\d+""").matches(it) }
}.getOrNull()

/** The app version: -PappVersion=1.2.3 (the release workflow passes the tag); a local run takes the latest tag. */
val appVersion = (findProperty("appVersion") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: latestReleaseTag() ?: "2.0.0"

/** The id that names this build to the OpenFlux service (an identifier, not a secret): -Popenflux.appId=ofa_… or OPENFLUX_APP_ID. */
val appId = ((findProperty("openflux.appId") as String?) ?: System.getenv("OPENFLUX_APP_ID")).orEmpty().trim()

val writeAppId by tasks.registering {
    val out = layout.buildDirectory.file("generated/appid/openflux-appid.txt")
    inputs.property("appId", appId)
    outputs.file(out)
    doLast { out.get().asFile.apply { parentFile.mkdirs() }.writeText(appId) }
}
sourceSets.main { resources.srcDir(layout.buildDirectory.dir("generated/appid")) }
tasks.named("processResources") { dependsOn(writeAppId) }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    // Skia's Windows natives (skiko) come with the host's runtime above only
    // when building on Windows; a Windows package needs them whatever builds it.
    if (findProperty("windowsPackage") == "true") implementation(compose.desktop.windows_x64)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.components.resources)
    testImplementation(libs.kotlin.testJunit)
}

/**
 * The core the app bundles, in resources/<windows|macos|linux> (Compose's
 * appResources layout, where CoreBinary looks for it): openflux-<os>-<arch>
 * [.exe], openflux-core.version and, on Windows, wintun.dll. The binaries are
 * not in git. The release workflow builds them before packaging; for a local
 * run or package, prepareCore builds the missing ones for this machine with
 * the local Go, from -PcoreDir=<checkout>, the OpenFlux/ submodule, ../OpenFlux
 * next to this repository, or else a clone of p1neappleXpress/OpenFlux in
 * build/openflux-core. -PskipCore=true runs the app without a core
 * (Settings → Core then takes a file of your own).
 */
abstract class PrepareCore @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Internal abstract val outDir: DirectoryProperty
    @get:Internal abstract val cloneDir: DirectoryProperty
    @get:Internal abstract val sourceDirs: ListProperty<File>
    @get:Internal abstract val goos: Property<String>
    @get:Internal abstract val goarch: Property<String>
    @get:Internal abstract val repository: Property<String>
    @get:Internal abstract val ref: Property<String>
    @get:Internal abstract val allowUnpinned: Property<Boolean>

    @get:Internal val coreName: String
        get() = "openflux-${goos.get()}-${goarch.get()}" + if (goos.get() == "windows") ".exe" else ""

    @get:Internal val missing: Boolean
        get() {
            val out = outDir.get().asFile
            return !File(out, coreName).isFile || (goos.get() == "windows" && !File(out, "wintun.dll").isFile) || stale()
        }

    /**
     * A core built earlier from another commit of the source than the one checked out now (the submodule moved):
     * a local run would keep an old core that cannot read newer links. CI builds its own, it is never stale here.
     */
    private fun stale(): Boolean {
        if (System.getenv("CI") != null) return false
        val source = sourceDirs.get().firstOrNull { File(it, "go.mod").isFile } ?: return false
        val branch = runCatching { run(source, "git", "rev-parse", "--abbrev-ref", "HEAD") }.getOrNull() ?: return false
        val rev = runCatching { run(source, "git", "describe", "--always", "--dirty") }.getOrNull() ?: return false
        val file = File(outDir.get().asFile, "openflux-core.version")
        return !file.isFile || file.readText().trim() != "$branch@$rev"
    }

    @TaskAction
    fun prepare() {
        val out = outDir.get().asFile.apply { mkdirs() }
        if (!File(out, coreName).isFile || stale()) buildCore(out)
        if (goos.get() == "windows" && !File(out, "wintun.dll").isFile) wintun(out)
    }

    private fun buildCore(out: File) {
        val go = runCatching { run(null, "go", "version") }.getOrNull()
            ?: throw GradleException(
                "Нет ядра OpenFlux в ${out.path}, а Go для его сборки не найден. Установите Go (https://go.dev/dl), " +
                    "соберите ядро scripts/build-core.sh или запустите без него: -PskipCore=true",
            )
        val source = sourceDirs.get().firstOrNull { File(it, "go.mod").isFile } ?: clone()
        checkPinned(source)
        logger.lifecycle("Собираю ядро OpenFlux ($coreName) из ${source.path}, $go")
        val target = File(out, coreName)
        exec.exec {
            workingDir = source
            environment("GOOS", goos.get())
            environment("GOARCH", goarch.get())
            environment("CGO_ENABLED", "0")
            environment("GOFLAGS", "-buildvcs=false")
            environment("GOTOOLCHAIN", "auto")
            commandLine("go", "build", "-trimpath", "-ldflags", "-s -w", "-o", target.absolutePath, ".")
        }
        target.setExecutable(true)
        val branch = runCatching { run(source, "git", "rev-parse", "--abbrev-ref", "HEAD") }.getOrNull()
        val rev = runCatching { run(source, "git", "describe", "--always", "--dirty") }.getOrNull()
        File(out, "openflux-core.version").writeText(if (branch != null && rev != null) "$branch@$rev\n" else "local\n")
    }

    /**
     * The OpenFlux submodule must be on the commit this repository pins: a `checkout main` inside it once rolled the
     * local core back to node-v1.0.0, and the app then could not talk to new nodes. -PcoreUnpinned=true builds anyway.
     */
    private fun checkPinned(source: File) {
        if (allowUnpinned.get()) return
        val parent = source.parentFile ?: return
        val entry = runCatching { run(parent, "git", "ls-tree", "HEAD", source.name) }.getOrNull()?.split(Regex("\\s+")) ?: return
        if (entry.getOrNull(1) != "commit") return // not a submodule of this repository (a sibling clone)
        val pinned = entry.getOrNull(2) ?: return
        val head = runCatching { run(source, "git", "rev-parse", "HEAD") }.getOrNull() ?: return
        if (head != pinned) {
            throw GradleException(
                "Сабмодуль OpenFlux стоит на ${head.take(7)}, а репозиторий закрепил ${pinned.take(7)}: ядро собралось бы " +
                    "не то. Верните его: git submodule update --init OpenFlux (или соберите нарочно: -PcoreUnpinned=true)",
            )
        }
    }

    private fun clone(): File {
        val dir = cloneDir.get().asFile
        if (File(dir, "go.mod").isFile) return dir
        dir.deleteRecursively()
        val url = "https://github.com/${repository.get()}"
        logger.lifecycle("Скачиваю исходники ядра: $url (${ref.get()})")
        exec.exec { commandLine("git", "clone", "--depth", "1", "-b", ref.get(), url, dir.absolutePath) }
        return dir
    }

    /** Wintun for the full tunnel: the official build, checked against its SHA-256 like in the release workflow. */
    private fun wintun(out: File) {
        val zip = URI("https://www.wintun.net/builds/wintun-$WINTUN_VERSION.zip").toURL().readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(zip).joinToString("") { "%02x".format(it) }
        if (sha != WINTUN_SHA256) throw GradleException("wintun-$WINTUN_VERSION.zip: SHA-256 $sha, ожидался $WINTUN_SHA256")
        val entry = "wintun/bin/${goarch.get()}/wintun.dll"
        ZipInputStream(zip.inputStream()).use { input ->
            while (true) {
                val e = input.nextEntry ?: throw GradleException("В wintun-$WINTUN_VERSION.zip нет $entry")
                if (e.name == entry) {
                    File(out, "wintun.dll").writeBytes(input.readBytes())
                    return
                }
            }
        }
    }

    private fun run(dir: File?, vararg command: String): String {
        val stdout = ByteArrayOutputStream()
        exec.exec {
            if (dir != null) workingDir = dir
            commandLine(*command)
            standardOutput = stdout
            errorOutput = ByteArrayOutputStream()
        }
        return stdout.toString().trim()
    }

    private companion object {
        const val WINTUN_VERSION = "0.14.1"
        const val WINTUN_SHA256 = "07c256185d6ee3652e09fa55c0b673e2624b565e02c4b9091c79ca7d2f24ef51"
    }
}

val hostOs = System.getProperty("os.name").lowercase()
val prepareCore by tasks.registering(PrepareCore::class) {
    description = "Builds the bundled OpenFlux core for this machine when resources/<os> has none"
    val goos = when {
        hostOs.contains("win") -> "windows"
        hostOs.contains("mac") -> "darwin"
        else -> "linux"
    }
    this.goos.set(goos)
    goarch.set(if (System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")) "arm64" else "amd64")
    outDir.set(layout.projectDirectory.dir("resources/" + if (goos == "darwin") "macos" else goos))
    cloneDir.set(layout.buildDirectory.dir("openflux-core"))
    sourceDirs.set(
        listOfNotNull(
            (findProperty("coreDir") as String?)?.let(::file),
            rootDir.resolve("OpenFlux"),
            rootDir.resolve("../OpenFlux"),
        ),
    )
    repository.set(providers.gradleProperty("coreRepo").orElse("p1neappleXpress/OpenFlux"))
    ref.set(providers.gradleProperty("coreRef").orElse("main"))
    allowUnpinned.set(findProperty("coreUnpinned") == "true")
    val skip = findProperty("skipCore") == "true"
    onlyIf { !skip && missing }
}
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(prepareCore) }

compose.desktop {
    application {
        mainClass = "io.openflux.desktop.MainKt"
        jvmArgs("-Dopenflux.version=$appVersion")
        // JCEF (the built-in browser) reaches into AWT internals.
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "OpenFlux"
            packageVersion = appVersion
            description = "OpenFlux desktop client"
            vendor = "OpenFlux"
            // The app icon: icons/openflux.svg, drawn as the Android launcher icon.
            // The core (and wintun.dll on Windows) in resources/<windows|macos|linux>,
            // put there by scripts/build-core.sh or the release workflow.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.instrument", "java.management", "java.net.http", "jdk.crypto.ec", "jdk.httpserver", "jdk.unsupported")

            windows {
                menuGroup = "OpenFlux"
                upgradeUuid = "3F0C7B52-9A2E-4C1B-8E77-5D2A6B1E9C40"
                shortcut = true
                perUserInstall = true
                iconFile.set(project.file("icons/openflux.ico"))
            }
            macOS {
                bundleID = "io.openflux.desktop"
                dockName = "OpenFlux"
                appCategory = "public.app-category.utilities"
                iconFile.set(project.file("icons/openflux.icns"))
            }
            linux {
                packageName = "openflux"
                menuGroup = "Network"
                appCategory = "Network"
                shortcut = true
                iconFile.set(project.file("icons/openflux.png"))
            }
        }
    }
}
