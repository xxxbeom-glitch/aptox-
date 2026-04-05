package com.aptox.app

import android.content.Context
import android.view.View
import android.widget.RemoteViews

/**
 * 하루 사용 제한 홈 위젯 RemoteViews 바인딩.
 * 데이터 연동 전 플레이스홀더 — 이후 동일 API로 실제 제한 목록 주입.
 */
object AptoxHomeWidgetBinder {

    fun bindDailyLimitWithSample(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setTextViewText(R.id.widget_row1_name, "Youtube")
        views.setTextViewText(R.id.widget_row1_time, "00:26:33")
        views.setTextViewText(R.id.widget_row2_name, "Wavve")
        views.setTextViewText(R.id.widget_row2_time, "00:26:33")
    }

    fun bindDailyLimitEmpty(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.GONE)
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty_message))
    }
}
