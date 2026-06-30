package com.cocos.atlastool.core

/**
 * XML 换行填充混淆器
 *
 * 通过在 XML 内容中插入大量换行符将文件膨胀到目标大小，
 * 实现简单的文件体积混淆。
 *
 * 应用场景：
 *   - 某些平台对上传文件有最小体积要求
 *   - 增加文件解析难度（简单的抗逆向手段）
 *   - 填充文件以绕过体积检测
 *
 * 原理：
 *   混淆：在 XML 声明后插入换行符，将文件填充到 TARGET_SIZE。
 *   反混淆：移除所有连续的空白行，恢复原始 XML 内容。
 *
 * 注意：这是一种非常基础的混淆手段，仅用于特定的填充需求。
 * 对于安全性要求高的场景，应使用加密而非混淆。
 */
object XMLObfuscator {

    /**
     * 目标文件大小（8 MB）
     *
     * 混淆后的 XML 文件将被填充到该大小。
     * 8MB 是许多平台的常见文件大小阈值。
     */
    const val TARGET_SIZE = 8 * 1024 * 1024

    /** 换行填充字符 */
    private const val FILL_CHAR = '\n'

    /**
     * 混淆 XML 内容
     *
     * 在 XML 文本中插入换行符，将文件膨胀到 TARGET_SIZE。
     *
     * 策略：
     *   1. 检查原始内容大小
     *   2. 如果已超过目标大小，直接返回原内容
     *   3. 计算需要填充的字节数
     *   4. 在 XML 声明（<?xml ...?>）之后插入换行符
     *   5. 如果没有 XML 声明，在文件开头插入
     *
     * @param content 原始 XML 文本内容
     * @return 混淆后的 XML 文本内容（填充到 TARGET_SIZE）
     */
    fun obfuscate(content: String): String {
        // ---- 如果已超过目标大小，无需混淆 ----
        if (content.length >= TARGET_SIZE) {
            return content
        }

        // ---- 计算需要填充的字节数 ----
        val fillSize = TARGET_SIZE - content.length

        // ---- 构建 XML 声明 ----
        val xmlDeclarationRegex = Regex("""<\?xml[^?]*\?>""")

        return if (xmlDeclarationRegex.containsMatchIn(content)) {
            // 在 XML 声明之后插入换行
            val match = xmlDeclarationRegex.find(content)!!
            val insertPos = match.range.last + 1
            val prefix = content.substring(0, insertPos)
            val suffix = content.substring(insertPos)

            // 构建填充字符串
            val fillString = FILL_CHAR.toString().repeat(fillSize)

            StringBuilder(prefix.length + fillString.length + suffix.length)
                .append(prefix)
                .append(fillString)
                .append(suffix)
                .toString()
        } else {
            // 没有 XML 声明，在开头插入换行
            val fillString = FILL_CHAR.toString().repeat(fillSize)
            StringBuilder(fillString.length + content.length)
                .append(fillString)
                .append(content)
                .toString()
        }
    }

    /**
     * 反混淆 XML 内容
     *
     * 移除 XML 中连续的换行/空白行，恢复原始 XML 内容。
     *
     * 策略：
     *   1. 检测并移除 XML 声明后的大量连续空白行
     *   2. 如果文件开头就是空白行，先移除开头的空白
     *   3. 压缩连续的空白行为单行
     *
     * @param content 混淆后的 XML 文本内容
     * @return 反混淆后的 XML 文本内容
     */
    fun deobfuscate(content: String): String {
        // ---- 快速检测：如果文件大小不超过 TARGET_SIZE 的 1.5 倍，可能未被混淆 ----
        if (content.length < TARGET_SIZE) {
            return content
        }

        // ---- 策略 1：移除 XML 声明后的连续空白行块 ----
        val xmlDeclarationRegex = Regex("""<\?xml[^?]*\?>""")

        val result = if (xmlDeclarationRegex.containsMatchIn(content)) {
            val match = xmlDeclarationRegex.find(content)!!
            val insertPos = match.range.last + 1
            val prefix = content.substring(0, insertPos)
            val suffix = content.substring(insertPos)

            // 移除 suffix 开头的连续换行
            val trimmedSuffix = suffix.trimStart(FILL_CHAR, '\r', ' ', '\t')

            StringBuilder(prefix.length + trimmedSuffix.length)
                .append(prefix)
                .append("\n")  // 保留一个换行
                .append(trimmedSuffix)
                .toString()
        } else {
            // ---- 策略 2：移除开头的连续空白行 ----
            content.trimStart(FILL_CHAR, '\r', ' ', '\t')
        }

        // ---- 额外清理：压缩多个连续空行为单个换行 ----
        return result.replace(Regex("""\n{3,}"""), "\n\n")
    }

    /**
     * 自定义大小混淆
     *
     * 与 obfuscate 类似，但允许指定目标大小。
     *
     * @param content    原始 XML 文本内容
     * @param targetSize 目标文件大小（字节）
     * @return 混淆后的 XML 文本内容
     */
    fun obfuscate(content: String, targetSize: Int): String {
        if (content.length >= targetSize) {
            return content
        }

        val fillSize = targetSize - content.length
        val fillString = FILL_CHAR.toString().repeat(fillSize)

        val xmlDeclarationRegex = Regex("""<\?xml[^?]*\?>""")

        return if (xmlDeclarationRegex.containsMatchIn(content)) {
            val match = xmlDeclarationRegex.find(content)!!
            val insertPos = match.range.last + 1
            val prefix = content.substring(0, insertPos)
            val suffix = content.substring(insertPos)

            StringBuilder(prefix.length + fillString.length + suffix.length)
                .append(prefix)
                .append(fillString)
                .append(suffix)
                .toString()
        } else {
            StringBuilder(fillString.length + content.length)
                .append(fillString)
                .append(content)
                .toString()
        }
    }
}
