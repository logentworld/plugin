package com.sleepysoong.logent.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** 도구가 모델에 돌려주는 이미지. MIME과 base64 원본을 보존해요. */
@Serializable
data class ToolImage(
    val data: String,
    val mimeType: String,
)

/** OpenAI 호환 도구 스키마. 플러그인도 같은 계약을 써요. */
@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: FunctionSpec,
)

@Serializable
data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

/** 도구가 요구하는 능력. 실행기는 이 선언으로 병렬성·자식 권한·직렬화를 계산해요. */
enum class ToolAccess {
    READ_ONLY,
    WRITE,
    DESKTOP,
    DELEGATE,
    STATE,
    UI,
}

/** 도구 실행 결과. 이미지는 호스트가 다시 검증한 뒤 모델에 전달해요. */
data class ToolResult(
    val text: String,
    val images: List<ToolImage> = emptyList(),
    val isError: Boolean = false,
)

/** 플러그인 도구 실행에 필요한 대화 문맥. 비밀 키는 넘기지 않아요. */
data class PluginContext(
    val sessionId: String,
    val workspace: String,
    val progress: (String) -> Unit = {},
)

/** 플러그인이 등록하는 도구 하나. */
class PluginTool(
    val spec: ToolSpec,
    val access: Set<ToolAccess>,
    val childVisible: Boolean = false,
    val serialized: Boolean = false,
    val label: String = spec.function.name,
    val summarize: (JsonObject) -> String? = { null },
    val execute: suspend (JsonObject, PluginContext) -> ToolResult,
) {
    val name: String get() = spec.function.name
}

/** 플러그인 스킬 읽기에 전달되는 호스트·offset·번들 상대 경로예요. */
data class PluginSkillReadContext(
    val host: PluginHost,
    val offset: Int = 0,
    val path: String? = null,
)

/**
 * 플러그인이 제공하는 읽기 전용 스킬이에요.
 * `assetPath`가 있으면 코어가 플러그인 패키지 자산을 읽고,
 * `read`가 있으면 플러그인이 직접 원격·계산 본문을 공급해요.
 */
data class PluginSkill(
    val name: String,
    val description: String,
    val assetPath: String? = null,
    val source: String = "플러그인",
    val promptIndex: Boolean = true,
    val read: (suspend (PluginSkillReadContext) -> Result<String>)? = null,
) {
    init {
        require(Regex("[a-z0-9][a-z0-9-]{1,63}").matches(name)) { "스킬 이름이 올바르지 않아요." }
        require(description.length <= 500) { "스킬 설명이 너무 길어요." }
        require(assetPath != null || read != null) { "스킬 본문을 읽을 자산 경로나 reader가 필요해요." }
    }
}

/** 도구 프로세스의 출력. 셸 상한과 잘림 여부를 그대로 보존해요. */
data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val truncated: Boolean,
) {
    val success: Boolean get() = exitCode == 0
}
