package org.maocide.undeadwallpaper.features

import android.os.SystemClock

class TransitionCrossfadeController(
    private var config: EnhancedWallpaperConfig
) {
    private enum class Phase { IDLE, FADING_OUT, FADING_IN }

    private var phase: Phase = Phase.IDLE
    private var phaseStartMs: Long = 0L
    private var pendingSwitchAction: (() -> Unit)? = null

    @Volatile var currentAlpha: Float = 1.0f
        private set

    fun updateConfig(newConfig: EnhancedWallpaperConfig) {
        config = newConfig
    }

    fun triggerSwitch(switchAction: () -> Unit) {
        if (!config.transitionEnabled || config.transitionDurationMs <= 0L) {
            currentAlpha = 1.0f
            switchAction()
            return
        }
        pendingSwitchAction = switchAction
        phase = Phase.FADING_OUT
        phaseStartMs = SystemClock.uptimeMillis()
    }

    fun stepAndGetAlpha(): Float {
        if (phase == Phase.IDLE) {
            currentAlpha = 1.0f
            return 1.0f
        }
        val halfDuration = (config.transitionDurationMs / 2L).coerceAtLeast(50L).toFloat()
        val elapsed = (SystemClock.uptimeMillis() - phaseStartMs).toFloat()
        val t = (elapsed / halfDuration).coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)

        when (phase) {
            Phase.FADING_OUT -> {
                currentAlpha = 1f - eased
                if (t >= 1f) {
                    pendingSwitchAction?.invoke()
                    pendingSwitchAction = null
                    phase = Phase.FADING_IN
                    phaseStartMs = SystemClock.uptimeMillis()
                }
            }
            Phase.FADING_IN -> {
                currentAlpha = eased
                if (t >= 1f) {
                    currentAlpha = 1f
                    phase = Phase.IDLE
                }
            }
            Phase.IDLE -> currentAlpha = 1f
        }
        return currentAlpha
    }
}
