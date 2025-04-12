package io.github.octestx.krecall.plugins.basic

import java.io.File
import java.io.OutputStream

/**
 * A parameterless constructor is required when inheriting a plugin.
 */
abstract class AbsCaptureScreenPlugin(pluginId: String): PluginBasic(pluginId) {
    abstract suspend fun supportOutputToStream(): Boolean

    /**
     * @exception Exception
     * @exception UnsupportedOperationException
     * IO
     */
    abstract suspend fun getScreen(outputStream: OutputStream): WindowInfo
    /**
     * @exception Exception
     * IO
     */
    abstract suspend fun getScreen(outputFileBitItNotExits: File): WindowInfo
}

data class WindowInfo(
    val screenId: Int,
    val appId: String,
    val windowTitle: String
)