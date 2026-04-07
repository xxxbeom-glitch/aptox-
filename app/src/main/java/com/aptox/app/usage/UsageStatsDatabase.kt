package com.aptox.app.usage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 앱별 일별 사용량 + 카테고리 일별 합산 + 앱별 12슬롯(2시간) 일별 합산.
 * Room 대신 SQLite 직접 사용 (KSP 호환 이슈 회피).
 */
class UsageStatsDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "aptox_usage.db",
    null,
    2,
) {

    override fun onCreate(db: SQLiteDatabase) {
        createV1Tables(db)
        createV2Tables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createV2Tables(db)
        }
    }

    private fun createV1Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_usage (
                date TEXT NOT NULL,
                packageName TEXT NOT NULL,
                usageMs INTEGER NOT NULL,
                sessionCount INTEGER NOT NULL,
                PRIMARY KEY (date, packageName)
            )
            """.trimIndent(),
        )
    }

    private fun createV2Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_daily (
                date TEXT NOT NULL,
                category TEXT NOT NULL,
                usageMs INTEGER NOT NULL,
                PRIMARY KEY (date, category)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS time_segment_daily (
                date TEXT NOT NULL,
                packageName TEXT NOT NULL,
                s0 INTEGER NOT NULL,
                s1 INTEGER NOT NULL,
                s2 INTEGER NOT NULL,
                s3 INTEGER NOT NULL,
                s4 INTEGER NOT NULL,
                s5 INTEGER NOT NULL,
                s6 INTEGER NOT NULL,
                s7 INTEGER NOT NULL,
                s8 INTEGER NOT NULL,
                s9 INTEGER NOT NULL,
                s10 INTEGER NOT NULL,
                s11 INTEGER NOT NULL,
                PRIMARY KEY (date, packageName)
            )
            """.trimIndent(),
        )
    }

    fun insertAll(entities: List<DailyUsageEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (e in entities) {
                val cv = ContentValues().apply {
                    put("date", e.date)
                    put("packageName", e.packageName)
                    put("usageMs", e.usageMs)
                    put("sessionCount", e.sessionCount)
                }
                db.insertWithOnConflict("daily_usage", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getByDateRange(startDate: String, endDate: String): List<DailyUsageEntity> {
        val db = readableDatabase
        val cursor = db.query(
            "daily_usage",
            arrayOf("date", "packageName", "usageMs", "sessionCount"),
            "date >= ? AND date <= ?",
            arrayOf(startDate, endDate),
            null,
            null,
            "date, packageName",
        )
        val result = mutableListOf<DailyUsageEntity>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    DailyUsageEntity(
                        date = it.getString(0),
                        packageName = it.getString(1),
                        usageMs = it.getLong(2),
                        sessionCount = it.getInt(3),
                    ),
                )
            }
        }
        return result
    }

    fun hasDataForDate(date: String): Boolean {
        readableDatabase.query(
            "daily_usage",
            arrayOf("date"),
            "date = ?",
            arrayOf(date),
            null,
            null,
            null,
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    fun getEarliestDate(): String? {
        return try {
            readableDatabase.rawQuery("SELECT MIN(date) FROM daily_usage", null).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getDistinctDateCount(): Int {
        return try {
            readableDatabase.rawQuery("SELECT COUNT(DISTINCT date) FROM daily_usage", null).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun replaceCategoryStatsForDay(date: String, rows: List<DailyCategoryStatEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("category_daily", "date = ?", arrayOf(date))
            for (r in rows) {
                val cv = ContentValues().apply {
                    put("date", r.date)
                    put("category", r.category)
                    put("usageMs", r.usageMs)
                }
                db.insert("category_daily", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceTimeSegmentsForDay(date: String, rows: List<DailyTimeSegmentEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("time_segment_daily", "date = ?", arrayOf(date))
            for (r in rows) {
                val cv = ContentValues().apply {
                    put("date", r.date)
                    put("packageName", r.packageName)
                    for (i in 0 until SLOT_COUNT) {
                        put("s$i", r.slotMs[i])
                    }
                }
                db.insert("time_segment_daily", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** [startDate]~[endDate] inclusive, 카테고리별 usageMs 합계 */
    fun getCategoryTotalsForDateRange(startDate: String, endDate: String): Map<String, Long> {
        val db = readableDatabase
        val q =
            """
            SELECT category, SUM(usageMs) FROM category_daily
            WHERE date >= ? AND date <= ?
            GROUP BY category
            """.trimIndent()
        db.rawQuery(q, arrayOf(startDate, endDate)).use { c ->
            val m = mutableMapOf<String, Long>()
            while (c.moveToNext()) {
                m[c.getString(0)] = c.getLong(1)
            }
            return m
        }
    }

    fun getTimeSegmentForDay(date: String, packageName: String): LongArray? {
        readableDatabase.query(
            "time_segment_daily",
            arrayOf("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"),
            "date = ? AND packageName = ?",
            arrayOf(date, packageName),
            null,
            null,
            null,
        ).use { c ->
            if (!c.moveToFirst()) return null
            return LongArray(SLOT_COUNT) { i -> c.getLong(i) }
        }
    }

    fun insertAllCategoryStats(rows: List<DailyCategoryStatEntity>) {
        if (rows.isEmpty()) return
        val byDate = rows.groupBy { it.date }
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((date, list) in byDate) {
                db.delete("category_daily", "date = ?", arrayOf(date))
                for (r in list) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("category", r.category)
                        put("usageMs", r.usageMs)
                    }
                    db.insert("category_daily", null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAllTimeSegments(rows: List<DailyTimeSegmentEntity>) {
        if (rows.isEmpty()) return
        val byDate = rows.groupBy { it.date }
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((date, list) in byDate) {
                db.delete("time_segment_daily", "date = ?", arrayOf(date))
                for (r in list) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("packageName", r.packageName)
                        for (i in 0 until SLOT_COUNT) {
                            put("s$i", r.slotMs[i])
                        }
                    }
                    db.insert("time_segment_daily", null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun hasCategoryDataForDateRange(startDate: String, endDate: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM category_daily WHERE date >= ? AND date <= ? LIMIT 1",
            arrayOf(startDate, endDate),
        ).use { return it.moveToFirst() }
    }

    /** 백업용: 일별 사용량 전체 */
    fun getAllDailyUsageRows(): List<DailyUsageEntity> {
        val db = readableDatabase
        db.rawQuery(
            "SELECT date, packageName, usageMs, sessionCount FROM daily_usage ORDER BY date, packageName",
            null,
        ).use { c ->
            val out = mutableListOf<DailyUsageEntity>()
            while (c.moveToNext()) {
                out.add(
                    DailyUsageEntity(
                        date = c.getString(0),
                        packageName = c.getString(1),
                        usageMs = c.getLong(2),
                        sessionCount = c.getInt(3),
                    ),
                )
            }
            return out
        }
    }

    /** 백업용: 카테고리 일별 전체 */
    fun getAllCategoryDailyRows(): List<DailyCategoryStatEntity> {
        readableDatabase.rawQuery(
            "SELECT date, category, usageMs FROM category_daily ORDER BY date, category",
            null,
        ).use { c ->
            val out = mutableListOf<DailyCategoryStatEntity>()
            while (c.moveToNext()) {
                out.add(
                    DailyCategoryStatEntity(
                        date = c.getString(0),
                        category = c.getString(1),
                        usageMs = c.getLong(2),
                    ),
                )
            }
            return out
        }
    }

    /** 백업용: 시간대 세그먼트 전체 */
    fun getAllTimeSegmentRows(): List<DailyTimeSegmentEntity> {
        val cols = (0 until SLOT_COUNT).map { "s$it" }.toTypedArray()
        readableDatabase.query(
            "time_segment_daily",
            arrayOf("date", "packageName") + cols,
            null,
            null,
            null,
            null,
            "date, packageName",
        ).use { c ->
            val out = mutableListOf<DailyTimeSegmentEntity>()
            while (c.moveToNext()) {
                val slots = LongArray(SLOT_COUNT) { i -> c.getLong(2 + i) }
                out.add(
                    DailyTimeSegmentEntity(
                        date = c.getString(0),
                        packageName = c.getString(1),
                        slotMs = slots,
                    ),
                )
            }
            return out
        }
    }

    /**
     * 복원용: 파싱된 데이터로 테이블별 교체. [replaceDaily] 등이 false이면 해당 테이블은 삭제·삽입하지 않음.
     * 호출 전후로 DB 인스턴스는 동일 파일을 가리킨다.
     */
    fun restoreUsageTablesSelective(
        replaceDaily: Boolean,
        dailyRows: List<DailyUsageEntity>,
        replaceCategory: Boolean,
        categoryRows: List<DailyCategoryStatEntity>,
        replaceSegment: Boolean,
        segmentRows: List<DailyTimeSegmentEntity>,
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (replaceDaily) {
                db.delete("daily_usage", null, null)
                for (e in dailyRows) {
                    val cv = ContentValues().apply {
                        put("date", e.date)
                        put("packageName", e.packageName)
                        put("usageMs", e.usageMs)
                        put("sessionCount", e.sessionCount)
                    }
                    db.insert("daily_usage", null, cv)
                }
            }
            if (replaceCategory) {
                db.delete("category_daily", null, null)
                for (r in categoryRows) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("category", r.category)
                        put("usageMs", r.usageMs)
                    }
                    db.insert("category_daily", null, cv)
                }
            }
            if (replaceSegment) {
                db.delete("time_segment_daily", null, null)
                for (r in segmentRows) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("packageName", r.packageName)
                        for (i in 0 until SLOT_COUNT) {
                            put("s$i", r.slotMs[i])
                        }
                    }
                    db.insert("time_segment_daily", null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val SLOT_COUNT = 12
    }
}
