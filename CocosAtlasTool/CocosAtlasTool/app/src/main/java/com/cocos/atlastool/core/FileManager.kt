package com.cocos.atlastool.core

import java.io.File

/**
 * 文件管理工具
 *
 * 提供文件操作相关的实用方法，包括：
 *   - 安全文件名处理（去除非法字符）
 *   - 文件备份轮转（保留多个历史版本）
 *   - 纹理/ plist 自动匹配
 *   - 按扩展名递归列出文件
 *
 * 所有方法均为静态工具方法，可在任意上下文中直接调用。
 */
object FileManager {

    /** 文件名中不允许出现的字符（跨平台安全字符集） */
    private const val INVALID_CHARS_REGEX = """[<>:"/\\|?*\x00-\x1F]"""

    /** 备份文件名后缀格式 */
    private const val BACKUP_SUFFIX_FORMAT = ".bak.%d"

    /**
     * 生成安全的文件名
     *
     * 将输入字符串中的非法文件名字符替换为下划线，
     * 确保生成的文件名在所有主流操作系统上均合法。
     *
     * 处理规则：
     *   - 移除控制字符（0x00~0x1F）
     *   - 替换 <>:"/\\|?* 为下划线
     *   - 去除首尾空白
     *   - 处理连续多个下划线
     *   - 限制文件名长度（255 字符）
     *   - 确保文件名不为空（返回 "unnamed"）
     *
     * @param name 原始文件名
     * @return 安全的文件名字符串
     */
    fun safeFileName(name: String): String {
        if (name.isBlank()) return "unnamed"

        return name
            // 替换非法字符为下划线
            .replace(Regex(INVALID_CHARS_REGEX), "_")
            // 去除首尾空白和点号（避免以点开头的隐藏文件）
            .trim(' ', '.', '_')
            // 压缩连续的下划线
            .replace(Regex("_{2,}"), "_")
            // 限制长度（保留扩展名）
            .let { fullName ->
                if (fullName.length > 255) {
                    val dotIndex = fullName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        val ext = fullName.substring(dotIndex)
                        val base = fullName.substring(0, dotIndex)
                        val maxBaseLen = 255 - ext.length - 1
                        base.substring(0, maxBaseLen) + "." + ext.substring(1)
                    } else {
                        fullName.substring(0, 255)
                    }
                } else {
                    fullName
                }
            }
            .ifBlank { "unnamed" }
    }

    /**
     * 文件备份轮转
     *
     * 在覆盖文件前创建备份，保留最近 N 个历史版本。
     * 备份文件命名格式：原文件名.bak.1, 原文件名.bak.2, ...
     * 数字越大表示越旧的备份。
     *
     * 轮转逻辑：
     *   1. 如果已有 maxBackups 个备份，删除最旧的（.bak.maxBackups）
     *   2. 所有现有备份编号 +1（.bak.1 → .bak.2, .bak.2 → .bak.3, ...）
     *   3. 将当前文件复制为 .bak.1
     *
     * 示例（maxBackups = 3）：
     *   原始: data.plist
     *   备份: data.plist.bak.1 (最新) → data.plist.bak.2 → data.plist.bak.3 (最旧)
     *
     * @param filepath    要备份的文件路径
     * @param maxBackups  最大备份数量（不包含原始文件）
     */
    fun rotateBackups(filepath: String, maxBackups: Int = 5) {
        val file = File(filepath)
        if (!file.exists()) return

        val parentDir = file.parentFile ?: return
        val baseName = file.name

        // ---- 删除超出数量限制的最旧备份 ----
        val oldestBackup = File(parentDir, "$baseName${String.format(BACKUP_SUFFIX_FORMAT, maxBackups)}")
        if (oldestBackup.exists()) {
            oldestBackup.delete()
        }

        // ---- 轮转现有备份（编号递增） ----
        for (i in (maxBackups - 1) downTo 1) {
            val oldBackup = File(parentDir, "$baseName${String.format(BACKUP_SUFFIX_FORMAT, i)}")
            val newBackup = File(parentDir, "$baseName${String.format(BACKUP_SUFFIX_FORMAT, i + 1)}")
            if (oldBackup.exists()) {
                oldBackup.renameTo(newBackup)
            }
        }

        // ---- 创建新备份（编号 1） ----
        val newBackup = File(parentDir, "$baseName${String.format(BACKUP_SUFFIX_FORMAT, 1)}")
        file.copyTo(newBackup, overwrite = true)
    }

    /**
     * 自动匹配纹理文件
     *
     * 根据 plist 文件路径，在相同目录下查找对应的纹理文件。
     * 匹配规则：
     *   1. 同名但不同扩展名：hero.plist → hero.png, hero.pvr, hero.ccz
     *   2. 去除后缀匹配：hero-uhd.plist → hero.png, hero-uhd.png
     *   3. 添加后缀匹配：hero.plist → hero-hd.plist 对应 hero-hd.png
     *
     * 支持的纹理扩展名：.png, .pvr, .ccz, .webp, .jpg, .jpeg
     *
     * @param plistPath plist 文件路径
     * @return 匹配到的纹理文件路径，未找到返回 null
     */
    fun findMatchingTexture(plistPath: String): String? {
        val plistFile = File(plistPath)
        if (!plistFile.exists()) return null

        val parentDir = plistFile.parentFile ?: return null
        val baseName = plistFile.nameWithoutExtension

        // 支持的纹理扩展名（按优先级排序）
        val textureExts = listOf("png", "pvr", "ccz", "webp", "jpg", "jpeg")

        // ---- 策略 1：完全同名匹配（不同扩展名） ----
        for (ext in textureExts) {
            val candidate = File(parentDir, "$baseName.$ext")
            if (candidate.exists()) return candidate.absolutePath
        }

        // ---- 策略 2：去除常见分辨率后缀匹配 ----
        // hero-uhd.plist → hero.png, hero-hd.plist → hero.png
        val suffixesToRemove = listOf("-uhd", "-hd", "-sd", "-md", "-ld", "_uhd", "_hd", "_sd", "_md", "_ld")
        for (suffix in suffixesToRemove) {
            if (baseName.endsWith(suffix, ignoreCase = true)) {
                val trimmedBase = baseName.removeSuffix(suffix)
                for (ext in textureExts) {
                    val candidate = File(parentDir, "$trimmedBase.$ext")
                    if (candidate.exists()) return candidate.absolutePath
                }
            }
        }

        // ---- 策略 3：遍历同目录下的纹理文件模糊匹配 ----
        val filesInDir = parentDir.listFiles() ?: return null
        for (file in filesInDir) {
            if (!file.isFile) continue
            val ext = file.extension.lowercase()
            if (ext !in textureExts) continue

            val texBaseName = file.nameWithoutExtension.lowercase()
            // 检查文件名是否包含 plist 的基础名
            if (texBaseName.contains(baseName.lowercase()) ||
                baseName.lowercase().contains(texBaseName)
            ) {
                return file.absolutePath
            }
        }

        return null
    }

    /**
     * 自动匹配 plist 文件
     *
     * 根据纹理文件路径，在相同目录下查找对应的 plist 文件。
     * 匹配规则与 findMatchingTexture 对称。
     *
     * @param texturePath 纹理文件路径
     * @return 匹配到的 plist 文件路径，未找到返回 null
     */
    fun findMatchingPlist(texturePath: String): String? {
        val textureFile = File(texturePath)
        if (!textureFile.exists()) return null

        val parentDir = textureFile.parentFile ?: return null
        val baseName = textureFile.nameWithoutExtension

        // ---- 策略 1：完全同名匹配 ----
        val candidate = File(parentDir, "$baseName.plist")
        if (candidate.exists()) return candidate.absolutePath

        // ---- 策略 2：去除分辨率后缀匹配 ----
        val suffixesToRemove = listOf("-uhd", "-hd", "-sd", "-md", "-ld", "_uhd", "_hd", "_sd", "_md", "_ld")
        for (suffix in suffixesToRemove) {
            if (baseName.endsWith(suffix, ignoreCase = true)) {
                val trimmedBase = baseName.removeSuffix(suffix)
                val plistCandidate = File(parentDir, "$trimmedBase.plist")
                if (plistCandidate.exists()) return plistCandidate.absolutePath
            }
        }

        // ---- 策略 3：遍历目录模糊匹配 ----
        val filesInDir = parentDir.listFiles() ?: return null
        for (file in filesInDir) {
            if (!file.isFile || file.extension.lowercase() != "plist") continue

            val plistBaseName = file.nameWithoutExtension.lowercase()
            if (plistBaseName.contains(baseName.lowercase()) ||
                baseName.lowercase().contains(plistBaseName)
            ) {
                return file.absolutePath
            }
        }

        return null
    }

    /**
     * 按扩展名列出文件
     *
     * 列出指定目录下具有特定扩展名的文件。
     * 支持递归搜索子目录。
     *
     * @param dir        要搜索的目录路径
     * @param extensions 要匹配的文件扩展名列表（不含点号，如 "png", "plist"）
     * @param recursive  是否递归搜索子目录
     * @return 匹配的文件列表，按路径排序
     * @throws IllegalArgumentException 如果目录不存在
     */
    fun listFilesWithExt(
        dir: String,
        extensions: List<String>,
        recursive: Boolean = false
    ): List<File> {
        val directory = File(dir)
        if (!directory.exists()) {
            throw IllegalArgumentException("目录不存在: $dir")
        }
        if (!directory.isDirectory) {
            throw IllegalArgumentException("路径不是目录: $dir")
        }

        // 统一转为小写便于比较
        val extSet = extensions.map { it.lowercase().removePrefix(".") }.toSet()

        val result = mutableListOf<File>()

        if (recursive) {
            // 递归遍历
            directory.walk()
                .filter { it.isFile }
                .filter { it.extension.lowercase() in extSet }
                .sortedBy { it.absolutePath }
                .forEach { result.add(it) }
        } else {
            // 仅当前目录
            directory.listFiles()
                ?.filter { it.isFile }
                ?.filter { it.extension.lowercase() in extSet }
                ?.sortedBy { it.absolutePath }
                ?.forEach { result.add(it) }
        }

        return result
    }

    /**
     * 获取文件扩展名（小写，不含点号）
     *
     * @param filename 文件名或文件路径
     * @return 小写扩展名，无扩展名返回空字符串
     */
    fun getExtension(filename: String): String {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param filename 文件名或文件路径
     * @return 不含扩展名的文件名部分
     */
    fun getBaseName(filename: String): String {
        val file = File(filename)
        return file.nameWithoutExtension
    }
}
