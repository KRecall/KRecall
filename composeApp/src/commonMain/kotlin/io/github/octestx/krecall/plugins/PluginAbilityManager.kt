package io.github.octestx.krecall.plugins

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import io.github.octestx.basic.multiplatform.ui.SystemMessage
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.plugins.basic.PluginAbilityInterfaces
import io.github.octestx.krecall.plugins.basic.PluginBasic
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.klogging.noCoLogger
import moe.tlaster.precompose.navigation.RouteBuilder

object PluginAbilityManager {
    private val ologger = noCoLogger<PluginAbilityManager>()
    suspend fun sendToast(toastModel: ToastModel, dismissListener: () -> Unit) {
        toast.showBlocking(toastModel) {
            dismissListener()
        }
    }

    suspend fun sendMessage(metadata: PluginMetadata, text: String) {
        SystemMessage.sendNotification(Notification(
            "KRecall-Plugin: $${metadata.pluginId}",
            text
        ))
    }

    private val _drawerUIs = mutableStateMapOf<PluginMetadata, @Composable ColumnScope.() -> Unit>()
    private val drawerUIs: Map<PluginMetadata, @Composable ColumnScope.() -> Unit> = _drawerUIs
    private fun setDrawerUI(metadata: PluginMetadata, ui: @Composable ColumnScope.() -> Unit) {
        _drawerUIs[metadata] = ui
    }
    @Composable
    fun ShaderDrawer() {
        for ((_, ui) in drawerUIs) {
            Column {
                ui()
            }
        }
    }

    private val extMainTabs = mutableStateListOf<Pair<String, @Composable () -> Unit>>()
    private fun addExtMainTabs(tabName: String, ui: @Composable () -> Unit) {
        extMainTabs += (tabName to ui)
    }
    /**
     * @param startIndex 0-based
     */
    @Composable
    fun ShaderExtMainTabs(startIndex: Int, currentIndex: Int, changeIndex: (index: Int) -> Unit) {
        extMainTabs.onEachIndexed { index, (tabName, _) ->
            NavigationDrawerItem(
                label = { Text(text = tabName) },
                selected = currentIndex == (startIndex + index),
                onClick = { changeIndex(startIndex + index) },
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
    @Composable
    fun ShaderExtMainTab(startIndex: Int, index: Int) {
        extMainTabs[index - startIndex].second.invoke()
    }

    private val _extSettingTabs = mutableStateListOf<Pair<String, @Composable () -> Unit>>()
    private val extSettingTabs: List<Pair<String, @Composable () -> Unit>> = _extSettingTabs
    private fun addExtSettingTabs(tabName: String, content: @Composable () -> Unit) {
        _extSettingTabs += (tabName to content)
    }
    /**
     * @param startIndex 0-based
     */
    @Composable
    fun ShaderExtSettingTabs(startIndex: Int, currentIndex: Int, changeIndex: (index: Int) -> Unit) {
        extSettingTabs.onEachIndexed { index, (tabName, _) ->
            NavigationDrawerItem(
                label = { Text(text = tabName) },
                selected = currentIndex == (startIndex + index),
                onClick = { changeIndex(startIndex + index) },
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
    @Composable
    fun ShaderExtSettingTab(startIndex: Int, index: Int) {
        extSettingTabs[index - startIndex].second.invoke()
    }
    private val routes = mutableListOf<RouteBuilder.() -> Unit>()
    fun bindPluginRouter(routeBuilder: RouteBuilder) {
        routeBuilder.apply {
            for (route in routes) {
                route()
            }
        }
    }

    fun registerPlugin(plugin: PluginBasic) {
        if (plugin is PluginAbilityInterfaces.DrawerUI) {
            setDrawerUI(plugin.metadata) {
                plugin.DrawerUIShader()
            }
        }
        if(plugin is PluginAbilityInterfaces.MainTabUI) {
            ologger.info { "MainTabUI: ${plugin.mainTabName}" }
            addExtMainTabs(plugin.mainTabName) {
                plugin.MainTabUIShader()
            }
        }
        if (plugin is PluginAbilityInterfaces.SettingTabUI) {
            addExtSettingTabs(plugin.settingTabName) {
                plugin.SettingTabUIShader()
            }
        }
        if (plugin is PluginAbilityInterfaces.RoutersBuilder) {
            routes += {
                for (route in plugin.routers) {
                    //{/plugin/myPluginId/routes/myRoute}
                    val path =
                        "/plugin/${plugin.metadata.pluginId}/routes" + (
                                if (route.key.startsWith("/").not()) {
                                    "/${route.key}"
                                } else {
                                    route.key
                                }
                                )
                    val ui = route.value
                    scene(path) {
                        ui(it)
                    }
                }
            }
        }
    }
}