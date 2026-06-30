package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.components.EmptyState
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.components.ProgressOverlay
import com.cocos.atlastool.ui.components.StatsCard
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 动画精简屏幕
 *
 * 支持单文件和批量两种模式：
 * - 单文件模式：选择一个 Plist 文件进行精简
 * - 批量模式：选择多个 Plist 文件或一个文件夹进行批量处理
 *
 * 操作流程：
 * 1. 切换单文件/批量模式
 * 2. 选择文件
 * 3. 选择输出目录
 * 4. 开始精简
 * 5. 显示进度和结果
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param onBrowseFolder 请求打开文件夹浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifyScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    onBrowseFolder: (requestId: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val simplifyState by viewModel.simplifyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "动画精简") },
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
        ) {
            // ===== 模式切换 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !simplifyState.isBatch,
                    onClick = {
                        viewModel.updateSimplifyState { it.copy(isBatch = false) }
                    },
                    label = { Text(text = "单文件") }
                )
                FilterChip(
                    selected = simplifyState.isBatch,
                    onClick = {
                        viewModel.updateSimplifyState { it.copy(isBatch = true) }
                    },
                    label = { Text(text = "批量") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 文件选择 / 列表展示 =====
            if (simplifyState.isBatch) {
                // 批量模式：选择文件夹或逐个添加文件
                FilePickerRow(
                    label = "选择文件夹",
                    filePath = "",
                    onChange = { onBrowseFolder("simplify_folder") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 已选文件列表
                if (simplifyState.selectedFiles.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Description,
                        title = "尚未选择文件",
                        description = "请选择包含 Plist 文件的文件夹"
                    )
                } else {
                    Text(
                        text = "已选文件 (${simplifyState.selectedFiles.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(simplifyState.selectedFiles) { filePath ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = filePath.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.updateSimplifyState {
                                            it.copy(
                                                selectedFiles = it.selectedFiles - filePath
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 单文件模式：选择单个 Plist 文件
                val singlePath = simplifyState.selectedFiles.firstOrNull() ?: ""
                FilePickerRow(
                    label = "Plist 文件",
                    filePath = singlePath,
                    onChange = { onBrowseFile("simplify_file", listOf("plist")) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 输出目录 =====
            FilePickerRow(
                label = "输出目录",
                filePath = simplifyState.outputPath,
                onChange = { onBrowseFolder("simplify_output") },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 操作按钮 / 进度 / 结果 =====
            when {
                simplifyState.isRunning -> {
                    // 处理中
                    ProgressOverlay(
                        progress = simplifyState.progress,
                        statusText = simplifyState.statusText,
                        onCancel = {
                            viewModel.updateSimplifyState { it.copy(isRunning = false) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                simplifyState.successCount > 0 || simplifyState.failCount > 0 -> {
                    // 完成状态：显示统计结果
                    StatsCard(
                        successCount = simplifyState.successCount,
                        failCount = simplifyState.failCount,
                        skipCount = 0,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetSimplifyState() },
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
                            // TODO: 调用 core 层执行精简操作
                            viewModel.updateSimplifyState {
                                it.copy(
                                    isRunning = true,
                                    progress = 0f,
                                    statusText = "正在精简..."
                                )
                            }
                        },
                        enabled = simplifyState.selectedFiles.isNotEmpty() &&
                                  simplifyState.outputPath.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "开始精简")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
