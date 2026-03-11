package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 카테고리 태그 (Figma 948-3543, DESIGNSYSTEM.md)
 * - 패딩: 4dp horizontal, 3dp vertical
 * - 모서리: 3dp
 * - 폰트: SUIT Variable ExtraBold, 9sp
 */
private val CategoryTagColors = mapOf(
    "OTT" to Color(0xFFEBCFFF),
    "SNS" to Color(0xFFFFC34B),
    "게임" to Color(0xFF818CFF),
    "쇼핑" to Color(0xFFA2A2A2),
    "웹툰" to Color(0xFF88C9FF),
    "주식,코인" to Color(0xFF3D9E5D),
    "주식·코인" to Color(0xFF3D9E5D),
    "기타" to Color(0xFFBDBDBD),
)

private fun categoryTagTextColor(tag: String): Color = when (tag) {
    "SNS" -> Color(0xFF553C0A)
    "OTT" -> Color(0xFF55366B)
    "기타" -> Color(0xFF424242)
    else -> Color.White
}

private fun categoryTagBgColor(tag: String): Color =
    CategoryTagColors[tag] ?: CategoryTagColors["기타"]!!

/** 정규화: 주식·코인, 주식/코인 등 → 주식,코인 (색상 조회용) */
private fun normalizeCategoryForKey(tag: String): String = when {
    tag == "주식·코인" || tag == "주식/코인" -> "주식,코인"
    else -> tag
}

/**
 * 디자인 가이드(DESIGNSYSTEM.md 948-3543) 준수 카테고리 태그.
 * 앱 목록, 통계, AI 분류 등 모든 화면에서 일관된 스타일 적용.
 */
@Composable
fun CategoryTag(
    tag: String,
    modifier: Modifier = Modifier,
) {
    val key = normalizeCategoryForKey(tag)
    val bgColor = categoryTagBgColor(key)
    val textColor = categoryTagTextColor(key)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = AppTypography.CategoryTag.copy(color = textColor),
        )
    }
}
