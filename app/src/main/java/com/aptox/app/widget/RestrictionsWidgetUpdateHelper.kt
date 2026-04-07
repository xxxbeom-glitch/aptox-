package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** 제한 저장/삭제 직후 홈 위젯을 한 번 갱신합니다. */
object RestrictionsWidgetUpdateHelper {

    fun updateAll(context: Context) {
        val app = context.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val cn = ComponentName(app, AptoxRestrictionStatusWidgetProvider::class.java)
        val ids = mgr.getAppWidgetIds(cn)
        if (ids.isEmpty()) return
        val views = AptoxRestrictionWidgetBinder.buildUpdate(app)
        for (id in ids) {
            mgr.updateAppWidget(id, views)
        }
    }
}
