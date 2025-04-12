package io.github.octestx.krecall.ui.home

import TimestampRateController
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import ui.core.AbsUIPage

class HomeTab(model: HomePageModel): AbsUIPage<Any?, HomeTab.HomePageState, HomeTab.HomePageAction>(model) {
    private val ologger = noCoLogger<HomeTab>()
    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI(state: HomePageState) {
        Column {
            CaptureScreenController(state)
            ProcessImageController(state)
            Row {
                Text("theNowMode")
                Switch(state.theNowMode, { state.action(HomePageAction.ChangeTheNowMode(!state.theNowMode)) })
            }
            Column(modifier = Modifier.verticalScroll(state.scrollState)) {
                if (state.selectedTimestampIndex >= 0) {
                    // 添加时间戳控制器
                    TimestampRateController(
                        timestamps = GlobalRecalling.allTimestamp,
                        currentIndex = state.selectedTimestampIndex,
                        theNowMode = state.theNowMode
                    ) {
                        state.action(HomePageAction.ChangeSelectedTimestampIndex(it))
                    }
                }
                state.currentImagePainter?.apply {
                    Image(
                        painter = this,
                        contentDescription = "Screen"
                    )
                }
                Text("Data: ${state.currentData}")
            }
        }
    }

    @Composable
    private fun CaptureScreenController(state: HomePageState) {
        val collectingScreenDelay by GlobalRecalling.collectingDelay.collectAsState()
        LinearProgressIndicator( progress = { (collectingScreenDelay.toDouble() / ConfigManager.config.collectScreenDelay).toFloat() })
        Row() {
            val collectingScreen by GlobalRecalling.collectingScreen.collectAsState()
            Text("CollectingScreen[${"%.1f".format(collectingScreenDelay.toDouble() / 1000)}s]")
            Switch(collectingScreen, { state.action(HomePageAction.ChangeCollectingScreen(!collectingScreen)) })
        }
    }

    @Composable
    private fun ProcessImageController(state: HomePageState) {
        Row {
            val processingData by GlobalRecalling.processingData.collectAsState()
            val processingDataCount = GlobalRecalling.processingDataList.count.collectAsState().value
            val text = if (processingDataCount > 0 ) {
                "ProcessingData[$processingDataCount]"
            } else {
                "ProcessingData"
            }
            Text(text)
            Switch(processingData, { state.action(HomePageAction.ChangeProcessingData(!processingData)) })
            if (processingData) {
                CircularProgressIndicator()
            }
        }
    }

    sealed class HomePageAction : AbsUIAction() {
        data class ChangeCollectingScreen(val collectingScreen: Boolean): HomePageAction()
        data class ChangeProcessingData(val processingData: Boolean): HomePageAction()
        data class ChangeTheNowMode(val theNowMode: Boolean): HomePageAction()
        data class ChangeSelectedTimestampIndex(val selectedTimestampIndex: Int): HomePageAction()
    }
    data class HomePageState(
        val theNowMode: Boolean,
        val scrollState: ScrollState,
        val selectedTimestampIndex: Int,
        val currentImagePainter: Painter?,
        val currentData: String,
        val action: (HomePageAction) -> Unit
    ): AbsUIState<HomePageAction>()

    class HomePageModel: AbsUIModel<Any?, HomePageState, HomePageAction>() {
        val ologger = noCoLogger<HomePageModel>()

        private var _theNowMode by mutableStateOf(true)
        private val _scrollState = ScrollState(0)
        private var _selectedTimestampIndex by mutableStateOf(GlobalRecalling.allTimestamp.lastIndex)
        private var _currentImagePainter: Painter? by mutableStateOf(null)
        private var _currentData by mutableStateOf("")

        init {
            GlobalRecalling.init()
        }

        @OptIn(ExperimentalResourceApi::class)
        @Composable
        override fun CreateState(params: Any?): HomePageState {
            TraceRealtime()
            return HomePageState(_theNowMode, _scrollState, _selectedTimestampIndex, _currentImagePainter, _currentData) {
                actionExecute(params, it)
            }
        }
        @OptIn(ExperimentalResourceApi::class)
        @Composable
        private fun TraceRealtime() {
            LaunchedEffect(_selectedTimestampIndex) {
                while (_selectedTimestampIndex < 0) {
                    _selectedTimestampIndex = GlobalRecalling.allTimestamp.lastIndex
                    delay(350)
                }
                //Realtime update current data
                while (_currentData.isEmpty()) {
                    PluginManager.getStoragePlugin().onSuccess { storagePlugin ->
                        _currentData = DataDB.getData(GlobalRecalling.allTimestamp[_selectedTimestampIndex])?.data_ ?: ""
                        storagePlugin.getScreenData(GlobalRecalling.allTimestamp[_selectedTimestampIndex])
                            .onSuccess {
                                val img = it.decodeToImageBitmap()
                                _currentImagePainter = BitmapPainter(img)
                            }
                    }
                    delay(1000)
                }
            }


            if (_selectedTimestampIndex >= 0) {
                //Realtime update current data
                LaunchedEffect(_selectedTimestampIndex) {
                    while (_currentData.isEmpty()) {
                        PluginManager.getStoragePlugin().onSuccess { storagePlugin ->
                            _currentData = DataDB.getData(GlobalRecalling.allTimestamp[_selectedTimestampIndex])?.data_ ?: ""
                            storagePlugin.getScreenData(GlobalRecalling.allTimestamp[_selectedTimestampIndex])
                                .onSuccess {
                                    val img = it.decodeToImageBitmap()
                                    _currentImagePainter = BitmapPainter(img)
                                }
                        }
                        delay(1000)
                    }
                }
            } else {
                _selectedTimestampIndex = GlobalRecalling.allTimestamp.lastIndex
                if (_selectedTimestampIndex >= 0) {
                    TraceRealtime()
                }
            }
        }
        override fun actionExecute(params: Any?, action: HomePageAction) {
            when(action) {
                is HomePageAction.ChangeCollectingScreen -> GlobalRecalling.collectingScreen.value = action.collectingScreen
                is HomePageAction.ChangeProcessingData -> GlobalRecalling.processingData.value = action.processingData
                is HomePageAction.ChangeTheNowMode -> _theNowMode = action.theNowMode
                is HomePageAction.ChangeSelectedTimestampIndex -> {
                    if (_selectedTimestampIndex != action.selectedTimestampIndex) {
                        _selectedTimestampIndex = action.selectedTimestampIndex
                        _currentData = ""
                    }
                }
            }
        }
    }
}