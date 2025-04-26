package io.github.octestx.krecall.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun FloatingBlock(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            // 阴影效果（elevation 控制阴影强度）
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = MaterialTheme.colorScheme.outline,
                spotColor = MaterialTheme.colorScheme.outline
            )
            // 圆角裁剪
            .clip(RoundedCornerShape(8.dp))
            // 背景颜色（使用 MaterialTheme 保持主题一致性）
            .background(MaterialTheme.colorScheme.surface)
            // 内边距
            .padding(6.dp)
    ) {
        content()
    }
}