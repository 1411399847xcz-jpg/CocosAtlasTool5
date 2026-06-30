package com.cocos.atlastool.core

/**
 * 动画/XML 精简引擎
 *
 * 用于精简 Cocos2d-x 游戏资源中的 XML 配置文件，
 * 减少文件体积和加载时间。
 *
 * 主要功能：
 *   - XML 标签精简：将冗长的标签名替换为更短的别名
 *   - Plist 精简：精简 plist XML 中的冗余字段和元数据
 *
 * 精简策略：
 *   - 标签名映射（如 "CCScaleTo" → "st"）
 *   - 移除注释和多余空白
 *   - 压缩属性格式
 *
 * 注意：精简后的文件可能不兼容原始工具，
 * 主要用于运行时加载优化或自定义引擎。
 */
object SimplifierEngine {

    /**
     * 默认标签映射表
     *
     * 将常见的 Cocos2d-x 动作/节点标签映射为简短别名。
     * 格式：原始标签名 → 简短标签名
     *
     * 这些映射遵循以下原则：
     *   - 保留语义可读性的同时尽量缩短
     *   - 避免与其他标签冲突
     *   - 常用标签使用更短的别名
     */
    val TAG_MAP: Map<String, String> = mapOf(
        // ---- Cocos2d-x 基础动作标签 ----
        "CCMoveTo" to "mt",
        "CCMoveBy" to "mb",
        "CCScaleTo" to "st",
        "CCScaleBy" to "sb",
        "CCRotateTo" to "rt",
        "CCRotateBy" to "rb",
        "CCSkewTo" to "kt",
        "CCSkewBy" to "kb",
        "CCFadeIn" to "fi",
        "CCFadeOut" to "fo",
        "CCFadeTo" to "ft",
        "CCBlink" to "bk",
        "CCTintTo" to "tt",
        "CCTintBy" to "tb",
        "CCJumpTo" to "jt",
        "CCJumpBy" to "jb",
        "CCBezierBy" to "bz",
        "CCCardinalSplineBy" to "cs",
        "CCFollow" to "fw",

        // ---- 组合动作标签 ----
        "CCSequence" to "sq",
        "CCSpawn" to "sp",
        "CCRepeat" to "rp",
        "CCRepeatForever" to "rf",
        "CCSpeed" to "sd",
        "CCTargetedAction" to "ta",

        // ---- 延时 ----
        "CCDelayTime" to "dl",

        // ---- 节点标签 ----
        "CCNode" to "n",
        "CCSprite" to "s",
        "CCLayer" to "l",
        "CCScene" to "sc",
        "CCLabelTTF" to "lt",
        "CCLabelBMFont" to "lb",
        "CCMenu" to "m",
        "CCMenuItem" to "mi",
        "CCMenuItemSprite" to "ms",
        "CCMenuItemLabel" to "ml",
        "CCClippingNode" to "cn",

        // ---- 物理/特效 ----
        "CCParticle" to "p",
        "CCParticleSystem" to "ps",
        "CCArmature" to "ar",
        "CCBone" to "bo",

        // ---- 属性 ----
        "key" to "k",
        "string" to "v",
        "integer" to "i",
        "real" to "r",
        "true" to "t",
        "false" to "f",
        "dict" to "d",
        "array" to "a",

        // ---- 通用 XML 标签 ----
        "plist" to "pl",
        "version" to "ver",
        "frames" to "fr",
        "metadata" to "md",
        "texture" to "tx",
        "format" to "fm",
        "spriteOffset" to "so",
        "spriteSize" to "ss",
        "spriteSourceSize" to "s3",
        "textureRect" to "tr",
        "textureRotated" to "tw",
        "anchor" to "an"
    )

    /**
     * 精简 XML 内容
     *
     * 根据标签映射表将 XML 中的冗长标签名替换为简短别名。
     * 同时移除 XML 注释和多余空白行。
     *
     * @param content   原始 XML 文本内容
     * @param customMap 自定义标签映射（会与默认映射合并，自定义优先）
     * @return 精简后的 XML 文本内容
     */
    fun simplifyXml(content: String, customMap: Map<String, String> = emptyMap()): String {
        // ---- 合并映射表（自定义覆盖默认） ----
        val mergedMap = TAG_MAP + customMap

        var result = content

        // ---- 1. 移除 XML 注释 <!-- ... --> ----
        result = Regex("""<!--.*?-->""", setOf(RegexOption.DOT_MATCHES_ALL)).replace(result, "")

        // ---- 2. 替换开始标签和结束标签 ----
        for ((original, alias) in mergedMap) {
            // 替换开始标签: <original> 或 <original ...>
            // 注意：先替换带属性的标签，再替换不带属性的
            result = result.replace("<$original ", "<$alias ")
            result = result.replace("<$original>", "<$alias>")
            result = result.replace("<$original/>", "<$alias/>")

            // 替换结束标签: </original>
            result = result.replace("</$original>", "</$alias>")
        }

        // ---- 3. 压缩多余空白行 ----
        result = result.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        return result
    }

    /**
     * 精简 Plist 内容
     *
     * 专门针对 Cocos2d-x plist SpriteFrame 格式的精简。
     * 除了标签名替换外，还会：
     *   - 精简帧数据格式（去除不必要的精度）
     *   - 压缩连续的空白字符
     *   - 移除 metadata 中的冗余信息
     *
     * @param content 原始 plist 文本内容
     * @return 精简后的 plist 文本内容
     */
    fun simplifyPlist(content: String): String {
        var result = simplifyXml(content)

        // ---- 1. 精简数字格式：去除浮点数不必要的精度 ----
        // 将 {{2.000000, 3.000000}} 精简为 {{2, 3}}
        result = Regex("""(\d+)\.0+\b""").replace(result) { matchResult ->
            matchResult.groupValues[1]
        }

        // ---- 2. 压缩花括号内的多余空格 ----
        // {{ 2 , 3 }, { 100, 200 }} → {{2,3},{100,200}}
        result = Regex("""\{\s*(-?\d+)\s*,\s*(-?\d+)\s*\}""").replace(result) { matchResult ->
            "{${matchResult.groupValues[1]},${matchResult.groupValues[2]}}"
        }
        // 外层花括号对
        result = Regex("""\{\s*\{""").replace(result, "{{")
        result = Regex("""\}\s*\}""").replace(result, "}}")
        result = Regex("""\}\s*,\s*\{""").replace(result, "},{")

        // ---- 3. 可选：移除 metadata 部分（如果不需要兼容编辑器） ----
        // 取消注释以下代码可以移除 metadata
        // result = Regex(
        //     """<key>metadata</key>\s*<dict>.*?</dict>""",
        //     setOf(RegexOption.DOT_MATCHES_ALL)
        // ).replace(result, "")

        return result
    }

    /**
     * 反向精简（恢复原始标签名）
     *
     * 将精简后的标签名恢复为原始的完整标签名。
     * 用于将精简文件还原为可编辑的原始格式。
     *
     * @param content   精简后的 XML 文本内容
     * @param customMap 自定义标签映射（需与精简时使用的映射一致）
     * @return 恢复后的 XML 文本内容
     */
    fun unsimplifyXml(content: String, customMap: Map<String, String> = emptyMap()): String {
        val mergedMap = TAG_MAP + customMap
        var result = content

        // 反向替换：先替换长别名（避免短别名误匹配），再替换短别名
        val sortedEntries = mergedMap.entries.sortedByDescending { it.value.length }

        for ((original, alias) in sortedEntries) {
            // 替换开始标签
            result = result.replace("<$alias ", "<$original ")
            result = result.replace("<$alias>", "<$original>")
            result = result.replace("<$alias/>", "<$original/>")

            // 替换结束标签
            result = result.replace("</$alias>", "</$original>")
        }

        return result
    }
}
