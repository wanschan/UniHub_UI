package com.example.applicationoneapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.ui.page.CirclesPage
import com.example.applicationoneapp.ui.page.HomePage
import com.example.applicationoneapp.ui.page.MePage
import com.example.applicationoneapp.ui.page.TimetablePage
import com.example.applicationoneapp.ui.sheet.Sheets
import com.example.applicationoneapp.ui.widget.PageShell
import com.example.applicationoneapp.ui.widget.SheetController
import com.example.applicationoneapp.ui.widget.TabBar
import com.example.applicationoneapp.ui.widget.ToastView
import com.example.applicationoneapp.ui.widget.sdpI

class MainActivity : AppCompatActivity() {

    private lateinit var pagesBox: FrameLayout
    private lateinit var tabBar: TabBar
    private lateinit var toast: ToastView
    private lateinit var pages: List<PageShell>
    private lateinit var mePage: MePage

    private var current = 0
    private var animating = false
    private val sheetController by lazy { SheetController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.apply(this)
        super.onCreate(savedInstanceState)
        buildUi()
        registerBackCallback()
    }

    /** 返回逻辑：先关弹层 → 回首页 → 退出（走 OnBackPressedDispatcher，兼容手势返回） */
    private fun registerBackCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    sheetController.isShowing() -> sheetController.dismiss()
                    current != 0 -> go(0)
                    else -> {
                        // 关掉自己再重新派发，让系统正常结束 Activity（含 predictive back 动画）
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun buildUi() {
        val rootView = FrameLayout(this)
        rootView.setBackgroundColor(Pal.bg(this))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rootView.addView(column, FrameLayout.LayoutParams(MP, MP))

        pagesBox = FrameLayout(this)
        column.addView(pagesBox, LinearLayout.LayoutParams(MP, 0, 1f))

        tabBar = TabBar(this)
        tabBar.onSelect = { index -> go(index) }
        column.addView(tabBar, LinearLayout.LayoutParams(MP, WC).apply {
            leftMargin = sdpI(14f)
            rightMargin = sdpI(14f)
            topMargin = sdpI(10f)
            bottomMargin = sdpI(16f)
        })

        toast = ToastView(this)
        rootView.addView(toast, FrameLayout.LayoutParams(WC, WC).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = sdpI(120f)
        })

        val home = HomePage(
            this,
            toast = { msg -> toast.show(msg) },
            onOpenTimetable = { go(1) },
            onGetPlugins = { showSheet { it.plugins() } },
            onMenu = { toast.show("更多入口即将开放（Demo）") },
        )
        val timetable = TimetablePage(this) { msg -> toast.show(msg) }
        val circles = CirclesPage(this) { msg -> toast.show(msg) }
        mePage = MePage(
            context = this,
            onSettings = { showSheet { it.settings() } },
            onFaq = { showSheet { it.faq() } },
            onAbout = { showSheet { it.about() } },
            onPrivacy = { showSheet { it.privacy() } },
            onContact = { showSheet { it.contact() } },
            onSponsor = { showSheet { it.sponsor() } },
            onVip = { showSheet { it.vip() } },
        )
        pages = listOf(home, timetable, circles, mePage)

        pages.forEach { page ->
            pagesBox.addView(page, FrameLayout.LayoutParams(MP, MP))
            page.alpha = 0f
            page.visibility = View.INVISIBLE
            page.sheet.translationX = resources.displayMetrics.widthPixels.toFloat()
        }
        pages[0].alpha = 1f
        pages[0].visibility = View.VISIBLE
        pages[0].sheet.translationX = 0f
        setContentView(rootView)
        setupSystemBars(rootView)
        tabBar.select(0)
    }

    private fun setupSystemBars(root: View) {
        // 状态栏 / 导航栏底色由主题（android:statusBarColor 等）声明为透明，
        // API 35+ 已废弃 Window.setStatusBarColor，不再在代码里设置。
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        val light = !ThemePrefs.isDark(this)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }
    }

    /** 打开底部弹层；每次新建 Sheets，往弹层容器里追加内容 */
    private fun showSheet(block: (Sheets) -> Unit) {
        sheetController.show { container ->
            val sheets = Sheets(this, { msg -> toast.show(msg) }, { sheetController.dismiss() })
            sheets.reset(container)
            block(sheets)
        }
    }

    /** 切换 Tab：标题区固定不动，只有下方纸面按方向平移 */
    private fun go(target: Int) {
        if (target !in pages.indices) return
        tabBar.select(target)
        if (target == current) return
        if (animating) {
            snapTo(target)
            return
        }

        val from = pages[current]
        val to = pages[target]
        val dir = if (target > current) 1 else -1
        val widthPx = resources.displayMetrics.widthPixels.toFloat()
        val path = android.view.animation.PathInterpolator(0.34f, 0.72f, 0.24f, 1f)

        to.body.scrollTo(0, 0)
        to.visibility = View.VISIBLE
        to.alpha = 1f
        to.sheet.translationX = dir * widthPx
        to.z = 3f
        from.z = 2f

        // 旧标题让位，避免两页标题叠在一起
        from.head.animate().alpha(0f).setDuration(130).start()
        to.head.alpha = 1f

        animating = true
        to.sheet.animate().translationX(0f).setDuration(420).setInterpolator(path)
            .withEndAction {
                from.visibility = View.INVISIBLE
                from.alpha = 0f
                from.head.alpha = 1f
                from.sheet.translationX = widthPx
                from.z = 1f
                animating = false
            }.start()
        from.sheet.animate().translationX(-dir * widthPx).setDuration(420).setInterpolator(path).start()

        current = target
        if (target == 3) mePage.refreshSettings()
    }

    private fun snapTo(target: Int) {
        val widthPx = resources.displayMetrics.widthPixels.toFloat()
        pages.forEachIndexed { i, page ->
            page.sheet.animate().cancel()
            page.visibility = if (i == target) View.VISIBLE else View.INVISIBLE
            page.alpha = if (i == target) 1f else 0f
            page.head.alpha = 1f
            page.sheet.translationX = if (i == target) 0f else widthPx
        }
        animating = false
        current = target
    }
}

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
