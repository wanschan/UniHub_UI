package com.example.applicationoneapp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.applicationoneapp.MainActivity
import com.example.applicationoneapp.R
import com.example.applicationoneapp.auth.AuthConfig
import com.example.applicationoneapp.auth.AuthStore
import com.example.applicationoneapp.auth.BrevoMailer
import com.example.applicationoneapp.common.Pal
import com.example.applicationoneapp.common.ThemePrefs
import com.example.applicationoneapp.common.Ui
import com.example.applicationoneapp.common.Ui.mono
import com.example.applicationoneapp.ui.widget.InkBlobs
import com.example.applicationoneapp.ui.widget.ToastView
import com.example.applicationoneapp.ui.widget.pressFeedback
import com.example.applicationoneapp.ui.widget.sdp
import com.example.applicationoneapp.ui.widget.sdpI
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 登录页：邮箱 + 6 位验证码。
 *
 * 与开场页分工：开场页每次启动都播，登录页只在未登录时出现。
 * 视觉上承接开场页的终点（白底、墨色晕染、顶部 Hello），所以转场是淡入而不是滑入。
 */
class LoginActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val random = SecureRandom()

    private lateinit var col: LinearLayout
    private lateinit var sub: TextView
    private lateinit var emailBox: LinearLayout
    private lateinit var emailField: EditText
    private lateinit var changeEmail: TextView
    private lateinit var codeSection: LinearLayout
    private lateinit var codeBox: LinearLayout
    private lateinit var codeField: EditText
    private lateinit var resend: TextView
    private lateinit var primary: TextView
    private lateinit var errorView: TextView
    private lateinit var toast: ToastView
    private lateinit var gapView: View
    private lateinit var createHint: TextView

    private var step = 1
    private var busy = false
    private var pendingCode: String? = null
    private var pendingEmail: String? = null
    private var expireAt = 0L
    private var attempts = 0
    private var countdown = 0

    /** debug 包允许在邮件发不出时把验证码显示在界面上，方便联调 */
    private val debuggable: Boolean by lazy {
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.apply(this)
        super.onCreate(savedInstanceState)
        buildUi()
        restoreState(savedInstanceState)
        applyBars()
        animateIn()
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Pal.bg(this@LoginActivity)) }
        root.addView(InkBlobs(this).apply { reveal(false) }, FrameLayout.LayoutParams(MP, MP))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }
        root.addView(scroll, FrameLayout.LayoutParams(MP, MP))
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Android 15+ 强制 edge-to-edge，adjustResize 不再缩小窗口，键盘高度要自己让
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, sys.top, 0, maxOf(sys.bottom, ime.bottom))
            insets
        }

        col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sdpI(26f), sdpI(30f), sdpI(26f), sdpI(18f))
        }
        scroll.addView(col, FrameLayout.LayoutParams(MP, WC))

        // ---- 顶部：承接开场页的 Hello
        col.addView(TextView(this).apply {
            tag = "hello"
            text = "Hello"
            setTextColor(Pal.ink(this@LoginActivity))
            textSize = 64f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = -0.04f
            includeFontPadding = false
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MP, WC))

        col.addView(TextView(this).apply {
            text = "UNIHUB"
            mono(9.5f)
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_3))
            letterSpacing = 0.34f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MP, WC).apply { topMargin = sdpI(10f) })

        sub = TextView(this).apply {
            text = "用校园邮箱接收验证码，免记密码"
            textSize = 13.5f
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_2))
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.35f)
        }
        col.addView(sub, LinearLayout.LayoutParams(MP, WC).apply { topMargin = sdpI(10f) })

        // 标题与表单之间留一段固定的呼吸区；第二步会压缩它给键盘让位
        gapView = View(this)
        col.addView(gapView, LinearLayout.LayoutParams(MP, sdpI(58f)))

        // ---- 表单
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "form"
        }
        col.addView(form, LinearLayout.LayoutParams(MP, WC))

        form.addView(fieldLabel("邮箱"), LinearLayout.LayoutParams(MP, WC))

        emailBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(sdpI(16f), 0, sdpI(12f), 0)
            background = fieldBg(false)
            setOnClickListener { emailField.requestFocus() }
        }
        form.addView(emailBox, LinearLayout.LayoutParams(MP, sdpI(56f)).apply { topMargin = sdpI(8f) })

        emailField = EditText(this).apply {
            background = null
            hint = "name@university.edu"
            setHintTextColor(Ui.color(this@LoginActivity, R.color.ink_4))
            setTextColor(Pal.ink(this@LoginActivity))
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            includeFontPadding = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(0, 0, 0, 0)
        }
        emailBox.addView(emailField, LinearLayout.LayoutParams(0, MP, 1f))

        changeEmail = TextView(this).apply {
            text = "更换"
            mono(10.5f)
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_2))
            setPadding(sdpI(10f), sdpI(6f), 0, sdpI(6f))
            visibility = View.GONE
            isClickable = true
            setOnClickListener { backToStep1() }
        }
        emailBox.addView(changeEmail, LinearLayout.LayoutParams(WC, WC))

        // ---- 验证码区（第二步才显示）
        codeSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        form.addView(codeSection, LinearLayout.LayoutParams(MP, WC))

        codeSection.addView(fieldLabel("验证码"), LinearLayout.LayoutParams(MP, WC).apply { topMargin = sdpI(18f) })

        codeBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(sdpI(16f), 0, sdpI(16f), 0)
            background = fieldBg(false)
            setOnClickListener { codeField.requestFocus() }
        }
        codeSection.addView(codeBox, LinearLayout.LayoutParams(MP, sdpI(58f)).apply { topMargin = sdpI(8f) })

        codeField = EditText(this).apply {
            background = null
            setTextColor(Pal.ink(this@LoginActivity))
            textSize = 22f
            typeface = Ui.MONO
            letterSpacing = 0.36f
            gravity = Gravity.CENTER
            isSingleLine = true
            includeFontPadding = false
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            filters = arrayOf(InputFilter.LengthFilter(6))
            setPadding(0, 0, 0, 0)
        }
        codeBox.addView(codeField, LinearLayout.LayoutParams(0, MP, 1f))

        resend = TextView(this).apply {
            text = "重新发送验证码"
            mono(10.5f)
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_2))
            gravity = Gravity.CENTER
            setPadding(0, sdpI(14f), 0, sdpI(4f))
            isClickable = true
            setOnClickListener { if (!busy && countdown <= 0) sendCode() }
        }
        codeSection.addView(resend, LinearLayout.LayoutParams(MP, WC))

        // ---- 错误提示
        errorView = TextView(this).apply {
            textSize = 12.5f
            setTextColor(Pal.accent(this@LoginActivity))
            setLineSpacing(0f, 1.4f)
            visibility = View.GONE
        }
        form.addView(errorView, LinearLayout.LayoutParams(MP, WC).apply { topMargin = sdpI(12f) })

        // ---- 主按钮
        primary = TextView(this).apply {
            text = "获取验证码"
            gravity = Gravity.CENTER
            textSize = 16.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Pal.onInk(this@LoginActivity))
            background = Ui.rounded(this@LoginActivity, Pal.ink(this@LoginActivity), 15f)
            isClickable = true
            setOnClickListener { onPrimary() }
        }
        primary.pressFeedback()
        form.addView(primary, LinearLayout.LayoutParams(MP, sdpI(54f)).apply { topMargin = sdpI(10f) })

        createHint = TextView(this).apply {
            text = "首次使用会自动创建账号，无需单独注册"
            textSize = 11f
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_3))
            gravity = Gravity.CENTER
            setPadding(0, sdpI(12f), 0, 0)
        }
        form.addView(createHint, LinearLayout.LayoutParams(MP, WC))

        // 按钮以下的剩余空间全给留白，条款贴底
        col.addView(View(this), LinearLayout.LayoutParams(MP, 0, 1f))

        // ---- 条款
        col.addView(TextView(this).apply {
            text = "继续即代表同意《用户协议》与《隐私政策》"
            textSize = 10.5f
            setTextColor(Ui.color(this@LoginActivity, R.color.ink_3))
            gravity = Gravity.CENTER
            setPadding(0, sdpI(18f), 0, 0)
            isClickable = true
            setOnClickListener { toast.show("《用户协议》与《隐私政策》（Demo）") }
        }, LinearLayout.LayoutParams(MP, WC))

        toast = ToastView(this)
        root.addView(toast, FrameLayout.LayoutParams(WC, WC).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = sdpI(96f)
        })

        setContentView(root)

        emailField.setOnFocusChangeListener { _, has -> emailBox.background = fieldBg(has) }
        codeField.setOnFocusChangeListener { _, has -> codeBox.background = fieldBg(has) }

        emailField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && step == 1) { sendCode(); true } else false
        }

        codeField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) == 6 && step == 2 && !busy) verify()
            }
        })
    }

    private fun fieldLabel(text: String) = TextView(this).apply {
        this.text = text
        mono(9.5f)
        setTextColor(Ui.color(this@LoginActivity, R.color.ink_3))
        letterSpacing = 0.16f
    }

    private fun fieldBg(focused: Boolean) = Ui.rounded(
        this,
        Pal.card(this),
        15f,
        if (focused) 1.6f else 1.2f,
        if (focused) Pal.ink(this) else Ui.color(this, R.color.line),
        withStroke = true,
    )

    /** 只让表单上浮：标题与背景在窗口出现的第一帧就与开场页一致，避免转场时闪一下空白 */
    private fun animateIn() {
        val form = col.findViewWithTag<View>("form") ?: return
        form.alpha = 0f
        form.translationY = sdp(18f)
        form.animate().alpha(1f).translationY(0f).setDuration(460)
            .setStartDelay(60)
            .setInterpolator(android.view.animation.PathInterpolator(0.22f, 0.9f, 0.28f, 1f))
            .start()
    }

    private fun applyBars() {
        val light = !ThemePrefs.isDark(this)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
    }

    // --------------------------------------------------------------- 交互

    private fun onPrimary() {
        if (busy) return
        if (step == 1) sendCode() else verify()
    }

    private fun sendCode() {
        val email = emailField.text.toString().trim()
        if (!isEmail(email)) {
            showError("请输入有效的邮箱地址")
            emailField.requestFocus()
            return
        }

        val code = String.format(Locale.US, "%06d", random.nextInt(1_000_000))
        pendingCode = code
        pendingEmail = email
        expireAt = System.currentTimeMillis() + AuthConfig.CODE_TTL_MINUTES * 60_000L
        attempts = 0

        setBusy(true, if (step == 1) "发送中…" else "重新发送中…")
        hideError()

        io.execute {
            val result = BrevoMailer.sendCode(email, code, AuthConfig.CODE_TTL_MINUTES)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setBusy(false, if (step == 2) "登录" else "获取验证码")

                if (result.ok) {
                    enterStep2(clearCode = true)
                    toast.show("验证码已发送，请查收邮件")
                } else if (debuggable) {
                    // 邮件没发出去也要能联调：把本次生成的验证码直接显示出来
                    enterStep2(clearCode = true, sent = false)
                    showError("邮件未发出：${result.message}\n调试验证码：$code（仅 debug 构建显示）")
                } else {
                    showError("发送失败：${result.message}")
                }
            }
        }
    }

    private fun verify() {
        val input = codeField.text.toString().trim()
        if (input.length != 6) {
            showError("请输入 6 位验证码")
            return
        }
        val code = pendingCode
        if (code.isNullOrEmpty()) {
            showError("请先获取验证码")
            return
        }
        if (System.currentTimeMillis() > expireAt) {
            showError("验证码已过期，请重新获取")
            return
        }

        attempts++
        if (input != code) {
            val left = AuthConfig.MAX_ATTEMPTS - attempts
            if (left <= 0) {
                pendingCode = null
                showError("错误次数过多，请重新获取验证码")
            } else {
                showError("验证码不正确，还可以尝试 $left 次")
                shake(codeBox)
            }
            return
        }

        val email = pendingEmail ?: return
        hideError()
        setBusy(true, "登录中…")
        AuthStore.signIn(this, email)
        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            goMain()
        }, 240)
    }

    private fun enterStep2(clearCode: Boolean, restore: Boolean = false, sent: Boolean = true) {
        if (step == 1) {
            step = 2
            changeEmail.visibility = View.VISIBLE
            emailField.isEnabled = false
            emailField.setTextColor(Ui.color(this, R.color.ink_2))
            sub.text = if (sent) "验证码已发送至 ${pendingEmail.orEmpty()}" else "邮件未能发出"
            primary.text = "登录"
            createHint.visibility = View.GONE
            setGap(16f, true)
            codeSection.visibility = View.VISIBLE
            codeSection.alpha = 0f
            codeSection.translationY = sdp(14f)
            codeSection.animate().alpha(1f).translationY(0f).setDuration(300).start()
        }
        if (clearCode) codeField.setText("")
        if (restore) {
            // 旋转屏幕后不重新计时，直接允许重发
            countdown = 0
            resend.isEnabled = true
            resend.alpha = 1f
            resend.text = "重新发送验证码"
        } else {
            startCountdown()
            codeField.requestFocus()
            showKeyboard(codeField)
        }
    }

    private fun backToStep1() {
        if (busy) return
        step = 1
        handler.removeCallbacks(tick)
        countdown = 0
        pendingCode = null
        pendingEmail = null
        changeEmail.visibility = View.GONE
        emailField.isEnabled = true
        emailField.setTextColor(Pal.ink(this))
        codeSection.visibility = View.GONE
        codeField.setText("")
        primary.text = "获取验证码"
        sub.text = "用校园邮箱接收验证码，免记密码"
        createHint.visibility = View.VISIBLE
        setGap(58f, true)
        hideError()
        emailField.requestFocus()
    }

    /** 第二步表单更高，把标题下的留白压掉，避免按钮被键盘挡住 */
    private fun setGap(dp: Float, animate: Boolean) {
        val target = sdpI(dp)
        val params = gapView.layoutParams as LinearLayout.LayoutParams
        if (!animate) {
            params.height = target
            gapView.layoutParams = params
            return
        }
        android.animation.ValueAnimator.ofInt(params.height, target).apply {
            duration = 260
            addUpdateListener { anim ->
                val lp = gapView.layoutParams as LinearLayout.LayoutParams
                lp.height = anim.animatedValue as Int
                gapView.layoutParams = lp
            }
            start()
        }
    }

    private fun setBusy(value: Boolean, label: String? = null) {
        busy = value
        primary.isEnabled = !value
        primary.alpha = if (value) 0.55f else 1f
        if (label != null) primary.text = label
    }

    private fun showError(msg: String?) {
        if (msg.isNullOrBlank()) {
            errorView.visibility = View.GONE
            errorView.text = ""
        } else {
            errorView.text = msg
            errorView.visibility = View.VISIBLE
        }
    }

    private fun hideError() = showError(null)

    private fun shake(v: View) {
        android.animation.ObjectAnimator.ofFloat(v, "translationX", 0f, -9f, 9f, -6f, 6f, 0f).apply {
            duration = 340
            start()
        }
    }

    private fun showKeyboard(v: View) {
        v.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return@postDelayed
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
        }, 140)
    }

    private fun goMain() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        overridePendingTransition(R.anim.route_in, R.anim.route_out)
        finish()
    }

    private fun isEmail(s: String): Boolean =
        s.length in 5..254 && Regex("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$").matches(s)

    private val tick = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            if (countdown <= 0) {
                resend.isEnabled = true
                resend.alpha = 1f
                resend.text = "重新发送验证码"
                return
            }
            resend.isEnabled = false
            resend.alpha = 0.45f
            resend.text = "重新发送（${countdown}s）"
            countdown--
            handler.postDelayed(this, 1000)
        }
    }

    private fun startCountdown() {
        handler.removeCallbacks(tick)
        countdown = AuthConfig.RESEND_SECONDS
        handler.post(tick)
    }

    // 旋转屏幕不丢验证码（验证码只存在内存里，重建时从 bundle 取回）
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, step)
        outState.putString(KEY_EMAIL, emailField.text.toString())
        outState.putString(KEY_PENDING_EMAIL, pendingEmail)
        outState.putString(KEY_PENDING_CODE, pendingCode)
        outState.putLong(KEY_EXPIRE, expireAt)
        outState.putInt(KEY_ATTEMPTS, attempts)
        outState.putString(KEY_CODE_INPUT, codeField.text.toString())
    }

    private fun restoreState(b: Bundle?) {
        if (b == null) return
        b.getString(KEY_EMAIL)?.takeIf { it.isNotBlank() }?.let { emailField.setText(it) }

        val code = b.getString(KEY_PENDING_CODE)
        val exp = b.getLong(KEY_EXPIRE, 0L)
        if (b.getInt(KEY_STEP, 1) == 2 && !code.isNullOrBlank() && exp > System.currentTimeMillis()) {
            pendingCode = code
            pendingEmail = b.getString(KEY_PENDING_EMAIL)
            expireAt = exp
            attempts = b.getInt(KEY_ATTEMPTS, 0)
            enterStep2(clearCode = false, restore = true)
            codeField.setText(b.getString(KEY_CODE_INPUT).orEmpty())
            codeField.setSelection(codeField.text.length)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        io.shutdownNow()
        super.onDestroy()
    }
}

private const val KEY_STEP = "login_step"
private const val KEY_EMAIL = "login_email"
private const val KEY_PENDING_EMAIL = "login_pending_email"
private const val KEY_PENDING_CODE = "login_pending_code"
private const val KEY_EXPIRE = "login_expire"
private const val KEY_ATTEMPTS = "login_attempts"
private const val KEY_CODE_INPUT = "login_code_input"

// 布局常量（避免与 import 的同名符号冲突）
// -1 / -2 即 MATCH_PARENT / WRAP_CONTENT，写成字面量才能声明成 const
private const val MP = -1
private const val WC = -2
