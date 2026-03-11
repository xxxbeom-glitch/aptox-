package com.cole.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/**
 * 말풍선 (Figma 1084-4659)
 * - 꼬리: 13×10dp, 좌측 방향 삼각형
 * - 본체: Primary200 배경, 2dp 라운드, 패딩 5h×3v
 * - 텍스트: Caption1 12sp, Primary300
 */
private val SpeechBubbleTailWidth = 13.dp
private val SpeechBubbleTailHeight = 10.dp
private val SpeechBubblePaddingH = 5.dp
private val SpeechBubblePaddingV = 3.dp
private val SpeechBubbleCornerRadius = 2.dp

@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tailDirection: TailDirection = TailDirection.Start,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (tailDirection) {
            TailDirection.Start -> {
                SpeechBubbleTail(modifier = Modifier, pointLeft = true)
                Box(
                    modifier = Modifier
                        .background(AppColors.Primary200, RoundedCornerShape(SpeechBubbleCornerRadius))
                        .padding(horizontal = SpeechBubblePaddingH, vertical = SpeechBubblePaddingV),
                ) {
                    Text(
                        text = text,
                        style = AppTypography.Caption1.copy(color = AppColors.Primary300),
                    )
                }
            }
            TailDirection.End -> {
                Box(
                    modifier = Modifier
                        .background(AppColors.Primary200, RoundedCornerShape(SpeechBubbleCornerRadius))
                        .padding(horizontal = SpeechBubblePaddingH, vertical = SpeechBubblePaddingV),
                ) {
                    Text(
                        text = text,
                        style = AppTypography.Caption1.copy(color = AppColors.Primary300),
                    )
                }
                SpeechBubbleTail(modifier = Modifier, pointLeft = false)
            }
        }
    }
}

@Composable
private fun SpeechBubbleTail(
    modifier: Modifier,
    pointLeft: Boolean,
) {
    Canvas(
        modifier = modifier.size(SpeechBubbleTailWidth, SpeechBubbleTailHeight),
    ) {
        val path = Path().apply {
            if (pointLeft) {
                // 삼각형: 좌측 꼭짓점, 우측이 밑변 (말풍선 본체에 붙음)
                moveTo(0f, size.height / 2f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            } else {
                // 우측 꼭짓점
                moveTo(size.width, size.height / 2f)
                lineTo(0f, 0f)
                lineTo(0f, size.height)
                close()
            }
        }
        drawPath(path, color = AppColors.Primary200)
    }
}

enum class TailDirection {
    Start,
    End,
}
