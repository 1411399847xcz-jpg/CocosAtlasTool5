package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 主屏幕 — 功能菜单
 *
 * 以卡片网格布局展示所有功能入口，每个功能包含：
 * - 功能图标
 * - 功能标题
 * - 简短描述
 *
 * 顶部显示应用标题和版本号，底部显示版本信息。
 */

/** 应用版本号 */
private const val APP_VERSION = "v1.0.0"

/**
 * 功能菜单项数据类
 *
 * @param id 功能唯一标识（对应导航路由）
 * @param icon 功能图标
 * @param title 功能标题
 * @param description 功能简短描述
 */
data class MenuItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String
)

/** 功能菜单列表 */
val menuItems = listOf(
    MenuItem(
        id = "crop_single",
        icon = Icons.Default.ContentCut,
        title = "单文件拆分",
        description = "选择单个 plist 文件拆分图集精灵"
    ),
    MenuItem(
        id = "crop_batch",
        icon = Icons.Default.PhotoLibrary,
        title = "批量拆分",
        description = "批量处理多个 plist 图集文件"
    ),
    MenuItem(
        id = "grid_split",
        icon = Icons.Default.GridOn,
        title = "网格切割",
        description = "按行列数切割纹理为等分小块"
    ),
    MenuItem(
        id = "view_frames",
        icon = Icons.Default.Image,
        title = "帧详情",
        description = "查看 plist 中所有帧的详细信息"
    ),
    MenuItem(
        id = "file_info",
        icon = Icons.Default.Info,
        title = "文件信息",
        description = "查看图集文件的详细元信息"
    ),
    MenuItem(
        id = "export_texture",
        icon = Icons.Default.FolderOpen,
        title = "导出纹理",
        description = "导出纹理图片到指定格式"
    ),
    MenuItem(
        id = "crack_center",
        icon = Icons.Default.Key,
        title = "密钥破解",
        description = "破解 CCZ 文件的加密密钥"
    ),
    MenuItem(
        id = "simplify",
        icon = Icons.Default.Movie,
        title = "动画精简",
        description = "精简动画帧，去除冗余帧数据"
    ),
    MenuItem(
        id = "trim_images",
        icon = Icons.Default.ContentCut,
        title = "去空白",
        description = "去除图片四周的空白区域"
    ),
    MenuItem(
        id = "xml_crypto",
        icon = Icons.Default.Lock,
        title = "XML加解密",
        description = "对 XML 文件进行加密或解密操作"
    ),
    MenuItem(
        id = "settings",
        icon = Icons.Default.Settings,
        title = "设置",
        description = "配置应用参数和输出选项"
    )
)

/**
 * 主屏幕 Composable
 *
 * @param onNavigate 点击菜单项时的导航回调，传入目标路由
 * @param modifier 布局修饰符
 */
@Composable
fun MainScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ===== 顶部：应用标题 + 版本号 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Cocos 图集拆分工具",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = APP_VERSION,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 卡片网格布局（2列） =====
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { item ->
                MenuCard(
                    menuItem = item,
                    onClick = { onNavigate(item.id) }
                )
            }
        }

        // ===== 底部：版本信息 =====
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CocosAtlasTool $APP_VERSION | Built with Jetpack Compose",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * 功能菜单卡片
 *
 * 显示单个功能入口，包含图标、标题和描述。
 *
 * @param menuItem 菜单项数据
 * @param onClick 卡片点击回调
 * @param modifier 布局修饰符
 */
@Composable
private fun MenuCard(
    menuItem: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 功能图标
            Icon(
                imageVector = menuItem.icon,
                contentDescription = menuItem.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 功能标题
            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 功能描述
            Text(
                text = menuItem.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        }
    }
}
