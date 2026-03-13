package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Figma 1136-6361: 일일사용량 제한 앱에서 사용 시간을 전부 소진했을 때 오버레이 미리보기.
 * - 배경: Grey850 (#0a0a0a)
 * - 문구: "오늘 사용가능한 시간을 전부 사용하셨어요"
 * - 닫기 버튼 (Primary pressed #614cc7)
 * 디버그 메뉴에서 확인 전용. 실제 BlockOverlayService에는 미적용.
 */
@Composable
fun DailyUsageLimitOverlayPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Grey850),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 246.dp),
        ) {
            Text(
                text = "오늘 사용가능한 시간을\n전부 사용하셨어요",
                style = AppTypography.HeadingH2.copy(color = AppColors.Grey100),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(26.dp))
            Box(
                modifier = Modifier
                    .widthIn(min = 246.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.ButtonPrimaryBgPressed)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "닫기",
                    style = AppTypography.ButtonLarge.copy(color = AppColors.ButtonPrimaryTextDefault),
                )
            }
        }
    }
}
