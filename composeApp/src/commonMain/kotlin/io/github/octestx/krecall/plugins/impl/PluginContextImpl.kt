package io.github.octestx.krecall.plugins.impl

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.plugins.PluginAbilityManager
import io.github.octestx.krecall.plugins.basic.PluginAbility
import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.repository.DataDB

class PluginContextImpl(metadata: PluginMetadata): PluginContext(metadata) {

    override fun addMark(timestamp: Long, mark: String) {
        DataDB.addMark(timestamp, mark)
    }

    override fun removeMark(timestamp: Long, mark: String) {
        DataDB.removeMark(timestamp, mark)
    }

    override fun listTimestampWithMark(mark: String): List<Long> = DataDB.listTimestampWithMark(mark)

    override fun listTimestampWithNotMark(mark: String): List<Long> = DataDB.listTimestampWithNotMark(mark)

    override fun addCollectingScreenStateListener(listener: (Boolean) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun addProcessingDataStateListener(listener: (Boolean) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun getCollectingScreenState(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getProcessingDataState(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setCollectingScreenState(state: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun setProcessingDataState(state: Boolean) {
        TODO("Not yet implemented")
    }

    override val ability: PluginAbility = object : PluginAbility() {
        override fun setDrawerUI(content: @Composable ColumnScope.() -> Unit) {
            PluginAbilityManager.setDrawerUI(metadata, content)
        }
        override fun addMainTab(tabName: String, tabContent: @Composable () -> Unit) {
            PluginAbilityManager.addExtMainTabs(tabName, tabContent)
        }

        override fun addSettingTab(tabName: String, tabContent: @Composable () -> Unit) {
            PluginAbilityManager.addExtSettingTabs(tabName, tabContent)
        }

        override suspend fun sendMessage(text: String) {
            PluginAbilityManager.sendMessage(metadata, text)
        }

        override suspend fun sendToast(toast: ToastModel, dismissListener: () -> Unit) {
            PluginAbilityManager.sendToast(toast, dismissListener)
        }
    }
}

