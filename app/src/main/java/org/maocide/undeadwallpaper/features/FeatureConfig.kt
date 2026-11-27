package org.maocide.undeadwallpaper.features

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

enum class LockScreenMode {
    SAME_AS_HOME,        // 锁屏与主屏一致
    SEPARATE_LOCK_VIDEO, // 锁屏使用独立视频
    FREEZE_ON_LOCK,      // 锁屏时定格暂停以省电
    PLAY_ONCE_ON_UNLOCK  // 解锁亮屏播放一次后定格
}

enum class DayNightSlot {
    DAY,
    NIGHT
}

data class EnhancedWallpaperConfig(
    // 1. 三击屏幕黑屏
    val tripleTapBlackoutEnabled: Boolean = true,
    val tripleTapWindowMs: Long = 450L,
    val tripleTapPausePlayer: Boolean = true,

    // 2. 昼夜自动切换
    val dayNightEnabled: Boolean = false,
    val dayStartHour: Int = 7,           // 白天开始小时 (0~23)
    val nightStartHour: Int = 19,        // 夜晚开始小时 (0~23)
    val dayVideoUri: String? = null,
    val nightVideoUri: String? = null,

    // 3. 多视频 / 昼夜切换过渡
    val transitionEnabled: Boolean = true,
    val transitionDurationMs: Long = 450L,

    // 4. 低电量 / 省电模式自动冻结
    val freezeOnPowerSave: Boolean = true,
    val freezeOnLowBattery: Boolean = true,
    val lowBatteryThresholdPercent: Int = 20,
    val resumeWhenCharging: Boolean = true,

    // 5. 锁屏与主屏差异化
    val lockScreenMode: LockScreenMode = LockScreenMode.SAME_AS_HOME,
    val lockScreenVideoUri: String? = null,

    // 6. 实时模糊与暗角
    val blurRadius: Float = 0.0f,        // 0.0f (关闭) ~ 12.0f
    val vignetteIntensity: Float = 0.0f, // 0.0f (关闭) ~ 1.0f
    val vignetteSoftness: Float = 0.45f, // 0.1f ~ 0.9f

    // 7. 陀螺仪双向视差
    val gyroParallaxEnabled: Boolean = false,
    val gyroStrengthX: Float = 0.06f,    // 最大水平偏移比例 (0.0 ~ 0.15)
    val gyroStrengthY: Float = 0.06f,    // 最大垂直偏移比例 (0.0 ~ 0.15)
    val gyroSmoothing: Float = 0.12f     // 低通滤波系数 (越小越丝滑)
)

object FeatureConfigStore {
    private const val PREF_NAME = "uwu_enhanced_features"
    const val ACTION_ENHANCED_CONFIG_CHANGED = "org.maocide.undeadwallpaper.ACTION_ENHANCED_CONFIG_CHANGED"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): EnhancedWallpaperConfig {
        val p = prefs(context)
        val lockModeName = p.getString("lock_screen_mode", LockScreenMode.SAME_AS_HOME.name)
        val lockMode = runCatching { LockScreenMode.valueOf(lockModeName ?: "") }
            .getOrDefault(LockScreenMode.SAME_AS_HOME)

        return EnhancedWallpaperConfig(
            tripleTapBlackoutEnabled = p.getBoolean("triple_tap_blackout_enabled", true),
            tripleTapWindowMs = p.getLong("triple_tap_window_ms", 450L).coerceIn(250L, 900L),
            tripleTapPausePlayer = p.getBoolean("triple_tap_pause_player", true),
            dayNightEnabled = p.getBoolean("day_night_enabled", false),
            dayStartHour = p.getInt("day_start_hour", 7).coerceIn(0, 23),
            nightStartHour = p.getInt("night_start_hour", 19).coerceIn(0, 23),
            dayVideoUri = p.getString("day_video_uri", null),
            nightVideoUri = p.getString("night_video_uri", null),
            transitionEnabled = p.getBoolean("transition_enabled", true),
            transitionDurationMs = p.getLong("transition_duration_ms", 450L).coerceIn(100L, 2000L),
            freezeOnPowerSave = p.getBoolean("freeze_on_power_save", true),
            freezeOnLowBattery = p.getBoolean("freeze_on_low_battery", true),
            lowBatteryThresholdPercent = p.getInt("low_battery_threshold", 20).coerceIn(5, 50),
            resumeWhenCharging = p.getBoolean("resume_when_charging", true),
            lockScreenMode = lockMode,
            lockScreenVideoUri = p.getString("lock_screen_video_uri", null),
            blurRadius = p.getFloat("blur_radius", 0.0f).coerceIn(0f, 12f),
            vignetteIntensity = p.getFloat("vignette_intensity", 0.0f).coerceIn(0f, 1f),
            vignetteSoftness = p.getFloat("vignette_softness", 0.45f).coerceIn(0.1f, 0.9f),
            gyroParallaxEnabled = p.getBoolean("gyro_parallax_enabled", false),
            gyroStrengthX = p.getFloat("gyro_strength_x", 0.06f).coerceIn(0f, 0.15f),
            gyroStrengthY = p.getFloat("gyro_strength_y", 0.06f).coerceIn(0f, 0.15f),
            gyroSmoothing = p.getFloat("gyro_smoothing", 0.12f).coerceIn(0.02f, 0.5f)
        )
    }

    fun saveAndNotify(context: Context, config: EnhancedWallpaperConfig) {
        prefs(context).edit()
            .putBoolean("triple_tap_blackout_enabled", config.tripleTapBlackoutEnabled)
            .putLong("triple_tap_window_ms", config.tripleTapWindowMs)
            .putBoolean("triple_tap_pause_player", config.tripleTapPausePlayer)
            .putBoolean("day_night_enabled", config.dayNightEnabled)
            .putInt("day_start_hour", config.dayStartHour)
            .putInt("night_start_hour", config.nightStartHour)
            .putString("day_video_uri", config.dayVideoUri)
            .putString("night_video_uri", config.nightVideoUri)
            .putBoolean("transition_enabled", config.transitionEnabled)
            .putLong("transition_duration_ms", config.transitionDurationMs)
            .putBoolean("freeze_on_power_save", config.freezeOnPowerSave)
            .putBoolean("freeze_on_low_battery", config.freezeOnLowBattery)
            .putInt("low_battery_threshold", config.lowBatteryThresholdPercent)
            .putBoolean("resume_when_charging", config.resumeWhenCharging)
            .putString("lock_screen_mode", config.lockScreenMode.name)
            .putString("lock_screen_video_uri", config.lockScreenVideoUri)
            .putFloat("blur_radius", config.blurRadius)
            .putFloat("vignette_intensity", config.vignetteIntensity)
            .putFloat("vignette_softness", config.vignetteSoftness)
            .putBoolean("gyro_parallax_enabled", config.gyroParallaxEnabled)
            .putFloat("gyro_strength_x", config.gyroStrengthX)
            .putFloat("gyro_strength_y", config.gyroStrengthY)
            .putFloat("gyro_smoothing", config.gyroSmoothing)
            .apply()

        val intent = Intent(ACTION_ENHANCED_CONFIG_CHANGED).setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}
