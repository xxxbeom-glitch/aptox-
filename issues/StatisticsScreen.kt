package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 통계 화면 AN-01 (Figma 919-3517)
 * - 탭: 오늘/주간/월간/연간
 * - 인사이트 카드 (연속 달성일, 달성율, 유지율)
 * - 날짜 범위 선택 + 요일별 막대 차트
 * - 스택 바 (카테고리 비율) + 최다 앱 리스트
 * - 제한 방식 필터 (시간 지정/일일) + 제한 앱 리스트
 * - 그룹 막대 차트 (전주 vs 이번주)
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // 기본 주간
    val tabLabels = listOf("오늘", "주간", "월간", "연간")
    val tabEnum = when (selectedTab) {
        0 -> StatisticsData.Tab.TODAY
        1 -> StatisticsData.Tab.WEEKLY
        2 -> StatisticsData.Tab.MONTHLY
        else -> StatisticsData.Tab.YEARLY
    }

    // 주간용 날짜 네비게이션 (0=이번 주)
    var weekOffset by remember { mutableIntStateOf(0) }
    val (weekStartMs, weekEndMs, dateRangeText) = remember(weekOffset) {
        StatisticsData.getWeekRange(weekOffset)
    }

    // 요일별 데이터 (주간 탭)
    var dayOfWeekMinutes by remember { mutableStateOf<List<Long>>(List(7) { 0L }) }
    var appList by remember { mutableStateOf<List<StatisticsData.StatsAppItem>>(emptyList()) }
    var comparisonData by remember { mutableStateOf<Pair<List<Long>, List<Long>>>(Pair(List(7) { 0L }, List(7) { 0L })) }

    LaunchedEffect(tabEnum, weekOffset) {
        withContext(Dispatchers.IO) {
            if (tabEnum == StatisticsData.Tab.WEEKLY) {
                dayOfWeekMinutes = StatisticsData.loadDayOfWeekMinutes(context, weekStartMs, weekEndMs)
                appList = StatisticsData.loadAppUsage(context, weekStartMs, weekEndMs)
                comparisonData = StatisticsData.loadWeekComparisonMinutes(context, weekOffset)
            } else {
                val (startMs, endMs) = StatisticsData.getTimeRange(context, tabEnum)
                appList = StatisticsData.loadAppUsage(context, startMs, endMs)
            }
        }
    }

    val restrictedApps = remember { AppRestrictionRepository(context).getAll() }
    var restrictionFilter by remember { mutableIntStateOf(0) } // 0=시간 지정, 1=일일
    val timeSpecifiedApps = restrictedApps.filter { it.blockUntilMs > 0 }
    val dailyLimitApps = restrictedApps.filter { it.blockUntilMs == 0L }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        ColeSegmentedTab(
            items = tabLabels,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )

        if (tabEnum == StatisticsData.Tab.WEEKLY) {
            StatsInsightCard()
            StatsDateChartSection(
                dateRangeText = dateRangeText,
                weekOffset = weekOffset,
                onWeekChange = { weekOffset = it },
                dayMinutes = dayOfWeekMinutes,
            )
            StatsStackedBarAndAppList(
                dateRangeText = dateRangeText,
                weekOffset = weekOffset,
                onWeekChange = { weekOffset = it },
                appList = appList,
            )
            StatsRestrictionSection(
                dateRangeText = dateRangeText,
                weekOffset = weekOffset,
                onWeekChange = { weekOffset = it },
                filterIndex = restrictionFilter,
                onFilterChange = { restrictionFilter = it },
                timeSpecifiedApps = timeSpecifiedApps,
                dailyLimitApps = dailyLimitApps,
            )
            StatsGroupedBarSection(
                dateRangeText = dateRangeText,
                weekOffset = weekOffset,
                onWeekChange = { weekOffset = it },
                comparisonData = comparisonData,
            )
        } else {
            StatsLegacyContent(
                tabEnum = tabEnum,
                appList = appList,
            )
        }
    }
}

/** 인사이트 카드 (Figma 926-8043) */
@Composable
private fun StatsInsightCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "지난주와 비슷한 한 주예요",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "지난주와 비슷한 패턴이에요. 꾸준히 유지하고 있다는 것 자체가 대단해요. 한 가지만 더 개선한다면 저녁 시간대 사용을 줄여보세요.",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatsInsightStatItem(label = "연속 달성일", value = "56일")
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(AppColors.BorderDivider))
            StatsInsightStatItem(label = "달성율", value = "65%")
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(AppColors.BorderDivider))
            StatsInsightStatItem(label = "유지율", value = "50%")
        }
    }
}

@Composable
private fun StatsInsightStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
        )
        Text(
            text = value,
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
    }
}

/** 날짜 Row + 요일별 막대 차트 (Figma 919-3520) */
@Composable
private fun StatsDateChartSection(
    dateRangeText: String,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    dayMinutes: List<Long>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        StatsDateRangeRow(
            dateRangeText = dateRangeText,
            canGoPrev = true,
            canGoNext = weekOffset < 0,
            onPrevClick = { onWeekChange(weekOffset - 1) },
            onNextClick = { if (weekOffset < 0) onWeekChange(weekOffset + 1) },
        )
        DayOfWeekBarChart(values = dayMinutes)
    }
}

/** 날짜 범위 Row (Figma 948-3627) */
@Composable
private fun StatsDateRangeRow(
    dateRangeText: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoPrev) Modifier.clickable { onPrevClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavLeft(enabled = canGoPrev, size = 22.dp)
            }
            Text(
                text = dateRangeText,
                style = AppTypography.Caption2.copy(color = AppColors.TextPrimary),
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoNext) Modifier.clickable { onNextClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavRight(enabled = canGoNext, size = 22.dp)
            }
        }
    }
}

private val DayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private val BarChartHeight = 126.dp
private val BarWidth = 26.dp
private val BarCornerRadius = 2.dp

@Composable
private fun DayOfWeekBarChart(
    values: List<Long>,
    modifier: Modifier = Modifier,
) {
    val padded = if (values.size >= 7) values.take(7) else values + List(7 - values.size) { 0L }
    val maxVal = padded.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val normalized = padded.map { (it.toFloat() / maxVal).coerceIn(0f, 1f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(BarChartHeight)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                for (i in 0..6) {
                    val y = size.height * (i / 6f)
                    drawLine(
                        color = AppColors.ChartGuideline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        pathEffect = pathEffect,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                normalized.forEachIndexed { index, value ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(BarWidth)
                                    .fillMaxHeight(value)
                                    .clip(RoundedCornerShape(BarCornerRadius))
                                    .background(AppColors.ChartTrackFill),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DayLabels.forEach { label ->
                Text(
                    text = label,
                    style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 카테고리 색상 (DESIGNSYSTEM 948-3543) */
private val CategoryColors = mapOf(
    "OTT" to Color(0xFFEBCFFF),
    "SNS" to Color(0xFFFFC34B),
    "게임" to Color(0xFF818CFF),
    "쇼핑" to Color(0xFFA2A2A2),
    "웹툰" to Color(0xFF88C9FF),
    "주식/코인" to Color(0xFF3D9E5D),
)

/** 스택 바 + 앱 리스트 (Figma 925-7299) */
@Composable
private fun StatsStackedBarAndAppList(
    dateRangeText: String,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    appList: List<StatisticsData.StatsAppItem>,
    modifier: Modifier = Modifier,
) {
    val segments = listOf(
        "OTT" to 36f,
        "SNS" to 20f,
        "게임" to 19f,
        "쇼핑" to 18f,
        "웹툰" to 15f,
        "주식/코인" to 2f,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        StatsDateRangeRow(
            dateRangeText = dateRangeText,
            canGoPrev = true,
            canGoNext = weekOffset < 0,
            onPrevClick = { onWeekChange(weekOffset - 1) },
            onNextClick = { if (weekOffset < 0) onWeekChange(weekOffset + 1) },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.ChartTrackBackground),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                segments.forEach { (label, pct) ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(pct / 100f)
                            .background(CategoryColors[label] ?: Color.Gray),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            segments.chunked(2).forEach { chunk ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    chunk.forEach { (label, pct) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(CategoryColors[label] ?: Color.Gray),
                                )
                                Text(
                                    text = label,
                                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                                )
                            }
                            Text(
                                text = "${pct.toInt()}%",
                                style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            appList.take(5).forEach { app ->
                StatsAppRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    categoryTag = app.categoryTag,
                    showDangerLabel = app.isRestricted,
                )
            }
        }
    }
}

@Composable
private fun StatsAppRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    categoryTag: String?,
    showDangerLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = appIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp)),
                tint = Color.Unspecified,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    categoryTag?.let { tag ->
                        StatsCategoryTag(tag = tag)
                    }
                    if (showDangerLabel) {
                        LabelDanger()
                    }
                }
                Text(
                    text = name,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = usageMinutes,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
        )
    }
}

@Composable
private fun StatsCategoryTag(tag: String, modifier: Modifier = Modifier) {
    val bgColor = CategoryColors[tag] ?: AppColors.Grey350
    val textColor = when (tag) {
        "SNS" -> Color(0xFF553C0A)
        "OTT" -> Color(0xFF55366B)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = AppTypography.LabelDanger.copy(color = textColor),
        )
    }
}

/** 제한 방식 필터 + 제한 앱 리스트 (Figma 925-7436, 948-3689) */
@Composable
private fun StatsRestrictionSection(
    dateRangeText: String,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    filterIndex: Int,
    onFilterChange: (Int) -> Unit,
    timeSpecifiedApps: List<com.cole.app.model.AppRestriction>,
    dailyLimitApps: List<com.cole.app.model.AppRestriction>,
    modifier: Modifier = Modifier,
) {
    val displayApps = if (filterIndex == 0) timeSpecifiedApps else dailyLimitApps

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        StatsDateRangeRow(
            dateRangeText = dateRangeText,
            canGoPrev = true,
            canGoNext = weekOffset < 0,
            onPrevClick = { onWeekChange(weekOffset - 1) },
            onNextClick = { if (weekOffset < 0) onWeekChange(weekOffset + 1) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("시간 지정 제한", "일일 사용량 제한").forEachIndexed { index, label ->
                val selected = filterIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) AppColors.Primary50 else Color.Transparent)
                        .clickable { onFilterChange(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = if (selected) AppTypography.Caption2.copy(color = AppColors.TextHighlight)
                        else AppTypography.Caption1.copy(color = AppColors.TextDisabled),
                    )
                }
            }
        }

        if (displayApps.isEmpty()) {
            Text(
                text = "제한 중인 앱이 없어요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                displayApps.forEach { app ->
                    val appIcon = rememberAppIconPainter(app.packageName)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = appIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                tint = Color.Unspecified,
                            )
                            Text(
                                text = app.appName,
                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                            )
                        }
                        Text(
                            text = "${app.limitMinutes}분",
                            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                        )
                    }
                }
            }
        }

        ColeInfoBoxCompact(
            text = "제한 앱은 설정에서 수정할 수 있어요",
        )
    }
}

/** 그룹 막대 차트 (Figma 948-3696) */
@Composable
private fun StatsGroupedBarSection(
    dateRangeText: String,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    comparisonData: Pair<List<Long>, List<Long>>,
    modifier: Modifier = Modifier,
) {
    val (thisWeek, prevWeek) = comparisonData
    val maxVal = (thisWeek + prevWeek).maxOrNull()?.takeIf { it > 0 } ?: 1L
    val thisNorm = thisWeek.map { (it.toFloat() / maxVal).coerceIn(0f, 1f) }
    val prevNorm = prevWeek.map { (it.toFloat() / maxVal).coerceIn(0f, 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        StatsDateRangeRow(
            dateRangeText = dateRangeText,
            canGoPrev = true,
            canGoNext = weekOffset < 0,
            onPrevClick = { onWeekChange(weekOffset - 1) },
            onNextClick = { if (weekOffset < 0) onWeekChange(weekOffset + 1) },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(BarChartHeight)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..6) {
                        val y = size.height * (i / 6f)
                        drawLine(
                            color = AppColors.ChartGuideline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    thisNorm.forEachIndexed { index, v1 ->
                        val v2 = prevNorm.getOrElse(index) { 0f }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(11.dp)
                                        .fillMaxHeight(v2)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(AppColors.Grey350),
                                )
                                Box(
                                    modifier = Modifier
                                        .width(11.dp)
                                        .fillMaxHeight(v1)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(AppColors.ChartTrackFill),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        ColeInfoBoxCompact(
            text = "보라색: 이번 주, 회색: 저번 주",
        )
    }
}

/** 오늘/월간/연간 탭용 기존 스타일 콘텐츠 */
@Composable
private fun StatsLegacyContent(
    tabEnum: StatisticsData.Tab,
    appList: List<StatisticsData.StatsAppItem>,
) {
    var timeSlotMinutes by remember { mutableStateOf<List<Long>>(List(8) { 0L }) }
    val context = LocalContext.current

    LaunchedEffect(tabEnum) {
        withContext(Dispatchers.IO) {
            timeSlotMinutes = StatisticsData.loadTimeSlotMinutes(context, tabEnum)
        }
    }

    val maxSlot = timeSlotMinutes.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val values = timeSlotMinutes.map { (it.toFloat() / maxSlot).coerceIn(0f, 1f) }
    val visibleSlotIndex = StatisticsData.getCurrentSlotIndex()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TimeSlotUsageCard(values = values, visibleSlotIndex = visibleSlotIndex)
        AppUsageCard(apps = appList)
    }
}

private val TopAppInfoTexts = listOf(
    "이 시간이면 서울 부산 KTX 왕복 8번이에요!",
    "해리포터 시리즈 전편을 두번 반복한 시간보다 많아요!",
    "손흥민이 토트넘에서 뛴 시간과 비슷해요",
    "10층짜리 빌딩을 짓는 시간보다 많아요",
    "이 정도면 아이유 콘서트 시간이랑 맞먹는 시간이에요",
)

@Composable
private fun TimeSlotUsageCard(
    values: List<Float>,
    visibleSlotIndex: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values, visibleSlotIndex) {
        progress.animateTo(0f, tween(150))
        progress.animateTo(1f, tween(600))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "시간대별 사용량",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(18.dp))
        }

        val paddedValues = if (values.size >= 8) values.take(8) else values + List(8 - values.size) { 0f }
        Row(
            modifier = Modifier.fillMaxWidth().height(126.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            paddedValues.forEachIndexed { index, value ->
                val isVisible = index <= visibleSlotIndex
                val barProgress = if (isVisible) ((progress.value * (visibleSlotIndex + 1)) - index).coerceIn(0f, 1f) else 0f
                val animatedHeight = if (isVisible) (value * barProgress).coerceIn(0f, 1f) else 0f
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .width(BarWidth)
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(BarCornerRadius))
                                    .background(AppColors.ChartTrackFill),
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatisticsData.SlotLabels.forEach { label ->
                Text(
                    text = "$label",
                    style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    apps: List<StatisticsData.StatsAppItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "사용시간 최다 앱",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
            apps.take(5).forEachIndexed { index, app ->
                StatsAppDataViewRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    isRestricted = app.isRestricted,
                    infoText = TopAppInfoTexts.getOrNull(index),
                )
            }
        }
    }
}

@Composable
private fun StatsAppDataViewRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    isRestricted: Boolean,
    infoText: String?,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    AppStatusDataViewRow(
        appName = name,
        appIcon = appIcon,
        totalUsageMinutes = usageMinutes,
        modifier = modifier,
        showDangerLabel = isRestricted,
        showLock = isRestricted,
        infoText = infoText,
    )
}
