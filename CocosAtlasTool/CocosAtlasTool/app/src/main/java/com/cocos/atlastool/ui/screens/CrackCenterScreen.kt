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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.components.FilePickerRow
import com.cocos.atlastool.ui.components.ProgressOverlay
import com.cocos.atlastool.ui.viewmodel.CrackMode
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 密钥破解屏幕
 *
 * 支持两种破解模式：
 * - 字典穷举：使用预设字典逐个尝试
 * - 字符穷举：按字符集组合逐个尝试
 *
 * 操作流程：
 * 1. 选择破解模式（Tab 切换）
 * 2. 选择 CCZ 文件
 * 3. 可选配置 Plist 和 SO 文件
 * 4. 配置线程数
 * 5. 开始破解
 * 6. 显示实时进度和状态
 * 7. 成功后显示密钥和导出路径
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrackCenterScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val crackState by viewModel.crackState.collectAsState()

    // Tab 状态
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("字典穷举", "字符穷举")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "密钥破解") },
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
            // ===== Tab 切换 =====
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            val mode = if (index == 0) CrackMode.DICT else CrackMode.CHAR
                            viewModel.updateCrackState { it.copy(mode = mode) }
                        },
                        text = { Text(text = title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 必选：CCZ 文件 =====
            Text(
                text = "必选文件",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FilePickerRow(
                label = "CCZ 文件",
                filePath = crackState.cczPath,
                onChange = { onBrowseFile("crack_ccz", listOf("ccz")) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 可选：Plist 和 SO 文件 =====
            Text(
                text = "可选文件",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FilePickerRow(
                label = "Plist 文件（可选）",
                filePath = crackState.plistPath,
                onChange = { onBrowseFile("crack_plist", listOf("plist")) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilePickerRow(
                label = "SO 文件（可选）",
                filePath = crackState.soPath,
                onChange = { onBrowseFile("crack_so", listOf("so")) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 线程数配置 =====
            Text(
                text = "配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "线程数:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(72.dp)
                )
                Slider(
                    value = crackState.threadCount.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateCrackState { it.copy(threadCount = value.toInt()) }
                    },
                    valueRange = 1f..16f,
                    steps = 14,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = crackState.threadCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 状态文本 =====
            Text(
                text = crackState.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 操作按钮 / 进度 / 结果 =====
            when {
                crackState.isRunning -> {
                    // 破解中：显示进度
                    ProgressOverlay(
                        progress = crackState.progress,
                        statusText = crackState.statusText,
                        onCancel = {
                            viewModel.updateCrackState { it.copy(isRunning = false, statusText = "已取消") }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                crackState.crackedKey.isNotEmpty() -> {
                    // 破解成功：显示密钥和导出路径
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "破解成功!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "密钥: ${crackState.crackedKey}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (crackState.exportPath.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "导出路径: ${crackState.exportPath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetCrackState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "重新开始")
                            }
                        }
                    }
                }
                else -> {
                    // 开始破解按钮
                    Button(
                        onClick = {
                            // TODO: 调用 core 层执行破解操作
                            viewModel.updateCrackState {
                                it.copy(
                                    isRunning = true,
                                    progress = 0f,
                                    statusText = "正在破解中..."
                                )
                            }
                        },
                        enabled = crackState.cczPath.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "开始破解")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
