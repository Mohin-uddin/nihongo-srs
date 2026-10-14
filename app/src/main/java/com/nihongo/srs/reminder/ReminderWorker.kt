package com.nihongo.srs.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihongo.srs.MainActivity
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.R

/**
 * Runs once a day (scheduled by [ReminderScheduler]) and shows a local
 * notification: how many words are due, plus a nudge to take today's exam.
 * Fully offline, no server or push service needed.
 */
class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NihongoApp
        if (!app.prefs.reminderEnabled) return Result.success()
        val due = app.repository.countDueToday()
        show(applicationContext, due)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "Daily study reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Reminds you to review your Japanese words" }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        fun show(context: Context, dueCount: Int) {
            val allowed = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!allowed) return

            val (title, text) = if (dueCount > 0) {
                "$dueCount words to review today • আজ $dueCount টি শব্দ রিভিউ" to
                    "Review now so you don't forget them! • এখনই রিভিউ করুন, ভুলবেন না!"
            } else {
                "Time for Japanese! • জাপানি শেখার সময়!" to
                    "Learn 10 new words and take today's exam • ১০টি নতুন শব্দ শিখুন, আজকের পরীক্ষা দিন"
            }
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Permission was revoked; nothing to do.
            }
        }
    }
}
