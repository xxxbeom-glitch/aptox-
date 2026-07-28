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

- `PACKAGE_USAGE_STATS`, Accessibility, Overlay/차단 UI
- Foreground Service, Exact Alarm, Boot Completed
- `QUERY_ALL_PACKAGES`, 알림, 배터리 최적화 예외
- 로컬 DB/백업, Firebase·로그인·탈퇴, 구독, AdMob
- Manifest `exported` 컴포넌트, `allowBackup` / backup rules

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
