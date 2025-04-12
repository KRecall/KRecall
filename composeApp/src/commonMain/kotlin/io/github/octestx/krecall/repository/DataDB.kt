package io.github.octestx.krecall.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.exceptionSerializableOjson
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import models.sqld.DataDBQueries
import models.sqld.DataItem

object DataDB {
    private val ologger = noCoLogger<DataDB>()
    private lateinit var driver: SqlDriver
    private lateinit var dataDBQueries: DataDBQueries

    fun init(dbFile: Path) {
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.toString().apply { ologger.info("loadSql: $this") }}")

        dataDBQueries = DataDBQueries(driver)

        //TODO 手动建表
        dataDBQueries.createTable()
        ologger.info("Table DataItem created or verified.")
    }

    fun listAllData(): List<DataItem> {
        return dataDBQueries.listAllData().executeAsList()
    }

    fun getData(timestamp: Long): DataItem? {
        return dataDBQueries.getData(timestamp).executeAsOneOrNull()
    }

    fun listNotProcessedData(): List<DataItem> {
        return dataDBQueries.listNotProcessedData().executeAsList()
    }

    suspend fun searchDataInAll(queries: List<String>): List<DataItem> {
        return withContext(Dispatchers.IO) {
            if (queries.isEmpty()) return@withContext emptyList()

            // 按结果集大小排序，从小集合开始交
            val sortedSets = queries.map { query ->
                dataDBQueries.searchDataInAll("%$query%").executeAsList().toSet()
            }.sortedBy { it.size }

            return@withContext sortedSets.reduceOrNull { acc, set ->
                acc.intersect(set).takeIf { it.isNotEmpty() } ?: return@withContext emptyList()
            }?.toList() ?: emptyList()
        }
    }

    fun searchDataWithTimeRange(startTimestamp: Long, endTimestamp: Long, queries: List<String>): List<DataItem> {
        if (queries.isEmpty()) return emptyList()

        // 按结果集大小排序，从小集合开始交
        val sortedSets = queries.map { query ->
            dataDBQueries.searchDataWithTimeRange(startTimestamp, endTimestamp, "%$query%").executeAsList().toSet()
        }.sortedBy { it.size }

        return sortedSets.reduceOrNull { acc, set ->
            acc.intersect(set).takeIf { it.isNotEmpty() } ?: return emptyList()
        }?.toList() ?: emptyList()
    }

    fun addNewRecord(screenId: Long, timestamp: Long, mark: String, appId: String, windowTitle: String) {
        dataDBQueries.addNewRecord(screenId, timestamp, mark, appId, windowTitle)
    }

    fun setData(timestamp: Long, data: String) {
        dataDBQueries.setData(data, timestamp)
    }

    fun processed(timestamp: Long) {
        dataDBQueries.processed(timestamp)
    }

    fun addMark(timestamp: Long, mark: String) {
        //判断mark是否包含意外换行
        if (mark.contains("\n")) {
            ologger.warn("mark contains unexpected newline: $mark")
            throw IllegalArgumentException("mark contains unexpected newline: $mark")
        }
        val data = dataDBQueries.getData(timestamp).executeAsOneOrNull() ?: return
        val newMark = data.mark + "\n" + mark
        dataDBQueries.setMarkScreenData(newMark, timestamp)
    }

    fun removeMark(timestamp: Long, mark: String) {
        //判断mark是否包含意外换行
        if (mark.contains("\n")) {
            ologger.warn("mark contains unexpected newline: $mark")
            throw IllegalArgumentException("mark contains unexpected newline: $mark")
        }
        val data = dataDBQueries.getData(timestamp).executeAsOneOrNull() ?: return
        data.mark.split("\n").filter { it != mark }.joinToString("\n").let {
            dataDBQueries.setMarkScreenData(it, timestamp)
        }
    }

    fun listTimestampWithMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithMark(mark).executeAsList()
    }

    fun listTimestampWithNotMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithNotMark(mark).executeAsList()
    }

    fun happenError(timestamp: Long, error: AIResult.Failed<String>) {
        dataDBQueries.happenError(exceptionSerializableOjson.encodeToString(error), timestamp)
    }

    fun happenError(timestamp: Long, error: Exception) {
        dataDBQueries.happenError(exceptionSerializableOjson.encodeToString(error), timestamp)
    }

    fun appendOCRData(timestamp: Long, data: String) {
        dataDBQueries.setOCRData(data, timestamp)
    }

    fun getLeastTimestamp(): Long? {
        return dataDBQueries.getLeastTimestamp().executeAsOneOrNull()
    }
}
