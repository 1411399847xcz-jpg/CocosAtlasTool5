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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.components.ProgressOverlay
import com.cocos.atlastool.ui.components.StatsCard
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 图片去白边屏幕
 *
 * 功能流程：
 * 1. 选择包含图片的文件夹
 * 2. 显示找到的图片数量
 * 3. 执行去白边操作
 * 4. 显示进度条和统计结果
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFolder 请求打开文件夹浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimImagesScreen(
    onBack: () -> Unit,
    onBrowseFolder: (requestId: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val trimState by viewModel.trimImagesState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "图片去白边") },
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
            // ===== 说明文字 =====
            Text(
                text = "去除图片四周的空白/透明区域，支持 PNG 和 JPG 格式。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 选择文件夹 =====
            FilePickerRow(
                label = "图片文件夹",
                filePath = trimState.folderPath,
                onChange = { onBrowseFolder("trim_folder") },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 找到的图片数量 =====
            if (trimState.folderPath.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "找到的图片数量: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = trimState.imageCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ===== 操作按钮 / 进度 / 结果 =====
            when {
                trimState.isRunning -> {
                    // 处理中：显示进度
                    ProgressOverlay(
                        progress = trimState.progress,
                        statusText = trimState.statusText,
                        onCancel = {
                            viewModel.updateTrimImagesState { it.copy(isRunning = false) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                trimState.successCount > 0 || trimState.failCount > 0 -> {
                    // 完成状态：显示统计结果
                    StatsCard(
                        successCount = trimState.successCount,
                        failCount = trimState.failCount,
                        skipCount = 0,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 结果摘要
                    Text(
                        text = "共处理 ${trimState.successCount + trimState.failCount} 张图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.resetTrimImagesState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "重新开始")
                    }
                }
                else -> {
                    // 开始按钮
                    Button(
                        onClick = {
                            // TODO: 调用 core 层执行去白边操作
                            viewModel.updateTrimImagesState {
                                it.copy(
                                    isRunning = true,
                                    progress = 0f,
                                    statusText = "正在处理..."
                                )
                            }
                        },
                        enabled = trimState.folderPath.isNotEmpty() && trimState.imageCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "开始去白边")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
