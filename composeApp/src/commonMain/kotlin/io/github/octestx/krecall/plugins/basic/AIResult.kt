package io.github.octestx.krecall.plugins.basic

import io.github.kotlin.fibonacci.utils.ojson
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@Serializable
sealed class AIResult<T> {
    @Serializable
    data class Success<T>(val result: T): AIResult<T>()
    @Serializable
    data class Failed<T>(
        @Contextual // 添加上下文序列化注解
        val error: Exception,
        val type: AIErrorType
    ): AIResult<T>()
}
val exceptionSerializableOjson = Json(ojson) {
    serializersModule = SerializersModule {
        contextual(Exception::class, ExceptionSerializer)
    }
}
// 自定义 Exception 序列化器
object ExceptionSerializer : KSerializer<Exception> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Exception", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Exception) {
        encoder.encodeString(value.stackTraceToString())
    }

    override fun deserialize(decoder: Decoder): Exception {
        return Exception(decoder.decodeString())
    }
}
enum class AIErrorType(val message: String) {
    INVALID_API_KEY("无效的API密钥"),
    INVALID_MODEL("无效的模型"),
    API_QUOTA_EXHAUSTED("API调用过量（无剩余Token）"),
    API_RATE_LIMIT_CONCURRENCY("API调用限制（并发过多）"),
    API_RATE_LIMIT_TOKEN("API调用限制（单位时间内Token限制）"),
    SENSITIVE_INFO("敏感信息"),
    REQUEST_TIMEOUT("请求超时"),
    REQUEST_INTERRUPTED("请求中断"),
    UNKNOWN("未知错误")
}