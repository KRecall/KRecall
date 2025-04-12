package io.github.octestx.krecall.plugins.basic

import org.koin.java.KoinJavaComponent.get
import java.io.File
import java.io.OutputStream

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsStoragePlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun requireImageOutputStream(timestamp: Long): OutputStream
    abstract suspend fun requireImageFileBitItNotExits(timestamp: Long): File
    abstract suspend fun requireAudioOutputStream(timestamp: Long): OutputStream
    abstract suspend fun processed(timestamp: Long)
    /**
     * @exception NoSuchFileException
     */
    abstract suspend fun getScreenData(timestamp: Long): Result<ByteArray>
    protected val imageDir: File = File(get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId), "ScreenImages").apply {
        if (!exists()) mkdirs()
    }
    protected val audioDir: File = File(get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId), "Audios").apply {
        if (!exists()) mkdirs()
    }
    protected fun mark(timestamp: Long, mark: String) = get<IPluginContext>(IPluginContext::class.java).addMark(timestamp, mark)
    protected fun listTimestampWithMark(mark: String): List<Long> = get<IPluginContext>(IPluginContext::class.java).listTimestampWithMark(mark)
    protected fun listTimestampWithNotMark(mark: String): List<Long> = get<IPluginContext>(IPluginContext::class.java).listTimestampWithNotMark(mark)
}