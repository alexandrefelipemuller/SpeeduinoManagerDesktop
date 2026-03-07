package com.speeduino.manager.shared

import android.util.Log

actual object Logger {
    actual fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: RuntimeException) {
            println("D/$tag: $message")
        }
    }

    actual fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: RuntimeException) {
            println("I/$tag: $message")
        }
    }

    actual fun w(tag: String, message: String) {
        try {
            Log.w(tag, message)
        } catch (_: RuntimeException) {
            println("W/$tag: $message")
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: RuntimeException) {
            if (throwable != null) {
                println("E/$tag: $message\n${throwable.stackTraceToString()}")
            } else {
                println("E/$tag: $message")
            }
        }
    }
}
