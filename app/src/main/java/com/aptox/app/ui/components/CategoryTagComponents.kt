package com.aptox.app.ui.components

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
import com.aptox.app.AppColors
import com.aptox.app.AppTypography

/**
 * ???? ?? (Figma 948-3543, DESIGNSYSTEM.md)
 * - ??: 4dp horizontal, 3dp vertical
 * - ???: 3dp
 * - ??: SUIT Variable ExtraBold, 9sp
 */
private val CategoryTagColors = mapOf(
    "OTT" to AppColors.CategoryTagOttBg,
    "SNS" to AppColors.CategoryTagSnsBg,
    "??" to AppColors.CategoryTagGameBg,
    "??" to AppColors.CategoryTagShoppingBg,
    "??" to AppColors.CategoryTagWebtoonBg,
    "??,??" to AppColors.CategoryTagStockBg,
    "????" to AppColors.CategoryTagStockBg,
    "??" to AppColors.CategoryTagOtherBg,
)

private fun categoryTagTextColor(tag: String): Color = when (tag) {
    "SNS" -> AppColors.CategoryTagSnsText
    "OTT" -> AppColors.CategoryTagOttText
    "??" -> AppColors.CategoryTagOtherText
    else -> AppColors.CategoryTagOnColorText
}

private fun categoryTagBgColor(tag: String): Color =
    CategoryTagColors[tag] ?: CategoryTagColors["??"]!!

/** ???: ????, ??/?? ? ? ??,?? (?? ???) */
private fun normalizeCategoryForKey(tag: String): String = when {
    tag == "????" || tag == "??/??" -> "??,??"
    else -> tag
}

/**
 * ??? ???(DESIGNSYSTEM.md 948-3543) ?? ???? ??.
 * ? ??, ??, AI ?? ? ?? ???? ??? ??? ??.
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
