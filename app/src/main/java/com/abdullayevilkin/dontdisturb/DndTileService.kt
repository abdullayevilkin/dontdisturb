package com.abdullayevilkin.dontdisturb

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: tap to toggle Do Not Disturb. */
class DndTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        DndManager.syncWithSystem(this)
        render(DndManager.isDndOn(this))
    }

    override fun onClick() {
        super.onClick()
        if (!DndManager.hasPolicyAccess(this)) {
            openApp() // lets the user grant "Do Not Disturb access"
            return
        }
        val turnOn = !DndManager.isDndOn(this)
        if (turnOn) DndManager.enable(this) else DndManager.disable(this)
        render(turnOn)
    }

    private fun render(on: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_dnd)
        if (Build.VERSION.SDK_INT >= 29) {
            val end = DndManager.endAt(this)
            tile.subtitle = when {
                !DndManager.hasPolicyAccess(this) -> getString(R.string.tile_sub_no_access)
                on && end > System.currentTimeMillis() ->
                    getString(R.string.tile_sub_until, DndManager.formatTime(this, end))
                on -> getString(R.string.tile_sub_on)
                else -> getString(R.string.tile_sub_off)
            }
        }
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
