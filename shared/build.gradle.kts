    plugins {
        alias(libs.plugins.kotlinMultiplatform)
        alias(libs.plugins.composeMultiplatform)
        alias(libs.plugins.composeCompiler)
        alias(libs.plugins.serial)
    }

    kotlin {
        jvm()


        sourceSets {
            commonMain.dependencies {
                implementation("io.github.alexzhirkevich:qrose:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.navigation)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
            commonTest.dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }