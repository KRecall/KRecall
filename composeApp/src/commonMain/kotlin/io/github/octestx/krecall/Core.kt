package io.github.octestx.krecall

import androidx.compose.ui.window.TrayState
import io.github.kotlin.fibonacci.BasicMultiplatformConfigModule
import io.github.kotlin.fibonacci.JVMInitCenter
import io.github.kotlin.fibonacci.JVMUIInitCenter
import io.github.kotlin.fibonacci.utils.checkSelfIsSingleInstance
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.IPluginContext
import io.github.octestx.krecall.plugins.impl.PluginContextImpl
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.FileTree
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.system.exitProcess

object Core {
    private val ologger = noCoLogger<Core>()
    private val ioscope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var initialized = false
    suspend fun init(trayState: TrayState) {
        if (initialized) return
        val isSingle = checkSelfIsSingleInstance()
        if (isSingle.not()) {
            ologger.error { "Already run one now!" }
            exitProcess(18)
        }

        val workDir = File(File(System.getProperty("user.dir")), "KRecall").apply {
            mkdirs()
        }
        val config = BasicMultiplatformConfigModule()
        config.configInnerAppDir(workDir)
        startKoin() {
            modules(
                config.asModule(),
                module {
                    single<IPluginContext> { PluginContextImpl() }
                }
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