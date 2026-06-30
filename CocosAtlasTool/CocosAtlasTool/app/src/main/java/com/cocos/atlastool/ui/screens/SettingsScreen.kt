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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocos.atlastool.ui.viewmodel.MainViewModel
import com.cocos.atlastool.ui.viewmodel.TagMapping

/**
 * 设置屏幕
 *
 * 使用分组 Card 展示各类设置项：
 * - 输出设置（格式、质量）
 * - 处理设置（Alpha 通道、自动裁剪）
 * - 穷举设置（线程数、字符集）
 * - 自定义标签替换
 *
 * 所有设置通过 ViewModel 的 StateFlow 管理，修改即时生效。
 *
 * @param onBack 返回上一页的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设置") },
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
            // ===== 输出设置 =====
            SettingsGroupCard(title = "输出设置") {
                // 输出格式：PNG / JPG 切换
                Text(
                    text = "输出格式",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.isPngFormat,
                        onClick = {
                            viewModel.updateSettingsState { it.copy(isPngFormat = true) }
                        },
                        label = { Text(text = "PNG") }
                    )
                    FilterChip(
                        selected = !settings.isPngFormat,
                        onClick = {
                            viewModel.updateSettingsState { it.copy(isPngFormat = false) }
                        },
                        label = { Text(text = "JPG") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // JPG 质量
                if (!settings.isPngFormat) {
                    Text(
                        text = "JPG 质量: ${settings.jpgQuality}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = settings.jpgQuality.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateSettingsState {
                                it.copy(jpgQuality = value.toInt())
                            }
                        },
                        valueRange = 10f..100f,
                        steps = 17,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 备份数量
                Text(
                    text = "备份数量: ${settings.backupCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = settings.backupCount.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateSettingsState {
                            it.copy(backupCount = value.toInt())
                        }
                    },
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== 处理设置 =====
            SettingsGroupCard(title = "处理设置") {
                // Alpha 通道开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "保留 Alpha 通道",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.keepAlpha,
                        onCheckedChange = { checked ->
                            viewModel.updateSettingsState { it.copy(keepAlpha = checked) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 自动裁剪开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "自动裁剪",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.autoCrop,
                        onCheckedChange = { checked ->
                            viewModel.updateSettingsState { it.copy(autoCrop = checked) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== 穷举设置 =====
            SettingsGroupCard(title = "穷举设置") {
                // 线程数
                Text(
                    text = "穷举线程数: ${settings.bruteThreadCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = settings.bruteThreadCount.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateSettingsState {
                            it.copy(bruteThreadCount = value.toInt())
                        }
                    },
                    valueRange = 1f..32f,
                    steps = 30,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 字符集配置
                Text(
                    text = "字符集",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = settings.charset,
                    onValueChange = { charset ->
                        viewModel.updateSettingsState { it.copy(charset = charset) }
                    },
                    label = { Text(text = "穷举字符集") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== 自定义标签替换 =====
            SettingsGroupCard(title = "自定义标签替换") {
                Text(
                    text = "配置原始标签到替换标签的映射关系",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 已有映射列表
                settings.tagMappings.forEachIndexed { index, mapping ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${mapping.originalTag} -> ${mapping.replaceTag}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                viewModel.updateSettingsState {
                                    it.copy(
                                        tagMappings = it.tagMappings.filterIndexed { i, _ -> i != index }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除映射",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 添加新映射按钮
                OutlinedButton(
                    onClick = {
                        // TODO: 弹出对话框输入映射对，暂时添加默认示例
                        viewModel.updateSettingsState {
                            it.copy(
                                tagMappings = it.tagMappings + TagMapping(
                                    originalTag = "original",
                                    replaceTag = "replaced"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加映射"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "添加映射")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 设置分组卡片
 *
 * 将相关设置项归类展示在一个 Card 中，统一标题和样式。
 *
 * @param title 分组标题
 * @param content 分组内的设置项内容
 */
@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
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
            // 分组标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分组内容
            content()
        }
    }
}
