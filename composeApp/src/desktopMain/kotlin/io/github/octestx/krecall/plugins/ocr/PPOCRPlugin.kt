package io.github.octestx.krecall.plugins.ocr

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import io.github.kotlin.fibonacci.utils.OS
import io.github.kotlin.fibonacci.utils.ojson
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AbsOCRPlugin
import io.github.octestx.krecall.plugins.basic.OCRResult
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.github.octestx.krecall.plugins.ocr.ocrlibrary.OCRCore
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File


class PPOCRPlugin: AbsOCRPlugin("PPOCRPlugin") {
    @Volatile
    private var ocrCore: OCRCore? = null
    override val supportPlatform: Set<OS.OperatingSystem> = setOf(OS.OperatingSystem.WIN, OS.OperatingSystem.LINUX, OS.OperatingSystem.MACOS, OS.OperatingSystem.OTHER)
    override val supportUI: Boolean = true
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: PPOCRPluginConfig

    @Serializable
    data class PPOCRPluginConfig(
        val modelFilePath: String
    )

    override suspend fun recognize(screen: ByteArray): OCRResult {
        kotlin.synchronized(Unit) {
            val tmpFile = File(pluginDir, "tmp.png")
            tmpFile.writeBytes(screen)
            val ocrResult = ocrCore!!.detect(tmpFile.absoluteFile, doAngle = true, mostAngle = true)
            return OCRResult(ocrResult.strRes, 1.0, ocrResult.textBlocks.map { textBlock -> textBlock.boxPoint.map { listOf(it.x, it.y) }.flatten() }, 0)
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
    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            //TODO
            config = PPOCRPluginConfig("")
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
            var modelDirPath by remember { mutableStateOf(config.modelFilePath) }
            TextField(modelDirPath, {
                modelDirPath = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("ModelsDir")
            })

            var saveText = "Save"
            Button(onClick = {
                try {
                    val newConfig = PPOCRPluginConfig(
                        modelDirPath
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
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }, enabled = initialized.value.not()) {
                Text(saveText)
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun tryInitInner(): InitResult {
        val confined = true
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
//        if (confined.not()) return InitResult.Failed(IllegalArgumentException("需要补全参数"))
        val libName = when (OS.getCurrentOS()) {
            OS.OperatingSystem.WIN -> "libRapidOcrOnnx.dll"
            OS.OperatingSystem.LINUX -> "libRapidOcrOnnx.so"
            OS.OperatingSystem.MACOS -> "libRapidOcrOnnx.dylib"
            OS.OperatingSystem.OTHER -> "libRapidOcrOnnx.so"
        }
        val libTmp = File(pluginDir, "$libName.tmp")
        val lib = File(pluginDir, libName)
        if (lib.exists().not()) {
            ologger.info { "部署PPOCR依赖库" }
            val libBytes = Res.readBytes("files/libs/$libName")
            libTmp.writeBytes(libBytes)
            libTmp.renameTo(lib)
            ologger.info { "部署PPOCR依赖库完成" }
        }

        val modelsDir = if (config.modelFilePath.isEmpty()) {
            ologger.warn { "未指定modelFilePath，使用内部模型" }
            val modelDir = File(pluginDir, "models")
            if (modelDir.exists().not()) {
                modelDir.mkdirs()
            }
            val detModelFile = File(modelDir, "det_infer.onnx")
            if (detModelFile.exists().not()) {
                ologger.info { "部署内部PPOCR-det模型" }
                val detModelFileTmp = File(modelDir, "det_infer.onnx1")
                val detModelBytes = Res.readBytes("files/models/det_infer.onnx")
                detModelFileTmp.writeBytes(detModelBytes)
                detModelFileTmp.renameTo(detModelFile)
            }

            val clsModelFile = File(modelDir, "cls_infer.onnx")
            if (clsModelFile.exists().not()) {
                ologger.info { "部署内部PPOCR-cls模型" }
                val clsModelFileTmp = File(modelDir, "cls_infer.onnx1")
                val clsModelBytes = Res.readBytes("files/models/cls_infer.onnx")
                clsModelFileTmp.writeBytes(clsModelBytes)
                clsModelFileTmp.renameTo(clsModelFile)
            }

            val recModelFile = File(modelDir, "rec_infer.onnx")
            if (recModelFile.exists().not()) {
                ologger.info { "部署内部PPOCR-rec模型" }
                val recModelFileTmp = File(modelDir, "rec_infer.onnx1")
                val recModelBytes = Res.readBytes("files/models/rec_infer.onnx")
                recModelFileTmp.writeBytes(recModelBytes)
                recModelFileTmp.renameTo(recModelFile)
            }

            val langModeFile = File(modelDir, "ppocr_keys.txt")
            if (langModeFile.exists().not()) {
                ologger.info { "部署内部PPOCR-langMode" }
                val langModeFileTmp = File(modelDir, "ppocr_keys.txt1")
                val langModeBytes = Res.readBytes("files/models/ppocr_keys.txt")
                langModeFileTmp.writeBytes(langModeBytes)
                langModeFileTmp.renameTo(langModeFile)
            }
            ologger.info { "部署内部PPOCR模型完成" }
            modelDir
        } else {
            File(config.modelFilePath)
        }

        ocrCore = OCRCore(lib.absolutePath, modelsDir)

        ologger.info { "Initialized" }
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}

//private fun main() {
//    runBlocking {
//        val workDir = File(File(System.getProperty("user.dir")), "KRecall").apply {
//            mkdirs()
//        }
//
//        val config = BasicMultiplatformConfigModule()
//        config.configInnerAppDir(workDir)
//        startKoin() {
//            modules(
//                config.asModule(),
//                module {
//                    single<IPluginContext> { PluginContextImpl() }
//                }
//            )
//        }
//        JVMInitCenter.init()
//        JVMUIInitCenter.init()
//
//        FileTree.init()
//
//        val plugin = PPOCRPlugin()
//        plugin.load()
//        plugin.selected()
//        plugin.tryInit()
//        val screen = File("/home/octest/Myself/tmp/Screenshot_20250301_234058.png").readBytes()
//        val data = plugin.recognize(screen)
//        println(data)
//        delay(1500)
//        println("EXIT")
//    }
//}