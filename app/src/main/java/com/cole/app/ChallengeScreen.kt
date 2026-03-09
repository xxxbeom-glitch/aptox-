package com.cole.app

import com.cole.app.model.BadgeDefinition
import com.cole.app.model.BadgeMasterData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ChallengeCardShape = RoundedCornerShape(12.dp)
private val ChallengeCardShadowElevation = 8.dp
private val ChallengeCardShadowColor = Color.Black.copy(alpha = 0.1f)

private val BadgeCardHeight = 130.dp
private val BadgeBackgroundSize = 56.dp
private val BadgeIconSize = 36.dp
private val BadgeCardPadding = 16.dp
private val BadgeToTextGap = 6.dp
private val TextToDateGap = 4.dp

/**
 * 챌린지 화면 (Figma CH_01 310:1865)
 */
@Composable
fun ChallengeScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(AppColors.SurfaceBackgroundBackground),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 최근 받은 뱃지 카드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
                .clip(ChallengeCardShape)
                .background(AppColors.SurfaceBackgroundCard)
                .padding(vertical = 28.dp, horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 대표 뱃지 (첫 번째 획득 뱃지 - badge_004 첫 성취)
                val featuredBadge = BadgeMasterData.badges[3] // badge_004
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg_active),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Image(
                        painter = painterResource(featuredBadge.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = featuredBadge.title,
                        style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    )
                    Text(
                        text = featuredBadge.description,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 뱃지 그리드 (3열)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            challengeBadgeItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { item ->
                        BadgeGridItem(
                            modifier = Modifier.weight(1f),
                            item = item,
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

private data class ChallengeBadgeItem(
    val badge: BadgeDefinition,
    val earned: Boolean,
    val date: String,
)

/** 배지 그리드 아이템 (BadgeMasterData + 획득 여부/날짜 - 추후 Firestore users/{uid}/badges 연동) */
private val challengeBadgeItems: List<ChallengeBadgeItem> = BadgeMasterData.badges.mapIndexed { index, badge ->
    ChallengeBadgeItem(
        badge = badge,
        earned = index < 15, // 목업: 1~15 획득, 16~18 미획득
        date = "2026. 3.31",
    )
}

@Composable
private fun BadgeGridItem(
    modifier: Modifier = Modifier,
    item: ChallengeBadgeItem,
) {
    val textColor = if (item.earned) AppColors.TextBody else AppColors.TextDisabled
    val dateColor = if (item.earned) AppColors.TextCaption else AppColors.TextDisabled
    val bgResId = if (item.earned) R.drawable.bg_active else R.drawable.bg_disable

    Box(
        modifier = modifier
            .height(BadgeCardHeight)
            .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
            .clip(ChallengeCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(BadgeCardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BadgeToTextGap),
        ) {
            Box(
                modifier = Modifier.size(BadgeBackgroundSize),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(bgResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Image(
                    painter = painterResource(item.badge.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(BadgeIconSize),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TextToDateGap),
            ) {
                Text(
                    text = item.badge.title,
                    style = AppTypography.Caption1.copy(color = textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = item.date,
                    style = AppTypography.Caption1.copy(color = dateColor),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
