// 应用模块构建脚本
// 配置 Android 应用、Jetpack Compose 及相关依赖

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.cocos.atlastool"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cocos.atlastool"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // 测试运行器配置
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 向量图标配置
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java/Kotlin 编译版本
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 构建特性：启用 Compose
    buildFeatures {
        compose = true
    }

    composeOptions {
        // Kotlin 编译器扩展版本，需与 Kotlin 版本匹配
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // 打包选项：排除重复的 META-INF 文件
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Compose BOM 统一管理 Compose 版本
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ===== Compose UI 组件 =====
    // Compose UI 核心库
    implementation("androidx.compose.ui:ui")
    // Compose UI 工具库（预览支持）
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material Design 3 组件库
    implementation("androidx.compose.material3:material3")
    // Compose Foundation（手势、布局基础）
    implementation("androidx.compose.foundation:foundation")

    // ===== Compose Navigation 导航组件 =====
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ===== AndroidX 核心库 =====
    // Kotlin 核心扩展
    implementation("androidx.core:core-ktx:1.12.0")
    // Activity Compose 集成
    implementation("androidx.activity:activity-compose:1.8.2")
    // Lifecycle ViewModel Compose 集成
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // Lifecycle Runtime Compose 集成
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // ===== Kotlin 协程 =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ===== 测试依赖 =====
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Material Icons 扩展图标库
    implementation("androidx.compose.material:material-icons-extended")
}
