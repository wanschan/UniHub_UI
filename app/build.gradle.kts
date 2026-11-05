plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.applicationoneapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.applicationoneapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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