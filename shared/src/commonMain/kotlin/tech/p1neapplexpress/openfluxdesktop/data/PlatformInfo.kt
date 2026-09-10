package tech.p1neapplexpress.openfluxdesktop.data

import java.io.File
import java.util.Locale

object PlatformInfo {
    val os: String = detectOS()
    val arch: String = detectArch()

    val binaryName: String
        get() = "universal-bypass-tool-$os-$arch"

    val binaryExtension: String
        get() = if (os == "windows") ".exe" else ""

    val binDir: File
        get() {
            val workDir = System.getProperty("user.dir")
            return File(workDir, "bin").apply { mkdirs() }
        }

    private fun detectOS(): String {
        val name = System.getProperty("os.name").lowercase(Locale.ROOT)
        return when {
            name.contains("win") -> "windows"
            name.contains("mac") || name.contains("darwin") -> "darwin"
            name.contains("linux") -> "linux"
            else -> "linux"
        }
    }

    private fun detectArch(): String {
        val arch = System.getProperty("os.arch").lowercase(Locale.ROOT)
        return when (arch) {
            "x86_64", "amd64" -> "amd64"
            "aarch64", "arm64" -> "arm64"
            "x86", "i386", "i686" -> "386"
            "arm" -> "arm"
            else -> "amd64"
        }
    }
}