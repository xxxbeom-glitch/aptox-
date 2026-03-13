package com.aptox.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * 23:55 리셋 예고 알림 스케줄링.
 * 일일사용량 제한 앱 중 내일도 동일 제한이 적용되는 앱에 대해 매일 23:55 알림 예약.
 */
object DailyUsageAlarmScheduler {

    private const val TAG = "DailyUsageAlarm"
    private const val REQUEST_CODE_2355 = 2355

    fun scheduleResetWarningIfNeeded(context: Context) {
        val repo = AppRestrictionRepository(context)
        val restrictions = repo.getAll()
            .filter { it.blockUntilMs <= 0 } // 일일사용량만
            .filter { isActiveTomorrow(it) }

        if (restrictions.isEmpty()) {
            cancelResetWarning(context)
            Log.d(TAG, "23:55 알림 스케줄 해제 (대상 없음)")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMs = next2355Millis()

        val packageNames = restrictions.map { it.packageName }.toTypedArray()
        val appNames = restrictions.map { it.appName }.toTypedArray()
        val intent = Intent(context, DailyUsageResetAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAMES, packageNames)
            putExtra(EXTRA_APP_NAMES, appNames)
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE_2355, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
            Log.d(TAG, "23:55 리셋 예고 알림 예약 (${restrictions.size}개 앱)")
        } catch (e: SecurityException) {
            Log.w(TAG, "정확 알람 권한 없음, setAndAllowWhileIdle 시도", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    fun cancelResetWarning(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyUsageResetAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE_2355, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }

    /** 다음 23:55 시각(ms). 이미 지났으면 내일 23:55 */
    private fun next2355Millis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 55)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var target = cal.timeInMillis
        if (target <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            target = cal.timeInMillis
        }
        return target
    }

    /** 내일도 동일 앱 제한이 적용되는지 */
    private fun isActiveTomorrow(r: com.aptox.app.model.AppRestriction): Boolean {
        if (r.repeatDays.isBlank()) return false // 오늘 하루만
        if (isDurationExpired(r.baselineTimeMs, r.durationWeeks)) return false
        val daySet = parseRepeatDays(r.repeatDays)
        if (daySet.isEmpty()) return false
        val todayIdx = todayDayIndex()
        val tomorrowIdx = (todayIdx + 1) % 7
        return tomorrowIdx in daySet
    }

    private fun parseRepeatDays(s: String): Set<Int> =
        s.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.toSet()

    private fun todayDayIndex(): Int {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 6 else dow - 2
    }

    private fun isDurationExpired(baselineTimeMs: Long, durationWeeks: Int): Boolean {
        if (durationWeeks <= 0) return false
        val endMs = baselineTimeMs + durationWeeks * 7L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() >= endMs
    }

    const val EXTRA_PACKAGE_NAMES = "package_names"
    const val EXTRA_APP_NAMES = "app_names"
}
