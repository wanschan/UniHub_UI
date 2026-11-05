package com.example.applicationoneapp.ui.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.widget.PageShell
import com.example.applicationoneapp.ui.widget.buildTodayHead
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdpI
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.R

/** 课程表：左侧节次时间轴 + 7 天网格 */
@SuppressLint("ViewConstructor")
class TimetablePage(
    context: Context,
    private val toast: (String) -> Unit,
) : PageShell(context) {

    private data class Course(
        val col: Int,
        val row: Int,
        val span: Int,
        val name: String,
        val place: String,
        val bg: Int,
        val fg: Int,
    )

    private val courses = listOf(
        Course(1, 3, 2, "创新创业教育", "本部思源楼-335", R.color.tint_blue_bg, R.color.tint_blue_fg),
        Course(3, 3, 2, "综合英语IV", "本部思源楼-236", R.color.tint_purple_bg, R.color.tint_purple_fg),
        Course(4, 3, 2, "网络安全技术与实践", "本部第二实验楼仿真实验", R.color.tint_sand_bg, R.color.tint_sand_fg),
        Course(1, 5, 2, "Web程序设计(C)", "本部计算机基础实验室-3(5)", R.color.tint_clay_bg, R.color.tint_clay_fg),
        Course(4, 5, 2, "计算机组成原理", "本部第二实验楼-402-403计", R.color.tint_sage_bg, R.color.tint_sage_fg),
        Course(2, 7, 2, "计算机组成原理", "本部第二实验楼-402-403计", R.color.tint_sage_bg, R.color.tint_sage_fg),
        Course(3, 7, 2, "毛泽东思想和中国特色社会主义理论体系概论", "本部", R.color.tint_rose_bg, R.color.tint_rose_fg),
        Course(4, 7, 2, "Web程序设计(C)", "本部计算机基础实验室-3(5)", R.color.tint_clay_bg, R.color.tint_clay_fg),
    )

    private val cellH: Int get() = context.sdpI(64f)

    init {
        addHead(buildTodayHead(context, "Today", "June 22，2026"))
        setBodyPadding(10f, bottom = 24f, top = 8f)
        buildBody()
    }

    private fun buildBody() {
        val ctx = context
        val root = bodyContent
        root.setPadding(0, ctx.sdpI(14f), 0, 0)

        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        val nums = listOf(15, 16, 17, 18, 19, 20, 21)
        val today = 4

        // ---- 表头
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, ctx.sdpI(8f))
        }
        root.addView(header, LinearLayout.LayoutParams(MP, WC))

        val timeColW = ctx.sdpI(40f)
        header.addView(TextView(ctx).apply {
            text = "6\n月"
            mono(12f)
            gravity = Gravity.CENTER
            setTextColor(Ui.color(ctx, R.color.ink_3))
            setLineSpacing(0f, 1.05f)
            setPadding(0, 0, 0, ctx.sdpI(4f))
        }, LinearLayout.LayoutParams(timeColW, WC))

        weekdays.forEachIndexed { i, wd ->
            val isToday = i == today
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            col.addView(TextView(ctx).apply {
                text = wd
                mono(12f)
                if (isToday) typeface = Ui.MONO_BOLD
                setTextColor(if (isToday) Pal.ink(ctx) else Ui.color(ctx, R.color.ink_3))
            })
            col.addView(TextView(ctx).apply {
                text = nums[i].toString()
                mono(14f)
                if (isToday) typeface = Ui.MONO_BOLD
                setTextColor(if (isToday) Pal.ink(ctx) else Ui.color(ctx, R.color.ink_2))
                setPadding(0, ctx.sdpI(2f), 0, ctx.sdpI(4f))
            })
            header.addView(col, LinearLayout.LayoutParams(0, WC, 1f))
        }

        // ---- 时间轴 + 网格
        val bodyRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(bodyRow, LinearLayout.LayoutParams(MP, WC))

        val times = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        bodyRow.addView(times, LinearLayout.LayoutParams(timeColW, WC))
        val slots = listOf(
            1 to "08:00\n08:50", 2 to "09:00\n09:50", 3 to "10:10\n11:00", 4 to "11:10\n12:00",
            5 to "14:00\n14:50", 6 to "15:00\n15:50", 7 to "16:10\n17:00", 8 to "17:10\n18:00",
        )
        slots.forEach { (index, range) ->
            times.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(TextView(ctx).apply {
                    text = index.toString()
                    mono(13f)
                    setTextColor(Ui.color(ctx, R.color.ink_2))
                })
                addView(TextView(ctx).apply {
                    text = range
                    mono(9.5f)
                    setTextColor(Ui.color(ctx, R.color.ink_4))
                    gravity = Gravity.CENTER
                    setLineSpacing(0f, 1.05f)
                })
            }, LinearLayout.LayoutParams(MP, cellH))
        }

        val gridWrap = FrameLayout(ctx)
        bodyRow.addView(gridWrap, LinearLayout.LayoutParams(0, WC, 1f))

        val gridCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        gridWrap.addView(gridCol, FrameLayout.LayoutParams(MP, WC))
        val lineColor = if (ThemePrefs.isDark(ctx)) Ui.color(ctx, R.color.line) else 0x80E6E7EA.toInt()
        repeat(8) {
            gridCol.addView(GridRow(ctx, lineColor), LinearLayout.LayoutParams(MP, cellH))
        }

        gridWrap.post {
            val cellW = gridWrap.width / 7f
            courses.forEach { course ->
                val card = courseCard(course)
                val lp = FrameLayout.LayoutParams(
                    (cellW - ctx.sdpI(3f)).toInt(),
                    (cellH * course.span - ctx.sdpI(3f)).toInt(),
                )
                lp.leftMargin = ((course.col - 1) * cellW).toInt() + ctx.sdpI(1.5f)
                lp.topMargin = (course.row - 1) * cellH + ctx.sdpI(1.5f)
                gridWrap.addView(card, lp)
            }
        }
    }

    private fun courseCard(course: Course): View {
        val ctx = context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(ctx.sdpI(2f), ctx.sdpI(5f), ctx.sdpI(2f), ctx.sdpI(5f))
            background = Ui.rounded(ctx, Ui.color(ctx, course.bg), 9f)
            isClickable = true
            setOnClickListener { toast("${course.name} · ${course.place}") }
            pressFeedback(0.97f)
        }
        card.addView(TextView(ctx).apply {
            text = course.name
            textSize = 10f
            setTextColor(Ui.color(ctx, course.fg))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 5
            setLineSpacing(0f, 1.26f)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(WC, 0, 1f).apply { gravity = Gravity.CENTER })
        card.addView(TextView(ctx).apply {
            text = "★"
            textSize = 8f
            setTextColor(Ui.color(ctx, course.fg))
            alpha = 0.6f
            gravity = Gravity.CENTER
            setPadding(0, ctx.sdpI(2f), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = course.place
            textSize = 9f
            setTextColor(Ui.color(ctx, course.fg))
            alpha = 0.78f
            gravity = Gravity.CENTER
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        return card
    }

    private class GridRow(context: Context, private val lineColor: Int) : LinearLayout(context) {
        private val paint = Paint().apply { strokeWidth = 1f; color = lineColor }

        init {
            setWillNotDraw(false)
            orientation = HORIZONTAL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = lineColor
            canvas.drawLine(0f, height - 1f, width.toFloat(), height - 1f, paint)
        }
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
