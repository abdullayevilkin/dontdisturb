package com.abdullayevilkin.dontdisturb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms a running timer after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DndManager.restore(context)
    }
}
