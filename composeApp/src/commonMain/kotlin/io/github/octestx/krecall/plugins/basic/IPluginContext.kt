package io.github.octestx.krecall.plugins.basic

import java.io.File

interface IPluginContext {
    fun getPluginDir(pluginId: String): File
    fun getPluginScreenDir(pluginId: String): File
    fun addMark(timestamp: Long, mark: String)
    fun removeMark(timestamp: Long, mark: String)
    fun listTimestampWithMark(mark: String): List<Long>
    fun listTimestampWithNotMark(mark: String): List<Long>
}