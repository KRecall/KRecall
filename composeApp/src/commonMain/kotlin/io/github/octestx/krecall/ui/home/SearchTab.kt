package io.github.octestx.krecall.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.Send
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.repository.DataDB
import io.klogging.noCoLogger
import kotlinx.coroutines.*
import models.sqld.DataItem
import ui.core.AbsUIPage

class SearchTab(model: SearchPageModel): AbsUIPage<Any?, SearchTab.SearchPageState, SearchTab.SearchPageAction>(model) {
    private val ologger = noCoLogger<SearchTab>()
    @Composable
    override fun UI(state: SearchPageState) {
        Column {
            Column {
                // ✅ 使用派生状态优化条件判断
                val showResultCount by remember {
                    derivedStateOf { state.searchResult.isNotEmpty() }
                }
                AnimatedVisibility(showResultCount) {
                    Text("Search result: ${state.searchResult.size}")
                }
                Row {
                    OutlinedTextField(
                        value = state.searchText,
                        onValueChange = { state.action(SearchPageAction.ChangeSearchText(it)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (state.searchText.isNotBlank()) {
                            state.action(SearchPageAction.AddTag(state.searchText))
                            state.action(SearchPageAction.ChangeSearchText("")) // 清空输入
                        }
                    }) {
                        Icon(TablerIcons.Send, null)
                    }
                }

                Box(modifier = Modifier.height(56.dp)) { // 限制高度避免内容溢出
                    val lazyListState = rememberLazyListState()

                    HorizontalScrollbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.9f) // 宽度略小于内容区
                            .height(8.dp)
                            .background(Color.Transparent),
                        adapter = rememberScrollbarAdapter(lazyListState),
                        style = ScrollbarStyle(
                            minimalHeight = 16.dp,
                            thickness = 8.dp,
                            shape = RoundedCornerShape(4.dp),
                            hoverDurationMillis = 300,
                            unhoverColor = Color.Gray.copy(alpha = 0.4f),
                            hoverColor = Color.DarkGray
                        )
                    )

                    LazyRow(
                        state = lazyListState, // 绑定滚动状态
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp) // 给滚动条留出空间
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
            }
            Box {
                LazyVerticalGrid(GridCells.FixedSize(100.dp), state = state.lazyGridState) {
                    items(state.searchResult, key = { it.timestamp }, contentType = { "DataItem" }) { item ->
                        Card(Modifier.padding(6.dp).clickable {
                            state.action(SearchPageAction.JumpView(item))
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
                            val displayText by remember(item.data_) {
                                derivedStateOf { item.data_?.take(150) ?: "NULL" }
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