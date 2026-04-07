package com.aptox.app.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataBackupMetadataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aptox_backup_metadata",
)

/** 로컬 ZIP 백업 완료 시각 (epoch ms). 요구 키명과 동일. */
private val KEY_LOCAL_BACKUP_LAST_TIMESTAMP = longPreferencesKey("local_backup_last_timestamp")

/** 유료(서버) 백업 완료 시각 (epoch ms). */
private val KEY_SERVER_BACKUP_LAST_TIMESTAMP = longPreferencesKey("server_backup_last_timestamp")

object DataBackupMetadataRepository {

    fun localLastBackupTimestampFlow(context: Context): Flow<Long?> {
        val app = context.applicationContext
        return app.dataBackupMetadataStore.data.map { it[KEY_LOCAL_BACKUP_LAST_TIMESTAMP] }
    }

    fun serverLastBackupTimestampFlow(context: Context): Flow<Long?> {
        val app = context.applicationContext
        return app.dataBackupMetadataStore.data.map { it[KEY_SERVER_BACKUP_LAST_TIMESTAMP] }
    }

    suspend fun readLocalLastBackupTimestamp(context: Context): Long? =
        context.applicationContext.dataBackupMetadataStore.data.first()[KEY_LOCAL_BACKUP_LAST_TIMESTAMP]

    suspend fun readServerLastBackupTimestamp(context: Context): Long? =
        context.applicationContext.dataBackupMetadataStore.data.first()[KEY_SERVER_BACKUP_LAST_TIMESTAMP]

    suspend fun setLocalBackupCompletedNow(context: Context) {
        val now = System.currentTimeMillis()
        context.applicationContext.dataBackupMetadataStore.edit { prefs ->
            prefs[KEY_LOCAL_BACKUP_LAST_TIMESTAMP] = now
        }
    }

    suspend fun setServerBackupCompletedNow(context: Context) {
        val now = System.currentTimeMillis()
        context.applicationContext.dataBackupMetadataStore.edit { prefs ->
            prefs[KEY_SERVER_BACKUP_LAST_TIMESTAMP] = now
        }
    }
}
