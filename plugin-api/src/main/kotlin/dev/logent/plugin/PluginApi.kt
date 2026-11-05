package dev.logent.plugin

/** 플러그인과 호스트가 함께 쓰는 계약 버전. 깨지는 변경은 반드시 올려요. */
const val PLUGIN_API_VERSION = 2

/**
 * Logent 기능 플러그인.
 *
 * 플러그인은 APK에 컴파일되는 신뢰 코드예요. 선언한 도구·권한은 호스트가 다시
 * 검증하며, 임의의 DEX를 실행하지 않아요. 변동 실행 자산은 플러그인이 소유한
 * APK 자산을 Termux에 배포해 써요.
 */
interface LogentPlugin {
    val id: String
    val name: String
    val version: String
    val apiVersion: Int get() = PLUGIN_API_VERSION

    /** 호스트가 등록할 도구 목록. 이름·권한은 호스트가 다시 검사해요. */
    fun tools(): List<PluginTool>

    /** 플러그인이 기여하는 읽기 전용 번들 스킬. */
    fun skills(): List<PluginSkill> = emptyList()

    /** 시스템 프롬프트에 덧붙일 짧은 사용 지침. */
    fun promptSection(): String? = null

    /** 호스트가 제공하는 공용 서비스. 코어는 타입으로 찾아요. */
    fun services(): List<PluginService> = emptyList()

    /** 플러그인이 기여하는 슬래시 커맨드. 입력 원문은 모델에 보내지 않아요. */
    fun slashCommands(): List<PluginSlashCommand> = emptyList()

    /**
     * 첫 도구 실행 전에 런타임·APK 자산을 준비해요. 성공한 뒤에도 매번 호출될 수
     * 있으므로 멱등하게 작성하고, 실패는 도구만 막고 앱 전체를 막지 않아요.
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

    /** Termux가 읽을 수 있는 현재 APK 경로. 큰 자산은 unzip으로 꺼내요. */
    val apkPath: String

    /** Termux 셸 한 번 실행. 출력 상한과 시간 상한은 호스트가 강제해요. */
    suspend fun shell(command: String, timeoutSeconds: Int): Result<ProcessResult>

    /** X11·D-Bus·AT-SPI 데스크톱이 준비됐는지 보장해요. */
    suspend fun ensureDesktop(): Result<Unit>

    /** APK 안의 플러그인 자산을 읽어요. 경로는 호출 플러그인의 assets 하위만 허용해요. */
    suspend fun readAsset(path: String): Result<ByteArray>

    /** 실행 중인 도구 카드에 짧은 진행 문구를 보여 줘요. */
    fun progress(message: String)
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

/** 플러그인이 소유한 슬래시 커맨드 정의예요. */
data class PluginSlashCommand(
    val name: String,
    val usage: String,
    val description: String,
    val buildPrompt: (String) -> String,
)
