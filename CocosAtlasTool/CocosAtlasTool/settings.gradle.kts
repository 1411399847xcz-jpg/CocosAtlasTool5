// 根项目设置文件
// 用于配置 Gradle 多项目构建结构

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// 根项目名称
rootProject.name = "CocosAtlasTool"

// 包含 app 模块
include(":app")
