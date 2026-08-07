plugins {
    kotlin("multiplatform") version "2.4.0" apply false
    kotlin("plugin.compose") version "2.4.0" apply false
    id("org.jetbrains.compose") version "1.9.0" apply false
}

allprojects {
    group = "com.speeduino.manager"
    version = "1.0.4"
}
