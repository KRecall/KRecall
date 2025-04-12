package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.exceptionSerializableOjson
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import models.sqld.DataItem
import ui.core.AbsUIPage

class ViewProcessFailsTab(model: ViewProcessFailsPageModel): AbsUIPage<Any?, ViewProcessFailsTab.ViewProcessFailsPageState, ViewProcessFailsTab.ViewProcessFailsPageAction>(model) {
    private val ologger = noCoLogger<ViewProcessFailsTab>()
    @Composable
    override fun UI(state: ViewProcessFailsPageState) {
        Column {
            Column {
                // ✅ 使用派生状态优化条件判断
                val showResultCount by remember {
                    derivedStateOf { state.failItems.isNotEmpty() }
                }
                AnimatedVisibility(showResultCount) {
                    Text("Search result: ${state.failItems.size}")
                }
            }
            Box {
                LazyVerticalGrid(GridCells.FixedSize(100.dp), state = state.lazyGridState) {
                    items(state.failItems, key = { it.timestamp }, contentType = { "DataItem" }) { item ->
                        Card(Modifier.padding(6.dp).clickable {
                            state.action(ViewProcessFailsPageAction.JumpView(item))
                        }) {
                            val timestamp = item.timestamp
                            var imgState: ImageState by rememberSaveable(timestamp) { mutableStateOf(ImageState.Loading) }
                            when (imgState) {
                                ImageState.Error -> {
                                    Text("ERROR!")
                                }
                                ImageState.Loading -> {
                                    CircularProgressIndicator()
                                }
                                is ImageState.Success -> {
                                    AsyncImage((imgState as ImageState.Success).bytes, null, contentScale = ContentScale.FillWidth)
                                }
                            }
                            LaunchedEffect(timestamp) {
                                if (imgState is ImageState.Success) {
                                    return@LaunchedEffect
                                }
                                if (GlobalRecalling.imageCache.containsKey(timestamp)) {
                                    imgState = ImageState.Success(GlobalRecalling.imageCache[timestamp]!!)
                                    return@LaunchedEffect
                                }

                                imgState = try {
                                    withContext(GlobalRecalling.imageLoadingDispatcher) {
                                        val bytes = GlobalRecalling.imageCache.getOrPut(timestamp) {
                                            PluginManager.getStoragePlugin().getOrNull()
                                                ?.getScreenData(timestamp)
                                                ?.getOrNull()
                                        }
                                        if (bytes == null) {
                                            ImageState.Error
                                        } else {
                                            ImageState.Success(bytes)
                                        }
                                    }
                                } catch (e: Exception) {
                                    ImageState.Error
                                }
                            }

                            // ✅ 使用派生状态减少文本计算
                            val displayText by remember(item.error) {
                                derivedStateOf {
                                    if (item.error == null) "NULL"
                                    else {
                                        val fail = exceptionSerializableOjson.decodeFromString<AIResult.Failed<String>>(item.error)
                                        "${fail.type.name} [${fail.type.message}]"
                                    }
                                }
                            }
                            Text(text = displayText, maxLines = 3)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd) // 右侧对齐
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f)), // 半透明背景
                    adapter = rememberScrollbarAdapter(
                        scrollState = state.lazyGridState,
                    )
                )
            }
        }
    }
    sealed class ViewProcessFailsPageAction : AbsUIAction() {
        data class JumpView(val dataItem: DataItem): ViewProcessFailsPageAction()
    }
    data class ViewProcessFailsPageState(
        val failItems: List<DataItem>,
        val lazyGridState: LazyGridState,
        val action: (ViewProcessFailsPageAction) -> Unit
    ): AbsUIState<ViewProcessFailsPageAction>()

    class ViewProcessFailsPageModel(private val jumpView: (data: DataItem, search: List<String>) -> Unit): AbsUIModel<Any?, ViewProcessFailsPageState, ViewProcessFailsPageAction>() {
        private val ioscope = CoroutineScope(Dispatchers.IO)
        private lateinit var uiscope: CoroutineScope
        val ologger = noCoLogger<ViewProcessFailsPageModel>()
        private val _failItems = mutableStateListOf<DataItem>()
        private val _lazyGridState = LazyGridState()
        private val _tags = mutableStateListOf<String>()

        @Composable
        override fun CreateState(params: Any?): ViewProcessFailsPageState {
            uiscope = rememberCoroutineScope()
            LaunchedEffect(GlobalRecalling.errorTimestampCount.collectAsState().value) {
                loadFailList()
            }
            return ViewProcessFailsPageState(_failItems, _lazyGridState) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: Any?, action: ViewProcessFailsPageAction) {
            when(action) {
                is ViewProcessFailsPageAction.JumpView -> jumpView(action.dataItem, listOf())
            }
        }
        private suspend fun loadFailList() {
            withContext(uiscope.coroutineContext) {
                _failItems.clear()
            }
            ologger.info { "Loading" }
            withContext(uiscope.coroutineContext) {
                _failItems.addAll(DataDB.listNotProcessedData().filter { it.status == 2L })
            }
            ologger.info { "Loaded: ${_failItems.size}" }
        }
    }
}