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

private val Context.permissionOnboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aptox_permission_onboarding",
)

private val KEY_ONBOARDING_FLOW_COMPLETED = booleanPreferencesKey("onboarding_flow_completed")
private val KEY_PERMISSION_FIGMA_1652_COMPLETED = booleanPreferencesKey("permission_figma_1652_completed")

/**
 * 최초 실행·온보딩 관련 DataStore.
 * - [isPermissionFigma1652OnboardingCompleted]: Figma 권한 온보딩(1652) 완료 여부 — **라우팅은 이 값만 사용** (재설치+백업 복원과 분리).
 * - [isOnboardingFlowCompleted]: 레거시 장문 온보딩 플래그(유지만, 신규 플로우에서는 미사용).
 */
class FirstRunFlowRepository(context: Context) {

    private val appContext = context.applicationContext

    private val legacyDataStore: DataStore<Preferences>
        get() = appContext.firstRunDataStore

    private val permissionOnboardingStore: DataStore<Preferences>
        get() = appContext.permissionOnboardingDataStore

    /** Figma 1652 권한 온보딩을 마쳤는지 (스플래시 → 메인 분기) */
    suspend fun isPermissionFigma1652OnboardingCompleted(): Boolean =
        permissionOnboardingStore.data
            .map { it[KEY_PERMISSION_FIGMA_1652_COMPLETED] == true }
            .first()

    suspend fun setPermissionFigma1652OnboardingCompleted(completed: Boolean = true) {
        permissionOnboardingStore.edit { prefs ->
            prefs[KEY_PERMISSION_FIGMA_1652_COMPLETED] = completed
        }
    }

    suspend fun isOnboardingFlowCompleted(): Boolean =
        legacyDataStore.data
            .map { it[KEY_ONBOARDING_FLOW_COMPLETED] == true }
            .first()

    suspend fun setOnboardingFlowCompleted(completed: Boolean = true) {
        legacyDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_FLOW_COMPLETED] = completed
        }
    }
}
