package org.maocide.undeadwallpaper.features

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

/**
 * 独立的「扩展功能区」页面，集中管理全部 7 项新增功能，不污染原版设置界面。
 * 采用纯代码构建 Material 3 卡片布局，直接放入工程即可编译，无需额外 XML 资源合并。
 */
class EnhancedFeaturesActivity : AppCompatActivity() {

    private lateinit var config: EnhancedWallpaperConfig
    private var pendingPickTarget: PickTarget? = null

    private var tvDayVideo: TextView? = null
    private var tvNightVideo: TextView? = null
    private var tvLockVideo: TextView? = null

    private enum class PickTarget { DAY, NIGHT, LOCK }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val uriStr = uri.toString()
        when (pendingPickTarget) {
            PickTarget.DAY -> {
                config = config.copy(dayVideoUri = uriStr)
                tvDayVideo?.text = "白天视频：已选择"
            }
            PickTarget.NIGHT -> {
                config = config.copy(nightVideoUri = uriStr)
                tvNightVideo?.text = "夜晚视频：已选择"
            }
            PickTarget.LOCK -> {
                config = config.copy(lockScreenVideoUri = uriStr)
                tvLockVideo?.text = "锁屏视频：已选择"
            }
            null -> Unit
        }
        persist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = FeatureConfigStore.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "扩展功能区"
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(8), pad, dp(24))
        }

        // 卡片 1：三击屏幕壁纸黑屏
        container.addView(buildCard("三击屏幕黑屏") { card ->
            card.addView(makeSwitch("开启三击黑屏（再三击恢复）", config.tripleTapBlackoutEnabled) { checked ->
                config = config.copy(tripleTapBlackoutEnabled = checked)
                persist()
            })
            card.addView(makeSwitch("黑屏时暂停视频解码（更省电）", config.tripleTapPausePlayer) { checked ->
                config = config.copy(tripleTapPausePlayer = checked)
                persist()
            })
            card.addView(makeSlider("三击判定间隔", config.tripleTapWindowMs.toFloat(), 250f, 800f, 50f, "ms") { v ->
                config = config.copy(tripleTapWindowMs = v.toLong())
                persist()
            })
        })

        // 卡片 2：昼夜自动切换
        container.addView(buildCard("昼夜自动切换") { card ->
            card.addView(makeSwitch("按时间自动切换白天/夜晚壁纸", config.dayNightEnabled) { checked ->
                config = config.copy(dayNightEnabled = checked)
                persist()
            })
            card.addView(makeSlider("白天开始时间", config.dayStartHour.toFloat(), 4f, 12f, 1f, ":00") { v ->
                config = config.copy(dayStartHour = v.roundToInt())
                persist()
            })
            card.addView(makeSlider("夜晚开始时间", config.nightStartHour.toFloat(), 16f, 23f, 1f, ":00") { v ->
                config = config.copy(nightStartHour = v.roundToInt())
                persist()
            })
            tvDayVideo = makeSubLabel(if (config.dayVideoUri.isNullOrBlank()) "白天视频：未设置" else "白天视频：已选择")
            card.addView(tvDayVideo)
            card.addView(makeButton("选择白天视频") {
                pendingPickTarget = PickTarget.DAY
                videoPicker.launch(arrayOf("video/*"))
            })
            tvNightVideo = makeSubLabel(if (config.nightVideoUri.isNullOrBlank()) "夜晚视频：未设置" else "夜晚视频：已选择")
            card.addView(tvNightVideo)
            card.addView(makeButton("选择夜晚视频") {
                pendingPickTarget = PickTarget.NIGHT
                videoPicker.launch(arrayOf("video/*"))
            })
        })

        // 卡片 3：多视频 / 昼夜切换过渡
        container.addView(buildCard("多视频 / 昼夜切换过渡") { card ->
            card.addView(makeSwitch("开启平滑淡入淡出过渡", config.transitionEnabled) { checked ->
                config = config.copy(transitionEnabled = checked)
                persist()
            })
            card.addView(makeSlider("过渡时长", config.transitionDurationMs.toFloat(), 150f, 1500f, 50f, "ms") { v ->
                config = config.copy(transitionDurationMs = v.toLong())
                persist()
            })
        })

        // 卡片 4：低电量 / 省电模式自动冻结
        container.addView(buildCard("低电量 / 省电自动冻结") { card ->
            card.addView(makeSwitch("系统省电模式时自动冻结", config.freezeOnPowerSave) { checked ->
                config = config.copy(freezeOnPowerSave = checked)
                persist()
            })
            card.addView(makeSwitch("低电量时自动冻结", config.freezeOnLowBattery) { checked ->
                config = config.copy(freezeOnLowBattery = checked)
                persist()
            })
            card.addView(makeSwitch("充电时自动恢复播放", config.resumeWhenCharging) { checked ->
                config = config.copy(resumeWhenCharging = checked)
                persist()
            })
            card.addView(makeSlider("低电量阈值", config.lowBatteryThresholdPercent.toFloat(), 5f, 50f, 5f, "%") { v ->
                config = config.copy(lowBatteryThresholdPercent = v.roundToInt())
                persist()
            })
        })

        // 卡片 5：锁屏与主屏差异化
        container.addView(buildCard("锁屏与主屏差异化") { card ->
            val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
            val modes = listOf(
                LockScreenMode.SAME_AS_HOME to "锁屏与主屏同步播放",
                LockScreenMode.SEPARATE_LOCK_VIDEO to "锁屏使用独立视频",
                LockScreenMode.FREEZE_ON_LOCK to "锁屏时定格暂停（省电）",
                LockScreenMode.PLAY_ONCE_ON_UNLOCK to "解锁亮屏播放一次后定格"
            )
            modes.forEachIndexed { idx, (mode, label) ->
                val rb = RadioButton(this).apply {
                    id = 1000 + idx
                    text = label
                    isChecked = (config.lockScreenMode == mode)
                }
                group.addView(rb)
            }
            group.setOnCheckedChangeListener { _, checkedId ->
                val index = checkedId - 1000
                if (index in modes.indices) {
                    config = config.copy(lockScreenMode = modes[index].first)
                    persist()
                }
            }
            card.addView(group)

            tvLockVideo = makeSubLabel(if (config.lockScreenVideoUri.isNullOrBlank()) "锁屏视频：未设置" else "锁屏视频：已选择")
            card.addView(tvLockVideo)
            card.addView(makeButton("选择锁屏独立视频") {
                pendingPickTarget = PickTarget.LOCK
                videoPicker.launch(arrayOf("video/*"))
            })
        })

        // 卡片 6：实时模糊与暗角
        container.addView(buildCard("实时模糊与暗角") { card ->
            card.addView(makeSlider("实时模糊半径", config.blurRadius, 0f, 12f, 0.5f, "px") { v ->
                config = config.copy(blurRadius = v)
                persist()
            })
            card.addView(makeSlider("边缘暗角强度", config.vignetteIntensity * 100f, 0f, 100f, 5f, "%") { v ->
                config = config.copy(vignetteIntensity = v / 100f)
                persist()
            })
            card.addView(makeSlider("暗角羽化范围", config.vignetteSoftness * 100f, 15f, 85f, 5f, "%") { v ->
                config = config.copy(vignetteSoftness = v / 100f)
                persist()
            })
        })

        // 卡片 7：陀螺仪双向视差
        container.addView(buildCard("陀螺仪双向视差") { card ->
            card.addView(makeSwitch("开启陀螺仪 X/Y 双向倾斜视差", config.gyroParallaxEnabled) { checked ->
                config = config.copy(gyroParallaxEnabled = checked)
                persist()
            })
            card.addView(makeSlider("水平视差幅度", config.gyroStrengthX * 100f, 0f, 15f, 1f, "%") { v ->
                config = config.copy(gyroStrengthX = v / 100f)
                persist()
            })
            card.addView(makeSlider("垂直视差幅度", config.gyroStrengthY * 100f, 0f, 15f, 1f, "%") { v ->
                config = config.copy(gyroStrengthY = v / 100f)
                persist()
            })
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
    }

    private fun persist() {
        FeatureConfigStore.saveAndNotify(this, config)
    }

    private fun buildCard(title: String, builder: (LinearLayout) -> Unit): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(16)
            setPadding(p, p, p, p)
        }
        val header = TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        inner.addView(header)
        builder(inner)
        card.addView(inner)
        return card
    }

    private fun makeSwitch(label: String, initial: Boolean, onChanged: (Boolean) -> Unit): MaterialSwitch {
        return MaterialSwitch(this).apply {
            text = label
            isChecked = initial
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
            setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
        }
    }

    private fun makeSlider(
        label: String,
        initial: Float,
        from: Float,
        to: Float,
        step: Float,
        unit: String,
        onChanged: (Float) -> Unit
    ): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(6))
        }
        val tv = TextView(this).apply {
            text = "$label：${formatVal(initial)}$unit"
            textSize = 13f
        }
        val slider = Slider(this).apply {
            valueFrom = from
            valueTo = to
            stepSize = step
            value = initial.coerceIn(from, to)
            addOnChangeListener { _, value, fromUser ->
                tv.text = "$label：${formatVal(value)}$unit"
                if (fromUser) onChanged(value)
            }
        }
        box.addView(tv)
        box.addView(slider)
        return box
    }

    private fun makeSubLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setPadding(0, dp(6), 0, dp(4))
    }

    private fun makeButton(text: String, onClick: () -> Unit): MaterialButton = MaterialButton(this).apply {
        this.text = text
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }
    }

    private fun formatVal(v: Float): String =
        if (v % 1f == 0f) v.roundToInt().toString() else String.format("%.1f", v)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
