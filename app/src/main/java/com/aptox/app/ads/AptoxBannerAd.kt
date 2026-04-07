package com.aptox.app.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aptox.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AptoxBannerAd"

private enum class BannerUiState {
    Loading,
    Loaded,
    Failed,
}

/**
 * AdMob BANNER(320×50) — 로드 실패 시 Composable을 제거해 영역을 붕괴(collapse).
 */
@Composable
fun AptoxBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
) {
    var state by remember(adUnitId) { mutableStateOf(BannerUiState.Loading) }

    if (state == BannerUiState.Failed) return

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        state = BannerUiState.Loaded
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w(TAG, "배너 로드 실패 code=${adError.code} ${adError.message}")
                        state = BannerUiState.Failed
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(
                when (state) {
                    BannerUiState.Loaded -> 50.dp
                    else -> 0.dp
                },
            ),
        onRelease = { adView -> adView.destroy() },
    )
}
