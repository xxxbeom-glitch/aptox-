# dist — 빌드 결과물 (찾기 쉬운 복사본)

Gradle이 만드는 **원본 경로**는 항상 `app/build/outputs/` 아래(고정)입니다.  
여기 `dist/`에는 단축 태스크가 **같은 파일을 알기 쉬운 이름으로 복사**해 둡니다.

## 명령 → 파일

| 명령 | 결과 파일 | 용도 |
|------|-----------|------|
| `.\gradlew.bat aptoxDebug` | `aptox-dev-debug.apk` | 내부 설치 (디버그 메뉴 ON) |
| `.\gradlew.bat aptoxTest` | `aptox-test-1.0.apk` | 내부/외부 테스트 설치 (디버그 메뉴 OFF) |
| `.\gradlew.bat aptox` | `aptox-dev-release.aab` | Play AAB (devRelease) |
| `.\gradlew.bat aptoxPlay` | `aptox-play-release.aab` | Play AAB 권장 (externalTestRelease) |

## 원본 경로 (참고)

- APK: `app/build/outputs/apk/<flavor>/<buildType>/`
- AAB: `app/build/outputs/bundle/<variant>/`

Release/AAB는 `gradle.properties`의 키스토어(`APTOX_KEYSTORE_*`)가 필요합니다.

`*.apk` / `*.aab` 는 git에 올리지 않습니다.
