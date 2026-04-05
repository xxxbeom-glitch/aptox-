package com.aptox.app.subscription

import android.app.Activity
import android.app.Application
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.aptox.app.SubscriptionPlanTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Google Play 구독 BillingClient — Play Console: 구독 ID [PRODUCT_ID], basePlan [BASE_PLAN_MONTHLY] / [BASE_PLAN_YEARLY].
 */
object SubscriptionBillingController {

    const val PRODUCT_ID = "aptox_premium"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    private const val TAG = "SubscriptionBilling"

    private lateinit var appContext: Application
    private var appScope: CoroutineScope? = null

    private var billingClient: BillingClient? = null
    private val productDetailsById = ConcurrentHashMap<String, ProductDetails>()

    /** [launchBillingFlow] 직전 플랜 — purchase JSON에 basePlanId가 없을 때 1회 저장용 */
    @Volatile
    private var pendingLaunchBasePlanId: String? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> pendingLaunchBasePlanId = null
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            else -> Log.w(TAG, "onPurchasesUpdated: ${result.responseCode} ${result.debugMessage}")
        }
    }

    @Suppress("DEPRECATION")
    fun initialize(application: Application, scope: CoroutineScope) {
        if (billingClient != null) return
        appContext = application
        appScope = scope
        val client = BillingClient.newBuilder(application)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    syncPurchasesToPremiumFlag()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.i(TAG, "Billing service disconnected")
            }
        })
    }

    private fun clientOrNull(): BillingClient? = billingClient

    private fun queryProductDetailsParams(): QueryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

    private fun queryProductDetails() {
        val client = clientOrNull() ?: return
        client.queryProductDetailsAsync(queryProductDetailsParams()) { billingResult, list ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetailsAsync: ${billingResult.responseCode} ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }
            list?.firstOrNull { it.productId == PRODUCT_ID }?.let { details ->
                productDetailsById[PRODUCT_ID] = details
            }
        }
    }

    /** [SubscriptionPlanTier]에 해당하는 Play basePlanId */
    fun basePlanIdForTier(tier: SubscriptionPlanTier): String = when (tier) {
        SubscriptionPlanTier.Monthly -> BASE_PLAN_MONTHLY
        SubscriptionPlanTier.Annual -> BASE_PLAN_YEARLY
    }

    /**
     * [ProductDetails]에서 [tier]에 맞는 offerToken (basePlanId 일치).
     * 동일 basePlan에 오퍼가 여러 개면 첫 번째 매칭을 사용합니다.
     */
    fun offerTokenForTier(details: ProductDetails, tier: SubscriptionPlanTier): String? {
        val want = basePlanIdForTier(tier)
        return details.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == want }
            ?.offerToken
    }

    /** 앱 시작·연결 후 활성 구독을 DataStore에 반영 */
    fun syncPurchasesToPremiumFlag() {
        val client = clientOrNull() ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchasesAsync: ${result.responseCode} ${result.debugMessage}")
                return@queryPurchasesAsync
            }
            val premiumPurchases = purchases.filter { isPremiumPurchase(it) }
            val active = premiumPurchases.isNotEmpty()
            val primary = premiumPurchases.maxByOrNull { parseExpiryEpochMillis(it) }
            appScope?.launch(Dispatchers.IO) {
                PremiumStatusRepository.setSubscribed(appContext, active)
                if (active && primary != null) {
                    PremiumStatusRepository.setSubscriptionDetails(
                        appContext,
                        parseExpiryEpochMillis(primary),
                        primary.isAutoRenewing,
                        parseBasePlanId(primary),
                    )
                }
            }
        }
    }

    private fun isPremiumPurchase(purchase: Purchase): Boolean {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        return purchase.products.any { it == PRODUCT_ID }
    }

    /** Play가 내려주는 purchase JSON의 `expiryTimeMillis`(숫자·문자열). 없으면 0. */
    private fun parseExpiryEpochMillis(purchase: Purchase): Long {
        return try {
            val json = JSONObject(purchase.originalJson)
            when {
                json.has("expiryTimeMillis") -> json.get("expiryTimeMillis").toLongish()
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /** Play purchase JSON에서 base plan id(`monthly` / `yearly`) 추출. 없거나 알 수 없으면 null. */
    private fun parseBasePlanId(purchase: Purchase): String? {
        val candidates = mutableListOf<String>()
        try {
            val json = JSONObject(purchase.originalJson)
            fun add(raw: String?) {
                val t = raw?.trim()
                if (!t.isNullOrEmpty()) candidates.add(t)
            }
            add(json.optString("basePlanId"))
            add(json.optString("base_plan_id"))
            val arrKeys = arrayOf("subscriptionOfferDetails", "lineItems")
            for (key in arrKeys) {
                val arr = json.optJSONArray(key) ?: continue
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(o.optString("basePlanId"))
                    add(o.optString("base_plan_id"))
                }
            }
        } catch (_: Exception) {
            return null
        }
        for (c in candidates) {
            when (c.lowercase(Locale.US)) {
                BASE_PLAN_MONTHLY -> return BASE_PLAN_MONTHLY
                BASE_PLAN_YEARLY -> return BASE_PLAN_YEARLY
            }
        }
        return null
    }

    private fun Any?.toLongish(): Long = when (this) {
        is Long -> this
        is Int -> toLong()
        is Number -> toLong()
        is String -> toLongOrNull() ?: 0L
        else -> 0L
    }

    /** JSON 우선, 없으면 방금 결제 플로우에서 선택한 base plan 한 번 반영 후 pending 비움 */
    private fun basePlanIdToStoreForPurchase(purchase: Purchase): String? {
        val fromJson = parseBasePlanId(purchase)
        if (fromJson != null) {
            pendingLaunchBasePlanId = null
            return fromJson
        }
        val pending = pendingLaunchBasePlanId
        pendingLaunchBasePlanId = null
        return when (pending?.lowercase(Locale.US)) {
            BASE_PLAN_MONTHLY -> BASE_PLAN_MONTHLY
            BASE_PLAN_YEARLY -> BASE_PLAN_YEARLY
            else -> null
        }
    }

    private fun persistPremiumFromPurchase(purchase: Purchase) {
        appScope?.launch(Dispatchers.IO) {
            PremiumStatusRepository.setSubscribed(appContext, true)
            PremiumStatusRepository.setSubscriptionDetails(
                appContext,
                parseExpiryEpochMillis(purchase),
                purchase.isAutoRenewing,
                basePlanIdToStoreForPurchase(purchase),
            )
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!isPremiumPurchase(purchase)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val client = clientOrNull() ?: return
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    persistPremiumFromPurchase(purchase)
                    syncPurchasesToPremiumFlag()
                } else {
                    Log.e(TAG, "acknowledgePurchase failed: ${result.responseCode} ${result.debugMessage}")
                }
            }
        } else {
            persistPremiumFromPurchase(purchase)
        }
    }

    fun launchSubscriptionFlow(activity: Activity, tier: SubscriptionPlanTier) {
        val client = clientOrNull()
        if (client == null || !client.isReady) {
            Log.w(TAG, "BillingClient not ready")
            return
        }
        val details = productDetailsById[PRODUCT_ID]
        if (details != null) {
            launchBillingFlow(activity, client, details, tier)
            return
        }
        queryProductDetails()
        client.queryProductDetailsAsync(queryProductDetailsParams()) { result, list ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || list.isNullOrEmpty()) {
                Log.w(TAG, "queryProductDetailsAsync (retry): ${result.responseCode}")
                return@queryProductDetailsAsync
            }
            val pd = list.firstOrNull { it.productId == PRODUCT_ID } ?: list.first()
            productDetailsById[PRODUCT_ID] = pd
            launchBillingFlow(activity, client, pd, tier)
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        client: BillingClient,
        productDetails: ProductDetails,
        tier: SubscriptionPlanTier,
    ) {
        val offerToken = offerTokenForTier(productDetails, tier)
        if (offerToken.isNullOrEmpty()) {
            Log.e(
                TAG,
                "No offerToken for productId=${productDetails.productId} basePlanId=${basePlanIdForTier(tier)}; " +
                    "offers=${productDetails.subscriptionOfferDetails?.map { it.basePlanId to it.offerId }}",
            )
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        pendingLaunchBasePlanId = basePlanIdForTier(tier)
        val launchResult = client.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingLaunchBasePlanId = null
            Log.e(TAG, "launchBillingFlow failed: ${launchResult.responseCode} ${launchResult.debugMessage}")
        }
    }
}
