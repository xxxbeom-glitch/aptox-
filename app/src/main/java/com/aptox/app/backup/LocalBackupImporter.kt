package com.aptox.app.backup

import android.content.Context
import android.net.Uri
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.BadgeRepository
import com.aptox.app.BadgeStatsPreferences
import com.aptox.app.NotificationPreferences
import com.aptox.app.PendingBadgesPreferences
import com.aptox.app.model.AppRestriction
import com.aptox.app.usage.AppDatabaseProvider
import com.aptox.app.usage.DailyCategoryStatEntity
import com.aptox.app.usage.DailyTimeSegmentEntity
import com.aptox.app.usage.DailyUsageEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * ZIP 백업 복원 ([LocalBackupExporter]와 동일 엔트리명·CSV 헤더).
 */
object LocalBackupImporter {

    suspend fun restoreFromUri(context: Context, uri: Uri): LocalBackupRestoreResult =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val input = app.contentResolver.openInputStream(uri)
                ?: return@withContext LocalBackupRestoreResult(
                    success = false,
                    failedSectionLabels = emptyList(),
                    fatalMessage = "파일을 열 수 없어요",
                )
            input.use { restoreFromZipStream(app, it) }
        }

    private suspend fun restoreFromZipStream(app: Context, inputStream: InputStream): LocalBackupRestoreResult {
        val failed = mutableListOf<String>()
        val map = readZipToMap(inputStream)

        val manifestBytes = map.findBytes(LocalBackupFormat.ENTRY_MANIFEST)
            ?: return LocalBackupRestoreResult(
                success = false,
                failedSectionLabels = listOf(LocalBackupFormat.SectionLabels.MANIFEST),
                fatalMessage = "백업 정보(manifest)가 없어요",
            )
        val manifestOk = parseManifest(manifestBytes)
        if (!manifestOk) {
            return LocalBackupRestoreResult(
                success = false,
                failedSectionLabels = listOf(LocalBackupFormat.SectionLabels.MANIFEST),
                fatalMessage = "지원하지 않는 백업 버전이에요",
            )
        }

        val restrictionsResult = parseRestrictions(map.findBytes(LocalBackupFormat.ENTRY_RESTRICTIONS))
        val dailyResult = parseDailyUsage(map.findBytes(LocalBackupFormat.ENTRY_DAILY_USAGE))
        val catResult = parseCategory(map.findBytes(LocalBackupFormat.ENTRY_CATEGORY_DAILY))
        val segResult = parseSegments(map.findBytes(LocalBackupFormat.ENTRY_TIME_SEGMENTS))
        val notifResult = parseNotificationPrefs(map.findBytes(LocalBackupFormat.ENTRY_NOTIFICATION_PREFS))
        val badgeStatsResult = parseBadgeStats(map.findBytes(LocalBackupFormat.ENTRY_BADGE_STATS))
        val pendingResult = parsePendingBadges(map.findBytes(LocalBackupFormat.ENTRY_PENDING_BADGES))
        val userBadgesResult = parseUserBadges(map.findBytes(LocalBackupFormat.ENTRY_USER_BADGES))

        if (restrictionsResult.failed) failed.add(LocalBackupFormat.SectionLabels.RESTRICTIONS)
        if (dailyResult.failed) failed.add(LocalBackupFormat.SectionLabels.USAGE)
        if (catResult.failed) failed.add(LocalBackupFormat.SectionLabels.CATEGORY)
        if (segResult.failed) failed.add(LocalBackupFormat.SectionLabels.TIME_SEGMENT)
        if (notifResult.failed) failed.add(LocalBackupFormat.SectionLabels.NOTIFICATION)
        if (badgeStatsResult.failed) failed.add(LocalBackupFormat.SectionLabels.BADGE_STATS)
        if (pendingResult.failed) failed.add(LocalBackupFormat.SectionLabels.PENDING_BADGES)
        if (userBadgesResult.failed) failed.add(LocalBackupFormat.SectionLabels.USER_BADGES)

        try {
            if (restrictionsResult.replace) {
                AppRestrictionRepository(app).replaceAll(restrictionsResult.rows)
            }
            AppDatabaseProvider.get(app).restoreUsageTablesSelective(
                replaceDaily = dailyResult.replace,
                dailyRows = dailyResult.rows,
                replaceCategory = catResult.replace,
                categoryRows = catResult.rows,
                replaceSegment = segResult.replace,
                segmentRows = segResult.rows,
            )
            if (notifResult.replace) {
                NotificationPreferences.importKeyValuePairs(app, notifResult.pairs)
            }
            if (badgeStatsResult.replace) {
                BadgeStatsPreferences.restoreEntriesFromBackup(app, badgeStatsResult.pairs)
            }
            if (pendingResult.replace) {
                PendingBadgesPreferences.setPendingBadges(app, pendingResult.ids.toSet())
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null && userBadgesResult.replace && userBadgesResult.rows.isNotEmpty()) {
                val repo = BadgeRepository(context = null)
                for ((bid, at) in userBadgesResult.rows) {
                    repo.restoreEarnedBadgeFromBackup(uid, bid, at)
                }
            }
        } catch (e: Exception) {
            return LocalBackupRestoreResult(
                success = false,
                failedSectionLabels = failed + "저장",
                fatalMessage = e.message ?: "복원 중 오류가 났어요",
            )
        }

        val anyApplied = restrictionsResult.replace || dailyResult.replace || catResult.replace ||
            segResult.replace || notifResult.replace || badgeStatsResult.replace ||
            pendingResult.replace ||
            (FirebaseAuth.getInstance().currentUser?.uid != null && userBadgesResult.replace && userBadgesResult.rows.isNotEmpty())
        if (!anyApplied) {
            return LocalBackupRestoreResult(
                success = false,
                failedSectionLabels = failed.distinct(),
                fatalMessage = "복원할 데이터가 없어요",
            )
        }
        return LocalBackupRestoreResult(
            success = true,
            failedSectionLabels = failed.distinct(),
            fatalMessage = null,
        )
    }

    private fun readZipToMap(inputStream: InputStream): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val name = entry.name.substringAfterLast('/')
                    out[name] = zis.readBytes()
                }
                zis.closeEntry()
            }
        }
        return out
    }

    private fun Map<String, ByteArray>.findBytes(fileName: String): ByteArray? =
        this[fileName] ?: entries.firstOrNull { it.key.equals(fileName, ignoreCase = true) }?.value

    private fun parseManifest(bytes: ByteArray): Boolean = try {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        json.optInt("formatVersion", -1) == LocalBackupFormat.FORMAT_VERSION
    } catch (_: Exception) {
        false
    }

    private data class RParse(val replace: Boolean, val failed: Boolean, val rows: List<AppRestriction>)
    private fun parseRestrictions(bytes: ByteArray?): RParse {
        if (bytes == null) return RParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return RParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_RESTRICTIONS)) {
            return RParse(false, true, emptyList())
        }
        val rows = mutableListOf<AppRestriction>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 8) return RParse(false, true, emptyList())
            rows.add(
                AppRestriction(
                    packageName = p[0],
                    appName = p[1],
                    limitMinutes = p[2].toIntOrNull() ?: return RParse(false, true, emptyList()),
                    blockUntilMs = p[3].toLongOrNull() ?: 0L,
                    baselineTimeMs = p[4].toLongOrNull() ?: 0L,
                    repeatDays = p[5],
                    durationWeeks = p[6].toIntOrNull() ?: 0,
                    startTimeMs = p[7].toLongOrNull() ?: 0L,
                ),
            )
        }
        return RParse(true, false, rows)
    }

    private data class DParse(val replace: Boolean, val failed: Boolean, val rows: List<DailyUsageEntity>)
    private fun parseDailyUsage(bytes: ByteArray?): DParse {
        if (bytes == null) return DParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return DParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_DAILY_USAGE)) {
            return DParse(false, true, emptyList())
        }
        val rows = mutableListOf<DailyUsageEntity>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 4) continue
            val date = p[0]
            val pkg = p[1]
            val ms = p[2].toLongOrNull() ?: continue
            val sc = p[3].toIntOrNull() ?: continue
            if (date.length != 8) continue
            rows.add(DailyUsageEntity(date, pkg, ms, sc))
        }
        return DParse(true, false, rows)
    }

    private data class CParse(val replace: Boolean, val failed: Boolean, val rows: List<DailyCategoryStatEntity>)
    private fun parseCategory(bytes: ByteArray?): CParse {
        if (bytes == null) return CParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_CATEGORY_DAILY)) {
            return CParse(false, true, emptyList())
        }
        val rows = mutableListOf<DailyCategoryStatEntity>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 3) continue
            val ms = p[2].toLongOrNull() ?: continue
            if (p[0].length != 8) continue
            rows.add(DailyCategoryStatEntity(p[0], p[1], ms))
        }
        return CParse(true, false, rows)
    }

    private data class SParse(val replace: Boolean, val failed: Boolean, val rows: List<DailyTimeSegmentEntity>)
    private fun parseSegments(bytes: ByteArray?): SParse {
        if (bytes == null) return SParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return SParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_TIME_SEGMENT)) {
            return SParse(false, true, emptyList())
        }
        val rows = mutableListOf<DailyTimeSegmentEntity>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 2 + DailyTimeSegmentEntity.TIME_SEGMENT_SLOT_COUNT) continue
            val date = p[0]
            val pkg = p[1]
            if (date.length != 8) continue
            val slots = LongArray(DailyTimeSegmentEntity.TIME_SEGMENT_SLOT_COUNT) { idx ->
                p[2 + idx].toLongOrNull() ?: 0L
            }
            rows.add(DailyTimeSegmentEntity(date, pkg, slots))
        }
        return SParse(true, false, rows)
    }

    private data class NParse(val replace: Boolean, val failed: Boolean, val pairs: List<Pair<String, String>>)
    private fun parseNotificationPrefs(bytes: ByteArray?): NParse {
        if (bytes == null) return NParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return NParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_NOTIFICATION_PREFS)) {
            return NParse(false, true, emptyList())
        }
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 2) continue
            pairs.add(p[0] to p[1])
        }
        return NParse(true, false, pairs)
    }

    private data class BParse(val replace: Boolean, val failed: Boolean, val pairs: List<Pair<String, String>>)
    private fun parseBadgeStats(bytes: ByteArray?): BParse {
        if (bytes == null) return BParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return BParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_BADGE_STATS)) {
            return BParse(false, true, emptyList())
        }
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 2) continue
            pairs.add(p[0] to p[1])
        }
        return BParse(true, false, pairs)
    }

    private data class PParse(val replace: Boolean, val failed: Boolean, val ids: List<String>)
    private fun parsePendingBadges(bytes: ByteArray?): PParse {
        if (bytes == null) return PParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return PParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_PENDING_BADGES)) {
            return PParse(false, true, emptyList())
        }
        val ids = mutableListOf<String>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.isNotEmpty() && p[0].isNotBlank()) ids.add(p[0].trim())
        }
        return PParse(true, false, ids)
    }

    private data class UParse(val replace: Boolean, val failed: Boolean, val rows: List<Pair<String, Long>>)
    private fun parseUserBadges(bytes: ByteArray?): UParse {
        if (bytes == null) return UParse(false, false, emptyList())
        val lines = bytes.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return UParse(false, true, emptyList())
        val header = LocalBackupCsv.parseHeaderLine(lines.first())
        if (!LocalBackupCsv.headersMatch(header, LocalBackupFormat.HEADER_USER_BADGES)) {
            return UParse(false, true, emptyList())
        }
        val rows = mutableListOf<Pair<String, Long>>()
        for (i in 1 until lines.size) {
            val p = LocalBackupCsv.parseLine(lines[i])
            if (p.size < 2) continue
            val at = p[1].toLongOrNull() ?: continue
            if (p[0].isBlank()) continue
            rows.add(p[0].trim() to at)
        }
        return UParse(true, false, rows)
    }
}
