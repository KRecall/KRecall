package io.github.octestx.krecall.plugins.impl.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.exception.InvalidRequestException
import com.aallam.openai.api.exception.OpenAIErrorDetails
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.github.kotlin.fibonacci.utils.OS
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AIErrorType
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.AbsOCRPlugin
import io.github.octestx.krecall.plugins.basic.OCRResult
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.klogging.noCoLogger
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File


class OCRByZhiPuPlugin: AbsOCRPlugin("OCRByZhiPuPlugin") {
    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.WIN, OS.OperatingSystem.LINUX, OS.OperatingSystem.MACOS, OS.OperatingSystem.OTHER)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: ScreenLanguageConverterByZhiPuPluginConfig

    @Serializable
    data class ScreenLanguageConverterByZhiPuPluginConfig(
        val apiKey: String,
        val model: String,
        val systemMsg: String,
        val temperature: Double,
        val topP: Double,
        val frequencyPenalty: Double,
        val backupOCRAPI: String,
    )
    private val defaultSystemMsg = """
        你是Win11的Recall功能的新后端，我会给你提供一系列电脑截图,请将截图中的内容分析出来，不要使用自然语言，请分成许多个短小的词语,并且进行合理断词,回答要全面，不能使用‘等’，例如分析文件管理器窗口时把里面包含的所有文件的文件名列出来,使用换行分割不同的词语,开头末尾不要引号,类似这样:Clash Verge
        OTStoragePlugin
        微信
        文件夹
        文件资源管理器
        所有图片
        gsd.png
        slk the first.png
        code.kt
    """.trimIndent()

    override suspend fun recognize(screen: ByteArray): OCRResult {
        val imgBase64 = screen.encodeBase64()
//        val messages: MutableList<ChatMessage> = ArrayList()
//        val contentList: MutableList<Map<String, Any>> = ArrayList()
//        contentList.add(mapOf(
//            "type" to "text",
//            "text" to config.systemMsg
//        ))
//        contentList.add(mapOf(
//            "type" to "image_url",
//            "image_url" to mapOf(
//                "url" to imgBase64
//            )
//        ))
//        val chatMessage = ChatMessage(ChatMessageRole.USER.value(), contentList)
//        messages.add(chatMessage)
//
//        val chatCompletionRequest = builder()
//            .model(config.model)
//            .stream(false)
//            .invokeMethod(Constants.invokeMethod)
//            .messages(messages)
//            .build()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(config.model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = config.systemMsg
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = listOf(
                        ImagePart(imgBase64)
                    )
                ),
            ),
            temperature = config.temperature,
            topP = config.topP,
            frequencyPenalty = config.frequencyPenalty,
        )
        ologger.info { "ConvertingData..." }
        try {
            val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
            val msg = completion.choices[0].message.content!!
            ologger.info { "ConvertedData: $msg" }
            return OCRResult(msg, 1.0, listOf(), 0)
        } catch (e: Exception) {
            if (e is InvalidRequestException) {
                val detail = e.error.detail
                val fail = AIResult.Failed<String>(e, getErrorTypeByZhiPuAI(detail))
                ologger.error(e) { "InvalidRequestException: [detail=$detail, fail=$fail]" }
                throw e
            } else {
                //TODO 错误类型分类
                val fail = AIResult.Failed<String>(e, AIErrorType.UNKNOWN)
                ologger.error(e) { "ConvertDataError: ${e.message}, fail: $fail" }
                throw e
            }
        }
    }

//    private val client: ClientV4 by lazy {
//        ClientV4.Builder(config.apiKey).networkConfig(
//            60,
//            60,
//            60,
//            60,
//            TimeUnit.SECONDS
//        ).build()
//    }
    private val openAI by lazy { OpenAI(config.apiKey, host = OpenAIHost("https://open.bigmodel.cn/api/paas/v4/")) }
    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            //TODO remove private api key
            config = ScreenLanguageConverterByZhiPuPluginConfig("2137bdde5a5344618ac99458a160430d.SQsjadVmdhLb5CgN", "GLM-4V-Flash", defaultSystemMsg, 0.1, 1.0, 2.0, "")
            configFile.writeText(ojson.encodeToString(config))
        }
        ologger.info { "Loaded" }
    }

    override fun selected() {}
    override fun unselected() {}
    private var savedConfig = MutableStateFlow(true)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        Column {
            var apiKey by remember { mutableStateOf(config.apiKey) }
            TextField(apiKey, {
                apiKey = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Key")
            })
            var model by remember { mutableStateOf(config.model) }
            TextField(model, {
                model = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Model")
            })
            var sysMsg by remember { mutableStateOf(config.systemMsg) }
            TextField(sysMsg, {
                sysMsg = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-SystemMessage")
            })
            var temperatureStr by remember { mutableStateOf(config.temperature.toString()) }
            TextField(temperatureStr, {
                temperatureStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-temperatureStr")
            })
            var topPStr by remember { mutableStateOf(config.topP.toString()) }
            TextField(topPStr, {
                topPStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-topP")
            })
            var frequencyPenaltyStr by remember { mutableStateOf(config.frequencyPenalty.toString()) }
            TextField(frequencyPenaltyStr, {
                frequencyPenaltyStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-frequencyPenalty")
            })

            var saveText = "Save"
            Button(onClick = {
                try {
                    if (
                        apiKey.isNotEmpty() &&
                        model.isNotEmpty() &&
                        sysMsg.isNotEmpty() &&
                        temperatureStr.toDoubleOrNull() != null &&
                        topPStr.toDoubleOrNull() != null &&
                        frequencyPenaltyStr.toDoubleOrNull() != null
                    ) {
                        val newConfig = ScreenLanguageConverterByZhiPuPluginConfig(
                            apiKey,
                            model,
                            sysMsg,
                            temperatureStr.toDouble(),
                            topPStr.toDouble(),
                            frequencyPenaltyStr.toDouble(),
                            ""//TODO use backupOCRAPI
                        )
                        configFile.writeText(ojson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
                        savedConfig.value = true
                        ologger.info { "Saved" }
                    }
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }, enabled = initialized.value.not()) {
                Text(saveText)
            }
        }
    }

    override suspend fun tryInitInner(): InitResult {
//        runBlocking { convert(File("/home/octest/Myself/tmp/Screenshot_20250301_234058.png").readBytes()) }
        val confined = config.apiKey.isNotEmpty() && config.model.isNotEmpty()
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (confined.not()) return InitResult.Failed(IllegalArgumentException("需要补全参数"))
        ologger.info { "Initialized" }
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized


    private fun getErrorTypeByZhiPuAI(details: OpenAIErrorDetails?): AIErrorType {
        if (details == null) return AIErrorType.UNKNOWN
        //look url: https://www.bigmodel.cn/dev/api/error-code/service-error
        return when (details.code) {
            // 1000系列（API密钥相关）
            "1000" -> AIErrorType.INVALID_API_KEY    // 原第5位
            "1001" -> AIErrorType.INVALID_API_KEY    // 原第4位
            "1002" -> AIErrorType.INVALID_API_KEY    // 原第3位
            "1003" -> AIErrorType.INVALID_API_KEY    // 原第2位
            "1004" -> AIErrorType.INVALID_API_KEY    // 原第1位

            // 1110-1120系列（API密钥/配额）
            "1110" -> AIErrorType.INVALID_API_KEY    // 原第9位
            "1111" -> AIErrorType.INVALID_API_KEY    // 原第8位
            "1112" -> AIErrorType.INVALID_API_KEY    // 原第7位
            "1113" -> AIErrorType.API_QUOTA_EXHAUSTED// 原第10位
            "1120" -> AIErrorType.INVALID_API_KEY    // 原第6位

            // 1200系列（模型相关）
            "1211" -> AIErrorType.INVALID_MODEL      // 原第11位
            "1220" -> AIErrorType.INVALID_MODEL      // 原第12位
            "1221" -> AIErrorType.INVALID_MODEL      // 原第13位
            "1222" -> AIErrorType.INVALID_MODEL      // 原第14位
            "1261" -> AIErrorType.API_QUOTA_EXHAUSTED// 原第15位

            // 1300系列（请求相关）
            "1300" -> AIErrorType.REQUEST_INTERRUPTED// 原第16位
            "1301" -> AIErrorType.SENSITIVE_INFO     // 原第17位
            "1302" -> AIErrorType.API_RATE_LIMIT_CONCURRENCY// 原第18位
            "1304" -> AIErrorType.API_RATE_LIMIT_TOKEN// 原第19位
            "1305" -> AIErrorType.API_RATE_LIMIT_CONCURRENCY// 原第20位
            else -> AIErrorType.UNKNOWN
        }
    }
}