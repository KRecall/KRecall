package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.octestx.krecall.repository.ConfigManager
import io.klogging.noCoLogger
import ui.core.AbsUIPage
import kotlin.math.roundToInt

class RecallSettingPage(model: RecallSettingPageModel): AbsUIPage<Any?, RecallSettingPage.PluginConfigState, RecallSettingPage.RecallSettingPageAction>(model) {
    private val ologger = noCoLogger<RecallSettingPage>()
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun UI(state: PluginConfigState) {
        Column {
            Text("Collect screen delay: ${state.collectScreenDelay / 1000}s")
            Slider(
                state.collectScreenDelay,
                onValueChange = {
                    state.action(RecallSettingPageAction.ChangeCollectScreenDelay(it))
                },
                //1s to 1min
                valueRange = 1 * 1000f..600 * 1000f,
                steps = 600
            )
            Button(onClick = {
                state.action(RecallSettingPageAction.Next)
            }) {
                Text("Next")
            }
        }
    }

    sealed class RecallSettingPageAction : AbsUIAction() {
        data class ChangeCollectScreenDelay(val collectScreenDelay: Float): RecallSettingPageAction()
        //只有当插件全部初始化完毕后用户才能主动调用这个事件
        data object Next: RecallSettingPageAction()
    }
    data class PluginConfigState(
        val collectScreenDelay: Float,
        val action: (RecallSettingPageAction) -> Unit,
    ): AbsUIState<RecallSettingPageAction>()

    class RecallSettingPageModel(private val next: () -> Unit): AbsUIModel<Any?, PluginConfigState, RecallSettingPageAction>() {
        val ologger = noCoLogger<RecallSettingPageModel>()

        private var _collectScreenDelay by mutableStateOf(ConfigManager.config.collectScreenDelay.toFloat())
        @Composable
        override fun CreateState(params: Any?): PluginConfigState {
            return PluginConfigState(_collectScreenDelay) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: RecallSettingPageAction) {
            when(action) {
                RecallSettingPageAction.Next -> {
                    ConfigManager.save(ConfigManager.config.copy(collectScreenDelay = _collectScreenDelay.roundToInt().toLong()))
                    next()
                }

                is RecallSettingPageAction.ChangeCollectScreenDelay -> _collectScreenDelay = action.collectScreenDelay
            }
        }
    }
}