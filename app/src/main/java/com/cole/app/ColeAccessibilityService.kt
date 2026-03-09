package com.cole.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * 앱 차단을 위한 접근성 서비스.
 * 설정 > 접근성 > 설치된 앱에서 활성화할 수 있으며,
 * 포그라운드 앱 변경 감지 등 앱 차단 기능 보조에 사용됩니다.
 */
class ColeAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        // 제한 앱 목록에 있고 차단 대상이면 BlockOverlayService 트리거 (UsageStats 보완)
        val repo = AppRestrictionRepository(this)
        val restriction = repo.getAll().find { it.packageName == pkg } ?: return

        val pauseRepo = PauseRepository(this)
        if (pauseRepo.isPaused(pkg)) return

        val shouldBlock = if (restriction.blockUntilMs > 0) {
            System.currentTimeMillis() < restriction.blockUntilMs
        } else {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return
            val limitMs = restriction.limitMinutes * 60L * 1000L
            val todayUsage = getTodayUsageMs(usm, pkg)
            todayUsage >= limitMs
        }

        if (shouldBlock && !BlockOverlayService.isRunning) {
            val intent = Intent(this, BlockOverlayService::class.java).apply {
                putExtra(BlockOverlayService.EXTRA_PACKAGE_NAME, pkg)
                putExtra(BlockOverlayService.EXTRA_BLOCK_UNTIL_MS, restriction.blockUntilMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun getTodayUsageMs(usm: android.app.usage.UsageStatsManager, packageName: String): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis(),
        ) ?: return 0
        return stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
    }

    override fun onInterrupt() {}
}
