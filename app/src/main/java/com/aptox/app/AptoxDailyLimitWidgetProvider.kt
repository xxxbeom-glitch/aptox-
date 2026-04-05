package com.aptox.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/** 홈 화면 「하루 사용 제한」 위젯 (Figma 1619:5438). */
class AptoxDailyLimitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_limit)
            AptoxHomeWidgetBinder.bindDailyLimitWithSample(context, views)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
