# Active Task Contract

이 문서는 현재 작업 한 건만 유지한다. 새 작업을 시작할 때 이전 내용을 교체한다.

## Task

- Task ID: UI-COMP-001
- Area: `ui/components` Form/InfoBox/CategoryTag/Button

## Goal

순수 UI 컴포넌트를 `ui/components`로 옮기고 CategoryTag 색을 AppColors 토큰화한다. 시스템(모니터/접근성/제한)은 건드리지 않는다.

## Result

- Status: PASS
- Verification: `:app:compileDevDebugKotlin` PASS
- Notes: 앱 동작 변경 없음 (패키지 이동 + import + 색 토큰)
