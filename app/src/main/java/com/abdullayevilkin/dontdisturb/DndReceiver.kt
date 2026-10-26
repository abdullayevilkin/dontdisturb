package com.abdullayevilkin.dontdisturb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the notification buttons and the timer-end alarm (not exported). */
class DndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DndManager.ACTION_START_TIMER ->
                DndManager.startTimer(context, intent.getIntExtra(DndManager.EXTRA_MINUTES, 0))
            DndManager.ACTION_DISABLE -> DndManager.disable(context)
            DndManager.ACTION_TIMER_END -> DndManager.onTimerFired(context)
        }
    }
}
