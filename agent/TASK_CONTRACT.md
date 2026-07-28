# Active Task Contract

이 문서는 현재 작업 한 건만 유지한다. 새 작업을 시작할 때 이전 내용을 교체한다.

## Task

- Task ID: HARNESS-002
- Screen ID: N/A
- Area: `.cursor/rules/10-android-compose-ui.mdc`

## Goal

Aptox용 Compose UI 규칙 축소판을 추가한다. Figma 수치 규칙은 기존 `.cursorrules`를 유지한다.

## Required

- `10-android-compose-ui.mdc` 추가 (구조·상태·접근성·리소스)
- Liftly식 절대좌표 금지 / Debug Catalog 강제 / 토큰만 사용은 제외
- `PROJECT_SPEC`·`agent/README`에 규칙 위치 반영

## Allowed Scope

- 위 규칙·agent 문서만

## Forbidden

- `app/` 소스 변경, 패키지 이동
- `.cursorrules` 삭제·수치 규칙 약화

## Verification

- [x] 규칙 파일 존재·역할 분리 확인
- [ ] Compile/Build (해당 없음 — 코드 미변경)

## Done When

- Compose 작업 시 적용될 aptox용 `10-...` 규칙이 있고, Figma fidelity와 충돌하지 않음

## Result

- Status: PASS
- Notes: 앱 코드 미변경
