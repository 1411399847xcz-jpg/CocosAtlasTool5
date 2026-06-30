package com.cocos.atlastool.core

import android.content.Context
import android.util.Log
import java.io.File
import java.util.zip.Inflater

/**
 * CCZ 容器解压器 + XXTEA 解密
 *
 * CCZ 是 Cocos2d-x 使用的压缩容器格式，文件结构为：
 *   - 4 字节 Magic：'CCZ!' (0x435A213F)
 *   - 2 字节 flags
 *   - 2 字节 headerSize
 *   - 4 字节 compressedLength（zlib 压缩数据长度）
 *   - N 字节 zlib 压缩数据
 *
 * 解压后的数据可能是：
 *   - 原始 PNG/PVR 纹理数据（无需解密）
 *   - XXTEA 加密数据（需要密钥解密后才能得到原始纹理）
 *
 * 本类提供完整的 CCZ 解压 + XXTEA 解密流程，支持已知密钥解密和 PVR 格式验证。
 */
class CCZParser(private val context: Context) {

    companion object {
        private const val TAG = "CCZParser"

        /** CCZ 文件 Magic Number: "CCZ!" */
        private const val CCZ_MAGIC: Int = 0x435A213F

        /** CCZ 头部固定大小（12 字节） */
        private const val CCZ_HEADER_SIZE: Int = 12
    }

    /**
     * 解压 CCZ 文件
     *
     * 完整流程：
     * 1. 读取并验证 CCZ 文件头
     * 2. 使用 zlib 解压原始数据
     * 3. 尝试用已知密钥列表进行 XXTEA 解密
     * 4. 验证解密结果是否为有效的 PVR/PNG 数据
     *
     * @param cczPath    CCZ 文件路径
     * @param knownKeys  已知的密钥映射（名称 → 密钥字符串）
     * @param bruteForce 是否在已知密钥失败后进行暴力破解
     * @param onProgress 进度回调（百分比, 状态描述）
     * @return 解密/解压后的原始纹理数据字节数组
     * @throws IllegalArgumentException 如果文件格式不正确
     * @throws RuntimeException 如果解压或解密失败
     */
    fun decompress(
        cczPath: String,
        knownKeys: Map<String, String>,
        bruteForce: Boolean = true,
        onProgress: ((Int, String) -> Unit)? = null
    ): ByteArray {
        onProgress?.invoke(0, "正在读取 CCZ 文件: ${File(cczPath).name}")

        // ---- 1. 读取文件 ----
        val file = File(cczPath)
        if (!file.exists()) {
            throw IllegalArgumentException("CCZ 文件不存在: $cczPath")
        }
        val rawData = file.readBytes()

        onProgress?.invoke(10, "正在验证 CCZ 头部...")

        // ---- 2. 验证 CCZ Magic ----
        validateCCZHeader(rawData)

        onProgress?.invoke(20, "正在 zlib 解压...")

        // ---- 3. zlib 解压 ----
        val decompressed = zlibDecompress(rawData)
        if (decompressed == null) {
            throw RuntimeException("zlib 解压失败，数据可能已损坏")
        }

        onProgress?.invoke(50, "zlib 解压完成，数据大小: ${decompressed.size} 字节")

        // ---- 4. 检查是否已经是原始纹理（无需解密） ----
        if (PVRParser.isPVR(decompressed) || isPNG(decompressed)) {
            onProgress?.invoke(100, "数据为原始纹理，无需解密")
            return decompressed
        }

        onProgress?.invoke(60, "尝试使用已知密钥解密...")

        // ---- 5. 尝试使用已知密钥解密 ----
        for ((keyName, keyValue) in knownKeys) {
            onProgress?.invoke(65, "尝试密钥: $keyName")
            try {
                val keyBytes = keyValue.toByteArray(Charsets.UTF_8)
                val decrypted = XXTEA.decrypt(decompressed, keyBytes)

                if (PVRParser.isPVR(decrypted) || isPNG(decrypted)) {
                    onProgress?.invoke(100, "使用密钥 '$keyName' 解密成功")
                    return decrypted
                }
            } catch (e: Exception) {
                Log.d(TAG, "密钥 '$keyName' 解密失败: ${e.message}")
                // 继续尝试下一个密钥
            }
        }

        if (bruteForce) {
            onProgress?.invoke(70, "已知密钥均失败，尝试暴力破解...")
            // 暴力破解逻辑委托给 BruteForceDecryptor
            // 这里返回解压后的数据，由调用者决定是否继续暴力破解
            return decompressed
        }

        throw RuntimeException(
            "所有已知密钥均解密失败。解压后数据前 16 字节: " +
            decompressed.copyOf(16).joinToString(" ") { "%02X".format(it) }
        )
    }

    /**
     * 验证 CCZ 文件头
     *
     * 检查文件前 4 字节是否为 "CCZ!" magic number。
     *
     * @param data CCZ 文件的原始字节数据
     * @throws IllegalArgumentException 如果 magic 不匹配或数据不足
     */
    private fun validateCCZHeader(data: ByteArray) {
        if (data.size < CCZ_HEADER_SIZE) {
            throw IllegalArgumentException(
                "CCZ 文件数据不足，至少需要 $CCZ_HEADER_SIZE 字节头部，当前仅 ${data.size} 字节"
            )
        }

        // 读取前 4 字节作为 big-endian int 比较
        val magic = ((data[0].toInt() and 0xFF) shl 24) or
                    ((data[1].toInt() and 0xFF) shl 16) or
                    ((data[2].toInt() and 0xFF) shl 8) or
                     (data[3].toInt() and 0xFF)

        if (magic != CCZ_MAGIC) {
            throw IllegalArgumentException(
                "无效的 CCZ magic number: 0x${magic.toUInt().toString(16).uppercase()}，" +
                "期望 0x435A213F ('CCZ!')"
            )
        }
    }

    /**
     * zlib 解压 CCZ 数据
     *
     * CCZ 文件结构：12 字节头部 + zlib 压缩数据。
     * 头部偏移 4-6 为 flags，8-11 为压缩数据长度。
     *
     * @param data CCZ 文件的原始字节数据
     * @return 解压后的字节数组，解压失败返回 null
     */
    private fun zlibDecompress(data: ByteArray): ByteArray? {
        return try {
            // 读取压缩数据长度（偏移 8，4 字节，小端序）
            val compressedLen = ((data[8].toInt() and 0xFF)) or
                               ((data[9].toInt() and 0xFF) shl 8) or
                               ((data[10].toInt() and 0xFF) shl 16) or
                               ((data[11].toInt() and 0xFF) shl 24)

            // 跳过 12 字节头部，提取 zlib 压缩数据
            val compressedData = data.copyOfRange(CCZ_HEADER_SIZE, CCZ_HEADER_SIZE + compressedLen)

            // 使用 Inflater 解压（zlib 格式）
            val inflater = Inflater()
            inflater.setInput(compressedData)

            // 预估解压后大小（CCZ 通常压缩比约 30%~60%）
            val estimatedSize = maxOf(compressedLen * 4, 1024 * 1024) // 至少 1MB
            val output = ByteArray(estimatedSize)
            val decompressedLen = inflater.inflate(output)

            inflater.end()

            output.copyOf(decompressedLen)
        } catch (e: Exception) {
            Log.e(TAG, "zlib 解压异常", e)
            null
        }
    }

    /**
     * 检查数据是否为 PNG 格式
     *
     * PNG magic: 89 50 4E 47 0D 0A 1A 0A
     */
    private fun isPNG(data: ByteArray): Boolean {
        if (data.size < 8) return false
        return data[0] == 0x89.toByte() &&
               data[1] == 0x50.toByte() &&
               data[2] == 0x4E.toByte() &&
               data[3] == 0x47.toByte() &&
               data[4] == 0x0D.toByte() &&
               data[5] == 0x0A.toByte() &&
               data[6] == 0x1A.toByte() &&
               data[7] == 0x0A.toByte()
    }

    /**
     * 检查数据是否为 PVR 格式
     *
     * 委托给 PVRParser.isPVR
     */
    fun isPVR(data: ByteArray): Boolean {
        return PVRParser.isPVR(data)
    }
}
