package com.cocos.atlastool

import android.app.Application

/**
 * CocosAtlasTool 应用全局 Application 类
 *
 * 用于初始化全局组件，例如：
 * - 依赖注入容器
 * - 全局崩溃处理器
 * - 第三方 SDK 初始化
 */
class AtlasToolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        // 全局 Application 实例，方便在非 Context 环境下获取
        lateinit var instance: AtlasToolApp
            private set
    }
}
