package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Settings
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.impl.PluginAbilityManager
import io.github.octestx.krecall.ui.TimestampViewPage
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi

class HomePage(model: HomePageModel): AbsUIPage<Any?, HomePage.HomePageState, HomePage.HomePageAction>(model) {
    private val ologger = noCoLogger<HomePage>()
    @OptIn(InternalResourceApi::class)
    @Composable
    override fun UI(state: HomePageState) {
//        BackHandler(enabled = drawerState.isOpen) {
//            scope.launch { drawerState.close() }
//        }
        ModalNavigationDrawer(
            drawerState = state.drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("KRecall", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
                    Button(
                        onClick = {
                            state.action(HomePageAction.Navigate("/setting"))
                        }, modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Row {
                            Icon(TablerIcons.Settings, contentDescription = "Settings")
                            Text("打开设置")
                        }
                    }
                    PluginAbilityManager.ShaderDrawer()
                    HorizontalDivider()
                    // Tab导航栏
                    NavigationDrawerItem(
                        label = { Text(text = "Home") },
                        selected = state.currentTabIndex == 0,
                        onClick = { state.action(HomePageAction.ChangeCurrentTabIndex(0)) },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "Search") },
                        selected = state.currentTabIndex == 1,
                        onClick = { state.action(HomePageAction.ChangeCurrentTabIndex(1)) },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    val count = GlobalRecalling.errorTimestampCount.collectAsState().value
                    NavigationDrawerItem(
                        label = {
                            AnimatedContent(count) {
                                if (count > 0) {
                                    Text("ViewProcessFails: $it")
                                } else {
                                    Text("No ViewProcessFails")
                                }
                            }
                        },
                        selected = state.currentTabIndex == 2,
                        onClick = {
                            if (count > 0) {
                                state.action(HomePageAction.ChangeCurrentTabIndex(2))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    PluginAbilityManager.ShaderExtMainTabs(
                        startIndex = 3,
                        currentIndex = state.currentTabIndex,
                        changeIndex = { state.action(HomePageAction.ChangeCurrentTabIndex(it)) }
                    )
                    // ...other drawer items
                }
            }
        ) {
            Column {
//                TabRow(selectedTabIndex = currentTabIndex) {
//                    Tab(
//                        selected = currentTabIndex == 0,
//                        onClick = {  }
//                    ) {
//                        Text("Home")
//                    }
//                    Tab(
//                        selected = currentTabIndex == 1,
//                        onClick = { currentTabIndex = 1 }
//                    ) {
//                        Text("Search")
//                    }
//                    val count = GlobalRecalling.errorTimestampCount.collectAsState().value
//                    Tab(
//                        selected = currentTabIndex == 2,
//                        onClick = { currentTabIndex = 2 },
//                        enabled = count > 0
//                    ) {
//                        AnimatedContent(count) {
//                            if (count > 0) {
//                                Text("ViewProcessFails: $it")
//                            } else {
//                                Text("No ViewProcessFails")
//                            }
//                        }
//                    }
//                }
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
                when (state.currentTabIndex) {
                    0 -> {
                        homeTab.Main(Unit)
                    }
                    1 -> {
                        searchTab.Main(Unit)
                    }
                    2 -> {
                        viewProcessFailsTab.Main(Unit)
                    }
                    else -> {
                        PluginAbilityManager.ShaderExtMainTab(
                            startIndex = 3,
                            index = state.currentTabIndex,
                        )
                    }
                }
            }
        }
    }

    sealed class HomePageAction : AbsUIAction() {
        data class Navigate(val route: String): HomePageAction()
        data class PutNavData(val key: String, val value: Any?): HomePageAction()
        data class ChangeCurrentTabIndex(val index: Int): HomePageAction()
    }
    data class HomePageState(
        val drawerState: DrawerState,
        val currentTabIndex: Int,
        val action: (HomePageAction) -> Unit,
    ): AbsUIState<HomePageAction>()

    class HomePageModel(
        private val navigate: (String) -> Unit,
        private val putNavData: (String, Any?) -> Unit
    ): AbsUIModel<Any?, HomePageState, HomePageAction>() {
        val ologger = noCoLogger<HomePageModel>()
        private val drawerState = DrawerState(DrawerValue.Closed) { true }
        private var currentTabIndex by mutableStateOf(0) // 当前选中Tab索引
        private lateinit var scope: CoroutineScope
        @Composable
        override fun CreateState(params: Any?): HomePageState {
            scope = rememberCoroutineScope()
            return HomePageState(drawerState, currentTabIndex) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: HomePageAction) {
            when(action) {
                is HomePageAction.Navigate -> navigate(action.route)
                is HomePageAction.PutNavData -> putNavData(action.key, action.value)
                is HomePageAction.ChangeCurrentTabIndex -> {
                    currentTabIndex = action.index
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        }
                    }
                }
            }
        }
    }
}