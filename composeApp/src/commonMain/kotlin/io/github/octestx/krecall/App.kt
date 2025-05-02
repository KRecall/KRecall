package io.github.octestx.krecall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.octestx.basic.multiplatform.ui.ui.BasicMUIWrapper
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.krecall.nav.GlobalNavDataExchangeCache
import io.github.octestx.krecall.plugins.PluginAbilityManager
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.ui.TimestampViewPage
import io.github.octestx.krecall.ui.home.HomePage
import io.github.octestx.krecall.ui.setting.SettingPage
import io.github.octestx.krecall.ui.tour.LoadingPage
import io.github.octestx.krecall.ui.tour.PluginConfigPage
import io.github.octestx.krecall.ui.tour.RecallSettingPage
import io.github.octestx.krecall.ui.tour.WelcomePage
import io.klogging.noCoLogger
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.*
import moe.tlaster.precompose.navigation.transition.NavTransition

class AppMainPage(model: AppMainPageModel): AbsUIPage<Any?, AppMainPage.AppMainPageState, AppMainPage.AppMainPageAction>(model) {
    private val ologger = noCoLogger<AppMainPage>()
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
                        val model = rememberSaveable() { LoadingPage.LoadingPageModel() }
                        val page = rememberSaveable {
                            LoadingPage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/tour/welcome",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable { WelcomePage.WelcomePageModel(next = {
                            state.navigator.navigate("/tour/recallSetting")
                        }) }
                        val page = rememberSaveable {
                            WelcomePage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/tour/recallSetting",
                        navTransition = NavTransition()
                    ) {
                        val model = rememberSaveable { RecallSettingPage.RecallSettingPageModel(next = {
                            state.navigator.navigate("/tour/pluginConfigPage")
                        }) }
                        val page = rememberSaveable {
                            RecallSettingPage(model)
                        }
                        page.Main(Unit)
                    }
                    scene(
                        route = "/tour/pluginConfigPage",
                        navTransition = NavTransition(),
                    ) {
                        val model = rememberSaveable {
                            PluginConfigPage.PluginConfigModel() {
                                state.navigator.navigate("/home", options = NavOptions(popUpTo = PopUpTo.Prev))
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
                        val modelData: TimestampViewPage.TimestampViewPageModelData = GlobalNavDataExchangeCache.getAndDestroyData(modelDataId) as TimestampViewPage.TimestampViewPageModelData
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
                    if (ConfigManager.config.initialized) {
                        if (PluginManager.needJumpConfigUI.value || ConfigManager.config.initPlugin.not()) {
                            state.navigator.navigate("/tour/pluginConfigPage")
                        } else {
                            state.navigator.navigate("/home")
                        }
                    } else {
                        state.navigator.navigate("/tour/welcome")
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

    class AppMainPageModel: AbsUIModel<Any?, AppMainPageState, AppMainPageAction>() {
        @Composable
        override fun CreateState(params: Any?): AppMainPageState {
            return AppMainPageState(
                navigator = rememberNavigator(),
            )
        }

        override fun actionExecute(params: Any?, action: AppMainPageAction) {

        }
    }
}