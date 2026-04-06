package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/** [StatsDateChartCategoryTabChip]와 동일 pill (보라 선택) */
private val DebugStatsPillWidth = 70.dp
private val DebugStatsPillHeight = 30.dp

@Composable
fun DebugStatsPeriodPillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) AppColors.Primary50 else AppColors.SurfaceBackgroundCard
    val fg = if (selected) AppColors.Primary300 else AppColors.TextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(
                interactionSource = remember(text) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonSmall.copy(color = fg),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DebugStatsPeriodPillsRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DebugStatsPillHeight)
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            DebugStatsPeriodPillChip(
                text = label,
                selected = index == selectedIndex,
                onClick = { onSelectIndex(index) },
                modifier = Modifier
                    .width(DebugStatsPillWidth)
                    .fillMaxHeight(),
            )
        }
    }
}

private val KoreanWeekdays = arrayOf("일", "월", "화", "수", "목", "금", "토")

fun debugTodayIndexFromMonday(): Int {
    val cal = Calendar.getInstance()
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    return (dow + 5) % 7
}

/** 매월 1일: M/1(요일), 그 외: d(요일) */
fun formatDebugDailyPillLabel(cal: Calendar): String {
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val w = KoreanWeekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return if (day == 1) "$month/1($w)" else "$day($w)"
}

fun debugThisWeekMondayStartMillis(): Long {
    val cal = Calendar.getInstance()
    val daysFromMonday = debugTodayIndexFromMonday()
    cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 일간: 이번 주 월~오늘 pill 라벨 */
fun debugBuildDailyPillLabels(): List<String> {
    val weekStart = debugThisWeekMondayStartMillis()
    val n = debugTodayIndexFromMonday() + 1
    return (0 until n).map { i ->
        val c = Calendar.getInstance()
        c.timeInMillis = weekStart
        c.add(Calendar.DAY_OF_YEAR, i)
        formatDebugDailyPillLabel(c)
    }
}

/** 선택한 날(월 기준 인덱스)의 start/end ms. 오늘이면 end = min(now, 일말) */
fun debugDailyRangeForDayIndex(dayIndexFromMonday: Int): Pair<Long, Long> {
    val weekStart = debugThisWeekMondayStartMillis()
    val c = Calendar.getInstance()
    c.timeInMillis = weekStart
    c.add(Calendar.DAY_OF_YEAR, dayIndexFromMonday)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val start = c.timeInMillis
    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 999)
    val endOfDay = c.timeInMillis
    val todayIdx = debugTodayIndexFromMonday()
    val end = if (dayIndexFromMonday == todayIdx) {
        min(System.currentTimeMillis(), endOfDay)
    } else {
        endOfDay
    }
    return start to end
}

/** 주간: 최근 4주, 라벨 N주차 (월요일 기준 주차) */
fun debugBuildWeeklyPillLabels(): List<String> {
    val weekOffsets = listOf(-3, -2, -1, 0)
    return weekOffsets.map { wo ->
        val (s, _, _) = StatisticsData.getWeekRange(wo)
        val c = Calendar.getInstance(Locale.KOREA)
        c.timeInMillis = s
        c.firstDayOfWeek = Calendar.MONDAY
        c.minimalDaysInFirstWeek = 4
        "${c.get(Calendar.WEEK_OF_YEAR)}주차"
    }
}

fun debugWeeklyRangeForPillIndex(pillIndex: Int): Pair<Long, Long> {
    val wo = pillIndex - 3
    val (s, e, _) = StatisticsData.getWeekRange(wo)
    return s to e
}

/** 월간: 최근 4개월 "M월" */
fun debugBuildMonthlyPillLabels(): List<String> {
    return listOf(-3, -2, -1, 0).map { mo ->
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, mo)
        "${c.get(Calendar.MONTH) + 1}월"
    }
}

/** 월간 pill: index 0=-3달 … 3=이번 달. 이번 달은 1일~오늘 23:59:59.999 */
fun debugMonthlyRangeForPillIndex(pillIndex: Int): Pair<Long, Long> {
    val mo = pillIndex - 3
    if (mo == 0) {
        val startCal = Calendar.getInstance()
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val start = startCal.timeInMillis
        val endCal = Calendar.getInstance()
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        return start to endCal.timeInMillis
    }
    val (s, e, _) = StatisticsData.getSingleMonthRange(mo)
    return s to e
}

fun debugDivideByDaysForRange(startMs: Long, endMs: Long, mainTab: Int): Int {
    return when (mainTab) {
        0 -> 0
        else -> StatisticsData.daysInclusiveCappedAtNow(startMs, endMs)
    }
}
