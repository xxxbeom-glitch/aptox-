package com.aptox.app.backup

/**
 * 로컬 ZIP 백업 포맷 (보내기·가져오기 공통).
 * CSV 헤더는 정확히 일치해야 해당 섹션을 복원한다.
 */
object LocalBackupFormat {

    const val FORMAT_VERSION: Int = 1

    const val ENTRY_MANIFEST = "manifest.json"
    const val ENTRY_RESTRICTIONS = "restrictions.csv"
    const val ENTRY_DAILY_USAGE = "daily_usage.csv"
    const val ENTRY_CATEGORY_DAILY = "category_daily.csv"
    const val ENTRY_TIME_SEGMENTS = "time_segment_daily.csv"
    const val ENTRY_NOTIFICATION_PREFS = "notification_prefs.csv"
    const val ENTRY_BADGE_STATS = "badge_stats.csv"
    const val ENTRY_PENDING_BADGES = "pending_badges.csv"
    const val ENTRY_USER_BADGES = "user_badges.csv"

    val HEADER_RESTRICTIONS = listOf(
        "packageName", "appName", "limitMinutes", "blockUntilMs", "baselineTimeMs",
        "repeatDays", "durationWeeks", "startTimeMs",
    )

    val HEADER_DAILY_USAGE = listOf("date", "packageName", "usageMs", "sessionCount")

    val HEADER_CATEGORY_DAILY = listOf("date", "category", "usageMs")

    val HEADER_TIME_SEGMENT = listOf(
        "date", "packageName",
        "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
    )

    val HEADER_NOTIFICATION_PREFS = listOf("key", "value")

    val HEADER_BADGE_STATS = listOf("key", "value")

    val HEADER_PENDING_BADGES = listOf("badgeId")

    val HEADER_USER_BADGES = listOf("badgeId", "achievedAtMs")

    /** UI·스낵바용 섹션 라벨 */
    object SectionLabels {
        const val MANIFEST = "백업 정보"
        const val RESTRICTIONS = "앱 제한 설정"
        const val USAGE = "일별 사용량 통계"
        const val CATEGORY = "카테고리 통계"
        const val TIME_SEGMENT = "시간대별 사용량"
        const val NOTIFICATION = "알림 설정"
        const val BADGE_STATS = "뱃지 진행 기록"
        const val PENDING_BADGES = "뱃지 대기 목록"
        const val USER_BADGES = "획득 뱃지(계정)"
    }
}
