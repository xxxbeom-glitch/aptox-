package com.cole.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 앱 차단 오버레이 Figma AA-01 (node 776-2776)
 * - 배경: Grey800 (#141414)
 * - 차단 아이콘 (휴대폰 금지 표시)
 * - "{App}은 사용제한 중이에요" + "제한 해제까지 남은 시간" + "3시간 30분"
 * - 5분 일시정지 버튼 (흰색 배경, 52dp, 12dp radius) + 닫기 버튼
 */
@Composable
fun BlockOverlayContent(
    appName: String,
    remainingTimeText: String,
    pauseButtonText: String,
    onPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    isPauseEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Grey800),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 차단 아이콘 + 타이틀 + 남은시간
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_block_overlay),
                contentDescription = "사용 제한 중",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "${appName}은\n사용제한 중이에요",
                style = AppTypography.Display3.copy(
                    color = AppColors.Grey100,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = "제한 해제까지 남은 시간",
                style = AppTypography.Caption2.copy(color = AppColors.Grey100),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = remainingTimeText,
                style = AppTypography.HeadingH3.copy(color = AppColors.Red300),
            )
        }

        Spacer(modifier = Modifier.height(88.dp))

        // 버튼 영역
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlockOverlayPauseButton(
                text = pauseButtonText,
                onClick = onPauseClick,
                enabled = isPauseEnabled,
            )
            BlockOverlayCloseButton(text = "닫기", onClick = onCloseClick)
        }
    }
}

/**
 * Figma 1006:3976 — 앱 차단 오버레이 전용 버튼
 * 흰색 배경(Grey100), 진한 텍스트(TextBody #1f1f1f), 52dp 높이, 240dp 너비, 12dp radius
 * 이 버튼은 오버레이에서만 사용됨
 */
@Composable
private fun BlockOverlayPauseButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(240.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) AppColors.Grey100 else AppColors.Grey100.copy(alpha = 0.5f),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge.copy(
                color = if (enabled) AppColors.TextBody else AppColors.TextDisabled,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Figma 1006:3977 — 닫기 버튼 (텍스트 전용)
 */
@Composable
private fun BlockOverlayCloseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(240.dp)
            .height(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge.copy(color = AppColors.Grey100),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * blockUntilMs > 0: "X시간 Y분" 또는 "Y분"
 * blockUntilMs == 0: "자정에 해제됩니다"
 */
fun formatBlockRemainingTime(blockUntilMs: Long): String {
    if (blockUntilMs <= 0) {
        return "자정에 해제됩니다"
    }
    val remainingMs = (blockUntilMs - System.currentTimeMillis()).coerceAtLeast(0)
    val totalMinutes = (remainingMs / 60_000).toInt()
    return when {
        totalMinutes >= 60 -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins == 0) "${hours}시간" else "${hours}시간 ${mins}분"
        }
        totalMinutes > 0 -> "${totalMinutes}분"
        else -> "곧 해제"
    }
}
