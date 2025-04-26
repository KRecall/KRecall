package io.github.octestx.krecall

import androidx.compose.ui.window.TrayState
import io.github.octestx.basic.multiplatform.common.BasicMultiplatformConfigModule
import io.github.octestx.basic.multiplatform.common.JVMInitCenter
import io.github.octestx.basic.multiplatform.common.utils.checkSelfIsSingleInstance
import io.github.octestx.basic.multiplatform.ui.JVMUIInitCenter
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.PluginBasicExt
import io.github.octestx.krecall.plugins.basic.PluginEnvironment
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.FileTree
import io.github.vinceglb.filekit.utils.toFile
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import kotlin.system.exitProcess

object Core {
    private val ologger = noCoLogger<Core>()
    private val ioscope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var initialized = false
    suspend fun init(trayState: TrayState, workDir: File) {
        if (initialized) return
        val isSingle = checkSelfIsSingleInstance()
        if (isSingle.not()) {
            ologger.error { "Already run one now!" }
            exitProcess(18)
        }

        val config = BasicMultiplatformConfigModule()
        config.configInnerAppDir(workDir)
        // 配置 Koin
        val injectPluginData = module {
            scope(named(PluginBasicExt.KOIN_INJECT_SCOPE_NAME)) {
                scoped { (metadata: PluginMetadata) ->
                    PluginEnvironment(
                        FileTree.pluginData(metadata.pluginId).toFile().absoluteFile
                    )
                }
            }
        }
        startKoin() {
            modules(
                config.asModule(),
                injectPluginData
            )
        }
        JVMInitCenter.init()
        JVMUIInitCenter.init(trayState)

        FileTree.init()
        runBlocking {
            PluginManager.init()
            if (ConfigManager.config.initialized && ConfigManager.config.initPlugin) {
                PluginManager.initAllPlugins()
            }
        }

        initialized = true
        ologger.info { "INITIALIZED" }
    }
}