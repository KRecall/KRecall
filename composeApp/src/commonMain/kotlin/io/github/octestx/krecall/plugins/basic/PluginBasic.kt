package io.github.octestx.krecall.plugins.basic

import androidx.compose.runtime.Composable
import io.github.kotlin.fibonacci.utils.OS
import kotlinx.coroutines.flow.StateFlow
import org.koin.java.KoinJavaComponent.get
import java.io.File

abstract class PluginBasic(val pluginId: String) {
    //决定了是否会被load
    abstract val supportPlatform: Set<OS.OperatingSystem>
    // 是否需要UI
    abstract val supportUI: Boolean
    abstract fun load()
    abstract fun selected()
    abstract fun unselected()
    @Composable
    abstract fun UI()
    protected abstract suspend fun tryInitInner(): InitResult
    suspend fun tryInit(): InitResult {
        return if (initialized.value.not()) tryInitInner()
        else InitResult.Success
    }
    abstract val initialized: StateFlow<Boolean>
    protected val pluginDir: File = get<IPluginContext>(IPluginContext::class.java).getPluginDir(pluginId)

    sealed class InitResult {
        data object Success: InitResult()
        // failed也会跳转到UI配置
        data class Failed(val exception: Exception): InitResult()
        data object RequestConfigUI: InitResult()
    }
}