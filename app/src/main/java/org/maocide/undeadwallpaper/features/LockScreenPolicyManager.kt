package org.maocide.undeadwallpaper.features

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class LockScreenPolicyManager(
    private val context: Context,
    private var config: EnhancedWallpaperConfig,
    private val listener: Listener
) {
    interface Listener {
        fun onSwitchToLockVideo(videoUri: String)
        fun onSwitchToHomeVideo()
        fun onLockScreenFreezeChanged(frozen: Boolean)
        fun onTriggerPlayOnceOnUnlock()
    }

    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var isRegistered = false
    private var currentlyLocked: Boolean = false

    val isKeyguardLocked: Boolean
        get() = keyguardManager.isKeyguardLocked

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    currentlyLocked = true
                    applyPolicyForCurrentState(unlockedEvent = false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    currentlyLocked = keyguardManager.isKeyguardLocked
                    applyPolicyForCurrentState(unlockedEvent = false)
                }
                Intent.ACTION_USER_PRESENT -> {
                    currentlyLocked = false
                    applyPolicyForCurrentState(unlockedEvent = true)
                }
            }
        }
    }

    fun updateConfig(newConfig: EnhancedWallpaperConfig) {
        config = newConfig
        currentlyLocked = keyguardManager.isKeyguardLocked
        applyPolicyForCurrentState(unlockedEvent = false)
    }

    fun start() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isRegistered = true
        currentlyLocked = keyguardManager.isKeyguardLocked
        applyPolicyForCurrentState(unlockedEvent = false)
    }

    fun stop() {
        if (!isRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        isRegistered = false
    }

    private fun applyPolicyForCurrentState(unlockedEvent: Boolean) {
        when (config.lockScreenMode) {
            LockScreenMode.SAME_AS_HOME -> {
                listener.onLockScreenFreezeChanged(false)
                listener.onSwitchToHomeVideo()
            }
            LockScreenMode.SEPARATE_LOCK_VIDEO -> {
                listener.onLockScreenFreezeChanged(false)
                val lockUri = config.lockScreenVideoUri
                if (currentlyLocked && !lockUri.isNullOrBlank()) {
                    listener.onSwitchToLockVideo(lockUri)
                } else {
                    listener.onSwitchToHomeVideo()
                }
            }
            LockScreenMode.FREEZE_ON_LOCK -> {
                listener.onSwitchToHomeVideo()
                listener.onLockScreenFreezeChanged(currentlyLocked)
            }
            LockScreenMode.PLAY_ONCE_ON_UNLOCK -> {
                listener.onSwitchToHomeVideo()
                if (currentlyLocked) {
                    listener.onLockScreenFreezeChanged(true)
                } else if (unlockedEvent) {
                    listener.onLockScreenFreezeChanged(false)
                    listener.onTriggerPlayOnceOnUnlock()
                }
            }
        }
    }
}
