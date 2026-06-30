package com.cocos.atlastool.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片处理器
 *
 * 提供精灵表裁剪、网格切分、透明边裁切和批量处理功能。
 * 用于从 Cocos2d-x 纹理图集中提取单个精灵帧图片。
 *
 * 核心功能：
 *   - 根据 plist 帧数据裁剪精灵（支持旋转帧）
 *   - 网格等分切分（用于没有 plist 的规则图集）
 *   - 透明边缘自动裁切
 *   - 协程版批量裁剪（带进度反馈）
 */
object ImageProcessor {

    private const val TAG = "ImageProcessor"

    /**
     * 裁剪单个精灵帧
     *
     * 从纹理图集中根据 SpriteFrame 的坐标信息裁剪出单个精灵。
     * 正确处理旋转帧：如果 rotated 为 true，则裁剪后需要旋转 90 度。
     *
     * @param source    源纹理 Bitmap
     * @param frame     精灵帧数据（包含坐标和尺寸信息）
     * @param outputPath 输出文件路径
     * @param format    输出图片格式（PNG/JPEG/WEBP）
     * @param quality   压缩质量（0~100，PNG 时忽略）
     * @return 是否成功保存
     * @throws IllegalArgumentException 如果裁剪区域超出源图范围
     */
    fun cropSprite(
        source: Bitmap,
        frame: PlistParser.SpriteFrame,
        outputPath: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        // ---- 校验裁剪区域 ----
        if (frame.x < 0 || frame.y < 0 ||
            frame.x + frame.w > source.width ||
            frame.y + frame.h > source.height
        ) {
            Log.w(TAG, "裁剪区域 ${frame.name} 超出源图范围: " +
                "source=${source.width}x${source.height}, " +
                "rect=(${frame.x}, ${frame.y}, ${frame.w}, ${frame.h})")
        }

        // ---- 安全裁剪：限制在源图范围内 ----
        val safeX = frame.x.coerceIn(0, source.width - 1)
        val safeY = frame.y.coerceIn(0, source.height - 1)
        val safeW = minOf(frame.w, source.width - safeX)
        val safeH = minOf(frame.h, source.height - safeY)

        // ---- 裁剪原始区域 ----
        val cropped = try {
            Bitmap.createBitmap(source, safeX, safeY, safeW, safeH)
        } catch (e: Exception) {
            Log.e(TAG, "裁剪帧 ${frame.name} 失败", e)
            return false
        }

        // ---- 处理旋转帧 ----
        val result = if (frame.rotated) {
            // 旋转帧：裁剪区域宽高互换，需要旋转 90 度
            // TexturePacker 中旋转帧实际存储时 w 和 h 已经是旋转后的尺寸
            rotateBitmap(cropped, 90f)
        } else {
            cropped
        }

        // ---- 确保输出目录存在 ----
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        // ---- 保存到文件 ----
        return try {
            val fos = FileOutputStream(outputFile)
            result.compress(format, quality, fos)
            fos.flush()
            fos.close()
            result.recycle()
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存帧 ${frame.name} 到 $outputPath 失败", e)
            result.recycle()
            false
        }
    }

    /**
     * 网格等分切分
     *
     * 将源纹理按指定行列数均匀切分为多个小图。
     * 适用于没有 plist 描述的规则精灵表。
     *
     * @param source    源纹理 Bitmap
     * @param cols      列数
     * @param rows      行数
     * @param outputDir 输出目录
     * @param format    输出图片格式
     * @param quality   压缩质量
     * @return 成功切分的图片数量
     */
    fun gridSplit(
        source: Bitmap,
        cols: Int,
        rows: Int,
        outputDir: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Int {
        require(cols > 0 && rows > 0) { "行列数必须大于 0" }

        val dir = File(outputDir)
        dir.mkdirs()

        val cellW = source.width / cols
        val cellH = source.height / rows
        var count = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * cellW
                val y = row * cellH

                try {
                    val cropped = Bitmap.createBitmap(source, x, y, cellW, cellH)
                    val filename = "sprite_${String.format("%03d", row * cols + col)}.${extForFormat(format)}"
                    val outputFile = File(dir, filename)
                    val fos = FileOutputStream(outputFile)
                    cropped.compress(format, quality, fos)
                    fos.flush()
                    fos.close()
                    cropped.recycle()
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "切分 (${row}, ${col}) 失败", e)
                }
            }
        }

        return count
    }

    /**
     * 裁切透明边缘
     *
     * 自动检测并裁切 Bitmap 四周的透明像素区域，
     * 返回只包含非透明内容的最小矩形 Bitmap。
     *
     * 算法：逐行逐列扫描，找到第一个/最后一个非透明像素的坐标。
     *
     * @param bitmap 原始 Bitmap
     * @return 裁切后的 Bitmap，如果整张图都是透明的返回 null
     */
    fun trimImage(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height

        // ---- 获取像素数组 ----
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // ---- 找到非透明区域的边界 ----
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] shr 24) and 0xFF
                if (alpha > 0) {
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                }
            }
        }

        // ---- 全透明图像 ----
        if (maxX < 0 || maxY < 0) {
            return null
        }

        // ---- 裁剪 ----
        val trimW = maxX - minX + 1
        val trimH = maxY - minY + 1

        // 如果裁剪区域等于原图，直接返回原图
        if (trimW == width && trimH == height) {
            return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, minX, minY, trimW, trimH)
        } catch (e: Exception) {
            Log.e(TAG, "裁切透明边缘失败", e)
            bitmap
        }
    }

    /**
     * 批量裁剪精灵帧（协程版）
     *
     * 解析 plist 文件，加载对应纹理，批量裁剪所有精灵帧。
     * 使用协程在 IO 线程池中执行，不阻塞 UI 线程。
     *
     * @param plistPath   plist 文件路径
     * @param texturePath 纹理图片路径（PNG/PVR）
     * @param outputDir   输出目录
     * @param format      输出图片格式
     * @param quality     压缩质量
     * @param keepStructure 是否保持 plist 中的目录结构（如 "walk/run_01.png"）
     * @param onProgress  进度回调（百分比, 当前处理的帧名）
     * @return Pair（成功数量, 失败数量）
     */
    suspend fun batchCrop(
        plistPath: String,
        texturePath: String,
        outputDir: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
        keepStructure: Boolean = true,
        onProgress: suspend (Int, String) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // ---- 1. 解析 plist ----
        val frames = PlistParser.parse(plistPath)
        if (frames.isEmpty()) {
            onProgress(0, "Plist 中没有找到帧数据")
            return@withContext Pair(0, 0)
        }

        onProgress(0, "共 ${frames.size} 帧，正在加载纹理...")

        // ---- 2. 加载纹理 ----
        val source = loadTexture(texturePath)
            ?: throw IllegalArgumentException("无法加载纹理: $texturePath")

        // ---- 3. 创建输出目录 ----
        val outDir = File(outputDir)
        outDir.mkdirs()

        // ---- 4. 逐帧裁剪 ----
        var successCount = 0
        var failCount = 0

        for ((index, entry) in frames.entries.withIndex()) {
            ensureActive() // 协程取消检查

            val frameName = entry.key
            val frame = entry.value

            // 确定输出路径
            val outputPath = if (keepStructure && frameName.contains("/")) {
                // 保持目录结构：walk/run_01.png → outputDir/walk/run_01.png
                File(outDir, frameName).absolutePath
            } else {
                // 扁平结构：所有帧输出到同一目录
                val ext = extForFormat(format)
                File(outDir, "${frameName.removeSuffix(".png")}.${ext}").absolutePath
            }

            val success = cropSprite(source, frame, outputPath, format, quality)
            if (success) {
                successCount++
            } else {
                failCount++
            }

            // 报告进度
            val percent = ((index + 1) * 100 / frames.size)
            if (index % 10 == 0 || index == frames.size - 1) {
                onProgress(percent, "裁剪 ${index + 1}/${frames.size}: $frameName")
            }
        }

        // ---- 5. 回收纹理 ----
        source.recycle()

        onProgress(100, "批量裁剪完成: 成功 $successCount, 失败 $failCount")
        Pair(successCount, failCount)
    }

    /**
     * 加载纹理图片
     *
     * 支持加载 PNG 文件和 PVR 文件（通过 PVRParser）。
     *
     * @param texturePath 纹理文件路径
     * @return Bitmap 对象，加载失败返回 null
     */
    private fun loadTexture(texturePath: File): Bitmap? {
        return loadTexture(texturePath.absolutePath)
    }

    private fun loadTexture(texturePath: String): Bitmap? {
        val file = File(texturePath)
        if (!file.exists()) {
            Log.e(TAG, "纹理文件不存在: $texturePath")
            return null
        }

        return when (file.extension.lowercase()) {
            "pvr" -> {
                // 使用 PVRParser 解析 PVR 格式
                try {
                    val data = file.readBytes()
                    val header = PVRParser.parseHeader(data)
                    PVRParser.parseToBitmap(data, header)
                } catch (e: Exception) {
                    Log.e(TAG, "PVR 解析失败: $texturePath", e)
                    null
                }
            }
            "png", "jpg", "jpeg", "webp" -> {
                // 使用 BitmapFactory 解码标准图片格式
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false
                }
                BitmapFactory.decodeFile(texturePath, options)
            }
            else -> {
                // 尝试作为 PNG/PVR 直接解码
                try {
                    val data = file.readBytes()
                    if (PVRParser.isPVR(data)) {
                        val header = PVRParser.parseHeader(data)
                        PVRParser.parseToBitmap(data, header)
                    } else {
                        BitmapFactory.decodeFile(texturePath)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "纹理解码失败: $texturePath", e)
                    null
                }
            }
        }
    }

    /**
     * 旋转 Bitmap
     *
     * @param bitmap 源 Bitmap
     * @param degrees 旋转角度（90, 180, 270）
     * @return 旋转后的新 Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    /**
     * 根据图片格式获取文件扩展名
     */
    private fun extForFormat(format: Bitmap.CompressFormat): String {
        return when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "png"
        }
    }
}
