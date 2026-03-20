package com.aptox.app.model

import com.aptox.app.R

/**
 * 배지 마스터 데이터 (Firestore badges 컬렉션과 동기화 예정)
 */
data class BadgeDefinition(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val condition: String,
    val icon: String,
    val message: String? = null,
) {
    val iconResId: Int get() = when (icon) {
        "ico_level1" -> R.drawable.ico_level1
        "ico_level2" -> R.drawable.ico_level2
        "ico_level3" -> R.drawable.ico_level3
        "ico_level4" -> R.drawable.ico_level4
        "ico_level5" -> R.drawable.ico_level5
        "ico_level6" -> R.drawable.ico_level6
        "ico_level7" -> R.drawable.ico_level7
        "ico_level8" -> R.drawable.ico_level8
        "ico_level9" -> R.drawable.ico_level9
        "ico_level10" -> R.drawable.ico_level10
        "ico_level11" -> R.drawable.ico_level11
        "ico_level12" -> R.drawable.ico_level12
        "ico_level13" -> R.drawable.ico_level13
        "ico_level14" -> R.drawable.ico_level14
        "ico_level15" -> R.drawable.ico_level15
        "ico_level16" -> R.drawable.ico_level16
        "ico_level17" -> R.drawable.ico_level17
        "ico_level18" -> R.drawable.ico_level18
        "ico_lock_challange" -> R.drawable.ico_lock_challange
        else -> R.drawable.ico_level1
    }
}

/**
 * 배지 마스터 데이터 (Firestore badges 컬렉션 시드)
 */
object BadgeMasterData {
    val badges: List<BadgeDefinition> = listOf(
        BadgeDefinition("badge_001", 1, "첫 걸음", "디지털 디톡스의 시작", "앱 제한을 처음 설정했을 때", "ico_level1"),
        BadgeDefinition("badge_002", 2, "일주일 연속", "제한 설정을 일주일 연속 지켰어요", "제한 설정 7일 연속 달성", "ico_level2"),
        BadgeDefinition("badge_003", 3, "한 달 연속", "제한 설정을 한 달 연속 지켰어요", "제한 설정 30일 연속 달성", "ico_level3"),
        BadgeDefinition("badge_004", 4, "첫 성취", "오늘 하루를 지켜냈어요", "일일 사용량 제한 목표 첫 달성", "ico_level4", message = "오늘 하루를 지켜냈어요. 내일도 할 수 있어요!"),
        BadgeDefinition("badge_005", 5, "꾸준한 실천", "반복이 실력이 되는 중", "일일 사용량 제한 누적 7회 달성", "ico_level5"),
        BadgeDefinition("badge_006", 6, "불굴의 의지", "의지력이 증명된 사람", "일일 사용량 제한 누적 30회 달성", "ico_level6"),
        BadgeDefinition("badge_007", 7, "시간지정 첫 성공", "시간지정 차단을 처음 완료했어요", "시간지정 차단 첫 성공", "ico_level7"),
        BadgeDefinition("badge_008", 8, "시간지정 습관", "시간지정 차단을 꾸준히 지키고 있어요", "시간지정 차단 누적 7회 성공", "ico_level8"),
        BadgeDefinition("badge_009", 9, "시간지정 달인", "시간지정 절제에 익숙해졌어요", "시간지정 차단 누적 30회 성공", "ico_level9"),
        BadgeDefinition("badge_010", 10, "밤 절제 첫걸음", "밤 10시 이후 차단을 처음 완료했어요", "밤 10시 이후 차단 첫 성공", "ico_level10"),
        BadgeDefinition("badge_011", 11, "밤 일주일", "밤 시간 절제를 일주일 연속 지켰어요", "밤 10시 이후 차단 7일 연속 성공", "ico_level11"),
        BadgeDefinition("badge_012", 12, "밤 한 달", "밤 시간 절제가 습관이 됐어요", "밤 10시 이후 차단 30일 연속 성공", "ico_level12"),
        BadgeDefinition("badge_013", 13, "첫 방어", "차단을 처음 막아냈어요", "차단 중 앱 접근 시도 첫 방어", "ico_level13"),
        BadgeDefinition("badge_014", 14, "단단한 방어", "유혹을 여러 번 막아냈어요", "차단 중 앱 접근 누적 50회 방어", "ico_level14"),
        BadgeDefinition("badge_015", 15, "철벽 방어", "의지의 벽이 높아졌어요", "차단 중 앱 접근 누적 200회 방어", "ico_level15"),
        BadgeDefinition("badge_016", 16, "사용 줄이기 10%", "지난 주보다 사용 시간을 줄였어요", "총 사용시간 전주 대비 10% 감소", "ico_level16"),
        BadgeDefinition("badge_017", 17, "사용 줄이기 30%", "눈에 띄게 줄이고 있어요", "총 사용시간 전주 대비 30% 감소", "ico_level17"),
        BadgeDefinition("badge_018", 18, "사용 줄이기 50%", "큰 변화를 만들었어요", "총 사용시간 전주 대비 50% 감소", "ico_level18"),
    )
}
