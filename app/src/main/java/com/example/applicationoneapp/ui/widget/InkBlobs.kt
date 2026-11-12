package com.example.applicationoneapp.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/**
 * 五颜六色的黑：四个大色斑以正片叠底缓慢游移。
 *
 * 开场页与登录页共用，保证两屏衔接时背景连续。
 * 注意：色斑以软件层绘制（径向渐变 + Matrix 漂移），单屏使用没有问题。
 */
class InkBlobs(context: Context) : View(context) {

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

    private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 30_000
        repeatCount = android.animation.ValueAnimator.INFINITE
        addUpdateListener {
            t = (it.animatedValue as Float) * 30_000f
            invalidate()
        }
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /** [animate] = false 时直接定格在完整状态（登录页从静止态继续） */
    fun reveal(animate: Boolean) {
        if (!animate) {
            shown = 1f
            invalidate()
            return
        }
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
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
