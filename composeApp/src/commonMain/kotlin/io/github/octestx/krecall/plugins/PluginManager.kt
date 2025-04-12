package io.github.octestx.krecall.plugins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kotlin.fibonacci.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsCaptureScreenPlugin
import io.github.octestx.krecall.plugins.basic.AbsOCRPlugin
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect fun getPlatformExtPlugins(): Set<PluginBasic>
expect fun getPlatformInnerPlugins(): Set<PluginBasic>

object PluginManager {
    private val ologger = noCoLogger<PluginManager>()

    private lateinit var allPlugin: Map<String, PluginBasic>

    //TODO: 以后可以添加新插件支持
    private val _captureScreenPlugin: MutableStateFlow<Result<AbsCaptureScreenPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val captureScreenPlugin: StateFlow<Result<AbsCaptureScreenPlugin>> get() = _captureScreenPlugin
    private val _storagePlugin: MutableStateFlow<Result<AbsStoragePlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val storagePlugin: StateFlow<Result<AbsStoragePlugin>> get() = _storagePlugin
    private val _ocrPlugin: MutableStateFlow<Result<AbsOCRPlugin>> = MutableStateFlow(Result.failure(Exception("Plugin not loaded")))
    val ocrPlugin: StateFlow<Result<AbsOCRPlugin>> get() = _ocrPlugin

    private val _needJumpConfigUI: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val needJumpConfigUI: StateFlow<Boolean> get() = _needJumpConfigUI

    suspend fun init() {
        ologger.info { "InitPluginManager" }
        loadPlugins()
        selectConfigFromConfigFile()
        saveConfig()
    }

    private fun loadPlugins() {
        val preparePlugins = mutableSetOf<PluginBasic>()
        preparePlugins.addAll(getPlatformExtPlugins())
        preparePlugins.addAll(getPlatformInnerPlugins())
        val plugins = mutableMapOf<String, PluginBasic>()
        for (plugin in preparePlugins) {
            if (plugin.supportPlatform.contains(OS.getCurrentOS()).not()) continue
            plugins[plugin.pluginId] = plugin
            plugin.load()
            when(plugin) {
                is AbsCaptureScreenPlugin -> _availableCaptureScreenPlugins[plugin.pluginId] = plugin
                is AbsStoragePlugin -> _availableStoragePlugins[plugin.pluginId] = plugin
                is AbsOCRPlugin -> _availableOCRPlugins[plugin.pluginId] = plugin
                else -> _allOtherPlugin[plugin.pluginId] = plugin
            }
        }
        allPlugin = plugins
        ologger.info { "LoadPlugins" }
    }

    private fun selectConfigFromConfigFile() {
        val config = ConfigManager.pluginConfig
        val captureScreenPlugin = kotlin.runCatching {
            val id = config.captureScreenPluginId ?: availableCaptureScreenPlugins.keys.firstOrNull()
            (availableCaptureScreenPlugins[id]!!).apply { selected() }
        }
        val storagePlugin = kotlin.runCatching {
            val id = config.storagePluginId ?: availableStoragePlugins.keys.firstOrNull()
            (allPlugin[id]!! as AbsStoragePlugin).apply { selected() }
        }
        val ocrPlugin = kotlin.runCatching {
            val id = config.ocrPluginId ?: availableOCRPlugins.keys.firstOrNull()
            (allPlugin[id]!! as AbsOCRPlugin).apply { selected() }
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

    private val _allOtherPlugin = mutableMapOf<String, PluginBasic>()
    val allOtherPlugin: Map<String, PluginBasic> = _allOtherPlugin

    private val _availableCaptureScreenPlugins = mutableMapOf<String, AbsCaptureScreenPlugin>()
    val availableCaptureScreenPlugins: Map<String, AbsCaptureScreenPlugin> = _availableCaptureScreenPlugins
    fun setCaptureScreenPlugin(pluginId: String) {
        if (pluginId == getCaptureScreenPlugin().getOrNull()?.pluginId) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getCaptureScreenPlugin().getOrNull()?.initialized?.value == true) return
        if (availableCaptureScreenPlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(captureScreenPluginId = pluginId))
            getCaptureScreenPlugin().getOrNull()?.unselected()
            _captureScreenPlugin.value = kotlin.runCatching { (availableCaptureScreenPlugins[pluginId]!!).apply { selected() } }
            saveConfig()
        } else {
            ologger.error { "Plugin $pluginId not found" }
        }
    }
    fun getCaptureScreenPlugin(): Result<AbsCaptureScreenPlugin> {
        return _captureScreenPlugin.value
    }

    private val _availableStoragePlugins = mutableMapOf<String, AbsStoragePlugin>()
    val availableStoragePlugins: Map<String, AbsStoragePlugin> = _availableStoragePlugins
    fun setStoragePlugin(pluginId: String) {
        if (pluginId == getStoragePlugin().getOrNull()?.pluginId) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getStoragePlugin().getOrNull()?.initialized?.value == true) return
        if (availableStoragePlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(storagePluginId = pluginId))
            getStoragePlugin().getOrNull()?.unselected()
            _storagePlugin.value = kotlin.runCatching { (availableStoragePlugins[pluginId]!!).apply { selected() } }
            saveConfig()
        }
    }
    fun getStoragePlugin(): Result<AbsStoragePlugin> {
        return _storagePlugin.value
    }

    private val _availableOCRPlugins = mutableMapOf<String, AbsOCRPlugin>()
    val availableOCRPlugins: Map<String, AbsOCRPlugin> = _availableOCRPlugins
    fun setOCRPlugin(pluginId: String) {
        if (pluginId == getOCRPlugin().getOrNull()?.pluginId) {
            return
        }
        // 如果插件已经初始化，则不切换
        if (getOCRPlugin().getOrNull()?.initialized?.value == true) return
        if (availableOCRPlugins.containsKey(pluginId)) {
            ConfigManager.savePluginConfig(ConfigManager.pluginConfig.copy(ocrPluginId = pluginId))
            getOCRPlugin().getOrNull()?.unselected()
            _ocrPlugin.value = kotlin.runCatching { (availableOCRPlugins[pluginId]!!).apply { selected() } }
            saveConfig()
        }
    }
    fun getOCRPlugin(): Result<AbsOCRPlugin> {
        return _ocrPlugin.value
    }

    private fun saveConfig() {
        ConfigManager.savePluginConfig(
            ConfigManager.pluginConfig.copy(
                captureScreenPluginId = getCaptureScreenPlugin().getOrNull()?.pluginId,
                storagePluginId = getStoragePlugin().getOrNull()?.pluginId,
                ocrPluginId = getOCRPlugin().getOrNull()?.pluginId
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