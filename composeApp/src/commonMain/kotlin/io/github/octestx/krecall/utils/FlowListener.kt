package io.github.octestx.krecall.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object FlowListener {
    private val scope = CoroutineScope(Dispatchers.Default)
    fun <T> addFlowListener(flow: Flow<T>, listener: (T) -> Unit) {
        scope.launch {
            flow.collectLatest { listener(it) }
        }
    }
}