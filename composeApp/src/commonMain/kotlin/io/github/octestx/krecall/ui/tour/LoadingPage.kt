package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.klogging.noCoLogger

class LoadingPage(model: LoadingPageModel): AbsUIPage<Any?, LoadingPage.LoadingPageState, LoadingPage.LoadingPageAction>(model) {
    private val ologger = noCoLogger<LoadingPage>()
    @Composable
    override fun UI(state: LoadingPageState) {
        Column {
            CircularProgressIndicator()
        }
    }

    sealed class LoadingPageAction : AbsUIAction() {
    }
    data class LoadingPageState(
        val action: (LoadingPageAction) -> Unit,
    ): AbsUIState<LoadingPageAction>()

    class LoadingPageModel(): AbsUIModel<Any?, LoadingPageState, LoadingPageAction>() {
        val ologger = noCoLogger<LoadingPageModel>()
        @Composable
        override fun CreateState(params: Any?): LoadingPageState {
            return LoadingPageState() {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: LoadingPageAction) {
        }
    }
}