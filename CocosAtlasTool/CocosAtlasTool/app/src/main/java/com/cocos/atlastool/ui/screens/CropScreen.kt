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
import com.cocos.atlastool.ui.components.StatsCard
import com.cocos.atlastool.ui.components.StepIndicator
import com.cocos.atlastool.ui.viewmodel.CropStep
import com.cocos.atlastool.ui.viewmodel.CropType
import com.cocos.atlastool.ui.viewmodel.MainViewModel

/**
 * 拆分屏幕 — 单文件/批量拆分界面
 *
 * 提供步骤引导式操作流程：
 * 1. 选择 Plist 文件
 * 2. 选择/自动匹配纹理文件
 * 3. 选择输出目录
 * 4. 开始拆分
 * 5. 显示结果
 *
 * @param type 拆分类型：SINGLE（单文件）或 BATCH（批量）
 * @param onBack 返回上一页的回调
 * @param onBrowseFile 请求打开文件浏览器的回调
 * @param onBrowseFolder 请求打开文件夹浏览器的回调
 * @param viewModel 主 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    type: CropType,
    onBack: () -> Unit,
    onBrowseFile: (requestId: String, extensions: List<String>) -> Unit,
    onBrowseFolder: (requestId: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val cropState by viewModel.cropState.collectAsState()

    // 步骤名称列表
    val stepNames = listOf("选择Plist", "选择纹理", "选择输出", "执行拆分")
    val currentStepIndex = when (cropState.step) {
        CropStep.SELECT_PLIST -> 0
        CropStep.SELECT_TEXTURE -> 1
        CropStep.SELECT_OUTPUT -> 2
        CropStep.PROCESSING -> 3
        CropStep.COMPLETED -> 3
    }

    // 页面标题
    val pageTitle = when (type) {
        CropType.SINGLE -> "单文件拆分"
        CropType.BATCH -> "批量拆分"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = pageTitle) },
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
            // ===== 步骤引导条 =====
            StepIndicator(
                steps = stepNames,
                currentStep = currentStepIndex
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 步骤内容区域 =====
            when (cropState.step) {
                CropStep.SELECT_PLIST -> {
                    // 步骤1：选择 Plist 文件
                    Text(
                        text = if (type == CropType.SINGLE) "请选择需要拆分的 Plist 文件"
                               else "请选择包含 Plist 文件的文件夹",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (type == CropType.SINGLE) {
                        FilePickerRow(
                            label = "Plist 文件",
                            filePath = cropState.plistPath,
                            onChange = { onBrowseFile("crop_plist", listOf("plist")) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        FilePickerRow(
                            label = "Plist 文件夹",
                            filePath = cropState.plistPath,
                            onChange = { onBrowseFolder("crop_plist_folder") },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.updateCropState { it.copy(step = CropStep.SELECT_TEXTURE) }
                        },
                        enabled = cropState.plistPath.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "下一步")
                    }
                }

                CropStep.SELECT_TEXTURE -> {
                    // 步骤2：选择纹理文件
                    Text(
                        text = "请选择对应的纹理文件（通常与 Plist 同目录）",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FilePickerRow(
                        label = "纹理文件",
                        filePath = cropState.texturePath,
                        onChange = { onBrowseFile("crop_texture", listOf("png", "jpg", "webp")) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 自动匹配按钮提示
                    OutlinedButton(
                        onClick = {
                            // TODO: 实现自动匹配纹理逻辑
                            viewModel.updateCropState {
                                it.copy(
                                    texturePath = it.plistPath.replace(".plist", ".png"),
                                    step = CropStep.SELECT_OUTPUT
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "自动匹配同名纹理")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateCropState { it.copy(step = CropStep.SELECT_OUTPUT) }
                        },
                        enabled = cropState.texturePath.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "下一步")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.updateCropState { it.copy(step = CropStep.SELECT_PLIST) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "上一步")
                    }
                }

                CropStep.SELECT_OUTPUT -> {
                    // 步骤3：选择输出目录
                    Text(
                        text = "请选择拆分后精灵图的输出目录",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FilePickerRow(
                        label = "输出目录",
                        filePath = cropState.outputPath,
                        onChange = { onBrowseFolder("crop_output") },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 开始拆分按钮
                    Button(
                        onClick = {
                            // TODO: 调用 core 层执行实际拆分操作
                            viewModel.updateCropState {
                                it.copy(
                                    step = CropStep.PROCESSING,
                                    progress = 0f,
                                    statusText = "正在拆分..."
                                )
                            }
                        },
                        enabled = cropState.outputPath.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "开始拆分")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.updateCropState { it.copy(step = CropStep.SELECT_TEXTURE) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = "上一步")
                    }
                }

                CropStep.PROCESSING -> {
                    // 步骤4：处理中
                    Spacer(modifier = Modifier.height(32.dp))
                    ProgressOverlay(
                        progress = cropState.progress,
                        statusText = cropState.statusText,
                        onCancel = {
                            // TODO: 实现取消操作
                            viewModel.updateCropState { it.copy(step = CropStep.SELECT_OUTPUT) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                CropStep.COMPLETED -> {
                    // 步骤5：完成，显示统计结果
                    Spacer(modifier = Modifier.height(16.dp))

                    StatsCard(
                        successCount = cropState.successCount,
                        failCount = cropState.failCount,
                        skipCount = cropState.skipCount,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 输出路径
                    FilePickerRow(
                        label = "输出路径",
                        filePath = cropState.outputPath,
                        onChange = { /* 已完成，仅显示 */ },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 打开输出目录按钮
                    Button(
                        onClick = {
                            // TODO: 打开文件管理器跳转到输出目录
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "打开输出目录")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 重新开始按钮
                    OutlinedButton(
                        onClick = { viewModel.resetCropState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(text = "重新开始")
                    }
                }
            }
        }
    }
}
