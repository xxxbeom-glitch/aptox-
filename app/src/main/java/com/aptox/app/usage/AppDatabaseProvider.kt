package com.aptox.app.usage

import android.content.Context

object AppDatabaseProvider {

    @Volatile
    private var instance: UsageStatsDatabase? = null

    fun get(context: Context): UsageStatsDatabase {
        return instance ?: synchronized(this) {
            instance ?: UsageStatsDatabase(context.applicationContext).also { instance = it }
        }
    }
}
