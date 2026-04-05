package com.aptox.app.subscription

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.premiumStatusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aptox_premium_status",
)

private val KEY_PREMIUM_SUBSCRIBED = booleanPreferencesKey("premium_subscribed")
private val KEY_PREMIUM_EXPIRY_MILLIS = longPreferencesKey("premium_subscription_expiry_millis")
private val KEY_PREMIUM_AUTO_RENEWING = booleanPreferencesKey("premium_subscription_auto_renewing")
private val KEY_PREMIUM_BASE_PLAN_ID = stringPreferencesKey("premium_subscription_base_plan_id")

/** DataStore에 저장된 Play 구독 요약 (UI용). [expiryEpochMillis] 0이면 미확인. */
data class PremiumSnapshot(
    val subscribed: Boolean,
    val expiryEpochMillis: Long,
    val autoRenewing: Boolean,
    /** Play base plan id(예: `monthly` / `yearly`). 없으면 UI는 연간 레이아웃 기본. */
    val basePlanId: String? = null,
)

/**
 * Play 결제·복원 결과를 반영하는 프리미엄 플래그 (Preferences DataStore).
 */
object PremiumStatusRepository {

    fun subscribedFlow(context: Context): Flow<Boolean> {
        val app = context.applicationContext
        return app.premiumStatusDataStore.data.map { prefs ->
            prefs[KEY_PREMIUM_SUBSCRIBED] == true
        }
    }

    fun premiumSnapshotFlow(context: Context): Flow<PremiumSnapshot> {
        val app = context.applicationContext
        return app.premiumStatusDataStore.data.map { prefs ->
            PremiumSnapshot(
                subscribed = prefs[KEY_PREMIUM_SUBSCRIBED] == true,
                expiryEpochMillis = prefs[KEY_PREMIUM_EXPIRY_MILLIS] ?: 0L,
                autoRenewing = prefs[KEY_PREMIUM_AUTO_RENEWING] ?: true,
                basePlanId = prefs[KEY_PREMIUM_BASE_PLAN_ID]?.takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun readSubscribed(context: Context): Boolean {
        val app = context.applicationContext
        return app.premiumStatusDataStore.data.first()[KEY_PREMIUM_SUBSCRIBED] == true
    }

    suspend fun setSubscribed(context: Context, subscribed: Boolean) {
        val app = context.applicationContext
        app.premiumStatusDataStore.edit { prefs ->
            prefs[KEY_PREMIUM_SUBSCRIBED] = subscribed
            if (!subscribed) {
                prefs.remove(KEY_PREMIUM_EXPIRY_MILLIS)
                prefs.remove(KEY_PREMIUM_AUTO_RENEWING)
                prefs.remove(KEY_PREMIUM_BASE_PLAN_ID)
            }
        }
    }

    suspend fun setSubscriptionDetails(
        context: Context,
        expiryEpochMillis: Long,
        autoRenewing: Boolean,
        /** 파싱 성공 시에만 넘기면 저장. null이면 기존 basePlanId 유지. */
        basePlanId: String? = null,
    ) {
        val app = context.applicationContext
        app.premiumStatusDataStore.edit { prefs ->
            prefs[KEY_PREMIUM_EXPIRY_MILLIS] = expiryEpochMillis
            prefs[KEY_PREMIUM_AUTO_RENEWING] = autoRenewing
            val plan = basePlanId?.trim()?.takeIf { it.isNotEmpty() }
            if (plan != null) {
                prefs[KEY_PREMIUM_BASE_PLAN_ID] = plan
            }
        }
    }
}
