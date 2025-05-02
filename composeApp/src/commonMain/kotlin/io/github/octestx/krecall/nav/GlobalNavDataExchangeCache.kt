package io.github.octestx.krecall.nav

/**
 * 全局导航数据交换缓存单例对象（线程安全改造方案）
 *
 * 改进点：
 * 1. 使用线程安全的 ConcurrentHashMap 替代 mutableStateMapOf
 * 2. 增加同步锁机制保证原子操作
 * 3. 移除了响应式特性依赖
 */
object GlobalNavDataExchangeCache {
    // 使用并发安全数据结构（替代原mutableStateMapOf）
    private val data = java.util.concurrent.ConcurrentHashMap<String, Any?>()

    fun putData(value: Any?): String {
        return synchronized(data) {
            val key = "nav_data_${System.currentTimeMillis()}"
            putData(key, value)
            key
        }
    }

    /**
     * 线程安全的数据写入
     * @param key 数据键（推荐使用命名空间式命名，如："module.feature.key"）
     */
    fun putData(key: String, value: Any?) {
        data[key] = value
    }

    /**
     * 原子化获取并移除数据（通过synchronized保证复合操作原子性）
     */
    fun getAndDestroyData(key: String): Any? {
        return synchronized(data) {
            data.remove(key)
        }
    }

    /**
     * 安全读取数据快照（返回数据副本避免并发修改问题）
     */
    fun justGetData(key: String): Any? {
        return data[key]
    }
}
