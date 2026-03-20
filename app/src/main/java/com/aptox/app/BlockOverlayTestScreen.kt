package com.aptox.app

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * [BlockOverlayService] 실제 UI 3종을 디버그에서 한 화면에서 실행.
 * - 시간 지정 차단 (blockUntilMs > 0)
 * - 일일 사용량 초과 (USAGE_EXCEEDED)
 * - 카운트 미시작 (COUNT_NOT_STARTED)
 */
@Composable
fun BlockOverlayTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val samplePkg = context.packageName
    val sampleName = try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(samplePkg, 0),
        ).toString()
    } catch (_: Exception) {
        "테스트 앱"
    }

    // 부모(DebugDesignSystemDetailSection)가 verticalScroll 이므로 여기서는 fillMaxSize+스크롤 금지(무한 높이 크래시 방지)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "차단 오버레이 (3종)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "실제 BlockOverlayService를 띄웁니다. 다른 오버레이가 떠 있으면 먼저 닫아주세요.",
            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(8.dp))

        AptoxPrimaryButton(
            text = "1. 시간 지정 차단",
            onClick = {
                startBlockOverlay(
                    context = context,
                    packageName = samplePkg,
                    blockUntilMs = System.currentTimeMillis() + (3 * 60 + 30) * 60_000L,
                    overlayState = null,
                    appName = null,
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "blockUntilMs > 0 · 일시정지·홈으로 이동",
            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
        )

        AptoxPrimaryButton(
            text = "2. 일일 사용량 초과",
            onClick = {
                startBlockOverlay(
                    context = context,
                    packageName = samplePkg,
                    blockUntilMs = 0L,
                    overlayState = BlockOverlayService.OVERLAY_STATE_USAGE_EXCEEDED,
                    appName = sampleName,
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "USAGE_EXCEEDED · 닫기만",
            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
        )

        AptoxPrimaryButton(
            text = "3. 카운트 미시작",
            onClick = {
                startBlockOverlay(
                    context = context,
                    packageName = samplePkg,
                    blockUntilMs = 0L,
                    overlayState = BlockOverlayService.OVERLAY_STATE_COUNT_NOT_STARTED,
                    appName = sampleName,
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "COUNT_NOT_STARTED · 카운트 시작 → 메인 바텀시트",
            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(16.dp))
        AptoxGhostButton(text = "돌아가기", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

private fun startBlockOverlay(
    context: Context,
    packageName: String,
    blockUntilMs: Long,
    overlayState: String?,
    appName: String?,
) {
    val intent = Intent(context, BlockOverlayService::class.java).apply {
        putExtra(BlockOverlayService.EXTRA_PACKAGE_NAME, packageName)
        putExtra(BlockOverlayService.EXTRA_BLOCK_UNTIL_MS, blockUntilMs)
        if (blockUntilMs <= 0L) {
            putExtra(
                BlockOverlayService.EXTRA_OVERLAY_STATE,
                overlayState ?: BlockOverlayService.OVERLAY_STATE_USAGE_EXCEEDED,
            )
            putExtra(BlockOverlayService.EXTRA_APP_NAME, appName ?: packageName)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
