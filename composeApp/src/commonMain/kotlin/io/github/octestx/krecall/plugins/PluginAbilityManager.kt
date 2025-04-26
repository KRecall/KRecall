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
import io.github.octestx.krecall.plugins.basic.PluginMetadata

object PluginAbilityManager {
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
    fun setDrawerUI(metadata: PluginMetadata, ui: @Composable ColumnScope.() -> Unit) {
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

    private val _extMainTabs = mutableStateListOf<Pair<String, @Composable () -> Unit>>()
    private val extMainTabs: List<Pair<String, @Composable () -> Unit>> = _extMainTabs
    fun addExtMainTabs(tabName: String, ui: @Composable () -> Unit) {
        _extMainTabs += (tabName to ui)
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
    fun addExtSettingTabs(tabName: String, ui: @Composable () -> Unit) {
        _extSettingTabs += (tabName to ui)
    }
    /**
     * @param startIndex 0-based
     */
    @Composable
    fun ShaderExtSettingTabs(startIndex: Int, currentIndex: Int, changeIndex: (index: Int) -> Unit) {
        extSettingTabs.onEachIndexed { index, (tabName, ui) ->
            NavigationDrawerItem(
                label = { Text(text = tabName) },
                selected = currentIndex == (startIndex + index),
                onClick = { changeIndex(startIndex + index) },
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
        TODO()
    }
    @Composable
    fun ShaderExtSettingTab(startIndex: Int, index: Int) {
        extSettingTabs[index - startIndex].second.invoke()
        TODO()
    }
}