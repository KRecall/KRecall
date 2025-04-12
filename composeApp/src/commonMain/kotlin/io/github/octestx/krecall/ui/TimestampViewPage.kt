package io.github.octestx.krecall.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBack
import compose.icons.tablericons.Download
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.exceptionSerializableOjson
import io.github.octestx.krecall.ui.TimestampViewPage.TimestampViewPageModelData
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import io.klogging.noCoLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.sqld.DataItem
import ui.core.AbsUIPage

class TimestampViewPage(model: TimestampViewPageModel): AbsUIPage<TimestampViewPageModelData, TimestampViewPage.TimestampViewPageState, TimestampViewPage.TimestampViewPageAction>(model) {
    private val ologger = noCoLogger<TimestampViewPage>()
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun UI(state: TimestampViewPageState) {
        Column {
            TopAppBar(title = {
                Text(text = "Timestamp: ${state.dataItem.timestamp}")
            }, navigationIcon = {
                IconButton(onClick = {
                    state.action(TimestampViewPageAction.GoBack)
                }) {
                    Icon(TablerIcons.ArrowBack, null)
                }
            })
            Box(
                modifier = Modifier
                    .fillMaxSize()
//                    .pointerInput(Unit) {
//                        detectTapGestures(
//                            onPress = { /* 处理按压 */ },
//                            onDoubleTap = { /* 处理双击 */ }
//                        )
//                    }
            ) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    when (state.imageState) {
                        ImageState.Error -> {
                            Text("ERROR!")
                        }
                        ImageState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is ImageState.Success -> {
                            AsyncImage(state.imageState.bytes, null, contentScale = ContentScale.FillWidth)
                            Row {
                                val launcher = rememberFileSaverLauncher { file ->
                                    // Write your data to the file
                                    if (file != null) {
                                        scope.launch {
                                            file.write(state.imageState.bytes)
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    launcher.launch("output-KRecallScreen-${state.dataItem.screenId}_${System.nanoTime()}", "png")
                                }) {
                                    Icon(TablerIcons.Download, null)
                                }
                            }
                        }
                    }
                    if (state.dataItem.status == 0L || state.dataItem.status == 1L) {
                        BasicText(
                            text = highlightText(
                                text = state.dataItem.data_ ?: "NULL",
                                highlights = state.highlights
                            ),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (state.dataItem.status == 2L) {
                        val err = exceptionSerializableOjson.decodeFromString<AIResult.Failed<String>>(state.dataItem.error!!)
                        SelectionContainer {
                            BasicText(
                                text = err.toString(),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Red),
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
//                        .alpha(if (isScrollVisible) 1f else 0.5f) // 透明度变化
                        .animateContentSize() // 尺寸动画
                )
            }
        }
    }
    private fun highlightText(text: String, highlights: List<String>): AnnotatedString {
        return buildAnnotatedString {
            if (highlights.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            // 预处理：收集所有匹配区间（不区分大小写）
            val ranges = mutableListOf<IntRange>()
            val lowerText = text.lowercase()

            highlights.forEach { keyword ->
                val lowerKeyword = keyword.lowercase()
                var startIndex = 0

                while (startIndex < text.length) {
                    val index = lowerText.indexOf(lowerKeyword, startIndex)
                    if (index == -1) break

                    ranges.add(index until (index + keyword.length))
                    startIndex = index + 1
                }
            }

            // 合并重叠/相邻的区间
            val mergedRanges = ranges.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { acc, range ->
                if (acc.isEmpty()) {
                    acc.add(range)
                } else {
                    val last = acc.last()
                    if (range.first <= last.last) {
                        acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
                    } else {
                        acc.add(range)
                    }
                }
                acc
            }

            // 构建带高亮的文本
            var lastPos = 0
            mergedRanges.forEach { range ->
                // 添加非高亮部分
                if (range.first > lastPos) {
                    append(text.substring(lastPos, range.first))
                }

                // 添加高亮部分
                withStyle(SpanStyle(background = Color.Yellow.copy(alpha = 0.4f))) {
                    append(text.substring(range))
                }

                lastPos = range.last
            }

            // 添加剩余部分
            if (lastPos < text.length) {
                append(text.substring(lastPos, text.length))
            }
        }
    }

    sealed class TimestampViewPageAction : AbsUIAction() {
        data object GoBack: TimestampViewPageAction()
    }
    data class TimestampViewPageState(
        val dataItem: DataItem,
        val highlights: List<String>,
        val imageState: ImageState,
        val action: (TimestampViewPageAction) -> Unit
    ): AbsUIState<TimestampViewPageAction>()

    data class TimestampViewPageModelData(
        val dataItem: DataItem,
        val highlights: List<String>
    )

    class TimestampViewPageModel(private val goBack: () -> Unit): AbsUIModel<TimestampViewPageModelData, TimestampViewPageState, TimestampViewPageAction>() {
        val ologger = noCoLogger<TimestampViewPageModel>()

        private var imgState: ImageState by  mutableStateOf(ImageState.Loading)

        @Composable
        override fun CreateState(params: TimestampViewPageModelData): TimestampViewPageState {
            LaunchedEffect(Unit) {
                if (GlobalRecalling.imageCache.containsKey(params.dataItem.timestamp)) {
                    imgState = ImageState.Success(GlobalRecalling.imageCache[params.dataItem.timestamp]!!)
                    return@LaunchedEffect
                }

                imgState = try {
                    withContext(GlobalRecalling.imageLoadingDispatcher) {
                        val bytes = GlobalRecalling.imageCache.getOrPut(params.dataItem.timestamp) {
                            PluginManager.getStoragePlugin().getOrNull()
                                ?.getScreenData(params.dataItem.timestamp)
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
            return TimestampViewPageState(params.dataItem, params.highlights, imgState) {
                actionExecute(params, it)
            }
        }
        override fun actionExecute(params: TimestampViewPageModelData, action: TimestampViewPageAction) {
            when(action) {
                TimestampViewPageAction.GoBack -> goBack()
            }
        }
    }
}