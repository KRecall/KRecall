package io.github.octestx.krecall.utils

import java.util.*

// 添加线程安全扩展（文件底部）：
fun <K, V> LinkedHashMap<K, V>.synchronized() = Collections.synchronizedMap(this)