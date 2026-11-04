# Logent Plugin API

Logent의 기능 플러그인이 구현하는 공개 계약이에요. 플러그인은 이 모듈만 의존하고
Android 내부 클래스·저장소·Context에 직접 접근하지 않아요.

## 계약

- `LogentPlugin`: 플러그인 id·이름·버전, 도구·스킬·프롬프트·서비스, 준비·세션 초기화.
- `PluginTool`: 모델에 노출할 도구 스키마, 권한, 직렬화, 실행기.
- `PluginHost`: Termux 셸, X11 데스크톱 보장, APK 자산 읽기만 제공.
- `PageRescueService`: 코어 `web_fetch`가 직접 HTTP·Jina로 실패했을 때 쓰는 선택 서비스.
- `PLUGIN_API_VERSION = 1`.

플러그인은 APK에 컴파일되는 신뢰 코드예요. DEX/AAR을 런타임에 동적으로 불러오지 않아요.
권한 선언은 호스트가 다시 검증하며, `ToolAccess.DELEGATE`는 플러그인 도구에 허용하지 않아요.

## 빌드

```bash
./gradlew :plugin-api:test
```

플러그인 저장소는 이 저장소를 형제 디렉터리(`../logent-plugin-api/plugin-api`)로 포함하거나
릴리스된 AAR을 사용해요. API를 깨는 변경은 `PLUGIN_API_VERSION`을 올리고 모든 플러그인
저장소를 함께 갱신해요.

## 라이선스

GPL-3.0. 자세한 내용은 `LICENSE`를 봐 주세요.
