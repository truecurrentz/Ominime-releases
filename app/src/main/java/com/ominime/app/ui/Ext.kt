package com.ominime.app.ui

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object fmt {
    fun clock(ms: Long): String {
        val s = (ms / 1000L).coerceAtLeast(0L)
        val h = s / 3600L
        val m = (s % 3600L) / 60L
        val sec = s % 60L
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
            else String.format(Locale.US, "%d:%02d", m, sec)
    }

    fun left(ms: Long): String {
        val m = (ms / 60000L).coerceAtLeast(0L)
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
    }

    fun size(b: Long): String = when {
        b >= 1L shl 30 -> String.format(Locale.US, "%.1f GB", b / 1073741824f)
        b >= 1L shl 20 -> String.format(Locale.US, "%.0f MB", b / 1048576f)
        else -> String.format(Locale.US, "%.0f KB", b / 1024f)
    }

    fun date(ms: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(ms))

    fun whens(ms: Long): String {
        if (ms <= 0) return ""
        val d = (ms - System.currentTimeMillis()) / 1000L
        if (d <= 0) return "aired"
        val h = d / 3600L
        return when {
            h < 24 -> "in ${h.coerceAtLeast(1)}h"
            h < 24 * 30 -> "in ${h / 24}d"
            else -> "in ${h / (24 * 30)}mo"
        }
    }

    fun cut(s: String?, n: Int): String {
        val t = s?.trim().orEmpty()
        if (t.length <= n) return t
        return t.take(n).trimEnd().trimEnd(',', '.', ' ') + "..."
    }

    fun upper(s: String?): String = s?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""

    fun ep(n: Int): String = if (n > 0) "EP $n" else "EP"

    fun tone(s: String?): Color = when (s) {
        "RELEASING" -> Color(0xFF32D74B)
        "FINISHED" -> Color(0xFF8E8E93)
        "NOT_YET_RELEASED" -> Color(0xFFFF9F0A)
        "CANCELLED" -> Color(0xFFFF453A)
        else -> Color(0xFF8E8E93)
    }
}

fun origin(url: String): String = runCatching {
    val u = java.net.URI(url)
    u.scheme + "://" + u.host + if (u.port > 0) ":" + u.port else ""
}.getOrDefault("https://www.google.com")

fun android.content.Context.findact(): android.app.Activity? {
    var c: android.content.Context = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}
