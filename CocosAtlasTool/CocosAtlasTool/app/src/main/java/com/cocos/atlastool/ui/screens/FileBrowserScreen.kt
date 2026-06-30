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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * 文件选择模式
 *
 * 区分选择文件和选择文件夹两种模式。
 */
enum class SelectMode {
    /** 选择文件 */
    FILE,
    /** 选择文件夹 */
    FOLDER
}

/**
 * 文件浏览器屏幕 — 通用文件/文件夹选择组件
 *
 * 提供统一的文件浏览界面，支持：
 * - 面包屑导航路径栏
 * - 文件夹和文件分开显示（文件夹在前）
 * - 按扩展名过滤文件
 * - 文件/文件夹两种选择模式
 * - FAB 按钮返回上级目录
 * - 确认按钮提交选择结果
 *
 * @param title 页面标题
 * @param allowedExtensions 允许选择的文件扩展名列表（如 ["plist", "png"]），为空则允许所有文件
 * @param selectMode 选择模式：FILE 或 FOLDER
 * @param onResult 选择完成后的回调，返回选中的文件/文件夹路径
 * @param onBack 返回上一页的回调
 * @param initialPath 初始浏览路径，默认为外部存储根目录
 */
@Composable
fun FileBrowserScreen(
    title: String,
    allowedExtensions: List<String> = emptyList(),
    selectMode: SelectMode = SelectMode.FILE,
    onResult: (String) -> Unit,
    onBack: () -> Unit,
    initialPath: String = "/sdcard"
) {
    // 当前浏览目录
    var currentPath by remember { mutableStateOf(initialPath) }
    // 当前选中的文件/文件夹名
    var selectedName by remember { mutableStateOf("") }

    // 获取当前目录下的文件和文件夹列表
    val currentDir = File(currentPath)
    val (folders, files) = remember(currentPath) {
        val items = currentDir.listFiles()?.sortedWith(compareByDescending<File> { it.isDirectory }
            .thenBy { it.name.lowercase() })
            ?: emptyList()
        // 文件夹列表
        val dirList = items.filter { it.isDirectory && !it.name.startsWith(".") }
        // 文件列表（按扩展名过滤）
        val fileList = if (allowedExtensions.isEmpty()) {
            items.filter { it.isFile && !it.name.startsWith(".") }
        } else {
            items.filter { it.isFile && !it.name.startsWith(".") &&
                allowedExtensions.any { ext -> it.name.lowercase().endsWith(".$ext") } }
        }
        Pair(dirList, fileList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // 返回按钮
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        },
        floatingActionButton = {
            // FAB 按钮：返回上级目录
            if (currentPath != "/") {
                FloatingActionButton(
                    onClick = {
                        currentPath = File(currentPath).parent ?: "/"
                        selectedName = ""
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "返回上级"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ===== 面包屑导航路径栏 =====
            BreadcrumbBar(
                currentPath = currentPath,
                onBreadcrumbClick = { path ->
                    currentPath = path
                    selectedName = ""
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 文件列表 =====
            if (folders.isEmpty() && files.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前目录为空",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 文件夹部分
                    if (folders.isNotEmpty()) {
                        item {
                            Text(
                                text = "文件夹 (${folders.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(folders) { folder ->
                            FileItemRow(
                                name = folder.name,
                                isDirectory = true,
                                isSelected = false,
                                onClick = {
                                    currentPath = folder.absolutePath
                                    selectedName = ""
                                }
                            )
                        }
                    }

                    // 文件部分
                    if (files.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "文件 (${files.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(files) { file ->
                            FileItemRow(
                                name = file.name,
                                isDirectory = false,
                                isSelected = selectedName == file.name,
                                onClick = {
                                    if (selectMode == SelectMode.FILE) {
                                        selectedName = file.name
                                    } else {
                                        // 文件夹模式下，点击文件无效
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 底部：确认按钮 + 当前选中路径 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 显示当前选中的路径
                val displayPath = if (selectedName.isNotEmpty()) {
                    "$currentPath/$selectedName"
                } else if (selectMode == SelectMode.FOLDER) {
                    currentPath
                } else {
                    ""
                }

                if (displayPath.isNotEmpty()) {
                    Text(
                        text = "已选择: ${displayPath.substringAfterLast("/")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 确认按钮
                val canConfirm = when (selectMode) {
                    SelectMode.FILE -> selectedName.isNotEmpty()
                    SelectMode.FOLDER -> currentPath.isNotEmpty()
                }

                OutlinedButton(
                    onClick = {
                        val resultPath = when (selectMode) {
                            SelectMode.FILE -> "$currentPath/$selectedName"
                            SelectMode.FOLDER -> currentPath
                        }
                        onResult(resultPath)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "确认选择")
                }
            }
        }
    }
}

/**
 * 面包屑导航路径栏
 *
 * 将当前路径按 "/" 分割显示，每段可点击跳转到对应目录。
 *
 * @param currentPath 当前路径
 * @param onBreadcrumbClick 点击路径段的回调
 */
@Composable
private fun BreadcrumbBar(
    currentPath: String,
    onBreadcrumbClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 根路径
        TextButton(
            onClick = { onBreadcrumbClick("/") },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
        ) {
            Text(
                text = "/",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 分割路径各段
        val segments = currentPath.split("/").filter { it.isNotEmpty() }
        segments.forEachIndexed { index, segment ->
            // 分隔符
            Text(
                text = "/",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            // 路径段（可点击）
            val isLast = index == segments.lastIndex
            val segmentPath = "/" + segments.subList(0, index + 1).joinToString("/")

            TextButton(
                onClick = { onBreadcrumbClick(segmentPath) },
                enabled = !isLast,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 文件/文件夹列表行
 *
 * 显示单个文件或文件夹的名称和图标，支持选中状态高亮。
 *
 * @param name 文件/文件夹名称
 * @param isDirectory 是否为文件夹
 * @param isSelected 是否被选中
 * @param onClick 点击回调
 */
@Composable
private fun FileItemRow(
    name: String,
    isDirectory: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件/文件夹图标
        Icon(
            imageVector = when {
                isDirectory -> Icons.Default.Folder
                isSelected -> Icons.Default.CheckCircle
                name.endsWith(".png", ignoreCase = true) ||
                    name.endsWith(".jpg", ignoreCase = true) ||
                    name.endsWith(".webp", ignoreCase = true) -> Icons.Default.Image
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = if (isDirectory) "文件夹" else "文件",
            tint = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isDirectory -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 文件名
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
