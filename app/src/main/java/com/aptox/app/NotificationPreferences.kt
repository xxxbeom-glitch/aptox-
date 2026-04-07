package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 알림 설정 SharedPreferences 저장.
 * - 각 토글 상태 저장
 * - 앱 재시작 후에도 유지
 */
object NotificationPreferences {

    private const val PREFS_NAME = "aptox_notification_prefs"
    private const val KEY_DEADLINE_IMMINENT = "deadline_imminent_enabled"
    private const val KEY_COUNT_REMINDER = "count_reminder_enabled"
    private const val KEY_BADGE_ACQUIRED = "badge_acquired_enabled"
    private const val KEY_TIME_SPECIFIED_START = "time_specified_start_enabled"
    private const val KEY_TIME_SPECIFIED_END = "time_specified_end_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDeadlineImminentEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEADLINE_IMMINENT, true)

    fun setDeadlineImminentEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEADLINE_IMMINENT, enabled).apply()
    }

    fun isCountReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COUNT_REMINDER, true)

    fun setCountReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COUNT_REMINDER, enabled).apply()
    }

    fun isBadgeAcquiredEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BADGE_ACQUIRED, true)

    fun setBadgeAcquiredEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BADGE_ACQUIRED, enabled).apply()
    }

    fun isTimeSpecifiedStartEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TIME_SPECIFIED_START, true)

    fun setTimeSpecifiedStartEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TIME_SPECIFIED_START, enabled).apply()
    }

    fun isTimeSpecifiedEndEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TIME_SPECIFIED_END, true)

    fun setTimeSpecifiedEndEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TIME_SPECIFIED_END, enabled).apply()
    }

    /**
     * goal_achieved_enabled → badge_acquired_enabled 키 마이그레이션.
     * 앱 시작 시 1회 호출. 기존 키가 존재하면 값을 새 키로 복사 후 삭제.
     */
    fun migrateIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_LEGACY_GOAL_ACHIEVED)) {
            val legacyValue = p.getBoolean(KEY_LEGACY_GOAL_ACHIEVED, true)
            p.edit()
                .putBoolean(KEY_BADGE_ACQUIRED, legacyValue)
                .remove(KEY_LEGACY_GOAL_ACHIEVED)
                .apply()
        }
    }

    private const val KEY_LEGACY_GOAL_ACHIEVED = "goal_achieved_enabled"

    /** 로컬 백업용: (키, "true"/"false") */
    fun exportKeyValuePairs(context: Context): List<Pair<String, String>> = listOf(
        KEY_DEADLINE_IMMINENT to isDeadlineImminentEnabled(context).toString(),
        KEY_COUNT_REMINDER to isCountReminderEnabled(context).toString(),
        KEY_BADGE_ACQUIRED to isBadgeAcquiredEnabled(context).toString(),
        KEY_TIME_SPECIFIED_START to isTimeSpecifiedStartEnabled(context).toString(),
        KEY_TIME_SPECIFIED_END to isTimeSpecifiedEndEnabled(context).toString(),
    )

    /** 백업 복원: 알 수 없는 키는 무시. 값이 true/false가 아니면 해당 키 스킵. */
    fun importKeyValuePairs(context: Context, pairs: List<Pair<String, String>>): Boolean {
        val ed = prefs(context).edit()
        var any = false
        for ((key, raw) in pairs) {
            val v = raw.trim().lowercase()
            val b = when (v) {
                "true", "1" -> true
                "false", "0" -> false
                else -> continue
            }
            when (key) {
                KEY_DEADLINE_IMMINENT -> ed.putBoolean(KEY_DEADLINE_IMMINENT, b)
                KEY_COUNT_REMINDER -> ed.putBoolean(KEY_COUNT_REMINDER, b)
                KEY_BADGE_ACQUIRED -> ed.putBoolean(KEY_BADGE_ACQUIRED, b)
                KEY_TIME_SPECIFIED_START -> ed.putBoolean(KEY_TIME_SPECIFIED_START, b)
                KEY_TIME_SPECIFIED_END -> ed.putBoolean(KEY_TIME_SPECIFIED_END, b)
                else -> continue
            }
            any = true
        }
        if (any) ed.commit() else return false
        TimeSpecifiedRestrictionNotificationHelper.syncChannelsWithPreferences(context.applicationContext)
        return true
    }
}
