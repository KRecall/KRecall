package io.github.octestx.krecall.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.Closeable

/**
 * Queue-based sequencer for managing ordered execution of animations or tasks.
 *
 * Ensures sequential processing of queued items with built-in waiting mechanism.
 */
class WaitingFlow(val delay: Long = 10): Closeable {
    // 用协程通道实现请求队列
    private val requestChannel = Channel<CompletableDeferred<Boolean>>(Channel.UNLIMITED)

    init {
        // 启动独立的协程处理队列
        CoroutineScope(Dispatchers.Default).launch {
            for (deferred in requestChannel) {
                delay(delay)    // 执行等待
                deferred.complete(true) // 完成本次等待
            }
        }
    }

    suspend fun wait(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        requestChannel.send(deferred)  // 加入队列
        return deferred.await()       // 等待处理完成
    }

    // 可选：安全关闭方法
    override fun close() {
        requestChannel.close()
    }
}