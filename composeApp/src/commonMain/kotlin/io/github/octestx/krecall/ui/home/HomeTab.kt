package io.github.octestx.krecall.ui.home

import TimestampRateController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.github.panpf.zoomimage.ZoomImage
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.ui.utils.DelayShowAnimation
import io.github.octestx.basic.multiplatform.ui.ui.utils.StepLoadAnimation
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.ConfigManager
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.ui.utils.FloatingBlock
import io.klogging.noCoLogger
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

class HomeTab(model: HomePageModel): AbsUIPage<Any?, HomeTab.HomePageState, HomeTab.HomePageAction>(model) {
    private val ologger = noCoLogger<HomeTab>()
    @Composable
    override fun UI(state: HomePageState) {
        MaterialTheme {
            StepLoadAnimation(5) { step ->
                Row(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Column(Modifier.weight(1f).verticalScroll(state.scrollState)) {
                        FloatingBlock(Modifier.fillMaxWidth().padding(5.dp)) {
                            Row {
                                if (step >= 1) {
                                    DelayShowAnimation {
                                        CaptureScreenController(state)
                                        VerticalDivider()
//                                        Spacer(Modifier.padding(12.dp).height(6.dp).background(MaterialTheme.colorScheme.onBackground).align(Alignment.CenterVertically))
                                    }
                                }
                                if (step >= 2) {
                                    DelayShowAnimation {
                                        ProcessImageController(state)
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(5.dp)) {
                            if (state.selectedTimestampIndex >= 0 && step >= 3) {
                                // 添加时间戳控制器
                                DelayShowAnimation {
                                    TimestampRateController(
                                        timestamps = GlobalRecalling.allTimestamp,
                                        currentIndex = state.selectedTimestampIndex,
                                        theNowMode = state.theNowMode,
                                        changeTheNowMode = { state.action(HomePageAction.ChangeTheNowMode(it))},
                                        modifier = Modifier.border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary), shape = MaterialTheme.shapes.medium).padding(5.dp)
                                    ) {
                                        state.action(HomePageAction.ChangeSelectedTimestampIndex(it))
                                    }
                                }
                            }
                            state.currentImagePainter?.apply {
                                Spacer(Modifier.height(5.dp))
                                if (step >= 4) {
                                    DelayShowAnimation {
                                        ZoomImage(
                                            painter = this,
                                            contentDescription = "Screen",
                                            modifier = Modifier.border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)).padding(5.dp).fillMaxWidth().aspectRatio(state.aspectRatio)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            if (step >= 5) {
                                Column(modifier = Modifier.fillMaxSize().border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary), shape = MaterialTheme.shapes.medium).padding(5.dp)) {
                                    Text("Data:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    SelectionContainer {
                                        Text(state.currentData, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(state.scrollState),
//                style = ScrollbarStyle(
//                    thickness = 8.dp,
//                    hoverDurationMillis = 300,
//                    unhoverColor = Color.LightGray,
//                    hoverColor = Color.Gray
//                )
                    )
                }
            }
        }
    }

    @Composable
    private fun CaptureScreenController(state: HomePageState) {
        val collectingScreenDelay by GlobalRecalling.collectingDelay.collectAsState()
        Row() {
            AnimatedVisibility(GlobalRecalling.collectingScreen.collectAsState().value) {
                Box() {
                    CircularProgressIndicator(progress = { (collectingScreenDelay.toDouble() / ConfigManager.config.collectScreenDelay).toFloat() }, modifier = Modifier.padding(5.dp))
                    Text("${"%.1f".format(collectingScreenDelay.toDouble() / 1000)}s", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            val collectingScreen by GlobalRecalling.collectingScreen.collectAsState()
            Text("获取信息", modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.typography.bodyMedium.color)
            Switch(collectingScreen, { state.action(HomePageAction.ChangeCollectingScreen(!collectingScreen)) })
        }
    }

    @Composable
    private fun ProcessImageController(state: HomePageState) {
        Row {
            val processingData by GlobalRecalling.processingData.collectAsState()
            val processingDataCount = GlobalRecalling.processingDataList.count.collectAsState().value
            Box {
                if (processingData) {
                    CircularProgressIndicator(modifier = Modifier.padding(5.dp))
                }
                Text(processingDataCount.toString(), modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Text("处理数据", modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall)
            Switch(processingData, { state.action(HomePageAction.ChangeProcessingData(!processingData)) })
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
        val aspectRatio: Float,
        val currentData: String,
        val action: (HomePageAction) -> Unit
    ): AbsUIState<HomePageAction>()

    class HomePageModel: AbsUIModel<Any?, HomePageState, HomePageAction>() {
        val ologger = noCoLogger<HomePageModel>()

        private var theNowMode by mutableStateOf(true)
        private val scrollState = ScrollState(0)
        private var selectedTimestampIndex by mutableStateOf(GlobalRecalling.allTimestamp.lastIndex)
        private var currentImagePainter: Painter? by mutableStateOf(null)
        private var currentImageAspectRatio by mutableStateOf(1f)
        private var currentData by mutableStateOf("")

        init {
            GlobalRecalling.init()
        }

        @OptIn(ExperimentalResourceApi::class)
        @Composable
        override fun CreateState(params: Any?): HomePageState {
            TraceRealtime()
            return HomePageState(theNowMode, scrollState, selectedTimestampIndex, currentImagePainter, currentImageAspectRatio, currentData) {
                actionExecute(params, it)
            }
        }
        @OptIn(ExperimentalResourceApi::class)
        @Composable
        private fun TraceRealtime() {
            var preTimestampIndex = -1
            LaunchedEffect(selectedTimestampIndex) {
                while (selectedTimestampIndex < 0) {
                    val leastTimestampIndex = GlobalRecalling.allTimestamp.lastIndex
                    if (preTimestampIndex != leastTimestampIndex) {
                        selectedTimestampIndex = leastTimestampIndex
                        preTimestampIndex = leastTimestampIndex
                    }
                    delay(350)
                }
                //Realtime update current data
                while (currentData.isEmpty()) {
                    PluginManager.getStoragePlugin().onSuccess { storagePlugin ->
                        currentData = DataDB.getData(GlobalRecalling.allTimestamp[selectedTimestampIndex])?.data_ ?: ""
                        storagePlugin.getScreenData(GlobalRecalling.allTimestamp[selectedTimestampIndex])
                            .onSuccess {
                                val img = it.decodeToImageBitmap()
                                currentImageAspectRatio = (img.width.toDouble() / img.height.toDouble()).toFloat()
                                currentImagePainter = BitmapPainter(img)
                            }
                    }
                    delay(1000)
                }
            }


            if (selectedTimestampIndex >= 0) {
                //Realtime update current data
                LaunchedEffect(selectedTimestampIndex) {
                    while (currentData.isEmpty()) {
                        PluginManager.getStoragePlugin().onSuccess { storagePlugin ->
                            currentData = DataDB.getData(GlobalRecalling.allTimestamp[selectedTimestampIndex])?.data_ ?: ""
                            storagePlugin.getScreenData(GlobalRecalling.allTimestamp[selectedTimestampIndex])
                                .onSuccess {
                                    val img = it.decodeToImageBitmap()
                                    currentImagePainter = BitmapPainter(img)
                                }
                        }
                        delay(1000)
                    }
                }
            } else {
                selectedTimestampIndex = GlobalRecalling.allTimestamp.lastIndex
                if (selectedTimestampIndex >= 0) {
                    TraceRealtime()
                }
            }
        }
        override fun actionExecute(params: Any?, action: HomePageAction) {
            when(action) {
                is HomePageAction.ChangeCollectingScreen -> GlobalRecalling.collectingScreen.value = action.collectingScreen
                is HomePageAction.ChangeProcessingData -> GlobalRecalling.processingData.value = action.processingData
                is HomePageAction.ChangeTheNowMode -> theNowMode = action.theNowMode
                is HomePageAction.ChangeSelectedTimestampIndex -> {
                    if (selectedTimestampIndex != action.selectedTimestampIndex) {
                        selectedTimestampIndex = action.selectedTimestampIndex
                        currentData = ""
                    }
                }
            }
        }
    }
}