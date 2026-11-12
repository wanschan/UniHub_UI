package com.example.applicationoneapp.ui

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.applicationoneapp.R
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdpI

/**
 * 实景地图：把校区 3D 模型（assets/map/campus.html）渲染在 WebView 里。
 *
 * 模型是 glTF（GLB 内联进 html），three.js 负责渲染，页面本身完全离线；
 * 顶部导航栏用原生控件写，保证和 App 其它页面同一套字号与描边风格。
 */
class CampusMapActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var loading: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.apply(this)
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Pal.bg(this@CampusMapActivity)) }

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(MP, MP))

        column.addView(buildHeader(), LinearLayout.LayoutParams(MP, WC))

        val stage = FrameLayout(this)
        column.addView(stage, LinearLayout.LayoutParams(MP, 0, 1f))

        web = WebView(this)
        stage.addView(web, FrameLayout.LayoutParams(MP, MP))
        setupWeb()

        loading = buildLoading()
        stage.addView(loading, FrameLayout.LayoutParams(MP, MP))

        setContentView(root)
        applyInsets(root)

        val path = "file:///android_asset/map/campus.html?dark=" + if (ThemePrefs.isDark(this)) "1" else "0"
        web.loadUrl(path)
    }

    // ------------------------------------------------------------ 顶栏

    private fun buildHeader(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(sdpI(18f), sdpI(8f), sdpI(18f), sdpI(10f))
        }

        val back = TextView(this).apply {
            text = "←"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Pal.ink(this@CampusMapActivity))
            background = Ui.rounded(
                this@CampusMapActivity, Color.TRANSPARENT, 999f,
                1.5f, Pal.line(this@CampusMapActivity), withStroke = true
            )
            isClickable = true
            contentDescription = "返回"
            setOnClickListener { finish() }
            pressFeedback(0.92f)
        }
        bar.addView(back, LinearLayout.LayoutParams(sdpI(38f), sdpI(38f)))

        bar.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sdpI(12f), 0, 0, 0)
            addView(TextView(this@CampusMapActivity).apply {
                text = "实景地图"
                textSize = 19f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Pal.ink(this@CampusMapActivity))
                includeFontPadding = false
            })
            addView(TextView(this@CampusMapActivity).apply {
                text = "拖动旋转 · 双指缩放 · 点楼层高亮"
                textSize = 11f
                setTextColor(Pal.ink3(this@CampusMapActivity))
                setPadding(0, sdpI(4f), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, WC, 1f))

        return bar
    }

    // ------------------------------------------------------------ WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWeb() {
        web.setBackgroundColor(Color.TRANSPARENT)
        web.overScrollMode = View.OVER_SCROLL_NEVER
        web.isVerticalScrollBarEnabled = false
        web.isHorizontalScrollBarEnabled = false

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) WebView.setWebContentsDebuggingEnabled(true)

        web.addJavascriptInterface(Bridge(), "AndroidMap")

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // JS 里也会回调一次，这里兜底，避免接口被禁时一直转圈
                view?.postDelayed({ hideLoading() }, 1200)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request?.isForMainFrame == true) {
                    showError("页面加载失败")
                }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                // WebView 进程被杀，避免整个 App 跟着崩
                showError("渲染进程已退出，请重进")
                return true
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            runOnUiThread { hideLoading() }
        }

        @JavascriptInterface
        fun onError(msg: String) {
            runOnUiThread { showError("模型加载失败") }
        }
    }

    // ------------------------------------------------------------ 加载态

    private fun buildLoading(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Pal.bg(this@CampusMapActivity))
        addView(ProgressBar(this@CampusMapActivity).apply {
            indeterminateTintList = Ui.csl(Pal.ink4(this@CampusMapActivity))
        }, LinearLayout.LayoutParams(sdpI(26f), sdpI(26f)))
        addView(TextView(this@CampusMapActivity).apply {
            text = "正在加载校园模型"
            textSize = 12.5f
            setTextColor(Pal.ink3(this@CampusMapActivity))
            setPadding(0, sdpI(14f), 0, 0)
        })
    }

    private fun hideLoading() {
        if (loading.visibility != View.VISIBLE) return
        loading.animate().alpha(0f).setDuration(280).withEndAction {
            loading.visibility = View.GONE
        }.start()
    }

    private fun showError(msg: String) {
        loading.animate().cancel()
        loading.visibility = View.VISIBLE
        loading.alpha = 1f
        loading.removeAllViews()
        loading.addView(TextView(this).apply {
            text = msg
            textSize = 13f
            setTextColor(Pal.ink2(this@CampusMapActivity))
        })
    }

    // ------------------------------------------------------------ 边距

    private fun applyInsets(root: View) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        val light = !ThemePrefs.isDark(this)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }
    }

    // ------------------------------------------------------------ 生命周期

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.onPause()
        web.pauseTimers()
        super.onPause()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.route_in, R.anim.route_out)
    }
}

private const val MP = -1
private const val WC = -2
