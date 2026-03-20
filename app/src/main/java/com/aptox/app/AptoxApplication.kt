package com.aptox.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aptox.app.usage.UsageStatsSyncWorker
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class AptoxApplication : Application() {

    /** 배지·백그라운드 Firestore 작업용 (UI 블로킹 방지) */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (defaultHandler !is AptoxUncaughtExceptionHandler) {
            Thread.setDefaultUncaughtExceptionHandler(
                AptoxUncaughtExceptionHandler(this, defaultHandler),
            )
        }
        scheduleUsageStatsSync()
        // 미로그인 상태에서 제한만 저장한 뒤 로그인하면 badge_001 등 Firestore 배지를 줄 수 있게 함
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                BadgeAutoGrant.onUserSignedInTryBadge001(this)
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    companion object {
        private const val TAG = "AptoxApplication"

        /** 포그라운드 상태에서 호출. 앱 실행 시 Application.onCreate는 백그라운드로 인식될 수 있어 MainActivity.onResume에서 호출 */
        fun startAppMonitorIfNeeded(context: android.content.Context) {
            try {
                ManualTimerRepository(context).ensureMidnightResetIfNeeded()
                DailyUsageMidnightResetScheduler.scheduleNextMidnight(context)
                val repo = AppRestrictionRepository(context)
                val map = repo.toRestrictionMap()
                if (map.isNotEmpty()) {
                    AppMonitorService.start(context, map)
                }
                DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(context)
                WeeklyReportAlarmScheduler.applySchedule(context, NotificationPreferences.isWeeklyReportEnabled(context))
            } catch (e: Throwable) {
                Log.e(TAG, "AppMonitor 시작 실패", e)
            }
        }
    }

    private fun scheduleUsageStatsSync() {
        val workManager = WorkManager.getInstance(this)
        val dailyRequest = PeriodicWorkRequestBuilder<UsageStatsSyncWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayTo0010(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "usage_stats_daily_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest,
        )
        if (StatisticsData.hasUsageAccess(this)) {
            val initialRequest = androidx.work.OneTimeWorkRequestBuilder<UsageStatsSyncWorker>()
                .setInputData(androidx.work.workDataOf(UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
                .build()
            workManager.enqueueUniqueWork(
                "usage_stats_initial_sync",
                androidx.work.ExistingWorkPolicy.KEEP,
                initialRequest,
            )
        }
    }

    /** 00:10 KST까지의 초기 지연 시간(ms) */
    private fun computeInitialDelayTo0010(): Long {
        val cal = java.util.Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 10)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        var target = cal.timeInMillis
        if (target <= now) target += 24 * 60 * 60 * 1000L
        return target - now
    }

}
