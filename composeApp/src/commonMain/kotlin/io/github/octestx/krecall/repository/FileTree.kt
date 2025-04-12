package io.github.octestx.krecall.repository

import io.github.kotlin.fibonacci.appDirs
import io.github.kotlin.fibonacci.utils.asFilePath
import io.github.kotlin.fibonacci.utils.link
import io.github.kotlin.fibonacci.utils.linkDir
import io.klogging.noCoLogger
import kotlinx.io.files.Path

object FileTree {
    private val ologger = noCoLogger<FileTree>()

    private lateinit var plugins: Path

    private lateinit var pluginsData: Path

    private lateinit var screenDir: Path

    private lateinit var dataDBFile: Path

    lateinit var configDir: Path private set
    fun init() {
        plugins = appDirs.getUserDataDir().asFilePath().linkDir("Plugins")
        ologger.info { "PluginDir: $plugins" }
//        pluginsJars = plugins.linkDir("jars")
        pluginsData = plugins.linkDir("data")

        screenDir = appDirs.getUserDataDir().asFilePath().linkDir("Screen")
        
        dataDBFile = appDirs.getUserDataDir().asFilePath().link("data.db")
        DataDB.init(dataDBFile)

        configDir = appDirs.getUserDataDir().asFilePath().linkDir("Configs")
        ConfigManager.reload()
    }
    fun pluginData(pluginId: String) = pluginsData.linkDir(pluginId)
    fun pluginScreenDir(pluginId: String) = screenDir.linkDir(pluginId)
}