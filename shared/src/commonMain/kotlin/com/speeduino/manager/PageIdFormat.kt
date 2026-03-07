package com.speeduino.manager

fun formatPageId(pageNum: Byte): String = formatPageId(pageNum.toInt() and 0xFF)

fun formatPageId(pageId: Int): String {
    val normalized = pageId and 0xFFFF
    return if (normalized > 0xFF || normalized >= 0xF0) {
        "0x${normalized.toString(16).uppercase().padStart(if (normalized > 0xFF) 4 else 2, '0')}"
    } else {
        normalized.toString()
    }
}
