package io.github.octestx.krecall.ui.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.*
import io.github.octestx.krecall.composeapp.generated.resources.Res
import io.github.octestx.krecall.ui.utils.LottieAnimationBlock
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.time.DurationUnit

object AnimationComponents {
    val big = 240.dp
    val small = 32.dp
    @ExperimentalResourceApi
    @Composable
    fun LoadingBig(modifier: Modifier = Modifier) {
        val path = "files/loading.lottie"
        LottieAnimationBlock("loading", modifier = Modifier.size(big).then(modifier), arrayOf(path)) {
            Res.readBytes(path)
        }
    }

    @ExperimentalResourceApi
    @Composable
    fun WarningSmall(modifier: Modifier = Modifier) {
        val path = "files/warning.lottie"
        LottieAnimationBlock("warning", modifier = Modifier.size(small).then(modifier), arrayOf(path)) {
            Res.readBytes(path)
        }
    }

    @ExperimentalResourceApi
    @Composable
    fun ProcessingSmall(modifier: Modifier = Modifier) {
        val path = "files/process.lottie"
        LottieAnimationBlock("process", modifier = Modifier.size(small).then(modifier), arrayOf(path)) {
            Res.readBytes(path)
        }
    }

    @Deprecated("无效")
    @ExperimentalResourceApi
    @Composable
    fun CapturingScreen(modifier: Modifier = Modifier): suspend () -> Unit {
        val path = "files/captureScreen.lottie"
        val composition by rememberLottieComposition(path) {
            LottieCompositionSpec.DotLottie(
                Res.readBytes(path)
            )
        }
        var isPlaying by remember { mutableStateOf(false) }
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
                isPlaying = isPlaying
            ),
            contentDescription = "CapturingScreen",
            modifier = modifier
        )
        return {
            isPlaying = true
            val delay = composition?.duration?.toLong(DurationUnit.MILLISECONDS)?: 0
            delay(delay)
            isPlaying = false
        }
    }
}