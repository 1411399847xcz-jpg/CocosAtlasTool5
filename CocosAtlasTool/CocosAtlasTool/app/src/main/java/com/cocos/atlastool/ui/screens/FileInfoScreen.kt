package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.components.EmptyState
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.viewmodel.FileDetailInfo
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 文件信息屏幕
 *
 * 功能流程：
 * 1. 选择图集文件（Plist 或纹理）
 * 2. 解析并显示文件的详细元信息
 *
 * 显示的信息包括：
 * - 文件名称
 * - 文件大小
 * - 修改日期
 * - 文件类型
 * - 其他附加信息
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val fileInfoState by viewModel.fileInfoState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "文件信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 选择文件 =====
            FilePickerRow(
                label = "图集文件",
                filePath = fileInfoState.filePath,
                onChange = { onBrowseFile("file_info", listOf("plist", "png", "jpg", "webp")) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 文件详细信息 =====
            if (fileInfoState.filePath.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    title = "请先选择文件",
                    description = "支持 Plist、PNG、JPG、WebP 格式"
                )
            } else if (fileInfoState.details.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    title = "无法读取文件信息",
                    description = "该文件可能不是有效的图集文件"
                )
            } else {
                // 显示文件详细信息列表
                fileInfoState.details.forEach { detail ->
                    FileDetailCard(detail = detail)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 文件详情卡片
 *
 * 显示单个文件信息项。
 *
 * @param detail 文件详细信息
 */
@Composable
private fun FileDetailCard(detail: FileDetailInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 信息行
            DetailRow(label = "文件名称", value = detail.name)
            DetailRow(label = "文件大小", value = detail.size)
            DetailRow(label = "修改日期", value = detail.modifiedDate)
            DetailRow(label = "文件类型", value = detail.type)

            // 附加信息
            if (detail.extraInfo.isNotEmpty()) {
                DetailRow(label = "附加信息", value = detail.extraInfo)
            }
        }
    }
}

/**
 * 详情行组件
 *
 * @param label 标签
 * @param value 值
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
