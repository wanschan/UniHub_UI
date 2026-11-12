package com.example.applicationoneapp.ui.page

import android.annotation.SuppressLint
import androidx.core.graphics.toColorInt
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.applicationoneapp.auth.AuthStore
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.LoginActivity
import com.example.applicationoneapp.ui.widget.PageShell
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdpI
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.common.Ui.dpF
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.R

/** 「我」：个人信息、功能菜单（设置 / 常见问题 / 更多 / 赞助）、会员卡 */
@SuppressLint("ViewConstructor")
class MePage(
    context: Context,
    private val onSettings: () -> Unit,
    private val onFaq: () -> Unit,
    private val onAbout: () -> Unit,
    private val onPrivacy: () -> Unit,
    private val onContact: () -> Unit,
    private val onSponsor: () -> Unit,
    private val onVip: () -> Unit,
) : PageShell(context) {

    private lateinit var settingsVal: TextView
    private lateinit var moreBlock: LinearLayout
    private lateinit var moreChev: TextView

    init {
        buildHead()
        setBodyPadding(20f, bottom = 26f, top = 24f)
        buildBody()
    }

    private val dark get() = ThemePrefs.isDark(context)

    fun refreshSettings() {
        if (::settingsVal.isInitialized) {
            settingsVal.text = ThemePrefs.settingsSummary(context)
        }
    }

    private fun buildHead() {
        val ctx = context
        addHead(object : LinearLayout(ctx) {
            init {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.sdpI(6f), 0, ctx.sdpI(4f))
                addView(TextView(ctx).apply {
                    text = AuthStore.initial(ctx)
                    textSize = 23f
                    gravity = Gravity.CENTER
                    setTextColor(if (ThemePrefs.isDark(ctx)) Pal.ink(ctx) else Color.WHITE)
                    background = Ui.rounded(ctx, if (ThemePrefs.isDark(ctx)) Ui.color(ctx, R.color.card_2) else "#2C2F34".toColorInt(), 12f)
                }, LinearLayout.LayoutParams(ctx.sdpI(54f), ctx.sdpI(54f)))
                addView(LinearLayout(ctx).apply {
                    orientation = VERTICAL
                    setPadding(ctx.sdpI(15f), 0, 0, 0)
                    addView(TextView(ctx).apply {
                        text = "Hello,"
                        mono(13f)
                        letterSpacing = 0.1f
                        setTextColor(Ui.color(ctx, R.color.ink_2))
                    })
                    addView(TextView(ctx).apply {
                        text = AuthStore.displayName(ctx).ifBlank { "同学" }
                        textSize = 23f
                        setTextColor(Pal.ink(ctx))
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        setPadding(0, ctx.sdpI(3f), 0, 0)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }, LinearLayout.LayoutParams(MP, WC))
                    addView(TextView(ctx).apply {
                        text = AuthStore.email(ctx)
                        mono(10.5f)
                        setTextColor(Ui.color(ctx, R.color.ink_3))
                        setPadding(0, ctx.sdpI(4f), 0, 0)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }, LinearLayout.LayoutParams(MP, WC))
                }, LinearLayout.LayoutParams(0, WC, 1f))
            }
        })
    }

    private fun buildBody() {
        val ctx = context
        val root = bodyContent

        // ---- 功能菜单
        val menu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(16f), 0, ctx.sdpI(16f), 0)
            background = Ui.rounded(ctx, Pal.card(ctx), 22f)
        }
        root.addView(menu, LinearLayout.LayoutParams(MP, WC))

        menu.addView(menuRow(R.drawable.ic_settings, "全局设置", null, "settings") { onSettings() })
        menu.addView(rowDivider())
        menu.addView(menuRow(R.drawable.ic_faq, "常见问题", null, null) { onFaq() })
        menu.addView(rowDivider())
        menu.addView(menuRow(R.drawable.ic_more_dots, "更多", null, "more") { toggleMore() })
        menu.addView(rowDivider())

        // 展开的子项
        moreBlock = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        menu.addView(moreBlock, LinearLayout.LayoutParams(MP, WC))
        moreBlock.addView(subRow("关于 UniHub") { onAbout() })
        moreBlock.addView(subDivider())
        moreBlock.addView(subRow("个人信息收集清单") { onPrivacy() })
        moreBlock.addView(subDivider())
        moreBlock.addView(subRow("联系我们") { onContact() })
        moreBlock.addView(subDivider())

        menu.addView(sponsorRow())
        menu.addView(rowDivider())
        menu.addView(logoutRow())
        refreshSettings()

        // ---- 会员卡
        root.addView(vipCard(), LinearLayout.LayoutParams(MP, ctx.sdpI(158f)).apply {
            topMargin = ctx.sdpI(34f)
        })
    }

    private fun rowDivider(): View = View(context).apply {
        setBackgroundColor(Ui.color(context, R.color.hairline_2))
        layoutParams = LinearLayout.LayoutParams(MP, 1)
    }

    private fun subDivider(): View = View(context).apply {
        setBackgroundColor(Ui.color(context, R.color.hairline))
        layoutParams = LinearLayout.LayoutParams(MP, 1)
    }

    private fun menuRow(iconRes: Int, label: String, value: String?, key: String?, onClick: () -> Unit): View {
        val ctx = context
        val row = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(16f), 0, ctx.sdpI(16f))
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(iconRes)
            setColorFilter(Pal.ink2(ctx), android.graphics.PorterDuff.Mode.SRC_IN)
        }, LinearLayout.LayoutParams(ctx.sdpI(20f), ctx.sdpI(20f)))
        row.addView(TextView(ctx).apply {
            text = label
            textSize = 15.5f
            setTextColor(Pal.ink(ctx))
            setPadding(ctx.sdpI(11f), 0, ctx.sdpI(8f), 0)
        }, LinearLayout.LayoutParams(0, WC, 1f))
        val valView = TextView(ctx).apply {
            text = value ?: ""
            mono(10.5f)
            setTextColor(Ui.color(ctx, R.color.ink_3))
        }
        row.addView(valView, LinearLayout.LayoutParams(WC, WC))
        if (key == "settings") settingsVal = valView
        val chev = TextView(ctx).apply {
            text = "›"
            textSize = 17f
            setTextColor(Ui.color(ctx, R.color.ink_4))
            setPadding(ctx.sdpI(6f), 0, 0, 0)
        }
        row.addView(chev, LinearLayout.LayoutParams(WC, WC))
        if (key == "more") moreChev = chev
        return row
    }

    private fun subRow(label: String, onClick: () -> Unit): View {
        val ctx = context
        return LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.sdpI(31f), ctx.sdpI(13f), 0, ctx.sdpI(13f))
            isClickable = true
            setOnClickListener { onClick() }
            addView(TextView(ctx).apply {
                text = label
                textSize = 14.5f
                setTextColor(Pal.ink2(ctx))
            }, LinearLayout.LayoutParams(0, WC, 1f))
            addView(TextView(ctx).apply {
                text = "›"
                textSize = 17f
                setTextColor(Ui.color(ctx, R.color.ink_4))
            })
        }
    }

    private fun sponsorRow(): View {
        val ctx = context
        val row = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(16f), 0, ctx.sdpI(16f))
            isClickable = true
            setOnClickListener { onSponsor() }
        }
        row.addView(TextView(ctx).apply {
            text = "♥"
            textSize = 17f
            setTextColor(Pal.accent(ctx))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ctx.sdpI(20f), ctx.sdpI(20f)))
        row.addView(TextView(ctx).apply {
            text = "赞助"
            textSize = 15.5f
            setTextColor(Pal.ink(ctx))
            setPadding(ctx.sdpI(11f), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, WC, 1f))
        row.addView(TextView(ctx).apply {
            text = "›"
            textSize = 17f
            setTextColor(Ui.color(ctx, R.color.ink_4))
        })
        return row
    }

    private fun logoutRow(): View {
        val ctx = context
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.sdpI(31f), ctx.sdpI(16f), 0, ctx.sdpI(16f))
            isClickable = true
            setOnClickListener { confirmLogout() }
            addView(TextView(ctx).apply {
                text = "退出登录"
                textSize = 15.5f
                setTextColor(Pal.accent(ctx))
            }, LinearLayout.LayoutParams(0, WC, 1f))
        }
    }

    private fun confirmLogout() {
        val ctx = context
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("退出登录")
            .setMessage("退出后需要重新用邮箱验证码登录，本机已有的课表与设置不会删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("退出") { _, _ ->
                AuthStore.signOut(ctx)
                ctx.startActivity(Intent(ctx, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                (ctx as? android.app.Activity)?.finish()
            }
            .show()
    }

    private fun toggleMore() {
        val open = moreBlock.visibility != View.VISIBLE
        moreBlock.visibility = if (open) View.VISIBLE else View.GONE
        moreChev.animate().rotation(if (open) 90f else 0f).setDuration(260).start()
        if (open) {
            moreBlock.alpha = 0f
            moreBlock.animate().alpha(1f).setDuration(220).start()
        }
    }

    private fun vipCard(): View {
        val ctx = context
        val wrap = FrameLayout(ctx)
        val blob = View(ctx).apply {
            background = Ui.rounded(ctx, Ui.color(ctx, R.color.vip_blob), 22f)
            rotation = -9f
        }
        wrap.addView(blob, FrameLayout.LayoutParams((ctx.resources.displayMetrics.widthPixels * 0.80f).toInt(), ctx.sdpI(130f)).apply {
            leftMargin = ctx.sdpI(6f)
            topMargin = ctx.sdpI(8f)
        })

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(20f), ctx.sdpI(22f), ctx.sdpI(20f), ctx.sdpI(20f))
            background = Ui.roundedCorners(
                ctx, Ui.color(ctx, R.color.vip_card), 22f,
                topLeft = true, topRight = false, bottomRight = false, bottomLeft = true,
            )
            elevation = Ui.dpF(ctx, 8f)
        }
        wrap.addView(card, FrameLayout.LayoutParams(MP, ctx.sdpI(132f)).apply { topMargin = ctx.sdpI(18f) })

        card.addView(TextView(ctx).apply {
            text = "49RMB"
            textSize = 27f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Ui.color(ctx, R.color.vip_text))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MP, 0, 1f))

        val foot = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        card.addView(foot, LinearLayout.LayoutParams(MP, WC))
        foot.addView(TextView(ctx).apply {
            text = "欢迎订阅\n您可享更多服务"
            textSize = 12.5f
            setTextColor(Ui.color(ctx, R.color.vip_text_2))
            setLineSpacing(0f, 1.45f)
        }, LinearLayout.LayoutParams(0, WC, 1f))
        foot.addView(TextView(ctx).apply {
            text = "详细信息›"
            textSize = 12.5f
            setTextColor(Ui.color(ctx, R.color.vip_more))
            isClickable = true
            setOnClickListener { onVip() }
        })
        return wrap
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
