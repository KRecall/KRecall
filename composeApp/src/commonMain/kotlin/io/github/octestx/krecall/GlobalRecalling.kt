package io.github.octestx.krecall

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import io.github.octestx.basic.multiplatform.common.utils.ObservableLinkedList
import io.github.octestx.basic.multiplatform.common.utils.TimeStamp
import io.github.octestx.basic.multiplatform.common.utils.synchronized
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.utils.exceptionSerializableOjson
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object GlobalRecalling {
    private val ologger = noCoLogger<GlobalRecalling>()
    private val ioscope = CoroutineScope(Dispatchers.IO)

    val allTimestamp = mutableStateListOf<Long>()
    val collectingScreen = MutableStateFlow(true)
    val collectingDelay = MutableStateFlow(0L)
    val processingData = MutableStateFlow(true)
    private val errorTimestamp = mutableStateMapOf<Long, Exception>()
    val errorTimestampCount = MutableStateFlow(0)

    //Timestamp
    val processingDataList = ObservableLinkedList<Long>()

    private val imageLoadingDispatcher = Dispatchers.IO.limitedParallelism(10)
    private const val MAX_CACHE_SIZE = 100

    suspend fun putImageToCache(timestamp: Long, recreate: suspend () -> ByteArray) {
        withContext(imageLoadingDispatcher) {
            imageCreatorCache[timestamp] = recreate
            imageCache[timestamp] = recreate()
        }
    }
    suspend fun getImageFromCache(timestamp: Long, recreate: (suspend () -> ByteArray?)? = null): ByteArray? {
        return withContext(imageLoadingDispatcher) {
            val cache = imageCache[timestamp]
            if (cache == null) {
                val creator = imageCreatorCache[timestamp]
                val created = if (creator != null) {
                    creator()
                } else {
                    if (recreate != null) recreate()
                    else null
                }
                if (created != null) {
                    imageCache[timestamp] = created
                }
                created
            } else cache
        }
    }
    private val imageCreatorCache: MutableMap<Long, suspend () -> ByteArray> = mutableMapOf()
    private val imageCache: MutableMap<Long, ByteArray> = object : LinkedHashMap<Long, ByteArray>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }.synchronized() // 添加线程安全包装

    private var initialized = false
    fun init() {
        if (initialized) return
        initialized = true
        allTimestamp.addAll(DataDB.listAllData().map { it.timestamp })
        val pairs = DataDB.listNotProcessedData().filter { it.status == 2L }.map { it.timestamp to exceptionSerializableOjson.decodeFromString<Exception>(it.error!!) }
        errorTimestamp.putAll(pairs)
        errorTimestampCount.value = errorTimestamp.size
        val collectingScreenJob = ioscope.launch {
            try {
                while (true) {
                    if (collectingScreen.value) {
                        ologger.info { "CollectingScreenJobLoop" }
                        val captureScreen = PluginManager.getCaptureScreenPlugin().getOrThrow()
                        val storage = PluginManager.getStoragePlugin().getOrThrow()
                        val timestamp = TimeStamp.current
                        val windowInfo = if (captureScreen.supportOutputToStream()) {
                            val outputStream =storage.requireImageOutputStream(timestamp)
                            captureScreen.getScreen(outputStream)
                        } else {
                            val file = storage.requireImageFileBitItNotExits(timestamp)
                            captureScreen.getScreen(file)
                        }
                        DataDB.addNewRecord(windowInfo.screenId.toLong(), timestamp, "", windowInfo.appId, windowInfo.windowTitle)
                        processingDataList.addLast(timestamp)
                        allTimestamp.add(timestamp)
                    }
                    // refresh collecting screen delay for each 500 ms
                    for (i in 0 until (ConfigManager.config.collectScreenDelay / 50)) {
                        delay(50)
                        collectingDelay.value = (i + 1) * 50
                    }
                    collectingDelay.value = 0
                }
            } catch (e: Exception) {
                ologger.error(e) { "Collecting Fail!" }
            }
        }
        val processingDataJob = ioscope.launch {
            val needProcessData = DataDB.listNotProcessedData()
            for (data in needProcessData) {
                processingDataList.addLast(data.timestamp)
            }
            while (true) {
                if (processingData.value) {
                    ologger.info { "ProcessingDataJobLoop" }
                    val timestamp = processingDataList.pollLastOrNull()
                    try {
                        if (timestamp == null) {
                            delay(1000)
                            continue
                        }
                        val storage = PluginManager.getStoragePlugin().getOrThrow()
                        val captureScreenPlugin = PluginManager.getOCRPlugin().getOrThrow()
                        val screen = storage.getScreenData(timestamp)
                        screen.onSuccess {
                            try {
                                val data = captureScreenPlugin.recognize(it)
                                DataDB.setData(timestamp, data.text)
                                storage.processed(timestamp)
                                DataDB.processed(timestamp)
                                errorTimestamp.remove(timestamp)
                                errorTimestampCount.value = errorTimestamp.size
                            } catch (e :Exception) {
                                DataDB.happenError(timestamp, e)
                                ologger.warn { "TODO action #84578854932" }
//                                errorTimestamp[timestamp] = e
//                                errorTimestampCount.value = errorTimestamp.size
                            }
                        }
                        ologger.info { "processed: $timestamp" }
                    } catch (e: Throwable) {
                        ologger.error(e) { "When processing the $timestamp catch a exception: ${e.message}" }
                    }
                }
            }
        }
    }
}