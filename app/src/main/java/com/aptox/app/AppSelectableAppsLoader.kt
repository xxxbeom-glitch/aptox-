package com.aptox.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.usage.UsageStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 제한 앱 추가 바텀시트용: 런처 앱 + 30일 사용 1분 이상 필터 + 7일 사용량 정렬.
 * [AddAppSelectBottomSheet]와 동일 로직 — 한 곳에서 유지해 스플래시 프리로드와 목록이 일치하도록 함.
 */
data class SelectableAppPickerItem(
    val name: String,
    val packageName: String,
)

/**
 * 프로세스 생명주기 동안만 유지. 스플래시·온보딩 완료·메인 진입 시 워밍.
 */
object AppSelectableAppsCache {
    @Volatile
    private var snapshot: List<SelectableAppPickerItem>? = null

    /** null 이면 아직 이번 세션에서 워밍 안 됨 */
    fun getIfWarmed(): List<SelectableAppPickerItem>? = snapshot

    fun isWarmed(): Boolean = snapshot != null

    fun set(items: List<SelectableAppPickerItem>) {
        snapshot = items
    }

    fun clear() {
        snapshot = null
    }
}

object AppSelectableAppsLoader {

    /**
     * 사용 통계 접근이 없으면 빈 목록(호출 측에서 캐시에 넣지 말 것).
     */
    suspend fun load(context: Context): List<SelectableAppPickerItem> = withContext(Dispatchers.Default) {
        val app = context.applicationContext
        if (!StatisticsData.hasUsageAccess(app)) return@withContext emptyList()
        runCatching { loadInternal(app) }.getOrElse { emptyList() }
    }

    private fun loadInternal(context: Context): List<SelectableAppPickerItem> {
        val pm = context.packageManager
        val selfPackageName = context.packageName
        val endTime = System.currentTimeMillis()
        val start7d = endTime - 7L * 24 * 60 * 60 * 1000L
        val start30d = endTime - 30L * 24 * 60 * 60 * 1000L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        fun usageForegroundMs(startMs: Long, endMs: Long): Map<String, Long> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                usageStatsManager
                    .queryAndAggregateUsageStats(startMs, endMs)
                    .mapValues { (_, u) -> u.totalTimeInForeground }
            } else {
                @Suppress("DEPRECATION")
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    startMs,
                    endMs,
                ) ?: emptyList()
                stats
                    .groupBy { it.packageName }
                    .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
            }

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        val launcherPackages = resolves
            .mapNotNull { runCatching { it.activityInfo.packageName }.getOrNull() }
            .filter { it != selfPackageName }
            .toSet()

        val usage30d = usageForegroundMs(start30d, endTime)
        val min30dForegroundMs = 60_000L
        val eligiblePackages = launcherPackages.filter { pkg ->
            (usage30d[pkg] ?: 0L) >= min30dForegroundMs
        }

        val usage7d = usageForegroundMs(start7d, endTime)
        return eligiblePackages
            .mapNotNull { pkg ->
                runCatching {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkg, 0)
                    }
                    val label = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                    SelectableAppPickerItem(name = label ?: pkg, packageName = pkg)
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedByDescending { item -> usage7d[item.packageName] ?: 0L }
    }
}
