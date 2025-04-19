package io.github.octestx.krecall.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun EnhancedDropdownSelector(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    onOpen: () -> Unit = {},       // 新增参数：打开时回调
    onClose: () -> Unit = {},      // 新增参数：关闭时回调
    onHover: (String) -> Unit = {}, // 新增参数：悬停时回调
    onExitHover: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()

    // 滚动定位逻辑
    if (expanded) {
        LaunchedEffect(Unit) {
            val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
            scrollState.scrollToItem(selectedIndex)
        }
    }

    Box(modifier = Modifier.wrapContentSize()) {
        // 触发按钮
        Button(
            onClick = {
                expanded = true
                onOpen() // 触发打开回调
            },
            modifier = Modifier.width(200.dp)
        ) {
            Text(
                text = selectedItem ?: "请选择",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.ArrowDropDown,
                contentDescription = "下拉箭头"
            )
        }

        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onClose() // 触发关闭回调
                onExitHover()
            },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            LazyColumn(state = scrollState, modifier = Modifier.width(200.dp).height(350.dp)) {
                itemsIndexed(items, key = { _, item -> item }) { index, item ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (item == selectedItem) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选中",
                                        tint = Color.Blue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = item,
                                    color = if (item == selectedItem) Color.Blue else Color.Black
                                )
                            }
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                            onClose()
                            onExitHover()
                        },
                        modifier = Modifier
//                            .hoverable { // 悬停检测
//                                onHover(item)
//                            }
                            .onPointerEvent(PointerEventType.Enter) {
                                onHover(item)
                            }
                            .onPointerEvent(PointerEventType.Move) {
                                onHover(item)
                            }
                            .onPointerEvent(PointerEventType.Exit) {
                                onExitHover()
                            }
//                            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
//                                hide()
//                            }
                            .background(
                                if (item == selectedItem) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                    )
                }
            }
        }
    }
}

private fun Modifier.onPointerEvent(
    eventType: PointerEventType,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) = pointerInput(eventType, pass, onEvent) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pass)
            if (event.type == eventType) {
                onEvent(event)
            }
        }
    }
}