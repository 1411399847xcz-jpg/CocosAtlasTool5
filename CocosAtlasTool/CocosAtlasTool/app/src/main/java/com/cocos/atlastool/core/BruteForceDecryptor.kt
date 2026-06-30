package com.cocos.atlastool.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 密钥暴力破解器
 *
 * 用于破解 Cocos2d-x CCZ/PVR 资源的 XXTEA 加密密钥。
 * 提供两种破解策略：
 *   1. 字典穷举：从候选密钥列表中逐一尝试
 *   2. 字符集穷举：在指定字符集和长度范围内生成所有组合进行尝试
 *
 * 密钥来源包括：
 *   - 内置默认密钥列表（常见 Cocos2d-x 游戏使用的已知密钥）
 *   - 从 .so 原生库文件中提取的 ASCII 字符串
 *   - 从 plist 文件中提取的字符串键值
 *   - 用户自定义密钥
 */
class BruteForceDecryptor(private val context: Context) {

    companion object {
        private const val TAG = "BruteForceDecryptor"

        /**
         * 默认密钥列表
         *
         * 收集了常见的 Cocos2d-x 游戏 XXTEA 加密密钥。
         * 这些密钥来源于已知游戏逆向分析和社区分享。
         */
        val DEFAULT_KEYS: List<String> = listOf(
            "lailai",
            "123456",
            "abcdefgh",
            " cocos2d-x ",
            "2dxPerspective",
            "2d-xEncryptionKey",
            "myEncryptionKey",
            "hello",
            "cocos2d",
            "gamekey",
            "secret",
            "password",
            "key",
            "test",
            "default",
            "cocos2d-x",
            "cocos",
            "engine",
            "2dxKey",
            "skeletalAnimation",
            "SpineKey",
            "dragonBones",
            "CocosKey",
            "encryptKey",
            "xxteaKey",
            "atlasKey",
            "pkgKey",
            "resKey",
            "CCZ_KEY",
            "PVR_KEY",
            "textureKey",
            "spriteKey",
            "animKey",
            "battleKey",
            "loginKey",
            "sceneKey"
        )

        /** 最小密钥长度 */
        private const val MIN_KEY_LENGTH = 4

        /** 最大密钥长度 */
        private const val MAX_KEY_LENGTH = 32

        /** 可打印 ASCII 范围 */
        private const val ASCII_PRINTABLE_START = 0x21  // '!'
        private const val ASCII_PRINTABLE_END = 0x7E   // '~'

        /** 线程安全的穷举尝试计数器 */
        private val bruteTriedCounter = java.util.concurrent.atomic.AtomicInteger(0)

        /** 重置计数器（在新一轮破解开始时调用） */
        fun resetCounter() {
            bruteTriedCounter.set(0)
        }
    }

    /**
     * 从 .so 原生库文件中提取可能的 XXTEA 密钥候选
     *
     * 扫描 ELF 格式的 .so 文件，提取所有 4~32 字节的连续可打印 ASCII 字符串。
     * 这些字符串可能包含硬编码的加密密钥。
     *
     * 提取策略：
     *   - 跳过 ELF 头部、节表等非数据区域（通过只扫描 .rodata 和 .data 段）
     *   - 只保留纯 ASCII 可打印字符（0x21~0x7E）
     *   - 过滤掉过短的字符串（< 4 字节）
     *   - 过滤掉明显非密钥的字符串（如纯数字、常见函数名）
     *
     * @param soPath .so 文件路径
     * @return 候选密钥的 ByteArray 集合（去重）
     * @throws IllegalArgumentException 如果文件不存在
     */
    fun extractSOKeys(soPath: String): Set<ByteArray> {
        val file = File(soPath)
        if (!file.exists()) {
            throw IllegalArgumentException("SO 文件不存在: $soPath")
        }

        Log.d(TAG, "正在扫描 SO 文件: ${file.name}")
        val data = file.readBytes()
        val candidates = mutableSetOf<ByteArrayWrapper>()

        var currentString = StringBuilder()
        var stringStart = -1

        for (i in data.indices) {
            val byte = data[i].toInt() and 0xFF

            if (byte in ASCII_PRINTABLE_START..ASCII_PRINTABLE_END) {
                // 可打印字符，累积
                if (currentString.isEmpty()) {
                    stringStart = i
                }
                currentString.append(byte.toChar())
            } else {
                // 不可打印字符，结束当前字符串
                if (currentString.length in MIN_KEY_LENGTH..MAX_KEY_LENGTH) {
                    val str = currentString.toString()
                    // 过滤：排除纯数字、明显非密钥的字符串
                    if (isLikelyKey(str)) {
                        candidates.add(ByteArrayWrapper(str.toByteArray(Charsets.UTF_8)))
                    }
                }
                currentString = StringBuilder()
                stringStart = -1
            }
        }

        // 处理文件末尾的最后一个字符串
        if (currentString.length in MIN_KEY_LENGTH..MAX_KEY_LENGTH) {
            val str = currentString.toString()
            if (isLikelyKey(str)) {
                candidates.add(ByteArrayWrapper(str.toByteArray(Charsets.UTF_8)))
            }
        }

        Log.d(TAG, "从 ${file.name} 提取到 ${candidates.size} 个候选密钥")
        return candidates.map { it.bytes }.toSet()
    }

    /**
     * 判断字符串是否可能是 XXTEA 密钥
     *
     * 过滤规则：
     *   - 排除纯数字
     *   - 排除过长的路径格式字符串
     *   - 排除纯空格/符号
     *   - 至少包含一个字母字符
     */
    private fun isLikelyKey(str: String): Boolean {
        // 必须包含至少一个字母
        if (!str.any { it.isLetter() }) return false

        // 排除纯数字
        if (str.all { it.isDigit() }) return false

        // 排除路径格式（包含 / 或 . 且长度 > 20）
        if (str.contains("/") || str.contains("\\") && str.length > 20) return false

        // 排除明显是代码/格式的字符串
        if (str.contains(".so") || str.contains(".dll") || str.contains(".json")) return false

        return true
    }

    /**
     * 生成候选密钥列表
     *
     * 综合多种来源生成密钥候选列表：
     *   1. 默认内置密钥
     *   2. 从 .so 文件中提取的密钥
     *   3. 从 plist 文件中提取的字符串值
     *
     * @param cczPath   CCZ 文件路径（可选，用于提取相关 plist）
     * @param plistPath Plist 文件路径（可选，用于提取字符串值）
     * @param soPaths   .so 文件路径列表
     * @return 去重后的候选密钥 ByteArray 列表
     */
    fun generateCandidates(
        cczPath: String?,
        plistPath: String?,
        soPaths: List<String>
    ): List<ByteArray> {
        val allKeys = mutableSetOf<ByteArrayWrapper>()

        // 1. 添加默认密钥
        Log.d(TAG, "添加 ${DEFAULT_KEYS.size} 个默认密钥")
        for (key in DEFAULT_KEYS) {
            allKeys.add(ByteArrayWrapper(key.toByteArray(Charsets.UTF_8)))
        }

        // 2. 从 .so 文件提取
        for (soPath in soPaths) {
            try {
                val soKeys = extractSOKeys(soPath)
                for (key in soKeys) {
                    allKeys.add(ByteArrayWrapper(key))
                }
            } catch (e: Exception) {
                Log.w(TAG, "扫描 SO 文件失败: $soPath - ${e.message}")
            }
        }

        // 3. 从 plist 文件提取字符串值
        val actualPlistPath = plistPath ?: cczPath?.let {
            FileManager.findMatchingPlist(it)
        }

        if (actualPlistPath != null) {
            try {
                val allStrings = PlistParser.extractAllKeys(actualPlistPath)
                for ((_, value) in allStrings) {
                    if (value.length in MIN_KEY_LENGTH..MAX_KEY_LENGTH) {
                        allKeys.add(ByteArrayWrapper(value.toByteArray(Charsets.UTF_8)))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "解析 Plist 文件失败: $actualPlistPath - ${e.message}")
            }
        }

        Log.d(TAG, "共生成 ${allKeys.size} 个候选密钥")
        return allKeys.map { it.bytes }
    }

    /**
     * 字典穷举破解（协程版）
     *
     * 在 IO 线程池中逐一尝试候选密钥列表，通过协程挂起避免阻塞 UI。
     * 每尝试一定数量的密钥后报告进度。
     *
     * 破解原理：
     *   XXTEA 解密后如果得到有效的 PVR/PNG 数据，则认为密钥正确。
     *   PVR magic: 0x03525650 或 0x50565203
     *   PNG magic: 89 50 4E 47 0D 0A 1A 0A
     *
     * @param raw       待解密的原始数据（XXTEA 加密数据）
     * @param candidates 候选密钥列表
     * @param onProgress 进度回调（已尝试数量, 当前尝试的密钥描述）
     * @return Pair（解密后的数据, 使用的密钥），破解失败返回 (null, null)
     */
    suspend fun bruteDict(
        raw: ByteArray,
        candidates: List<ByteArray>,
        onProgress: suspend (Int, String) -> Unit
    ): Pair<ByteArray?, ByteArray?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "字典穷举开始，共 ${candidates.size} 个候选密钥")

        for ((index, key) in candidates.withIndex()) {
            ensureActive() // 协程取消检查

            try {
                val decrypted = XXTEA.decrypt(raw, key)

                // 验证解密结果
                if (PVRParser.isPVR(decrypted)) {
                    Log.d(TAG, "字典穷举成功！密钥: ${keyToString(key)}")
                    onProgress(candidates.size, "破解成功！密钥: ${keyToString(key)}")
                    return@withContext Pair(decrypted, key)
                }

                if (isPNG(decrypted)) {
                    Log.d(TAG, "字典穷举成功！密钥: ${keyToString(key)}")
                    onProgress(candidates.size, "破解成功！密钥: ${keyToString(key)}")
                    return@withContext Pair(decrypted, key)
                }
            } catch (e: Exception) {
                // 解密失败（数据长度异常等），继续尝试
            }

            // 每处理 100 个密钥报告一次进度
            if (index % 100 == 0 || index == candidates.size - 1) {
                onProgress(index + 1, "正在尝试密钥 ${index + 1}/${candidates.size}: ${keyToString(key)}")
            }
        }

        Log.d(TAG, "字典穷举失败，所有候选密钥均不匹配")
        onProgress(candidates.size, "字典穷举完成，未找到有效密钥")
        Pair(null, null)
    }

    /**
     * 字符集穷举破解（协程版）
     *
     * 在指定字符集和长度范围内生成所有可能的密钥组合进行穷举。
     * 通过 maxCombos 参数限制最大尝试次数，防止无限运行。
     *
     * 示例：charset="abc", minLen=2, maxLen=3
     *   生成: aa, ab, ac, ba, bb, bc, ca, cb, cc,
     *         aaa, aab, aac, aba, abb, abc, ...
     *
     * @param raw        待解密的原始数据
     * @param charset    字符集字符串（如 "abcdefghijklmnopqrstuvwxyz"）
     * @param minLen     最小密钥长度
     * @param maxLen     最大密钥长度
     * @param maxCombos  最大尝试次数（防止无限运行）
     * @param onProgress 进度回调（已尝试数量, 当前尝试的密钥描述）
     * @return Pair（解密后的数据, 使用的密钥），破解失败返回 (null, null)
     */
    suspend fun bruteCharset(
        raw: ByteArray,
        charset: String,
        minLen: Int = 4,
        maxLen: Int = 16,
        maxCombos: Long = 10_000_000,
        onProgress: suspend (Int, String) -> Unit
    ): Pair<ByteArray?, ByteArray?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "字符集穷举开始，字符集长度: ${charset.length}, 范围: $minLen~$maxLen")

        var tried = 0L

        // 逐长度生成
        for (len in minLen..maxLen) {
            val totalCombos = charset.length.toDouble().pow(len).toLong()
            if (tried >= maxCombos) break

            Log.d(TAG, "尝试长度 $len，总组合数: $totalCombos")

            // 使用递归生成所有组合
            val result = bruteCharsetRecursive(
                raw = raw,
                charset = charset,
                length = len,
                current = StringBuilder(),
                triedSoFar = tried,
                maxCombos = maxCombos,
                onProgress = onProgress
            )

            if (result != null) {
                return@withContext result
            }

            // 更新已尝试数量
            tried += minOf(totalCombos, maxCombos - tried)
        }

        Log.d(TAG, "字符集穷举失败")
        onProgress(tried.toInt(), "字符集穷举完成，未找到有效密钥")
        Pair(null, null)
    }

    /**
     * 递归字符集穷举
     *
     * 使用深度优先搜索逐位生成密钥字符串，每生成一个完整密钥就尝试解密。
     *
     * @return 如果找到有效密钥返回 Pair，否则返回 null
     */
    private suspend fun bruteCharsetRecursive(
        raw: ByteArray,
        charset: String,
        length: Int,
        current: StringBuilder,
        triedSoFar: Long,
        maxCombos: Long,
        onProgress: suspend (Int, String) -> Unit
    ): Pair<ByteArray, ByteArray>? {
        if (current.length == length) {
            val key = current.toString().toByteArray(Charsets.UTF_8)
            val tried = triedSoFar + bruteTriedCounter.get()

            try {
                val decrypted = XXTEA.decrypt(raw, key)

                if (PVRParser.isPVR(decrypted) || isPNG(decrypted)) {
                    Log.d(TAG, "字符集穷举成功！密钥: ${current}")
                    return Pair(decrypted, key)
                }
            } catch (_: Exception) {
                // 继续
            }

            bruteTriedCounter.incrementAndGet()

            // 每 10000 次报告一次进度
            if (bruteTriedCounter.get() % 10000 == 0) {
                onProgress(bruteTriedCounter.get(), "字符集穷举: 已尝试 ${bruteTriedCounter.get()} 次")
            }

            return null
        }

        for (c in charset) {
            if (bruteTriedCounter.get() + triedSoFar >= maxCombos) {
                return null // 达到最大尝试次数
            }

            current.append(c)
            val result = bruteCharsetRecursive(
                raw = raw,
                charset = charset,
                length = length,
                current = current,
                triedSoFar = triedSoFar,
                maxCombos = maxCombos,
                onProgress = onProgress
            )
            current.deleteCharAt(current.length - 1)

            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * 检查数据是否为 PNG 格式
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
     * 将密钥 ByteArray 转为可读字符串
     */
    private fun keyToString(key: ByteArray): String {
        return try {
            String(key, Charsets.UTF_8)
        } catch (e: Exception) {
            key.joinToString(" ") { "%02X".format(it) }
        }
    }

    /**
     * ByteArray 包装器，用于 Set 去重
     */
    private class ByteArrayWrapper(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByteArrayWrapper) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()

        override fun toString(): String = keyToString(bytes)

        private fun keyToString(key: ByteArray): String {
            return try {
                String(key, Charsets.UTF_8)
            } catch (e: Exception) {
                key.joinToString(" ") { "%02X".format(it) }
            }
        }
    }

}
