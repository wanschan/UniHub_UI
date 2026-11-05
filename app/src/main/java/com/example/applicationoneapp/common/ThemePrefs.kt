package com.example.applicationoneapp.common

import androidx.core.content.edit
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题 / 语言偏好：与参考稿一致，支持 亮色 / 暗色 / 跟随系统，
 * 偏好存本地，下次打开依然生效。
 */
object ThemePrefs {

    private const val SP = "campus_link_prefs"
    private const val KEY_THEME = "cl-theme"
    private const val KEY_LANG = "cl-lang"

    const val LIGHT = "light"
    const val DARK = "dark"
    const val AUTO = "auto"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)

    fun theme(ctx: Context): String = sp(ctx).getString(KEY_THEME, LIGHT) ?: LIGHT

    fun lang(ctx: Context): String = sp(ctx).getString(KEY_LANG, AUTO) ?: AUTO

    fun setTheme(ctx: Context, value: String) {
        sp(ctx).edit { putString(KEY_THEME, value) }
        apply(ctx)
    }

    fun setLang(ctx: Context, value: String) {
        sp(ctx).edit { putString(KEY_LANG, value) }
        apply(ctx)
    }

    /** 在 Activity 创建前调用，保证首屏不闪白 */
    fun apply(ctx: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme(ctx)) {
                DARK -> AppCompatDelegate.MODE_NIGHT_YES
                AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    fun isDark(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun themeText(ctx: Context): String = when (theme(ctx)) {
        DARK -> "暗色"
        AUTO -> "跟随系统"
        else -> "亮色"
    }

    fun langText(ctx: Context): String = when (lang(ctx)) {
        "zh" -> "中文"
        "en" -> "English"
        else -> "跟随系统"
    }

    fun settingsSummary(ctx: Context) = "${langText(ctx)} · ${themeText(ctx)}"

    fun lightStatusBar(activity: Activity, lightIconsOnDarkBackground: Boolean) {
        val window = activity.window
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        val darkUi = isDark(activity)
        // 深色背景（开屏）+ 暗色主题：状态栏用浅色图标
        val lightBar = if (lightIconsOnDarkBackground) !darkUi else !darkUi
        controller.isAppearanceLightStatusBars = lightBar
        controller.isAppearanceLightNavigationBars = lightBar
    }
}
