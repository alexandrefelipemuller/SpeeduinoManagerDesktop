plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.speeduino.manager.desktop.MainKt"
        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            val currentOs = org.gradle.internal.os.OperatingSystem.current()
            when {
                currentOs.isMacOsX -> targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
                )
                currentOs.isWindows -> targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
                )
                else -> targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage
                )
            }
            packageName = "SpeeduinoManagerDesktop"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("../assets/icon_256.png"))
            }
            windows {
                iconFile.set(project.file("../assets/icon.ico"))
                menuGroup = "SpeeduinoManagerDesktop"
                shortcut = true
            }
            macOS {
                iconFile.set(project.file("../assets/icon.icns"))
            }
        }
    }
}
