# Project Spec

쉽게 변하지 않는 기준만 기록한다. 화면 하나·버그 하나 작업은 `TASK_CONTRACT.md`에 쓴다.

## Identity

- App name: Aptox
- Application ID: `com.aptox.app`
- Primary platform: Android
- UI framework: Jetpack Compose 위주 (일부 Java/Views·위젯 XML 혼재)
- Project purpose: 디지털 디톡스 — 앱 사용 시간을 측정·제한하고, 차단/알림/통계로 사용 습관을 관리하는 앱

## Product Scope (현재 코드 기준 요약)

확정된 “1차만 만든다” 목록이 아직 없으면, **이미 있는 핵심 영역**을 기준으로 한다. 새 기능을 임의로 추가하지 않는다.

- 앱 제한: 일일 사용량 제한, 시간 지정 제한, 차단 오버레이/다이얼로그
- 사용 시간: UsageStats 기반 집계·통계·알림
- 시스템 연동: AccessibilityService, Foreground Service(모니터), 위젯, 부팅/알람 리시버
- 계정·백업·구독·광고 등 부가 기능은 코드에 있으면 유지·수정만 하고, 요청 없이 새 축을 늘리지 않는다

세부 플로우·데이터 모델은 `docs/restriction-flow.md`, `docs/statistics-data-rules.md`를 본다.

## Explicitly careful areas

아래는 회귀·스토어·개인정보 위험이 크다. 요청 범위를 넘는 리팩터·권한 확대·구조 변경을 하지 않는다.
해당 구역을 수정하면 완료 전 `.cursor/skills/review-production-readiness/SKILL.md`를 실행한다.

### 손대면 안 되는 것 (승인 없이)

- 패키지 폴더 대이동 / 모듈 쪼개기
- Manifest 권한 추가·확대, `exported` 완화
- Accessibility / UsageStats / 차단 로직을 “우회”로 단순화
- destructive DB migration, 사용자 데이터 삭제로 오류 해결
- Debug 메뉴·테스트 플래그를 출시 flavor에 켜기
- Secret·광고/결제 키 하드코딩 확대

### 위험 구역 체크리스트 (수정 시)

| 구역 | 대표 위치 | 손댈 때 확인할 것 |
|------|-----------|-------------------|
| 사용 모니터 FGS | `AppMonitorService` | FGS type, 알림, 배터리/권한 상태, 프로세스 재시작 |
| 접근성 | `AptoxAccessibilityService`, `accessibility_service_config.xml` | 설정 XML, BIND 권한, 차단 트리거 회귀 |
| 차단 UI | `BlockDialogActivity` | exported=false, task/affinity, 오버레이·다이얼로그 진입 경로 |
| 제한 데이터 | `AppRestrictionRepository`, `model/AppRestriction`, `RestrictionDeleteHelper` | 일일/시간지정 구분, 기존 설정 보존, `docs/restriction-flow.md` |
| 사용량·통계 | `usage/*`, 관련 NotificationHelper | UsageStats 권한, 자정 리셋, `docs/statistics-data-rules.md` |
| 알람·부팅 | `*AlarmScheduler`, `*AlarmReceiver`, `BootCompletedReceiver` | 중복 스케줄, exact alarm 폴백, boot 후 복구 |
| 일시정지 타이머 | `PauseTimerNotificationService`, `PauseRepository` | FGS, 알림 채널, 종료·복귀 |
| 위젯 | `widget/*`, Manifest widget receivers | exported·action, 데이터 로더, 갱신 스케줄 |
| 백업 | `backup/*`, `DataBackupBottomSheet` | 형식 검증, 실패 시 기존 데이터 보존, backup_rules |
| 구독·결제 | `SubscriptionManager`, `subscription/*` | 영수증/상태 오판 금지, 로그에 구매 정보 미출력 |
| 계정 | Login/SignUp/Withdraw 관련 Screen·Helper | 토큰·개인정보 로그 금지, 탈퇴 삭제 범위 |
| Manifest/백업 정책 | `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | 권한·exported·allowBackup 의도 유지 |
| Flavor/릴리즈 | `app/build.gradle.kts` (`SHOW_DEBUG_MENU` 등) | dev vs externalTest/release 플래그 혼입 금지 |

### 작업 전 짧은 질문

1. 이 변경이 제한이 풀리거나 잘못 걸리는 쪽으로 갈 수 있나?
2. 권한·exported·백업 범위가 넓어지나?
3. 기존 사용자 설정·통계·백업이 깨질 수 있나?
4. Debug 전용 동작이 출시 빌드에 새나가지 않나?

하나라도 “예/모름”이면 `TASK_CONTRACT`에 검증 항목을 적고, 필요 시 `BLOCKED`로 확인을 받는다.

## Technical Baseline

- minSdk: 26
- targetSdk: 36
- compileSdk: 36
- Compose BOM: `2024.09.00` (`gradle/libs.versions.toml`)
- Flavors: `dev` / `externalTest` 등 distribution dimension (`SHOW_DEBUG_MENU` 등)
- Architecture (현실):
  - 루트 패키지 `com.aptox.app`에 Screen·Repository·Service가 많이 혼재
  - 일부 하위 패키지: `usage/`, `backup/`, `widget/`, `subscription/`, `ui/theme/`
  - **패키지 대이동은 별도 작업·승인 없이 하지 않는다**
- 목표 방향(강제 이전 아님): feature/data/service 분리, Debug 전용은 장기적으로 `src/debug`

## Design & UI contracts

- Figma 수치·헤더·버튼 규칙은 `agent/design/rules.md`가 담당한다 (요약·수정 범위는 루트 `.cursorrules`)
- 디자인 문서 읽는 순서는 `agent/design/README.md`
- 디자인 시스템·컴포넌트 메모는 `docs/DESIGNSYSTEM.md`
- Liftly식 “절대좌표 금지 / 토큰만” 규칙을 이 프로젝트에 강제하지 않는다

## Agent operating docs

| 문서 | 용도 |
|------|------|
| `TASK_CONTRACT.md` | 현재 작업 1건 |
| `SESSION_HANDOFF.md` | 세션 인수인계 |
| `ERROR_LEDGER.md` | 반복 오류 |
| `.cursor/rules/00-project-core.mdc` | 항상 적용되는 최소 운영 |
| `.cursor/rules/10-android-compose-ui.mdc` | Compose 구조·상태·접근성 (Figma 수치는 `agent/design/rules.md` 우선) |
| `.cursor/rules/20-quality-and-recovery.mdc` | 빌드·검증·복구 |
| `.cursor/rules/30-production-engineering.mdc` | 권한·데이터·릴리즈 안전 |

## Out of scope (아직 하지 않음)

- 코드 패키지 재배치
- Debug Catalog를 `src/debug`로 일괄 이전
- 기존 `.cursorrules` / `docs/` / issue-bridge 삭제
- Liftly식 “절대좌표 금지 / 토큰만” 강제
