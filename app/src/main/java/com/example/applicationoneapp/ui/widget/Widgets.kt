package com.example.applicationoneapp.ui.widget

import android.annotation.SuppressLint
import androidx.appcompat.widget.AppCompatTextView
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.applicationoneapp.R
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.common.Ui.dp
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui.mono
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** 全局缩放系数（相对 392dp 设计稿） */
fun Context.uiScale(): Float = resources.displayMetrics.widthPixels / dpF(this, 392f)

fun Context.sdp(v: Float): Float = dpF(this, v * uiScale())

fun Context.sdpI(v: Float): Int = (dpF(this, v * uiScale()) + 0.5f).toInt()

/** 轻触缩放反馈（返回 false，点击事件仍交给 View 自身处理） */
@SuppressLint("ClickableViewAccessibility")
fun View.pressFeedback(scale: Float = 0.965f) {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(scale).scaleY(scale).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
        false
    }
}

// --------------------------------------------------------------------- 页面骨架

/**
 * 每页拆两层：head 固定标题、sheet 纸面（随切换平移）。
 */
open class PageShell(context: Context) : LinearLayout(context) {

    val head: FrameLayout
    val sheet: FrameLayout
    val body: ScrollView
    val bodyContent: LinearLayout

    init {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(MP, MP)
        setBackgroundColor(Pal.bg(context))

        head = FrameLayout(context).apply { setBackgroundColor(Pal.bg(context)) }
        addView(head, LinearLayout.LayoutParams(MP, WC))

        val sheetBox = FrameLayout(context)
        addView(sheetBox, LinearLayout.LayoutParams(MP, 0, 1f))
        sheet = FrameLayout(context).apply { setBackgroundColor(Pal.bg(context)) }
        sheetBox.addView(sheet, FrameLayout.LayoutParams(MP, MP))

        body = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
        }
        sheet.addView(body, FrameLayout.LayoutParams(MP, MP))
        bodyContent = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        body.addView(bodyContent, FrameLayout.LayoutParams(MP, WC))
    }

    fun addHead(view: View, padH: Float = 20f) {
        val ctx = context
        head.setPadding(ctx.sdpI(padH), ctx.sdpI(56f), ctx.sdpI(padH), 0)
        head.addView(view, FrameLayout.LayoutParams(MP, WC))
    }

    fun setBodyPadding(h: Float, bottom: Float = 10f, top: Float = 0f) {
        body.setPadding(context.sdpI(h), context.sdpI(top), context.sdpI(h), context.sdpI(bottom))
        body.clipToPadding = false
    }
}

/** 大标题 Today + 日期 */
fun buildTodayHead(context: Context, title: String, dateText: String): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply {
            text = title
            setTextColor(Pal.ink(context))
            textSize = 34f
            letterSpacing = -0.02f
            includeFontPadding = false
        })
        addView(TextView(context).apply {
            text = dateText
            mono(15f)
            letterSpacing = 0.07f
            setTextColor(Pal.ink2(context))
            setPadding(0, context.sdpI(10f), 0, context.sdpI(4f))
        })
    }

// --------------------------------------------------------------------- Tab 栏

class TabBar(context: Context) : LinearLayout(context) {

    private val tabs = mutableListOf<TextView>()
    var onSelect: ((Int) -> Unit)? = null

    var selected = 0
        private set

    init {
        orientation = LinearLayout.HORIZONTAL
        background = Ui.rounded(context, Pal.bg(context), 15f, 1.6f, Pal.ink(context), withStroke = true)
        val pad = Ui.dp(context, 5f)
        setPadding(pad, pad, pad, pad)
        listOf("首页", "课表", "圈子", "我").forEachIndexed { index, label ->
            val tv = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 15f
                isClickable = true
                setPadding(0, Ui.dp(context, 11f), 0, Ui.dp(context, 11f))
                setOnClickListener { onSelect?.invoke(index) }
            }
            addView(tv, LinearLayout.LayoutParams(0, WC, 1f))
            tabs.add(tv)
        }
        select(0)
    }

    fun select(index: Int) {
        selected = index
        tabs.forEachIndexed { i, tv ->
            val on = i == index
            tv.setTextColor(if (on) Pal.onInk(context) else Ui.color(context, R.color.tab_fg))
            tv.typeface = Typeface.create("sans-serif", if (on) Typeface.BOLD else Typeface.NORMAL)
            tv.background = if (on) Ui.rounded(context, Pal.ink(context), 11f) else null
        }
    }
}

// --------------------------------------------------------------------- Toast

class ToastView(context: Context) : AppCompatTextView(context) {

    private var hide: Runnable? = null

    init {
        setTextColor(Color.WHITE)
        textSize = 13.5f
        gravity = Gravity.CENTER
        setPadding(context.sdpI(18f), context.sdpI(11f), context.sdpI(18f), context.sdpI(11f))
        background = Ui.rounded(context, Ui.color(context, R.color.toast_bg), 999f)
        alpha = 0f
        maxWidth = (context.resources.displayMetrics.widthPixels * 0.86f).toInt()
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    fun show(msg: String) {
        text = msg
        animate().cancel()
        animate().alpha(1f).translationY(0f).setDuration(200).start()
        hide?.let { removeCallbacks(it) }
        val r = Runnable { animate().alpha(0f).translationY(this@ToastView.context.sdp(10f)).setDuration(230).start() }
        hide = r
        postDelayed(r, 1900)
    }
}

// --------------------------------------------------------------------- 底部弹层

class SheetController(private val context: Context) {

    private var dialog: BottomSheetDialog? = null

    fun isShowing(): Boolean = dialog?.isShowing == true

    fun show(builder: (LinearLayout) -> Unit) {
        dismiss()
        val dlg = BottomSheetDialog(context, R.style.SheetDialog)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.sdpI(20f), context.sdpI(10f), context.sdpI(20f), context.sdpI(28f))
        }
        container.addView(View(context).apply {
            background = Ui.rounded(context, Ui.color(context, R.color.ink_4), 4f)
        }, LinearLayout.LayoutParams(context.sdpI(40f), context.sdpI(4f)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = context.sdpI(16f)
        })
        builder(container)
        dlg.setContentView(container)
        dlg.setCanceledOnTouchOutside(true)
        dlg.setOnDismissListener { dialog = null }
        dialog = dlg
        dlg.show()
        dlg.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dlg.behavior.isDraggable = true
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}

// --------------------------------------------------------------------- 常用小件

fun Context.titleText(text: String, size: Float = 19f, bold: Boolean = true): TextView =
    TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Pal.ink(this@titleText))
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
