package com.aptox.app

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
 * [BlockDialogActivity] AlertDialog 3종을 디버그에서 한 화면에서 실행.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "차단 AlertDialog (3종)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "BlockDialogActivity로 AlertDialog를 띄웁니다.",
            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(8.dp))

        AptoxPrimaryButton(
            text = "1. 시간 지정 차단",
            onClick = {
                BlockDialogActivity.start(
                    context = context,
                    packageName = samplePkg,
                    appName = sampleName,
                    blockUntilMs = System.currentTimeMillis() + (3 * 60 + 30) * 60_000L,
                    overlayState = BlockDialogActivity.OVERLAY_STATE_USAGE_EXCEEDED,
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
                BlockDialogActivity.start(
                    context = context,
                    packageName = samplePkg,
                    appName = sampleName,
                    blockUntilMs = 0L,
                    overlayState = BlockDialogActivity.OVERLAY_STATE_USAGE_EXCEEDED,
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
                BlockDialogActivity.start(
                    context = context,
                    packageName = samplePkg,
                    appName = sampleName,
                    blockUntilMs = 0L,
                    overlayState = BlockDialogActivity.OVERLAY_STATE_COUNT_NOT_STARTED,
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
