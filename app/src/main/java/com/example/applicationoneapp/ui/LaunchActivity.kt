package com.example.applicationoneapp.ui

import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.example.applicationoneapp.MainActivity
import com.example.applicationoneapp.R
import com.example.applicationoneapp.auth.AuthStore
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.ui.widget.InkBlobs

/**
 * 开场动画（每次启动必播）。
 *
 * 深灰底白色圆盘 Logo 呼吸一次 → 白盘从圆心放大铺满 → 墨色晕染浮现 + Hello 上浮 →
 * 停留片刻后按登录态分流：已登录进主页，未登录进登录页。
 *
 * 点击屏幕任意处可跳过动画。
 */
@SuppressLint("CustomSplashScreen")
class LaunchActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val timers = mutableListOf<Runnable>()

    private lateinit var screen: FrameLayout
    private lateinit var backdrop: FrameLayout
    private lateinit var disc: FrameLayout
    private lateinit var discLabel: TextView
    private lateinit var door: View
    private lateinit var gate: FrameLayout
    private lateinit var ink: InkBlobs
    private lateinit var hello: TextView

    private var done = false
    private var leaving = false

    private val s: Float get() = resources.displayMetrics.widthPixels / dpF(this, 392f)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.apply(this)
        super.onCreate(savedInstanceState)
        buildUi()
        if (ThemePrefs.isDark(this)) {
            prepareDark()
            screen.post { darkSequence() }
        } else {
            screen.setBackgroundColor(Color.BLACK)
            screen.post { sequence() }
        }
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi() {
        screen = FrameLayout(this)

        // 深灰底 + 中心径向高光（暗色主题下整体隐藏，直接露出应用背景色）
        backdrop = FrameLayout(this)
        screen.addView(backdrop, FrameLayout.LayoutParams(MP, MP))
        backdrop.addView(View(this).apply {
            background = "#15161A".toColorInt().toDrawable()
        }, FrameLayout.LayoutParams(MP, MP))
        backdrop.addView(View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                gradientType = android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT
                gradientRadius = dpF(this@LaunchActivity, 300f * s)
                colors = intArrayOf(0xFF35373E.toInt(), 0xFF23252A.toInt(), 0xFF141518.toInt())
                setGradientCenter(0.5f, 0.5f)
            }
        }, FrameLayout.LayoutParams(MP, MP))

        // 白色门盘：与 Logo 同尺寸同位置，放大后铺满
        door = View(this).apply { background = Ui.oval(Color.WHITE); alpha = 0f }
        val discSize = dpF(this, 112f * s).toInt()
        screen.addView(door, FrameLayout.LayoutParams(discSize, discSize).apply { gravity = Gravity.CENTER })

        ink = InkBlobs(this)
        screen.addView(ink, FrameLayout.LayoutParams(MP, MP))

        // 内容层：靠 inset padding 避开真实状态栏 / 导航栏
        val content = FrameLayout(this)
        screen.addView(content, FrameLayout.LayoutParams(MP, MP))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(screen) { _, insets ->
            val sys = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            content.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        hello = TextView(this).apply {
            text = "Hello"
            setTextColor("#111214".toColorInt())
            textSize = 92f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = -0.045f
            gravity = Gravity.CENTER
            alpha = 0f
            setLineSpacing(0f, 1.1f)
        }
        content.addView(hello, FrameLayout.LayoutParams(MP, WC).apply { gravity = Gravity.TOP })

        // 门层：Logo 圆盘
        gate = FrameLayout(this)
        content.addView(gate, FrameLayout.LayoutParams(MP, MP))
        val gateInner = FrameLayout(this)
        gate.addView(gateInner, FrameLayout.LayoutParams(WC, WC).apply { gravity = Gravity.CENTER })
        disc = FrameLayout(this).apply {
            background = Ui.oval(Color.WHITE)
            elevation = dpF(this@LaunchActivity, 16f * s)
        }
        gateInner.addView(disc, FrameLayout.LayoutParams(discSize, discSize))
        val logoSize = (discSize * 0.94f).toInt()
        disc.addView(ImageView(this).apply { setImageResource(R.drawable.ic_logo_mark) },
            FrameLayout.LayoutParams(logoSize, logoSize).apply { gravity = Gravity.CENTER })
        discLabel = TextView(this).apply {
            text = "UNIHUB"
            letterSpacing = 0.42f
            mono(9.5f * s)
            setTextColor("#5CFFFFFF".toColorInt())
            alpha = 0f
        }
        gateInner.addView(discLabel, FrameLayout.LayoutParams(WC, WC).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = discSize + dpF(this@LaunchActivity, 26f * s).toInt()
        })

        setContentView(screen)
        screen.setOnClickListener { if (!done) skip() }
    }

    // ------------------------------------------------------------- sequence

    private fun at(delayMs: Long, block: () -> Unit) {
        val r = Runnable { if (!leaving) block() }
        timers.add(r)
        handler.postDelayed(r, delayMs)
    }

    /** 亮色：黑底 Logo → 白盘铺满 → 墨色 + Hello */
    private fun sequence() {
        hello.translationY = resources.displayMetrics.heightPixels / 2f - hello.height / 2f
        hello.alpha = 0f
        hello.scaleX = 0.94f
        hello.scaleY = 0.94f

        at(140) { breathe() }
        at(790) {
            openDoor()
            at(570) { ink.reveal(true) }
            at(730) {
                hello.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setInterpolator(PathInterpolator(0.22f, 0.9f, 0.28f, 1f))
                    .setDuration(500).start()
            }
            at(1240) { placeHelloTop(true) }
            at(2000) { route() }
        }
    }

    /** 暗色：不铺白，Logo 呼吸后淡出，墨色与 Hello 直接在暗底上浮现 */
    private fun prepareDark() {
        screen.setBackgroundColor(Pal.bg(this))
        backdrop.alpha = 0f
        hello.setTextColor(Pal.ink(this))
        statusIconsDark(false)
        gate.alpha = 0f
        disc.alpha = 0f
        hello.alpha = 0f
        hello.scaleX = 0.94f
        hello.scaleY = 0.94f
        hello.translationY = resources.displayMetrics.heightPixels / 2f - hello.height / 2f
    }

    private fun darkSequence() {
        gate.alpha = 1f
        disc.alpha = 1f
        breathe()

        at(760) {
            ink.reveal(true)
            disc.animate().alpha(0f).scaleX(1.14f).scaleY(1.14f).setDuration(460)
                .setInterpolator(DecelerateInterpolator()).start()
            discLabel.animate().alpha(0f).setDuration(260).start()
            hello.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520)
                .setInterpolator(PathInterpolator(0.22f, 0.9f, 0.28f, 1f)).start()
        }
        at(1180) { placeHelloTop(true) }
        at(1980) { route() }
    }

    private fun breathe() {
        discLabel.animate().alpha(1f).setDuration(400).start()
        disc.animate().scaleX(1.075f).scaleY(1.075f).setDuration(290)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                disc.animate().scaleX(1f).scaleY(1f).setDuration(310)
                    .setInterpolator(DecelerateInterpolator()).start()
            }.start()
    }

    /** 白盘从 Logo 圆心放大铺满（先慢后快） */
    private fun openDoor() {
        disc.animate().scaleX(2.3f).scaleY(2.3f).alpha(0f).setDuration(700)
            .setInterpolator(PathInterpolator(0.42f, 0f, 0.66f, 1f)).withEndAction { disc.visibility = View.INVISIBLE }.start()
        discLabel.animate().alpha(0f).setDuration(300).start()

        door.alpha = 1f
        val cubic = android.view.animation.Interpolator { t -> t * t * t }
        val ax = ObjectAnimator.ofFloat(door, "scaleX", 1f, 18f).apply { duration = 700; interpolator = cubic }
        val ay = ObjectAnimator.ofFloat(door, "scaleY", 1f, 18f).apply { duration = 700; interpolator = cubic }
        ax.start(); ay.start()

        // 白页铺开后：状态栏转黑字，底色换白
        screen.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            statusIconsDark(true)
            screen.setBackgroundColor(Pal.bg(this@LaunchActivity))
        }, 420)
    }

    /**
     * Hello 上浮到顶部并缩到与登录页标题一致的大小。
     * 两屏用同一组字号 / 位置，转场时标题不会跳动。
     */
    private fun placeHelloTop(animate: Boolean) {
        val targetY = dpF(this, 30f * s) - hello.height * 0.152f
        if (!animate) {
            hello.scaleX = HELLO_SCALE
            hello.scaleY = HELLO_SCALE
            hello.translationY = targetY
            return
        }
        hello.animate().scaleX(HELLO_SCALE).scaleY(HELLO_SCALE).translationY(targetY)
            .setInterpolator(PathInterpolator(0.22f, 0.9f, 0.28f, 1f))
            .setDuration(680).start()
    }

    /** 点击跳过：直接把画面定格在终点，稍作停顿再分流 */
    private fun skip() {
        timers.forEach { handler.removeCallbacks(it) }
        done = true
        screen.setBackgroundColor(Pal.bg(this))
        statusIconsDark(!ThemePrefs.isDark(this))
        gate.alpha = 0f
        door.alpha = 0f
        disc.animate().cancel()
        ink.reveal(false)
        ink.alpha = 1f
        hello.alpha = 1f
        placeHelloTop(false)
        handler.postDelayed({ route() }, 160)
    }

    /** 按登录态分流 */
    private fun route() {
        if (leaving) return
        leaving = true
        done = true
        val next = if (AuthStore.isLoggedIn(this)) MainActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, next))
        overridePendingTransition(R.anim.route_in, R.anim.route_out)
        // 淡入期间保留开屏窗口，立刻 finish 会先露出白底一帧
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                finish()
                overridePendingTransition(0, 0)
            }
        }, 360)
    }

    /** true = 深色图标（白底用）；false = 浅色图标（深底用） */
    private fun statusIconsDark(dark: Boolean) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = dark
        controller.isAppearanceLightNavigationBars = dark
    }

    override fun onDestroy() {
        timers.forEach { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}

/** Hello 从 92sp 缩到 64sp，与登录页标题对齐 */
private const val HELLO_SCALE = 64f / 92f

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
