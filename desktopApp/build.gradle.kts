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

// --- MSIX packaging (Windows) ---
// Compose Desktop's nativeDistributions only produces Exe/Msi via jpackage, which cannot
// emit MSIX directly. Instead we take the plain app-image jpackage already builds and wrap
// it into an MSIX with the Windows SDK's makeappx.exe.
val msixVersion = "${rootProject.version}.0" // MSIX requires a 4-part numeric version
val msixPackageName: String = (project.findProperty("msixPackageName") as String?) ?: "PowerfoolTech.EcuManager"
val msixPublisherCN: String = (project.findProperty("msixPublisherCN") as String?)
    ?: "5F9CC0D0-18CD-4B58-BC0F-D353EDA0FC90"
val msixPublisherDisplayName: String =
    (project.findProperty("msixPublisherDisplayName") as String?) ?: "PowerfoolTech"
val msixExecutable = "SpeeduinoManagerDesktop.exe"

val msixStagingDir = layout.buildDirectory.dir("msix/staging")
val msixOutputDir = layout.buildDirectory.dir("msix/output")

val prepareMsixLayout by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Assembles the app-image plus MSIX manifest/assets into a staging folder."
    dependsOn("createReleaseDistributable")

    from(layout.buildDirectory.dir("compose/binaries/main-release/app/SpeeduinoManagerDesktop"))
    from(project.file("../assets/msix")) {
        include("*.png")
    }
    from(project.file("../assets/msix/AppxManifest.xml.template")) {
        rename { "AppxManifest.xml" }
        expand(
            "VERSION" to msixVersion,
            "PACKAGE_NAME" to msixPackageName,
            "PUBLISHER_CN" to msixPublisherCN,
            "PUBLISHER_DISPLAY_NAME" to msixPublisherDisplayName,
            "EXECUTABLE" to msixExecutable
        )
    }
    into(msixStagingDir)
}

fun findMakeAppx(): String {
    val fromPath = "makeappx.exe"
    val kitsRoot = file("C:/Program Files (x86)/Windows Kits/10/bin")
    if (kitsRoot.exists()) {
        val candidate = kitsRoot.listFiles { f -> f.isDirectory }
            ?.sortedDescending()
            ?.map { it.resolve("x64/makeappx.exe") }
            ?.firstOrNull { it.exists() }
        if (candidate != null) return candidate.absolutePath
    }
    return fromPath
}

val packageMsix by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Packages the release app-image into an unsigned .msix using the Windows SDK."
    dependsOn(prepareMsixLayout)

    val outDir = msixOutputDir.get().asFile
    val outFile = outDir.resolve("SpeeduinoManagerDesktop-${rootProject.version}.msix")

    doFirst {
        outDir.mkdirs()
    }

    commandLine(
        findMakeAppx(), "pack",
        "/d", msixStagingDir.get().asFile.absolutePath,
        "/p", outFile.absolutePath,
        "/overwrite"
    )

    doLast {
        logger.lifecycle("MSIX package created at: ${outFile.absolutePath}")
        logger.lifecycle("It is unsigned — sign it before installing/distributing, e.g.:")
        logger.lifecycle("  signtool sign /fd SHA256 /a /f <cert.pfx> /p <password> \"${outFile.absolutePath}\"")
    }
}

fun findSignTool(): String {
    val fromPath = "signtool.exe"
    val kitsRoot = file("C:/Program Files (x86)/Windows Kits/10/bin")
    if (kitsRoot.exists()) {
        val candidate = kitsRoot.listFiles { f -> f.isDirectory }
            ?.sortedDescending()
            ?.map { it.resolve("x64/signtool.exe") }
            ?.firstOrNull { it.exists() }
        if (candidate != null) return candidate.absolutePath
    }
    return fromPath
}

// Pass -PmsixCertPath=... -PmsixCertPassword=... to sign in place after packageMsix.
val signMsix by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Signs the packaged .msix with a code-signing certificate (PFX)."
    dependsOn(packageMsix)

    val certPath = project.findProperty("msixCertPath") as String?
    val certPassword = (project.findProperty("msixCertPassword") as String?) ?: ""
    val outFile = msixOutputDir.get().asFile.resolve("SpeeduinoManagerDesktop-${rootProject.version}.msix")

    doFirst {
        require(!certPath.isNullOrBlank()) {
            "Pass -PmsixCertPath=<path-to-pfx> (and optionally -PmsixCertPassword=<password>) to sign the MSIX."
        }
    }

    commandLine(
        findSignTool(), "sign",
        "/fd", "SHA256",
        "/f", certPath ?: "",
        "/p", certPassword,
        outFile.absolutePath
    )

    doLast {
        logger.lifecycle("Signed MSIX: ${outFile.absolutePath}")
    }
}
