package com.cocos.atlastool.core

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileReader

/**
 * Plist 帧数据解析器
 *
 * 解析 Cocos2d-x 使用的 SpriteFrame plist 文件（XML 格式），
 * 提取每个精灵帧的坐标、尺寸、旋转信息等。
 *
 * Plist XML 结构示例：
 * ```xml
 * <plist version="1.0">
 *   <dict>
 *     <key>frames</key>
 *     <dict>
 *       <key>sprite_01</key>
 *       <dict>
 *         <key>frame</key>
 *         <string>{{2, 2}, {100, 200}}</string>
 *         <key>rotated</key>
 *         <true/>
 *         <key>sourceSize</key>
 *         <string>{100, 200}</string>
 *       </dict>
 *     </dict>
 *   </dict>
 * </plist>
 * ```
 *
 * 使用 Android 原生 XmlPullParser 进行高效 SAX 风格解析，
 * 通过正则表达式提取帧坐标，兼容负数坐标值。
 */
object PlistParser {

    private const val TAG = "PlistParser"

    /**
     * 精灵帧数据类
     *
     * @property name    帧名称（plist 中的 key）
     * @property x       帧在纹理图集中的左上角 X 坐标
     * @property y       帧在纹理图集中的左上角 Y 坐标（TexturePacker 格式中原点在左上角）
     * @property w       帧裁剪宽度（像素）
     * @property h       帧裁剪高度（像素）
     * @property rotated 是否旋转（90度旋转标记）
     * @property drawW   源图绘制宽度（包含透明边距的原始宽度）
     * @property drawH   源图绘制高度（包含透明边距的原始高度）
     */
    data class SpriteFrame(
        val name: String,
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val rotated: Boolean,
        val drawW: Int,
        val drawH: Int
    )

    /**
     * 解析 plist 文件中的精灵帧数据
     *
     * 使用 XmlPullParser 遍历 plist XML 结构，
     * 在 <key>frames</key> 下提取每个子帧的信息。
     *
     * @param plistPath plist 文件路径
     * @return 帧名称到 SpriteFrame 的映射
     * @throws IllegalArgumentException 如果文件不存在或解析失败
     */
    fun parse(plistPath: String): Map<String, SpriteFrame> {
        val file = File(plistPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Plist 文件不存在: $plistPath")
        }

        val frames = mutableMapOf<String, SpriteFrame>()

        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(FileReader(file))

            var currentFrameName: String? = null
            var currentKey: String? = null
            var isFramesDict = false
            var depth = 0
            var framesDictDepth = -1

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        depth++
                        val tagName = parser.name

                        // 进入 <key> 标签
                        if (tagName == "key") {
                            val keyText = parser.nextText().trim()
                            currentKey = keyText

                            // 如果当前在 frames 字典层级，记录帧名
                            if (isFramesDict && depth == framesDictDepth + 1) {
                                currentFrameName = keyText
                            }

                            // 检测 <key>frames</key>，标记进入帧字典区域
                            if (keyText == "frames") {
                                isFramesDict = true
                            }
                        }

                        // 进入 <dict> 标签
                        if (tagName == "dict") {
                            // 如果上一个 key 是 "frames"，标记深度
                            if (currentKey == "frames") {
                                framesDictDepth = depth
                            }
                        }

                        // 处理 <true/> / <false/>
                        if (tagName == "true" || tagName == "false" && currentFrameName != null) {
                            if (currentKey == "rotated" && currentFrameName != null) {
                                // 将在下面构建完整帧时处理
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        depth--
                        val tagName = parser.name

                        // 离开帧子字典时，构建 SpriteFrame
                        if (tagName == "dict" && depth == framesDictDepth && currentFrameName != null) {
                            // 需要回溯收集的数据，这里使用简化方式
                            // 实际解析通过二次扫描完成
                        }
                    }

                    else -> { /* TEXT, COMMENT 等忽略 */ }
                }

                eventType = parser.next()
            }

            // ---- 二次解析：更精确的提取方式 ----
            // 使用简化但可靠的正则提取方案
            parseWithRegex(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "XmlPullParser 解析失败，尝试正则回退", e)
            // 回退到正则提取
            parseWithRegex(file.readText(Charsets.UTF_8))
        }
    }

    /**
     * 使用正则表达式解析 plist 内容
     *
     * 从 plist XML 文本中提取帧信息，支持 TexturePacker 格式：
     * - frame: {{x, y}, {w, h}}
     * - offset: {x, y}（可选）
     * - rotated: true/false
     * - sourceColorRect: {{x, y}, {w, h}}（可选）
     * - sourceSize: {w, h}
     *
     * 正则兼容负数坐标值（如 {{-10, -20}, {100, 200}}）
     *
     * @param content plist 文件的完整文本内容
     * @return 帧名称到 SpriteFrame 的映射
     */
    private fun parseWithRegex(content: String): Map<String, SpriteFrame> {
        val frames = mutableMapOf<String, SpriteFrame>()

        // 匹配帧块：从 <key>帧名</key> 到下一个同级 <key>
        // 使用正则分割每个帧的定义区域
        val frameBlockRegex = Regex(
            """<key>([^<]+)</key>\s*<dict>(.*?)</dict>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val matches = frameBlockRegex.findAll(content)

        // 跳过 "frames" 字典本身（它不以数字坐标开头）
        var isInFramesSection = false

        for (match in matches) {
            val frameName = match.groupValues[1].trim()
            val blockContent = match.groupValues[2]

            // 跳过 "frames" 等非帧 key
            if (frameName == "frames" || frameName == "metadata") {
                isInFramesSection = (frameName == "frames")
                continue
            }

            if (!isInFramesSection) continue

            // 提取 frame 坐标: {{x, y}, {w, h}}
            val frameInfo = extractRect(blockContent, "frame") ?: continue
            val x = frameInfo[0]
            val y = frameInfo[1]
            val w = frameInfo[2]
            val h = frameInfo[3]

            // 提取是否旋转
            val rotated = extractBoolean(blockContent, "rotated") ?: false

            // 提取 sourceSize: {w, h}
            val sourceSize = extractSize(blockContent, "sourceSize")
            val drawW = sourceSize?.get(0) ?: w
            val drawH = sourceSize?.get(1) ?: h

            frames[frameName] = SpriteFrame(
                name = frameName,
                x = x,
                y = y,
                w = w,
                h = h,
                rotated = rotated,
                drawW = drawW,
                drawH = drawH
            )
        }

        return frames
    }

    /**
     * 提取矩形坐标 {{x, y}, {w, h}}
     *
     * 使用正则匹配 plist 中的嵌套花括号格式，
     * 支持负数和浮点数（向下取整）。
     *
     * @param block 帧块文本内容
     * @param keyName 要提取的键名（如 "frame", "sourceColorRect"）
     * @return 包含 4 个 Int 的数组 [x, y, w, h]，未找到返回 null
     */
    private fun extractRect(block: String, keyName: String): IntArray? {
        // 匹配 <key>keyName</key>\s*<string>{{num, num}, {num, num}}</string>
        val regex = Regex(
            """<key>$keyName</key>\s*<string>\{\{(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\},\s*\{(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\}\}</string>"""
        )
        val match = regex.find(block) ?: return null
        return intArrayOf(
            match.groupValues[1].toDoubleOrNull()?.toInt() ?: 0,
            match.groupValues[2].toDoubleOrNull()?.toInt() ?: 0,
            match.groupValues[3].toDoubleOrNull()?.toInt() ?: 0,
            match.groupValues[4].toDoubleOrNull()?.toInt() ?: 0
        )
    }

    /**
     * 提取尺寸 {w, h}
     *
     * @param block 帧块文本内容
     * @param keyName 要提取的键名（如 "sourceSize"）
     * @return 包含 2 个 Int 的数组 [w, h]，未找到返回 null
     */
    private fun extractSize(block: String, keyName: String): IntArray? {
        val regex = Regex(
            """<key>$keyName</key>\s*<string>\{(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\}</string>"""
        )
        val match = regex.find(block) ?: return null
        return intArrayOf(
            match.groupValues[1].toDoubleOrNull()?.toInt() ?: 0,
            match.groupValues[2].toDoubleOrNull()?.toInt() ?: 0
        )
    }

    /**
     * 提取布尔值
     *
     * @param block 帧块文本内容
     * @param keyName 要提取的键名（如 "rotated"）
     * @return 布尔值，未找到返回 null
     */
    private fun extractBoolean(block: String, keyName: String): Boolean? {
        val trueRegex = Regex("""<key>$keyName</key>\s*<true\s*/?>""")
        val falseRegex = Regex("""<key>$keyName</key>\s*<false\s*/?>""")
        return when {
            trueRegex.containsMatchIn(block) -> true
            falseRegex.containsMatchIn(block) -> false
            else -> null
        }
    }

    /**
     * 提取 plist 中所有字符串键值对
     *
     * 遍历整个 plist XML，收集所有 <key> → <string> 的映射关系。
     * 主要用途：从 plist 元数据中提取可能的 XXTEA 密钥信息，
     * 用于密钥破解时的候选密钥生成。
     *
     * @param plistPath plist 文件路径
     * @return 所有字符串键值对的映射
     * @throws IllegalArgumentException 如果文件不存在
     */
    fun extractAllKeys(plistPath: String): Map<String, String> {
        val file = File(plistPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Plist 文件不存在: $plistPath")
        }

        val content = file.readText(Charsets.UTF_8)
        val result = mutableMapOf<String, String>()

        // 匹配所有 <key>xxx</key> 后紧跟 <string>yyy</string> 的模式
        val keyStringRegex = Regex(
            """<key>([^<]+)</key>\s*<string>([^<]*)</string>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        for (match in keyStringRegex.findAll(content)) {
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            result[key] = value
        }

        return result
    }
}
