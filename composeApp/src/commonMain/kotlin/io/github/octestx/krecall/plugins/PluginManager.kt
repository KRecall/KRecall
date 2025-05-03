package io.github.octestx.krecall.plugins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import arrow.atomic.AtomicBoolean
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.krecall.exception.InvalidKeyPluginException
import io.github.octestx.krecall.plugins.basic.*
import io.github.octestx.krecall.plugins.impl.PluginAbilityManager
import io.github.octestx.krecall.plugins.impl.PluginContextImpl
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect suspend fun getPlatformExtPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic>
expect suspend fun getPlatformInnerPlugins(): Map<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic>

object PluginManager {
    private val ologger = noCoLogger<PluginManager>()

    private val allPlugin: MutableMap<PluginMetadata, PluginBasic> = mutableMapOf()

    //TODO: 以后可以添加新插件支持
    private val _captureScreenPlugin: MutableStateFlow<Result<AbsCaptureScreenPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val captureScreenPlugin: StateFlow<Result<AbsCaptureScreenPlugin>> get() = _captureScreenPlugin
    private val _storagePlugin: MutableStateFlow<Result<AbsStoragePlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val storagePlugin: StateFlow<Result<AbsStoragePlugin>> get() = _storagePlugin
    private val _ocrPlugin: MutableStateFlow<Result<AbsOCRPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val ocrPlugin: StateFlow<Result<AbsOCRPlugin>> get() = _ocrPlugin

    private val _needJumpConfigUI: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val needJumpConfigUI: StateFlow<Boolean> get() = _needJumpConfigUI

    var allPluginLoaded = AtomicBoolean(false)

    @Throws(InvalidKeyPluginException::class)
    suspend fun initLoadedPlugins() {
        ologger.info { "initLoadedPlugins" }
        if (allPluginLoaded.value.not()) throw InvalidKeyPluginException("All plugins not loaded")
        selectConfigFromConfigFile()
        saveConfig()
    }

    /**
     * 加载插件
     */
    @Throws(InvalidKeyPluginException::class)
    suspend fun loadPlugins(): Result<Unit> {
        return kotlin.runCatching {
            ologger.info { "loadPlugins" }
            val preparePlugins = mutableMapOf<PluginMetadata, (metadata: PluginMetadata) -> PluginBasic>()
            preparePlugins.putAll(getPlatformExtPlugins())
            preparePlugins.putAll(getPlatformInnerPlugins())
            val plugins = mutableMapOf<PluginMetadata, PluginBasic>()
            for ((metadata, pluginCreator) in preparePlugins) {
                if (metadata.supportPlatform.contains(OS.currentOS).not()) continue
                val plugin = pluginCreator(metadata)
                plugins[metadata] = plugin
                plugin.load(PluginContextImpl(plugin.metadata))
                PluginAbilityManager.registerPlugin(plugin)
                when(plugin) {
                    is AbsCaptureScreenPlugin -> _availableCaptureScreenPlugins[metadata] = plugin
                    is AbsStoragePlugin -> _availableStoragePlugins[metadata] = plugin
                    is AbsOCRPlugin -> _availableOCRPlugins[metadata] = plugin
                    //TODO add new plugin type
                    else -> _allOtherPlugin[metadata] = plugin
                }
            }
            allPlugin.putAll(plugins)
            ologger.info { "LoadPlugins" }
            checkKeyPlugin()
            allPluginLoaded.set(true)
        }
    }
    @Throws(InvalidKeyPluginException::class)
    private fun checkKeyPlugin() {
        if (availableCaptureScreenPlugins.isEmpty() || availableStoragePlugins.isEmpty() || availableOCRPlugins.isEmpty()) {
            val errorMessage = """
                Have some plugins not available load
                availableCaptureScreenPlugins: ${availableCaptureScreenPlugins.keys}
                availableStoragePlugins: ${availableStoragePlugins.keys}
                availableOCRPlugins: ${availableOCRPlugins.keys}
            """.trimIndent()
            ologger.error {
                errorMessage
            }
            throw InvalidKeyPluginException(
                errorMessage,
                AbsCaptureScreenPlugin::class to availableCaptureScreenPlugins.values,
                AbsStoragePlugin::class to availableStoragePlugins.values,
                AbsOCRPlugin::class to availableOCRPlugins.values
            )
        }
    }

    private fun selectConfigFromConfigFile() {
        val config = ConfigManager.pluginConfig
        val captureScreenPlugin = kotlin.runCatching {
            val metadata = if (config.captureScreenPluginId == null) availableCaptureScreenPlugins.keys.firstOrNull() else availableCaptureScreenPlugins.keys.firstOrNull { it.pluginId == config.captureScreenPluginId }
            (availableCaptureScreenPlugins[metadata]!!).apply { selected() }
        }
        val storagePlugin = kotlin.runCatching {
            val metadata = if (config.storagePluginId == null) availableStoragePlugins.keys.firstOrNull() else availableStoragePlugins.keys.firstOrNull { it.pluginId == config.storagePluginId }
            availableStoragePlugins[metadata]!!.apply { selected() }
        }
        val ocrPlugin = kotlin.runCatching {
            val metadata = if (config.ocrPluginId == null) availableOCRPlugins.keys.firstOrNull() else availableOCRPlugins.keys.firstOrNull { it.pluginId == config.ocrPluginId }
            availableOCRPlugins[metadata]!!.apply { selected() }
        }
        if (captureScreenPlugin.isFailure || storagePlugin.isFailure || ocrPlugin.isFailure) {
            val errorMessage = """
                Plugins not loaded
                config:
                    captureScreenPlugin: ${config.captureScreenPluginId}
                    storagePluginId: ${config.storagePluginId}
                    ocrPluginId: ${config.ocrPluginId}
                loadingPluginsException:
                    captureScreenPlugin: ${captureScreenPlugin.exceptionOrNull()?.stackTrace}
                    storagePlugin: ${storagePlugin.exceptionOrNull()?.stackTrace}
                    ocrPlugin: ${ocrPlugin.exceptionOrNull()?.stackTrace}
            """.trimIndent()
            ologger.error { errorMessage }
        }
        _captureScreenPlugin.value = captureScreenPlugin
        _storagePlugin.value = storagePlugin
        _ocrPlugin.value = ocrPlugin
        ologger.info { "SelectConfigFromConfigFile" }
    }

    private val _allOtherPlugin = mutableMapOf<PluginMetadata, PluginBasic>()
    val allOtherPlugin: Map<PluginMetadata, PluginBasic> = _allOtherPlugin

    private val _availableCaptureScreenPlugins = mutableMapOf<PluginMetadata, AbsCaptureScreenPlugin>()
    val availableCaptureScreenPlugins: Map<PluginMetadata, AbsCaptureScreenPlugin> = _availableCaptureScreenPlugins
    fun setCaptureScreenPlugin(pluginMetadata: PluginMetadata) {
        if (pluginMetadata == getCaptureScreenPlugin().getOrNull()?.metadata) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getCaptureScreenPlugin().getOrNull()?.initialized?.value == true) return
        if (availableCaptureScreenPlugins.containsKey(pluginMetadata)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(captureScreenPluginId = pluginMetadata.pluginId))
            getCaptureScreenPlugin().getOrNull()?.unselected()
            _captureScreenPlugin.value = kotlin.runCatching { (availableCaptureScreenPlugins[pluginMetadata]!!).apply { selected() } }
            saveConfig()
        } else {
            ologger.error { "Plugin $pluginMetadata not found" }
        }
    }
    fun getCaptureScreenPlugin(): Result<AbsCaptureScreenPlugin> {
        return _captureScreenPlugin.value
    }

    private val _availableStoragePlugins = mutableMapOf<PluginMetadata, AbsStoragePlugin>()
    val availableStoragePlugins: Map<PluginMetadata, AbsStoragePlugin> = _availableStoragePlugins
    fun setStoragePlugin(pluginMetadata: PluginMetadata) {
        if (pluginMetadata == getStoragePlugin().getOrNull()?.metadata) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getStoragePlugin().getOrNull()?.initialized?.value == true) return
        if (availableStoragePlugins.containsKey(pluginMetadata)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(storagePluginId = pluginMetadata.pluginId))
            getStoragePlugin().getOrNull()?.unselected()
            _storagePlugin.value = kotlin.runCatching { (availableStoragePlugins[pluginMetadata]!!).apply { selected() } }
            saveConfig()
        }
    }
    fun getStoragePlugin(): Result<AbsStoragePlugin> {
        return _storagePlugin.value
    }

    private val _availableOCRPlugins = mutableMapOf<PluginMetadata, AbsOCRPlugin>()
    val availableOCRPlugins: Map<PluginMetadata, AbsOCRPlugin> = _availableOCRPlugins
    fun setOCRPlugin(pluginMetadata: PluginMetadata) {
        if (pluginMetadata == getOCRPlugin().getOrNull()?.metadata) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getOCRPlugin().getOrNull()?.initialized?.value == true) return
        if (availableOCRPlugins.containsKey(pluginMetadata)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(ocrPluginId = pluginMetadata.pluginId))
            getOCRPlugin().getOrNull()?.unselected()
            _ocrPlugin.value = kotlin.runCatching { (availableOCRPlugins[pluginMetadata]!!).apply { selected() } }
            saveConfig()
        }
    }
    fun getOCRPlugin(): Result<AbsOCRPlugin> {
        return _ocrPlugin.value
    }

    private fun saveConfig() {
        ConfigManager.savePluginConfig(
            ConfigManager.pluginConfig.copy(
                captureScreenPluginId = getCaptureScreenPlugin().getOrNull()?.metadata?.pluginId,
                storagePluginId = getStoragePlugin().getOrNull()?.metadata?.pluginId,
                ocrPluginId = getOCRPlugin().getOrNull()?.metadata?.pluginId
            )
        )
        ologger.info { "SaveConfig" }
    }

    suspend fun initAllPlugins() {
        val captureScreenPlugin = getCaptureScreenPlugin().getOrNull()
        val storagePlugin = getStoragePlugin().getOrNull()
        val ocrPlugin = getOCRPlugin().getOrNull()
        if (captureScreenPlugin == null || storagePlugin == null || ocrPlugin == null) {
            _needJumpConfigUI.value = true
            return
        }
        captureScreenPlugin.tryInit().apply {
            if (this is PluginBasic.InitResult.Failed || this is PluginBasic.InitResult.RequestConfigUI) _needJumpConfigUI.value = true
            if (this is PluginBasic.InitResult.Failed) ologger.error(exception) { "Try to init CaptureScreenPlugin catch: ${exception.message}" }
        }
        storagePlugin.tryInit().apply {
            if (this is PluginBasic.InitResult.Failed || this is PluginBasic.InitResult.RequestConfigUI) _needJumpConfigUI.value = true
            if (this is PluginBasic.InitResult.Failed) ologger.error(exception) { "Try to init StoragePlugin catch: ${exception.message}" }
        }
        ocrPlugin.tryInit().apply {
            if (this is PluginBasic.InitResult.Failed || this is PluginBasic.InitResult.RequestConfigUI) _needJumpConfigUI.value = true
            if (this is PluginBasic.InitResult.Failed) ologger.error(exception) { "Try to init OCRPlugin catch: ${exception.message}" }
        }
    }

    @Composable
    fun AllPluginInitialized(): Boolean {
        val captureScreenPluginInitialized = captureScreenPlugin.collectAsState().value.map { it.initialized.collectAsState().value }.getOrElse { false }
        val storagePluginInitialized = storagePlugin.collectAsState().value.map { it.initialized.collectAsState().value }.getOrElse { false }
        val ocrPluginInitialized = ocrPlugin.collectAsState().value.map { it.initialized.collectAsState().value }.getOrElse { false }

        return captureScreenPluginInitialized && storagePluginInitialized && ocrPluginInitialized
    }
}

suspend fun PluginBasic.tryInit() = tryInit()