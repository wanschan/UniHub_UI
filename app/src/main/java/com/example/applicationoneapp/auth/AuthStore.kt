package com.example.applicationoneapp.auth

import android.content.Context
import androidx.core.content.edit

/**
 * 本地登录态。
 *
 * 说明：这是纯客户端的 Demo 登录，登录态只存在本机 SharedPreferences 里，
 * 换设备 / 清数据就要重新验证。真正的账号体系需要服务端签发 token。
 */
object AuthStore {

    private const val SP = "campus_link_auth"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_SINCE = "since"
    private const val KEY_METHOD = "method"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)

    fun isLoggedIn(ctx: Context): Boolean = !sp(ctx).getString(KEY_EMAIL, null).isNullOrBlank()

    fun email(ctx: Context): String = sp(ctx).getString(KEY_EMAIL, "") ?: ""

    fun since(ctx: Context): Long = sp(ctx).getLong(KEY_SINCE, 0L)

    fun method(ctx: Context): String = sp(ctx).getString(KEY_METHOD, "邮箱验证码") ?: "邮箱验证码"

    /** 昵称：优先用注册时填的，没有就从邮箱前缀推导（ruining.liu@x.edu → Ruining Liu） */
    fun displayName(ctx: Context): String {
        val saved = sp(ctx).getString(KEY_NAME, "") ?: ""
        if (saved.isNotBlank()) return saved
        val prefix = email(ctx).substringBefore('@')
        if (prefix.isBlank()) return ""
        return prefix
            .split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                if (part.first().isLetter() && part.first().code < 128) {
                    part.first().uppercase() + part.drop(1)
                } else {
                    part
                }
            }
            .ifBlank { prefix }
    }

    /** 头像用的首字 */
    fun initial(ctx: Context): String {
        val name = displayName(ctx).trim()
        return if (name.isBlank()) "?" else name.take(1).uppercase()
    }

    fun signIn(ctx: Context, email: String, name: String? = null, method: String = "邮箱验证码") {
        sp(ctx).edit(commit = true) {
            putString(KEY_EMAIL, email.trim().lowercase())
            putString(KEY_NAME, name?.trim().orEmpty())
            putString(KEY_METHOD, method)
            putLong(KEY_SINCE, System.currentTimeMillis())
        }
    }

    fun signOut(ctx: Context) {
        sp(ctx).edit { clear() }
    }
}
