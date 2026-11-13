package com.sleepysoong.logent.plugin

/**
 * 채팅 수명주기에서 플러그인이 구독할 수 있는 이벤트예요.
 *
 * 플러그인은 [PluginHooks.chatEvents]에 원하는 이벤트만 선언해요. 구독하지 않은
 * 이벤트의 훅은 호출되지 않으므로, 기존 플러그인은 아무것도 바꾸지 않아도 돼요.
 */
enum class PluginChatEvent {
    /** 사용자 메시지가 대화에 커밋되기 전이에요. */
    USER_SEND,

    /** AI 응답이 대화에 커밋되기 전이에요. */
    AI_RECEIVE,

    /** 메시지가 대화와 저장소에서 삭제되기 전이에요. */
    MESSAGE_DELETE,

    /** 삭제한 메시지를 되돌리기 직전이에요. */
    MESSAGE_RESTORE,
}

/** 사용자 메시지가 만들어진 입력 경로예요. */
enum class PluginMessageOrigin {
    /** 채팅 입력창에서 보낸 메시지예요. */
    COMPOSER,

    /** 음성 입력으로 만든 메시지예요. */
    VOICE,

    /** 슬래시 커맨드가 만든 프롬프트예요. */
    SLASH_PROMPT,

    /** 예약 실행이 만든 메시지예요. */
    SCHEDULE,

    /** 서브에이전트 실행이 만든 메시지예요. */
    SUBAGENT,
}

/**
 * 채팅 훅 한 번에 전달되는 문맥이에요.
 *
 * [session]의 세션·작업 디렉터리·모델·프로바이더·플래그·호스트처럼 실행 주체와
 * 저장 위치를 정하는 값은 읽기 전용이에요. 플러그인은 [messageId]·[origin]·
 * [queued]·[sourceId]·[parentSessionId]로 같은 이벤트를 구분해요.
 */
data class PluginChatContext(
    val session: PluginHookContext,
    val messageId: Long,
    val origin: PluginMessageOrigin,
    val queued: Boolean = false,
    val sourceId: String? = null,
    val parentSessionId: String? = null,
)

/**
 * 채팅 훅이 주고받는 메시지 본문이에요.
 *
 * [apiMessage]의 역할·본문·이름·도구 호출·도구 호출 ID, [images], UI 표시용
 * [title]·[detail]·[isError], 제공자가 보고한 [inputTokens]·[outputTokens]·
 * [responseModel]까지 모두 플러그인이 쓸 수 있어요. before 훅이 돌려준 값이
 * 실제 커밋·삭제·되돌리기 대상이 되고, 에코용 사본이 아니에요.
 */
data class PluginChatMessage(
    val apiMessage: ApiMessage,
    val images: List<ToolImage> = emptyList(),
    val title: String? = null,
    val detail: String? = null,
    val isError: Boolean = false,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val responseModel: String? = null,
)

/**
 * 메시지 삭제 여부를 정하는 결과예요.
 *
 * [Delete]는 삭제할 메시지를 그대로 넘겨 실제 대화·저장소·되돌리기에서 지우게 해요.
 * [Deny]는 UI·이력·되돌리기·저장 큐를 전혀 바꾸지 않아요. 호스트는 거부를 성공으로
 * 표시하지 않고 [Deny.reason]을 사용자에게 보여 줘요.
 */
sealed interface PluginDeleteDecision {
    data class Delete(val message: PluginChatMessage) : PluginDeleteDecision
    data class Deny(val reason: String) : PluginDeleteDecision
}
