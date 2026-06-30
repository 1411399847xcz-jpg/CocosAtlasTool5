package com.cocos.atlastool.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.floor

/**
 * XXTEA 解密算法 — Kotlin 实现
 *
 * 忠实翻译自 Python 版本的 _mx 和 decrypt 循环逻辑。
 * 使用 Little-Endian 字节序，通过 ByteBuffer / IntBuffer 高效操作 32 位整数数组。
 *
 * XXTEA (Corrected Block TEA) 是一种可变长分组的 Feistel 密码，
 * 常用于 Cocos2d-x 资源加密。解密时密钥固定为 4 个 uint32，
 * 数据按 uint32 数组处理，循环中的 MX 函数使用移位和加法混合。
 */
object XXTEA {

    /** XXTEA 核心参数：DELTA 值，来源于黄金分割比例 */
    private const val DELTA: UInt = 0x9E3779B9u

    /** 最少轮数 */
    private const val MIN_ROUNDS: UInt = 6u

    /**
     * XXTEA 解密方法
     *
     * @param data   待解密的密文字节数组（长度必须是 4 的倍数）
     * @param key    解密密钥字节数组（长度不限，内部会取前 16 字节或补零到 16 字节）
     * @return       解密后的明文字节数组
     * @throws IllegalArgumentException 如果输入数据长度不是 4 的倍数
     */
    fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        // ---- 前置校验 ----
        require(data.isNotEmpty()) { "待解密数据不能为空" }

        val paddedData = if (data.size % 4 != 0) {
            // 补齐到 4 字节对齐
            data.copyOf(data.size + (4 - data.size % 4))
        } else {
            data
        }

        // ---- 将字节数组转为 Little-Endian Int 数组 ----
        val v = byteToIntArrayLE(paddedData)
        val n = v.size

        require(n >= 2) { "XXTEA 解密数据至少需要 2 个 uint32（8 字节）" }

        // ---- 准备密钥 Int 数组（固定 4 个 uint32，不足补零） ----
        val k = byteToIntArrayLE(key.padKeyTo16())

        // ---- 计算 XXTEA 参数 ----
        // rounds 为 (floor(n / 2) + 5) * 2，但不低于 MIN_ROUNDS
        val rounds = ((floor(n.toDouble() / 2.0) + 5) * 2).toUInt().coerceAtLeast(MIN_ROUNDS)

        // ---- 解密主循环 ----
        var q = rounds
        var sum = (q * DELTA) and 0xFFFFFFFFu  // 等价于 Python 中的 (q * DELTA) & 0xffffffff
        val pEnd = n - 1

        while (q != 0u) {
            val e = (sum shr 2).toInt() and 3
            var p = pEnd
            while (p > 0) {
                val z = v[p - 1]
                // MX 函数：核心混合运算
                val mx = mx(sum, v[p], z, k, p, e)
                v[p] = (v[p] - mx) and 0xFFFFFFFFu
                p--
            }
            val z = v[pEnd]
            val mx = mx(sum, v[0], z, k, 0, e)
            v[0] = (v[0] - mx) and 0xFFFFFFFFu
            sum = (sum - DELTA) and 0xFFFFFFFFu
            q--
        }

        // ---- 将 Int 数组转回字节数组 ----
        return intArrayToByteArrayLE(v, paddedData.size)
    }

    /**
     * XXTEA 的 MX 函数
     *
     * 对应 Python 版本中的：
     *   (((z >> 5 ^ y << 2) + (y >> 3 ^ z << 4)) ^ ((sum ^ y) + (k[(p & 3) ^ e] ^ z)))
     *
     * @param sum   当前累计值
     * @param y     当前位置的值（v[p]）
     * @param z     前一个位置的值（v[p-1] 或 v[n-1]）
     * @param k     密钥 Int 数组
     * @param p     当前位置索引
     * @param e     (sum >> 2) & 3 的结果
     * @return      MX 计算结果
     */
    private fun mx(sum: UInt, y: UInt, z: UInt, k: UIntArray, p: Int, e: Int): UInt {
        val part1 = (z shr 5) xor (y shl 2)
        val part2 = (y shr 3) xor (z shl 4)
        val part3 = sum xor y
        val part4 = k[(p and 3) xor e] xor z
        return ((part1 + part2) xor (part3 + part4)) and 0xFFFFFFFFu
    }

    /**
     * 将密钥字节数组补齐或截断到 16 字节（4 个 uint32）
     */
    private fun ByteArray.padKeyTo16(): ByteArray {
        return if (this.size >= 16) {
            this.copyOf(16)
        } else {
            ByteArray(16) { i -> if (i < this.size) this[i] else 0 }
        }
    }

    /**
     * 将字节数组按 Little-Endian 转为 UIntArray
     *
     * 每 4 字节组合为一个 32 位无符号整数，低字节在前。
     */
    private fun byteToIntArrayLE(data: ByteArray): UIntArray {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val intBuf = buf.asIntBuffer()
        val result = UIntArray(intBuf.remaining())
        for (i in result.indices) {
            result[i] = intBuf.get().toUInt() and 0xFFFFFFFFu
        }
        return result
    }

    /**
     * 将 UIntArray 按 Little-Endian 转回字节数组
     *
     * @param intArray  UInt 数组
     * @param byteCount 输出字节数量（用于去除尾部填充）
     */
    private fun intArrayToByteArrayLE(intArray: UIntArray, byteCount: Int): ByteArray {
        val buf = ByteBuffer.allocate(intArray.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        val intBuf = buf.asIntBuffer()
        for (v in intArray) {
            intBuf.put(v.toInt())
        }
        return buf.array().copyOf(byteCount)
    }
}
