package io.github.octestx.krecall.ui.tour

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 *
 */
@Composable
fun ViewKRecallUpdatedInfo(nextStep: () -> Unit) {
    Column {
        Text("这里没有详细信息")
        Button(onClick = {
            nextStep()
        }) {
            Text("Next")
        }
    }
}