package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 23:55에 발동. 내일도 동일 제한이 있는 앱에 대해 리셋 예고 알림 발송.
 * "5분 후 [앱이름] 일일사용량 시간이 초기화돼요"
 */
class DailyUsageResetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val packageNames = intent?.getStringArrayExtra(DailyUsageAlarmScheduler.EXTRA_PACKAGE_NAMES)
        val appNames = intent?.getStringArrayExtra(DailyUsageAlarmScheduler.EXTRA_APP_NAMES)
        if (packageNames.isNullOrEmpty() || appNames.isNullOrEmpty() || packageNames.size != appNames.size) {
            Log.w(TAG, "23:55 알림: 데이터 불일치")
            return
        }
        for (i in packageNames.indices) {
            DailyUsageNotificationHelper.sendResetWarningNotification(
                context, appNames[i], packageNames[i],
            )
        }
        Log.d(TAG, "23:55 리셋 예고 알림 ${packageNames.size}개 발송")
        // 알림 발송 후 다음 날 23:55 재예약 (스케줄러 호출)
        DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(context)
    }

    companion object {
        private const val TAG = "DailyUsageReset"
    }
}
