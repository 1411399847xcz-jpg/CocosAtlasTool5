// 根项目构建脚本
// 声明全局插件版本和公共仓库配置

// Android Gradle 插件版本
buildscript {
    val agpVersion = "8.2.0"
    val kotlinVersion = "1.9.22"

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // Android Gradle Plugin
        classpath("com.android.tools.build:gradle:$agpVersion")
        // Kotlin 编译器插件
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

// 所有子项目共享的仓库配置
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
