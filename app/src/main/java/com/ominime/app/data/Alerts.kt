package com.ominime.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ominime.app.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

object alerts {
    private const val slot = "ominime-episodes"
    private const val job = "ominime-check"

    fun setUp(c: Context) {
        runCatching {
            val mgr = c.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(slot, "New episodes", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Tells you when a show on your list airs a new episode"
            mgr.createNotificationChannel(ch)
        }
    }

    fun start(c: Context) {
        runCatching {
            val req = PeriodicWorkRequestBuilder<Checker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(job, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }

    fun stop(c: Context) {
        runCatching { WorkManager.getInstance(c).cancelUniqueWork(job) }
    }

    fun push(c: Context, n: Note) {
        runCatching {
            setUp(c)
            val open = c.packageManager.getLaunchIntentForPackage(c.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open", n.id)
                data = Uri.parse("ominime://detail/" + n.id)
            }
            val tap = PendingIntent.getActivity(
                c,
                n.id * 31 + n.ep,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val card = NotificationCompat.Builder(c, slot)
                .setSmallIcon(R.drawable.note)
                .setContentTitle(n.title)
                .setContentText("Episode " + n.ep + " just aired")
                .setSubText("Ominime")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(tap)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Episode " + n.ep + " just aired. Tap to open the show."))
            val mgr = NotificationManagerCompat.from(c)
            if (mgr.areNotificationsEnabled()) mgr.notify(n.id, card.build())
        }
    }
}

class Checker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        runCatching {
            var wait = 0
            while (!store.ready.value && wait < 40) {
                delay(250)
                wait++
            }
            if (!store.conf.alerts) return Result.success()
            val got = repo.scan()
            if (got.notes.isNotEmpty()) got.notes.take(4).forEach { alerts.push(applicationContext, it) }
        }
        alerts.start(applicationContext)
        return Result.success()
    }
}
