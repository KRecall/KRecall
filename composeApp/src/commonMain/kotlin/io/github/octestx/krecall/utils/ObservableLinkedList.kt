package io.github.octestx.krecall.utils

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

class ObservableLinkedList<E> {
    private val list = LinkedList<E>()
    val count = MutableStateFlow(0)
    fun addLast(e: E) {
        list.addLast(e)
        count.value = list.size
    }
    fun pollLastOrNull(): E? {
        return if (list.isEmpty()) null else {
            list.pollLast().apply {
                count.value = list.size
            }
        }
    }
}