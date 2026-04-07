package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.aptox.app.MainActivity

/**
 * 4x1 홈 화면 제한 앱 현황 위젯 (하루 사용량 + 시간 지정).
 */
class AptoxRestrictionStatusWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val cn = android.content.ComponentName(app, AptoxRestrictionStatusWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isNotEmpty()) {
                onUpdate(app, mgr, ids)
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext
        val views = AptoxRestrictionWidgetBinder.buildUpdate(app)
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RestrictionsWidgetRefreshScheduler.schedule(context.applicationContext)
        if (!AptoxRestrictionWidgetBinder.isPremiumUser(context)) {
            val app = context.applicationContext
            app.startActivity(
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_OPEN_SUBSCRIPTION_FROM_WIDGET, true)
                },
            )
        }
    }

    override fun onDisabled(context: Context) {
        RestrictionsWidgetRefreshScheduler.cancel(context.applicationContext)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.aptox.app.action.WIDGET_REFRESH_RESTRICTIONS"
    }
}
