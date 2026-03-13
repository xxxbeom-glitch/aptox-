package com.aptox.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Figma Shadow/Card: X=0, Y=0, Blur=6, Spread=0, #000000 6% */
private val CardShadowColor = Color.Black.copy(alpha = 0.06f)

private enum class CenterPhase { TITLE, CARDS }

/** 애니메이션 설정: duration(ms), stagger(ms) */
private data class SmoothnessConfig(
    val durationMs: Int,
    val staggerMs: Int,
    val label: String = "",
)

/** Figma 1127-5788, 1127-5822: 스마트폰 사용 패턴 분석 화면 */
@Composable
fun UsagePatternAnalysisScreen(
    userName: String,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var titleVisible by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(CenterPhase.TITLE) }
    var card1Show by remember { mutableStateOf(false) }
    var card2Show by remember { mutableStateOf(false) }
    var card3Show by remember { mutableStateOf(false) }

    val config = SmoothnessConfig(800, 550, "보통")
    val ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val cardSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(600 + 1600)
        phase = CenterPhase.CARDS
        card1Show = true
        delay(200)
        card2Show = true
        delay(400)
        card3Show = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 타이틀 ↔ 카드 그룹 전환 (화면 중앙에 딱 위치)
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    fadeIn(animationSpec = tween(config.durationMs, easing = ease)) togetherWith
                        fadeOut(animationSpec = tween(config.durationMs, easing = ease))
                },
                label = "centerPhase",
            ) { state ->
                when (state) {
                    CenterPhase.TITLE -> AnimatedVisibility(
                        visible = titleVisible,
                        enter = fadeIn(animationSpec = tween(config.durationMs, easing = ease)),
                        exit = fadeOut(animationSpec = tween(config.durationMs, easing = ease)),
                    ) {
                        Column(
                            modifier = Modifier.width(328.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${userName}님의",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "스마트폰 사용패턴을",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "분석해봤어요",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    CenterPhase.CARDS -> {
                        val offset1 by animateDpAsState(
                            targetValue = if (card1Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card1Offset",
                        )
                        val alpha1 by animateFloatAsState(
                            targetValue = if (card1Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card1Alpha",
                        )
                        val offset2 by animateDpAsState(
                            targetValue = if (card2Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card2Offset",
                        )
                        val alpha2 by animateFloatAsState(
                            targetValue = if (card2Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card2Alpha",
                        )
                        val offset3 by animateDpAsState(
                            targetValue = if (card3Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card3Offset",
                        )
                        val alpha3 by animateFloatAsState(
                            targetValue = if (card3Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card3Alpha",
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset1)
                                    .alpha(alpha1),
                            ) {
                                UsagePatternCard1Animated(
                                    appName = "유튜브",
                                    totalHours = 304,
                                    recommendedHours = 1.0,
                                    averageHours = 6.5,
                                    config = config,
                                    ease = ease,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset2)
                                    .alpha(alpha2),
                            ) {
                                UsagePatternCard2Animated(
                                    message = "아영님은 OTT 앱을 상당히 많이 사용하시고 계세요",
                                    categoryBars = listOf(
                                        "OTT" to 133f / 160f,
                                        "쇼핑" to 96f / 160f,
                                        "SNS" to 51f / 160f,
                                    ),
                                    config = config,
                                    ease = ease,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset3)
                                    .alpha(alpha3),
                            ) {
                                UsagePatternCard3Animated(userName = userName, config = config, ease = ease)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val stepSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

/** 카드 1: 앱 사용량 요약 (유튜브 304시간) - 카드 → 타이틀 → 본문 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard1Animated(
    appName: String,
    totalHours: Int,
    recommendedHours: Double,
    averageHours: Double,
    config: SmoothnessConfig,
    ease: Easing,
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        step = 1
        delay(config.staggerMs.toLong())
        step = 2
        delay(config.staggerMs.toLong())
        step = 3
    }

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append(appName) }
                        append("를 ")
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append("${totalHours}시간") }
                        append("이나\n사용하셨어요")
                    },
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            AnimatedVisibility(
                visible = step >= 2,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = "${appName} 하루 권장 시청 사용 시간은 ${recommendedHours.toInt()}시간이에요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
            AnimatedVisibility(
                visible = step >= 3,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = "아영님은 하루 평균 ${averageHours}시간 사용하셨어요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
        }
    }
}

/** 카드 1: 앱 사용량 요약 (유튜브 304시간) - 기본 버전 */
@Composable
private fun UsagePatternCard1(
    appName: String,
    totalHours: Int,
    recommendedHours: Double,
    averageHours: Double,
) {
    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColors.Primary300)) { append(appName) }
                    append("를 ")
                    withStyle(SpanStyle(color = AppColors.Primary300)) { append("${totalHours}시간") }
                    append("이나\n사용하셨어요")
                },
                style = AppTypography.HeadingH3.copy(
                    color = AppColors.TextPrimary,
                    lineHeight = AppTypography.HeadingH3.lineHeight,
                ),
            )
            Text(
                text = "${appName} 하루 권장 시청 사용 시간은 ${recommendedHours.toInt()}시간이에요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
            Text(
                text = "아영님은 하루 평균 ${averageHours}시간 사용하셨어요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
        }
    }
}

/** 카드 2: 카테고리별 사용량 막대 - 타이틀 → 막대 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard2Animated(
    message: String,
    categoryBars: List<Pair<String, Float>>,
    config: SmoothnessConfig,
    ease: Easing,
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        step = 1
        categoryBars.forEachIndexed { i, _ ->
            delay(config.staggerMs.toLong())
            step = 2 + i
        }
    }

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("아영님은 ")
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append("OTT 앱") }
                        append("을\n")
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append("상당히 많이 사용하시고 계세요") }
                    },
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryBars.forEachIndexed { i, (label, ratio) ->
                    AnimatedVisibility(
                        visible = step >= 2 + i,
                        enter = fadeIn(animationSpec = stepSpringSpec),
                        exit = fadeOut(animationSpec = stepSpringSpec),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.Caption1.copy(color = AppColors.TextTertiary),
                                modifier = Modifier.width(36.dp),
                            )
                            CategoryBar(ratio = ratio, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(AppColors.Grey100),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedRatio)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(AppColors.Primary300),
        )
    }
}

/** 카드 3: 밤 11시~12시 시간대별 그래프 - 타이틀 → 차트 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard3Animated(
    userName: String,
    config: SmoothnessConfig,
    ease: Easing,
) {
    val context = LocalContext.current
    var timeSlotMinutes by remember { mutableStateOf<List<Long>?>(null) }
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val (startMs, endMs) = StatisticsData.getLastNDaysRange(7, 0).let { (s, e, _) -> s to e }
            StatisticsData.loadTimeSlot12Minutes(context, startMs, endMs, 0)
        }
        timeSlotMinutes = loaded
    }

    LaunchedEffect(Unit) {
        step = 1
        delay(config.staggerMs.toLong())
        step = 2
    }

    // 실제 데이터 없을 때 Figma 디자인용 더미 (18~24시 peak)
    val padded = timeSlotMinutes?.let { if (it.size >= 12) it.take(12) else it + List(12 - it.size) { 0L } }
        ?: listOf(6L, 6L, 8L, 35L, 74L, 62L, 52L, 25L, 62L, 81L, 109L, 87L)
    val maxIdx = padded.indices.maxByOrNull { padded[it] }?.takeIf { padded[it] > 0 } ?: 10
    val timeSlotMaxMinutes = 120L
    val normalized = padded.map { (it.toFloat() / timeSlotMaxMinutes).coerceIn(0f, 1f) }

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = "밤 11시부터 12시까지\n사용량이 가장 많았어요",
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            AnimatedVisibility(
                visible = step >= 2,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                TimeSlotBarChartComponent(
                    values = normalized,
                    maxValueIdx = maxIdx,
                    showSpeechBubble = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UsagePatternCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                clip = false,
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .background(AppColors.SurfaceBackgroundCard, cardShape)
            .padding(horizontal = 16.dp, vertical = 26.dp),
    ) {
        content()
    }
}
