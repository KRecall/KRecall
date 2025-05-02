package io.github.octestx.krecall.ui.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBack
import io.github.octestx.basic.multiplatform.common.appDirs
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.ui.global.LocalLanguage
import io.github.octestx.basic.multiplatform.ui.ui.theme.ThemeRepository
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.EnhancedDropdownSelector
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.developer_avatar
import io.github.octestx.krecall.plugins.impl.PluginAbilityManager
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.system.exitProcess

class SettingPage(model: SettingPage.SettingPageModel): AbsUIPage<Any?, SettingPage.SettingPageActionState, SettingPage.SettingPageAction>(model) {
    private val ologger = noCoLogger<SettingPage>()
    @OptIn(InternalResourceApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun UI(state: SettingPageActionState) {
        Column {
            TopAppBar(title = {
                Text("KRecall", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }, navigationIcon = {
                IconButton(onClick = {
                    state.action(SettingPageAction.GoBack)
                }) {
                    Icon(TablerIcons.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
            })
            Row(modifier = Modifier.fillMaxSize()) {
                val theLastTabIndex = 1
                ModalDrawerSheet {
                    LazyColumn {
                        item {
                            // Tab导航栏
                            NavigationDrawerItem(
                                label = { Text(text = "Home") },
                                selected = state.currentTabIndex == 0,
                                onClick = { state.action(SettingPageAction.ChangeCurrentTabIndex(0)) },
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        item {
                            PluginAbilityManager.ShaderExtSettingTabs(theLastTabIndex, state.currentTabIndex) {
                                state.action(SettingPageAction.ChangeCurrentTabIndex(it))
                            }
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)).padding(6.dp).weight(1f)) {
                    item {
                        when (state.currentTabIndex) {
                            0 -> {
                                Row {
                                    Column {
                                        Image(
                                            painter = painterResource(Res.drawable.developer_avatar),
                                            contentDescription = "App Logo",
                                            modifier = Modifier.padding(16.dp).size(120.dp).clip(MaterialTheme.shapes.medium)
                                        )
                                        Text("Code by OCTest", color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(Modifier.fillMaxWidth()) {
                                        Column {
                                            SelectionContainer {
                                                Row {
                                                    Text("数据路径：")
                                                    Text(appDirs.getUserDataDir(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            Text("Language: ${LocalLanguage.current}")
                                        }
                                        Row {
                                            Button(
                                                onClick = {
                                                    ConfigManager.save(ConfigManager.config.copy(initPlugin = false))
                                                    toast.applyShow("重启生效")
                                                },
                                                modifier = Modifier.weight(1f).padding(12.dp)
                                            ) {
                                                Text("重新配置插件")
                                            }
                                            Button(
                                                onClick = {
                                                    exitProcess(0)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError),
                                                modifier = Modifier.weight(1f).padding(12.dp)
                                            ) {
                                                Text("终止KRecall进程", color = MaterialTheme.colorScheme.error, fontWeight = MaterialTheme.typography.bodyMedium.fontWeight)
                                            }
                                        }
                                        Row {
                                            val currentThemeKey = ThemeRepository.currentTheme.first
                                            val availableThemeKey = remember { ThemeRepository.allTheme.keys.toList() }
                                            var whenHoverTmpSaveResThemeKey: String? by rememberSaveable { mutableStateOf(null) }
                                            Text("选择主题", modifier = Modifier.align(Alignment.CenterVertically))
                                            Spacer(Modifier.width(12.dp))
                                            EnhancedDropdownSelector(
                                                availableThemeKey,
                                                selectedItem = if (whenHoverTmpSaveResThemeKey != null) whenHoverTmpSaveResThemeKey else currentThemeKey,
                                                onItemSelected = {
                                                    ThemeRepository.switchTheme(it)
                                                },
                                                onHover = {
                                                    if (whenHoverTmpSaveResThemeKey == null) {
                                                        whenHoverTmpSaveResThemeKey = it
                                                    }
                                                    if (currentThemeKey != it) {
                                                        ThemeRepository.switchTheme(it)
                                                    }
                                                },
                                                onExitHover = {
                                                    whenHoverTmpSaveResThemeKey?.let {
                                                        ThemeRepository.switchTheme(it)
                                                        whenHoverTmpSaveResThemeKey = null
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                PluginAbilityManager.ShaderExtSettingTab(theLastTabIndex, state.currentTabIndex)
                            }
                        }
                    }
                }
            }
        }
    }

    sealed class SettingPageAction : AbsUIAction() {
        data object GoBack: SettingPageAction()
        data class Navigate(val route: String): SettingPageAction()
        data class PutNavData(val key: String, val value: Any?): SettingPageAction()
        data class ChangeCurrentTabIndex(val index: Int) : SettingPageAction()
    }
    data class SettingPageActionState(
        val currentTabIndex: Int,
        val action: (SettingPageAction) -> Unit,
    ): AbsUIState<SettingPageAction>()

    class SettingPageModel(
        private val navigate: (String) -> Unit,
        private val putNavData: (String, Any?) -> Unit,
        private val goBack: () -> Unit
    ): AbsUIModel<Any?, SettingPageActionState, SettingPageAction>() {
        val ologger = noCoLogger<SettingPageModel>()
        private var currentTabIndex by mutableStateOf(0)
        @Composable
        override fun CreateState(params: Any?): SettingPageActionState {
            return SettingPageActionState(currentTabIndex) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: SettingPageAction) {
            when(action) {
                is SettingPageAction.Navigate -> navigate(action.route)
                is SettingPageAction.PutNavData -> putNavData(action.key, action.value)
                SettingPageAction.GoBack -> goBack()
                is SettingPageAction.ChangeCurrentTabIndex -> currentTabIndex = action.index
            }
        }
    }
}