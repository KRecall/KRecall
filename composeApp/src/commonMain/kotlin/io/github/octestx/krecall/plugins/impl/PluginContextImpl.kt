package io.github.octestx.krecall.plugins.impl

import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.PluginAbility
import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.plugins.basic.WindowInfo
import io.github.octestx.krecall.plugins.basic.model.RecordData
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.utils.FlowListener
import io.klogging.logger
import models.sqld.DataItem

/**
 * 实现插件的各种能力
 */
class PluginContextImpl(metadata: PluginMetadata): PluginContext(metadata) {
    override val ability: PluginAbility = object : PluginAbility() {
        private val ologger = logger("${PluginAbility::class}<PluginId: ${metadata.pluginId}>")

        override fun addMark(timestamp: Long, mark: String) {
            DataDB.addMark(timestamp, mark)
        }

        override fun removeMark(timestamp: Long, mark: String) {
            DataDB.removeMark(timestamp, mark)
        }

        override fun addProcessingDataStateListener(listener: (Boolean) -> Unit) {
            FlowListener.addFlowListener(GlobalRecalling.processingData, listener)
        }

        override suspend fun getProcessingDataState(): Boolean {
            return GlobalRecalling.processingData.value
        }

        override suspend fun setProcessingDataState(state: Boolean) {
            GlobalRecalling.processingData.value = state
            ologger.info { "setProcessingDataState to $state" }
        }

        override fun addCollectingScreenStateListener(listener: (Boolean) -> Unit) {
            FlowListener.addFlowListener(GlobalRecalling.collectingScreen, listener)
        }

        override suspend fun getCollectingScreenState(): Boolean {
            return GlobalRecalling.collectingScreen.value
        }

        override suspend fun setCollectingScreenState(state: Boolean) {
            GlobalRecalling.collectingScreen.value = state
            ologger.info { "setCollectingScreenState to $state" }
        }


        override suspend fun getRecordDataByTimestamp(timestamp: Long): RecordData {
            return DataDB.getData(timestamp)?.toRecordData()?: throw IllegalArgumentException("cannot get RecordData")
        }

        override suspend fun getRecordScreenImageByTimestamp(timestamp: Long): ByteArray {
            return GlobalRecalling.getImageFromCache(timestamp) {
                PluginManager.getStoragePlugin().getOrNull()?.getScreenData(timestamp)?.getOrNull()
            }!!
        }

        override fun listTimestampWithMark(mark: String): List<Long> {
            return DataDB.listTimestampWithMark(mark)
        }

        override fun listTimestampWithNotMark(mark: String): List<Long> {
            return DataDB.listTimestampWithNotMark(mark)
        }

        override suspend fun navigateBack() {
            PluginAbilityManager.navigateBack()
        }

        override suspend fun navigateTo(route: String, vararg args: Pair<String, Any?>) {
            PluginAbilityManager.navigateTo(route, *args)
        }

        override suspend fun sendMessage(text: String) {
            PluginAbilityManager.sendMessage(metadata, text)
        }

        override suspend fun sendToast(toast: ToastModel, dismissListener: () -> Unit) {
            PluginAbilityManager.sendToast(toast, dismissListener)
        }
    }

    fun DataItem.toRecordData(): RecordData = RecordData(
        timestamp = timestamp,
        marks = mark.split("\n"),
        ocr = ocr?:"",
        data = data_?:"",
        status = status,
        error = error,
        windowInfo = WindowInfo(
            screenId = screenId.toInt(),
            appId = appId?:"",
            windowTitle = windowTitle?:""
        )
    )
}

