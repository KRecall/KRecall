package io.github.octestx.krecall.repository

import io.github.octestx.basic.multiplatform.common.appDirs
import io.github.octestx.basic.multiplatform.common.utils.asKFilePath
import io.github.octestx.basic.multiplatform.common.utils.link
import io.github.octestx.basic.multiplatform.common.utils.linkDir
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
        plugins = appDirs.getUserDataDir().asKFilePath().linkDir("Plugins")
        ologger.info { "PluginDir: $plugins" }
//        pluginsJars = plugins.linkDir("jars")
        pluginsData = plugins.linkDir("data")

        screenDir = appDirs.getUserDataDir().asKFilePath().linkDir("Screen")
        
        dataDBFile = appDirs.getUserDataDir().asKFilePath().link("data.db")
        DataDB.init(dataDBFile)

        configDir = appDirs.getUserDataDir().asKFilePath().linkDir("Configs")
        ConfigManager.reload()
    }
    fun pluginData(pluginId: String) = pluginsData.linkDir(pluginId)
    fun pluginScreenDir(pluginId: String) = screenDir.linkDir(pluginId)
}