package io.github.octestx.krecall.plugins.impl.storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.octestx.basic.multiplatform.common.utils.*
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.StepLoadAnimation
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

class OTStoragePlugin(metadata: PluginMetadata): AbsStoragePlugin(metadata) {
    private lateinit var imagePHash: ImagePHash
    companion object {
        val metadata = PluginMetadata(
            pluginId = "OTStoragePlugin",
            supportPlatform = setOf(OS.OperatingSystem.WIN, OS.OperatingSystem.LINUX, OS.OperatingSystem.MACOS, OS.OperatingSystem.OTHER),
            supportUI = true,
            mainClass = "io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin"
        )
    }
    private val ologger = noCoLogger<OTStoragePlugin>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: StorageConfig

    @Serializable
    data class StorageConfig(
        val filePath: String,
        val limitStorage: Long,
        val imageSimilarityLimit: Float,
    )
    override suspend fun requireImageOutputStream(timestamp: Long): OutputStream = requireImageFileBitItNotExits(timestamp).apply { createNewFile() }.outputStream()
    private fun linkScreenFile(fileTimestamp: Long): File {
        return File(getScreenDir(), "$fileTimestamp.png")
    }
    private fun getAudioFile(fileTimestamp: Long): File {
        return File(getCaptureAudioDir(), "$fileTimestamp.wav")
    }

    override suspend fun requireImageFileBitItNotExits(timestamp: Long): File {
        val f = linkScreenFile(timestamp)
        if (f.exists()) f.delete()
        OTStorageDB.addNewRecord(timestamp, timestamp)
        actuallyStorageScreenCount ++
        return f
    }

    override suspend fun requireAudioOutputStream(timestamp: Long): OutputStream {
        val f = getAudioFile(timestamp)
        if (f.exists()) f.delete()
        return f.outputStream()
    }

    override suspend fun processed(timestamp: Long) = withContext(Dispatchers.IO) {
        checkSpace()
        val previousFileTimestamp = OTStorageDB.getPreviousData(timestamp)?.fileTimestamp
        if (previousFileTimestamp != null) {
            val currentImg = getScreenDataByFileTimestamp(timestamp).getOrNull()
            val previousImg = getScreenDataByFileTimestamp(previousFileTimestamp).getOrNull()
            if (currentImg != null && previousImg != null) {
                val distance = imagePHash.distance(currentImg, previousImg)
                val similarity = imagePHash.toPercent(distance)
                ologger.info { "图片相似度检测结果: ${similarity * 100}% [$distance] (${timestamp} vs ${previousFileTimestamp})" }

                if (similarity >= config.imageSimilarityLimit) {
                    //TODO 相似度算法残废
                    // 更新索引并删除当前文件
                    OTStorageDB.setFileTimestamp(timestamp, previousFileTimestamp)
                    linkScreenFile(timestamp).delete()
                    actuallyStorageScreenCount --
                    ologger.info { "相似度${"%.2f".format(similarity*100)}% 超过阈值，已复用历史图片" }
                }
            }
        }
        //TODO 图片优化存储
    }

    private suspend fun checkSpace() {
        val store = withContext(Dispatchers.IO) {
            Files.getFileStore(Paths.get(getScreenDir().absolutePath))
        }
        if (store.usableSpace < config.limitStorage) {
            val list = listTimestampWithNotMark("Deleted").sorted()
            //为了避免持续性清理，一次性清除1.5倍的数据
            val needSpace = (config.limitStorage - store.usableSpace) * 1.5
            var countSpace = 0L
            val countedFiles = mutableListOf<Pair<Long, File>>()
            for (itemTimeStamp in list) {
                val f = linkScreenFile(itemTimeStamp)
                countSpace += f.length()
                countedFiles.add(itemTimeStamp to f)
                if (countSpace >= needSpace) break
            }
            for (f in countedFiles) {
                f.second.delete()
                mark(f.first, "Deleted")
                ologger.info { "DeleteFile: ${f.second.absolutePath}" }
            }
            ologger.info { "已清理$countSpace Bytes空间" }
        }
    }

    override suspend fun getScreenData(timestamp: Long): Result<ByteArray> {
        var fileTimestamp = OTStorageDB.getData(timestamp)?.fileTimestamp
//        //TODO以前的数据没有
//        if (fileTimestamp == null) {
//            fileTimestamp = timestamp
//            OTStorageDB.addNewRecord(timestamp, timestamp)
//        }
        return getScreenDataByFileTimestamp(fileTimestamp!!)
    }

    // use storage dir timestamp, not record-timestamp
    private suspend fun getScreenDataByFileTimestamp(timestamp: Long): Result<ByteArray> {
        val f = File(getScreenDir(), "$timestamp.png")
        return if (f.exists()) Result.success(f.readBytes())
        else Result.failure(FileNotFoundException(f.absolutePath))
    }

    private fun getScreenDir() = if (config.filePath.isEmpty()) imageDir else File(config.filePath)
    private fun getCaptureAudioDir() = if (config.filePath.isEmpty()) audioDir else File(config.filePath)
    override fun load() {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            config = StorageConfig(
                filePath = "",
                limitStorage = 20L * 1024 * 1024 * 1024,
                0.95f,
            )
            configFile.writeText(ojson.encodeToString(config))
        }
        OTStorageDB.init(pluginDir.toKPath().linkFile("data.db"))
        ologger.info { "Loaded" }
    }

    override fun selected() {
        imagePHash = ImagePHash()
    }
    override fun unselected() {}
    private var savedConfig = MutableStateFlow(true)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        Column {
            var filePath by remember { mutableStateOf(config.filePath ?: "") }
            TextField(filePath, {
                filePath = it
                configDataChange()
            }, label = {
                Text("文件存储路径，为空则默认")
            })
            var limitStorage by remember { mutableStateOf((config.limitStorage / (1024L * 1024 * 1024)).toString()) }
            val num = limitStorage.toIntOrNull()
            Row {
                TextField(limitStorage, {
                    limitStorage = it
                    configDataChange()
                }, label = {
                    val text = if (num == null) "只能输入数字!" else "$num GB"
                    Text("存储限制[$text]")
                })
                Text("GB")
            }
            var imageSimilarityLimit by remember { mutableStateOf(config.imageSimilarityLimit) }
            Text("drop similarity more that: ${imageSimilarityLimit * 100} % Image")
            Slider(imageSimilarityLimit, {
                imageSimilarityLimit = it
                configDataChange()
            })
            var saveText = "Save"
            Button(onClick = {
                try {
                    val checkedDir = if (filePath.isEmpty()) {
                        true
                    } else {
                        File(filePath).apply { mkdirs() }.exists()
                    }
                    if (num != null && checkedDir) {
                        val newConfig = StorageConfig(filePath, num * (1024L * 1024 * 1024), imageSimilarityLimit)
                        configFile.writeText(ojson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
                        savedConfig.value = true
                        ologger.info { "Saved" }
                        toast.applyShow("Saved", type = ToastModel.Type.Success)
                    }
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }, enabled = initialized.value.not()) {
                Text(saveText)
            }
        }
    }


    override suspend fun tryInitInner(context: PluginContext): InitResult {
        ologger.info { "TryInit" }
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (getScreenDir().canRead().not()) return InitResult.Failed(FileNotFoundException("Can't read: ${getScreenDir().absolutePath}"))

        actuallyStorageScreenCount = getScreenDir().listFileNotDir().size

        context.ability.setDrawerUI {
            Card(Modifier.padding(8.dp)) {
                val space = getStorageSpaceInfo()
                Text("存储空间：${space.progress * 100}%", modifier = Modifier.padding(8.dp))
                HorizontalDivider()
                LinearProgressIndicator(
                    progress = { space.progress },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                )
            }
        }
        context.ability.addMainTab("OTStorage") {
            StepLoadAnimation(5) { step ->
                Column {
                    AnimatedVisibility(step >= 1) {
                        Text("OTStorage", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge)
                    }
                    AnimatedVisibility(step >= 2) {
                        Card(Modifier.padding(8.dp)) {
                            val space = getStorageSpaceInfo()
                            Text("存储空间：${space.progress * 100}%", modifier = Modifier.padding(8.dp))
                            AnimatedVisibility(step >= 3) {
                                Text("${storage(space.usedSpace)} / ${storage(space.totalSpace)}")
                            }
                            AnimatedVisibility(step >= 4) {
                                HorizontalDivider()
                            }
                            AnimatedVisibility(step >= 5) {
                                LinearProgressIndicator(
                                    progress = { space.progress },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                )
                            }
                            AnimatedVisibility(step >= 6) {
                                Text("实际存储截屏数量: $actuallyStorageScreenCount", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }

        ologger.info { "Initialized" }
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized

    private fun configDataChange() {
        savedConfig.value = false
        _initialized.value = false
    }

    private var actuallyStorageScreenCount by mutableStateOf(0)
    private fun getStorageSpaceInfo(): StorageSpaceInfo {
        val totalSpace = getScreenDir().totalSpace
        val usableSpace = getScreenDir().usableSpace
        return StorageSpaceInfo(totalSpace, usableSpace)
    }
    data class StorageSpaceInfo(
        val totalSpace: Long,
        val usableSpace: Long,
    ) {
        val progress: Float by lazy {
            (usedSpace.toDouble() / totalSpace).toFloat()
        }
        val usedSpace = totalSpace - usableSpace
    }
}