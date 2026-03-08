package com.cole.app.usage

/**
 * 앱별 일별 사용량. 기기 UsageStats가 ~7일 후 삭제되기 전에 Worker가 저장.
 * @param date yyyyMMdd (예: 20250308)
 */
data class DailyUsageEntity(
    val date: String,
    val packageName: String,
    val usageMs: Long,
    val sessionCount: Int,
)
