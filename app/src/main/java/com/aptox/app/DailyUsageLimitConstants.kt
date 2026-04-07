package com.aptox.app

/**
 * 일일 사용량 "제한 없음" 선택 시 저장되는 [AppRestriction.limitMinutes] sentinel.
 * [AppMonitorService]에서 이 값 이상이면 일일 한도 초과 차단·알림을 적용하지 않음.
 */
object DailyUsageLimitConstants {
    const val UNLIMITED_MINUTES_SENTINEL: Int = 1_000_000
}
