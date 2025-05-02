package io.github.octestx.krecall.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.compottie.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LottieAnimationBlock(contentDescription: String?, modifier: Modifier = Modifier, keys: Array<Any?> = arrayOf(), bytes: ByteArray) {
    LottieAnimationBlock(contentDescription, modifier, keys) { bytes }
}

@Composable
fun LottieAnimationBlock(contentDescription: String?, modifier: Modifier = Modifier, keys: Array<Any?> = arrayOf(), bytes: suspend () -> ByteArray) {
    val composition by rememberLottieComposition(keys = keys) {
        LottieCompositionSpec.DotLottie(
            bytes()
        )
    }

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            iterations = Compottie.IterateForever
        ),
        contentDescription = contentDescription,
        modifier = modifier
    )
}