package io.github.octestx.krecall.plugins.utils

import io.github.octestx.krecall.plugins.basic.OCRResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File

@Serializable
data class OCRMeta(
    val image_size: ImageSize,
    val process_time: Double
)

@Serializable
data class ImageSize(
    val width: Int,
    val height: Int
)

@Serializable
data class OCRResponse(
    val meta: OCRMeta,
    val results: List<OCRResult>
)

//TODO
class OCRAPIClient(
    private val apiAddress: String,
    private val timeoutMillis: Long = 30_000
) {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun recognize(image: ByteArray): OCRResponse {
        return client.post("$apiAddress/ocr") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "image",
                            ByteArrayInputStream(image),
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                            }
                        )
                    }
                )
            )
        }.body()
    }

    fun close() {
        client.close()
    }

    fun formatLog(response: OCRResponse): String = buildString {
        appendLine("处理时间: ${response.meta.process_time}s")
        appendLine("图片尺寸: ${response.meta.image_size.width}x${response.meta.image_size.height}")

        response.results.forEachIndexed { index, result ->
            appendLine("识别结果 #${index + 1}:")
            append(OCRResult.formatLog(result)).split("\n").joinToString { "    \n" }
        }
    }
}

// 使用示例
suspend fun main() {
    val client = OCRAPIClient("http://localhost:5000")

    try {
        // 从文件读取字节
        val imageBytes = File("test.jpg").readBytes()

        val response = client.recognize(imageBytes)
        println(client.formatLog(response))
    } catch (e: Exception) {
        println("OCR 失败: ${e.message}")
    } finally {
        client.close()
    }
}