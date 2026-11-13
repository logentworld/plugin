package com.sleepysoong.logent.plugin

/** 훅 한 번에 전달되는 세션 문맥이에요. 플러그인 코드는 이 값만으로 호스트 기능을 써요. */
data class PluginHookContext(
    val sessionId: String,
    val workspace: String,
    val model: String,
    val provider: String,
    val flags: Set<String> = emptySet(),
    val host: PluginHost,
)

/** 도구 호출을 계속할지, 인자를 바꿀지, 거부할지 정해요. */
sealed interface PluginToolCallDecision {
    data object Continue : PluginToolCallDecision
    data class Replace(val argumentsJson: String) : PluginToolCallDecision
    data class Deny(val message: String) : PluginToolCallDecision
}

/**
 * 플러그인이 코어 수명주기에 개입하는 훅이에요.
 *
 * 모든 훅은 실패해도 코어 실행을 막지 않아요. 여러 플러그인이 있으면 등록 순서대로
 * 이어서 적용하며, 시스템·사용자 문자열은 마지막 결과를 다음 플러그인에 넘겨요.
 *
 * [userMessage]는 모델 요청 직전의 프롬프트만 다듬는 별개 훅이에요. 대화에 남는
 * 사용자·AI 메시지와 삭제·복원은 [chatEvents]로 구독한 채팅 훅이 다뤄요. before*
 * 훅이 돌려준 값은 실제 커밋·삭제·되돌리기 페이로드이고, post 훅은 호스트 메모리와
 * 저장 큐에 반영된 뒤에 호출되며 네트워크·디스크 fsync 완료를 뜻하지 않아요.
 */
interface PluginHooks {
    suspend fun systemPrompt(context: PluginHookContext, prompt: String): String = prompt

    suspend fun userMessage(context: PluginHookContext, text: String): String = text

    suspend fun toolCall(
        context: PluginHookContext,
        toolName: String,
        argumentsJson: String,
    ): PluginToolCallDecision = PluginToolCallDecision.Continue

    suspend fun toolResult(
        context: PluginHookContext,
        toolName: String,
        result: ToolResult,
    ): ToolResult = result

    suspend fun sessionStart(context: PluginHookContext) {}

    fun sessionReset() {}

    /**
     * 구독할 채팅 이벤트예요. 기본값은 빈 집합이라 기존 플러그인은 채팅 훅을 받지
     * 않아요.
     *
     * 코어는 여기 선언된 이벤트의 before*·post 훅만 호출해요.
     */
    val chatEvents: Set<PluginChatEvent> get() = emptySet()

    /**
     * 사용자 메시지가 대화에 커밋되기 전에 본문을 바꿔요.
     *
     * 돌려준 [PluginChatMessage]가 실제 커밋 대상이에요. 중첩된 도구 호출과 이미지를
     * 포함한 모든 페이로드 필드를 바꿀 수 있고, [PluginChatContext]의 세션·신원·
     * 프로바이더·키는 읽기 전용이에요. 부모 코루틴이 취소되면 CancellationException을
     * 그대로 전파하고, 호스트가 소유한 시간 제한을 넘기면 이전 페이로드를 써요.
     */
    suspend fun beforeUserSend(
        context: PluginChatContext,
        message: PluginChatMessage,
    ): PluginChatMessage = message

    /** 사용자 메시지가 호스트 메모리와 저장 큐에 반영된 뒤에 호출돼요. */
    suspend fun userSent(context: PluginChatContext, message: PluginChatMessage) {}

    /**
     * AI 응답이 대화에 커밋되기 전에 본문을 바꿔요.
     * 적용 규칙은 [beforeUserSend]와 같아요.
     */
    suspend fun beforeAiReceive(
        context: PluginChatContext,
        message: PluginChatMessage,
    ): PluginChatMessage = message

    /** AI 응답이 호스트 메모리와 저장 큐에 반영된 뒤에 호출돼요. */
    suspend fun aiReceived(context: PluginChatContext, message: PluginChatMessage) {}

    /**
     * 메시지를 삭제하기 전에 허용 여부와 실제 삭제 페이로드를 정해요.
     *
     * [PluginDeleteDecision.Deny]를 돌려주면 UI·이력·되돌리기·저장 큐를 바꾸지 않고
     * 호스트는 성공을 표시하지 않아요. 기본값은 전달받은 메시지를 그대로 지워요.
     */
    suspend fun beforeMessageDelete(
        context: PluginChatContext,
        message: PluginChatMessage,
    ): PluginDeleteDecision = PluginDeleteDecision.Delete(message)

    /** 메시지 삭제가 호스트 메모리와 저장 큐에 반영된 뒤에 호출돼요. */
    suspend fun messageDeleted(context: PluginChatContext, message: PluginChatMessage) {}

    /** 삭제한 메시지를 되돌리기 직전에 복원할 본문을 바꿔요. */
    suspend fun beforeMessageRestore(
        context: PluginChatContext,
        message: PluginChatMessage,
    ): PluginChatMessage = message

    /** 메시지 복원이 호스트 메모리와 저장 큐에 반영된 뒤에 호출돼요. */
    suspend fun messageRestored(context: PluginChatContext, message: PluginChatMessage) {}
}
