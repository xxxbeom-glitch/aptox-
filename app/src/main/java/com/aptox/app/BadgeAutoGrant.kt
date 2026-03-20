package com.aptox.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.aptox.app.model.BadgeMasterData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 프로덕션 배지 자동 지급 (Firestore 중복 방지 + 토스트 + [GoalAchievementNotificationHelper]).
 */
object BadgeAutoGrant {

    private const val TAG = "BadgeAutoGrant"

    private fun yyyyMMdd(cal: Calendar): String {
        return String.format(
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun yesterdayYyyyMMdd(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return yyyyMMdd(cal)
    }

    private fun dateYyyyMMddFromMillis(ms: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        return yyyyMMdd(cal)
    }

    private fun hourOfDayLocal(ms: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * 해당 날짜에 '제한 설정 달성': 제한이 1개 이상 있고,
     * 일일 제한(blockUntilMs==0) 앱은 모두 그날 누적 사용량 ≤ 제한.
     */
    fun restrictionDaySucceeded(context: Context, dateYyyyMMdd: String): Boolean {
        val repo = AppRestrictionRepository(context)
        val list = repo.getAll()
        if (list.isEmpty()) return false
        val timer = ManualTimerRepository(context)
        for (r in list) {
            if (r.blockUntilMs == 0L) {
                val used = timer.getAccumMsForDate(r.packageName, dateYyyyMMdd)
                val limitMs = r.limitMinutes * 60L * 1000L
                if (used > limitMs) return false
            }
        }
        return true
    }

    private fun showBadgeToast(context: Context, badgeId: String) {
        val title = BadgeMasterData.badges.find { it.id == badgeId }?.title ?: badgeId
        val appCtx = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appCtx, "🏅 $title 뱃지를 획득했어요!", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun grantIfNew(context: Context, userId: String, badgeId: String): Boolean {
        val debug001 = badgeId == "badge_001"
        val repo = BadgeRepository(FirebaseFirestore.getInstance(), context.applicationContext)
        val existing = runCatching { repo.getUserBadge(userId, badgeId) }
        if (debug001) {
            Log.d(TAG, "badge_001 grantIfNew: getUserBadge 결과=${existing.getOrNull()} (실패=${existing.isFailure}, err=${existing.exceptionOrNull()?.message})")
        }
        if (existing.getOrNull() != null) {
            if (debug001) Log.d(TAG, "badge_001: 이미 보유 → grantBadge 스킵")
            return false
        }
        if (debug001) Log.d(TAG, "badge_001: grantBadge 호출 직전 userId=$userId")
        val result = repo.grantBadge(userId, badgeId)
        if (debug001) {
            if (result.isSuccess) Log.d(TAG, "badge_001: grantBadge 성공")
            else Log.e(TAG, "badge_001: grantBadge 실패", result.exceptionOrNull())
        }
        if (result.isFailure) return false
        showBadgeToast(context, badgeId)
        return true
    }

    private fun runAsync(context: Context, block: suspend () -> Unit) {
        val ac = context.applicationContext
        val app = ac as? AptoxApplication
        if (app == null) {
            Log.e(TAG, "runAsync 중단: applicationContext가 AptoxApplication이 아님 (class=${ac.javaClass.name})")
            return
        }
        app.applicationScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                Log.e(TAG, "배지 처리 실패", e)
            }
        }
    }

    /** badge_001: 최초 제한 등록 */
    fun onFirstRestrictionSaved(context: Context) {
        Log.d(TAG, "onFirstRestrictionSaved 진입")
        val uidPreview = FirebaseAuth.getInstance().currentUser?.uid
        if (uidPreview == null) {
            Log.w(
                TAG,
                "badge_001: uid=null (스플래시 플로우에 로그인 단계 없음). " +
                    "설정에서 구글 로그인하면 자동으로 badge_001 지급을 다시 시도합니다.",
            )
        } else {
            Log.d(TAG, "badge_001: uid OK (prefix=${uidPreview.take(6)}…)")
        }
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "badge_001 코루틴 내부: uid null → 중단 (로그인 시 onUserSignedInTryBadge001에서 재시도)")
                return@runAsync
            }
            Log.d(TAG, "badge_001 코루틴: grantIfNew 진입")
            grantIfNew(context, uid, "badge_001")
        }
    }

    /**
     * 로그인 직후·앱 기동 시(이미 로그인): 제한이 1개 이상이면 badge_001 지급 재시도.
     * 미로그인 상태에서 제한만 저장한 뒤 구글 로그인하는 경우를 처리한다.
     */
    fun onUserSignedInTryBadge001(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "onUserSignedInTryBadge001: uid=null 스킵")
            return
        }
        val hasRestriction = AppRestrictionRepository(context.applicationContext).getAll().isNotEmpty()
        if (!hasRestriction) {
            Log.d(TAG, "onUserSignedInTryBadge001: 제한 없음 스킵")
            return
        }
        Log.d(TAG, "onUserSignedInTryBadge001: 제한 있음 → badge_001 grantIfNew 시도 (uid prefix=${uid.take(6)}…)")
        runAsync(context) {
            grantIfNew(context, uid, "badge_001")
        }
    }

    /** 자정 알람 후: 연속/누적 달성일·야간 연속 (002,003,004,005,006,011,012) */
    fun onMidnightReset(context: Context) {
        val yesterday = yesterdayYyyyMMdd()
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runAsync
            if (!BadgeStatsPreferences.tryMarkMidnightProcessed(context, yesterday)) return@runAsync

            val progressRepo = BadgeProgressRepository(context)
            val success = restrictionDaySucceeded(context, yesterday)
            if (success) {
                val streak = BadgeStatsPreferences.getRestrictionStreak(context) + 1
                BadgeStatsPreferences.setRestrictionStreak(context, streak)
                val cum = BadgeStatsPreferences.getDailyGoalCumulative(context) + 1
                BadgeStatsPreferences.setDailyGoalCumulative(context, cum)
                progressRepo.accumulatedAchievementDays = cum
                progressRepo.consecutiveAchievementDays = streak
                if (streak >= 7) grantIfNew(context, uid, "badge_002")
                if (streak >= 30) grantIfNew(context, uid, "badge_003")
                if (cum >= 1) grantIfNew(context, uid, "badge_004")
                if (cum >= 7) grantIfNew(context, uid, "badge_005")
                if (cum >= 30) grantIfNew(context, uid, "badge_006")
            } else {
                BadgeStatsPreferences.setRestrictionStreak(context, 0)
                progressRepo.consecutiveAchievementDays = 0
            }

            val hadNight = BadgeStatsPreferences.consumeNightSuccessForDay(context, yesterday)
            if (hadNight) {
                val ns = BadgeStatsPreferences.getNightStreak(context) + 1
                BadgeStatsPreferences.setNightStreak(context, ns)
                if (ns >= 7) grantIfNew(context, uid, "badge_011")
                if (ns >= 30) grantIfNew(context, uid, "badge_012")
            } else {
                BadgeStatsPreferences.setNightStreak(context, 0)
            }
        }
    }

    /** 시간지정 차단 창이 자연 종료될 때 (AppMonitorService). */
    @JvmStatic
    fun onTimeBlockWindowEnded(context: Context, packageName: String, blockUntilMs: Long) {
        if (blockUntilMs <= 0L) return
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runAsync
            if (!BadgeStatsPreferences.tryConsumeTimeBlockExpiry(context, packageName, blockUntilMs)) return@runAsync

            val total = BadgeStatsPreferences.getTimeBlockSuccessTotal(context) + 1
            BadgeStatsPreferences.setTimeBlockSuccessTotal(context, total)
            if (total >= 1) grantIfNew(context, uid, "badge_007")
            if (total >= 7) grantIfNew(context, uid, "badge_008")
            if (total >= 30) grantIfNew(context, uid, "badge_009")

            if (hourOfDayLocal(blockUntilMs) >= 22) {
                val dayKey = dateYyyyMMddFromMillis(blockUntilMs)
                BadgeStatsPreferences.markNightSuccessForCalendarDay(context, dayKey)
                grantIfNew(context, uid, "badge_010")
            }
        }
    }

    /** 차단 오버레이(실제 차단) 표시 시 (013~015). COUNT_NOT_STARTED 제외. */
    fun onBlockDefenseOverlayShown(context: Context) {
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runAsync
            val count = BadgeStatsPreferences.incrementDefenseTotal(context)
            if (count >= 1) grantIfNew(context, uid, "badge_013")
            if (count >= 50) grantIfNew(context, uid, "badge_014")
            if (count >= 200) grantIfNew(context, uid, "badge_015")
        }
    }

    /** 통계 주간 탭 진입 시 1회/주: 지난 완료 주(-1) vs 그전 주(-2) 총 사용시간 감소율 */
    suspend fun checkWeeklyUsageReductionOnStatsOpen(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!StatisticsData.hasUsageAccess(context)) return
        val (thisWeekStart, _, _) = StatisticsData.getWeekRange(0)
        if (BadgeStatsPreferences.getWeeklyReductionCheckWeekStart(context) == thisWeekStart) return
        BadgeStatsPreferences.setWeeklyReductionCheckWeekStart(context, thisWeekStart)

        val (s1, e1, _) = StatisticsData.getWeekRange(-1)
        val (s2, e2, _) = StatisticsData.getWeekRange(-2)
        val recent = StatisticsData.loadDayOfWeekMinutes(context, s1, e1).sum()
        val older = StatisticsData.loadDayOfWeekMinutes(context, s2, e2).sum()
        if (older <= 0L) return

        val ratio = (older - recent).toDouble() / older.toDouble()
        if (ratio >= 0.1) grantIfNew(context, uid, "badge_016")
        if (ratio >= 0.3) grantIfNew(context, uid, "badge_017")
        if (ratio >= 0.5) grantIfNew(context, uid, "badge_018")
    }

    /** 디버그: 누적/연속 수치를 로컬에 반영 후 조건에 맞는 배지만 지급 시도 */
    suspend fun debugApplyProgressAndGrant(context: Context, userId: String, accum: Int, consec: Int): List<String> {
        BadgeStatsPreferences.setDailyGoalCumulative(context, accum)
        BadgeStatsPreferences.setRestrictionStreak(context, consec)
        val progressRepo = BadgeProgressRepository(context)
        progressRepo.accumulatedAchievementDays = accum
        progressRepo.consecutiveAchievementDays = consec
        val granted = mutableListOf<String>()
        if (consec >= 7 && grantIfNew(context, userId, "badge_002")) granted.add("badge_002")
        if (consec >= 30 && grantIfNew(context, userId, "badge_003")) granted.add("badge_003")
        if (accum >= 1 && grantIfNew(context, userId, "badge_004")) granted.add("badge_004")
        if (accum >= 7 && grantIfNew(context, userId, "badge_005")) granted.add("badge_005")
        if (accum >= 30 && grantIfNew(context, userId, "badge_006")) granted.add("badge_006")
        return granted
    }
}
