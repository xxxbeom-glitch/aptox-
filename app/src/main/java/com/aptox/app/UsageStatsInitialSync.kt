package com.aptox.app

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aptox.app.usage.UsageStatsSyncWorker

/**
 * 최초 설치·온보딩 직후 Room 백필용 7일 UsageStatsSyncWorker 스케줄.
 * - 부팅 시 권한 없으면 플래그만 세우고, 권한 생긴 뒤 [flushPendingInitialSyncIfNeeded] / [enqueueInitial7DayIfPermitted]로 처리.
 * - `usage_stats_initial_sync`는 [ExistingWorkPolicy.KEEP]으로 중복 실행 방지.
 */
object UsageStatsInitialSync {

    private const val PREFS = "aptox_usage_stats_sync"
    private const val KEY_PENDING_INITIAL = "pending_initial_sync_no_permission_at_boot"
    const val UNIQUE_WORK_NAME = "usage_stats_initial_sync"

    /** [Application.onCreate]에서만: 권한 있으면 즉시 워커, 없으면 나중에 flush 할 플래그. */
    fun onApplicationColdStart(app: android.app.Application) {
        if (StatisticsData.hasUsageAccess(app)) {
            enqueueInitial7DayWorker(app)
            clearPendingFlag(app)
        } else {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PENDING_INITIAL, true)
                .apply()
        }
    }

    fun clearPendingFlag(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING_INITIAL, false)
            .apply()
    }

    private fun enqueueInitial7DayWorker(context: Context) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        val request = OneTimeWorkRequestBuilder<UsageStatsSyncWorker>()
            .setInputData(workDataOf(UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
            .build()
        wm.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** 사용 통계 권한이 있을 때만 7일 초기 동기화 워커 등록(중복 KEEP). */
    fun enqueueInitial7DayIfPermitted(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        enqueueInitial7DayWorker(context)
        clearPendingFlag(context.applicationContext)
    }

    /** 부팅 시 권한 없어 스킵된 경우, 메인·온보딩 완료 후 권한이 생기면 1회 보정. */
    fun flushPendingInitialSyncIfNeeded(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PENDING_INITIAL, false)) return
        if (!StatisticsData.hasUsageAccess(app)) return
        enqueueInitial7DayWorker(app)
        clearPendingFlag(app)
    }
}
