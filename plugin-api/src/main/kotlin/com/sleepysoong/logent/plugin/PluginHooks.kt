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
}
