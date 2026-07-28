# aptox 앱 알림 텍스트 현황

> 현황 파악용 문서. 텍스트 수정 시 참고.

---

## 1. 푸시 알림 (Notification)

| 알림 종류 | 현재 title | 현재 body | 위치(파일명) |
|-----------|------------|-----------|---------------|
| **하루 사용량 - 23:55 리셋 예고** | 5분 후 $appName 하루 사용량 시간이 초기화돼요 | (null) | `DailyUsageNotificationHelper.kt` (33-34줄) |
| **하루 사용량 - 5분 전 경고** | $appName 제한까지 5분 남았어요 | (null) | `DailyUsageNotificationHelper.kt` (136-137줄) |
| **하루 사용량 - 제한 도달** | $appName 오늘 사용량을 모두 소진했어요 | (null) | `DailyUsageNotificationHelper.kt` (155-156줄) |
| **하루 사용량 - 마감 임박(1분 전)** | 사용 시간이 얼마 남지 않았어요 ⚠️ | $appName 사용 가능 시간이 1분 남았어요 | `DailyUsageNotificationHelper.kt` (112-113줄) |
| **목표 달성(뱃지 획득)** | 🏅 새로운 뱃지를 획득했어요! | $badgeTitle 뱃지를 획득했어요. 확인해보세요! | `GoalAchievementNotificationHelper.kt` (34-35줄) |
| **카운트 미중지** | 카운트가 진행 중이에요 ⏱ | $appName 카운트가 아직 진행 중이에요. 중지해주세요 | `CountReminderNotificationHelper.kt` (34-35줄) |
| **주간 리포트** | 지난주 리포트가 도착했어요 | BriefSummaryPreloader 요약 또는 "지난주 스마트폰 사용 리포트를 확인하세요" | `WeeklyReportNotificationHelper.kt` (34-35줄), 실제 전달값은 `WeeklyReportWorker.kt` (20-22줄) |
| **일시정지 종료 1분 전 경고** | $appName | 1분 후 다시 사용이 제한됩니다 | `PauseTimerNotificationService.kt` (145-146줄) |

*`$appName`, `$badgeTitle` 등은 실제 값으로 치환됨.*

---

## 2. 포그라운드 서비스 알림

| 알림 종류 | 현재 title | 현재 body | 위치(파일명) |
|-----------|------------|-----------|---------------|
| **앱 차단 오버레이 서비스** | 앱 사용 제한 중 | (없음) | `BlockOverlayService.kt` (100줄) |
| **앱 모니터링 서비스 (기본)** | 앱 사용 시간을 기록하고 있어요 | (빈칸) | `AppMonitorService.java` (144-145줄) |
| **앱 모니터링 - 카운트 진행 중** | 앱 사용 시간을 기록하고 있어요 | $appName 사용 중 · 남은 시간 HH:MM:SS | `AppMonitorService.java` (182-184줄) |
| **일시정지 타이머** | $appName 일시정지 중 | 남은 시간: $timeText (M:SS 형식) | `PauseTimerNotificationService.kt` (128-129줄) |

---

## 3. 알림 액션 버튼

| 알림 종류 | 액션 텍스트 | 위치(파일명) |
|-----------|-------------|---------------|
| **앱 모니터링 - 카운트 중** | 카운트 중지 | `AppMonitorService.java` (186줄) |

---

## 4. 알림 채널 (설정 화면에 표시되는 이름/설명)

| 채널 ID | 채널 이름 | 채널 설명 | 위치(파일명) |
|---------|-----------|-----------|---------------|
| daily_usage_limit | 하루 사용량 알림 | 하루 사용량 리셋, 제한 경고 알림 | `DailyUsageNotificationHelper.kt` (24, 181줄) |
| goal_achievement | 목표 달성 | 챌린지 목표 달성 알림 | `GoalAchievementNotificationHelper.kt` (18-19, 48줄) |
| count_reminder | 카운트 중지 | 카운트 중지 잊음 알림 | `CountReminderNotificationHelper.kt` (18-19, 59줄) |
| weekly_report | 주간 리포트 | 주간 사용 리포트 알림 | `WeeklyReportNotificationHelper.kt` (18-19, 48줄) |
| block_overlay | 앱 차단 | (설명 없음) | `BlockOverlayService.kt` (84-86줄) |
| app_monitor | 앱 모니터링 | (설명 없음) | `AppMonitorService.java` (335-336줄) |
| pause_timer | 일시정지 타이머 | (설명 없음) | `PauseTimerNotificationService.kt` (159줄) |
| pause_warning | 일시정지 종료 알림 | 일시정지 1분 전 알림 | `PauseTimerNotificationService.kt` (164줄) |

---

## 5. 참고 사항

- **strings.xml**: 알림 관련 문자열은 정의되어 있지 않음 (알림 텍스트는 전부 코드에 하드코딩).
- **주간 리포트**: 실제 body는 `BriefSummaryPreloader.ensureLastWeekAndGetTitle()`로 AI 요약이 생성되며, fallback은 "지난주 스마트폰 사용 리포트를 확인하세요".
- **오버레이 UI 텍스트** (차단 화면 등)는 푸시 알림이 아닌 화면 표시용이라 위 표에 미포함.
