package com.aptox.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.aptox.app.MainActivity
import com.aptox.app.R
import com.aptox.app.subscription.PremiumStatusRepository
import com.aptox.app.subscription.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object AptoxRestrictionWidgetBinder {

    private const val ICON_PX = 128

    fun isPremiumUser(context: Context): Boolean = runBlocking(Dispatchers.IO) {
        val app = context.applicationContext
        val store = PremiumStatusRepository.readSubscribed(app)
        SubscriptionManager.isSubscribedWithStore(store, app)
    }

    fun buildUpdate(context: Context): RemoteViews {
        val app = context.applicationContext
        val (premium, rows) = runBlocking(Dispatchers.IO) {
            val store = PremiumStatusRepository.readSubscribed(app)
            val p = SubscriptionManager.isSubscribedWithStore(store, app)
            val r = if (p) RestrictionWidgetDataLoader.loadRows(app) else emptyList()
            p to r
        }

        val views = RemoteViews(app.packageName, R.layout.widget_restrictions_4x1)

        val clickIntent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!premium) {
                putExtra(MainActivity.EXTRA_OPEN_SUBSCRIPTION_FROM_WIDGET, true)
            }
        }
        val contentPi = PendingIntent.getActivity(
            app,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, contentPi)

        if (!premium) {
            views.setViewVisibility(R.id.widget_scroll, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            views.setViewVisibility(R.id.widget_free_user_message, View.VISIBLE)
            return views
        }

        views.setViewVisibility(R.id.widget_free_user_message, View.GONE)

        if (rows.isEmpty()) {
            views.setViewVisibility(R.id.widget_scroll, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            return views
        }

        views.setViewVisibility(R.id.widget_scroll, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)

        val pm = app.packageManager
        val highlight = app.getColor(R.color.widget_text_highlight)
        val primary = app.getColor(R.color.widget_text_primary)
        for (row in rows) {
            val item = RemoteViews(app.packageName, R.layout.widget_restriction_item)
            item.setTextViewText(R.id.widget_item_name, row.appName)
            item.setTextViewText(R.id.widget_item_status, row.statusText)
            val statusColor = if (row.statusUsesHighlightColor) highlight else primary
            item.setInt(R.id.widget_item_status, "setTextColor", statusColor)

            val bmp = loadIconBitmap(pm, row.packageName)
            if (bmp != null) {
                item.setImageViewBitmap(R.id.widget_item_icon, bmp)
            } else {
                item.setImageViewResource(R.id.widget_item_icon, R.mipmap.ic_launcher)
            }
            views.addView(R.id.widget_items_container, item)
        }

        return views
    }

    private fun loadIconBitmap(pm: android.content.pm.PackageManager, packageName: String): Bitmap? {
        return try {
            val dr = pm.getApplicationIcon(packageName)
            dr.toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
    }
}
