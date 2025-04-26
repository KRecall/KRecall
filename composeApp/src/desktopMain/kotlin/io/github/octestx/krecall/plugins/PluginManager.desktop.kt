package io.github.octestx.krecall.plugins

import io.github.octestx.basic.multiplatform.common.appDirs
import io.github.octestx.basic.multiplatform.common.utils.asKFilePath
import io.github.octestx.basic.multiplatform.common.utils.linkDir
import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByWinPowerShellPlugin
import io.github.octestx.krecall.plugins.impl.ocr.OCRByZhiPuPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.github.vinceglb.filekit.utils.toFile

actual suspend fun getPlatformExtPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic> {
    val jarsDir = appDirs.getUserDataDir().asKFilePath().linkDir("JarPlugins").toFile()
    JarPluginManager.loadPluginsFromDir(jarsDir)
    return JarPluginManager.plugins.values.map {
        it.metadata to { metadata: PluginMetadata ->
            if (metadata != it.metadata) throw IllegalArgumentException("metadata is not right")
            it.instanceCreator()
        }
    }.toMap()
}

actual suspend fun getPlatformInnerPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic> {
    //TODO
    return mapOf(
//        CaptureScreenByKDESpectaclePlugin.metadata to { CaptureScreenByKDESpectaclePlugin(it) },
        CaptureScreenByWinPowerShellPlugin.metadata to { CaptureScreenByWinPowerShellPlugin(it) },

        OCRByZhiPuPlugin.metadata to { OCRByZhiPuPlugin(it) },
//        PPOCRPlugin(),

        OTStoragePlugin.metadata to { OTStoragePlugin(it) }
    )
}