package io.github.octestx.krecall.plugins

import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByKDESpectaclePlugin
import io.github.octestx.krecall.plugins.capturescreen.CaptureScreenByWinPowerShellPlugin
import io.github.octestx.krecall.plugins.impl.ocr.OCRByZhiPuPlugin
import io.github.octestx.krecall.plugins.impl.storage.OTStoragePlugin
import io.github.octestx.krecall.plugins.ocr.PPOCRPlugin

actual fun getPlatformExtPlugins(): Set<PluginBasic> {
    //TODO
    return setOf()
}

actual fun getPlatformInnerPlugins(): Set<PluginBasic> {
    //TODO
    return setOf(
        CaptureScreenByKDESpectaclePlugin(),
        CaptureScreenByWinPowerShellPlugin(),

        OCRByZhiPuPlugin(),
        PPOCRPlugin(),

        OTStoragePlugin(),
    )
}