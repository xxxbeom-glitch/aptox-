package com.cole.app.model

import com.cole.app.R

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
        BadgeDefinition("badge_001", 1, "첫 걸음", "디지털 디톡스의 시작", "처음으로 앱 제한 설정 완료", "ico_level1"),
        BadgeDefinition("badge_002", 2, "절제의 길", "꾸준히 걷고 있는 중", "제한 설정 7일 연속 달성", "ico_level2"),
        BadgeDefinition("badge_003", 3, "절제 완성", "절제가 습관이 된 사람", "제한 설정 30일 연속 달성", "ico_level3"),
        BadgeDefinition("badge_004", 4, "첫 성취", "오늘 하루를 지켜냈어요", "일일 사용량 제한 첫 달성", "ico_level4"),
        BadgeDefinition("badge_005", 5, "꾸준한 실천", "반복이 실력이 되는 중", "일일 사용량 제한 누적 7회 달성", "ico_level5"),
        BadgeDefinition("badge_006", 6, "불굴의 의지", "의지력이 증명된 사람", "일일 사용량 제한 누적 30회 달성", "ico_level6"),
        BadgeDefinition("badge_007", 7, "집중 시작", "집중의 첫 불꽃", "시간지정 차단 첫 성공", "ico_level7"),
        BadgeDefinition("badge_008", 8, "집중 유지", "집중력이 단단해지는 중", "시간지정 차단 누적 7회 성공", "ico_level8"),
        BadgeDefinition("badge_009", 9, "집중 마스터", "집중력의 끝판왕", "시간지정 차단 누적 30회 성공", "ico_level9"),
        BadgeDefinition("badge_010", 10, "야간 절제", "밤을 지킨 첫날", "밤 10시 이후 차단 첫 성공", "ico_level10"),
        BadgeDefinition("badge_011", 11, "야간 수호", "수면을 스스로 지키는 사람", "밤 10시 이후 차단 7일 연속 성공", "ico_level11"),
        BadgeDefinition("badge_012", 12, "야간 마스터", "건강한 수면 루틴 완성", "밤 10시 이후 차단 30일 연속 성공", "ico_level12"),
        BadgeDefinition("badge_013", 13, "첫 차단", "유혹을 처음으로 막아냈어요", "차단 중 앱 접근 시도 첫 방어", "ico_level13"),
        BadgeDefinition("badge_014", 14, "철벽 수비", "흔들리지 않는 의지", "차단 중 앱 접근 시도 누적 50회 방어", "ico_level14"),
        BadgeDefinition("badge_015", 15, "난공불락", "어떤 유혹도 통하지 않는 사람", "차단 중 앱 접근 시도 누적 200회 방어", "ico_level15"),
        BadgeDefinition("badge_016", 16, "첫 도전", "변화가 시작됐어요", "총 사용시간 10% 감소 달성", "ico_level16"),
        BadgeDefinition("badge_017", 17, "도전 계속", "눈에 띄는 변화가 생겼어요", "총 사용시간 30% 감소 달성", "ico_level17"),
        BadgeDefinition("badge_018", 18, "도전 완주", "완전히 다른 삶을 살고 있어요", "총 사용시간 50% 감소 달성", "ico_level18"),
    )
}
