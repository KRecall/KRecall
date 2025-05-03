package io.github.octestx.krecall

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.TrayState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.octestx.basic.multiplatform.common.utils.link
import io.github.octestx.basic.multiplatform.common.utils.ojson
import io.github.octestx.basic.multiplatform.ui.ui.BasicMUIWrapper
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.exception.InvalidKeyPluginException
import io.github.octestx.krecall.model.InitConfig
import io.github.octestx.krecall.nav.GlobalNavDataExchangeCache
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.impl.PluginAbilityManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.ui.TimestampViewPage
import io.github.octestx.krecall.ui.animation.AnimationComponents
import io.github.octestx.krecall.ui.callExtremeErrorWindow
import io.github.octestx.krecall.ui.home.HomePage
import io.github.octestx.krecall.ui.setting.SettingPage
import io.github.octestx.krecall.ui.tour.PluginConfigPage
import io.github.octestx.krecall.ui.tour.RecallSettingPage
import io.github.octestx.krecall.ui.tour.SelectLanguage
import io.github.octestx.krecall.ui.tour.WelcomePage
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.klogging.noCoLogger
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.*
import moe.tlaster.precompose.navigation.transition.NavTransition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File

class AppMainPage(private val model: AppMainPageModel): AbsUIPage<Any?, AppMainPage.AppMainPageState, AppMainPage.AppMainPageAction>(model) {
    private val ologger = noCoLogger<AppMainPage>()
    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI(state: AppMainPageState) {
        BasicMUIWrapper {
            PreComposeApp {
                // An alias of SingletonImageLoader.setSafe that's useful for
                // Compose Multiplatform apps.
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .memoryCache {
                            MemoryCache.Builder()
                                // 设置缓存大小为 100MB
                                .maxSizeBytes(100L * 1024 * 1024)
                                .build()
                        }
                        .build()
                }

                val initConfigFile = remember {
                    File(File(System.getProperty("user.dir")), "KRecall-INIT.json").apply {
                        parentFile.mkdirs()
                        createNewFile()
                    }
                }
                var initConfig by remember {
                    mutableStateOf(
                        try {
                            ojson.decodeFromString<InitConfig>(initConfigFile.readText())
                        } catch (e: Throwable) {
                            val config = InitConfig()
                            val text = ojson.encodeToString(config)
                            initConfigFile.writeText(text)
                            config
                        }
                    )
                }
                NavHost(
                    // 将 Navigator 给到 NavHost
                    navigator = state.navigator,
                    // 定义初始导航路径
                    initialRoute = "/loading",
                    // 自定义页面导航动画，这个是个可选项
                    navTransition = NavTransition(),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
//                    .onKeyEvent { event ->
//                        when (event.key) {
//                            Key.Escape -> {
//                                if (true) {
//                                    navigator.popBackStack()
//                                    true  // 事件已处理
//                                } else {
//                                    false
//                                }
//                            }
//                            else -> false
//                        }
//                    },
                ) {
                    scene(
                        route = "/loading",
                        navTransition = NavTransition(),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            AnimationComponents.LoadingBig(Modifier.align(Alignment.Center))
                        }
                    }
                    scene("/tour/selectLanguage") {
                        SelectLanguage {
                            state.navigator.navigate("/tour/welcome")
                        }
                    }
                    scene(
                        route = "/tour/welcome",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable { WelcomePage.WelcomePageModel(next = {
                            state.navigator.navigate("/tour/initConfig")
                        }) }
                        val page = rememberSaveable {
                            WelcomePage(model)
                        }
                        page.Main(Unit)
                    }
                    scene("/tour/initConfig") {
                        InitConfig(initConfig, { initConfig = it }, initConfigFile, { state.navigator.navigate("/initCore") })
                    }
                    scene("/initCore") {
                        var showProgress by remember { mutableStateOf(true) }
                        AnimatedVisibility(visible = showProgress) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimationComponents.LoadingBig(Modifier.align(Alignment.Center))
                            }
                        }
                        LaunchedEffect(Unit) {
                            val workDir = File(initConfig.dataDirAbsPath, "KRecall")
                            val result = Core.init(model.trayState, workDir)
                            result.onFailure {
                                showProgress = false
                                callExtremeErrorWindow(it)
                            }
                            result.onSuccess {
                                state.navigator.navigate("/loadPlugins")
                            }
                        }
                    }
                    scene("/loadPlugins") {
                        var showProgress by remember { mutableStateOf(true) }
                        var exceptionFeedback by remember { mutableStateOf<Throwable?>(null) }
                        suspend fun loadPlugins(): Result<Unit> = kotlin.runCatching {
                            showProgress = true
                            PluginManager.loadPlugins().onSuccess {
                                PluginManager.initLoadedPlugins()
                                showProgress = false
                                if (ConfigManager.config.initPlugin.not()) {
                                    state.navigator.navigate("/tour/pluginConfigPage")
                                    return@runCatching
                                }
                                PluginManager.initAllPlugins()
                                if (PluginManager.needJumpConfigUI.value) {
                                    state.navigator.navigate("/tour/pluginConfigPage")
                                } else {
                                    state.navigator.navigate("/home")
                                }
                            }.onFailure {
                                showProgress = false
                                exceptionFeedback = it
                                ologger.error(it) { "loadPlugins failed!" }
                            }
                        }
                        LaunchedEffect(Unit) {
                            loadPlugins().onFailure {
                                callExtremeErrorWindow(it)
                            }
                        }
                        AnimatedContent(showProgress) { loading ->
                            if (loading) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Text("加载插件中", modifier = Modifier.align(Alignment.TopCenter).padding(16.dp), style = MaterialTheme.typography.titleLarge)
                                    AnimationComponents.LoadingBig(Modifier.align(Alignment.Center))
                                }
                            } else {
                                val exception = exceptionFeedback
                                if (exception != null) {
                                    Column {
                                        val scope = rememberCoroutineScope()
                                        when (exception) {
                                            is InvalidKeyPluginException -> {
                                                Text("加载插件失败", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                                                Text("一些关键插件没有被加载,请检查插件目录是否存在关键插件的Jar包", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                                            }
                                            else -> {
                                                Text("加载插件失败", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        Button(onClick = {
                                            scope.launch {
                                                loadPlugins().onFailure {
                                                    callExtremeErrorWindow(it)
                                                }
                                            }
                                        }, modifier = Modifier.padding(16.dp)) {
                                            Text("重试")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    scene(
                        route = "/tour/pluginConfigPage",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable {
                            PluginConfigPage.PluginConfigModel() {
                                state.navigator.navigate("/tour/recallSetting", options = NavOptions(popUpTo = PopUpTo.Prev))
                                ConfigManager.save(ConfigManager.config.copy(
                                    initialized = true,
                                    initPlugin = true
                                ))
                            }
                        }
                        val page = rememberSaveable {
                            PluginConfigPage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/tour/recallSetting",
                        navTransition = NavTransition()
                    ) {
                        val model = rememberSaveable { RecallSettingPage.RecallSettingPageModel(next = {
                            state.navigator.navigate("/home")
                        }) }
                        val page = rememberSaveable {
                            RecallSettingPage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/home",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable {
                            HomePage.HomePageModel(
                                navigate = {
                                    state.navigator.navigate(it)
                                }, putNavData = { key, value ->
                                    GlobalNavDataExchangeCache.putData(key, value)
                                }
                            )
                        }
                        val page = rememberSaveable {
                            HomePage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/timestampViewPage",
                        navTransition = NavTransition(),
                    ) {
                        val modelDataId = it.query<String>("modelDataId")
                        if (modelDataId == null) {
                            state.navigator.goBack()
                            return@scene
                        }
                        LaunchedEffect(Unit) {
                            ologger.info { "ReceiveModelDataId: $modelDataId" }
                        }
                        val modelData: TimestampViewPage.TimestampViewPageModelData = remember { GlobalNavDataExchangeCache.getAndDestroyData(modelDataId) as TimestampViewPage.TimestampViewPageModelData }
                        val model = rememberSaveable { TimestampViewPage.TimestampViewPageModel {
                            state.navigator.goBack()
                        } }
                        val page = rememberSaveable {
                            TimestampViewPage(model)
                        }
                        page.Main(modelData)
                    }
                    scene(
                        route = "/setting",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable {
                            SettingPage.SettingPageModel(
                                navigate = { target ->
                                    state.navigator.navigate(target)
                                }, putNavData = { key, value ->
                                    GlobalNavDataExchangeCache.putData(key, value)
                                }, goBack = {
                                    state.navigator.goBack()
                                }
                            )
                        }
                        val page = rememberSaveable {
                            SettingPage(model)
                        }
                        page.Main(Unit)
                    }

                    PluginAbilityManager.bindPluginRouter(this)
                }




                LaunchedEffect(Unit) {
                    if (initConfig.dataDirAbsPath == null) {
                        state.navigator.navigate("/tour/selectLanguage")
                    } else {
                        state.navigator.navigate("/initCore")
                    }
                }
            }
        }
    }

    sealed class AppMainPageAction: AbsUIAction() {

    }
    data class AppMainPageState(
        val navigator: Navigator,
    ): AbsUIState<AppMainPageAction>()

    class AppMainPageModel(val trayState: TrayState): AbsUIModel<Any?, AppMainPageState, AppMainPageAction>() {
        @Composable
        override fun CreateState(params: Any?): AppMainPageState {
            return AppMainPageState(
                navigator = rememberNavigator(),
            )
        }

        override fun actionExecute(params: Any?, action: AppMainPageAction) {

        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun InitConfig(
        initConfig: InitConfig,
        changeInitConfig: (InitConfig) -> Unit,
        initConfigFile: File,
        nextStep: () -> Unit
    ) {
        if (initConfig.dataDirAbsPath == null) {
            var showCheckSelectWorkDirDialog: File? by remember { mutableStateOf(null) }

            val workDir = showCheckSelectWorkDirDialog
            if (workDir != null) {
                BasicAlertDialog(onDismissRequest = {}) {
                    Card(Modifier.background(MaterialTheme.colorScheme.background)) {
                        val existKRecallDir = remember(workDir) {
                            val krecallDir = workDir.link("KRecall")
                            if (krecallDir.isFile) krecallDir.delete()
                            krecallDir.exists()
                        }
                        if (existKRecallDir) {
                            Text("发现已存在的KRecall数据目录，是否确认选择")
                        } else {
                            Text("未发现KRecall数据目录，如果你需要加载已有的KRecall数据目录, 请重新选择")
                        }
                        Row {
                            Button(onClick = {
                                showCheckSelectWorkDirDialog = null
                            }, modifier = Modifier.padding(6.dp)) {
                                Text("重新选择")
                            }
                            Button(onClick = {
                                val config = initConfig.copy(dataDirAbsPath = workDir.absolutePath)
                                val text = ojson.encodeToString(config)
                                initConfigFile.writeText(text)
                                changeInitConfig(config)
                            }, modifier = Modifier.padding(6.dp)) {
                                Text("确认选择")
                            }
                        }
                    }
                }
            }
            val scope = rememberCoroutineScope()
            Column(Modifier.padding(25.dp)) {
                Text("请先设置数据目录[第一次使用只要输入空文件夹目录.已有数据用户请输入数据目录,不是名称为KRecall的目录，而是它的父目录]！")
                var path by remember { mutableStateOf("") }
                val pickWorkDirDialogLauncher = rememberDirectoryPickerLauncher { directory ->
                    val absPath = directory?.absolutePath()
                    if (absPath != null) {
                        path = absPath
                    }
                }
                OutlinedTextField(path, { path = it}, modifier = Modifier.fillMaxWidth())
                Row {
                    OutlinedButton(onClick = {
                        pickWorkDirDialogLauncher.launch()
                    }) {
                        Text("选择目录")
                    }
                    OutlinedButton(onClick = {
                        val workDir = File(path)
                        if (workDir.exists()) {
                            showCheckSelectWorkDirDialog = workDir
                        } else {
                            scope.launch {
                                toast.applyShow("目录无效, 目录必须存在", type = ToastModel.Type.Error)
                            }
                        }
                    }) {
                        Text("Apply")
                    }
                }
            }
        } else {
            LaunchedEffect(Unit) {
                nextStep()
            }
        }
    }
}