import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.ArrowRight
import io.github.octestx.basic.multiplatform.common.utils.TimeStamp
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.composeapp.generated.resources.current_location
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimestampRateController(
    timestamps: List<Long>,
    currentIndex: Int,
    theNowMode: Boolean,
    changeTheNowMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    changeIndex: (Int) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var isDragging by remember { mutableStateOf(false) }
    val maxMultiplier = 15f

    // 计算速度倍率（-15x 到 +15x）
    val speedMultiplier = remember(sliderPosition) {
        (sliderPosition - 0.5f) * 2 * maxMultiplier
    }
    LaunchedEffect(theNowMode) {
        while (true) {
            if (theNowMode) {
                changeIndex(timestamps.lastIndex)
            }
            delay(350)
        }
    }
    // 自动滚动协程
    LaunchedEffect(isDragging, speedMultiplier) {
        if (isDragging && timestamps.isNotEmpty()) {
            var lastUpdateTime = 0L
            while (true) {
                val interval = (50 / abs(speedMultiplier)).toLong().coerceAtLeast(50)
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastUpdateTime >= interval) {
                    val direction = sign(speedMultiplier)
                    val newIndex = (currentIndex + direction).coerceIn(0, timestamps.lastIndex)

                    if (newIndex != currentIndex) {
                        changeIndex(newIndex)
                    }

                    lastUpdateTime = currentTime
                }
                delay(16) // 约60fps更新频率
            }
        }
    }

    Column(modifier.padding(16.dp)) {
        Row {
            TooltipArea(tooltip = {
                Text("锁定当前时间[$theNowMode]")
            }) {
                IconToggleButton(
                    theNowMode,
                    onCheckedChange = {
                        changeTheNowMode(!theNowMode)
                    }
                ) {
                    Icon(painterResource(Res.drawable.current_location), contentDescription = null, tint = if (theNowMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                }
            }
            // 倍率指示器
            AnimatedContent(speedMultiplier) { speed ->
                Text(
                    text = "×${abs(speed).format(1)} ${if(speed>0)"▶" else if (speed<0)"◀" else ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            IconButton(onClick = {
                val newIndex = currentIndex - 50
                if (newIndex >= 0) {
                    changeIndex(newIndex)
                } else {
                    changeIndex(0)
                }
            }, enabled = (currentIndex - 1 >= 0) && theNowMode.not()) {
                Icon(
                    imageVector = TablerIcons.ArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = {
                val newIndex = currentIndex - 1
                if (newIndex >= 0) {
                    changeIndex(newIndex)
                }
            }, enabled = (currentIndex - 1 >= 0) && theNowMode.not()) {
                Icon(
                    imageVector = TablerIcons.ArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                val newIndex = currentIndex + 1
                if (newIndex <= timestamps.lastIndex) {
                    changeIndex(newIndex)
                }
            }, enabled = (currentIndex + 1 <= timestamps.lastIndex) && theNowMode.not()) {
                Icon(
                    imageVector = TablerIcons.ArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                val newIndex = currentIndex + 100
                if (newIndex <= timestamps.lastIndex) {
                    changeIndex(newIndex)
                } else {
                    changeIndex(timestamps.lastIndex)
                }
            }, enabled = (currentIndex + 1 <= timestamps.lastIndex) && theNowMode.not()) {
                Icon(
                    imageVector = TablerIcons.ArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // 滑动控制器
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isDragging = true
            },
            onValueChangeFinished = {
                sliderPosition = 0.5f
                isDragging = false
            },
            valueRange = 0f..1f,
            enabled = theNowMode.not(),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            },
            track = { sliderPositions ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val background = MaterialTheme.colorScheme.surfaceVariant
                    val line = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val trackWidth = size.width
                        val activeOffset = sliderPositions.valueRange.endInclusive * trackWidth

                        // 绘制背景轨道
                        drawLine(
                            color = background,
                            start = Offset(0f, center.y),
                            end = Offset(trackWidth, center.y),
                            strokeWidth = 8.dp.toPx()
                        )

                        // 绘制激活轨道
                        drawLine(
                            color = line,
                            start = Offset(center.x - trackWidth/4, center.y),
                            end = Offset(center.x + trackWidth/4, center.y),
                            strokeWidth = 12.dp.toPx()
                        )
                    }
                }
            }
        )

        // 时间戳显示
        val lastIndex = timestamps.lastIndex
        val current = buildString {
            append("Current: ")
            append(timestamps[currentIndex])
            append("[${currentIndex}]")
        }
        val next = buildString {
            append("Next: ")
            val time = timestamps.getOrNull(currentIndex + 1)
            if (time != null) {
                append(time)
                append("[${lastIndex - currentIndex}]")
            } else {
                append("NULL")
            }
        }
        Text(
            text = "${TimeStamp.formatTimestampToChinese(timestamps[currentIndex])} SelectedTimestamp: $current $next",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 辅助扩展函数
private fun Float.format(decimal: Int): String = "%.${decimal}f".format(this)
private fun sign(value: Float): Int = if (value > 0) 1 else if (value < 0) -1 else 0
