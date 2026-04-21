package android.util

object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6

    fun v(tag: String, msg: String): Int = print("V", tag, msg)
    fun d(tag: String, msg: String): Int = print("D", tag, msg)
    fun i(tag: String, msg: String): Int = print("I", tag, msg)
    fun w(tag: String, msg: String): Int = print("W", tag, msg)
    fun e(tag: String, msg: String): Int = print("E", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable?): Int {
        print("E", tag, msg)
        tr?.printStackTrace()
        return ERROR
    }

    private fun print(level: String, tag: String, msg: String): Int {
        println("$level/$tag: $msg")
        return when (level) {
            "V" -> VERBOSE
            "D" -> DEBUG
            "I" -> INFO
            "W" -> WARN
            else -> ERROR
        }
    }
}
