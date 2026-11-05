package com.example.applicationoneapp.ui.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.widget.PageShell
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdpI
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.R

/** 圈子：四个圈子卡片 + 可按圈子筛选的动态流 */
@SuppressLint("ViewConstructor")
class CirclesPage(
    context: Context,
    private val toast: (String) -> Unit,
) : PageShell(context) {

    private data class Circle(
        val name: String,
        val meta: String,
        val iconRes: Int,
        val bg: Int,
        val fgName: String,
        val tagBg: Int,
        val tagFg: Int,
    )

    private data class Post(
        val circle: String,
        val time: String,
        val text: String,
        val likes: Int,
        val comments: Int,
    )

    private val circles = listOf(
        Circle("考研互助", "128 人 · 今日 12 条", R.drawable.ic_book, R.color.tint_blue_bg, "#26374F", R.color.tint_blue_bg, R.color.tint_blue_fg),
        Circle("二手书市", "96 人 · 在售 342 本", R.drawable.ic_books, R.color.tint_sand_bg, "#6A530D", R.color.tint_sand_bg, R.color.tint_sand_fg),
        Circle("美食探店", "213 人 · 今日 8 条", R.drawable.ic_food, R.color.tint_clay_bg, "#7A3823", R.color.tint_clay_bg, R.color.tint_clay_fg),
        Circle("电竞开黑", "64 人 · 今晚 3 局", R.drawable.ic_gamepad, R.color.tint_sage_bg, "#255034", R.color.tint_sage_bg, R.color.tint_sage_fg),
    )

    private val posts = listOf(
        Post("考研互助", "12 分钟前", "求 408 数据结构真题，有一起在图书馆四楼自习的吗？可以互相监督打卡。", 24, 6),
        Post("二手书市", "38 分钟前", "出《计算机网络》第八版 + 配套习题册，九成新，25 元自提，本部宿舍楼下。", 11, 3),
        Post("美食探店", "1 小时前", "三食堂二楼新开的砂锅窗口，砂锅鸡 15 元管饱，加面不要钱，冲！", 46, 12),
        Post("电竞开黑", "2 小时前", "今晚 8 点 5v5，缺一个打野，段位钻石以上，宿舍 302 集合。", 8, 9),
    )

    private val cardViews = mutableListOf<LinearLayout>()
    private val postViews = mutableListOf<View>()
    private lateinit var feedTitle: TextView
    private lateinit var feedAll: TextView
    private var active: String? = null

    init {
        buildHead()
        setBodyPadding(20f, bottom = 30f, top = 4f)
        buildBody()
    }

    private fun buildHead() {
        addHead(object : LinearLayout(context) {
            init {
                orientation = VERTICAL
                addView(TextView(context).apply {
                    text = "Circles"
                    mono(15f)
                    letterSpacing = 0.07f
                    setTextColor(Ui.color(context, R.color.ink_2))
                })
                addView(TextView(context).apply {
                    text = "圈子"
                    textSize = 34f
                    setTextColor(Pal.ink(context))
                    letterSpacing = -0.02f
                    setPadding(0, context.sdpI(6f), 0, 0)
                })
            }
        })
    }

    private fun buildBody() {
        val ctx = context
        val root = bodyContent

        // ---- 圈子卡片 2×2
        val grid = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.sdpI(22f), 0, 0)
        }
        root.addView(grid, LinearLayout.LayoutParams(MP, WC))
        circles.chunked(2).forEach { pair ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(row, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(12f) })
            pair.forEachIndexed { i, circle ->
                val card = circleCard(circle)
                row.addView(card, LinearLayout.LayoutParams(0, WC, 1f).apply {
                    if (i == 0) rightMargin = ctx.sdpI(6f) else leftMargin = ctx.sdpI(6f)
                })
                cardViews.add(card)
            }
        }

        // ---- 动态标题
        val feedHead = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(26f), 0, ctx.sdpI(12f))
        }
        root.addView(feedHead, LinearLayout.LayoutParams(MP, WC))
        feedTitle = TextView(ctx).apply {
            text = "最新动态"
            textSize = 16.5f
            setTextColor(Pal.ink(ctx))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        feedHead.addView(feedTitle, LinearLayout.LayoutParams(0, WC, 1f))
        feedAll = TextView(ctx).apply {
            text = "查看全部"
            textSize = 14.5f
            setTextColor(Ui.color(ctx, R.color.accent))
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { applyCircle(null) }
            visibility = View.GONE
        }
        feedHead.addView(feedAll, LinearLayout.LayoutParams(WC, WC))

        // ---- 动态列表
        val feed = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(feed, LinearLayout.LayoutParams(MP, WC))
        posts.forEach { post ->
            val v = postCard(post)
            feed.addView(v, LinearLayout.LayoutParams(MP, WC).apply { bottomMargin = ctx.sdpI(10f) })
            postViews.add(v)
        }
    }

    private fun circleCard(circle: Circle): LinearLayout {
        val ctx = context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            setPadding(ctx.sdpI(12f), ctx.sdpI(13f), ctx.sdpI(12f), ctx.sdpI(12f))
            background = circleBg(false)
            elevation = if (ThemePrefs.isDark(ctx)) 0f else Ui.dpF(ctx, 2.5f)
            isClickable = true
            setOnClickListener {
                applyCircle(if (active == circle.name) null else circle.name)
                toast(if (active == circle.name) "「${circle.name}」里的新消息" else "已显示全部圈子")
            }
            pressFeedback(0.965f)
        }
        val iconBox = FrameLayout(ctx).apply { background = Ui.rounded(ctx, Ui.color(ctx, circle.bg), 9f) }
        iconBox.addView(ImageView(ctx).apply { setImageResource(circle.iconRes) },
            FrameLayout.LayoutParams(WC, WC).apply { gravity = Gravity.CENTER })
        card.addView(iconBox, LinearLayout.LayoutParams(ctx.sdpI(34f), ctx.sdpI(34f)))
        card.addView(TextView(ctx).apply {
            text = circle.name
            textSize = 15.5f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Pal.ink(ctx))
            setPadding(0, ctx.sdpI(11f), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = circle.meta
            mono(10.5f)
            setTextColor(Ui.color(ctx, R.color.ink_3))
            setPadding(0, ctx.sdpI(4f), 0, 0)
        })
        card.tag = circle
        return card
    }

    private fun circleBg(isOn: Boolean): android.graphics.drawable.Drawable {
        val ctx = context
        return if (ThemePrefs.isDark(ctx)) {
            if (isOn) Ui.rounded(ctx, Ui.color(ctx, R.color.card), 15f, 1.5f, Pal.ink(ctx), withStroke = true)
            else Ui.rounded(ctx, Ui.color(ctx, R.color.card), 15f, 1f, Ui.color(ctx, R.color.hairline_2))
        } else {
            if (isOn) Ui.rounded(ctx, Color.WHITE, 15f, 1.5f, Pal.ink(ctx), withStroke = true)
            else Ui.rounded(ctx, Color.WHITE, 15f, 1f, Ui.color(ctx, R.color.hairline))
        }
    }

    private fun postCard(post: Post): View {
        val ctx = context
        val circle = circles.first { it.name == post.circle }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(15f), ctx.sdpI(14f), ctx.sdpI(15f), ctx.sdpI(13f))
            background = Ui.rounded(ctx, Pal.card(ctx), 18f)
        }
        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        card.addView(top, LinearLayout.LayoutParams(MP, WC).apply { bottomMargin = ctx.sdpI(9f) })
        top.addView(TextView(ctx).apply {
            text = circle.name
            textSize = 11f
            setPadding(ctx.sdpI(9f), ctx.sdpI(5f), ctx.sdpI(9f), ctx.sdpI(5f))
            setTextColor(Ui.color(ctx, circle.tagFg))
            background = Ui.rounded(ctx, Ui.color(ctx, circle.tagBg), 999f)
        })
        top.addView(TextView(ctx).apply {
            text = post.time
            mono(10.5f)
            setTextColor(Ui.color(ctx, R.color.ink_3))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, WC, 1f))
        card.addView(TextView(ctx).apply {
            text = post.text
            textSize = 14.5f
            setTextColor(Ui.color(ctx, R.color.ink_2))
            setLineSpacing(0f, 1.55f)
        })
        val foot = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.sdpI(10f), 0, 0)
        }
        card.addView(foot, LinearLayout.LayoutParams(MP, WC))
        listOf("♡ ${post.likes}", "💬 ${post.comments}").forEach { label ->
            foot.addView(TextView(ctx).apply {
                text = label
                mono(11f)
                setTextColor(Ui.color(ctx, R.color.ink_3))
                setPadding(0, 0, ctx.sdpI(18f), 0)
            })
        }
        return card
    }

    /** 点卡片筛选动态 */
    private fun applyCircle(name: String?) {
        active = name
        cardViews.forEach { card ->
            val circle = card.tag as? Circle
            card.background = circleBg(circle?.name != null && circle.name == name)
        }
        postViews.forEachIndexed { i, v ->
            v.visibility = if (name == null || posts[i].circle == name) View.VISIBLE else View.GONE
        }
        feedTitle.text = if (name == null) "最新动态" else "「$name」最新动态"
        feedAll.visibility = if (name == null) View.GONE else View.VISIBLE
    }
}
// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
