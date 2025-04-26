package io.github.octestx.krecall.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowBack
import compose.icons.tablericons.Download
import io.github.octestx.basic.multiplatform.ui.ui.core.AbsUIPage
import io.github.octestx.basic.multiplatform.ui.utils.highlightText
import io.github.octestx.krecall.GlobalRecalling
import io.github.octestx.krecall.model.ImageState
import io.github.octestx.krecall.plugins.PluginManager
import io.github.octestx.krecall.ui.TimestampViewPage.TimestampViewPageModelData
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import io.klogging.noCoLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.sqld.DataItem

class TimestampViewPage(model: TimestampViewPageModel): AbsUIPage<TimestampViewPageModelData, TimestampViewPage.TimestampViewPageState, TimestampViewPage.TimestampViewPageAction>(model) {
    private val ologger = noCoLogger<TimestampViewPage>()
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun UI(state: TimestampViewPageState) {
        MaterialTheme {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(title = {
                    Text(text = "Timestamp: ${state.dataItem.timestamp}")
                }, navigationIcon = {
                    IconButton(onClick = {
                        state.action(TimestampViewPageAction.GoBack)
                    }) {
                        Icon(TablerIcons.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
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
                        Column(Modifier.padding(5.dp).border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)).padding(5.dp)) {
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
                                            Icon(TablerIcons.Download, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                        if (state.dataItem.status == 0L || state.dataItem.status == 1L) {
                            Text(
                                text = if (state.dataItem.data_ == null) {
                                    AnnotatedString("NULL")
                                } else {
                                    highlightText(
                                        text = state.dataItem.data_,
                                        highlightColor = MaterialTheme.colorScheme.primary,
                                        highlights = state.highlights
                                    )
                                },
                                modifier = Modifier.padding(8.dp).border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = MaterialTheme.shapes.medium).padding(5.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else if (state.dataItem.status == 2L) {
//                        val err = exceptionSerializableOjson.decodeFromString<AIResult.Failed<String>>(state.dataItem.error!!)
                            val err = state.dataItem.error!!
                            SelectionContainer {
                                Text(
                                    text = err,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
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