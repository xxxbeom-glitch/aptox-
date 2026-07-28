# agent 폴더 안내

이 폴더는 **앱 코드를 고칠 때 AI와 같이 보는 운영 메모**다.  
실제 앱 기능 코드는 `app/`에 있고, 여기는 약속·범위·인수인계만 둔다.

헷갈리면 **이 파일만 먼저** 보면 된다.

---

## 파일별로 뭐 하는 곳?

| 파일 | 쉬운 말로 |
|------|-----------|
| `PROJECT_SPEC.md` | 앱이 뭐 하고, 뭐는 함부로 건드리면 안 되는지 (**위험 구역 표** 포함) |
| `TASK_CONTRACT.md` | **지금 하는 일 한 장**. 새 일 시작하면 갈아끼움 |
| `SESSION_HANDOFF.md` | 다음에 이어서 할 일 짧은 메모 |
| `ERROR_LEDGER.md` | 또 같은 빌드/오류 실수 하지 말자 기록 |

디자인·플로우 세부 문서는 여기가 아니라 기존 `docs/`를 본다.

| 궁금한 것 | 열 곳 |
|-----------|--------|
| 디자인/컴포넌트 | `docs/DESIGNSYSTEM.md` |
| 앱 제한 플로우 | `docs/restriction-flow.md` |
| 통계 데이터 규칙 | `docs/statistics-data-rules.md` |
| Figma·헤더·버튼 수치 | `agent/design/rules.md` (요약: `.cursorrules`) |
| 디자인 문서 읽는 순서 | `agent/design/README.md` |
| Compose 구조·상태·접근성 | `.cursor/rules/10-android-compose-ui.mdc` |

---

## 한 가지만 기억

- **큰 약속·범위** → `PROJECT_SPEC`
- **이번 일만** → `TASK_CONTRACT`
- **이어서** → `SESSION_HANDOFF`
- **화면/디자인 세부** → `agent/design/rules.md` + `docs/`
