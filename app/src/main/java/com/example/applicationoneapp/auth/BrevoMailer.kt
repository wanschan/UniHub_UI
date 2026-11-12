package com.example.applicationoneapp.auth

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用 Brevo 事务邮件接口发验证码。纯 HttpURLConnection，不引三方网络库。
 *
 * 必须在工作线程调用（内部是阻塞 IO）。
 */
object BrevoMailer {

    data class SendResult(val ok: Boolean, val message: String = "")

    fun sendCode(to: String, code: String, ttlMinutes: Int): SendResult {
        if (AuthConfig.BREVO_API_KEY.isBlank()) {
            return SendResult(false, "未配置 Brevo API Key")
        }
        if (AuthConfig.SENDER_EMAIL.isBlank()) {
            return SendResult(false, "未配置发件人邮箱")
        }

        val payload = JSONObject().apply {
            put("sender", JSONObject().apply {
                put("name", AuthConfig.SENDER_NAME)
                put("email", AuthConfig.SENDER_EMAIL)
            })
            put("to", JSONArray().put(JSONObject().put("email", to)))
            put("subject", "UniHub 登录验证码：$code")
            put("htmlContent", html(code, ttlMinutes))
            put("textContent", "UniHub 登录验证码：$code（$ttlMinutes 分钟内有效）")
            put("tags", JSONArray().put("login-otp"))
        }.toString()

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(AuthConfig.BREVO_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("api-key", AuthConfig.BREVO_API_KEY)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "UniHub-Android/1.0")
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (status in 200..299) {
                SendResult(true)
            } else {
                SendResult(false, describe(status, text))
            }
        } catch (e: Exception) {
            SendResult(false, "网络连接失败：${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn?.disconnect()
        }
    }

    /** 把 Brevo 的错误码翻译成用户能看懂的话 */
    private fun describe(status: Int, body: String): String {
        val raw = runCatching {
            val o = JSONObject(body)
            val code = o.optString("code")
            val msg = o.optString("message")
            if (msg.isBlank()) code else "$msg（$code）"
        }.getOrElse { body.take(120).ifBlank { "HTTP $status" } }

        return when {
            status == 401 -> "API Key 无效，请检查 local.properties 里的 brevo.apiKey"
            status == 403 -> "API Key 没有发信权限：需要在 Brevo 里勾选 Transactional emails"
            status == 400 && body.contains("sender", true) -> "发件人未验证：$raw"
            status >= 500 -> "Brevo 服务异常（$status），请稍后再试"
            else -> raw
        }
    }

    private fun html(code: String, ttl: Int): String = """
        <div style="margin:0;padding:32px 16px;background:#F2F2F4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
          <div style="max-width:480px;margin:0 auto;background:#111214;border-radius:20px;padding:36px 32px;">
            <div style="font-size:11px;letter-spacing:3px;color:#8B8F96;">UNIHUB</div>
            <div style="font-size:26px;font-weight:600;color:#F4F5F7;margin:14px 0 8px;">登录验证码</div>
            <div style="font-size:13px;line-height:1.7;color:#B6BAC1;">
              你正在登录 UniHub 校园服务平台，请在应用中输入下面的 6 位验证码。
            </div>
            <div style="margin:26px 0;padding:22px 12px;background:#1C1E23;border-radius:14px;text-align:center;">
              <span style="font-family:ui-monospace,Menlo,Consolas,monospace;font-size:34px;font-weight:700;letter-spacing:9px;color:#FFFFFF;">$code</span>
            </div>
            <div style="font-size:12px;line-height:1.8;color:#8B8F96;">
              验证码 $ttl 分钟内有效，请勿转发给他人。<br>
              若非本人操作，忽略此邮件即可。
            </div>
          </div>
          <div style="max-width:480px;margin:16px auto 0;text-align:center;font-size:11px;color:#9A9EA5;">
            此邮件由系统自动发送，请勿直接回复。
          </div>
        </div>
    """.trimIndent()
}
