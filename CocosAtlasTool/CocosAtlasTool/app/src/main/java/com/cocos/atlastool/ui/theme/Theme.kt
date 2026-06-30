package com.cocos.atlastool.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 主题定义
 *
 * 提供 AtlasToolTheme Composable 函数，支持：
 * - 浅色主题
 * - 深色主题
 * - Android 12+ 动态配色（Material You）
 * - 状态栏颜色自动适配
 */

/** 浅色配色方案 */
private val LightColorScheme = lightColorScheme(
    primary = Indigo60,
    onPrimary = LightOnPrimary,
    primaryContainer = Indigo10,
    onPrimaryContainer = Indigo40,

    secondary = Teal60,
    onSecondary = LightOnSecondary,
    secondaryContainer = Teal10,
    onSecondaryContainer = Teal40,

    tertiary = Amber60,
    tertiaryContainer = Amber10,
    onTertiaryContainer = Amber40,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    error = Error60,
    onError = LightOnPrimary,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Error40
)

/** 深色配色方案 */
private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = DarkOnPrimary,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo20,

    secondary = Teal80,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal20,

    tertiary = Amber80,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber20,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = Error80,
    onError = Color(0xFF1C1917),
    errorContainer = Error40,
    onErrorContainer = Color(0xFFFEE2E2)
)

/**
 * AtlasTool 自定义主题
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param dynamicColor 是否启用 Android 12+ 动态配色，默认启用
 * @param content 主题包裹的子内容
 */
@Composable
fun AtlasToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // 根据条件选择配色方案
    val colorScheme = when {
        // Android 12+ 支持动态配色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // 低版本使用预定义配色
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 自动设置状态栏颜色，使状态栏与应用主题一致
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色为透明，让背景色自然延伸
            window.statusBarColor = colorScheme.primary.toArgb()
            // 控制状态栏图标颜色（亮色图标）
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
