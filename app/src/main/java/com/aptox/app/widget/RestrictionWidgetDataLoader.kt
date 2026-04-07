package com.aptox.app.widget

import android.content.Context
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.DailyUsageLimitConstants
import com.aptox.app.ManualTimerRepository
import com.aptox.app.PauseRepository
import com.aptox.app.RestrictionDeleteHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 홈 위젯용 제한 앱 한 줄 상태 (하루 사용량 + 시간 지정).
 * [com.aptox.app.loadRestrictionItems]와 동일한 데이터 규칙을 따름.
 */
data class RestrictionWidgetRow(
    val packageName: String,
    val appName: String,
    val statusText: String,
    /** 제한 초과 / 제한 중 등 강조 (AppColors.TextHighlight 대응) */
    val statusUsesHighlightColor: Boolean,
)

private const val TIME_SPEC_ONE_DAY_MS = 24L * 60 * 60 * 1000

private fun todayDayIndex(): Int {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 6 else dow - 2
}

private fun parseRepeatDays(repeatDays: String): Set<Int> =
    repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.toSet()

private fun daysUntilNextRestriction(todayIdx: Int, restrictionDayIndices: Set<Int>): Int {
    if (todayIdx in restrictionDayIndices) return 0
    for (d in 1..7) {
        val idx = (todayIdx + d) % 7
        if (idx in restrictionDayIndices) return d
    }
    return 7
}

private fun isDurationExpired(baselineTimeMs: Long, durationWeeks: Int): Boolean {
    if (durationWeeks <= 0) return false
    val endMs = baselineTimeMs + durationWeeks * 7L * 24 * 60 * 60 * 1000
    return System.currentTimeMillis() >= endMs
}

private fun isTodayOnlyExpired(baselineTimeMs: Long): Boolean {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return baselineTimeMs < cal.timeInMillis
}

private fun rollToNextTimeSpecifiedWindow(startTimeMs: Long, blockUntilMs: Long, now: Long): Pair<Long, Long> {
    var s = startTimeMs
    var e = blockUntilMs
    while (e <= now) {
        s += TIME_SPEC_ONE_DAY_MS
        e += TIME_SPEC_ONE_DAY_MS
    }
    return s to e
}

private fun formatHm(timeMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(timeMs))

object RestrictionWidgetDataLoader {

    private const val MAX_ROWS = 24

    fun loadRows(context: Context): List<RestrictionWidgetRow> {
        val app = context.applicationContext
        val repo = AppRestrictionRepository(app)
        var restrictions = repo.getAll()
        if (restrictions.isEmpty()) return emptyList()

        val pauseRepo = PauseRepository(app)
        val todayIdx = todayDayIndex()
        val now = System.currentTimeMillis()

        val out = ArrayList<RestrictionWidgetRow>()

        for (orig in restrictions) {
            if (out.size >= MAX_ROWS) break
            val isTimeSpecified = orig.startTimeMs > 0
            if (isTimeSpecified) {
                val restriction = if (orig.blockUntilMs <= now) {
                    repo.renewExpiredTimeSpecifiedRestrictions()
                    repo.getAll().find { it.packageName == orig.packageName } ?: continue
                } else orig

                val isPaused = pauseRepo.isPaused(restriction.packageName)
                val (winStart, winEnd) = rollToNextTimeSpecifiedWindow(
                    restriction.startTimeMs,
                    restriction.blockUntilMs,
                    now,
                )
                val isInRestriction = !isPaused && now >= winStart && now < winEnd

                val (text, highlight) = when {
                    isPaused -> "일시정지 중" to false
                    isInRestriction -> "제한 중" to true
                    else -> "${formatHm(winStart)} 시작" to false
                }
                out.add(
                    RestrictionWidgetRow(
                        packageName = restriction.packageName,
                        appName = restriction.appName,
                        statusText = text,
                        statusUsesHighlightColor = highlight,
                    ),
                )
            } else {
                val repeatDaySet = parseRepeatDays(orig.repeatDays)
                if (repeatDaySet.isEmpty()) {
                    if (isTodayOnlyExpired(orig.baselineTimeMs)) {
                        RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                        continue
                    }
                } else {
                    if (isDurationExpired(orig.baselineTimeMs, orig.durationWeeks)) {
                        RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                        continue
                    }
                }

                val timerRepo = ManualTimerRepository(app)
                val usageMs = timerRepo.getTodayUsageMs(orig.packageName)
                val isDailyUnlimited =
                    orig.limitMinutes >= DailyUsageLimitConstants.UNLIMITED_MINUTES_SENTINEL
                val limitMs =
                    if (isDailyUnlimited) Long.MAX_VALUE / 4 else orig.limitMinutes * 60L * 1000L
                val remainingMs = (limitMs - usageMs).coerceAtLeast(0)
                val isCountActive = timerRepo.isSessionActive(orig.packageName)

                val isEveryDay = repeatDaySet.size == 7
                val daysUntil =
                    if (!isEveryDay && repeatDaySet.isNotEmpty()) daysUntilNextRestriction(todayIdx, repeatDaySet) else 0

                val (text, highlight) = when {
                    daysUntil > 0 -> "${daysUntil}일 후 예정" to false
                    isDailyUnlimited -> when {
                        isCountActive -> "이용 중" to false
                        else -> "제한 없음" to false
                    }
                    remainingMs <= 0 && isCountActive -> "제한 초과" to true
                    remainingMs <= 0 && !isCountActive -> "오늘 완료" to false
                    else -> {
                        val remainingMin = (remainingMs / 60_000).toInt().coerceAtLeast(1)
                        "${remainingMin}분 남음" to false
                    }
                }

                out.add(
                    RestrictionWidgetRow(
                        packageName = orig.packageName,
                        appName = orig.appName,
                        statusText = text,
                        statusUsesHighlightColor = highlight,
                    ),
                )
            }
        }

        return out
    }
}
