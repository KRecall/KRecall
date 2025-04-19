package io.github.octestx.krecall.plugins

import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.basic.PluginMetadata

actual suspend fun getPlatformExtPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic> {
    TODO("Not yet implemented")
}

actual suspend fun getPlatformInnerPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic> {
    TODO("Not yet implemented")
}