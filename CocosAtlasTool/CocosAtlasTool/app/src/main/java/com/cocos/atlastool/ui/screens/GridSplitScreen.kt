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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.components.EmptyState
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.components.ProgressOverlay
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 网格切割屏幕
 *
 * 功能流程：
 * 1. 选择纹理图片
 * 2. 显示预览缩略图
 * 3. 配置行列数（Slider + TextField）
 * 4. 选择输出目录
 * 5. 执行切割并显示进度
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param onBrowseFolder 请求打开文件夹浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridSplitScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    onBrowseFolder: (requestId: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val gridState by viewModel.gridSplitState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "网格切割") },
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
            // ===== 选择纹理文件 =====
            FilePickerRow(
                label = "纹理文件",
                filePath = gridState.texturePath,
                onChange = { onBrowseFile("grid_texture", listOf("png", "jpg", "webp")) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ===== 预览缩略图区域 =====
            if (gridState.texturePath.isNotEmpty()) {
                // 预览区域占位（实际应加载纹理缩略图）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "纹理预览",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "纹理预览",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                EmptyState(
                    icon = Icons.Default.Image,
                    title = "请先选择纹理文件",
                    description = "支持 PNG、JPG、WebP 格式",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 行列数配置 =====
            Text(
                text = "切割配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 列数配置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "列数:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(60.dp)
                )
                Slider(
                    value = gridState.columns.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateGridSplitState { it.copy(columns = value.toInt()) }
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = gridState.columns.toString(),
                    onValueChange = { text ->
                        text.toIntOrNull()?.let {
                            if (it in 1..20) {
                                viewModel.updateGridSplitState { state -> state.copy(columns = it) }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 行数配置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行数:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(60.dp)
                )
                Slider(
                    value = gridState.rows.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateGridSplitState { it.copy(rows = value.toInt()) }
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = gridState.rows.toString(),
                    onValueChange = { text ->
                        text.toIntOrNull()?.let {
                            if (it in 1..20) {
                                viewModel.updateGridSplitState { state -> state.copy(rows = it) }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 切割总数提示
            Text(
                text = "将切割为 ${gridState.columns} x ${gridState.rows} = ${gridState.columns * gridState.rows} 张图片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 输出目录 =====
            FilePickerRow(
                label = "输出目录",
                filePath = gridState.outputPath,
                onChange = { onBrowseFolder("grid_output") },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 操作按钮 =====
            if (gridState.isRunning) {
                // 切割中：显示进度
                ProgressOverlay(
                    progress = gridState.progress,
                    statusText = gridState.statusText,
                    onCancel = {
                        viewModel.updateGridSplitState { it.copy(isRunning = false) }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // 开始切割按钮
                Button(
                    onClick = {
                        // TODO: 调用 core 层执行网格切割
                        viewModel.updateGridSplitState {
                            it.copy(
                                isRunning = true,
                                progress = 0f,
                                statusText = "正在切割中...",
                                cutCount = 0
                            )
                        }
                    },
                    enabled = gridState.texturePath.isNotEmpty() && gridState.outputPath.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = "开始切割")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
