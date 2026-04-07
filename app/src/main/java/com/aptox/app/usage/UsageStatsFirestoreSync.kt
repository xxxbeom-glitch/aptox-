package com.aptox.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.aptox.app.StatisticsData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UsageStats → 로컬 DB → Firestore 업로드.
 * [UsageStatsSyncWorker]와 수동 "서버 백업" 버튼에서 공통 사용.
 */
object UsageStatsFirestoreSync {

    suspend fun sync(context: Context, initialSync: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!StatisticsData.hasUsageAccess(context)) {
            return@withContext Result.failure(IllegalStateException("usage_stats_denied"))
        }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext Result.failure(IllegalStateException("no_usage_manager"))
        val db = AppDatabaseProvider.get(context)
        val userPackages = UsageStatsDaySyncUtils.getUserInstalledPackages(context)

        val daysToSync = if (initialSync) {
            (1..7).map { daysAgo -> UsageStatsDateUtils.daysAgoToYyyyMmDd(daysAgo) }
        } else {
            listOf(UsageStatsDateUtils.daysAgoToYyyyMmDd(1))
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val statsRepo = StatisticsBackupFirestoreRepository()

        for (dateStr in daysToSync) {
            val (startMs, endMs) = UsageStatsDateUtils.yyyyMmDdToRange(dateStr)
            val entities = UsageStatsDaySyncUtils.aggregateDailyUsageFromEvents(
                usm, startMs, endMs, userPackages, dateStr,
            )
            if (entities.isEmpty()) continue

            db.insertAll(entities)

            val categoryRows = UsageStatsDaySyncUtils.buildCategoryStatsForDay(context, entities)
            db.replaceCategoryStatsForDay(dateStr, categoryRows)

            val segmentRows = UsageStatsDaySyncUtils.aggregateTimeSegmentSlotsFromEvents(
                usm, startMs, endMs, userPackages, dateStr,
            )
            db.replaceTimeSegmentsForDay(dateStr, segmentRows)

            uid?.let { userId ->
                runCatching { DailyUsageFirestoreRepository().uploadDailyUsage(userId, entities) }
                runCatching { statsRepo.uploadCategoryStatsForDay(userId, categoryRows) }
                runCatching { statsRepo.uploadTimeSegmentsForDay(userId, segmentRows) }
            }
        }
        Result.success(Unit)
    }
}
