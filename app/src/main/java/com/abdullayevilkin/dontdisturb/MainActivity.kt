package com.abdullayevilkin.dontdisturb

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

/** Setup screen: grant access, add the tile, turn the knob to pick a timer, toggle DND. */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var btnAccess: MaterialButton
    private lateinit var btnNotif: MaterialButton
    private lateinit var btnExact: MaterialButton
    private lateinit var btnToggle: MaterialButton
    private lateinit var knob: TimerKnobView

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }

    private val uiPrefs by lazy { getSharedPreferences("ui", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        status = findViewById(R.id.tvStatus)
        btnAccess = findViewById(R.id.btnAccess)
        btnNotif = findViewById(R.id.btnNotif)
        btnExact = findViewById(R.id.btnExact)
        btnToggle = findViewById(R.id.btnToggle)
        knob = findViewById(R.id.knob)

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        btnNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        btnExact.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 31) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
        findViewById<MaterialButton>(R.id.btnAddTile).setOnClickListener { requestAddTile() }

        knob.minutes = uiPrefs.getInt(KEY_LAST_MINUTES, 30)
        knob.onChange = { uiPrefs.edit().putInt(KEY_LAST_MINUTES, it).apply() }

        findViewById<MaterialButton>(R.id.btnStartTimer).setOnClickListener {
            startTimer(knob.minutes)
        }

        btnToggle.setOnClickListener {
            if (DndManager.isDndOn(this)) {
                DndManager.disable(this)
            } else if (!DndManager.enable(this)) {
                needAccess()
            }
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        DndManager.syncWithSystem(this)
        render()
    }

    private fun startTimer(minutes: Int) {
        if (!DndManager.startTimer(this, minutes)) needAccess()
        render()
    }

    private fun needAccess() {
        Toast.makeText(this, R.string.toast_need_access, Toast.LENGTH_LONG).show()
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            getSystemService(StatusBarManager::class.java).requestAddTileService(
                ComponentName(this, DndTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_dnd),
                mainExecutor
            ) { }
        } else {
            Toast.makeText(this, R.string.toast_tile_manual, Toast.LENGTH_LONG).show()
        }
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun render() {
        val access = DndManager.hasPolicyAccess(this)
        val on = DndManager.isDndOn(this)
        val end = DndManager.endAt(this)

        status.text = when {
            !access -> getString(R.string.status_no_access)
            on && end > System.currentTimeMillis() ->
                getString(R.string.status_on_until, DndManager.formatTime(this, end))
            on -> getString(R.string.status_on)
            else -> getString(R.string.status_off)
        }

        btnAccess.visibility = if (access) View.GONE else View.VISIBLE
        btnNotif.visibility = if (notificationsAllowed()) View.GONE else View.VISIBLE
        btnExact.visibility = if (DndManager.canScheduleExact(this)) View.GONE else View.VISIBLE
        btnToggle.setText(if (on) R.string.turn_off else R.string.turn_on)
    }

    private companion object {
        const val KEY_LAST_MINUTES = "last_minutes"
    }
}
