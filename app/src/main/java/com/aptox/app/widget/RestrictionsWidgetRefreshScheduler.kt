package com.aptox.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * 제한 현황 위젯을 약 10분 간격으로 갱신 ([AlarmManager.setInexactRepeating] — 정확히 10분이 아닐 수 있음).
 */
object RestrictionsWidgetRefreshScheduler {

    private const val REQUEST_CODE = 94002
    private const val TEN_MIN_MS = 10 * 60 * 1000L

    fun scheduleIfHasWidgets(context: Context) {
        val app = context.applicationContext
        val ids = AppWidgetManager.getInstance(app).getAppWidgetIds(
            ComponentName(app, AptoxRestrictionStatusWidgetProvider::class.java),
        )
        if (ids.isEmpty()) {
            cancel(app)
            return
        }
        schedule(app)
    }

    fun schedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, AptoxRestrictionStatusWidgetProvider::class.java).apply {
            action = AptoxRestrictionStatusWidgetProvider.ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        val trigger = SystemClock.elapsedRealtime() + TEN_MIN_MS
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            trigger,
            TEN_MIN_MS,
            pi,
        )
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, AptoxRestrictionStatusWidgetProvider::class.java).apply {
            action = AptoxRestrictionStatusWidgetProvider.ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
    }
}
