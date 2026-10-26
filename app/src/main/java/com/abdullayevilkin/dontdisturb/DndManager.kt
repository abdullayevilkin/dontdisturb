package com.abdullayevilkin.dontdisturb

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import java.util.Date

/**
 * Single place that turns Do Not Disturb on/off, owns the auto-disable timer
 * and keeps the notification in sync. No root needed: it uses the official
 * NotificationManager.setInterruptionFilter() API, which requires the user to
 * grant "Do Not Disturb access" once.
 */
object DndManager {

    const val ACTION_START_TIMER = "com.abdullayevilkin.dontdisturb.START_TIMER"
    const val ACTION_DISABLE = "com.abdullayevilkin.dontdisturb.DISABLE"
    const val ACTION_TIMER_END = "com.abdullayevilkin.dontdisturb.TIMER_END"
    const val EXTRA_MINUTES = "minutes"

    private const val PREFS = "dnd_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_END_AT = "end_at"

    // Behaves like the system's own "Do Not Disturb" (respects the user's allowed exceptions).
    // Use INTERRUPTION_FILTER_NONE for total silence, or _ALARMS for "alarms only".
    private const val FILTER_ON = NotificationManager.INTERRUPTION_FILTER_PRIORITY

    private fun nm(ctx: Context): NotificationManager =
        ctx.getSystemService(NotificationManager::class.java)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPolicyAccess(ctx: Context): Boolean = nm(ctx).isNotificationPolicyAccessGranted

    fun isDndOn(ctx: Context): Boolean {
        val filter = nm(ctx).currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    /** Epoch millis when the running timer ends, or 0 when there is no timer. */
    fun endAt(ctx: Context): Long = prefs(ctx).getLong(KEY_END_AT, 0L)

    fun formatTime(ctx: Context, millis: Long): String =
        DateFormat.getTimeFormat(ctx).format(Date(millis))

    /** Turn DND on without a timer. */
    fun enable(ctx: Context): Boolean {
        if (!hasPolicyAccess(ctx)) return false
        nm(ctx).setInterruptionFilter(FILTER_ON)
        prefs(ctx).edit().putBoolean(KEY_ACTIVE, true).putLong(KEY_END_AT, 0L).apply()
        cancelAlarm(ctx)
        NotificationHelper.show(ctx)
        return true
    }

    /** Turn DND on (if needed) and switch it off automatically after [minutes]. */
    fun startTimer(ctx: Context, minutes: Int): Boolean {
        if (minutes <= 0 || !hasPolicyAccess(ctx)) return false
        nm(ctx).setInterruptionFilter(FILTER_ON)
        val end = System.currentTimeMillis() + minutes * 60_000L
        prefs(ctx).edit().putBoolean(KEY_ACTIVE, true).putLong(KEY_END_AT, end).apply()
        scheduleAlarm(ctx, end)
        NotificationHelper.show(ctx)
        return true
    }

    fun disable(ctx: Context) {
        if (hasPolicyAccess(ctx)) {
            nm(ctx).setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        clearState(ctx)
    }

    /** Called by the timer alarm. */
    fun onTimerFired(ctx: Context) {
        val end = endAt(ctx)
        if (end == 0L) return // timer was cancelled in the meantime
        if (end > System.currentTimeMillis() + 5_000L) return // stale alarm from an older timer
        disable(ctx)
    }

    /** If the user turned DND off elsewhere (system settings), drop our timer and notification. */
    fun syncWithSystem(ctx: Context) {
        val active = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        if (active && !isDndOn(ctx)) clearState(ctx)
    }

    /** After reboot or app update: re-arm a running timer or finish an expired one. */
    fun restore(ctx: Context) {
        syncWithSystem(ctx)
        if (!prefs(ctx).getBoolean(KEY_ACTIVE, false)) return
        val end = endAt(ctx)
        when {
            end == 0L -> NotificationHelper.show(ctx)
            end <= System.currentTimeMillis() -> disable(ctx)
            else -> {
                scheduleAlarm(ctx, end)
                NotificationHelper.show(ctx)
            }
        }
    }

    fun canScheduleExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun clearState(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        cancelAlarm(ctx)
        NotificationHelper.cancel(ctx)
    }

    private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        0,
        Intent(ctx, DndReceiver::class.java).setAction(ACTION_TIMER_END),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun scheduleAlarm(ctx: Context, at: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx)
        try {
            if (canScheduleExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx))
    }
}
