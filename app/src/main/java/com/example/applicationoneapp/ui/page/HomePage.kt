package com.example.applicationoneapp.ui.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.widget.PageShell
import com.example.applicationoneapp.ui.widget.buildTodayHead
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdp
import com.example.applicationoneapp.ui.widget.sdpI
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.common.Ui.dp
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.R

/** 首页：快捷启动 / NFC 前往 / 扫码前往 */
@SuppressLint("ViewConstructor")
class HomePage(
    context: Context,
    private val toast: (String) -> Unit,
    private val onOpenTimetable: () -> Unit,
    private val onGetPlugins: () -> Unit,
    private val onMenu: () -> Unit,
) : PageShell(context) {

    private val segItems = mutableListOf<TextView>()
    private val panels = mutableListOf<View>()

    init {
        addHead(buildTodayHead(context, "Today", "June 22，2026"))
        setBodyPadding(20f, bottom = 28f, top = 8f)
        buildBody()
    }

    private val dark get() = ThemePrefs.isDark(context)

    private fun buildBody() {
        val ctx = context
        val root = bodyContent

        // ---- 分段标题：三横线菜单 + 三个分栏
        val seg = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(16f), 0, 0)
        }
        root.addView(seg, LinearLayout.LayoutParams(MP, WC))

        val menu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.sdpI(8f), ctx.sdpI(24f), ctx.sdpI(8f))
            addView(View(ctx).apply { setBackgroundColor(Pal.ink(ctx)) }, LinearLayout.LayoutParams(Ui.dp(ctx, 19f), Ui.dp(ctx, 2f)))
            addView(View(ctx).apply { setBackgroundColor(Pal.ink(ctx)) }, LinearLayout.LayoutParams(Ui.dp(ctx, 13f), Ui.dp(ctx, 2f)).apply { topMargin = Ui.dp(ctx, 4f) })
            setOnClickListener { onMenu() }
            pressFeedback(0.92f)
        }
        seg.addView(menu, LinearLayout.LayoutParams(WC, WC))

        listOf("快捷启动", "NFC前往", "扫码前往").forEachIndexed { index, label ->
            val tv = TextView(ctx).apply {
                text = label
                textSize = 16f
                maxLines = 1
                isClickable = true
                setPadding(0, 0, ctx.sdpI(22f), ctx.sdpI(8f))
                setOnClickListener { switchPanel(index) }
            }
            seg.addView(tv, LinearLayout.LayoutParams(WC, WC))
            segItems.add(tv)
        }

        // ---- 面板
        val panelsBox = FrameLayout(ctx)
        root.addView(panelsBox, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(18f) })
        panels.add(buildQuickPanel())
        panels.add(buildNfcPanel())
        panels.add(buildScanPanel())
        panels.forEachIndexed { i, v ->
            panelsBox.addView(v, FrameLayout.LayoutParams(MP, WC))
            v.visibility = if (i == 0) View.VISIBLE else View.GONE
        }
        switchPanel(0)
    }

    private fun switchPanel(index: Int) {
        segItems.forEachIndexed { i, tv ->
            val on = i == index
            tv.setTextColor(if (on) Pal.ink(context) else Ui.color(context, R.color.ink_4))
            tv.typeface = Typeface.create("sans-serif", if (on) Typeface.BOLD else Typeface.NORMAL)
            tv.background = if (on) underline() else null
        }
        panels.forEachIndexed { i, v ->
            v.visibility = if (i == index) View.VISIBLE else View.GONE
            if (i == index) {
                v.animate().cancel()
                v.alpha = 0f
                v.translationY = context.sdp(8f).toInt().toFloat()
                v.animate().alpha(1f).translationY(0f).setDuration(320)
                    .setInterpolator(android.view.animation.PathInterpolator(0.22f, 0.9f, 0.28f, 1f)).start()
            }
        }
    }

    /** 选中项底部 2dp 描边（下划线） */
    private fun underline(): GradientDrawable {
        val ctx = context
        return object : GradientDrawable() {
            init {
                setColor(Color.TRANSPARENT)
                cornerRadius = 0f
                shape = RECTANGLE
            }

            override fun draw(canvas: Canvas) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Pal.ink(ctx)
                    strokeWidth = Ui.dpF(ctx, 2f)
                }
                val y = bounds.bottom - paint.strokeWidth / 2f
                canvas.drawLine(bounds.left.toFloat(), y, bounds.right.toFloat(), y, paint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        }
    }

    // ------------------------------------------------------------- 快捷启动

    private fun buildQuickPanel(): View {
        val ctx = context
        val panel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        panel.addView(row, LinearLayout.LayoutParams(MP, WC))

        val courseIcon = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFFF0453B.toInt(), 0xFFC81D18.toInt()),
        ).apply { cornerRadius = Ui.dpF(ctx, 9f) }

        row.addView(
            pluginCard("课程", R.drawable.ic_course, courseIcon) { onOpenTimetable() },
            LinearLayout.LayoutParams(0, WC, 1f).apply { rightMargin = ctx.sdpI(7f) },
        )
        row.addView(
            pluginCard("饮水", R.drawable.ic_water, Ui.rounded(ctx, Color.WHITE, 9f)) {
                toast("已打开饮水服务 · 本次消费 -0.21 元")
            },
            LinearLayout.LayoutParams(0, WC, 1f).apply { leftMargin = ctx.sdpI(7f) },
        )

        val add = TextView(ctx).apply {
            text = "获取插件>"
            textSize = 16f
            setTextColor(Ui.color(ctx, R.color.ink_4))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.sdpI(17f), 0, 0, 0)
            background = Ui.rounded(ctx, if (dark) Ui.color(ctx, R.color.card_2) else Ui.color(ctx, R.color.card), 16f)
            setOnClickListener { onGetPlugins() }
            pressFeedback(0.97f)
        }
        // 原型里「获取插件」是网格第二行的一个格子（半宽），不是整条
        val row2 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        panel.addView(row2, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(14f) })
        row2.addView(add, LinearLayout.LayoutParams(0, ctx.sdpI(60f), 1f).apply { rightMargin = ctx.sdpI(7f) })
        row2.addView(View(ctx), LinearLayout.LayoutParams(0, ctx.sdpI(60f), 1f))
        return panel
    }

    private fun pluginCard(
        title: String,
        iconRes: Int,
        iconBg: android.graphics.drawable.Drawable,
        onClick: () -> Unit,
    ): View {
        val ctx = context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.sdpI(12f), ctx.sdpI(11f), ctx.sdpI(12f), ctx.sdpI(11f))
            background = if (dark) {
                Ui.rounded(ctx, Ui.color(ctx, R.color.card), 16f, 1f, Ui.color(ctx, R.color.hairline_2))
            } else {
                Ui.rounded(ctx, Color.WHITE, 16f)
            }
            elevation = if (dark) 0f else Ui.dpF(ctx, 2.5f)
            isClickable = true
            setOnClickListener { onClick() }
            pressFeedback(0.965f)
        }
        val iconBox = FrameLayout(ctx).apply { background = iconBg }
        iconBox.addView(ImageView(ctx).apply { setImageResource(iconRes) },
            FrameLayout.LayoutParams(WC, WC).apply { gravity = Gravity.CENTER })
        card.addView(iconBox, LinearLayout.LayoutParams(ctx.sdpI(38f), ctx.sdpI(38f)))
        card.addView(TextView(ctx).apply {
            text = title
            textSize = 17f
            setTextColor(Pal.ink(ctx))
            setPadding(ctx.sdpI(11f), 0, 0, 0)
        }, LinearLayout.LayoutParams(WC, WC))
        return card
    }

    // ------------------------------------------------------------- NFC

    private fun buildNfcPanel(): View {
        val ctx = context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(20f), ctx.sdpI(20f), ctx.sdpI(20f), ctx.sdpI(20f))
            background = Ui.rounded(ctx, Pal.card(ctx), 18f)
        }

        val pulseBox = FrameLayout(ctx).apply {
            background = Ui.oval(if (dark) Ui.color(ctx, R.color.card_2) else Color.WHITE)
        }
        pulseBox.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_nfc) },
            FrameLayout.LayoutParams(WC, WC).apply { gravity = Gravity.CENTER })
        card.addView(pulseBox, LinearLayout.LayoutParams(ctx.sdpI(64f), ctx.sdpI(64f)))

        card.addView(TextView(ctx).apply {
            text = "靠近读卡器即可通行"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Pal.ink(ctx))
            setPadding(0, ctx.sdpI(16f), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = "保持屏幕点亮，将手机背面贴近校园终端"
            mono(13f)
            letterSpacing = 0.02f
            setTextColor(Ui.color(ctx, R.color.ink_3))
            setPadding(0, ctx.sdpI(6f), 0, 0)
            setLineSpacing(0f, 1.6f)
        })

        val rows = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        card.addView(rows, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(12f) })
        listOf("宿舍门禁" to "已绑定", "图书馆闸机" to "已绑定", "食堂刷卡机" to "可用")
            .forEachIndexed { i, (name, value) ->
                if (i > 0) rows.addView(divider())
                rows.addView(infoRow(name, value))
            }
        return card
    }

    // ------------------------------------------------------------- 扫码

    private fun buildScanPanel(): View {
        val ctx = context
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val scan = FrameLayout(ctx).apply { background = Ui.rounded(ctx, Pal.card(ctx), 18f) }
        box.addView(scan, LinearLayout.LayoutParams(MP, ctx.sdpI(210f)))
        val size = ctx.sdpI(150f)
        scan.addView(ScanBracket(ctx), FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER })

        box.addView(TextView(ctx).apply {
            text = "对准场景二维码，自动打开对应服务"
            mono(13f)
            setTextColor(Ui.color(ctx, R.color.ink_3))
            gravity = Gravity.CENTER
            setPadding(0, ctx.sdpI(14f), 0, 0)
        })

        val rows = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(rows, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(12f) })
        listOf("教学楼 A 座 · 空教室查询" to "2 分钟前", "第三食堂 · 套餐下单" to "今天 12:04")
            .forEachIndexed { i, (name, value) ->
                if (i > 0) rows.addView(divider())
                rows.addView(infoRow(name, value))
            }
        return box
    }

    private fun divider(): View = View(context).apply {
        setBackgroundColor(Ui.color(context, R.color.hairline_2))
        layoutParams = LinearLayout.LayoutParams(MP, Ui.dp(context, 1f))
    }

    private fun infoRow(name: String, value: String): View {
        val ctx = context
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(13f), 0, ctx.sdpI(13f))
            addView(TextView(ctx).apply {
                text = name
                textSize = 15f
                setTextColor(Pal.ink(ctx))
            }, LinearLayout.LayoutParams(0, WC, 1f))
            addView(TextView(ctx).apply {
                text = value
                mono(13f)
                setTextColor(Ui.color(ctx, R.color.ink_3))
            })
        }
    }

    /** 扫码取景框：四角 L 形 + 上下往复的扫描线 */
    private class ScanBracket(context: Context) : View(context) {

        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = Ui.dpF(context, 2.5f)
        }
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeWidth = Ui.dpF(context, 2f)
        }
        private var phase = 0f

        private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
        }

        init {
            setWillNotDraw(false)
            animator.start()
        }

        override fun onDetachedFromWindow() {
            animator.cancel()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            stroke.color = Ui.color(context, R.color.ink)
            line.color = Ui.color(context, R.color.ink)
            val len = width * 0.17f
            val p = stroke.strokeWidth / 2f
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawLine(p, p, p, p + len, stroke)
            canvas.drawLine(p, p, p + len, p, stroke)
            canvas.drawLine(w - p, p, w - p, p + len, stroke)
            canvas.drawLine(w - p, p, w - p - len, p, stroke)
            canvas.drawLine(p, h - p, p, h - p - len, stroke)
            canvas.drawLine(p, h - p, p + len, h - p, stroke)
            canvas.drawLine(w - p, h - p, w - p, h - p - len, stroke)
            canvas.drawLine(w - p, h - p, w - p - len, h - p, stroke)

            val y = h * (0.14f + 0.70f * phase)
            line.alpha = 220
            canvas.drawLine(w * 0.10f, y, w * 0.90f, y, line)
            line.alpha = 70
            val gap = Ui.dpF(context, 4f)
            canvas.drawLine(w * 0.10f, y - gap, w * 0.90f, y - gap, line)
        }
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
