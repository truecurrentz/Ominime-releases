package com.ominime.app

import android.app.Application
import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.ominime.app.data.alerts
import com.ominime.app.data.caps
import com.ominime.app.data.net
import com.ominime.app.data.offline
import com.ominime.app.data.store
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                File(filesDir, "crash.txt").writeText(
                    "time " + System.currentTimeMillis() + "\nthread " + t.name + "\n" +
                        android.util.Log.getStackTraceString(e)
                )
            }
            if (prev != null) prev.uncaughtException(t, e)
            else android.os.Process.killProcess(android.os.Process.myPid())
        }
        runCatching { net.init(this) }
        runCatching { caps.init(this) }
        runCatching { store.init(this) }
        runCatching { offline.init(this) }
        runCatching { alerts.setUp(this) }
        runCatching { alerts.start(this) }
        runCatching {
            Coil.setImageLoader(
                ImageLoader.Builder(this)
                    .crossfade(180)
                    .okHttpClient { net.cli }
                    .build()
            )
        }
    }
}

fun lastcrash(c: Context): String? = runCatching {
    val f = File(c.filesDir, "crash.txt")
    if (f.exists()) f.readText().takeIf { it.isNotBlank() } else null
}.getOrNull()

fun wipecrash(c: Context) {
    runCatching { File(c.filesDir, "crash.txt").delete() }
}
