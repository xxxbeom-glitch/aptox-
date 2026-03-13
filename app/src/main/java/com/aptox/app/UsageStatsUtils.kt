package com.aptox.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * UsageStats 기반 사용량 계산 유틸.
 * - baselineTimeMs > 0: 해당 시각 이후 사용량만 반영 (제한 추가 시점부터)
 * - baselineTimeMs <= 0: 당일 전체 사용량 (레거시)
 */
object UsageStatsUtils {

    /**
     * 일일 사용량 제한용: 자정(00:00) 리셋.
     * baseline이 오늘 이전이면 오늘 00:00을 사용해 당일 사용량만 카운트.
     * 앱이 백그라운드/종료 상태에서 자정 넘어갔을 때도 리셋 적용.
     */
    @JvmStatic
    fun getEffectiveBaselineForDailyUsage(baselineTimeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMidnight = cal.timeInMillis
        return maxOf(baselineTimeMs, todayMidnight)
    }

    /**
     * 일일 사용량 제한용 사용량(ms). 자정 기준 리셋 적용.
     */
    @JvmStatic
    fun getDailyUsageLimitMs(
        usm: UsageStatsManager,
        packageName: String,
        baselineTimeMs: Long,
        packagesWithVisibleWindow: Set<String>? = null,
    ): Long {
        val effectiveBaseline = getEffectiveBaselineForDailyUsage(baselineTimeMs)
        return getUsageSinceBaselineMs(usm, packageName, effectiveBaseline, packagesWithVisibleWindow)
    }

    /** 자정 이전에 시작해 자정을 넘긴 세션을 잡기 위한 쿼리 구간 확장(25시간) */
    private const val PRE_BASELINE_QUERY_MS = 25L * 60 * 60 * 1000

    @JvmStatic
    fun getUsageSinceBaselineMs(
        usm: UsageStatsManager,
        packageName: String,
        baselineTimeMs: Long,
        packagesWithVisibleWindow: Set<String>? = null,
    ): Long {
        if (baselineTimeMs <= 0) {
            return getTodayUsageMsLegacy(usm, packageName)
        }
        val now = System.currentTimeMillis()
        // 자정 넘어가는 엣지케이스: 23:50에 앱 켜고 00:20까지 사용 시, 00:00~00:20 구간만 카운트해야 함.
        // queryEvents(baseline, now)만 쓰면 23:50의 MOVE_TO_FOREGROUND를 못 보므로, 구간을 과거로 확장
        val queryStart = maxOf(0, baselineTimeMs - PRE_BASELINE_QUERY_MS)
        val events = usm.queryEvents(queryStart, now) ?: return 0
        val hasVisibleWindow = packagesWithVisibleWindow?.contains(packageName) == true

        var totalMs = 0L
        var sessionStartMs = -1L
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (packageName != event.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // PiP 복귀 시: sessionStartMs가 이미 설정돼 있으면 기존 세션 유지 (새 세션으로 취급하지 않음)
                    if (sessionStartMs < 0) {
                        sessionStartMs = event.timeStamp
                    }
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (sessionStartMs >= 0) {
                        val start = maxOf(baselineTimeMs, sessionStartMs)
                        val end = event.timeStamp
                        if (end > start) totalMs += (end - start)
                        sessionStartMs = if (hasVisibleWindow) event.timeStamp else -1
                    }
                }
            }
        }

        // 아직 포그라운드/PiP에 있는 세션
        if (sessionStartMs >= 0) {
            val start = maxOf(baselineTimeMs, sessionStartMs)
            if (now > start) totalMs += (now - start)
        }

        return totalMs
    }

    /** 당일 0시 ~ 현재 사용량 (baseline 미적용) */
    @JvmStatic
    fun getTodayUsageMsLegacy(usm: UsageStatsManager, packageName: String): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis(),
        ) ?: return 0
        return stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
    }

    /** baseline 적용된 사용량(분). Kotlin에서 convenience */
    fun getUsageSinceBaselineMinutes(
        usm: UsageStatsManager,
        packageName: String,
        baselineTimeMs: Long,
        packagesWithVisibleWindow: Set<String>? = null,
    ): Long = getUsageSinceBaselineMs(usm, packageName, baselineTimeMs, packagesWithVisibleWindow) / 60_000

    fun getUsageSinceBaselineMinutes(
        context: Context,
        packageName: String,
        baselineTimeMs: Long,
        packagesWithVisibleWindow: Set<String>? = null,
    ): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        return getUsageSinceBaselineMinutes(usm, packageName, baselineTimeMs, packagesWithVisibleWindow)
    }
}
