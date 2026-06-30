package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.components.ProgressOverlay
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 导出纹理屏幕
 *
 * 功能流程：
 * 1. 选择纹理文件（Plist 配对的 PNG/JPG）
 * 2. 选择输出目录
 * 3. 执行导出
 * 4. 显示进度
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param onBrowseFolder 请求打开文件夹浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTextureScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    onBrowseFolder: (requestId: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val exportState by viewModel.exportTextureState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "导出纹理") },
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
                text = "将图集纹理导出为独立的图片文件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 选择纹理文件 =====
            FilePickerRow(
                label = "纹理文件",
                filePath = exportState.filePath,
                onChange = { onBrowseFile("export_texture", listOf("png", "jpg", "webp")) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 输出目录 =====
            FilePickerRow(
                label = "输出目录",
                filePath = exportState.outputPath,
                onChange = { onBrowseFolder("export_output") },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 操作按钮 / 进度 =====
            if (exportState.isRunning) {
                // 处理中：显示进度
                ProgressOverlay(
                    progress = exportState.progress,
                    statusText = exportState.statusText,
                    onCancel = {
                        viewModel.updateExportTextureState { it.copy(isRunning = false) }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // 开始按钮
                Button(
                    onClick = {
                        // TODO: 调用 core 层执行导出操作
                        viewModel.updateExportTextureState {
                            it.copy(
                                isRunning = true,
                                progress = 0f,
                                statusText = "正在导出..."
                            )
                        }
                    },
                    enabled = exportState.filePath.isNotEmpty() &&
                              exportState.outputPath.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = "开始导出")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 重置按钮
                OutlinedButton(
                    onClick = { viewModel.resetExportTextureState() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = "重新选择")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
