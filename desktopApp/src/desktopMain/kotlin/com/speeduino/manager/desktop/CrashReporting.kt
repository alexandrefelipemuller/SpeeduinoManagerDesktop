package com.speeduino.manager.desktop

import io.sentry.Sentry

object CrashReporting {
    fun init() {
        // Baked in at build time by the generateSentryConfig Gradle task; empty when not configured.
        val dsn = System.getenv("SENTRY_DSN")
            ?: CrashReporting::class.java.getResource("/sentry-dsn.txt")?.readText()?.trim()
        if (dsn.isNullOrBlank()) return

        // jpackage sets this property in packaged builds; absent when running via Gradle.
        val packagedVersion = System.getProperty("jpackage.app-version")
        val environment = System.getenv("SENTRY_ENVIRONMENT")
            ?: if (packagedVersion != null) "production" else "development"

        Sentry.init { options ->
            options.dsn = dsn
            options.release = "speeduino-manager-desktop@${packagedVersion ?: "dev"}"
            options.environment = environment
            options.tracesSampleRate = if (environment == "production") 0.1 else 1.0
            options.isDebug = System.getenv("SENTRY_DEBUG") == "true"
        }

        if (System.getenv("SENTRY_TEST_EVENT") == "true") {
            try {
                throw Exception("This is a test.")
            } catch (e: Exception) {
                Sentry.captureException(e)
            }
        }
    }
}
