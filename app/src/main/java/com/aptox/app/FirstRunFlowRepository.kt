package com.aptox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.firstRunDataStore: DataStore<Preferences> by preferencesDataStore(name = "aptox_first_run")

private val KEY_ONBOARDING_FLOW_COMPLETED = booleanPreferencesKey("onboarding_flow_completed")

/**
 * 최초 1회 사용패턴 분석 화면([UsagePatternAnalysisScreen]) 완료 여부.
 * (구명칭 onboarding_flow_completed — 앱 설명 온보딩 등은 제거됨)
 */
class FirstRunFlowRepository(context: Context) {

    private val appContext = context.applicationContext

    private val dataStore: DataStore<Preferences>
        get() = appContext.firstRunDataStore

    suspend fun isOnboardingFlowCompleted(): Boolean =
        dataStore.data
            .map { it[KEY_ONBOARDING_FLOW_COMPLETED] == true }
            .first()

    suspend fun setOnboardingFlowCompleted(completed: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_FLOW_COMPLETED] = completed
        }
    }
}
