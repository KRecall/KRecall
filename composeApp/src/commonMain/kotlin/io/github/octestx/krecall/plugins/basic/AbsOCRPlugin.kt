package io.github.octestx.krecall.plugins.basic

import kotlinx.serialization.Serializable

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsOCRPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun recognize(screen: ByteArray): OCRResult
}

@Serializable
data class OCRResult(
    val text: String,
    val confidence: Double,
    val text_region: List<List<Int>>,
    val rotation: Int
) {
    companion object {
        fun formatLog(result: OCRResult): String = buildString {
            appendLine("文本: ${result.text}")
            appendLine("置信度: ${"%.2f".format(result.confidence * 100)}%")
            appendLine("文本区域坐标: ${result.text_region}")
            appendLine("旋转角度: ${result.rotation}°\n")
        }
    }
}