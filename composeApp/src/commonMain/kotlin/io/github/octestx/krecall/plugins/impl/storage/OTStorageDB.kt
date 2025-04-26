// composeApp/src/commonMain/kotlin/io/github/octestx/krecall/repository/OTStorageDB.kt
package io.github.octestx.krecall.plugins.impl.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.klogging.noCoLogger
import kotlinx.io.files.Path
import models.sqld.OTStorageDBItem
import models.sqld.OTStorageDBQueries

object OTStorageDB {
    private val ologger = noCoLogger<OTStorageDB>()
    private lateinit var driver: SqlDriver
    private lateinit var otStorageDBQueries: OTStorageDBQueries

    fun init(dbFile: Path) {
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.toString().apply {
            ologger.info("Initializing OTStorageDB: $this")
        }}")
        // 表结构已在 .sq 文件定义，此处无需重复创建
        otStorageDBQueries = OTStorageDBQueries(driver)
        otStorageDBQueries.createTable()
    }

    // region CRUD 操作
    fun listDataWithTimestampRange(start: Long, end: Long): List<OTStorageDBItem> {
        return otStorageDBQueries.listDataWithTimestampRange(start, end).executeAsList()
    }

    fun getData(timestamp: Long): OTStorageDBItem? {
        return otStorageDBQueries.getData(timestamp).executeAsOneOrNull()
    }

    fun getPreviousData(beforeTimestamp: Long): OTStorageDBItem? {
        return otStorageDBQueries.getPreviousData(beforeTimestamp).executeAsOneOrNull()
    }
    // endregion

    // region 数据操作
    fun addNewRecord(timestamp: Long, fileTimestamp: Long) {
        otStorageDBQueries.addNewRecord(timestamp, fileTimestamp)
    }

    fun setFileTimestamp(timestamp: Long, fileTimestamp: Long) {
        otStorageDBQueries.setFileTimestamp(fileTimestamp, timestamp)
    }

    fun markScreenData(timestamp: Long, mark: String) {
        otStorageDBQueries.markScreenData(mark, timestamp)
    }
    // endregion

    // region 查询操作
    fun listTimestampWithMark(mark: String): List<Long> {
        return otStorageDBQueries.listTimestampWithMark(mark).executeAsList()
    }

    fun listTimestampWithNotMark(mark: String): List<Long> {
        return otStorageDBQueries.listTimestampWithNotMark(mark).executeAsList()
    }
    // endregion
}
