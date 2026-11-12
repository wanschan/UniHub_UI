package com.example.applicationoneapp.ui.sheet

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdpI
import com.example.applicationoneapp.ui.widget.titleText
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.R
import com.example.applicationoneapp.BuildConfig

/**
 * 弹层内容构建器。每个方法都往 sheet 容器里追加视图，
 * 由 SheetController 负责显示与关闭。
 */
class Sheets(
    private val activity: AppCompatActivity,
    private val toast: (String) -> Unit,
    private val dismiss: () -> Unit,
    private val onOpenMap: (() -> Unit)? = null,
) {

    private val ctx: Context get() = activity

    /** 本次弹层的内容容器，由 [reset] 重置后逐个方法往里追加 */
    private lateinit var root: LinearLayout

    fun reset(container: LinearLayout) {
        root = container
    }

    // ------------------------------------------------------------ 通用

    private fun sub(text: String): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(Ui.color(ctx, R.color.ink_3))
        setLineSpacing(0f, 1.6f)
        setPadding(0, ctx.sdpI(5f), 0, ctx.sdpI(16f))
    }

    private fun cta(label: String, onClick: () -> Unit): TextView = TextView(ctx).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(Pal.onInk(ctx))
        background = Ui.rounded(ctx, Pal.ink(ctx), 14f)
        isClickable = true
        setPadding(0, ctx.sdpI(14f), 0, ctx.sdpI(14f))
        setOnClickListener { onClick() }
        pressFeedback(0.98f)
    }

    private fun sectionLabel(text: String): TextView = TextView(ctx).apply {
        this.text = text
        mono(10.5f)
        letterSpacing = 0.12f
        setTextColor(Ui.color(ctx, R.color.ink_3))
        setPadding(0, 0, 0, ctx.sdpI(9f))
    }

    // 自定义 TextView 继承 AppCompatTextView（直接继承 TextView 无法拿到 AppCompat 的兼容实现）
    private class PillButton(context: Context) : AppCompatTextView(context) {
        var selected = false
            private set

        init {
            gravity = Gravity.CENTER
            textSize = 13.5f
            setPadding(0, context.sdpI(9f), 0, context.sdpI(9f))
            isClickable = true
        }

        fun setOn(on: Boolean) {
            selected = on
            setTextColor(if (on) Pal.onInk(context) else Pal.ink2(context))
            background = if (on) Ui.rounded(context, Pal.ink(context), 10f) else null
            typeface = Typeface.create("sans-serif", if (on) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    // ------------------------------------------------------------ 全局设置

    fun settings() {
        root.addView(ctx.titleText("全局设置"))
        root.addView(sub("偏好保存在本机，下次打开依然生效"))

        // 语言
        root.addView(sectionLabel("语言"))
        val langSeg = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.rounded(ctx, Ui.color(ctx, R.color.seg_track), 13f)
            setPadding(ctx.sdpI(4f), ctx.sdpI(4f), ctx.sdpI(4f), ctx.sdpI(4f))
        }
        root.addView(langSeg, LinearLayout.LayoutParams(MP, WC))

        val langs = listOf("auto" to "跟随系统", "zh" to "中文", "en" to "English")
        val langButtons = mutableListOf<PillButton>()
        langs.forEach { (value, label) ->
            val b = PillButton(ctx).apply {
                text = label
                setOn(ThemePrefs.lang(ctx) == value)
                setOnClickListener {
                    ThemePrefs.setLang(ctx, value)
                    langButtons.forEachIndexed { idx, btn -> btn.setOn(langs[idx].first == value) }
                    toast(if (value == "auto") "语言：跟随系统" else if (value == "zh") "语言：中文" else "Language: English")
                }
            }
            langSeg.addView(b, LinearLayout.LayoutParams(0, WC, 1f))
            langButtons.add(b)
        }

        // 主题
        val themeLabel = sectionLabel("主题")
        themeLabel.setPadding(0, ctx.sdpI(20f), 0, ctx.sdpI(9f))
        root.addView(themeLabel)
        val themeSeg = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.rounded(ctx, Ui.color(ctx, R.color.seg_track), 13f)
            setPadding(ctx.sdpI(4f), ctx.sdpI(4f), ctx.sdpI(4f), ctx.sdpI(4f))
        }
        root.addView(themeSeg, LinearLayout.LayoutParams(MP, WC))

        val themes = listOf("light" to "亮色", "dark" to "暗色", "auto" to "跟随系统")
        val themeButtons = mutableListOf<PillButton>()
        themes.forEach { (value, label) ->
            val b = PillButton(ctx).apply {
                text = label
                setOn(ThemePrefs.theme(ctx) == value)
                setOnClickListener {
                    ThemePrefs.setTheme(ctx, value)
                    toast(if (value == "light") "主题：亮色" else if (value == "dark") "主题：暗色" else "主题：跟随系统")
                    dismiss()
                    activity.recreate()
                }
            }
            themeSeg.addView(b, LinearLayout.LayoutParams(0, WC, 1f))
            themeButtons.add(b)
        }

        // 版本更新
        root.addView(View(ctx).apply {
            setBackgroundColor(Ui.color(ctx, R.color.line))
        }, LinearLayout.LayoutParams(MP, 1).apply { topMargin = ctx.sdpI(20f) })

        val updHint = TextView(ctx).apply {
            text = "当前 1.4.0 · 2026.06"
            mono(11.5f)
            setTextColor(Ui.color(ctx, R.color.ink_3))
            setPadding(0, ctx.sdpI(5f), 0, 0)
        }
        val updBtn = TextView(ctx).apply {
            text = "检测更新"
            textSize = 13f
            setTextColor(Pal.ink(ctx))
            setPadding(ctx.sdpI(15f), ctx.sdpI(8f), ctx.sdpI(15f), ctx.sdpI(8f))
            background = Ui.rounded(ctx, Color.TRANSPARENT, 999f, 1.4f, Pal.ink(ctx), withStroke = true)
            isClickable = true
        }
        val updRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(18f), 0, 0)
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(ctx).apply {
                    text = "版本更新"
                    textSize = 15f
                    setTextColor(Pal.ink(ctx))
                })
                addView(updHint)
            }, LinearLayout.LayoutParams(0, WC, 1f))
            addView(updBtn)
        }
        root.addView(updRow, LinearLayout.LayoutParams(MP, WC))

        updBtn.setOnClickListener {
            if (!updBtn.isEnabled) return@setOnClickListener
            updBtn.isEnabled = false
            updBtn.alpha = 0.45f
            updBtn.text = "检测中…"
            updHint.text = "正在检查更新…"
            updBtn.postDelayed({
                updBtn.text = "已是最新"
                updHint.text = "当前 ${BuildConfig.VERSION_NAME}（2026.11）· 已是最新版本"
                toast("已是最新版本")
            }, 900)
        }
    }

    // ------------------------------------------------------------ 常见问题

    private val faqs = listOf(
        "NFC 靠近闸机没反应怎么办？" to
            "先在「首页 · NFC前往」里确认该门禁已绑定，再把手机背面顶部贴近读卡区 1–2 秒。金属手机壳和厚卡包会影响识别，建议取下。",
        "49RMB 订阅包含哪些权益？" to
            "校园网加速、云打印额度 200 页、洗衣房 9 折、饮水机无限畅饮，按学期计费，可在「我」页底部查看并管理。",
        "怎么把教务系统的课表导入？" to
            "在课表页下拉可触发同步，首次需要登录一次教务系统；之后每次打开会自动比对最新课表。",
        "圈子里的帖子可以删除吗？" to
            "自己发布的帖子长按可选择删除；他人内容请点右上角「···」举报，管理员会在 24 小时内处理。",
        "换手机之后数据还在吗？" to
            "课表、订阅与圈子账号跟着校园账号走，登录同一账号即可恢复；本地偏好（主题、语言）需要重新设置。",
    )

    fun faq() {
        root.addView(ctx.titleText("常见问题"))
        root.addView(sub("点开查看详情，仍有疑问可在「更多 · 联系我们」找到我们"))
        root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) }, LinearLayout.LayoutParams(MP, 1))
        faqs.forEach { (q, a) ->
            val answer = TextView(ctx).apply {
                text = a
                textSize = 13.5f
                setTextColor(Ui.color(ctx, R.color.ink_3))
                setLineSpacing(0f, 1.62f)
                setPadding(0, 0, ctx.sdpI(14f), ctx.sdpI(15f))
                visibility = View.GONE
            }
            val qRow = TextView(ctx).apply {
                text = q
                textSize = 14.5f
                setTextColor(Pal.ink(ctx))
                setLineSpacing(0f, 1.5f)
                setPadding(0, ctx.sdpI(15f), 0, ctx.sdpI(15f))
                compoundDrawablePadding = ctx.sdpI(12f)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_plus, 0)
                isClickable = true
            }
            val block = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(qRow, LinearLayout.LayoutParams(MP, WC))
                addView(answer, LinearLayout.LayoutParams(MP, WC))
                addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) }, LinearLayout.LayoutParams(MP, 1))
            }
            qRow.setOnClickListener {
                val open = answer.visibility != View.VISIBLE
                answer.visibility = if (open) View.VISIBLE else View.GONE
                qRow.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (open) R.drawable.ic_minus else R.drawable.ic_plus, 0)
                if (open) {
                    answer.alpha = 0f
                    answer.animate().alpha(1f).setDuration(200).start()
                }
            }
            root.addView(block, LinearLayout.LayoutParams(MP, WC))
        }
    }

    // ------------------------------------------------------------ 关于

    fun about() {
        root.addView(ctx.titleText("关于 UniHub"))

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(20f), 0, 0)
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_logo_mark)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "UniHub"
            background = Ui.rounded(
                ctx, Color.WHITE, 17f,
                strokeDp = 1f, strokeColor = Ui.color(ctx, R.color.line), withStroke = true,
            )
        }, LinearLayout.LayoutParams(ctx.sdpI(56f), ctx.sdpI(56f)))
        row.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(14f), 0, 0, 0)
            addView(TextView(ctx).apply {
                text = "UniHub"
                textSize = 17f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setTextColor(Pal.ink(ctx))
            })
            addView(TextView(ctx).apply {
                text = "VERSION 1.4.0 · BUILD 2026.06"
                mono(11f)
                letterSpacing = 0.06f
                setTextColor(Ui.color(ctx, R.color.ink_3))
                setPadding(0, ctx.sdpI(5f), 0, 0)
            })
        }, LinearLayout.LayoutParams(WC, WC))
        root.addView(row, LinearLayout.LayoutParams(MP, WC))

        root.addView(TextView(ctx).apply {
            text = "把课程表、门禁、饮水、洗衣、打印这些校园里的小事收进一个 App —— 少装几个小程序，少走几趟路。"
            textSize = 13.5f
            setTextColor(Ui.color(ctx, R.color.ink_3))
            setLineSpacing(0f, 1.68f)
            setPadding(0, ctx.sdpI(16f), 0, 0)
        })

        listOf("服务条款", "隐私政策", "开源许可").forEachIndexed { i, doc ->
            val v = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.sdpI(13f), 0, ctx.sdpI(13f))
                isClickable = true
                setOnClickListener { toast("$doc（Demo 占位）") }
                addView(TextView(ctx).apply {
                    text = doc
                    textSize = 14f
                    setTextColor(Pal.ink(ctx))
                }, LinearLayout.LayoutParams(0, WC, 1f))
                addView(TextView(ctx).apply {
                    text = "›"
                    textSize = 16f
                    setTextColor(Ui.color(ctx, R.color.ink_4))
                })
            }
            v.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
                LinearLayout.LayoutParams(MP, 1).apply { })
            root.addView(v, LinearLayout.LayoutParams(MP, WC).apply {
                topMargin = if (i == 0) ctx.sdpI(18f) else 0
            })
        }
    }

    // ------------------------------------------------------------ 隐私

    private val privacy = listOf(
        Triple("账号信息", "手机号 · 学号", "用于登录、找回账号，以及与教务系统绑定课表。"),
        Triple("位置信息", "仅使用期间", "用于推荐最近的食堂、饮水点与共享单车，不保存历史轨迹。"),
        Triple("交易记录", "近 6 个月", "展示校园卡与订阅流水；仅在您主动查询时读取。"),
        Triple("设备信息", "型号 · 系统版本", "排查兼容性与崩溃问题，不含广告标识符。"),
        Triple("相机", "按需授权", "仅用于「扫码前往」，不录制、不上传画面。"),
    )

    fun privacy() {
        root.addView(ctx.titleText("个人信息收集清单"))
        root.addView(sub("我们只收集提供服务所必需的信息，你可以随时撤回授权"))
        privacy.forEachIndexed { i, (name, valText, use) ->
            val item = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, ctx.sdpI(13f), 0, ctx.sdpI(13f))
                if (i > 0) addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
                    LinearLayout.LayoutParams(MP, 1).apply { })
            }
            item.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(ctx).apply {
                    text = name
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setTextColor(Pal.ink(ctx))
                }, LinearLayout.LayoutParams(0, WC, 1f))
                addView(TextView(ctx).apply {
                    text = valText
                    mono(10.5f)
                    setTextColor(Ui.color(ctx, R.color.ink_3))
                })
            }, LinearLayout.LayoutParams(MP, WC))
            item.addView(TextView(ctx).apply {
                text = use
                textSize = 13f
                setTextColor(Ui.color(ctx, R.color.ink_3))
                setLineSpacing(0f, 1.55f)
                setPadding(0, ctx.sdpI(5f), 0, 0)
            })
            root.addView(item, LinearLayout.LayoutParams(MP, WC))
        }
    }

    // ------------------------------------------------------------ 联系我们

    fun contact() {
        root.addView(ctx.titleText("联系我们"))
        root.addView(sub("工作日 9:00 – 18:00 内回复，周末可能慢一些"))
        root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) }, LinearLayout.LayoutParams(MP, 1))
        listOf(
            "客服邮箱" to "hi@unihub.app",
            "微信公众号" to "UniHub",
            "线下服务站" to "图书馆 B1 · 12 号",
        ).forEachIndexed { index, (k, v) ->
            if (index > 0) root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
                LinearLayout.LayoutParams(MP, 1))
            root.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.sdpI(14f), 0, ctx.sdpI(14f))
                addView(TextView(ctx).apply {
                    text = k
                    textSize = 14.5f
                    setTextColor(Pal.ink(ctx))
                }, LinearLayout.LayoutParams(0, WC, 1f))
                addView(TextView(ctx).apply {
                    text = v
                    mono(12f)
                    setTextColor(Ui.color(ctx, R.color.ink_3))
                })
            }, LinearLayout.LayoutParams(MP, WC))
        }
        root.addView(cta("复制客服邮箱") {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("email", "hi@unihub.app"))
            toast("邮箱已复制")
        }, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(22f) })
    }

    // ------------------------------------------------------------ 赞助

    private var pick = 12

    fun sponsor() {
        root.addView(ctx.titleText("赞助 UniHub"))
        root.addView(sub("服务器和域名都是自己掏钱，赞助会全部用在维持服务上"))

        val grid = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.sdpI(20f), 0, 0)
        }
        root.addView(grid, LinearLayout.LayoutParams(MP, WC))

        val goBtn = cta("赞助 ¥$pick") {
            dismiss()
            toast("感谢赞助 ¥$pick（Demo）")
        }

        val options = listOf(6 to "一杯奶茶", 12 to "一杯咖啡", 28 to "一顿夜宵")
        val buttons = mutableListOf<LinearLayout>()
        // 选中态底色：暗色白色微透 / 亮色黑色微透，直接写 ARGB
        val highlight = if (ThemePrefs.isDark(ctx)) 0x10FFFFFF else 0x09000000
        val render = {
            options.forEachIndexed { i, (amount, _) ->
                buttons[i].background = Ui.rounded(
                    ctx,
                    if (pick == amount) highlight else Color.TRANSPARENT,
                    16f, 1f, if (pick == amount) Pal.ink(ctx) else Ui.color(ctx, R.color.line), withStroke = true,
                )
            }
            Unit
        }

        options.forEach { (amount, label) ->
            val b = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, ctx.sdpI(16f), 0, ctx.sdpI(13f))
                isClickable = true
                addView(TextView(ctx).apply {
                    text = "¥$amount"
                    mono(17f)
                    setTextColor(Pal.ink(ctx))
                    gravity = Gravity.CENTER
                })
                addView(TextView(ctx).apply {
                    text = label
                    textSize = 11.5f
                    setTextColor(Ui.color(ctx, R.color.ink_3))
                    gravity = Gravity.CENTER
                    setPadding(0, ctx.sdpI(7f), 0, 0)
                })
                setOnClickListener {
                    pick = amount
                    goBtn.text = "赞助 ¥$pick"
                    render()
                }
            }
            grid.addView(b, LinearLayout.LayoutParams(0, WC, 1f).apply {
                leftMargin = if (amount == 6) 0 else ctx.sdpI(5f)
                rightMargin = if (amount == 28) 0 else ctx.sdpI(5f)
            })
            buttons.add(b)
        }
        render()

        root.addView(TextView(ctx).apply {
            text = "仅作演示，不会真的扣款"
            mono(10.5f)
            letterSpacing = 0.04f
            setTextColor(Ui.color(ctx, R.color.ink_3))
            gravity = Gravity.CENTER
            setPadding(0, ctx.sdpI(18f), 0, 0)
        }, LinearLayout.LayoutParams(MP, WC))

        root.addView(goBtn, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(22f) })
    }

    // ------------------------------------------------------------ 获取插件

    private val plugins = mutableListOf(
        Triple("校园卡余额", true, "饮水/食堂通用"),
        Triple("洗衣房", false, "看空闲机位"),
        Triple("图书馆座位", false, "预约自习位"),
        Triple("快递驿站", false, "取件码提醒"),
        Triple("校园网续费", true, "到期自动提醒"),
    )

    fun plugins() {
        root.addView(ctx.titleText("获取插件"))
        root.addView(sub("插件会出现在「快捷启动」中，随时可关闭"))

        // 实景地图：已内置的插件，点开直接进 3D 校区
        root.addView(mapRow())
        root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
            LinearLayout.LayoutParams(MP, 1))

        val toggles = mutableListOf<TextView>()
        plugins.forEachIndexed { index, item ->
            // 分割线放在竖直容器里；放进横向行会被 MATCH_PARENT 撑满，把内容挤出行外
            if (index > 0) root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
                LinearLayout.LayoutParams(MP, 1))
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.sdpI(14f), 0, ctx.sdpI(14f))
            }
            val btn = TextView(ctx).apply {
                text = if (item.second) "已开启" else "开启"
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(ctx.sdpI(15f), ctx.sdpI(6f), ctx.sdpI(15f), ctx.sdpI(6f))
                setOnClickListener {
                    val on = !(tag as Boolean)
                    tag = on
                    text = if (on) "已开启" else "开启"
                    setTextColor(if (on) Pal.onInk(ctx) else Pal.ink(ctx))
                    background = Ui.rounded(ctx, if (on) Pal.ink(ctx) else Color.TRANSPARENT, 999f, 1.4f, Pal.ink(ctx), withStroke = true)
                    if (on) toast("${item.first} 已添加")
                }
            }
            btn.tag = item.second
            btn.setTextColor(if (item.second) Pal.onInk(ctx) else Pal.ink(ctx))
            btn.background = Ui.rounded(ctx, if (item.second) Pal.ink(ctx) else Color.TRANSPARENT, 999f, 1.4f, Pal.ink(ctx), withStroke = true)
            toggles.add(btn)

            row.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(ctx).apply {
                    text = item.first
                    textSize = 15.5f
                    setTextColor(Pal.ink(ctx))
                })
                addView(TextView(ctx).apply {
                    text = item.third
                    mono(10.5f)
                    setTextColor(Ui.color(ctx, R.color.ink_3))
                    setPadding(0, ctx.sdpI(4f), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, WC, 1f))
            row.addView(btn)
            root.addView(row, LinearLayout.LayoutParams(MP, WC))
        }
    }

    // ------------------------------------------------------------ 实景地图入口

    private fun openMap() {
        dismiss()
        onOpenMap?.invoke()
    }

    /** 插件库顶部的「实景地图」卡片：左图标 + 标题副标题 + 打开 */
    private fun mapRow(): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.sdpI(2f), 0, ctx.sdpI(15f))
            isClickable = true
            setOnClickListener { openMap() }
        }

        row.addView(TextView(ctx).apply {
            text = "3D"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            setTextColor(Ui.color(ctx, R.color.tint_clay_fg))
            background = Ui.rounded(ctx, Ui.color(ctx, R.color.tint_clay_bg), 13f)
        }, LinearLayout.LayoutParams(ctx.sdpI(46f), ctx.sdpI(46f)))

        row.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.sdpI(12f), 0, 0, 0)
            addView(TextView(ctx).apply {
                text = "实景地图"
                textSize = 15.5f
                setTextColor(Pal.ink(ctx))
            })
            addView(TextView(ctx).apply {
                text = "男生宿舍 · 6F ｜ 3D 校园"
                mono(10.5f)
                setTextColor(Ui.color(ctx, R.color.ink_3))
                setPadding(0, ctx.sdpI(4f), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, WC, 1f))

        row.addView(TextView(ctx).apply {
            text = "打开"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Pal.onInk(ctx))
            background = Ui.rounded(ctx, Pal.ink(ctx), 999f)
            setPadding(ctx.sdpI(16f), ctx.sdpI(7f), ctx.sdpI(16f), ctx.sdpI(7f))
            isClickable = true
            setOnClickListener { openMap() }
            pressFeedback(0.94f)
        })

        return row
    }

    // ------------------------------------------------------------ 订阅

    fun vip() {
        root.addView(ctx.titleText("订阅服务 49RMB / 学期"))
        root.addView(sub("含校园网加速、云打印额度 200 页、洗衣房 9 折、饮水机无限畅饮。"))
        listOf("校园网加速", "云打印额度", "洗衣房折扣", "饮水无限畅饮").forEachIndexed { i, name ->
            if (i > 0) root.addView(View(ctx).apply { setBackgroundColor(Ui.color(ctx, R.color.line)) },
                LinearLayout.LayoutParams(MP, 1))
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, ctx.sdpI(14f), 0, ctx.sdpI(14f))
            }
            row.addView(TextView(ctx).apply {
                text = name
                textSize = 15.5f
                setTextColor(Pal.ink(ctx))
            }, LinearLayout.LayoutParams(0, WC, 1f))
            row.addView(TextView(ctx).apply {
                text = "✓"
                textSize = 14f
                setTextColor(Pal.ink(ctx))
            })
            root.addView(row, LinearLayout.LayoutParams(MP, WC))
        }
        root.addView(cta("立即订阅 49RMB") {
            dismiss()
            toast("已进入支付流程（Demo）")
        }, LinearLayout.LayoutParams(MP, WC).apply { topMargin = ctx.sdpI(22f) })
    }

}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
