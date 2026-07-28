# Active Task Contract

이 문서는 현재 작업 한 건만 유지한다. 새 작업을 시작할 때 이전 내용을 교체한다.

## Task

- Task ID: HARNESS-001
- Screen ID: N/A
- Area: agent / .cursor rules & skills

## Goal

Aptox에 Liftly 대비 안전한 최소 운영 하네스만 추가한다. 앱 코드·패키지 구조는 변경하지 않는다.

## Required

- `agent/` 골격 (README, PROJECT_SPEC, TASK_CONTRACT, SESSION_HANDOFF, ERROR_LEDGER)
- `.cursor/rules/00-project-core.mdc`, `20-quality-and-recovery.mdc`, `30-production-engineering.mdc`
- `.cursor/skills/diagnose-and-recover`, `review-production-readiness`
- 기존 `.cursorrules`, `docs/`, issue-bridge 유지

## Allowed Scope

- 위 문서·규칙·skill 파일 추가/수정만

## Forbidden

- `app/` 소스·Gradle·Manifest 변경
- 패키지 이동, 의존성 추가
- Compose UI 규칙(`10-...`) / Debug Catalog / Figma skill 도입
- 기존 `.cursorrules` 삭제

## Verification

- [x] 문서·규칙 파일 존재 확인
- [ ] Compile 또는 Build (해당 없음 — 코드 미변경)
- [ ] Lint (해당 없음)
- [ ] Preview 또는 Debug 확인 (해당 없음)

## Done When

- 최소 하네스 파일이 추가되고, 기존 aptox 규칙·docs와 충돌하지 않게 역할이 분리됨

## Result

- Status: PASS
- Notes: 앱 코드 미변경. 문서·규칙·skill만 추가. 빌드 검증은 해당 없음.
