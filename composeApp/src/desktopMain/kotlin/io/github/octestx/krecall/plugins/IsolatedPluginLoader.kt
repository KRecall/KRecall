package io.github.octestx.krecall.plugins

import java.io.File
import java.net.URLClassLoader

class IsolatedPluginLoader(private val jarPath: String) {
    private val classLoader by lazy {
        URLClassLoader(
            arrayOf(File(jarPath).toURI().toURL()),
            null  // 使用独立 ClassLoader 实现隔离
        )
    }

    fun <T> loadClass(className: String, type: Class<T>): T {
        return classLoader.loadClass(className)
            .getDeclaredConstructor()
            .newInstance() as T
    }
}
