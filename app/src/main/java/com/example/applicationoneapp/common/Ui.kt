package com.example.applicationoneapp.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/** 设计稿以 392dp 宽绘制，这里按真实屏宽等比换算 */
object Ui {

    const val BASE_WIDTH = 392f

    fun dp(context: Context, value: Float): Int =
        (context.resources.displayMetrics.density * value + 0.5f).toInt()

    fun dpF(context: Context, value: Float): Float =
        context.resources.displayMetrics.density * value

    fun screenWidth(context: Context): Float = context.resources.displayMetrics.widthPixels.toFloat()

    /** 与参考稿一致的等比缩放系数 */
    fun scale(context: Context): Float = screenWidth(context) / (BASE_WIDTH * context.resources.displayMetrics.density)

    fun color(context: Context, res: Int): Int = androidx.core.content.ContextCompat.getColor(context, res)

    fun csl(color: Int): ColorStateList = ColorStateList.valueOf(color)

    fun sp(value: Float): Float = value

    /** 圆角背景 */
    fun rounded(context: Context, color: Int, radiusDp: Float, strokeDp: Float = 0f, strokeColor: Int = Color.TRANSPARENT, withStroke: Boolean = false): GradientDrawable {
        val d = GradientDrawable()
        d.cornerRadius = dpF(context, radiusDp)
        d.setColor(color)
        if (strokeDp > 0f && (withStroke || strokeColor != Color.TRANSPARENT)) {
            d.setStroke(dp(context, strokeDp).coerceAtLeast(1), strokeColor)
        }
        return d
    }

    fun oval(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    /** 上下或左右单边圆角 */
    fun roundedCorners(
        context: Context,
        color: Int,
        radiusDp: Float,
        topLeft: Boolean = true,
        topRight: Boolean = true,
        bottomRight: Boolean = true,
        bottomLeft: Boolean = true,
    ): GradientDrawable {
        val r = dpF(context, radiusDp)
        val radii = floatArrayOf(
            if (topLeft) r else 0f, if (topLeft) r else 0f,
            if (topRight) r else 0f, if (topRight) r else 0f,
            if (bottomRight) r else 0f, if (bottomRight) r else 0f,
            if (bottomLeft) r else 0f, if (bottomLeft) r else 0f,
        )
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(color)
        }
    }

    val MONO: Typeface = Typeface.create("monospace", Typeface.NORMAL)
    val MONO_BOLD: Typeface = Typeface.create("monospace", Typeface.BOLD)

    fun TextView.mono(sizeSp: Float = 13f, colorInt: Int = currentTextColor, bold: Boolean = false) {
        typeface = if (bold) MONO_BOLD else MONO
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(colorInt)
    }

    fun dpToPx(context: Context, value: Float): Int = dp(context, value)

    fun margin(view: View, l: Int = Int.MIN_VALUE, t: Int = Int.MIN_VALUE, r: Int = Int.MIN_VALUE, b: Int = Int.MIN_VALUE) {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (l != Int.MIN_VALUE) lp.leftMargin = l
        if (t != Int.MIN_VALUE) lp.topMargin = t
        if (r != Int.MIN_VALUE) lp.rightMargin = r
        if (b != Int.MIN_VALUE) lp.bottomMargin = b
        view.layoutParams = lp
    }

    fun removeParent(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
