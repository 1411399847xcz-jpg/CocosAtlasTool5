package com.cocos.atlastool.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cocos.atlastool.ui.screens.*

/**
 * 应用导航图
 *
 * 使用 Jetpack Navigation Compose 定义所有路由和对应的 Screen。
 * 共享同一个 MainViewModel 实例，通过 StateFlow 在各 Screen 间传递数据。
 *
 * 路由列表：
 * - main: 主菜单屏幕
 * - crop_single: 单文件拆分
 * - crop_batch: 批量拆分
 * - grid_split: 网格切割
 * - view_frames: 帧详情
 * - file_info: 文件信息
 * - export_texture: 导出纹理
 * - crack_center: 密钥破解
 * - simplify: 动画精简
 * - trim_images: 去空白
 * - xml_crypto: XML 加解密
 * - settings: 设置
 * - file_browser: 文件浏览器（通用组件）
 */

/** 路由常量 */
object Routes {
    const val MAIN = "main"
    const val CROP_SINGLE = "crop_single"
    const val CROP_BATCH = "crop_batch"
    const val GRID_SPLIT = "grid_split"
    const val VIEW_FRAMES = "view_frames"
    const val FILE_INFO = "file_info"
    const val EXPORT_TEXTURE = "export_texture"
    const val CRACK_CENTER = "crack_center"
    const val SIMPLIFY = "simplify"
    const val TRIM_IMAGES = "trim_images"
    const val XML_CRYPTO = "xml_crypto"
    const val SETTINGS = "settings"
    const val FILE_BROWSER = "file_browser"
}

/**
 * 应用导航入口 Composable
 *
 * 创建 NavController、NavHost 和共享的 ViewModel，
 * 注册所有路由及其对应的 Screen Composable。
 */
@Composable
fun AppNavigation() {
    // 导航控制器
    val navController = rememberNavController()

    // 共享的 ViewModel 实例
    val viewModel: com.cocos.atlastool.ui.viewmodel.MainViewModel = viewModel()

    // 观察文件浏览器请求状态，自动导航到文件浏览器
    val fileBrowserRequest by viewModel.fileBrowserRequest.collectAsState()

    // 当有文件选择请求时，自动跳转到文件浏览器
    if (fileBrowserRequest != null) {
        navController.navigate(Routes.FILE_BROWSER) {
            // 避免在返回栈中创建多个文件浏览器实例
            popUpTo(Routes.FILE_BROWSER) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        // ===== 主菜单 =====
        composable(Routes.MAIN) {
            MainScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // ===== 单文件拆分 =====
        composable(Routes.CROP_SINGLE) {
            CropScreen(
                type = com.cocos.atlastool.ui.viewmodel.CropType.SINGLE,
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 批量拆分 =====
        composable(Routes.CROP_BATCH) {
            CropScreen(
                type = com.cocos.atlastool.ui.viewmodel.CropType.BATCH,
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 网格切割 =====
        composable(Routes.GRID_SPLIT) {
            GridSplitScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 帧详情 =====
        composable(Routes.VIEW_FRAMES) {
            FrameViewerScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 文件信息 =====
        composable(Routes.FILE_INFO) {
            FileInfoScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 导出纹理 =====
        composable(Routes.EXPORT_TEXTURE) {
            ExportTextureScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 密钥破解 =====
        composable(Routes.CRACK_CENTER) {
            CrackCenterScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 动画精简 =====
        composable(Routes.SIMPLIFY) {
            SimplifyScreen(
                onBack = { navController.popBackStack() },
                onBrowseFile = { requestId, extensions ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        allowedExtensions = extensions,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE
                    )
                },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== 去空白 =====
        composable(Routes.TRIM_IMAGES) {
            TrimImagesScreen(
                onBack = { navController.popBackStack() },
                onBrowseFolder = { requestId ->
                    viewModel.requestFileSelection(
                        requestId = requestId,
                        selectMode = com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER
                    )
                },
                viewModel = viewModel
            )
        }

        // ===== XML 加解密 =====
        composable(Routes.XML_CRYPTO) {
            XmlCryptoScreen(
                onBack = { navController.popBackStack() },
                onOperation = { operation ->
                    // TODO: 根据操作类型进入对应的操作流程
                    // 目前由 XmlCryptoScreen 内部处理
                },
                viewModel = viewModel
            )
        }

        // ===== 设置 =====
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        // ===== 文件浏览器（通用组件） =====
        composable(Routes.FILE_BROWSER) {
            val request = fileBrowserRequest

            if (request != null) {
                FileBrowserScreen(
                    title = when (request.selectMode) {
                        com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE -> "选择文件"
                        com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER -> "选择文件夹"
                    },
                    allowedExtensions = request.allowedExtensions,
                    selectMode = when (request.selectMode) {
                        com.cocos.atlastool.ui.viewmodel.FileSelectMode.FILE -> SelectMode.FILE
                        com.cocos.atlastool.ui.viewmodel.FileSelectMode.FOLDER -> SelectMode.FOLDER
                    },
                    onResult = { selectedPath ->
                        // 将选择结果写入 ViewModel
                        viewModel.setFileBrowserResult(selectedPath)
                        // 清除请求状态
                        viewModel.clearFileBrowserState()
                        // 返回上一页
                        navController.popBackStack()
                    },
                    onBack = {
                        // 取消选择，清除请求状态并返回
                        viewModel.clearFileBrowserState()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
