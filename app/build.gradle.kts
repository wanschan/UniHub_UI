import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 登录验证码用的 Brevo 凭据放在 local.properties（不入库），
// 通过 BuildConfig 注入，避免把 API Key 明文写进源码再推到公开仓库。
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val brevoApiKey = localProps.getProperty("brevo.apiKey")
    ?: System.getenv("BREVO_API_KEY").orEmpty()
val brevoSenderEmail = localProps.getProperty("brevo.senderEmail")
    ?: "wanschan@outlook.com"
val brevoSenderName = localProps.getProperty("brevo.senderName")
    ?: "UniHub"

android {
    namespace = "com.example.applicationoneapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.applicationoneapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "26w46a"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 运行时从 BuildConfig 读取，源码里不再出现真实 Key
        buildConfigField("String", "BREVO_API_KEY", brevoApiKey.asBuildConfigString())
        buildConfigField("String", "BREVO_SENDER_EMAIL", brevoSenderEmail.asBuildConfigString())
        buildConfigField("String", "BREVO_SENDER_NAME", brevoSenderName.asBuildConfigString())
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
        buildConfig = true
    }
    lint {
        // 本项目的界面文案全部由 Kotlin 代码构建（没有 layout XML），且只做中文单语种，
        // 不打算翻译，所以关闭「硬编码文案」「缺翻译」这类提示，避免刷屏。
        disable += "HardcodedText"
        disable += "SetTextI18n"
        disable += "MissingTranslation"
        // 其余检查保持开启，有 error 时照常中断构建
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}