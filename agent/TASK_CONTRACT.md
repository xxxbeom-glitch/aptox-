# Active Task Contract

이 문서는 현재 작업 한 건만 유지한다. 새 작업을 시작할 때 이전 내용을 교체한다.

## Task

- Task ID: HARNESS-003
- Screen ID: N/A
- Area: agent/design + .cursorrules

## Goal

`.cursorrules`의 헤더·버튼·Figma 세부 규칙을 `agent/design/rules.md`로 옮기고, `.cursorrules`는 운영 요약만 남긴다.

## Required

- `agent/design/README.md`, `agent/design/rules.md` 추가
- `.cursorrules` 슬림화 + 링크
- core / Compose 규칙 / agent README·PROJECT_SPEC 경로 갱신

## Allowed Scope

- 위 문서·규칙만

## Forbidden

- `app/` 소스 변경
- 디자인 규칙 내용 약화·삭제

## Verification

- [x] 헤더·버튼 규칙이 `agent/design/rules.md`에 존재
- [x] `.cursorrules`가 해당 문서를 가리킴
- [ ] Build (해당 없음)

## Done When

- UI 세부 규칙 위치가 `agent/design/`으로 정리되고 기존 수치 약속이 유지됨

## Result

- Status: PASS
- Notes: 앱 코드 미변경
