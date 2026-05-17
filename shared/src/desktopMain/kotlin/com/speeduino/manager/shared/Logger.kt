package com.speeduino.manager.shared

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual object Logger {
    private val lock = ReentrantLock()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logDir = File(System.getProperty("user.home"), ".speeduino-manager-desktop/logs").apply { mkdirs() }
    private val logFile = File(logDir, "desktop.log")

    actual fun d(tag: String, message: String) {
        write("D", tag, message)
    }

    actual fun i(tag: String, message: String) {
        write("I", tag, message)
    }

    actual fun w(tag: String, message: String) {
        write("W", tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        write("E", tag, message)
        throwable?.printStackTrace()
        if (throwable != null) {
            appendLine(stackTraceString(throwable))
        }
    }

    private fun write(level: String, tag: String, message: String) {
        val line = "$level/$tag: $message"
        println(line)
        appendLine("[${formatter.format(Date())}] $line")
    }

    private fun appendLine(line: String) {
        lock.withLock {
            logFile.appendText(line + System.lineSeparator())
        }
    }

    private fun stackTraceString(throwable: Throwable): String {
        return buildString {
            appendLine(throwable.toString())
            throwable.stackTrace.forEach { frame ->
                appendLine("\tat $frame")
            }
            var cause = throwable.cause
            while (cause != null) {
                appendLine("Caused by: $cause")
                cause.stackTrace.forEach { frame ->
                    appendLine("\tat $frame")
                }
                cause = cause.cause
            }
        }
    }
}
