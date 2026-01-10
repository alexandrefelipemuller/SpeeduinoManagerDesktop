plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
        }
        val jvmTest by creating {
            dependsOn(commonTest)
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation("com.fazecast:jSerialComm:2.11.0")
            }
        }
        val desktopTest by getting {
            dependsOn(jvmTest)
        }
    }
}
