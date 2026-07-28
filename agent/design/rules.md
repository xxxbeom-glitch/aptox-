# Design Rules

디자인 시안만으로 알 수 없거나, 화면에 반복되는 레이아웃·배치 규칙을 적는다.
컴포넌트·색·타이포 상세는 `docs/DESIGNSYSTEM.md`를 본다.
앱 제한 데이터·플로우 로직은 `docs/restriction-flow.md`를 본다.

## Figma 수치

- 피그마 수치를 그대로 적용한다.
- 텍스트 크기, 간격, 패딩을 임의로 바꾸지 않는다.
- 디자인과 시스템 inset·터치 영역·폰트 스케일이 충돌하면 사용성을 해치지 않는 선에서 맞추고, 달라진 점을 보고한다.

## 앱 제한 플로우 헤더

앱 제한 플로우(AA-01, AA-02A-01, AA-02A-05, AA-DAILY, Common 등) 전체 화면은 동일한 레이아웃을 사용한다.

- 루트: `windowInsetsPadding(statusBars)` → `padding(top = AddAppHeaderTopPadding)` → `windowInsetsPadding(navigationBars)`
- 헤더 위치: status bar 바로 아래 18dp (`AddAppHeaderTopPadding`, 메인 화면과 동일)
- 구조: `Column` + `AptoxHeaderSub` + content + button(해당 시)

## 버튼 가이드

- 버튼 배치의 **기준점은 하단**이다.
- 버튼이 1라인이어도 하단에 여백을 비우지 않는다.
- 버튼·링크 등 최하단 요소를 화면 하단에 밀착시킨다.

## Do not

- 이 문서와 `.cursorrules`를 무시하고 간격·타이포를 “예쁘게” 재해석하지 않는다.
- 앱 제한 플로우만 다른 헤더 패딩·구조를 쓰지 않는다.
- Liftly식 “절대좌표 금지 / 토큰만”을 이유로 Figma 수치를 버리지 않는다.
