---
name: diagnose-and-recover
description: >-
  Diagnoses and recovers from Gradle build, Kotlin compile, Lint, or test
  failures with minimal changes. Use when a build or test fails, or the same
  error repeats after a fix.
---

# Diagnose and Recover

빌드, Lint, 테스트 또는 실행 오류를 최소 변경으로 진단하고 복구하는 절차다.

## Use when

- Gradle Build가 실패할 때
- Kotlin/Java Compile 오류가 발생할 때
- Lint 또는 Test가 실패할 때
- 수정 후 같은 오류가 반복될 때

## Procedure

### 1. 실패 고정

- 실패한 정확한 명령과 종료 코드를 확인한다.
- flavor가 있으면 명령에 포함한다 (`devDebug` 등).
- 재현 가능한 경우 같은 명령을 한 번 실행해 현재 오류를 고정한다.
- 긴 로그 전체보다 최초의 근본 오류와 관련 stack trace를 우선 확인한다.

### 2. 과거 기록 검색

- 오류 메시지의 핵심 부분으로 `agent/ERROR_LEDGER.md`를 검색한다.
- 기존 해결법이 현재 코드와 버전에 적용 가능한지 확인한다.

### 3. 원인 가설

다음 범주 중 하나로 분류한다.

- Syntax 또는 Type
- Dependency 또는 Version
- Resource 또는 Manifest
- Compose State 또는 Lifecycle
- Service / Permission / Accessibility
- Test Environment
- Configuration
- External Tool

확인하지 않은 가설을 사실처럼 기록하지 않는다.

### 4. 최소 수정

- 근본 원인과 직접 관련된 파일만 수정한다.
- 오류를 숨기기 위한 무조건적인 예외 처리, 테스트 비활성화, Lint 억제를 사용하지 않는다.
- 버전 업그레이드나 아키텍처 변경은 작은 수정으로 해결되지 않을 때만 검토한다.
- 임시 우회, 테스트 삭제·주석 처리, DB 초기화/destructive migration, SSL·보안 검사 비활성화, 권한 무단 확대로 해결하지 않는다.
- 금지 우회는 `.cursor/rules/30-production-engineering.mdc`를 따른다.

### 5. 동일 명령 재실행

- 실패했던 명령을 먼저 다시 실행한다.
- 통과한 뒤 관련 상위 검증을 실행한다.

### 6. 반복 제어

- 첫 실패: 원인 확인 후 최소 수정
- 두 번째 실패: 가설과 확인 결과를 기록하고 다른 접근 사용
- 세 번째 실패: `STOP` 또는 `BLOCKED`로 전환

### 7. 기록

반복 가능성이 있는 오류만 `agent/ERROR_LEDGER.md` 형식으로 기록한다.

```text
Fingerprint:
Status: OPEN | INVESTIGATING | BLOCKED | WORKAROUND | RESOLVED
Command:
Root cause:
Fix:
Verification:
Occurrences:
```

## Output format

```text
Status: PASS | RETRY | BLOCKED | STOP
Failed command:
Root cause:
Changed files:
Verification:
Remaining risk:
```
