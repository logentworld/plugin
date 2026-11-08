package com.sleepysoong.logent.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 플러그인 AI 프로바이더가 호스트와 주고받는 대화 메시지예요.
 *
 * 코어의 실행 이력과 같은 구조를 공유해 매 라운드 메시지를 복사하지 않아요.
 * 이미지 content는 문자열 또는 이미지 파트 배열이에요.
 */
@Serializable
data class ApiMessage(
    val role: String,
    val content: JsonElement? = null,
    val name: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ApiToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

/** 완료 응답의 메시지와 제공자가 보고한 토큰 사용량이에요. 누락된 값은 알 수 없어요. */
data class ChatResult(
    val message: ApiMessage,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val responseModel: String? = null,
)

@Serializable
data class ApiToolCall(
    val id: String,
    val type: String,
    val function: ApiFunctionCall,
)

@Serializable
data class ApiFunctionCall(
    val name: String,
    val arguments: String,
)

/** 프로바이더가 제공하는 모델과 알려진 문맥 한도예요. */
data class PluginAiModel(
    val id: String,
    val name: String = id,
    val contextWindow: Int? = null,
)

/** 한 번의 모델 요청이에요. 도구 스키마는 기존 ToolSpec 계약을 그대로 써요. */
data class PluginAiRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val tools: List<ToolSpec> = emptyList(),
)

/** 스트리밍 중 UI에 전달할 사고·본문 델타예요. */
sealed interface PluginAiStreamEvent {
    data class Reasoning(val delta: String) : PluginAiStreamEvent
    data class Content(val delta: String) : PluginAiStreamEvent
}

/**
 * 실행 하나 동안 고정되는 프로바이더 세션이에요.
 *
 * 호스트는 실행을 시작할 때 세션을 한 번 만들고 재시도·도구 라운드에서 같은 세션을 써요.
 * 세션은 `open` 시점의 키·모델·패키지 identity를 유지하고, 다른 세션과 동시에 쓸 수 있어요.
 */
interface PluginAiSession {
    suspend fun complete(request: PluginAiRequest): ChatResult

    suspend fun completeStreaming(
        request: PluginAiRequest,
        onEvent: (PluginAiStreamEvent) -> Unit,
    ): ChatResult

    /** 진행 중인 HTTP 연결을 즉시 끊어요. */
    fun cancel() {}
}

/**
 * 플러그인이 `services()`로 기여하는 AI 백엔드예요.
 *
 * 키는 선언된 SECRET 설정에서 읽고, 호스트는 실행 시작 시 [open] 결과를 고정해요.
 * `id`는 플러그인 전체에서 유일해야 하고, 모델·endpoint·인증은 플러그인 코드가 소유해요.
 */
interface PluginAiProvider : PluginService {
    /** `[a-z0-9][a-z0-9._-]{1,63}` 형식이며 다른 플러그인과 겹치면 안 돼요. */
    val id: String
    val name: String
    val description: String
    val defaultModel: String

    /** 이 플러그인이 선언한 SECRET 설정 키예요. 키가 필요 없으면 null이에요. */
    val apiKeySetting: String? get() = null

    /** 제공자가 알려진 경우에만 문맥 한도를 선언해요. */
    val contextWindow: Int? get() = null

    /** 설정 화면에서 고를 수 있는 모델 목록이에요. 비어 있으면 기본 모델만 써요. */
    val models: List<PluginAiModel> get() = emptyList()

    /** 실행 시작 시 호출돼요. 네트워크 호출 없이 세션만 구성해요. */
    fun open(host: PluginHost): PluginAiSession
}

/**
 * 프로바이더 오류를 코어 재시도 정책에 전달해요.
 *
 * 429·5xx 같은 일시적 오류만 [status]를 채우고, 인증·문맥 오류는 메시지로 구분해요.
 */
class PluginAiException(
    message: String,
    val status: Int? = null,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
