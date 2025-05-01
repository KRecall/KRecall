package io.github.octestx.krecall

import androidx.compose.ui.window.TrayState
import arrow.fx.coroutines.await.ExperimentalAwaitAllApi
import arrow.fx.coroutines.await.awaitAll
import io.github.octestx.basic.multiplatform.common.BasicMultiplatformConfigModule
import io.github.octestx.basic.multiplatform.common.JVMInitCenter
import io.github.octestx.basic.multiplatform.common.utils.checkSelfIsSingleInstance
import io.github.octestx.basic.multiplatform.ui.JVMUIInitCenter
import io.github.octestx.krecall.exceptions.InvalidKeyPluginException
import io.github.octestx.krecall.exceptions.SingleInstanceException
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
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

object Core {
    private val ologger = noCoLogger<Core>()
    private val ioscope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var initialized = false

    /**
     * 初始化核心模块
     * @param trayState 托盘状态
     * @exception SingleInstanceException 如果已经运行了一个实例
     * @exception InvalidKeyPluginException 关键插件没有集全
     */
    @OptIn(ExperimentalAwaitAllApi::class)
    suspend fun init(trayState: TrayState, workDir: File): Result<Unit> = awaitAll {
        if (initialized) return@awaitAll Result.success(Unit)
        val run1 = async(Dispatchers.IO) {
            if (checkSelfIsSingleInstance().not()) {
                ologger.error(SingleInstanceException()) { "Already run one now!" }
                Result.failure(SingleInstanceException())
            } else {
                Result.success(Unit)
            }
        }

        val run2 = async(Dispatchers.IO) {
            try {
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

                PluginManager.init()
                if (ConfigManager.config.initialized && ConfigManager.config.initPlugin) {
                    PluginManager.initAllPlugins()
                }
                Result.success(Unit)
            } catch (e: Throwable) {
                ologger.error(e) { "Init failed!" }
                Result.failure(e)
            }
        }

        run2.await().onFailure { return@awaitAll Result.failure(it) }
        //It may be thrown InvalidKeyPluginException

        run1.await().onFailure { return@awaitAll Result.failure(it) }

        initialized = true
        ologger.info { "INITIALIZED" }
        return@awaitAll Result.success(Unit)
    }
}