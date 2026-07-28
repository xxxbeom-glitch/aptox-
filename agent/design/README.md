# Design Source

화면·레이아웃을 구현할 때 참고하는 디자인 규칙 모음이다.
공통 컴포넌트·색 메모는 기존 `docs/DESIGNSYSTEM.md`에 두고, 여기에는 **반복되는 레이아웃·배치 약속**만 둔다.

## 구성

| 경로 | 내용 |
|------|------|
| `rules.md` | Figma 수치 준수, 앱 제한 플로우 헤더, 하단 버튼 배치 |
| `docs/DESIGNSYSTEM.md` | 컴포넌트·색·타이포 메모 (기존) |
| `docs/restriction-flow.md` | 앱 제한 플로우 구현 현황 |

## 읽는 순서 (UI 구현 시)

1. `agent/TASK_CONTRACT.md`
2. 루트 `.cursorrules` (수정 범위·커밋·Figma 수치 요약)
3. `agent/design/rules.md`
4. 관련 `docs/` (DESIGNSYSTEM, restriction-flow 등)
5. `.cursor/rules/10-android-compose-ui.mdc` (구조·상태·접근성)
6. Figma / 스크린샷

## 충돌 시 우선순위

1. 사용자 최신 요청 / `TASK_CONTRACT`
2. 루트 `.cursorrules`의 수정 범위·확인 규칙
3. `agent/design/rules.md` (헤더·버튼·Figma 수치)
4. `docs/` 확정 문서
5. `10-android-compose-ui.mdc` (구조; 수치 충돌 시 3이 우선)
6. 기존 코드 패턴
