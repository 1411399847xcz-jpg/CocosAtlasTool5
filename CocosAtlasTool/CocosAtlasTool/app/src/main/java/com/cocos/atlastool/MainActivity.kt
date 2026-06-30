package com.cocos.atlastool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cocos.atlastool.ui.navigation.AppNavigation
import com.cocos.atlastool.ui.theme.AtlasToolTheme

/**
 * 主 Activity
 *
 * 单 Activity 架构，使用 Jetpack Compose 承载所有界面。
 * 通过 setContent 设置 Compose 主题和导航入口。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置 Compose 内容视图
        setContent {
            // AtlasTool 自定义主题
            AtlasToolTheme {
                // 主题表面容器，确保 Material 主题正确应用
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 应用导航入口
                    AppNavigation()
                }
            }
        }
    }
}
