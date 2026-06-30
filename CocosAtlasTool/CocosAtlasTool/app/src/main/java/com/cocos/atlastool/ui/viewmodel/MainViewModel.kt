package com.cocos.atlastool.ui.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocos.atlastool.core.BruteForceDecryptor
import com.cocos.atlastool.core.CCZParser
import com.cocos.atlastool.core.ImageProcessor
import com.cocos.atlastool.core.PlistParser
import com.cocos.atlastool.core.PVRParser
import com.cocos.atlastool.core.SimplifierEngine
import com.cocos.atlastool.core.XMLObfuscator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * 主 ViewModel
 *
 * 管理各功能模块的状态，包括：
 * - 拆分操作状态 (CropState)
 * - 密钥破解状态 (CrackState)
 * - 动画精简状态 (SimplifyState)
 * - 设置状态 (SettingsState)
 * - 文件浏览器返回值
 * - 通用进度状态
 *
 * 使用 Kotlin StateFlow 实现响应式状态管理。
 * 各 Screen 通过收集 StateFlow 来观察状态变化。
 */

// ============================================================
// 拆分操作状态
// ============================================================

/** 拆分类型 */
enum class CropType {
    /** 单文件拆分 */
    SINGLE,
    /** 批量拆分 */
    BATCH
}

/** 拆分操作步骤 */
enum class CropStep {
    /** 选择 Plist 文件 */
    SELECT_PLIST,
    /** 选择/自动匹配纹理文件 */
    SELECT_TEXTURE,
    /** 选择输出目录 */
    SELECT_OUTPUT,
    /** 执行拆分中 */
    PROCESSING,
    /** 拆分完成 */
    COMPLETED
}

/** 拆分操作状态 */
data class CropState(
    /** 当前步骤 */
    val step: CropStep = CropStep.SELECT_PLIST,
    /** 选中的 Plist 文件路径 */
    val plistPath: String = "",
    /** 选中的纹理文件路径 */
    val texturePath: String = "",
    /** 输出目录路径 */
    val outputPath: String = "",
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 状态描述文本 */
    val statusText: String = "",
    /** 成功数量 */
    val successCount: Int = 0,
    /** 失败数量 */
    val failCount: Int = 0,
    /** 跳过数量 */
    val skipCount: Int = 0
)

// ============================================================
// 密钥破解状态
// ============================================================

/** 穷举模式 */
enum class CrackMode {
    /** 字典穷举 */
    DICT,
    /** 字符穷举 */
    CHAR
}

/** 密钥破解状态 */
data class CrackState(
    /** 穷举模式 */
    val mode: CrackMode = CrackMode.DICT,
    /** 选中的 CCZ 文件路径 */
    val cczPath: String = "",
    /** 可选的 Plist 文件路径 */
    val plistPath: String = "",
    /** 可选的 SO 文件路径 */
    val soPath: String = "",
    /** 线程数量 */
    val threadCount: Int = 4,
    /** 当前状态文本 */
    val statusText: String = "等待开始",
    /** 是否正在破解中 */
    val isRunning: Boolean = false,
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 破解成功后的密钥 */
    val crackedKey: String = "",
    /** 导出路径 */
    val exportPath: String = ""
)

// ============================================================
// 动画精简状态
// ============================================================

/** 动画精简状态 */
data class SimplifyState(
    /** 是否批量模式 */
    val isBatch: Boolean = false,
    /** 已选中的 Plist 文件列表 */
    val selectedFiles: List<String> = emptyList(),
    /** 输出目录路径 */
    val outputPath: String = "",
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 当前状态文本 */
    val statusText: String = "",
    /** 是否正在处理 */
    val isRunning: Boolean = false,
    /** 成功数量 */
    val successCount: Int = 0,
    /** 失败数量 */
    val failCount: Int = 0
)

// ============================================================
// 网格切割状态
// ============================================================

/** 网格切割状态 */
data class GridSplitState(
    /** 选中的纹理文件路径 */
    val texturePath: String = "",
    /** 列数 */
    val columns: Int = 4,
    /** 行数 */
    val rows: Int = 4,
    /** 输出目录路径 */
    val outputPath: String = "",
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 当前状态文本 */
    val statusText: String = "",
    /** 是否正在切割 */
    val isRunning: Boolean = false,
    /** 已切割数量 */
    val cutCount: Int = 0
)

// ============================================================
// XML 加解密状态
// ============================================================

/** XML 操作类型 */
enum class XmlOperation {
    /** 单个加密 */
    ENCRYPT_SINGLE,
    /** 单个解密 */
    DECRYPT_SINGLE,
    /** 批量加密 */
    ENCRYPT_BATCH,
    /** 批量解密 */
    DECRYPT_BATCH
}

/** XML 加解密状态 */
data class XmlCryptoState(
    /** 当前操作类型 */
    val operation: XmlOperation = XmlOperation.ENCRYPT_SINGLE,
    /** 选中的文件路径（单个模式） */
    val filePath: String = "",
    /** 选中的文件夹路径（批量模式） */
    val folderPath: String = "",
    /** 输出目录路径 */
    val outputPath: String = "",
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 当前状态文本 */
    val statusText: String = "",
    /** 是否正在处理 */
    val isRunning: Boolean = false,
    /** 成功数量 */
    val successCount: Int = 0,
    /** 失败数量 */
    val failCount: Int = 0
)

// ============================================================
// 图片去白边状态
// ============================================================

/** 图片去白边状态 */
data class TrimImagesState(
    /** 选中的文件夹路径 */
    val folderPath: String = "",
    /** 找到的图片数量 */
    val imageCount: Int = 0,
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 当前状态文本 */
    val statusText: String = "",
    /** 是否正在处理 */
    val isRunning: Boolean = false,
    /** 成功数量 */
    val successCount: Int = 0,
    /** 失败数量 */
    val failCount: Int = 0
)

// ============================================================
// 帧详情状态
// ============================================================

/** 单帧信息 */
data class FrameInfo(
    /** 帧名称 */
    val name: String,
    /** X 坐标 */
    val x: Int,
    /** Y 坐标 */
    val y: Int,
    /** 宽度 */
    val width: Int,
    /** 高度 */
    val height: Int,
    /** 是否旋转 */
    val isRotated: Boolean
)

/** 帧详情状态 */
data class FrameViewerState(
    /** 选中的 Plist 文件路径 */
    val plistPath: String = "",
    /** 帧列表 */
    val frames: List<FrameInfo> = emptyList(),
    /** 当前选中查看详情的帧索引 */
    val selectedFrameIndex: Int = -1,
    /** 搜索关键词 */
    val searchQuery: String = ""
)

// ============================================================
// 文件信息状态
// ============================================================

/** 文件信息项 */
data class FileDetailInfo(
    /** 文件名称 */
    val name: String,
    /** 文件大小 */
    val size: String,
    /** 修改日期 */
    val modifiedDate: String,
    /** 文件类型 */
    val type: String,
    /** 其他附加信息 */
    val extraInfo: String = ""
)

/** 文件信息状态 */
data class FileInfoState(
    /** 选中的文件路径 */
    val filePath: String = "",
    /** 文件详细信息列表 */
    val details: List<FileDetailInfo> = emptyList()
)

// ============================================================
// 设置状态
// ============================================================

/** 标签映射对（用于自定义标签替换） */
data class TagMapping(
    /** 原始标签 */
    val originalTag: String,
    /** 替换标签 */
    val replaceTag: String
)

/** 设置状态 */
data class SettingsState(
    /** 输出格式：true=PNG, false=JPG */
    val isPngFormat: Boolean = true,
    /** JPG 质量值 (0 ~ 100) */
    val jpgQuality: Int = 90,
    /** 备份数量 */
    val backupCount: Int = 3,
    /** 是否保留 Alpha 通道 */
    val keepAlpha: Boolean = true,
    /** 是否自动裁剪 */
    val autoCrop: Boolean = true,
    /** 穷举线程数 */
    val bruteThreadCount: Int = 4,
    /** 字符集配置 */
    val charset: String = "0123456789abcdefghijklmnopqrstuvwxyz",
    /** 自定义标签替换映射 */
    val tagMappings: List<TagMapping> = emptyList()
)

// ============================================================
// 导出纹理状态
// ============================================================

/** 导出纹理状态 */
data class ExportTextureState(
    /** 选中的文件路径 */
    val filePath: String = "",
    /** 输出目录 */
    val outputPath: String = "",
    /** 处理进度 (0f ~ 1f) */
    val progress: Float = 0f,
    /** 当前状态文本 */
    val statusText: String = "",
    /** 是否正在处理 */
    val isRunning: Boolean = false
)

// ============================================================
// 文件浏览器返回结果
// ============================================================

/** 文件选择模式 */
enum class FileSelectMode {
    /** 选择文件 */
    FILE,
    /** 选择文件夹 */
    FOLDER
}

/** 文件浏览器请求 */
data class FileBrowserRequest(
    /** 请求标识，用于区分不同的文件选择场景 */
    val requestId: String,
    /** 允许的文件扩展名列表 */
    val allowedExtensions: List<String>,
    /** 选择模式 */
    val selectMode: FileSelectMode
)

// ============================================================
// MainViewModel
// ============================================================

/**
 * 主 ViewModel 实现
 *
 * 集中管理所有功能模块的状态，通过 StateFlow 向 UI 层暴露。
 * UI 层通过收集对应的 StateFlow 来响应状态变化。
 *
 * 各功能方法实际调用 core 层的核心逻辑（在 viewModelScope + IO 调度器中执行），
 * 并通过 StateFlow 更新 UI 状态。
 *
 * 文件选择交互流程：
 * 1. Screen 需要选择文件时，设置 fileBrowserRequest 并导航到 FileBrowserScreen
 * 2. FileBrowserScreen 选择完成后，将结果写入 fileBrowserResult
 * 3. 原 Screen 通过观察 fileBrowserResult 获取选择结果
 */
class MainViewModel : ViewModel() {

    // ===== 应用上下文（用于需要 Context 的 core 层类） =====
    private lateinit var appContext: Context

    /**
     * 初始化 ViewModel，传入 Application Context
     *
     * 必须在使用功能方法之前调用。
     *
     * @param context Activity 或 Application 的 Context
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ===== 拆分操作 =====
    private val _cropState = MutableStateFlow(CropState())
    val cropState: StateFlow<CropState> = _cropState.asStateFlow()

    /** 更新拆分状态 */
    fun updateCropState(updater: (CropState) -> CropState) {
        _cropState.update(updater)
    }

    /** 重置拆分状态 */
    fun resetCropState() {
        _cropState.value = CropState()
    }

    /**
     * 开始拆分操作
     *
     * 在 IO 线程中调用 PlistParser.parse() 解析 plist，
     * 然后调用 ImageProcessor.batchCrop() 批量裁剪精灵帧。
     */
    fun startCrop() {
        val state = _cropState.value
        if (state.plistPath.isEmpty() || state.texturePath.isEmpty() || state.outputPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _cropState.update { it.copy(step = CropStep.PROCESSING, progress = 0f, statusText = "正在解析 Plist...") }
            try {
                val (successCount, failCount) = ImageProcessor.batchCrop(
                    plistPath = state.plistPath,
                    texturePath = state.texturePath,
                    outputDir = state.outputPath,
                    onProgress = { percent, msg ->
                        _cropState.update { it.copy(progress = percent / 100f, statusText = msg) }
                    }
                )
                _cropState.update {
                    it.copy(
                        step = CropStep.COMPLETED,
                        progress = 1f,
                        statusText = "拆分完成: 成功 $successCount, 失败 $failCount",
                        successCount = successCount,
                        failCount = failCount
                    )
                }
            } catch (e: Exception) {
                _cropState.update {
                    it.copy(
                        step = CropStep.SELECT_PLIST,
                        progress = 0f,
                        statusText = "拆分失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== 密钥破解 =====
    private val _crackState = MutableStateFlow(CrackState())
    val crackState: StateFlow<CrackState> = _crackState.asStateFlow()

    /** 更新破解状态 */
    fun updateCrackState(updater: (CrackState) -> CrackState) {
        _crackState.update(updater)
    }

    /** 重置破解状态 */
    fun resetCrackState() {
        _crackState.value = CrackState()
    }

    /**
     * 开始字典穷举破解
     *
     * 在 IO 线程中调用 BruteForceDecryptor.bruteDict() 进行字典穷举。
     */
    fun startBruteDict() {
        val state = _crackState.value
        if (state.cczPath.isEmpty()) return
        if (!::appContext.isInitialized) return

        viewModelScope.launch(Dispatchers.IO) {
            _crackState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在读取 CCZ 文件...") }
            try {
                // 读取 CCZ 原始数据
                val raw = File(state.cczPath).readBytes()

                // 生成候选密钥列表
                val decryptor = BruteForceDecryptor(appContext)
                val soPaths = if (state.soPath.isNotEmpty()) listOf(state.soPath) else emptyList()
                val plistPath = state.plistPath.ifEmpty { null }
                val candidates = decryptor.generateCandidates(
                    cczPath = state.cczPath,
                    plistPath = plistPath,
                    soPaths = soPaths
                )

                _crackState.update { it.copy(statusText = "共 ${candidates.size} 个候选密钥，开始穷举...") }

                // 字典穷举
                val (decrypted, key) = decryptor.bruteDict(raw, candidates) { tried, msg ->
                    val pct = if (candidates.isNotEmpty()) tried.toFloat() / candidates.size else 0f
                    _crackState.update { it.copy(progress = pct.coerceIn(0f, 1f), statusText = msg) }
                }

                if (decrypted != null && key != null) {
                    val keyStr = String(key, Charsets.UTF_8)
                    _crackState.update {
                        it.copy(
                            isRunning = false,
                            progress = 1f,
                            statusText = "破解成功！密钥: $keyStr",
                            crackedKey = keyStr
                        )
                    }
                } else {
                    _crackState.update {
                        it.copy(
                            isRunning = false,
                            progress = 1f,
                            statusText = "字典穷举失败，未找到有效密钥"
                        )
                    }
                }
            } catch (e: Exception) {
                _crackState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "破解失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 开始字符集穷举破解
     *
     * 在 IO 线程中调用 BruteForceDecryptor.bruteCharset() 进行字符集穷举。
     */
    fun startBruteCharset() {
        val state = _crackState.value
        if (state.cczPath.isEmpty()) return
        if (!::appContext.isInitialized) return

        viewModelScope.launch(Dispatchers.IO) {
            _crackState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在读取 CCZ 文件...") }
            try {
                val raw = File(state.cczPath).readBytes()
                val settings = _settingsState.value
                val decryptor = BruteForceDecryptor(appContext)

                _crackState.update { it.copy(statusText = "字符集穷举开始...") }

                val (decrypted, key) = decryptor.bruteCharset(
                    raw = raw,
                    charset = settings.charset,
                    onProgress = { tried, msg ->
                        _crackState.update { it.copy(statusText = msg) }
                    }
                )

                if (decrypted != null && key != null) {
                    val keyStr = String(key, Charsets.UTF_8)
                    _crackState.update {
                        it.copy(
                            isRunning = false,
                            progress = 1f,
                            statusText = "破解成功！密钥: $keyStr",
                            crackedKey = keyStr
                        )
                    }
                } else {
                    _crackState.update {
                        it.copy(
                            isRunning = false,
                            progress = 1f,
                            statusText = "字符集穷举失败，未找到有效密钥"
                        )
                    }
                }
            } catch (e: Exception) {
                _crackState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "破解失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== 动画精简 =====
    private val _simplifyState = MutableStateFlow(SimplifyState())
    val simplifyState: StateFlow<SimplifyState> = _simplifyState.asStateFlow()

    /** 更新精简状态 */
    fun updateSimplifyState(updater: (SimplifyState) -> SimplifyState) {
        _simplifyState.update(updater)
    }

    /** 重置精简状态 */
    fun resetSimplifyState() {
        _simplifyState.value = SimplifyState()
    }

    /**
     * 开始动画/XML 精简
     *
     * 在 IO 线程中根据文件扩展名调用 SimplifierEngine.simplifyXml() 或 simplifyPlist()。
     */
    fun startSimplify() {
        val state = _simplifyState.value
        if (state.selectedFiles.isEmpty() || state.outputPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _simplifyState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在精简...", successCount = 0, failCount = 0) }
            try {
                var successCount = 0
                var failCount = 0
                val total = state.selectedFiles.size

                state.selectedFiles.forEachIndexed { index, filePath ->
                    try {
                        val file = File(filePath)
                        val content = file.readText(Charsets.UTF_8)

                        // 根据扩展名选择精简方法
                        val simplified = if (file.extension.lowercase() == "plist") {
                            SimplifierEngine.simplifyPlist(content)
                        } else {
                            SimplifierEngine.simplifyXml(content)
                        }

                        // 保存精简结果到输出目录
                        val outputFile = File(state.outputPath, file.name)
                        outputFile.parentFile?.mkdirs()
                        outputFile.writeText(simplified, Charsets.UTF_8)
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                    }

                    // 更新进度
                    val pct = (index + 1).toFloat() / total
                    _simplifyState.update {
                        it.copy(
                            progress = pct,
                            statusText = "精简 ${index + 1}/$total: ${File(filePath).name}",
                            successCount = successCount,
                            failCount = failCount
                        )
                    }
                }

                _simplifyState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "精简完成: 成功 $successCount, 失败 $failCount",
                        successCount = successCount,
                        failCount = failCount
                    )
                }
            } catch (e: Exception) {
                _simplifyState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "精简失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== 网格切割 =====
    private val _gridSplitState = MutableStateFlow(GridSplitState())
    val gridSplitState: StateFlow<GridSplitState> = _gridSplitState.asStateFlow()

    /** 更新网格切割状态 */
    fun updateGridSplitState(updater: (GridSplitState) -> GridSplitState) {
        _gridSplitState.update(updater)
    }

    /** 重置网格切割状态 */
    fun resetGridSplitState() {
        _gridSplitState.value = GridSplitState()
    }

    /**
     * 开始网格切割
     *
     * 在 IO 线程中加载纹理图片，然后调用 ImageProcessor.gridSplit() 进行切割。
     */
    fun startGridSplit() {
        val state = _gridSplitState.value
        if (state.texturePath.isEmpty() || state.outputPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _gridSplitState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在加载纹理...", cutCount = 0) }
            try {
                // 加载纹理图片
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val source = BitmapFactory.decodeFile(state.texturePath, options)
                    ?: throw IllegalArgumentException("无法加载纹理: ${state.texturePath}")

                _gridSplitState.update { it.copy(statusText = "正在切割...") }

                // 调用 core 层执行网格切割
                val cutCount = ImageProcessor.gridSplit(
                    source = source,
                    cols = state.columns,
                    rows = state.rows,
                    outputDir = state.outputPath
                )

                source.recycle()

                _gridSplitState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "切割完成: 共 ${state.columns * state.rows} 张，成功 $cutCount 张",
                        cutCount = cutCount
                    )
                }
            } catch (e: Exception) {
                _gridSplitState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "切割失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== XML 加解密 =====
    private val _xmlCryptoState = MutableStateFlow(XmlCryptoState())
    val xmlCryptoState: StateFlow<XmlCryptoState> = _xmlCryptoState.asStateFlow()

    /** 更新 XML 加解密状态 */
    fun updateXmlCryptoState(updater: (XmlCryptoState) -> XmlCryptoState) {
        _xmlCryptoState.update(updater)
    }

    /** 重置 XML 加解密状态 */
    fun resetXmlCryptoState() {
        _xmlCryptoState.value = XmlCryptoState()
    }

    /**
     * 开始 XML 混淆（加密）
     *
     * 在 IO 线程中调用 XMLObfuscator.obfuscate() 对文件进行换行填充混淆。
     */
    fun startXmlEncrypt() {
        val state = _xmlCryptoState.value
        val filePath = state.filePath
        if (filePath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _xmlCryptoState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在混淆...") }
            try {
                val file = File(filePath)
                val content = file.readText(Charsets.UTF_8)

                // 调用 core 层混淆
                val obfuscated = XMLObfuscator.obfuscate(content)

                // 确定输出路径
                val outputFile = if (state.outputPath.isNotEmpty()) {
                    File(state.outputPath, file.name)
                } else {
                    file // 原地覆盖
                }
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(obfuscated, Charsets.UTF_8)

                _xmlCryptoState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "混淆完成: ${outputFile.absolutePath}",
                        successCount = 1
                    )
                }
            } catch (e: Exception) {
                _xmlCryptoState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "混淆失败: ${e.message}",
                        failCount = 1
                    )
                }
            }
        }
    }

    /**
     * 开始 XML 反混淆（解密）
     *
     * 在 IO 线程中调用 XMLObfuscator.deobfuscate() 对文件进行反混淆。
     */
    fun startXmlDecrypt() {
        val state = _xmlCryptoState.value
        val filePath = state.filePath
        if (filePath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _xmlCryptoState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在反混淆...") }
            try {
                val file = File(filePath)
                val content = file.readText(Charsets.UTF_8)

                // 调用 core 层反混淆
                val deobfuscated = XMLObfuscator.deobfuscate(content)

                // 确定输出路径
                val outputFile = if (state.outputPath.isNotEmpty()) {
                    File(state.outputPath, file.name)
                } else {
                    file // 原地覆盖
                }
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(deobfuscated, Charsets.UTF_8)

                _xmlCryptoState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "反混淆完成: ${outputFile.absolutePath}",
                        successCount = 1
                    )
                }
            } catch (e: Exception) {
                _xmlCryptoState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "反混淆失败: ${e.message}",
                        failCount = 1
                    )
                }
            }
        }
    }

    // ===== 图片去白边 =====
    private val _trimImagesState = MutableStateFlow(TrimImagesState())
    val trimImagesState: StateFlow<TrimImagesState> = _trimImagesState.asStateFlow()

    /** 更新去白边状态 */
    fun updateTrimImagesState(updater: (TrimImagesState) -> TrimImagesState) {
        _trimImagesState.update(updater)
    }

    /** 重置去白边状态 */
    fun resetTrimImagesState() {
        _trimImagesState.value = TrimImagesState()
    }

    /**
     * 开始图片去白边
     *
     * 在 IO 线程中遍历文件夹中的图片，逐个调用 ImageProcessor.trimImage() 裁切透明边缘。
     */
    fun startTrimImages() {
        val state = _trimImagesState.value
        if (state.folderPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _trimImagesState.update {
                it.copy(
                    isRunning = true,
                    progress = 0f,
                    statusText = "正在扫描图片...",
                    successCount = 0,
                    failCount = 0
                )
            }
            try {
                val folder = File(state.folderPath)
                // 扫描所有图片文件
                val imageFiles = folder.listFiles()?.filter { file ->
                    file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
                }?.sortedBy { it.name } ?: emptyList()

                val total = imageFiles.size
                _trimImagesState.update { it.copy(imageCount = total, statusText = "找到 $total 张图片，开始处理...") }

                var successCount = 0
                var failCount = 0

                imageFiles.forEachIndexed { index, imageFile ->
                    try {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: run {
                            failCount++
                            return@forEachIndexed
                        }

                        // 调用 core 层裁切透明边缘
                        val trimmed = ImageProcessor.trimImage(bitmap)
                        if (trimmed != null && trimmed !== bitmap) {
                            // 保存裁切后的图片（覆盖原文件）
                            val fos = java.io.FileOutputStream(imageFile)
                            trimmed.compress(
                                when (imageFile.extension.lowercase()) {
                                    "jpg", "jpeg" -> android.graphics.Bitmap.CompressFormat.JPEG
                                    "webp" -> android.graphics.Bitmap.CompressFormat.WEBP
                                    else -> android.graphics.Bitmap.CompressFormat.PNG
                                },
                                100,
                                fos
                            )
                            fos.flush()
                            fos.close()
                            trimmed.recycle()
                        }
                        if (bitmap !== trimmed) bitmap.recycle()
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                    }

                    // 更新进度
                    val pct = (index + 1).toFloat() / total
                    _trimImagesState.update {
                        it.copy(
                            progress = pct,
                            statusText = "处理 ${index + 1}/$total: ${imageFile.name}",
                            successCount = successCount,
                            failCount = failCount
                        )
                    }
                }

                _trimImagesState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "去白边完成: 成功 $successCount, 失败 $failCount",
                        successCount = successCount,
                        failCount = failCount
                    )
                }
            } catch (e: Exception) {
                _trimImagesState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "去白边失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== 帧详情 =====
    private val _frameViewerState = MutableStateFlow(FrameViewerState())
    val frameViewerState: StateFlow<FrameViewerState> = _frameViewerState.asStateFlow()

    /** 更新帧详情状态 */
    fun updateFrameViewerState(updater: (FrameViewerState) -> FrameViewerState) {
        _frameViewerState.update(updater)
    }

    /** 重置帧详情状态 */
    fun resetFrameViewerState() {
        _frameViewerState.value = FrameViewerState()
    }

    /**
     * 加载 Plist 帧数据
     *
     * 在 IO 线程中调用 PlistParser.parse() 解析 plist 文件，
     * 将解析结果转换为 FrameInfo 列表更新到 UI 状态。
     */
    fun loadFrames() {
        val state = _frameViewerState.value
        if (state.plistPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _frameViewerState.update { it.copy(frames = emptyList(), selectedFrameIndex = -1) }
            try {
                // 调用 core 层解析 plist
                val parsedFrames = PlistParser.parse(state.plistPath)

                // 转换为 UI 层的 FrameInfo 列表
                val frameInfos = parsedFrames.map { (_, spriteFrame) ->
                    FrameInfo(
                        name = spriteFrame.name,
                        x = spriteFrame.x,
                        y = spriteFrame.y,
                        width = spriteFrame.w,
                        height = spriteFrame.h,
                        isRotated = spriteFrame.rotated
                    )
                }.sortedBy { it.name }

                _frameViewerState.update {
                    it.copy(frames = frameInfos)
                }
            } catch (e: Exception) {
                _frameViewerState.update {
                    it.copy(frames = emptyList())
                }
            }
        }
    }

    // ===== 文件信息 =====
    private val _fileInfoState = MutableStateFlow(FileInfoState())
    val fileInfoState: StateFlow<FileInfoState> = _fileInfoState.asStateFlow()

    /** 更新文件信息状态 */
    fun updateFileInfoState(updater: (FileInfoState) -> FileInfoState) {
        _fileInfoState.update(updater)
    }

    /** 重置文件信息状态 */
    fun resetFileInfoState() {
        _fileInfoState.value = FileInfoState()
    }

    /**
     * 加载文件详细信息
     *
     * 在 IO 线程中根据文件类型调用 CCZParser / PVRParser 解析文件头，
     * 提取格式、尺寸、压缩方式等元数据信息。
     */
    fun loadFileInfo() {
        val state = _fileInfoState.value
        if (state.filePath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _fileInfoState.update { it.copy(details = emptyList()) }
            try {
                val file = File(state.filePath)
                if (!file.exists()) return@launch

                val details = mutableListOf<FileDetailInfo>()

                // 基本信息
                details.add(FileDetailInfo(
                    name = "文件名称",
                    size = file.name,
                    modifiedDate = "",
                    type = "基本",
                    extraInfo = ""
                ))
                details.add(FileDetailInfo(
                    name = "文件大小",
                    size = formatFileSize(file.length()),
                    modifiedDate = "",
                    type = "基本",
                    extraInfo = ""
                ))
                details.add(FileDetailInfo(
                    name = "修改日期",
                    size = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(file.lastModified())),
                    modifiedDate = "",
                    type = "基本",
                    extraInfo = ""
                ))

                val ext = file.extension.lowercase()

                // CCZ 文件解析
                if (ext == "ccz") {
                    try {
                        val data = file.readBytes()
                        if (data.size >= 12) {
                            val magic = ((data[0].toInt() and 0xFF) shl 24) or
                                    ((data[1].toInt() and 0xFF) shl 16) or
                                    ((data[2].toInt() and 0xFF) shl 8) or
                                    (data[3].toInt() and 0xFF)
                            val isCCZ = magic == 0x435A213F
                            details.add(FileDetailInfo(
                                name = "CCZ 格式",
                                size = if (isCCZ) "有效 CCZ 文件" else "非 CCZ 格式",
                                modifiedDate = "",
                                type = "格式",
                                extraInfo = "Magic: 0x${magic.toUInt().toString(16).uppercase()}"
                            ))
                        }

                        // 尝试进一步解析（如果有 Context）
                        if (::appContext.isInitialized) {
                            val parser = CCZParser(appContext)
                            val compressedLen = ((data[8].toInt() and 0xFF)) or
                                    ((data[9].toInt() and 0xFF) shl 8) or
                                    ((data[10].toInt() and 0xFF) shl 16) or
                                    ((data[11].toInt() and 0xFF) shl 24)
                            details.add(FileDetailInfo(
                                name = "压缩数据长度",
                                size = "$compressedLen 字节",
                                modifiedDate = "",
                                type = "CCZ",
                                extraInfo = ""
                            ))
                        }
                    } catch (e: Exception) {
                        details.add(FileDetailInfo(
                            name = "CCZ 解析",
                            size = "解析失败",
                            modifiedDate = "",
                            type = "错误",
                            extraInfo = e.message ?: "未知错误"
                        ))
                    }
                }

                // PVR 文件解析
                if (ext == "pvr") {
                    try {
                        val data = file.readBytes()
                        val header = PVRParser.parseHeader(data)
                        details.add(FileDetailInfo(
                            name = "PVR 格式",
                            size = "有效 PVR 文件",
                            modifiedDate = "",
                            type = "格式",
                            extraInfo = "像素格式: ${header.format}, ${header.width}x${header.height}"
                        ))
                        details.add(FileDetailInfo(
                            name = "纹理尺寸",
                            size = "${header.width} x ${header.height} 像素",
                            modifiedDate = "",
                            type = "PVR",
                            extraInfo = ""
                        ))
                        details.add(FileDetailInfo(
                            name = "字节序",
                            size = if (header.isLittleEndian) "小端 (Little Endian)" else "大端 (Big Endian)",
                            modifiedDate = "",
                            type = "PVR",
                            extraInfo = ""
                        ))
                    } catch (e: Exception) {
                        details.add(FileDetailInfo(
                            name = "PVR 解析",
                            size = "解析失败",
                            modifiedDate = "",
                            type = "错误",
                            extraInfo = e.message ?: "未知错误"
                        ))
                    }
                }

                // Plist 文件解析
                if (ext == "plist") {
                    try {
                        val frames = PlistParser.parse(state.filePath)
                        details.add(FileDetailInfo(
                            name = "帧数量",
                            size = "${frames.size} 帧",
                            modifiedDate = "",
                            type = "Plist",
                            extraInfo = ""
                        ))
                    } catch (e: Exception) {
                        details.add(FileDetailInfo(
                            name = "Plist 解析",
                            size = "解析失败",
                            modifiedDate = "",
                            type = "错误",
                            extraInfo = e.message ?: "未知错误"
                        ))
                    }
                }

                _fileInfoState.update { it.copy(details = details) }
            } catch (e: Exception) {
                // 忽略
            }
        }
    }

    /**
     * 格式化文件大小为可读字符串
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    // ===== 设置 =====
    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    /** 更新设置状态 */
    fun updateSettingsState(updater: (SettingsState) -> SettingsState) {
        _settingsState.update(updater)
    }

    // ===== 导出纹理 =====
    private val _exportTextureState = MutableStateFlow(ExportTextureState())
    val exportTextureState: StateFlow<ExportTextureState> = _exportTextureState.asStateFlow()

    /** 更新导出纹理状态 */
    fun updateExportTextureState(updater: (ExportTextureState) -> ExportTextureState) {
        _exportTextureState.update(updater)
    }

    /** 重置导出纹理状态 */
    fun resetExportTextureState() {
        _exportTextureState.value = ExportTextureState()
    }

    /**
     * 导出纹理
     *
     * 在 IO 线程中根据文件类型处理：
     * - CCZ 文件: 调用 CCZParser.decompress() 解压后，再调用 PVRParser.parseToBitmap() 转换
     * - PVR 文件: 直接调用 PVRParser.parseToBitmap() 转换
     * - PNG 文件: 直接复制
     * 最终导出为 PNG 格式到指定输出目录。
     */
    fun exportTexture() {
        val state = _exportTextureState.value
        if (state.filePath.isEmpty() || state.outputPath.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _exportTextureState.update { it.copy(isRunning = true, progress = 0f, statusText = "正在读取文件...") }
            try {
                val file = File(state.filePath)
                val data = file.readBytes()
                val ext = file.extension.lowercase()

                var bitmap: android.graphics.Bitmap? = null

                when (ext) {
                    "ccz" -> {
                        // CCZ 文件：解压后解析为 Bitmap
                        _exportTextureState.update { it.copy(statusText = "正在解压 CCZ...", progress = 0.3f) }
                        if (!::appContext.isInitialized) {
                            throw IllegalStateException("ViewModel 未初始化 Context")
                        }
                        val parser = CCZParser(appContext)
                        val decompressed = parser.decompress(
                            cczPath = state.filePath,
                            knownKeys = emptyMap(),
                            bruteForce = false
                        )

                        _exportTextureState.update { it.copy(statusText = "正在解析纹理...", progress = 0.6f) }

                        if (PVRParser.isPVR(decompressed)) {
                            val header = PVRParser.parseHeader(decompressed)
                            bitmap = PVRParser.parseToBitmap(decompressed, header)
                        } else {
                            // 可能是 PNG 格式
                            bitmap = BitmapFactory.decodeByteArray(decompressed, 0, decompressed.size)
                        }
                    }
                    "pvr" -> {
                        // PVR 文件：直接解析
                        _exportTextureState.update { it.copy(statusText = "正在解析 PVR...", progress = 0.3f) }
                        val header = PVRParser.parseHeader(data)
                        bitmap = PVRParser.parseToBitmap(data, header)
                    }
                    "png", "jpg", "jpeg", "webp" -> {
                        // 标准图片格式：直接解码
                        _exportTextureState.update { it.copy(statusText = "正在解码图片...", progress = 0.3f) }
                        bitmap = BitmapFactory.decodeFile(state.filePath)
                    }
                    else -> {
                        throw IllegalArgumentException("不支持的文件格式: .$ext")
                    }
                }

                if (bitmap == null) {
                    throw IllegalArgumentException("无法解析纹理数据")
                }

                // 保存为 PNG
                _exportTextureState.update { it.copy(statusText = "正在保存 PNG...", progress = 0.8f) }

                val outputDir = File(state.outputPath)
                outputDir.mkdirs()

                val outputFileName = file.nameWithoutExtension + ".png"
                val outputFile = File(outputDir, outputFileName)
                val fos = java.io.FileOutputStream(outputFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                bitmap.recycle()

                _exportTextureState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        statusText = "导出完成: ${outputFile.absolutePath}"
                    )
                }
            } catch (e: Exception) {
                _exportTextureState.update {
                    it.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "导出失败: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== 文件浏览器交互 =====

    /** 文件浏览器请求（由发起文件选择的 Screen 设置） */
    private val _fileBrowserRequest = MutableStateFlow<FileBrowserRequest?>(null)
    val fileBrowserRequest: StateFlow<FileBrowserRequest?> = _fileBrowserRequest.asStateFlow()

    /** 文件浏览器返回结果（由 FileBrowserScreen 选择后设置） */
    private val _fileBrowserResult = MutableStateFlow<String>("")
    val fileBrowserResult: StateFlow<String> = _fileBrowserResult.asStateFlow()

    /**
     * 发起文件选择请求
     *
     * @param requestId 请求标识（如 "crop_plist", "crack_ccz" 等）
     * @param allowedExtensions 允许的文件扩展名列表（如 ["plist", "png"]）
     * @param selectMode 文件选择模式
     */
    fun requestFileSelection(
        requestId: String,
        allowedExtensions: List<String> = emptyList(),
        selectMode: FileSelectMode = FileSelectMode.FILE
    ) {
        _fileBrowserRequest.value = FileBrowserRequest(
            requestId = requestId,
            allowedExtensions = allowedExtensions,
            selectMode = selectMode
        )
    }

    /**
     * 设置文件浏览器返回结果
     *
     * @param path 选择的文件/文件夹路径
     */
    fun setFileBrowserResult(path: String) {
        _fileBrowserResult.value = path
    }

    /**
     * 清除文件浏览器请求和结果
     */
    fun clearFileBrowserState() {
        _fileBrowserRequest.value = null
        _fileBrowserResult.value = ""
    }
}
