# Logent Plugin API

Logent 코어와 기능 플러그인이 함께 쓰는 공개 JVM 계약이에요. Android 내부 클래스나
`Context`에 의존하지 않고, JDK 17용 JAR로 발행해요.

## API 4.4 계약

- `LogentPlugin`: 플러그인 식별자·버전, 도구·스킬·프롬프트·서비스·슬래시 커맨드를 선언해요.
- `PluginTool`: 모델에 노출할 스키마·권한·직렬화 정책과 실행기를 제공해요.
- `PluginHost`: 제한된 셸·데스크톱·APK 자산·플러그인 설정·HTTPS와 SSE 스트리밍에 접근해요.
- `PluginService`: 코어가 타입으로 찾는 서비스의 표식이에요.
- `PageRescueService`: 코어 `web_fetch`가 직접 HTTP·Jina로 실패했을 때 구조해요.
- `PluginAiProvider`: `services()` 또는 설정을 읽는 `services(host)`로 AI 백엔드를 기여해요.
  키는 선언된 SECRET 설정에서 읽거나 선택 사항으로 두고, `PluginAiSession`이 완료·
  스트리밍·취소를 맡아요. 코어는 실행 시작 시 세션을 고정해 재시도 중 설정 변경으로
  다른 백엔드에 키를 보내지 않아요. `open(host)`에 전달하는
  호스트의 `aiConversationId`는 코어가 공급하는 불투명한 영속 대화 ID예요. 재시도·도구
  라운드·새 턴에서 유지하고, 새 대화·분기 대화·서브에이전트 대화에는 각각 별도 ID를 써요.
  AI 대화에 바인딩되지 않은 일반 도구 호스트와 기존 호스트의 기본값은 `null`이에요.
- `ApiMessage`·`ApiToolCall`·`ChatResult`·`ToolSpec`: 코어와 프로바이더가 메시지를 복사하지 않고
  같은 대화 계약을 공유해요.
- `PluginSlashCommand`: 모델에 원문을 보내지 않는 슬래시 커맨드를 선언해요.
- 설정과 콘텐츠 계약도 이 artifact에서 함께 버전 관리해요.
- `PluginSettingGroup`·`PluginSettingEntry`: 같은 필드 양식으로 여러 항목을 추가·수정·
  삭제해요. `settingGroups()`로 양식을 선언하고 `host.settingEntries()`로 읽어요.
  `writeSettingEntry()`는 전달한 필드만 원자 갱신하며 생략한 필드는 유지해요. 비밀값은
  내보내기·가져오기에서 제외해요. `writeSettingEntryIfUnchanged()`는 기대한 필드 값이
  그대로일 때만 갱신하고, 바뀌거나 삭제됐으면 `false`를 반환해요.
- 계약 상수는 `PLUGIN_API_VERSION = 4`, Maven 버전은 `4.4.0`이에요. 4.4는 반복 설정
  그룹과 설정 기반 서비스 기여를 기본 메서드로 추가해 기존 API 4 플러그인을 다시
  컴파일할 필요는 없어요. 새 기능을 사용하는 플러그인은 4.4 호스트가 필요해요.

플러그인은 서명된 `.lpg`로 배포되는 신뢰 코드예요. 호스트는 권한 선언을 다시 검사하고,
`ToolAccess.DELEGATE`를 플러그인 도구에 허용하지 않아요.

## 버전된 artifact 사용

좌표는 **`com.sleepysoong.logent:logent-plugin-api:4.4.0`**이에요. 기본 소비 경로는
GitHub Packages이며, 형제 디렉터리의 소스가 없어도 artifact를 사용할 수 있어요.
GitHub Packages는 공개 패키지 다운로드에도 인증이 필요할 수 있어요.
로컬에서는 `read:packages` 권한 토큰을 환경 변수로만 전달하고 저장소에 기록하지 않아요.
다른 저장소의 Actions에서 소비할 때는 해당 저장소의 패키지 읽기 권한을 설정하거나,
`read:packages` 토큰을 Actions secret으로 전달해요.

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/logentworld/plugin")
        content { includeModule("com.sleepysoong.logent", "logent-plugin-api") }
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}
dependencies {
    implementation("com.sleepysoong.logent:logent-plugin-api:4.4.0")
}
```

공개 API 패키지는 `com.sleepysoong.logent.plugin`이에요. API 4는 이전 패키지와
바이너리 호환되지 않으므로 소비자도 함께 이관해야 해요.

코어와 공식 플러그인은 `plugin-api.lock`의 `group`, `artifact`, `version`, `repository`,
`commit`을 고정해요. 동적 버전·SNAPSHOT·암묵적인 소스 fallback은 사용하지 않아요.
명시적 개발 옵션 `-PlogentPluginApiSource=true`일 때만 형제 저장소를 composite build로
대체하고, `-PlogentPluginApiMavenLocal=true`일 때만 Maven Local을 검색해요.
두 옵션은 배포용 설정이 아니에요.

## 공개 ABI baseline

공식 [Kotlin Gradle 플러그인의 ABI 검증](https://kotlinlang.org/docs/gradle-binary-compatibility-validation.html)을
사용해요. 직접 만든 소스 문자열 검사기가 아니라 Kotlin/JVM 바이너리에서 공개 시그니처를
덤프하고 `plugin-api/api/`의 baseline과 비교해요. baseline 누락이나 변경은 검사를 실패시켜요.
CI와 Maven 발행은 baseline을 자동 갱신하지 않아요.

```bash
# 최초 baseline 생성 또는 의도한 공개 API 변경 뒤에만 실행해요.
./gradlew :plugin-api:updateKotlinAbi
# 생성된 plugin-api/api/ 덤프 차이를 검토하고 소스와 함께 커밋해요.

# 일반 개발·CI에서는 기존 baseline과 비교해요.
./gradlew :plugin-api:checkKotlinAbi :plugin-api:test
```

호환성을 깨는 변경은 계약 상수와 Maven 주 버전을 함께 올리고 소비자도 함께 이관해요.
공개 시그니처가 같아도 동작 계약이 바뀔 수 있으므로, ABI 검사만으로 의미적 호환성이
증명되지는 않아요.

## 로컬 Maven 발행과 pin 확인

```bash
# 이 저장소에서 실행해요. checkKotlinAbi도 자동으로 실행해요.
./gradlew :plugin-api:publishToMavenLocal

# 코어 저장소에서 고정 파일을 검사해요. API 소스 체크아웃은 필수가 아니에요.
cd ../logent
./scripts/check-plugin-api.sh

# 깨끗한 API 체크아웃의 HEAD·버전까지 확인하려면 실행해요.
./scripts/check-plugin-api.sh --source ../logent-plugin-api

# 의도한 API 소스와 baseline을 커밋한 뒤 새 버전·커밋을 고정해요.
./scripts/lock-plugin-api.sh ../logent-plugin-api
```

커밋되지 않은 API 소스는 정식 pin으로 기록할 수 없어요. 소스를 수정하는 중에는 명시적
소스 개발 옵션을 쓰고, 배포 전에는 깨끗한 체크아웃으로 pin을 갱신해요.
JAR manifest의 `Implementation-Version`, `Logent-Plugin-Api-Commit`과 Maven POM의
`logent.pluginApi.commit`에도 발행 버전·소스 커밋을 남겨요.

## CI와 GitHub Packages 릴리스

`gradle.properties`의 `pluginApiVersion`이 발행 버전의 단일 원천이에요.
PR과 `main` CI는 공개 ABI, 단위 테스트, Maven Local 발행을 확인해요.
릴리스는 같은 버전의 `v4.4.0` 형식 태그를 push할 때 실행해요. 태그와 버전이 다르면
실패하고, ABI·테스트를 통과한 뒤 GitHub Packages에 JAR·소스 JAR·POM·Gradle metadata를
발행하고 GitHub Release에 JAR를 첨부해요.

수동 발행이 필요하면 환경에 `GITHUB_ACTOR`, `GITHUB_TOKEN`을 설정해요.
토큰에는 `write:packages` 권한이 필요해요. 값은 명령행 인자·Gradle 설정·로그에 남기지 않아요.

```bash
./gradlew :plugin-api:checkKotlinAbi :plugin-api:test
./gradlew :plugin-api:publishPluginApiPublicationToGitHubPackagesRepository
```

Actions는 저장소의 `GITHUB_TOKEN`과 최소 권한 선언을 사용해요. 같은 정식 버전을
덮어쓰지 않고, 새 API를 발행할 때는 버전과 소비자 pin을 함께 올려요.

## 라이선스

GPL-3.0이에요. 자세한 내용은 `LICENSE`를 봐 주세요.
