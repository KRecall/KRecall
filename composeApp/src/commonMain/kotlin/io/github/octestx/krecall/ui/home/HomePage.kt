package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.kotlin.fibonacci.ui.toast
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.developer_avatar
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.ui.TimestampViewPage
import io.klogging.noCoLogger
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.core.AbsUIPage

class HomePage(model: HomePageModel): AbsUIPage<Any?, HomePage.HomePageState, HomePage.HomePageAction>(model) {
    private val ologger = noCoLogger<HomePage>()
    @OptIn(InternalResourceApi::class)
    @Composable
    override fun UI(state: HomePageState) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
//        BackHandler(enabled = drawerState.isOpen) {
//            scope.launch { drawerState.close() }
//        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Column(Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)).padding(16.dp)) {
                    Text("KRecall")
                    Image(
                        painter = painterResource(Res.drawable.developer_avatar),
                        contentDescription = "App Logo",
                        modifier = Modifier.padding(16.dp).size(120.dp).clip(MaterialTheme.shapes.medium)
                    )
                    Text("Code by OCTest")
                    Button(onClick = {
                        ConfigManager.save(ConfigManager.config.copy(initPlugin = false))
                        toast.applyShow("重启生效")
                    }) {
                        Text("重新配置插件")
                    }
                }
            }
        ) {
            Column {
                // Tab导航栏
                var currentTabIndex by rememberSaveable { mutableStateOf(0) } // 当前选中Tab索引
                TabRow(selectedTabIndex = currentTabIndex) {
                    Tab(
                        selected = currentTabIndex == 0,
                        onClick = { currentTabIndex = 0 }
                    ) {
                        Text("Home")
                    }
                    Tab(
                        selected = currentTabIndex == 1,
                        onClick = { currentTabIndex = 1 }
                    ) {
                        Text("Search")
                    }
                    val count = GlobalRecalling.errorTimestampCount.collectAsState().value
                    Tab(
                        selected = currentTabIndex == 2,
                        onClick = { currentTabIndex = 2 },
                        enabled = count > 0
                    ) {
                        AnimatedContent(count) {
                            if (count > 0) {
                                Text("ViewProcessFails: $it")
                            } else {
                                Text("No ViewProcessFails")
                            }
                        }
                    }
                }
                val homeModel = rememberSaveable() { HomeTab.HomePageModel() }
                val homeTab = rememberSaveable() { HomeTab(homeModel) }

                val searchModel = rememberSaveable() { SearchTab.SearchPageModel(jumpView = { data, search ->
                    val modelData = TimestampViewPage.TimestampViewPageModelData(data, search)
                    val modelDataId = "Homepage-searchTab jump to timestampViewPage: modelData"
                    state.action(HomePageAction.PutNavData(modelDataId, modelData))
                    ologger.info { "SendModelDataId: $modelDataId" }
                    state.action(HomePageAction.Navigate("/timestampViewPage?modelDataId=$modelDataId"))
                }) }
                val searchTab = rememberSaveable() { SearchTab(searchModel) }

                val viewProcessFailsModel = rememberSaveable() { ViewProcessFailsTab.ViewProcessFailsPageModel(jumpView = { data, search ->
                    val modelData = TimestampViewPage.TimestampViewPageModelData(data, search)
                    val modelDataId = "Homepage-viewProcessFailsTab jump to timestampViewPage: modelData"
                    state.action(HomePageAction.PutNavData(modelDataId, modelData))
                    ologger.info { "SendModelDataId: $modelDataId" }
                    state.action(HomePageAction.Navigate("/timestampViewPage?modelDataId=$modelDataId"))
                }) }
                val viewProcessFailsTab = rememberSaveable() { ViewProcessFailsTab(viewProcessFailsModel) }
                // 内容区域
                when (currentTabIndex) {
                    0 -> {
                        homeTab.Main(Unit)
                    }
                    1 -> {
                        searchTab.Main(Unit)
                    }
                    2 -> {
                        viewProcessFailsTab.Main(Unit)
                    }
                }
            }
        }
    }

    sealed class HomePageAction : AbsUIAction() {
        data class Navigate(val route: String): HomePageAction()
        data class PutNavData(val key: String, val value: Any?): HomePageAction()
    }
    data class HomePageState(
        val action: (HomePageAction) -> Unit,
    ): AbsUIState<HomePageAction>()

    class HomePageModel(
        private val navigate: (String) -> Unit,
        private val putNavData: (String, Any?) -> Unit
    ): AbsUIModel<Any?, HomePageState, HomePageAction>() {
        val ologger = noCoLogger<HomePageModel>()
        @Composable
        override fun CreateState(params: Any?): HomePageState {
            return HomePageState() {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: HomePageAction) {
            when(action) {
                is HomePageAction.Navigate -> navigate(action.route)
                is HomePageAction.PutNavData -> putNavData(action.key, action.value)
            }
        }
    }
}