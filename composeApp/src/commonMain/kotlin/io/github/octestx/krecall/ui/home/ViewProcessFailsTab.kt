package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.sqld.DataItem

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
                            var imageState: ImageState by rememberSaveable(timestamp) { mutableStateOf(ImageState.Loading) }
                            AnimatedContent(imageState) { imgState ->
                                when (imgState) {
                                    is ImageState.Error -> {
                                        Text("ERROR!")
                                        LaunchedEffect(imgState) {
                                            toast.applyShow("ViewProcessFailsTab图片无法渲染: ${imgState.cause.message}", type = ToastModel.Type.Error)
                                        }
                                    }
                                    ImageState.Loading -> {
                                        CircularProgressIndicator()
                                    }
                                    is ImageState.Success -> {
                                        CoilZoomAsyncImage(imgState.bytes, null, contentScale = ContentScale.FillWidth)
                                    }
                                }
                            }
                            LaunchedEffect(timestamp) {
                                if (imageState is ImageState.Success) {
                                    return@LaunchedEffect
                                }
                                imageState = try {
                                    val bytes = GlobalRecalling.getImageFromCache(timestamp) {
                                        PluginManager.getStoragePlugin().getOrNull()
                                            ?.getScreenData(timestamp)
                                            ?.getOrNull()
                                    }
                                    if (bytes == null) {
                                        ologger.error(NullPointerException("getImageFromCache不存在数据"))
                                        ImageState.Error(NullPointerException("getImageFromCache不存在数据"))
                                    } else {
                                        ImageState.Success(bytes)
                                    }
                                } catch (e: Exception) {
                                    ologger.error(e)
                                    ImageState.Error(e)
                                }
                            }

                            // ✅ 使用派生状态减少文本计算
                            val displayText by remember(item.error) {
                                derivedStateOf {
                                    if (item.error == null) "NULL"
                                    else {
//                                        val fail = exceptionSerializableOjson.decodeFromString<AIResult.Failed<String>>(item.error)
//                                        "${fail.type.name} [${fail.type.message}]"
                                        item.error
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