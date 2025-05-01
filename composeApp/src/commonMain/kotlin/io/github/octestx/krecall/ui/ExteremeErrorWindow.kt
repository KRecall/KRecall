package io.github.octestx.krecall.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.system.exitProcess


private val extremeError = MutableStateFlow<Throwable?>(null)
@Composable
fun ApplicationScope.extremeErrorWindowInject() {
    extremeError.collectAsState().value?.let { throwable ->
        Window(
            onCloseRequest = {
                exitProcess(84)
            },
            title = "KRecall-DangerError!"
        ) {
            SelectionContainer {
                LazyColumn {
                    item {
                        Text("KRecall-DangerError! If you exit this window, the KRecall will exitProcess", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        HorizontalDivider()
                        Text(throwable::class.qualifiedName?:"Unknown Error!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        HorizontalDivider()
                        Text(throwable.message?:"Unknown Error message!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        HorizontalDivider()
                        Text(throwable.stackTraceToString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
fun callExtremeErrorWindow(throwable: Throwable) {
    extremeError.value = throwable
}