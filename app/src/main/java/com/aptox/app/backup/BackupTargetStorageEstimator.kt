package com.aptox.app.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.ceil

/**
 * 로컬 백업 ZIP에 해당하는 데이터의 **디스크 상** 예상 크기.
 * - 사용량 SQLite (`aptox_usage.db` + wal/shm)
 * - 백업 대상 SharedPreferences (제한·알림·뱃지/대기뱃지)
 * - 앱 `files/datastore` 아래 `.preferences_pb` 전부 (DataStore 합산)
 */
object BackupTargetStorageEstimator {

    private val BACKUP_SHARED_PREF_NAMES = arrayOf(
        "aptox_app_restrictions",
        "aptox_notification_prefs",
        "aptox_badge_auto_stats",
    )

    suspend fun estimateTotalBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        var total = 0L
        total += databaseFilesLength(app, "aptox_usage.db")
        val prefsRoot = File(app.filesDir.parent ?: return@withContext total, "shared_prefs")
        for (name in BACKUP_SHARED_PREF_NAMES) {
            val f = File(prefsRoot, "$name.xml")
            if (f.isFile) total += f.length()
        }
        val dsDir = File(app.filesDir, "datastore")
        if (dsDir.isDirectory) {
            dsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".preferences_pb")) {
                    total += file.length()
                }
            }
        }
        total
    }
}

object BackupSizeFormatter {

    /** &lt; 1KB → "1KB 미만", 그 외 → "X.X MB" 또는 큰 값은 정수 MB */
    fun format(bytes: Long): String {
        if (bytes < 1024L) return "1KB 미만"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val text = if (mb < 10.0) {
            String.format(Locale.KOREA, "%.1f", mb)
        } else {
            String.format(Locale.KOREA, "%.0f", ceil(mb))
        }
        return "$text MB"
    }

}

private fun databaseFilesLength(context: Context, dbName: String): Long {
    val path = context.getDatabasePath(dbName)
    if (!path.exists()) return 0L
    var sum = path.length()
    val abs = path.absolutePath
    val wal = File("$abs-wal")
    val shm = File("$abs-shm")
    if (wal.isFile) sum += wal.length()
    if (shm.isFile) sum += shm.length()
    return sum
}
