package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val CompactBoxPaddingHorizontal = 12.dp
private val CompactBoxPaddingVertical = 8.dp
private val CompactBoxCornerRadius = 6.dp
// 패딩(8*2) + Caption1 lineHeight(19sp) ≈ 35dp
private val CompactBoxMinHeight = 35.dp

/**
 * Figma 901-3501: InfoBox Compact
 * 패딩 horizontal=12dp(가변), vertical=8dp, minHeight=35dp, rounded 6dp
 */
@Composable
fun ColeInfoBoxCompact(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    contentPaddingHorizontal: Dp = CompactBoxPaddingHorizontal,
    contentPaddingVertical: Dp = CompactBoxPaddingVertical,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CompactBoxMinHeight)
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(CompactBoxCornerRadius))
            .border(1.dp, AppColors.BorderInfoBox, RoundedCornerShape(CompactBoxCornerRadius))
            .padding(horizontal = contentPaddingHorizontal, vertical = contentPaddingVertical),
    ) {
        Text(
            text = text,
            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            overflow = if (maxLines < Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
    }
}

/**
 * ColeInfoBoxCompact 신규 디자인
 * - border 제거
 * - 배경: Primary50
 * - 텍스트: Primary400
 * - 기본 패딩: 상하 18dp, 좌우 16dp
 */
@Composable
fun ColeInfoBoxCompactNewDesign(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    contentPaddingHorizontal: Dp = 16.dp,
    contentPaddingVertical: Dp = 18.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CompactBoxMinHeight)
            .background(AppColors.Primary50, RoundedCornerShape(CompactBoxCornerRadius))
            .padding(horizontal = contentPaddingHorizontal, vertical = contentPaddingVertical),
    ) {
        Text(
            text = text,
            style = AppTypography.Caption1.copy(color = AppColors.Primary400),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            overflow = if (maxLines < Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
    }
}

/**
 * Figma: InfoBox / Notice 안내 박스
 * SurfaceBackgroundInfoBox, BorderInfoBox 사용
 */
@Composable
fun ColeInfoBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(12.dp))
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.Disclaimer.copy(color = AppColors.TextTertiary),
            textAlign = TextAlign.Center,
        )
    }
}
