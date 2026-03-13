package com.aptox.app

/**
 * 테스트 설정용 디버그 오버라이드 (DEBUG 빌드에서만 사용)
 * - 제한앱 모두 삭제: 직접 호출
 * - 구독 상태: SubscriptionManager.debugForceSubscribed
 * - 오늘 사용량 강제: loadRestrictionItems에서 사용
 * - 주간 챌린지 달성 일수: StatisticsScreen에서 사용
 */
object DebugTestSettings {

    /** 오늘 사용량 강제 설정 (분). null이면 실제 UsageStats 사용 */
    var debugTodayUsageMinutes: Long? = null

    /** 주간 챌린지 달성 일수 강제 설정. null이면 기본값 사용 */
    var debugWeeklyChallengeDays: Int? = null

    /** 알림내역 테스트용 갯수. null이면 0개 (빈 상태). 0, 3, 5, 10 중 선택 */
    var debugNotificationHistoryCount: Int? = null
}
