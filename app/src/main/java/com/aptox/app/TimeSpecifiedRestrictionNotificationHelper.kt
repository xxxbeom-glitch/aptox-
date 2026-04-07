package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 시간 지정 제한: 시작/종료 시점 알림
 *
 * Android 8+ 에서 시작/종료를 **별도 알림 채널**로 두어
 * 시스템 설정의 "지정 시간 제한 시작 알림" / "종료 알림" 행과 [NotificationPreferences] 값이 맞도록 한다.
 * (레거시 단일 채널 `time_specified_restriction` 은 동기화 시 제거)
 */
object TimeSpecifiedRestrictionNotificationHelper {

    private const val CHANNEL_ID_LEGACY = "time_specified_restriction"
    private const val CHANNEL_ID_START = "time_specified_restriction_start"
    private const val CHANNEL_ID_END = "time_specified_restriction_end"
    private const val CHANNEL_NAME_START = "지정 시간 제한 시작 알림"
    private const val CHANNEL_NAME_END = "지정 시간 제한 종료 알림"

    /**
     * [NotificationPreferences] 의 시작/종료 토글에 맞춰 채널 importance 를 갱신한다.
     * 앱 알림 설정 화면 진입·토글 변경·앱 기동 시 호출.
     */
    fun syncChannelsWithPreferences(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val app = context.applicationContext
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { nm.deleteNotificationChannel(CHANNEL_ID_LEGACY) }
        upsertChannel(
            nm,
            CHANNEL_ID_START,
            CHANNEL_NAME_START,
            NotificationPreferences.isTimeSpecifiedStartEnabled(app),
        )
        upsertChannel(
            nm,
            CHANNEL_ID_END,
            CHANNEL_NAME_END,
            NotificationPreferences.isTimeSpecifiedEndEnabled(app),
        )
    }

    private fun upsertChannel(nm: NotificationManager, id: String, name: String, enabled: Boolean) {
        val importance = if (enabled) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_NONE
        nm.createNotificationChannel(NotificationChannel(id, name, importance))
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * @param isStart true: 시작 알림, false: 종료(해제) 알림
     */
    fun show(context: Context, appName: String, packageName: String, isStart: Boolean) {
        if (isStart && !NotificationPreferences.isTimeSpecifiedStartEnabled(context)) return
        if (!isStart && !NotificationPreferences.isTimeSpecifiedEndEnabled(context)) return
        syncChannelsWithPreferences(context)
        val channelId = if (isStart) CHANNEL_ID_START else CHANNEL_ID_END
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val title = if (isStart) {
            "지금부터 $appName 사용 제한이 시작됩니다"
        } else {
            "$appName 사용 제한이 해제됐습니다"
        }
        // 제목 한 줄만 두어 BigText/2줄 템플릿 확장 화살표 유발 최소화 (액션 없음)
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(notificationId(packageName, isStart), notification)
    }

    private fun notificationId(packageName: String, isStart: Boolean): Int {
        val h = packageName.hashCode() xor if (isStart) 0x1111 else 0x2222
        return (h and 0x7fffffff)
    }
}
