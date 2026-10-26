package com.abdullayevilkin.dontdisturb

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Ongoing notification shown while DND is on. It carries the timer choices
 * (15 min / 1 hour) and a "Turn off" button; once a timer runs it shows a
 * live countdown.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "dnd_status"
    private const val NOTIFICATION_ID = 1

    fun show(ctx: Context) {
        if (!canPost(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        ensureChannel(ctx, nm)

        val end = DndManager.endAt(ctx)
        val timerRunning = end > System.currentTimeMillis()

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dnd)
            .setContentTitle(ctx.getString(R.string.notif_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(ctx))

        if (timerRunning) {
            builder
                .setContentText(ctx.getString(R.string.notif_until, DndManager.formatTime(ctx, end)))
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder
                .setContentText(ctx.getString(R.string.notif_pick_timer))
                .setShowWhen(false)
        }

        builder
            .addAction(0, ctx.getString(R.string.action_15m), timerIntent(ctx, 15))
            .addAction(0, ctx.getString(R.string.action_1h), timerIntent(ctx, 60))
            .addAction(0, ctx.getString(R.string.action_off), disableIntent(ctx))

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(ctx: Context, nm: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(channel)
    }

    private fun openAppIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun timerIntent(ctx: Context, minutes: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        minutes, // distinct request code per choice
        Intent(ctx, DndReceiver::class.java)
            .setAction(DndManager.ACTION_START_TIMER)
            .putExtra(DndManager.EXTRA_MINUTES, minutes),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun disableIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        0,
        Intent(ctx, DndReceiver::class.java).setAction(DndManager.ACTION_DISABLE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
