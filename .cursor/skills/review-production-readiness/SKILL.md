---
name: review-production-readiness
description: >-
  Reviews Aptox Android changes for production readiness covering security,
  privacy, local data, network, permissions (UsageStats/Accessibility),
  lifecycle, dependencies, tests, and release safety. Use before finishing work
  that touches storage, network, auth, permissions, personal data, migrations,
  blocking logic, or release configuration.
---

# Review Production Readiness

저장, 네트워크, 권한, 개인정보, 인증, 마이그레이션, 릴리즈, UsageStats,
Accessibility, 차단 로직이 포함된 변경의 실서비스 준비 상태를 검토한다.
단순 Figma UI 수치·문구 맞춤에는 강제하지 않는다.

상세 규칙은 `.cursor/rules/30-production-engineering.mdc`를 따른다.
오류 재현·재검증은 `.cursor/skills/diagnose-and-recover/SKILL.md`와
`20-quality-and-recovery.mdc`를 따른다.

## Use when

- Room / DataStore / 파일 저장 / 백업 / 마이그레이션을 추가·수정할 때
- 네트워크, Deep Link, Intent 입력을 다룰 때
- 권한, Manifest exported, Accessibility, Foreground Service, 백업 설정을 변경할 때
- 사용 시간·제한 설정·계정·알림 이력을 다루거나 로그를 추가할 때
- 의존성·ProGuard·서명·versionCode·flavor 플래그 등 릴리즈 설정을 변경할 때
- 출시 직전 또는 "완료" 전에 안전성 확인이 필요할 때

## Procedure

### 1. 변경 범위 확인

- 변경 파일과 영향 기능을 나열한다.
- 단순 UI만이면 이 Skill을 축소하거나 건너뛰고 이유를 남긴다.

### 2. 관련 지침과 PROJECT_SPEC 확인

- `agent/PROJECT_SPEC.md`의 범위·주의 영역을 확인한다.
- `30-production-engineering.mdc`의 금지 항목을 기준으로 검토한다.
- 제한 플로우면 `docs/restriction-flow.md`도 확인한다.

### 3. 민감 데이터와 권한 사용 확인

- 수집·저장·로그되는 데이터 종류를 확인한다.
- 불필요한 권한·로그·분석 전송이 없는지 확인한다.

### 4. 네트워크와 외부 입력 검토

- 네트워크가 범위에 없으면 `해당없음`으로 기록한다.
- HTTPS, 타임아웃, 재시도 제한, 응답 검증, Intent/Deep Link/packageName 검증을 확인한다.

### 5. 로컬 저장과 마이그레이션 검토

- 저장 실패 시 기존 데이터 보존, transaction, 백업 호환을 확인한다.
- destructive migration이나 DB 삭제로 문제를 숨기지 않았는지 확인한다.

### 6. Coroutine과 생명주기 검토

- `GlobalScope` 금지, 취소 가능성, 중복 요청, Service/알람 중복을 확인한다.

### 7. 오류 처리와 데이터 정합성 검토

- 빈 catch, 실패를 성공으로 위장, 민감정보 로그, 사용자 노출 문구를 확인한다.

### 8. 의존성 변경 검토

- 신규 라이브러리 필요성, 중복, 버전 임의 변경 여부를 확인한다.

### 9. 테스트 대상과 테스트 결과 확인

- Debug 메뉴만으로 완료 처리하지 않는다.
- 실패한 테스트 삭제·주석 처리가 없는지 확인한다.

### 10. Debug/Release 차이 확인

- `SHOW_DEBUG_MENU` 등 flavor 플래그, 상세 로그, 테스트 광고 ID가 출시 정책과 맞는지 확인한다.

### 11. 검증 명령 실행

가능한 범위에서 해당 flavor 빌드·테스트를 실행한다.
프로젝트에 `scripts/verify-ui.ps1`이 없으면 그 항목은 `해당없음`이다.
실행하지 못한 항목은 `NOT VERIFIED`와 사유를 남긴다.

### 12. 결과 기록

- `PASS` / `BLOCKED` / `STOP` 중 하나로 판정한다.
- `STOP`이면 완료라고 보고하지 않는다.
- 필요 시 `agent/SESSION_HANDOFF.md`에 남은 위험만 짧게 남긴다.

## Verdict criteria

- `PASS`: 필수 검증을 모두 통과했다.
- `BLOCKED`: 구현은 가능하지만 외부 정보나 사용자 결정이 필요하다.
- `STOP`: 보안, 데이터 손실, 릴리즈 위험이 해결되지 않았다.

## Output format

```text
Production Readiness Review

- Scope:
- Security:
- Privacy:
- Local Data:
- Network:
- Permissions:
- Error Handling:
- Lifecycle:
- Dependencies:
- Tests:
- Release:
- Remaining Risks:
- Result: PASS / BLOCKED / STOP
```
