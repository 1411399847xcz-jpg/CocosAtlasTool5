package com.cocos.atlastool.core

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PVR 纹理文件解析器
 *
 * 支持 Cocos2d-x 常用的 PVR 格式纹理文件解析。
 * PVR 是 PowerVR 纹理压缩格式，广泛用于 iOS/Android 游戏开发。
 * 文件头包含 magic number、像素格式、宽高等元数据，
 * 解析后可将像素数据解码为 Android Bitmap 对象。
 */
object PVRParser {

    /** PVR Magic Number — 小端序 */
    private const val MAGIC_LE: Int = 0x03525650

    /** PVR Magic Number — 大端序 */
    private const val MAGIC_BE: Int = 0x50565203

    /**
     * PVR 文件头数据类
     *
     * @property format         像素格式编号（对应 PVR 标准格式枚举）
     * @property width          纹理宽度（像素）
     * @property height         纹理高度（像素）
     * @property dataOffset     像素数据在文件中的偏移量（字节）
     * @property isLittleEndian 文件是否使用小端字节序
     */
    data class PVRHeader(
        val format: Int,
        val width: Int,
        val height: Int,
        val dataOffset: Int,
        val isLittleEndian: Boolean
    )

    /**
     * PVR 像素格式常量
     * 参考PVRTexTool 和 Cocos2d-x 使用的格式编号
     */
    private const val PVRTC_2BPP_RGB: Int = 0
    private const val PVRTC_2BPP_RGBA: Int = 1
    private const val PVRTC_4BPP_RGB: Int = 2
    private const val PVRTC_4BPP_RGBA: Int = 3
    private const val DXT1: Int = 4
    private const val DXT2: Int = 5
    private const val DXT3: Int = 6
    private const val DXT4: Int = 7
    private const val DXT5: Int = 8
    private const val PVRTC_2BPP_RGB_A: Int = 9
    private const val PVRTC_2BPP_RGBA_A: Int = 10
    private const val PVRTC_4BPP_RGB_A: Int = 11
    private const val PVRTC_4BPP_RGBA_A: Int = 12
    private const val ETC1: Int = 13
    private const val ETC2_RGB: Int = 14
    private const val ETC2_RGBA: Int = 15

    /**
     * 解析 PVR 文件头
     *
     * 读取文件前 52 字节的标准 PVR v3 头部，
     * 识别 magic number 确定字节序后提取关键字段。
     *
     * @param data PVR 文件的完整字节数组
     * @return PVRHeader 数据类，包含解析出的元数据
     * @throws IllegalArgumentException 如果数据不足或 magic 不匹配
     */
    fun parseHeader(data: ByteArray): PVRHeader {
        require(data.size >= 52) {
            "PVR 文件数据不足，至少需要 52 字节头部，当前仅 ${data.size} 字节"
        }

        // 读取前 4 字节判断 magic
        val magic = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int

        val isLittleEndian: Boolean
        if (magic == MAGIC_LE) {
            isLittleEndian = true
        } else if (magic == MAGIC_BE) {
            isLittleEndian = false
        } else {
            throw IllegalArgumentException(
                "无效的 PVR magic number: 0x${magic.toUInt().toString(16).uppercase()}，" +
                "期望 0x03525650（小端）或 0x50565203（大端）"
            )
        }

        val order = if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

        // 按标准 PVR v3 头部布局解析
        val headerBuf = ByteBuffer.wrap(data, 0, 52).order(order)

        // 4 字节：version
        val version = headerBuf.int
        // 4 字节：flags
        val flags = headerBuf.int
        // 4 字节：pixelFormat（四字符码 → 作为 int 读取）
        val pixelFormatFourCC = headerBuf.int
        // 4 字节：pixelFormat（数值格式）
        val pixelFormat = headerBuf.int
        // 4 字节：colourSpace
        val colourSpace = headerBuf.int
        // 4 字节：channelType
        val channelType = headerBuf.int
        // 4 字节：height
        val height = headerBuf.int
        // 4 字节：width
        val width = headerBuf.int
        // 4 字节：depth
        val depth = headerBuf.int
        // 4 字节：numberOfSurfaces
        val numberOfSurfaces = headerBuf.int
        // 4 字节：numberOfFaces
        val numberOfFaces = headerBuf.int
        // 4 字节：mipMapCount
        val mipMapCount = headerBuf.int
        // 4 字节：metadataSize
        val metadataSize = headerBuf.int

        // 像素数据偏移 = 头部大小（52字节） + 元数据大小
        val dataOffset = 52 + metadataSize

        return PVRHeader(
            format = pixelFormat,
            width = width,
            height = height,
            dataOffset = dataOffset,
            isLittleEndian = isLittleEndian
        )
    }

    /**
     * 将 PVR 数据解析为 Bitmap
     *
     * 根据头部中的像素格式，将原始像素数据解码为 ARGB_8888 格式的 Bitmap。
     *
     * @param data   PVR 文件的完整字节数组
     * @param header 已解析的 PVR 头部信息
     * @return 解码后的 Bitmap 对象
     * @throws UnsupportedOperationException 如果像素格式不被支持
     */
    fun parseToBitmap(data: ByteArray, header: PVRHeader): Bitmap {
        // 提取像素数据区域
        val pixelData = data.copyOfRange(header.dataOffset, data.size)
        val w = header.width
        val h = header.height

        // 创建目标 Bitmap（ARGB_8888 格式保证精度）
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)

        when (header.format) {
            8 -> decodeRGB888(pixelData, w, h, pixels)
            9 -> decodeRGBA8888(pixelData, w, h, pixels, false)
            13 -> decodeBGRA8888(pixelData, w, h, pixels)
            10 -> decodeRGB565(pixelData, w, h, pixels)
            11 -> decodeRGBA4444(pixelData, w, h, pixels)
            else -> {
                bitmap.recycle()
                throw UnsupportedOperationException(
                    "不支持的 PVR 像素格式: ${header.format}。" +
                    "当前支持: 8(RGB888), 9(RGBA8888), 13(BGRA8888), 10(RGB565), 11(RGBA4444)"
                )
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * 检查数据是否为 PVR 格式
     */
    fun isPVR(data: ByteArray): Boolean {
        if (data.size < 4) return false
        val magic = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        return magic == MAGIC_LE || magic == MAGIC_BE
    }

    // ===================== 像素解码方法 =====================

    /**
     * 解码 RGB888 格式（每像素 3 字节，无 Alpha）
     * 格式 8：R, G, B 顺序
     */
    private fun decodeRGB888(data: ByteArray, w: Int, h: Int, pixels: IntArray) {
        var offset = 0
        for (i in 0 until w * h) {
            val r = data[offset++].toInt() and 0xFF
            val g = data[offset++].toInt() and 0xFF
            val b = data[offset++].toInt() and 0xFF
            // Alpha 默认设为完全不透明
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    /**
     * 解码 RGBA8888 格式（每像素 4 字节）
     * 格式 9：R, G, B, A 顺序
     */
    private fun decodeRGBA8888(data: ByteArray, w: Int, h: Int, pixels: IntArray, isBGRA: Boolean) {
        var offset = 0
        for (i in 0 until w * h) {
            val c1 = data[offset++].toInt() and 0xFF
            val c2 = data[offset++].toInt() and 0xFF
            val c3 = data[offset++].toInt() and 0xFF
            val c4 = data[offset++].toInt() and 0xFF

            pixels[i] = if (isBGRA) {
                // BGRA → ARGB
                (c4 shl 24) or (c3 shl 16) or (c2 shl 8) or c1
            } else {
                // RGBA → ARGB
                (c4 shl 24) or (c1 shl 16) or (c2 shl 8) or c3
            }
        }
    }

    /**
     * 解码 BGRA8888 格式（每像素 4 字节）
     * 格式 13：B, G, R, A 顺序
     */
    private fun decodeBGRA8888(data: ByteArray, w: Int, h: Int, pixels: IntArray) {
        var offset = 0
        for (i in 0 until w * h) {
            val b = data[offset++].toInt() and 0xFF
            val g = data[offset++].toInt() and 0xFF
            val r = data[offset++].toInt() and 0xFF
            val a = data[offset++].toInt() and 0xFF
            // BGRA → ARGB
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    /**
     * 解码 RGB565 格式（每像素 2 字节，16 位色）
     * 格式 10：高 5 位 R，中 6 位 G，低 5 位 B
     */
    private fun decodeRGB565(data: ByteArray, w: Int, h: Int, pixels: IntArray) {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until w * h) {
            val pixel = buf.short.toUInt() and 0xFFFFu
            val r = ((pixel shr 11) and 0x1Fu).toInt()
            val g = ((pixel shr 5) and 0x3Fu).toInt()
            val b = (pixel and 0x1Fu).toInt()

            // 将 5/6 位色扩展到 8 位：乘以 255 再除以最大值
            val r8 = (r * 255 + 15) / 31
            val g8 = (g * 255 + 31) / 63
            val b8 = (b * 255 + 15) / 31

            pixels[i] = (0xFF shl 24) or (r8 shl 16) or (g8 shl 8) or b8
        }
    }

    /**
     * 解码 RGBA4444 格式（每像素 2 字节，16 位色带 Alpha）
     * 格式 11：高 4 位 R，中 4 位 G，中 4 位 B，低 4 位 A
     */
    private fun decodeRGBA4444(data: ByteArray, w: Int, h: Int, pixels: IntArray) {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until w * h) {
            val pixel = buf.short.toUInt() and 0xFFFFu
            val r = ((pixel shr 12) and 0xFu).toInt()
            val g = ((pixel shr 8) and 0xFu).toInt()
            val b = ((pixel shr 4) and 0xFu).toInt()
            val a = (pixel and 0xFu).toInt()

            // 4 位色扩展到 8 位
            val r8 = (r * 255 + 7) / 15
            val g8 = (g * 255 + 7) / 15
            val b8 = (b * 255 + 7) / 15
            val a8 = (a * 255 + 7) / 15

            pixels[i] = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
        }
    }
}
