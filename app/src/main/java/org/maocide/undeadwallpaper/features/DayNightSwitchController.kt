package org.maocide.undeadwallpaper.features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * 昼夜自动切换控制器。
 * 监听系统分钟级时间变化 (ACTION_TIME_TICK) 与时区/时间修改，
 * 当跨越白天/夜晚分界点时触发回调，配合 TransitionCrossfadeController 实现平滑切换。
 */
class DayNightSwitchController(
    private val context: Context,
    private var config: EnhancedWallpaperConfig,
    private val onPeriodVideoResolved: (period: DayNightSlot, videoUri: String) -> Unit
) {
    private var isRegistered = false
    private var lastPeriod: DayNightSlot? = null
    private var lastAppliedUri: String? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            evaluateNow(forceNotify = false)
        }
    }

    fun resolveCurrentPeriod(): DayNightSlot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayStart = config.dayStartHour
        val nightStart = config.nightStartHour
        val isDay = if (dayStart < nightStart) {
            hour in dayStart until nightStart
        } else {
            // 兼容跨午夜的自定义区间
            hour >= dayStart || hour < nightStart
        }
        return if (isDay) DayNightSlot.DAY else DayNightSlot.NIGHT
    }

    fun resolveCurrentVideoUri(): String? {
        if (!config.dayNightEnabled) return null
        return when (resolveCurrentPeriod()) {
            DayNightSlot.DAY -> config.dayVideoUri
            DayNightSlot.NIGHT -> config.nightVideoUri
        }
    }

    fun updateConfig(newConfig: EnhancedWallpaperConfig) {
        config = newConfig
        evaluateNow(forceNotify = true)
    }

    fun start() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isRegistered = true
        evaluateNow(forceNotify = true)
    }

    fun stop() {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        isRegistered = false
    }

    fun evaluateNow(forceNotify: Boolean = false) {
        if (!config.dayNightEnabled) return
        val period = resolveCurrentPeriod()
        val uri = resolveCurrentVideoUri() ?: return
        if (forceNotify || period != lastPeriod || uri != lastAppliedUri) {
            lastPeriod = period
            lastAppliedUri = uri
            onPeriodVideoResolved(period, uri)
        }
    }
}
