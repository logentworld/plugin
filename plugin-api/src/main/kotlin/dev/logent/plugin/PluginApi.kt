package dev.logent.plugin

/** 플러그인과 호스트가 함께 쓰는 계약 버전. 깨지는 변경은 반드시 올려요. */
const val PLUGIN_API_VERSION = 3

/**
 * Logent 기능 플러그인.
 *
 * 플러그인은 서명된 `.lpg` 패키지의 DEX로 APK 밖에서 로드되는 신뢰 코드예요.
 * 선언한 도구·권한은 호스트가 다시 검증하고, 패키지 서명·해시·API 범위를
 * 통과한 코드만 실행해요. 플러그인마다 자산·설정·데이터 디렉터리를 분리해요.
 */
interface LogentPlugin {
    val id: String
    val name: String
    val version: String
    val apiVersion: Int get() = PLUGIN_API_VERSION

    /** 호스트가 등록할 도구 목록. 이름·권한은 호스트가 다시 검사해요. */
    fun tools(): List<PluginTool> = emptyList()

    /** 플러그인이 기여하는 읽기 전용 스킬. 본문은 자산 또는 플러그인 reader가 공급해요. */
    fun skills(): List<PluginSkill> = emptyList()

    /** 시스템 프롬프트에 덧붙일 짧은 사용 지침. */
    fun promptSection(): String? = null

    /** 호스트가 제공하는 공용 서비스. 코어는 타입으로 찾아요. */
    fun services(): List<PluginService> = emptyList()

    /** 플러그인이 기여하는 슬래시 커맨드. 입력 원문은 모델에 보내지 않아요. */
    fun slashCommands(): List<PluginSlashCommand> = emptyList()

    /** 코어가 렌더링하고 앱 전용 저장소에 보관할 설정 스키마예요. */
    fun settings(): List<PluginSetting> = emptyList()

    /** 시스템·사용자·도구·세션 수명주기에 개입하는 훅이에요. */
    fun hooks(): PluginHooks? = null

    /** 플러그인이 기여하는 MCP Streamable HTTP 서버예요. */
    fun mcpServers(): List<PluginMcpServer> = emptyList()

    /**
     * 첫 도구 실행 전에 런타임·플러그인 자산을 준비해요. 성공한 뒤에도 매번 호출될 수
     * 있으므로 멱등하게 작성하고, 실패는 해당 플러그인 기능만 막고 앱 전체를 막지 않아요.
     */
    suspend fun ensureReady(host: PluginHost): Result<Unit> = Result.success(Unit)

    /** 대화가 바뀌거나 중지될 때 플러그인 내부 상태를 즉시 정리해요. */
    fun onSessionReset() {}
}

/**
 * 플러그인이 코어에 요청할 수 있는 기능. 플러그인은 Android Context나 내부
 * 저장소에 직접 접근하지 않아요.
 */
interface PluginHost {
    val appVersion: String

    /** 기존 플러그인 호환용 APK 경로예요. 새 플러그인은 contentArchive를 써요. */
    val apkPath: String

    /** 플러그인 전용 영속 저장소예요. 다른 플러그인과 섞이지 않아요. */
    val dataDir: String get() = ""

    /** 플러그인 전용 캐시 저장소예요. 앱이 지워도 되는 파일만 둬요. */
    val cacheDir: String get() = ""

    /** Termux 셸 한 번 실행. 출력 상한과 시간 상한은 호스트가 강제해요. */
    suspend fun shell(command: String, timeoutSeconds: Int): Result<ProcessResult>

    /** X11·D-Bus·AT-SPI 데스크톱이 준비됐는지 보장해요. */
    suspend fun ensureDesktop(): Result<Unit>

    /** 활성 콘텐츠 팩 또는 플러그인 패키지의 자산을 읽어요. */
    suspend fun readAsset(path: String): Result<ByteArray>

    /** 같은 assets/plugins 경로를 담은 검증된 아카이브의 Termux 경로예요. */
    suspend fun contentArchive(assetPrefix: String): Result<String> =
        Result.failure(UnsupportedOperationException("플러그인 자산 아카이브를 지원하지 않아요."))

    /** 현재 플러그인이 선언한 설정만 읽어요. 비밀값은 로그·프롬프트에 넣지 않아요. */
    fun setting(key: String): String? = null

    /** 선언한 설정 키만 저장해요. 호스트가 범위·종류를 다시 검증해요. */
    fun writeSetting(key: String, value: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("플러그인 설정 쓰기를 지원하지 않아요."))

    /** 실행 중인 도구 카드에 짧은 진행 문구를 보여 줘요. */
    fun progress(message: String)

    /**
     * 공용 HTTPS 호출이에요. 호스트가 공용 주소·리다이렉트·시간·응답 크기를 강제해요.
     * 이 기능을 지원하지 않는 호스트는 실패를 돌려주므로 플러그인은 기능만 비활성화해요.
     */
    suspend fun http(request: PluginHttpRequest): Result<PluginHttpResponse> =
        Result.failure(UnsupportedOperationException("플러그인 HTTPS를 지원하지 않아요."))
}

/** web_fetch가 직접 HTTP·Jina로 실패했을 때 플러그인이 맡는 구조 요청. */
data class WebRescue(
    val text: String,
    val layer: String,
    val status: Int? = null,
    val verdict: String = "",
    val untriedRoutes: List<String> = emptyList(),
    val needsBrowser: Boolean = false,
)

/** 플러그인이 호스트에 제공하는 타입 있는 서비스의 표식이에요. */
interface PluginService

fun interface PageRescueService : PluginService {
    suspend fun fetchPage(url: String, maxChars: Int, timeoutSeconds: Int): Result<WebRescue>
}

/** 플러그인 슬래시 커맨드가 실행 중에 쓰는 좁은 문맥이에요. */
interface PluginSlashContext {
    val sessionId: String
    val workspace: String
    val host: PluginHost

    suspend fun reply(text: String, isError: Boolean = false)

    /** 모델에 보낼 프롬프트를 반환하지 않고 도구를 직접 실행해요. */
    suspend fun runTool(name: String, argumentsJson: String): ToolResult
}

/** 슬래시 커맨드 실행 결과예요. prompt가 있으면 코어가 모델 요청을 시작해요. */
data class PluginSlashResult(
    val prompt: String? = null,
    val handled: Boolean = true,
)

/**
 * 플러그인이 소유한 슬래시 커맨드예요.
 * 간단한 프롬프트 명령은 `buildPrompt`, 상태 변경·도구 실행은 `handler`를 써요.
 */
class PluginSlashCommand(
    val name: String,
    val usage: String,
    val description: String,
    val buildPrompt: ((String) -> String)? = null,
    private val handler: (suspend (String, PluginSlashContext) -> PluginSlashResult)? = null,
) {
    init {
        require(buildPrompt != null || handler != null) { "buildPrompt나 handler가 필요해요." }
    }

    suspend fun execute(args: String, context: PluginSlashContext): PluginSlashResult {
        handler?.let { return it(args, context) }
        return PluginSlashResult(prompt = buildPrompt?.invoke(args))
    }
}
