package org.maocide.undeadwallpaper.features

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * 三击桌面空白处切换壁纸纯黑/恢复播放控制器。
 * - 内置防滑动误触半径校验（超过 touchSlop 自动重置计数）
 * - 支持平滑淡入淡出黑屏（uBlackoutAlpha: 1.0f <-> 0.0f）
 * - 黑屏期间可自动暂停 ExoPlayer 解码以降低功耗
 */
class TripleTapBlackoutController(
    context: Context,
    private var config: EnhancedWallpaperConfig,
    private val onBlackoutStateChanged: (isBlackedOut: Boolean) -> Unit
) {
    private val maxTapDistancePx: Float = ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat()

    private var tapCount = 0
    private var lastTapTimeMs = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private var downX = 0f
    private var downY = 0f

    @Volatile var isBlackedOut: Boolean = false
        private set

    private var currentAlpha: Float = 1.0f
    private var animStartAlpha: Float = 1.0f
    private var animTargetAlpha: Float = 1.0f
    private var animStartMs: Long = 0L
    private val fadeDurationMs: Long = 260L

    fun updateConfig(newConfig: EnhancedWallpaperConfig) {
        config = newConfig
        if (!config.tripleTapBlackoutEnabled && isBlackedOut) {
            setBlackout(false)
        }
    }

    /**
     * 在 WallpaperService.Engine.onTouchEvent(event) 中调用。
     * 返回 true 表示本次触摸触发了三击黑屏切换。
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!config.tripleTapBlackoutEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val moveDist = hypot((event.x - downX).toDouble(), (event.y - downY).toDouble()).toFloat()
                // 若手指发生明显滑动（例如翻页），不计入点击
                if (moveDist > maxTapDistancePx * 0.5f) {
                    resetTaps()
                    return false
                }

                val now = SystemClock.uptimeMillis()
                val withinTime = (now - lastTapTimeMs) <= config.tripleTapWindowMs
                val withinArea = hypot((event.x - lastTapX).toDouble(), (event.y - lastTapY).toDouble()).toFloat() <= maxTapDistancePx

                if (tapCount > 0 && withinTime && withinArea) {
                    tapCount += 1
                } else {
                    tapCount = 1
                }

                lastTapTimeMs = now
                lastTapX = event.x
                lastTapY = event.y

                if (tapCount >= 3) {
                    resetTaps()
                    setBlackout(!isBlackedOut)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> resetTaps()
        }
        return false
    }

    fun setBlackout(blackout: Boolean) {
        if (isBlackedOut == blackout) return
        isBlackedOut = blackout
        animStartAlpha = currentAlpha
        animTargetAlpha = if (blackout) 0.0f else 1.0f
        animStartMs = SystemClock.uptimeMillis()
        onBlackoutStateChanged(blackout)
    }

    /**
     * 每帧渲染时调用，返回传入着色器 uBlackoutAlpha 的值 (0.0f = 纯黑, 1.0f = 正常画面)。
     */
    fun stepAndGetAlpha(): Float {
        if (currentAlpha == animTargetAlpha) return currentAlpha
        val elapsed = (SystemClock.uptimeMillis() - animStartMs).toFloat()
        val t = (elapsed / fadeDurationMs).coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)
        currentAlpha = animStartAlpha + (animTargetAlpha - animStartAlpha) * eased
        return currentAlpha
    }

    val shouldPausePlayer: Boolean
        get() = isBlackedOut && config.tripleTapPausePlayer

    private fun resetTaps() {
        tapCount = 0
        lastTapTimeMs = 0L
    }
}
