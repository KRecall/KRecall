package io.github.octestx.krecall.plugins.impl

import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.repository.DataDB

class PluginContextImpl(metadata: PluginMetadata): PluginContext(metadata) {

    override fun addMark(timestamp: Long, mark: String) {
        DataDB.addMark(timestamp, mark)
    }

    override fun removeMark(timestamp: Long, mark: String) {
        DataDB.removeMark(timestamp, mark)
    }

    override fun listTimestampWithMark(mark: String): List<Long> = DataDB.listTimestampWithMark(mark)

    override fun listTimestampWithNotMark(mark: String): List<Long> = DataDB.listTimestampWithNotMark(mark)
}

