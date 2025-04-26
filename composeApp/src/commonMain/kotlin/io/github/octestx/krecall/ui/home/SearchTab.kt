package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.Send
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.ui.toast
import io.github.octestx.basic.multiplatform.ui.ui.utils.DelayShowAnimation
import io.github.octestx.basic.multiplatform.ui.ui.utils.StepLoadAnimation
import io.github.octestx.basic.multiplatform.ui.ui.utils.ToastModel
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.github.octestx.krecall.utils.WaitingFlow
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import models.sqld.DataItem

class SearchTab(model: SearchPageModel): AbsUIPage<Any?, SearchTab.SearchPageState, SearchTab.SearchPageAction>(model) {
    private val ologger = noCoLogger<SearchTab>()
    val waitingFlow = WaitingFlow()
    @Composable
    override fun UI(state: SearchPageState) {
        MaterialTheme {
            StepLoadAnimation(4) { step ->
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Column {
                        // ✅ 使用派生状态优化条件判断
                        val showResultCount by remember {
                            derivedStateOf { state.searchResult.isNotEmpty() }
                        }
                        AnimatedVisibility(showResultCount) {
                            Text("Search result: ${state.searchResult.size}", modifier = Modifier.padding(8.dp))
                        }
                        SearchBar(step, state)

                        SearchTagsController(step, state)
                    }
                    Row(Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            GridCells.FixedSize(100.dp),
                            state = state.lazyGridState,
                            modifier = Modifier.weight(1f)
                        ) {
                            items(
                                state.searchResult,
                                key = { it.timestamp },
                                contentType = { "DataItem" }
                            ) { item ->
                                SearchItem(state, item)
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(Color.LightGray.copy(alpha = 0.5f)) // 半透明背景
                            ,
                            adapter = rememberScrollbarAdapter(
                                scrollState = state.lazyGridState,
                            )
                        )
                    }
                }
            }
        }
    }
    @Composable
    private fun SearchTagsController(step: Int, state: SearchPageState) {
        if (state.tags.isNotEmpty()) {
            Column(modifier = Modifier.height(56.dp)) { // 限制高度避免内容溢出
                val lazyListState = rememberLazyListState()
                if (step >= 3) {
                    LazyRow(
                        state = lazyListState, // 绑定滚动状态
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        items(state.tags) { tag ->
                            ElevatedFilterChip(
                                selected = false,
                                onClick = { state.action(SearchPageAction.RemoveTag(tag)) },
                                label = { Text(text = tag) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                if (step >= 4) {
                    HorizontalScrollbar(
                        modifier = Modifier
                            .fillMaxWidth(),
                        adapter = rememberScrollbarAdapter(lazyListState),
                    )
                }
            }
        }
    }

    @Composable
    private fun SearchBar(step: Int, state: SearchPageState) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            if (step >= 1) {
                OutlinedTextField(
                    value = state.searchText,
                    onValueChange = { state.action(SearchPageAction.ChangeSearchText(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (step >= 2) {
                DelayShowAnimation {
                    IconButton(onClick = {
                        if (state.searchText.isNotBlank()) {
                            state.action(SearchPageAction.AddTag(state.searchText))
                            state.action(SearchPageAction.ChangeSearchText("")) // 清空输入
                        }
                    }, modifier = Modifier.align(Alignment.CenterVertically)) {
                        Icon(TablerIcons.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchItem(state: SearchPageState, item: DataItem) {
        Card(Modifier.padding(6.dp).clickable {
            state.action(SearchPageAction.JumpView(item))
        }) {
            val timestamp = item.timestamp
            var imgState: ImageState by remember(timestamp) {
                mutableStateOf(
                    ImageState.Loading
                )
            }
            var show by remember(timestamp) {
                mutableStateOf(false)
            }
            AnimatedVisibility(show) {
                AnimatedContent(imgState) { imageState ->
                    when (imageState) {
                        is ImageState.Error -> {
                            Text("ERROR!", color = MaterialTheme.colorScheme.error)
                            LaunchedEffect(imageState) {
                                toast.applyShow("SearchTab图片无法渲染: ${imageState.cause.message}", type = ToastModel.Type.Error)
                            }
                        }
                        ImageState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is ImageState.Success -> {
                            AsyncImage(
                                imageState.bytes,
                                null,
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
            // ✅ 使用派生状态减少文本计算
            val displayText by remember(item.data_) {
                derivedStateOf { item.data_?.take(150) ?: "NULL" }
            }
            Text(text = displayText, maxLines = 3)
            LaunchedEffect(timestamp) {
                waitingFlow.wait()
                show = true
            }
            LaunchedEffect(timestamp) {
                if (imgState is ImageState.Success) {
                    return@LaunchedEffect
                }

                imgState = try {
                    val bytes = GlobalRecalling.getImageFromCache(timestamp) {
                        PluginManager.getStoragePlugin().getOrNull()
                            ?.getScreenData(timestamp)
                            ?.getOrNull()
                    }
                    if (bytes == null) {
                        ologger.error(NullPointerException("getImageFromCache不存在数据[timestamp=$timestamp]"))
                        ImageState.Error(NullPointerException("getImageFromCache不存在数据[timestamp=$timestamp]"))
                    } else {
                        ImageState.Success(bytes)
                    }
                } catch (e: Exception) {
                    ologger.error(e)
                    ImageState.Error(e)
                }
            }
        }
    }
    sealed class SearchPageAction : AbsUIAction() {
        data class ChangeSearchText(val newText: String): SearchPageAction()
        data class JumpView(val dataItem: DataItem): SearchPageAction()
        data class AddTag(val tag: String): SearchPageAction()
        data class RemoveTag(val tag: String): SearchPageAction()
    }
    data class SearchPageState(
        val searchText: String,
        val searchResult: List<DataItem>,
        val lazyGridState: LazyGridState,
        val tags: List<String>,
        val action: (SearchPageAction) -> Unit
    ): AbsUIState<SearchPageAction>()

    class SearchPageModel(private val jumpView: (data: DataItem, search: List<String>) -> Unit): AbsUIModel<Any?, SearchPageState, SearchPageAction>() {
        private val ioscope = CoroutineScope(Dispatchers.IO)
        private lateinit var uiscope: CoroutineScope
        val ologger = noCoLogger<SearchPageModel>()
        private var _searchText by mutableStateOf("")
        private val _searchResultList = mutableStateListOf<DataItem>()
        private val _lazyGridState = LazyGridState()
        private val _tags = mutableStateListOf<String>()

        @Composable
        override fun CreateState(params: Any?): SearchPageState {
            uiscope = rememberCoroutineScope()
            return SearchPageState(_searchText, _searchResultList, _lazyGridState, _tags) {
                actionExecute(params, it)
            }
        }
        private var changingTextFieldJob: Job? = null
        override fun actionExecute(params: Any?, action: SearchPageAction) {
            when(action) {
                is SearchPageAction.ChangeSearchText -> {
                    _searchText = action.newText
                    changingTextFieldJob?.cancel()
                    changingTextFieldJob = ioscope.launch {
                        delay(100)
                        search(action.newText, _tags)
                    }
                }
                is SearchPageAction.JumpView -> jumpView(action.dataItem, getSearchTags(_searchText, _tags))
                is SearchPageAction.AddTag -> {
                    if (_tags.contains(action.tag)) {
                        return
                    }
                    _tags.add(action.tag)
                    ioscope.launch {
                        search(_searchText, _tags)
                    }
                }
                is SearchPageAction.RemoveTag -> {
                    if (!_tags.contains(action.tag)) {
                        return
                    }
                    _tags.remove(action.tag)
                    ioscope.launch {
                        search(_searchText, _tags)
                    }
                }
            }
        }
        private fun getSearchTags(text: String, tags: List<String>): List<String> {
            if (text.isBlank() && tags.isEmpty()) {
                return listOf()
            }
            return if (text.isBlank()) {
                tags
            } else {
                listOf(text, *tags.toTypedArray())
            }
        }

        private suspend fun search(text: String, tags: List<String>) {
            withContext(uiscope.coroutineContext) {
                _searchResultList.clear()
            }
            val list = getSearchTags(text, tags)
            if (list.isEmpty()) {
                return
            }
            ologger.info { "Searching: $list" }
            withContext(uiscope.coroutineContext) {
                _searchResultList.addAll(DataDB.searchDataInAll(list))
            }
            ologger.info { "Searched: ${_searchResultList.size}" }
        }
    }
}