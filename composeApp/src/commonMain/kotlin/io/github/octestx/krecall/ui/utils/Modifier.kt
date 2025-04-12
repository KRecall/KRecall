package io.github.octestx.krecall.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.borderProgress(
    progress: Float,
    startColor: Color = Color.Blue,
    endColor: Color = Color.Magenta,
    spacing: Dp = 4.dp  // 新增间距参数
): Modifier = composed {
    val strokeWidth = with(LocalDensity.current) { 4.dp.toPx() }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() } // 转换为像素
    val halfStroke = strokeWidth / 2

    this.drawWithContent {
        drawContent()
        val totalLength = 2 * (size.width + size.height) - 4 * strokeWidth
        val currentLength = totalLength * progress.coerceIn(0f..1f)
        var remaining = currentLength

        // 上边右半部分（添加垂直间距）
        if (remaining > 0) {
            val topRightAvailable = size.width / 2 - halfStroke
            val drawLength = minOf(remaining, topRightAvailable)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(size.width / 2, halfStroke),
                    end = Offset(size.width / 2 + drawLength, halfStroke)
                ),
                start = Offset(size.width / 2, halfStroke - spacingPx), // 上移
                end = Offset(size.width / 2 + drawLength, halfStroke - spacingPx),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            remaining -= drawLength
        }

        // 右边（添加水平间距）
        if (remaining > 0) {
            val rightAvailable = size.height - strokeWidth
            val drawHeight = minOf(remaining, rightAvailable)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(size.width - halfStroke, halfStroke),
                    end = Offset(size.width - halfStroke, halfStroke + drawHeight)
                ),
                start = Offset(size.width - halfStroke + spacingPx, halfStroke - spacingPx), // 右移
                end = Offset(size.width - halfStroke + spacingPx, halfStroke + drawHeight - spacingPx),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            remaining -= drawHeight
        }

        // 下边（添加垂直间距）
        if (remaining > 0) {
            val bottomAvailable = size.width - strokeWidth
            val drawWidth = minOf(remaining, bottomAvailable)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(endColor, startColor), // 反转颜色顺序
                    start = Offset(size.width - halfStroke, size.height - halfStroke),
                    end = Offset(size.width - halfStroke - drawWidth, size.height - halfStroke)
                ),
                start = Offset(size.width - halfStroke + spacingPx, size.height - halfStroke + spacingPx), // 下移
                end = Offset(size.width - halfStroke - drawWidth + spacingPx, size.height - halfStroke + spacingPx),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            remaining -= drawWidth
        }

        // 左边（添加水平间距）
        if (remaining > 0) {
            val leftAvailable = size.height - strokeWidth
            val drawHeight = minOf(remaining, leftAvailable)
            val endY = (size.height - halfStroke) - drawHeight + spacingPx
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(endColor, startColor), // 反转颜色顺序
                    start = Offset(halfStroke, size.height - halfStroke),
                    end = Offset(halfStroke, endY)
                ),
                start = Offset(halfStroke - spacingPx, size.height - halfStroke + spacingPx), // 左移
                end = Offset(halfStroke - spacingPx, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            remaining -= drawHeight
        }

        // 上边左半部分（添加垂直间距）
        if (remaining > 0) {
            val topLeftAvailable = size.width / 2 - halfStroke
            val drawLength = minOf(remaining, topLeftAvailable)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor),
                    start = Offset(halfStroke, halfStroke),
                    end = Offset(halfStroke + drawLength, halfStroke)
                ),
                start = Offset(halfStroke - spacingPx, halfStroke - spacingPx), // 左移+上移
                end = Offset(halfStroke + drawLength - spacingPx, halfStroke - spacingPx),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            remaining -= drawLength
        }
    }
}
