# 🐛 이슈 요약

통계 화면 리뉴얼(AN-01) 후 제대로 동작하지 않음

## 문제 설명

Figma 919-3517 기반으로 통계 화면을 새로 구현한 이후, 앱이 제대로 동작하지 않는 상태.
빌드/설치 단계에서 실패했으며, 런타임/화면 동작 문제 가능성도 있음.

## 발생 상황

- 언제: 통계 화면 AN-01 리뉴얼 구현 직후
- 어디서: 통계 화면(StatisticsScreen.kt), build task `installDebug`
- 증상: 
  - `installDebug` 실패 → No connected devices
  - 사용자 보고: "제대로 안되고있어"

## 에러 메시지

```
> Task :app:installDebug FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:installDebug'.
> com.android.builder.testing.api.DeviceException: No connected devices!

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
```

## 시도한 방법

- [x] `assembleDebug` — 컴파일은 성공
- [ ] 에뮬레이터 실행 후 installDebug
- [ ] 실제 기기 USB 연결 후 installDebug
- [ ] 앱 실행 후 통계 화면 동작 확인

## 관련 파일

- `app/src/main/java/com/cole/app/StatisticsScreen.kt` - 통계 화면 AN-01 전체 구현
- `app/src/main/java/com/cole/app/StatisticsData.kt` - getWeekRange, loadDayOfWeekMinutes 등 확장
- `app/src/main/java/com/cole/app/IconComponents.kt` - IcoNavLeft, IcoNavRight (날짜 네비게이션)
- `app/src/main/java/com/cole/app/ListComponents.kt` - LabelDanger, AppStatusDataViewRow
- `app/src/main/java/com/cole/app/InfoBoxComponents.kt` - ColeInfoBoxCompact
- `app/src/main/java/com/cole/app/AppColors.kt` - 통계 차트/카테고리 색상
- `app/src/main/java/com/cole/app/AppTypography.kt` - 텍스트 스타일
- `app/src/main/java/com/cole/app/NavigationComponents.kt` - ColeSegmentedTab
- `app/src/main/java/com/cole/app/AppRestrictionRepository.kt` - 제한 앱 데이터
- `docs/DESIGNSYSTEM.md` - 통계 컴포넌트 가이드

## 예상 원인

1. **installDebug 실패**: 연결된 디바이스/에뮬레이터 없음 — 환경 이슈
2. **통계 화면 런타임 문제**: 디바이스 연결 후 앱 실행 시 크래시 또는 UI 오류 가능성
3. **데이터 로딩**: UsageStats 권한 미부여 시 빈 데이터 표시
4. **날짜 네비게이션**: weekOffset 로직(이전/다음 주) 검증 필요

## 참고 사항

- Figma 기준: 919-3517 (AN-01 통계 화면)
- 주간 탭 기본 선택, 오늘/월간/연간은 기존 스타일 유지
- 인사이트 카드(연속 달성일, 달성율, 유지율)는 현재 목업 데이터
- 카테고리 태그(SNS, OTT 등)는 StatsAppItem.categoryTag — 매핑 미구현 시 null
- 스크린샷이 있다면 `screenshots/` 폴더에 넣어주시면 좋음

---
*생성 시각: 2025-03-04*
*프로젝트: cole (디지털 디톡스 앱)*
*환경: Android / Kotlin + Jetpack Compose*
