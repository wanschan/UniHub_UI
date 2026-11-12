package com.example.applicationoneapp.auth

import com.example.applicationoneapp.BuildConfig

/**
 * 登录相关配置。
 *
 * ⚠️ 安全提示：把 Brevo 的 API Key 放在客户端，等于公开它——
 * APK 可以被反编译，Key 可以直接被拿走发垃圾邮件。所以：
 *
 * 1. 真实 Key **不进源码、不进仓库**，写在 `local.properties` 的 `brevo.apiKey` 里，
 *    由 Gradle 注入到 [BuildConfig]，本文件只读取。
 * 2. 仓库里永远不会出现真实 Key；别人 clone 下来构建时该值为空，
 *    App 会提示「未配置 Brevo API Key」，不会崩溃。
 * 3. 正式上线前应当把「生成验证码 + 调 Brevo」搬到自己的服务器（或用云函数），
 *    App 只负责「请求验证码 / 校验验证码」。当前写法仅适用于课程 Demo。
 */
object AuthConfig {

    /**
     * Brevo API Key，来源：`local.properties` → `brevo.apiKey`
     * （或环境变量 `BREVO_API_KEY`）。
     */
    val BREVO_API_KEY: String = BuildConfig.BREVO_API_KEY

    /**
     * 发件人。**必须**是 Brevo 里已验证的发件人，否则接口返回 400 "sender not valid"。
     *
     * 默认用 wanschan@outlook.com（Verified）。它是免费邮箱域名，
     * Brevo 会提示 DKIM / DMARC 不达标：发到 Outlook 自己没问题，发 Gmail / QQ 有进垃圾箱的风险。
     * 以后有域名了，在 Brevo 里验证域名并把 `brevo.senderEmail` 换成 xxx@你的域名 即可。
     */
    val SENDER_EMAIL: String = BuildConfig.BREVO_SENDER_EMAIL
    val SENDER_NAME: String = BuildConfig.BREVO_SENDER_NAME

    /** 验证码有效期（分钟） */
    const val CODE_TTL_MINUTES = 5

    /** 重发冷却（秒） */
    const val RESEND_SECONDS = 60

    /** 同一验证码允许输错的次数 */
    const val MAX_ATTEMPTS = 5

    const val BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email"
}
