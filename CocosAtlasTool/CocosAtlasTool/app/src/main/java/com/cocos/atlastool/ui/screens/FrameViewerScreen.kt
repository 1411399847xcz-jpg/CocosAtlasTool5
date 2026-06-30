package com.cocos.atlastool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.cocos.atlastool.ui.viewmodel.FrameInfo
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 帧详情屏幕
 *
 * 功能流程：
 * 1. 选择 Plist 文件
 * 2. 显示所有帧列表（支持搜索过滤）
 * 3. 点击帧显示详细信息（坐标、尺寸、是否旋转）
 * 4. 可以点击预览单帧
 *
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameViewerScreen(
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val frameState by viewModel.frameViewerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "帧详情") },
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
            // ===== 选择 Plist 文件 =====
            FilePickerRow(
                label = "Plist 文件",
                filePath = frameState.plistPath,
                onChange = { onBrowseFile("frame_plist", listOf("plist")) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 统一声明过滤后的帧列表（避免重复声明）
            val filteredFrames = if (frameState.searchQuery.isNotEmpty()) {
                frameState.frames.filter { it.name.contains(frameState.searchQuery, ignoreCase = true) }
            } else {
                frameState.frames
            }

            // 搜索框
            if (frameState.frames.isNotEmpty()) {
                OutlinedTextField(
                    value = frameState.searchQuery,
                    onValueChange = { query ->
                        viewModel.updateFrameViewerState { it.copy(searchQuery = query) }
                    },
                    label = { Text(text = "搜索帧名") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 帧总数
                Text(
                    text = "共 ${filteredFrames.size} 帧",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ===== 帧列表 / 空状态 =====
            if (frameState.plistPath.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Description,
                    title = "请先选择 Plist 文件",
                    description = "选择后即可查看所有帧的详细信息"
                )
            } else if (frameState.frames.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "未找到帧数据",
                    description = "该 Plist 文件可能不包含有效的帧信息"
                )
            } else {
                // 显示帧列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(filteredFrames) { index, frame ->
                        FrameListItem(
                            frame = frame,
                            isSelected = index == frameState.selectedFrameIndex,
                            onClick = {
                                viewModel.updateFrameViewerState {
                                    it.copy(selectedFrameIndex = if (it.selectedFrameIndex == index) -1 else index)
                                }
                            }
                        )
                    }
                }
            }

            // ===== 帧详细信息面板 =====
            if (frameState.selectedFrameIndex >= 0 &&
                frameState.selectedFrameIndex < frameState.frames.size) {
                val selectedFrame = frameState.frames[frameState.selectedFrameIndex]
                FrameDetailPanel(
                    frame = selectedFrame,
                    onPreview = {
                        // TODO: 显示单帧预览
                    }
                )
            }
        }
    }
}

/**
 * 帧列表项
 *
 * 显示单个帧的名称，点击可展开查看详情。
 *
 * @param frame 帧信息
 * @param isSelected 是否选中
 * @param onClick 点击回调
 */
@Composable
private fun FrameListItem(
    frame: FrameInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 帧缩略图占位
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 帧名称
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = frame.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${frame.width}x${frame.height}" +
                           if (frame.isRotated) " (旋转)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 帧详细信息面板
 *
 * 显示选中帧的坐标、尺寸和旋转信息。
 *
 * @param frame 帧信息
 * @param onPreview 预览按钮回调
 */
@Composable
private fun FrameDetailPanel(
    frame: FrameInfo,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "帧详细信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 信息行
            InfoRow(label = "帧名称", value = frame.name)
            InfoRow(label = "X 坐标", value = "${frame.x}px")
            InfoRow(label = "Y 坐标", value = "${frame.y}px")
            InfoRow(label = "宽度", value = "${frame.width}px")
            InfoRow(label = "高度", value = "${frame.height}px")
            InfoRow(
                label = "旋转",
                value = if (frame.isRotated) "是" else "否"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 预览按钮
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "预览此帧")
            }
        }
    }
}

/**
 * 信息行组件
 *
 * @param label 标签
 * @param value 值
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
