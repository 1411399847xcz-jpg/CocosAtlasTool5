package com.cocos.atlastool.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 通用 UI 组件库
 *
 * 提供在多个 Screen 中复用的通用 Compose 组件，包括：
 * - 步骤引导条
 * - 统计卡片
 * - 文件选择行
 * - 进度遮罩
 * - 空状态占位
 */

// ============================================================
// StepIndicator — 步骤引导条
// ============================================================

/**
 * 步骤引导条组件
 *
 * 显示水平排列的步骤指示器，标记当前步骤、已完成步骤和待完成步骤。
 * 适用于多步操作流程（如：选择文件 -> 配置参数 -> 执行操作）。
 *
 * @param steps 步骤名称列表
 * @param currentStep 当前步骤索引（从 0 开始）
 * @param modifier 布局修饰符
 */
@Composable
fun StepIndicator(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, stepName ->
            // 步骤圆圈 + 连接线
            StepCircle(
                stepNumber = index + 1,
                stepName = stepName,
                isCompleted = index < currentStep,
                isCurrent = index == currentStep,
                isLast = index == steps.lastIndex
            )

            // 非最后一步时显示连接线
            if (index < steps.lastIndex) {
                StepConnector(
                    isCompleted = index < currentStep,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 单个步骤圆圈
 */
@Composable
private fun StepCircle(
    stepNumber: Int,
    stepName: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean
) {
    val color = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val contentColor = when {
        isCompleted || isCurrent -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                // 已完成：显示勾号
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已完成",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stepName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(48.dp)
        )
    }
}

/**
 * 步骤间的连接线
 */
@Composable
private fun StepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.outlineVariant,
        label = "连接线颜色"
    )

    Box(
        modifier = modifier
            .height(2.dp)
            .background(color, RoundedCornerShape(1.dp))
    )
}

// ============================================================
// StatsCard — 统计卡片
// ============================================================

/**
 * 统计结果卡片
 *
 * 显示操作完成后的统计数据，包含成功、失败、跳过计数。
 * 通常在操作完成后展示。
 *
 * @param successCount 成功数量
 * @param failCount 失败数量
 * @param skipCount 跳过数量
 * @param modifier 布局修饰符
 */
@Composable
fun StatsCard(
    successCount: Int,
    failCount: Int,
    skipCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "处理结果",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 成功数
                StatsItem(
                    label = "成功",
                    count = successCount,
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF22C55E)
                )
                // 失败数
                StatsItem(
                    label = "失败",
                    count = failCount,
                    icon = Icons.Default.Error,
                    color = Color(0xFFEF4444)
                )
                // 跳过数
                StatsItem(
                    label = "跳过",
                    count = skipCount,
                    icon = Icons.Default.SkipNext,
                    color = Color(0xFFF59E0B)
                )
            }
        }
    }
}

/**
 * 统计项（单个数值展示）
 */
@Composable
private fun StatsItem(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// FilePickerRow — 文件选择行
// ============================================================

/**
 * 文件选择行组件
 *
 * 显示当前选中的文件路径，并提供更改按钮。
 * 点击"更改"按钮触发 onChange 回调，通常用于跳转到文件浏览器。
 *
 * @param label 文件类型标签（如 "选择 Plist 文件"）
 * @param filePath 当前选中的文件路径，为空时显示提示
 * @param onChange 点击更改按钮的回调
 * @param modifier 布局修饰符
 */
@Composable
fun FilePickerRow(
    label: String,
    filePath: String,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件/文件夹图标
            Icon(
                imageVector = if (filePath.isEmpty()) Icons.Default.Folder
                              else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 标签 + 路径
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (filePath.isEmpty()) "未选择文件"
                           else filePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (filePath.isEmpty()) MaterialTheme.colorScheme.outline
                           else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 更改按钮
            TextButton(onClick = onChange) {
                Text(text = "更改")
            }
        }
    }
}

// ============================================================
// ProgressOverlay — 带取消按钮的进度遮罩
// ============================================================

/**
 * 进度遮罩组件
 *
 * 在执行长时间操作时显示全屏半透明遮罩，包含：
 * - 进度条
 * - 实时状态文本
 * - 可选的取消按钮
 *
 * @param progress 当前进度值 (0f ~ 1f)
 * @param statusText 状态描述文本
 * @param onCancel 取消按钮回调，为 null 时不显示取消按钮
 * @param modifier 布局修饰符
 */
@Composable
fun ProgressOverlay(
    progress: Float,
    statusText: String,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度百分比
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 状态文本
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 可选取消按钮
            onCancel?.let {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = it) {
                    Text(text = "取消")
                }
            }
        }
    }
}

// ============================================================
// EmptyState — 空状态占位
// ============================================================

/**
 * 空状态占位组件
 *
 * 在列表为空或没有数据时显示的提示界面，包含图标和文字说明。
 *
 * @param icon 显示的图标
 * @param title 标题文字
 * @param description 详细描述
 * @param actionLabel 操作按钮文字，为 null 时不显示按钮
 * @param onAction 操作按钮点击回调
 * @param modifier 布局修饰符
 */
@Composable
fun EmptyState(
    icon: ImageVector = Icons.Default.Info,
    title: String,
    description: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // 描述
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        // 操作按钮
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}
