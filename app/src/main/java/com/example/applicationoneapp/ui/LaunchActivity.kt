package com.example.applicationoneapp.ui

import android.annotation.SuppressLint
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.applicationoneapp.MainActivity
import com.example.applicationoneapp.R
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.common.Ui.dp
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui.mono

/**
 * 开屏 + 登录引导页。
 * 深灰底白色圆盘 Logo 呼吸一次 → 白盘从圆心放大铺满 → 墨色晕染浮现 + Hello 上浮 → 显示出两个入口。
 * 点击屏幕任意处可跳过动画。
 */
@SuppressLint("CustomSplashScreen")
class LaunchActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val timers = mutableListOf<Runnable>()
    private var toastHide: Runnable? = null

    private lateinit var screen: FrameLayout
    private lateinit var disc: FrameLayout
    private lateinit var discLabel: TextView
    private lateinit var door: View
    private lateinit var gate: FrameLayout
    private lateinit var ink: InkBlobs
    private lateinit var hello: TextView
    private lateinit var actions: LinearLayout

    private var done = false
    private var leaving = false
    private var slide: ValueAnimator? = null

    private val s: Float get() = resources.displayMetrics.widthPixels / dpF(this, 392f)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.apply(this)
        super.onCreate(savedInstanceState)
        buildUi()
        val openingOut = PathInterpolator(0.22f, 0.9f, 0.28f, 1f)
        if (ThemePrefs.isDark(this)) {
            // 暗色主题下跳过深灰开屏，直接进入登录
            screen.setBackgroundColor(Pal.bg(this))
            hello.setTextColor(Pal.ink(this))
            setDarkStatusIcons(false)
            gate.alpha = 0f
            disc.alpha = 0f
            screen.post {
                ink.reveal(false)
                hello.alpha = 1f
                placeHelloTop(false)
                actions.alpha = 1f
                actions.translationY = 0f
                done = true
            }
        } else {
            screen.setBackgroundColor(Color.BLACK)
            screen.post { sequence(openingOut) }
        }
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi() {
        screen = FrameLayout(this)

        // 深灰底 + 中心径向高光
        screen.addView(View(this).apply {
            background = "#15161A".toColorInt().toDrawable()
        }, FrameLayout.LayoutParams(MP, MP))
        screen.addView(View(this).apply {
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
        val login = FrameLayout(this)
        screen.addView(login, FrameLayout.LayoutParams(MP, MP))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(screen) { _, insets ->
            val sys = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            login.setPadding(0, sys.top, 0, sys.bottom)
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
        login.addView(hello, FrameLayout.LayoutParams(MP, WC).apply {
            gravity = Gravity.TOP
        })

        actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = 0f
            translationY = dpF(this@LaunchActivity, 16f * s)
        }
        login.addView(actions, FrameLayout.LayoutParams(MP, WC).apply {
            gravity = Gravity.BOTTOM
            leftMargin = dpF(this@LaunchActivity, 22f * s).toInt()
            rightMargin = dpF(this@LaunchActivity, 22f * s).toInt()
            bottomMargin = dpF(this@LaunchActivity, 30f * s).toInt()
        })
        actions.addView(makeButton("登录", true), LinearLayout.LayoutParams(MP, dpF(this, 54f * s).toInt()).apply {
            bottomMargin = dpF(this@LaunchActivity, 11f * s).toInt()
        })
        actions.addView(makeButton("创建账户", false), LinearLayout.LayoutParams(MP, dpF(this, 54f * s).toInt()))

        val legal = TextView(this).apply {
            text = "继续即代表同意 《用户协议》 与 《隐私政策》"
            gravity = Gravity.CENTER
            mono(11f * s)
            setTextColor("#8B8F96".toColorInt())
            setPadding(0, dpF(this@LaunchActivity, 15f * s).toInt(), 0, 0)
        }
        actions.addView(legal, LinearLayout.LayoutParams(MP, WC))
        legal.setOnClickListener { showToast("《用户协议》与《隐私政策》（Demo）") }

        // 门层：Logo 圆盘
        gate = FrameLayout(this)
        login.addView(gate, FrameLayout.LayoutParams(MP, MP))
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
            text = "CAMPUS LINK"
            letterSpacing = 0.42f
            mono(9.5f * s)
            setTextColor("#5CFFFFFF".toColorInt())
            alpha = 0f
        }
        gateInner.addView(discLabel, FrameLayout.LayoutParams(WC, WC).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = discSize + dpF(this@LaunchActivity, 26f * s).toInt()
        })


        val toast = TextView(this).apply {
            tag = "toast"
            setTextColor(Color.WHITE)
            textSize = 13.5f * s
            setPadding(dp(this@LaunchActivity, 18f), dp(this@LaunchActivity, 11f), dp(this@LaunchActivity, 18f), dp(this@LaunchActivity, 11f))
            background = Ui.rounded(this@LaunchActivity, "#F0181A1E".toColorInt(), 999f)
            alpha = 0f
        }
        screen.addView(toast, FrameLayout.LayoutParams(WC, WC).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(this@LaunchActivity, 118f)
        })

        setContentView(screen)
        screen.setOnClickListener { if (!done) finishInstantly() }
    }

    @SuppressLint("ClickableViewAccessibility") // ACTION_UP 里已显式 performClick
    private fun makeButton(label: String, primary: Boolean): TextView {
        val tv = TextView(this)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.textSize = 16.5f * s
        tv.isClickable = true
        val night = ThemePrefs.isDark(this)
        if (primary) {
            // 亮色：墨黑实心；暗色：反转为浅色实心
            val bg = if (night) "#F4F5F7".toColorInt() else "#111214".toColorInt()
            tv.setTextColor(if (night) "#0D0F13".toColorInt() else Color.WHITE)
            tv.background = Ui.rounded(this, bg, 15f * s, 1.6f * s, bg)
            tv.setOnClickListener { enterApp() }
        } else {
            tv.setTextColor(if (night) "#F4F5F7".toColorInt() else "#111214".toColorInt())
            tv.background = Ui.rounded(
                this,
                if (night) Color.TRANSPARENT else Color.WHITE,
                15f * s, 1.6f * s,
                if (night) "#454B54".toColorInt() else "#111214".toColorInt(),
            )
            tv.setOnClickListener { showToast("创建账户流程（Demo）") }
        }
        tv.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start(); true }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(140).start(); true }
                else -> false
            }
        }
        return tv
    }

    // ------------------------------------------------------------- sequence

    private fun at(delayMs: Long, block: () -> Unit) {
        val r = Runnable { if (!leaving) block() }
        timers.add(r)
        handler.postDelayed(r, delayMs)
    }

    private fun sequence(open: PathInterpolator) {
        // 初始位置：Hello 居中偏下、模糊
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
                    .setInterpolator(open).setDuration(500).start()
            }
            at(1240) { placeHelloTop(true) }
            at(1520) {
                actions.animate().alpha(1f).translationY(0f).setInterpolator(open).setDuration(520).start()
                done = true
            }
        }
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
        setDarkStatusIcons(false)
        screen.postDelayed({ screen.setBackgroundColor(Color.WHITE) }, 420)
    }

    private fun placeHelloTop(animate: Boolean) {
        val targetY = dpF(this, 92f * s) - hello.height / 2f
        if (!animate) {
            hello.alpha = 1f
            hello.scaleX = 0.46f
            hello.scaleY = 0.46f
            hello.translationY = targetY
            return
        }
        hello.animate().scaleX(0.46f).scaleY(0.46f).translationY(targetY)
            .setInterpolator(PathInterpolator(0.22f, 0.9f, 0.28f, 1f))
            .setDuration(680).start()
    }

    private fun finishInstantly() {
        timers.forEach { handler.removeCallbacks(it) }
        screen.setBackgroundColor(Color.WHITE)
        setDarkStatusIcons(false)
        gate.alpha = 0f
        door.alpha = 0f
        disc.animate().cancel()
        ink.reveal(false)
        hello.alpha = 1f
        placeHelloTop(false)
        actions.alpha = 1f
        actions.translationY = 0f
        done = true
    }

    private fun enterApp() {
        if (leaving) return
        leaving = true
        screen.animate().scaleX(1.06f).scaleY(1.06f).alpha(0f).setDuration(360)
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.start()
    }

    private fun setDarkStatusIcons(dark: Boolean) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = dark
        controller.isAppearanceLightNavigationBars = dark
    }

    private fun showToast(msg: String) {
        val toast = screen.findViewWithTag<TextView>("toast") ?: return
        toast.text = msg
        toast.animate().alpha(1f).setDuration(180).start()
        toastHide?.let { handler.removeCallbacks(it) }
        val hide = Runnable { toast.animate().alpha(0f).setDuration(220).start() }
        toastHide = hide
        handler.postDelayed(hide, 1800)
    }

    override fun onDestroy() {
        timers.forEach { handler.removeCallbacks(it) }
        toastHide?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    /** 五颜六色的黑：四个大色斑以正片叠底缓慢游移 */
    private class InkBlobs(context: Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val baseColors = intArrayOf(0xFF160E34.toInt(), 0xFF38082C.toInt(), 0xFF042628.toInt(), 0xFF3E1606.toInt())

        // 几何占比常量：圆心贴四角、半径按屏宽，保证大屏也能吃到边缘
        private val cxRatio = floatArrayOf(-0.02f, 1.02f, 0.98f, 0.02f)
        private val cyRatio = floatArrayOf(-0.03f, -0.01f, 1.01f, 0.99f)
        private val radiusRatio = floatArrayOf(0.50f, 0.46f, 0.54f, 0.44f)
        private val driftX = floatArrayOf(0.06f, -0.05f, -0.04f, 0.05f)
        private val driftY = floatArrayOf(0.05f, 0.06f, -0.05f, -0.04f)
        private val period = floatArrayOf(27f, 32f, 24f, 30f)

        // Shader 只跟控件尺寸有关：尺寸不变就复用，绘制时用 Matrix 做漂移
        private val shaders = arrayOfNulls<RadialGradient>(4)
        private val matrix = Matrix()
        private var shaderW = 0
        private var shaderH = 0

        private var shown = 0f
        private var t = 0f

        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30_000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                t = (it.animatedValue as Float) * 30_000f
                invalidate()
            }
        }

        init {
            setWillNotDraw(false)
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        fun reveal(animate: Boolean) {
            if (!animate) {
                shown = 1f
                invalidate()
                return
            }
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                addUpdateListener { shown = it.animatedValue as Float; invalidate() }
                start()
            }
            animator.start()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0 || shown <= 0f) return

            ensureShaders(width, height)
            // 渐隐交给 paint.alpha，避免每帧重建 Shader
            paint.xfermode = null
            paint.alpha = (255 * shown).coerceIn(0f, 255f).toInt()

            for (i in 0 until 4) {
                val shader = shaders[i] ?: continue
                val phase = ((t / 1000f) / period[i] * (Math.PI * 2)).toFloat()
                val dx = kotlin.math.sin(phase) * driftX[i] * w
                val dy = kotlin.math.cos(phase * 0.8f) * driftY[i] * h
                val scale = 1f + 0.06f * kotlin.math.sin(phase * 0.7f)
                val cx = cxRatio[i] * w
                val cy = cyRatio[i] * h

                matrix.setScale(scale, scale, cx, cy)
                matrix.postTranslate(dx, dy)
                shader.setLocalMatrix(matrix)

                paint.shader = shader
                canvas.drawCircle(cx + dx, cy + dy, radiusRatio[i] * w * scale, paint)
            }
            paint.alpha = 255
        }

        /** Shader 按尺寸缓存；尺寸变化（旋转/分屏）时才重建 */
        private fun ensureShaders(w: Int, h: Int) {
            if (shaderW == w && shaderH == h && shaders[0] != null) return
            shaderW = w
            shaderH = h
            val fw = w.toFloat()
            val fh = h.toFloat()
            for (i in 0 until 4) {
                val c = baseColors[i]
                shaders[i] = RadialGradient(
                    cxRatio[i] * fw, cyRatio[i] * fh, radiusRatio[i] * fw,
                    intArrayOf(withAlpha(c, 0.50f), withAlpha(c, 0.22f), withAlpha(c, 0f)),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
        }

        private fun withAlpha(color: Int, a: Float): Int {
            val alpha = (255 * a).coerceIn(0f, 255f).toInt()
            return (alpha shl 24) or (color and 0x00FFFFFF)
        }
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
