plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation("io.ecucore:core-runtime")
                implementation("io.ecucore:core-tuning")
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.json:json:20240303")
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
            packageVersion = rootProject.version.toString()

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
