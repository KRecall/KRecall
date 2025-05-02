package io.github.octestx.krecall.ui.utils

import okio.withLock
import java.util.concurrent.locks.ReentrantLock

// TODO
class CallListener<A: Any?> {
    private val lock = ReentrantLock()
    private val listeners = mutableListOf<suspend (arg: A) -> Unit>()
    suspend fun call(arg: A) {
        lock.withLock {
            for (listener in listeners) {
                listener(arg)
            }
        }
    }
    fun addListener(listener: suspend (arg: A) -> Unit) {
        lock.withLock {
            listeners += listener
        }
    }
}