package io.github.octestx.krecall.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
@Deprecated("")
fun AnimationVisibilityShowNow(
    content: @Composable () -> Unit
) {
    var show by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        delay(10)
        show = true
    }
    AnimatedVisibility(show) {
        content()
    }
}

@Composable
fun DelayShowAnimation(
    delay: Long = 10,
    animationTime: Long = 300,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var show by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        delay(delay)
        show = true
    }
    AnimatedVisibility(show, enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandIn()) {
        content()
    }
}

@Composable
fun StepLoadAnimation(
    step: Int,
    nextStepTime: Long = 100,
    content: @Composable (step: Int) -> Unit
) {
    var currentStep by remember {
        mutableStateOf(0)
    }
    LaunchedEffect(Unit) {
        while (currentStep < step) {
            currentStep++
            delay(nextStepTime)
        }
    }
    content(currentStep)
}