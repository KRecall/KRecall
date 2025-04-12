package io.github.octestx.krecall.ui.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.*
import io.github.kotlin.fibonacci.ui.toast
import io.github.kotlin.fibonacci.ui.utils.ToastModel
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.exceptions.ConfigurationNotSavedException
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.AbsCaptureScreenPlugin
import io.github.octestx.krecall.plugins.basic.AbsOCRPlugin
import io.github.octestx.krecall.plugins.basic.AbsStoragePlugin
import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.klogging.noCoLogger
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ui.core.AbsUIPage

class PluginConfigPage(model: PluginConfigModel): AbsUIPage<Any?, PluginConfigPage.PluginConfigState, PluginConfigPage.PluginConfigAction>(model) {
    private val ologger = noCoLogger<PluginConfigPage>()
    @Composable
    override fun UI(state: PluginConfigState) {
        Column {
            Row {
                val allInitialized = PluginManager.AllPluginInitialized()
                if (allInitialized) {
                    Text("AllPluginsInitialized")
                    Button(onClick = {
                        state.action(PluginConfigAction.ConfigDone)
                    }) {
                        Text("ConfigDone")
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            PluginManager.initAllPlugins()
                        }
                    }) {
                        Text("InitAllPlugins")
                    }
                }

            }
            Box(Modifier.fillMaxSize()) {
                val scrollState = rememberLazyListState()
                LazyColumn(state = scrollState) {
                    item {
                        PluginCard(state.captureScreenPlugin, state.availableCaptureScreenPlugins, "截屏插件") {
                            state.action(PluginConfigAction.SelectGetScreenPlugin(it))
                        }
                    }
                    item {
                        PluginCard(state.storagePlugin, state.availableStoragePlugins, "存储插件") {
                            state.action(PluginConfigAction.SelectStoragePlugin(it))
                        }
                    }
                    item {
                        PluginCard(state.ocrPlugin, state.availableOCRPlugins, "OCR插件") {
                            state.action(PluginConfigAction.SelectScreenLanguageConverterPlugin(it))
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd) // 右侧对齐
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f)), // 半透明背景
                    adapter = rememberScrollbarAdapter(
                        scrollState = scrollState,
                    )
                )
            }
        }
    }
    @OptIn(ExperimentalResourceApi::class)
    @Composable
    private fun <P: PluginBasic> PluginCard(pluginData: Result<P>, availablePlugins: List<P>, type: String, selectedPlugin: (pluginId: String) -> Unit) {
        Column {
            Row {
                Text("Available: ")
                LazyRow {
                    items(availablePlugins) { plugin ->
                        Button(onClick = {
                            selectedPlugin(plugin.pluginId)
                        }, enabled = (pluginData.getOrNull()?.pluginId ?: System.nanoTime()) != plugin.pluginId) {
                            Text(plugin.pluginId)
                        }
                    }
                }
            }
            var err: Throwable? by remember { mutableStateOf(null) }
            pluginData.onFailure {
                Text("$type: 未加载[${it.message}]", color = MaterialTheme.colorScheme.secondary)
            }
            pluginData.onSuccess {
                val initialized by it.initialized.collectAsState()
                Column {
                    Row {
                        if (initialized) {
                            Text("$type[${it.pluginId}]: 已初始化", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        } else if (err == null) {
                            Text("$type[${it.pluginId}]: 未初始化", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        } else {
                            Text("$type[${it.pluginId}]: 未初始化[${err?.message}]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        }
                        AnimatedVisibility(pluginData.getOrNull()?.initialized?.value != true) {
                            Button(onClick = {
                                scope.launch {
                                    it.tryInit().also { initResult ->
                                        err = if (initResult is PluginBasic.InitResult.Failed) initResult.exception
                                        else null
                                    }
                                }
                            }) {
                                Text("INIT", modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    AnimatedContent(err) { err ->
                        if (err != null) {
                            Row {
                                val composition by rememberLottieComposition {
                                    LottieCompositionSpec.DotLottie(
                                        Res.readBytes("files/warning.lottie")
                                    )
                                }

                                Image(
                                    painter = rememberLottiePainter(
                                        composition = composition,
                                        iterations = Compottie.IterateForever
                                    ),
                                    contentDescription = "Lottie animation",
                                    modifier = Modifier.size(32.dp)
                                )
                                when (err) {
                                    is ConfigurationNotSavedException -> {
                                        Text("配置未保存: ${err.message}", modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    is IllegalArgumentException -> {
                                        Text("参数错误: ${err.message}", modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    else -> {
                                        Text("插件运行时异常: ${err.message}", modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                    if (it.supportUI) {
                        Surface(Modifier.padding(12.dp)) {
                            it.UI()
                        }
                    }
                }
            }
            LaunchedEffect(pluginData) {
                pluginData.onFailure {
                    ologger.error(it) { "插件异常[$type]: ${it.message}" }
                    toast.applyShow(ToastModel("插件异常[$type]: ${it.message}", type = ToastModel.Type.Error))
                }
            }
            LaunchedEffect(err) {
                err?.also {
                    ologger.error(it) { "插件运行时异常: ${it.message}" }
                    toast.applyShow(ToastModel("插件运行时异常: ${it.message}", type = ToastModel.Type.Error))
                }
            }
        }
    }

    sealed class PluginConfigAction : AbsUIAction() {
        //只有当插件全部初始化完毕后用户才能主动调用这个事件
        data object ConfigDone: PluginConfigAction()
        data class SelectGetScreenPlugin(val pluginId: String): PluginConfigAction()
        data class SelectStoragePlugin(val pluginId: String): PluginConfigAction()
        data class SelectScreenLanguageConverterPlugin(val pluginId: String): PluginConfigAction()
    }
    data class PluginConfigState(
        val captureScreenPlugin: Result<AbsCaptureScreenPlugin>,
        val availableCaptureScreenPlugins: List<AbsCaptureScreenPlugin>,
        val storagePlugin: Result<AbsStoragePlugin>,
        val availableStoragePlugins: List<AbsStoragePlugin>,
        val ocrPlugin: Result<AbsOCRPlugin>,
        val availableOCRPlugins: List<AbsOCRPlugin>,
        val action: (PluginConfigAction) -> Unit,
    ): AbsUIState<PluginConfigAction>()

    class PluginConfigModel(private val configDone: () -> Unit): AbsUIModel<Any?, PluginConfigState, PluginConfigAction>() {
        val ologger = noCoLogger<PluginConfigModel>()
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState(
                PluginManager.captureScreenPlugin.collectAsState().value,
                PluginManager.availableCaptureScreenPlugins.values.toList(),
                PluginManager.storagePlugin.collectAsState().value,
                PluginManager.availableStoragePlugins.values.toList(),
                PluginManager.ocrPlugin.collectAsState().value,
                PluginManager.availableOCRPlugins.values.toList(),
            ) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: PluginConfigAction) {
            when(action) {
                PluginConfigAction.ConfigDone -> {
                    if (PluginManager.getCaptureScreenPlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "GetScreenPlugin is not initialized" }
                    } else if (PluginManager.getStoragePlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "StoragePlugin is not initialized" }
                    }  else if (PluginManager.getOCRPlugin().getOrNull()?.initialized?.value != true) {
                        ologger.info { "ScreenLanguageConverterPlugin is not initialized" }
                    } else {
                        ologger.info { "All plugins are initialized" }
                        toast.applyShow("TODO, ConfigDone")
                    }
                    configDone()
                }

                is PluginConfigAction.SelectGetScreenPlugin -> {
                    PluginManager.setCaptureScreenPlugin(action.pluginId)
                }
                is PluginConfigAction.SelectScreenLanguageConverterPlugin -> {
                    PluginManager.setOCRPlugin(action.pluginId)
                }
                is PluginConfigAction.SelectStoragePlugin -> {
                    PluginManager.setStoragePlugin(action.pluginId)
                }
            }
        }
    }
}