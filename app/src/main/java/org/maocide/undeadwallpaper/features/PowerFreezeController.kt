package org.maocide.undeadwallpaper.features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat

class PowerFreezeController(
    private val context: Context,
    private var config: EnhancedWallpaperConfig,
    private val onFreezeStateChanged: (shouldFreeze: Boolean) -> Unit
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var isRegistered = false
    private var lastFrozenState: Boolean? = null

    private var batteryPercent: Int = 100
    private var isCharging: Boolean = false
    private var isPowerSaveMode: Boolean = false

    val shouldFreeze: Boolean
        get() {
            if (config.resumeWhenCharging && isCharging) return false
            val powerSaveFreeze = config.freezeOnPowerSave && isPowerSaveMode
            val lowBatteryFreeze = config.freezeOnLowBattery && batteryPercent <= config.lowBatteryThresholdPercent
            return powerSaveFreeze || lowBatteryFreeze
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> parseBatteryIntent(intent)
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    isPowerSaveMode = powerManager.isPowerSaveMode
                }
            }
            dispatchIfChanged()
        }
    }

    fun updateConfig(newConfig: EnhancedWallpaperConfig) {
        config = newConfig
        dispatchIfChanged()
    }

    fun start() {
        if (isRegistered) return
        isPowerSaveMode = powerManager.isPowerSaveMode
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        val stickyBattery = ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        stickyBattery?.let { parseBatteryIntent(it) }
        isRegistered = true
        dispatchIfChanged()
    }

    fun stop() {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        isRegistered = false
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) {
            batteryPercent = (level * 100) / scale
        }
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        isCharging = plugged != 0
    }

    private fun dispatchIfChanged() {
        val frozen = shouldFreeze
        if (frozen != lastFrozenState) {
            lastFrozenState = frozen
            onFreezeStateChanged(frozen)
        }
    }
}
