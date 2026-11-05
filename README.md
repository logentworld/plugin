# Logent Plugin API

Logent 코어와 기능 플러그인이 함께 쓰는 공개 JVM 계약이에요. Android 내부 클래스나
`Context`에 의존하지 않고, JDK 17용 JAR로 발행해요.

## API 2 계약

- `LogentPlugin`: 플러그인 식별자·버전, 도구·스킬·프롬프트·서비스·슬래시 커맨드를 선언해요.
- `PluginTool`: 모델에 노출할 스키마·권한·직렬화 정책과 실행기를 제공해요.
- `PluginHost`: 제한된 셸·데스크톱·APK 자산·플러그인 설정에 접근해요.
- `PluginService`: 코어가 타입으로 찾는 서비스의 표식이에요.
- `PageRescueService`: 코어 `web_fetch`가 직접 HTTP·Jina로 실패했을 때 구조해요.
- `PluginSlashCommand`: 모델에 원문을 보내지 않는 슬래시 커맨드를 선언해요.
- 설정과 콘텐츠 계약도 이 artifact에서 함께 버전 관리해요.
- 계약 상수는 `PLUGIN_API_VERSION = 2`, Maven 버전은 `2.0.0`이에요.

플러그인은 APK에 컴파일되는 신뢰 코드예요. DEX/AAR을 런타임에 동적으로 불러오지 않아요.
호스트는 권한 선언을 다시 검사하고, `ToolAccess.DELEGATE`를 플러그인 도구에 허용하지 않아요.

## 버전된 artifact 사용

좌표는 **`dev.logent:logent-plugin-api:2.0.0`**이에요. 기본 소비 경로는
GitHub Packages이며, 형제 디렉터리의 소스가 없어도 artifact를 사용할 수 있어요.
GitHub Packages는 공개 패키지 다운로드에도 인증이 필요할 수 있어요.
로컬에서는 `read:packages` 권한 토큰을 환경 변수로만 전달하고 저장소에 기록하지 않아요.
다른 저장소의 Actions에서 소비할 때는 해당 저장소의 패키지 읽기 권한을 설정하거나,
`read:packages` 토큰을 Actions secret으로 전달해요.

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sleepysoong/logent-plugin-api")
        content { includeModule("dev.logent", "logent-plugin-api") }
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}
dependencies {
    implementation("dev.logent:logent-plugin-api:2.0.0")
}
```

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
릴리스는 같은 버전의 `v2.0.0` 형식 태그를 push할 때 실행해요. 태그와 버전이 다르면
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
