package com.aptox.app

import java.time.LocalDate
import kotlin.random.Random

/** 홈 상단 인삿말 시간대 (시각은 [hour] 0~23 기준) */
internal enum class HomeGreetingTimeBand {
    /** 6~11시 */
    Morning,
    /** 12~17시 */
    Afternoon,
    /** 18~21시 */
    Evening,
    /** 22~5시 */
    Night,
}

internal fun hourToHomeGreetingTimeBand(hour: Int): HomeGreetingTimeBand = when (hour) {
    in 6..11 -> HomeGreetingTimeBand.Morning
    in 12..17 -> HomeGreetingTimeBand.Afternoon
    in 18..21 -> HomeGreetingTimeBand.Evening
    else -> HomeGreetingTimeBand.Night
}

/**
 * 홈 인삿말 문구 상수.
 * 서브 멘트는 [LocalDate] + 시간대 기반 시드로 하루·시간대별로 고정(앱 재실행해도 동일).
 */
object HomeGreetingMents {

    private val TITLE_BY_BAND: Map<HomeGreetingTimeBand, String> = mapOf(
        HomeGreetingTimeBand.Morning to "좋은 아침이에요",
        HomeGreetingTimeBand.Afternoon to "안녕하세요",
        HomeGreetingTimeBand.Evening to "오늘 하루도 수고했어요",
        HomeGreetingTimeBand.Night to "늦은 시간이네요",
    )

    private val SUBS_MORNING = listOf(
        "오늘도 스마트폰보다 내가 이겨요",
        "좋은 하루의 시작은 절제에서 와요",
        "오늘 하루, 폰보다 나를 먼저 챙겨요",
        "아침부터 잘하고 있어요, 진짜로",
        "오늘도 어제보다 조금 더 나아질 거예요",
    )

    private val SUBS_AFTERNOON = listOf(
        "지금 이 순간도 절제가 쌓이고 있어요",
        "잘 버티고 있어요, 계속 이대로",
        "오늘 오후도 나쁘지 않은데요?",
        "조금씩이라도 달라지고 있어요",
        "지금 이 선택이 나중의 나를 만들어요",
    )

    private val SUBS_EVENING = listOf(
        "오늘 잘 버텼어요, 진짜로요",
        "하루 끝에 이 앱 보는 것만으로도 대단해요",
        "오늘 하루도 꽤 잘한 것 같은데요",
        "내일의 나는 오늘의 나보다 나을 거예요",
        "수고했어요, 오늘도 한 걸음 나아갔어요",
    )

    private val SUBS_NIGHT = listOf(
        "폰 내려놓고 내일을 충전해볼까요",
        "늦은 시간엔 폰보다 잠이 더 좋아요",
        "오늘은 여기까지, 내일 또 잘해봐요",
        "자기 전 폰은 내일의 나를 힘들게 해요",
        "지금 내려놓는 게 내일의 승리예요",
    )

    private fun subsForBand(band: HomeGreetingTimeBand): List<String> = when (band) {
        HomeGreetingTimeBand.Morning -> SUBS_MORNING
        HomeGreetingTimeBand.Afternoon -> SUBS_AFTERNOON
        HomeGreetingTimeBand.Evening -> SUBS_EVENING
        HomeGreetingTimeBand.Night -> SUBS_NIGHT
    }

    /**
     * @param date 기준일 (시드)
     * @param hourOfDay 0~23
     */
    fun pick(date: LocalDate, hourOfDay: Int): HomeGreeting {
        val band = hourToHomeGreetingTimeBand(hourOfDay)
        val title = TITLE_BY_BAND.getValue(band)
        val subs = subsForBand(band)
        val seed = date.toEpochDay() xor (band.ordinal.toLong() shl 42)
        val index = Random(seed).nextInt(subs.size)
        return HomeGreeting(title = title, subtext = subs[index])
    }
}
