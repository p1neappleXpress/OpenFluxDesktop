import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serial)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "tech.p1neapplexpress.openfluxdesktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
            )

            packageName = "OpenFlux"
            packageVersion = "1.0.0"

            modules(
                "java.net.http",
                "java.sql",
                "jdk.unsupported",
                "jdk.crypto.ec",
            )

            macOS {
                bundleID = "tech.p1neapplexpress.openflux"
            }

            windows {
                menuGroup = "OpenFlux"
                upgradeUuid = "8F4A3B2C-1D5E-4F7A-9B8C-2D6E3F1A5B9C"
            }
        }
    }
}